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
import toniarts.openkeeper.game.component.Armour;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Decay;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Frozen;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Invisible;
import toniarts.openkeeper.game.component.Invulnerable;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.OnFire;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Poisoned;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.ShotFlight;
import toniarts.openkeeper.game.component.ShotViewState;
import toniarts.openkeeper.game.component.SpeedBlur;
import toniarts.openkeeper.game.component.SpeedChange;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.Tremor;
import toniarts.openkeeper.game.component.Unconscious;
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
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.FEAR;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.FREEZE;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.INFERNO;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.INSTANTANEOUS_HIT;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.INVISIBLE;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.INVUNERABLE;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.LIGHTNING;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.MODIFY_ARMOUR;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.MODIFY_HEALTH;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.MODIFY_SPEED;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.POISON_SPIT;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.POSSESS_CREATURE;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.SIGHT_OF_EVIL;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.SUMMON_REAPER;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.TREMOR;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.TURNCOAT;

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
        result.put(CREATE_CREATURE, request -> {

            // The shot's second value is how many creatures to create, each at level 1 (0x4AD7A0)
            for (int i = 0; i < request.shotData2(); i++) {
                creaturesController.spawnCreature((short) request.shotData1(), request.playerId(), 1,
                        WorldUtils.vector3fToVector2f(request.position()), ICreaturesController.SpawnType.CONJURE);
            }
        });
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
        // Haste and Slow: the first value is the new speed as a percentage of the current one, the second how many
        // seconds it lasts. The speed stays between a quarter of the creature's own and a tile a game tick, and a
        // speed change that is still running is left as it is. Then, like 0x4AE000, a creature faster than its own
        // speed shows the speed blur for as many game ticks as the second value, a slower one none
        result.put(MODIFY_SPEED, request -> {
            Mobile mobile = entityData.getComponent(request.target(), Mobile.class);
            CreatureComponent creature = entityData.getComponent(request.target(), CreatureComponent.class);
            if (mobile == null || creature == null) {
                return;
            }
            if (entityData.getComponent(request.target(), SpeedChange.class) == null) {
                float speed = FastMath.clamp(mobile.maxSpeed * request.shotData1() / 100, creature.speed / 4, kwdFile.getGameLevel().getTicksPerSec());
                mobile = new Mobile(mobile.canFly, mobile.canWalkOnWater, mobile.canWalkOnLava, speed);
                entityData.setComponents(request.target(), mobile, new SpeedChange(gameTimer.getGameTime() + request.shotData2()));
            }
            if (mobile.maxSpeed > creature.speed) {
                entityData.setComponent(request.target(), new SpeedBlur(gameTimer.getGameTime()
                        + request.shotData2() / (double) kwdFile.getGameLevel().getTicksPerSec()));
            } else if (mobile.maxSpeed < creature.speed) {
                entityData.removeComponent(request.target(), SpeedBlur.class);
            }
        });
        // Cast Armour: the first value is how long the armour lasts, in game ticks
        result.put(MODIFY_ARMOUR, request -> entityData.setComponent(request.target(),
                new Armour(gameTimer.getGameTime() + (double) request.shotData1() / kwdFile.getGameLevel().getTicksPerSec())));
        // Invisible: the first value is how long it lasts, in seconds
        result.put(INVISIBLE, request -> entityData.setComponent(request.target(),
                new Invisible(gameTimer.getGameTime() + request.shotData1())));
        // Invulnerable: the first value is how long it lasts, in seconds
        result.put(INVUNERABLE, request -> entityData.setComponent(request.target(),
                new Invulnerable(gameTimer.getGameTime() + request.shotData1())));
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
        // The shot is at its target at once and dies there, its death shot goes on with the same values
        result.put(INSTANTANEOUS_HIT, request -> {
            if (request.shot().getDeathShotId() != 0) {
                createShot(request.shot().getDeathShotId(), request.shotData1(), request.shotData2(), request.playerId(), request.position(), request.target(), null);
            }
        });
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
        result.put(POISON_SPIT, this::poison);

        // The Fear Trap's shot does nothing itself: DK II's handler for it is empty (the FEAR entry of the shot table
        // filled at 0x4AD1B0). The trap scares with the threat it makes after it fires, see TrapSystem
        result.put(FEAR, request -> {
        });

        return result;
    }

    @Override
    public void createShot(short shotTypeId, int shotData1, int shotData2, short playerId, Vector3f position, EntityId target, EntityId shooter) {

        // Creature attacks still apply their damage themselves, see CreatureController#executeAttack. Processing
        // their shots here too would hit twice (Mistress and Fairy lightning share the keeper's process type).
        // What a creature casts on its own side is no attack and is processed here
        Shot shot = kwdFile.getShotById(shotTypeId);
        if (shooter != null && shot.getCollideType() != Shot.CollideType.OWN_CREATURES) {
            return;
        }

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
     * A shot deals its damage by its damage type, whatever else the shot does.
     * A shot that hits at once does it once, a flying shot on every turn
     * (0x4AC3C0). The shot's data 1 is the damage:
     * <ul>
     * <li>A shot that hits a thing (Magic Door) hurts its target.</li>
     * <li>A shot with constant damage (Spike Trap) hurts the creatures in a
     * square twice shot data 2 wide.</li>
     * <li>A shot with damage from its origin (Fireburst Trap, Gas Trap, Gas
     * Cloud) hurts the things within shot data 2 tiles, the less the further
     * they are (0x4AC6D0).</li>
     * </ul>
     *
     * @param request the shot
     */
    private void damage(ShotRequest request) {
        Shot.DamageType damageType = request.shot().getDamageType();
        if (damageType == Shot.DamageType.HIT_THING && request.target() != null) {
            EntityController.setDamage(entityData, request.target(), request.shotData1());
        } else if (damageType == Shot.DamageType.CONSTANT) {
            damageCreatures(request);
        } else if (damageType == Shot.DamageType.RANGE_FROM_ORIGIN && request.shotData2() > 0) {
            damageFromOrigin(request);
        }
    }

    /**
     * Constant damage (0x4AC5A0) hurts every creature but those of the player
     * that fired the shot in a square of tiles twice the radius (shot data 2)
     * wide, all with the full damage. The square runs from the radius below
     * the shot's tile to one less than the radius above it, so a radius of 1,
     * the Spike Trap's, covers the shot's tile and the three on its lower side
     *
     * @param request the shot
     */
    private void damageCreatures(ShotRequest request) {
        IEntityPositionLookup entityPositionLookup = gameController.getEntityLookupService();
        Point tile = WorldUtils.vectorToPoint(request.position());
        int radius = request.shotData2();
        for (int x = tile.x - radius; x < tile.x + radius; x++) {
            for (int y = tile.y - radius; y < tile.y + radius; y++) {
                for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(x, y)) {
                    Owner owner = entityData.getComponent(entityId, Owner.class);
                    if (owner == null || owner.ownerId == request.playerId()
                            || entityData.getComponent(entityId, CreatureComponent.class) == null
                            || entityData.getComponent(entityId, Health.class) == null) {
                        continue;
                    }

                    // One picked up this tick is still in the lookup, but not on the map
                    if (EntityController.getPosition(entityData, entityId) != null) {
                        EntityController.setDamage(entityData, entityId, request.shotData1());
                    }
                }
            }
        }
    }

    /**
     * Damage from the origin (0x4AC6D0). The tiles within the radius are gone
     * through row by row, and each thing of another player on them that is
     * within the radius takes damage x (radius - distance) / radius. That is
     * then the damage for the next thing, so the damage runs down within the
     * turn, and once it is 0 nothing more is hit. Creatures and objects are
     * hit, and doors and traps too unless the shot is gas.
     *
     * @param request the shot
     */
    private void damageFromOrigin(ShotRequest request) {
        IEntityPositionLookup entityPositionLookup = gameController.getEntityLookupService();
        boolean gas = request.shot().getAttackCategory() == Shot.AttackCategory.GAS;
        int radius = request.shotData2();
        int damage = request.shotData1();
        Vector3f origin = request.position();
        Point tile = WorldUtils.vectorToPoint(origin);
        for (int y = tile.y - radius; y <= tile.y + radius; y++) {
            for (int x = tile.x - radius; x <= tile.x + radius; x++) {
                for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(x, y)) {
                    if (!isHitFromOrigin(entityId, request.playerId(), gas)) {
                        continue;
                    }

                    // One picked up this tick is still in the lookup, but not on the map
                    Vector3f position = EntityController.getPosition(entityData, entityId);
                    if (position == null) {
                        continue;
                    }
                    float distance = FastMath.sqrt(FastMath.sqr(position.x - origin.x) + FastMath.sqr(position.z - origin.z));
                    if (distance >= radius) {
                        continue;
                    }
                    damage = (int) (damage * (radius - distance) / radius);
                    if (damage == 0) {
                        return;
                    }
                    EntityController.setDamage(entityData, entityId, damage);
                }
            }
        }
    }

    private boolean isHitFromOrigin(EntityId entityId, short playerId, boolean gas) {
        Owner owner = entityData.getComponent(entityId, Owner.class);
        if (owner == null || owner.ownerId == playerId || entityData.getComponent(entityId, Health.class) == null) {
            return false;
        }
        if (entityData.getComponent(entityId, CreatureComponent.class) != null) {
            return true;
        }

        // Of the objects only the damageable ones are hurt, like chickens, not the furniture (0x4B2CE0)
        ObjectComponent object = entityData.getComponent(entityId, ObjectComponent.class);
        if (object != null) {
            return kwdFile.getObject(object.objectId).getFlags().contains(GameObject.ObjectFlag.DAMAGEABLE);
        }
        return !gas && (entityData.getComponent(entityId, DoorComponent.class) != null
                || entityData.getComponent(entityId, TrapComponent.class) != null);
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
     * Poisons the creature the shot hit for shot data 1 ticks, taking shot data
     * 2 damage a tick. A new poisoning starts the poison over. A dying creature
     * isn't poisoned (0x4B0270)
     *
     * @param request the shot
     */
    private void poison(ShotRequest request) {
        EntityId target = request.target();
        if (target == null || entityData.getComponent(target, CreatureComponent.class) == null
                || entityData.getComponent(target, Unconscious.class) != null) {
            return;
        }

        entityData.setComponent(target, new Poisoned(request.shotData1(), request.shotData2()));
    }

    /**
     * Raises an alarm for the player where the shot was fired and tells the
     * player of it, unless the player's last alarm still lasts
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
        gameEventService.emitToPlayer(GameEventType.ALARM_RAISED, request.playerId());
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

        // A trap's shot flies if it moves towards its target, a creature's whenever it has a speed, like the
        // Bile Demon's Gas Cloud (0x4984D0). A creature's instant shot, like Hail Storm, is a shot too, it stands
        // still and goes onto its target in its turn (0x498870, 0x4AFCB0)
        boolean creatureShot = shooter != null && entityData.getComponent(shooter, CreatureComponent.class) != null;
        boolean flying = creatureShot
                ? shot.getSpeed() > 0 || shot.getProcessType() == Shot.ProcessType.INSTANTANEOUS_HIT
                : ShotFlightRules.isFlying(shot);
        if (!flying || targetPosition == null) {
            return false;
        }

        Vector3f velocity = ShotFlightRules.aim(position, targetPosition, shot.getSpeed());
        addShot(new ShotFlight(shotTypeId, playerId, shooter, target, shotData1, shotData2, velocity, new Vector3f(position),
                maxRange, shot.getHealth()));

        return true;
    }

    private void addShot(ShotFlight flight) {
        int ticksPerSecond = kwdFile.getGameLevel().getTicksPerSec();
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity,
                new Position(FastMath.atan2(flight.velocity.x, flight.velocity.z), new Vector3f(flight.start)),
                new ShotViewState(flight.shotId, flight.velocity.mult(ticksPerSecond)),
                flight);
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

        // The shot's data 1 is its damage. An invulnerable creature takes none
        // and isn't set alight either (0x4AC420)
        if (shot.getDamageType() == Shot.DamageType.HIT_THING && flight.shotData1 > 0
                && entityData.getComponent(thing, Invulnerable.class) == null) {
            if (shot.getAttackCategory() == Shot.AttackCategory.FIRE) {
                setAlight(thing);
            }
            EntityController.setDamage(entityData, thing, flight.shotData1);
        }

        // Only a shot made to does its process on a hit. An instant shot's process is its move onto its target, on
        // every turn (0x4ABD60)
        if (shot.getShotProcessFlags().contains(Shot.ShotProcessFlag.HIT_COLLIDE_THING)) {
            process(new ShotRequest(shot, flight.shotData1, flight.shotData2, flight.ownerId,
                    EntityController.getPosition(entityData, shotEntity), thing, flight.shooter));
        }
    }

    @Override
    public void damageArea(EntityId shotEntity) {
        ShotFlight flight = entityData.getComponent(shotEntity, ShotFlight.class);

        damage(new ShotRequest(kwdFile.getShotById(flight.shotId), flight.shotData1, flight.shotData2, flight.ownerId,
                EntityController.getPosition(entityData, shotEntity), null));
    }

    @Override
    public void createChildShot(EntityId shotEntity, short shotTypeId, Vector3f position) {
        ShotFlight flight = entityData.getComponent(shotEntity, ShotFlight.class);

        // The new shot keeps the old one's values and target, not its own (0x4AB180)
        addShot(new ShotFlight(shotTypeId, flight.ownerId, flight.shooter, flight.target, flight.shotData1, flight.shotData2,
                new Vector3f(), new Vector3f(position), 0, kwdFile.getShotById(shotTypeId).getHealth()));
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
