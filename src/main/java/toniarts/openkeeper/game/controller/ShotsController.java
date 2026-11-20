/*
 * Copyright (C) 2014-2024 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.controller;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.filter.FieldFilter;
import toniarts.openkeeper.game.component.Alarm;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Decay;
import toniarts.openkeeper.game.component.Frozen;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.OnFire;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.ShotFlight;
import toniarts.openkeeper.game.component.ShotViewState;
import toniarts.openkeeper.game.component.SpeedChange;
import toniarts.openkeeper.game.component.Tremor;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.controller.shot.IShotProcessor;
import toniarts.openkeeper.game.controller.shot.InfernoShotProcessor;
import toniarts.openkeeper.game.controller.shot.LightningShotProcessor;
import toniarts.openkeeper.game.controller.shot.ShotFlightRules;
import toniarts.openkeeper.game.controller.shot.ShotRequest;
import toniarts.openkeeper.game.event.GameEvent.ThingFlag;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import java.lang.System.Logger;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.ALARM;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.CHICKEN;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.CREATE_CREATURE;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.CREATE_OBJECT;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.DRAIN;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.FREEZE;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.INFERNO;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.LIGHTNING;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.MODIFY_HEALTH;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.POSSESS_CREATURE;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.SIGHT_OF_EVIL;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.SUMMON_REAPER;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.TREMOR;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.TURNCOAT;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.WEB_AREA_EFFECT;

/**
 * Fires shots. What a shot does is decided by its process type, each
 * implemented process type has an {@link IShotProcessor}.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class ShotsController implements IShotsController {

    private static final Logger logger = System.getLogger(ShotsController.class.getName());

    /**
     * How long, in seconds, an alarm lasts. No new alarm is raised for the
     * player meanwhile. Not in the level or shot data: it is the 50 seconds of
     * message 0x2f ("Enemy creatures are rampaging through your Dungeon") in
     * DKII.EXE's message table, which the alarm shot's handler (0x4AE120)
     * queues through the message queue (0x4C4600)
     */
    private static final double ALARM_DURATION = 50;

    /**
     * How many game ticks a creature burns after a fire shot set it alight
     * (0x4AC420). The shots' burn duration in the data is 0 for every shot and
     * the original doesn't read it
     */
    private static final int ON_FIRE_TICKS = 10;

    private final IKwdFile kwdFile;
    private final EntityData entityData;
    private final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private final IGameTimer gameTimer;
    private final IGameController gameController;
    private final IMapController mapController;
    private final ILevelInfo levelInfo;
    private final IObjectsController objectsController;
    private final ICreaturesController creaturesController;
    private final GameEventService gameEventService;
    private final Map<Shot.ProcessType, IShotProcessor> processors;
    private final Set<Shot.ProcessType> unprocessedTypes = EnumSet.noneOf(Shot.ProcessType.class);

    public ShotsController(IKwdFile kwdFile, EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings, IGameTimer gameTimer,
            IGameController gameController, IMapController mapController, ILevelInfo levelInfo, IObjectsController objectsController, ICreaturesController creaturesController,
            GameEventService gameEventService) {
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.gameSettings = gameSettings;
        this.gameTimer = gameTimer;
        this.gameController = gameController;
        this.mapController = mapController;
        this.levelInfo = levelInfo;
        this.objectsController = objectsController;
        this.creaturesController = creaturesController;
        this.gameEventService = gameEventService;
        this.processors = createProcessors();
    }

    private Map<Shot.ProcessType, IShotProcessor> createProcessors() {
        Map<Shot.ProcessType, IShotProcessor> result = new EnumMap<>(Shot.ProcessType.class);
        result.put(CREATE_CREATURE, request -> creaturesController.spawnCreature((short) request.shotData1(), request.playerId(),
                request.shotData2(), WorldUtils.vector3fToVector2f(request.position()), ICreaturesController.SpawnType.CONJURE));
        result.put(CREATE_OBJECT, request -> {
            GameObject object = kwdFile.getObject(request.shotData1());
            if (object != null && object.getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_GOLD)) {

                // Create Gold: the shot's second value is the amount of gold
                Point tile = WorldUtils.vectorToPoint(request.position());
                objectsController.loadObject((short) request.shotData1(), request.playerId(), tile.x, tile.y, request.shotData2(), null, null);
            } else {
                objectsController.loadObject((short) request.shotData1(), request.playerId(), request.position(), 0);
            }
        });
        result.put(POSSESS_CREATURE, request -> creaturesController.createController(request.target()).setPossession(true));
        result.put(MODIFY_HEALTH, request -> EntityController.setDamage(entityData, request.target(), -request.shotData1()));
        result.put(CHICKEN, request -> {
            if (request.target() != null && creaturesController.isValidEntity(request.target())) {
                creaturesController.createController(request.target()).turnIntoChicken(request.shotData1());
            }
        });
        result.put(SIGHT_OF_EVIL, request -> {

            // Shot data 1 is the radius, the shot lifetime (in game ticks) the duration
            if (request.shotData1() > 0) {
                mapController.revealSightOfEvil(WorldUtils.vectorToPoint(request.position()), request.shotData1(), request.shot().getHealth(), request.playerId());
            }
        });
        result.put(TREMOR, request -> {

            // Shot data 1 is the duration in seconds, shot data 2 the radius in tiles
            double now = gameTimer.getGameTime();
            entityData.setComponent(entityData.createEntity(), new Tremor(WorldUtils.vectorToPoint(request.position()), request.shotData2(), now, now + request.shotData1()));
        });
        result.put(TURNCOAT, request -> {
            if (request.target() != null && creaturesController.isValidEntity(request.target())) {
                creaturesController.createController(request.target()).turncoat(request.playerId(), request.shotData1());
            }
        });
        result.put(SUMMON_REAPER, request -> new ReaperSummoner(kwdFile, entityData, gameTimer, creaturesController).summon(request.shot().getShotId(),
                request.shotData1(), request.playerId(), WorldUtils.vector3fToVector2f(request.position())));
        result.put(LIGHTNING, new LightningShotProcessor(entityData, gameTimer, kwdFile, mapController, creaturesController));
        // Drain: the first value is how much of the shooter's full health it takes off the target, as a percentage,
        // but no more than the target has left. The shooter gets it
        result.put(DRAIN, request -> {
            Health health = entityData.getComponent(request.target(), Health.class);
            Health shooterHealth = request.shooter() != null ? entityData.getComponent(request.shooter(), Health.class) : null;
            if (health != null && shooterHealth != null) {
                int amount = Math.min(health.health, shooterHealth.maxHealth * request.shotData1() / 100);
                EntityController.setDamage(entityData, request.target(), amount);
                EntityController.setDamage(entityData, request.shooter(), -amount);
            }
        });
        result.put(INFERNO, new InfernoShotProcessor(entityData, gameTimer, () -> kwdFile.getGameLevel().getTicksPerSec()));
        result.put(FREEZE, this::freeze);
        result.put(ALARM, this::raiseAlarm);
        result.put(WEB_AREA_EFFECT, this::web);

        return result;
    }

    @Override
    public void createShot(short shotTypeId, int shotData1, int shotData2, short playerId, Vector3f position, EntityId target, EntityId shooter) {

        // Creature attacks still apply their damage themselves, see CreatureController#executeAttack. Processing
        // their shots here too would hit twice (Mistress and Fairy lightning share the keeper's process type)
        if (shooter != null) {
            return;
        }

        Shot shot = kwdFile.getShotById(shotTypeId);
        ShotRequest request = new ShotRequest(shot, shotData1, shotData2, playerId, position, target);
        notifyShotHit(shot, target);
        damage(request);
        process(request);
    }

    private void process(ShotRequest request) {
        Shot.ProcessType processType = request.shot().getProcessType();
        IShotProcessor processor = processors.get(processType);
        if (processor != null) {
            processor.process(request);
        } else if (processType != Shot.ProcessType.NONE && unprocessedTypes.add(processType)) {
            logger.log(Logger.Level.WARNING, "Shot type {0} not implemented", processType);
        }
    }

    /**
     * A shot that hits at once deals its damage by its damage type, whatever
     * else the shot does (0x4AC3C0). The shot's data 1 is the damage:
     * <ul>
     * <li>A shot that hits a thing (Magic Door) hurts its target.</li>
     * <li>A shot with constant damage (Spike Trap) hurts the creatures on its
     * tile.</li>
     * <li>A shot with damage from its origin (Fireburst Trap, Gas Trap) hurts
     * the creatures within shot data 2 tiles.</li>
     * </ul>
     *
     * @param request the shot
     */
    private void damage(ShotRequest request) {
        Shot.DamageType damageType = request.shot().getDamageType();
        if (damageType == Shot.DamageType.HIT_THING && request.target() != null) {
            EntityController.setDamage(entityData, request.target(), request.shotData1());
        } else if (damageType == Shot.DamageType.CONSTANT) {
            damageCreatures(request, 0);
        } else if (damageType == Shot.DamageType.RANGE_FROM_ORIGIN) {
            damageCreatures(request, request.shotData2());
        }
    }

    /**
     * Area damage spares only the creatures of the player that fired the shot
     * (0x4AC5A0)
     *
     * @param request the shot
     * @param radius how far the shot reaches in tiles, 0 for just its tile
     */
    private void damageCreatures(ShotRequest request, int radius) {
        IEntityPositionLookup entityPositionLookup = gameController.getEntityLookupService();
        Vector3f origin = request.position();
        Point tile = WorldUtils.vectorToPoint(origin);
        for (int x = tile.x - radius; x <= tile.x + radius; x++) {
            for (int y = tile.y - radius; y <= tile.y + radius; y++) {
                for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(x, y)) {
                    Owner owner = entityData.getComponent(entityId, Owner.class);
                    if (owner == null || owner.ownerId == request.playerId()
                            || entityData.getComponent(entityId, CreatureComponent.class) == null
                            || entityData.getComponent(entityId, Health.class) == null) {
                        continue;
                    }

                    Vector3f position = EntityController.getPosition(entityData, entityId);
                    float dx = position.x - origin.x;
                    float dz = position.z - origin.z;
                    if (radius == 0 || dx * dx + dz * dz <= radius * radius) {
                        EntityController.setDamage(entityData, entityId, request.shotData1());
                    }
                }
            }
        }
    }

    /**
     * Freezes the target for shot data 1 seconds, unless it already is frozen
     * or is of a creature type that can't be frozen
     *
     * @param request the shot
     */
    private void freeze(ShotRequest request) {
        EntityId target = request.target();
        if (target == null || entityData.getComponent(target, Frozen.class) != null) {
            return;
        }

        // Like with setting off traps, the data flag OpenKeeper calls IS_HORNY makes the Horned Reaper immune
        CreatureComponent creatureComponent = entityData.getComponent(target, CreatureComponent.class);
        if (creatureComponent == null) {
            return;
        }
        Creature creature = kwdFile.getCreature(creatureComponent.creatureId);
        if (creature != null && creature.getFlags().contains(Creature.CreatureFlag.IS_HORNY)) {
            return;
        }

        entityData.setComponent(target, new Frozen(gameTimer.getGameTime(), request.shotData1()));
    }

    /**
     * Raises an alarm for the player where the shot was fired, unless the
     * player's last alarm still lasts
     *
     * @param request the shot
     */
    private void raiseAlarm(ShotRequest request) {
        if (entityData.findEntity(new FieldFilter<>(Owner.class, "ownerId", request.playerId()), Alarm.class, Owner.class) != null) {
            return;
        }

        EntityId alarm = entityData.createEntity();
        entityData.setComponents(alarm, new Alarm(), new Owner(request.playerId(), request.playerId()),
                new Position(0, new Vector3f(request.position())), new Decay(gameTimer.getGameTime(), ALARM_DURATION));
    }

    /**
     * The Maiden's web (0x4B0050). Every creature closer than shot data 2
     * tiles to where the web lands is caught, whoever owns it, only the
     * shooter is left out. A creature caught walks at a quarter of its own
     * speed for shot data 1 seconds, unless its speed is already changed
     * (0x48CAD0).<br>
     * The web asks 0x48CAD0 for 90 % less speed, which is below the quarter
     * that 0x48CAD0 keeps at least. DK II 1.7 does that sum unsigned and ends
     * up at the speed cap instead, we follow the intent.
     *
     * @param request the shot
     */
    private void web(ShotRequest request) {
        IEntityPositionLookup entityPositionLookup = gameController.getEntityLookupService();
        Vector3f origin = request.position();
        Point tile = WorldUtils.vectorToPoint(origin);
        int radius = request.shotData2();
        for (int x = Math.max(0, tile.x - radius); x <= Math.min(mapController.getMapData().getWidth() - 1, tile.x + radius); x++) {
            for (int y = Math.max(0, tile.y - radius); y <= Math.min(mapController.getMapData().getHeight() - 1, tile.y + radius); y++) {
                for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(x, y)) {
                    Mobile mobile = entityData.getComponent(entityId, Mobile.class);
                    CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
                    if (entityId.equals(request.shooter()) || mobile == null || creatureComponent == null
                            || entityData.getComponent(entityId, SpeedChange.class) != null) {
                        continue;
                    }

                    Vector3f position = EntityController.getPosition(entityData, entityId);
                    float dx = position.x - origin.x;
                    float dz = position.z - origin.z;
                    if (dx * dx + dz * dz < radius * radius) {
                        entityData.setComponents(entityId,
                                new Mobile(mobile.canFly, mobile.canWalkOnWater, mobile.canWalkOnLava, creatureComponent.speed / 4),
                                new SpeedChange(gameTimer.getGameTime() + request.shotData1()));
                    }
                }
            }
        }
    }

    private void notifyShotHit(Shot shot, EntityId target) {
        Position targetPosition = target != null ? entityData.getComponent(target, Position.class) : null;
        if (targetPosition != null) {
            gameEventService.emitAt(GameEventType.SHOT_HIT, ThingFlag.SHOT, shot.getShotId(), target, targetPosition.position);
        }
    }

    @Override
    public boolean fireShot(short shotTypeId, int shotData1, int shotData2, short playerId, Vector3f position, EntityId target,
            EntityId shooter, float maxRange) {
        Shot shot = kwdFile.getShotById(shotTypeId);
        Vector3f targetPosition = getAimPoint(target);
        if (!ShotFlightRules.isFlying(shot) || targetPosition == null) {
            return false;
        }

        Vector3f velocity = ShotFlightRules.aim(position, targetPosition, shot.getSpeed());
        int ticksPerSecond = kwdFile.getGameLevel().getTicksPerSec();
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity,
                new Position(FastMath.atan2(velocity.x, velocity.z), new Vector3f(position)),
                new ShotViewState(shotTypeId, velocity.mult(ticksPerSecond)),
                new ShotFlight(shotTypeId, playerId, shooter, shotData1, shotData2, velocity, new Vector3f(position), maxRange,
                        shot.getHealth()));

        return true;
    }

    /**
     * Shots are aimed at the middle of a creature, half its height above its
     * feet (0x4A7FD0)
     *
     * @param target the target
     * @return the spot to aim at, {@code null} if the target isn't in the
     * world
     */
    private Vector3f getAimPoint(EntityId target) {
        Vector3f position = EntityController.getPosition(entityData, target);
        CreatureComponent creatureComponent = entityData.getComponent(target, CreatureComponent.class);
        if (position == null || creatureComponent == null) {
            return position;
        }

        return position.add(0, creatureComponent.height / 2, 0);
    }

    @Override
    public void hit(EntityId shotEntity, EntityId thing) {
        ShotFlight flight = entityData.getComponent(shotEntity, ShotFlight.class);
        Shot shot = kwdFile.getShotById(flight.shotId);

        // The shot's data 1 is its damage (0x4AC420)
        if (shot.getDamageType() == Shot.DamageType.HIT_THING && flight.shotData1 > 0) {
            if (shot.getAttackCategory() == Shot.AttackCategory.FIRE) {
                setAlight(thing);
            }
            EntityController.setDamage(entityData, thing, flight.shotData1);
        }

        process(new ShotRequest(shot, flight.shotData1, flight.shotData2, flight.ownerId,
                EntityController.getPosition(entityData, shotEntity), thing, flight.shooter));
    }

    /**
     * A fire shot sets a creature alight when the creature has less than 21 %
     * of its health left before the hit and doesn't stand on water (0x4AC420).
     * Another hit lights the fire again for the full time
     *
     * @param thing the thing the shot hit
     */
    private void setAlight(EntityId thing) {
        Health health = entityData.getComponent(thing, Health.class);
        Position position = entityData.getComponent(thing, Position.class);
        if (health == null || health.maxHealth <= 0 || position == null
                || entityData.getComponent(thing, CreatureComponent.class) == null
                || health.health * 100 / health.maxHealth >= 21
                || mapController.isWater(WorldUtils.vectorToPoint(position.position))) {
            return;
        }

        int ticksPerSecond = kwdFile.getGameLevel().getTicksPerSec();
        entityData.setComponent(thing, new OnFire(gameTimer.getGameTime() + (double) ON_FIRE_TICKS / ticksPerSecond));
    }

}
