/*
 * Copyright (C) 2014-2026 OpenKeeper
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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Alarm;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.Decay;
import toniarts.openkeeper.game.component.Frozen;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.OnFire;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.ShotFlight;
import toniarts.openkeeper.game.component.ShotViewState;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.GameObjectFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.TrapFiringFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShotsControllerTest {

    private static final short HEAL_ID = 4;
    private static final short CREATE_OBJECT_ID = 2;
    private static final short GOLD_OBJECT_ID = 1;
    private static final short SIGHT_OF_EVIL_ID = 33;
    private static final short SKELETON_ID = 9;
    private static final int RAISE_DEAD_EFFECT_ID = 324;
    private static final short DRAIN_ID = 42;
    private static final short SPIKE_ID = 27;
    private static final short GAS_CLOUD_ID = 58;
    private static final short DOOR_FIREBALL_ID = 57;
    private static final short FREEZE_ID = 45;
    private static final short ALARM_ID = 13;
    private static final short REAPER_ID = 2;
    private static final short PLAYER_ID = 3;
    private static final short ENEMY_ID = 4;
    private static final int TICKS_PER_SECOND = 4;
    private static final float DELTA = 0.0001f;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Short, Shot> shotsById = new HashMap<>();
    private final List<Object[]> loadedObjects = new ArrayList<>();
    private final List<EntityId> deadBodies = new ArrayList<>();
    private final List<Object[]> raised = new ArrayList<>();
    private final List<Object[]> effects = new ArrayList<>();
    private final Set<Point> waterTiles = new HashSet<>();
    private final List<EntityId> pickedUpThings = new ArrayList<>();
    private final ShotsController shotsController;
    private double gameTime = 0;

    ShotsControllerTest() {
        shotsById.put(ShotFixtures.THUNDERBOLT_ID, ShotFixtures.thunderbolt());
        shotsById.put(ShotFixtures.FIREBALL_ID, ShotFixtures.fireball());
        shotsById.put(ShotFixtures.RAISE_DEAD_ID, ShotFixtures.raiseDead());
        addShot(HEAL_ID, Shot.ProcessType.MODIFY_HEALTH);
        addShot(CREATE_OBJECT_ID, Shot.ProcessType.CREATE_OBJECT);
        addShot(SIGHT_OF_EVIL_ID, Shot.ProcessType.SIGHT_OF_EVIL);
        // Drain does its process when it hits, as in the original data
        shotsById.put(DRAIN_ID, ShotFixtures.shot(DRAIN_ID, Shot.ProcessType.DRAIN, 0, EnumSet.of(Shot.ShotProcessFlag.HIT_COLLIDE_THING)));
        shotsById.put(SPIKE_ID, TrapFiringFixtures.shot(SPIKE_ID, Shot.ProcessType.NONE, Shot.DamageType.CONSTANT));
        shotsById.put(GAS_CLOUD_ID, TrapFiringFixtures.shot(GAS_CLOUD_ID, Shot.ProcessType.GAS, Shot.DamageType.RANGE_FROM_ORIGIN));
        shotsById.put(DOOR_FIREBALL_ID, TrapFiringFixtures.shot(DOOR_FIREBALL_ID, Shot.ProcessType.NONE, Shot.DamageType.HIT_THING));
        shotsById.put(FREEZE_ID, TrapFiringFixtures.shot(FREEZE_ID, Shot.ProcessType.FREEZE, Shot.DamageType.NONE));
        shotsById.put(ALARM_ID, TrapFiringFixtures.shot(ALARM_ID, Shot.ProcessType.ALARM, Shot.DamageType.NONE));
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getShotById" -> shotsById.get((Short) args[0]);
                    case "getObject" -> (int) args[0] == GOLD_OBJECT_ID ? GameObjectFixtures.gameObject("Gold", GameObject.ObjectFlag.OBJECT_TYPE_GOLD) : null;
                    case "getGameLevel" -> ShotFixtures.gameLevel(TICKS_PER_SECOND);
                    case "getCreature" -> (short) args[0] == REAPER_ID ? TrapFiringFixtures.creature(REAPER_ID, Creature.CreatureFlag.IS_HORNY) : null;
                    default -> null;
                });
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(), new Class<?>[]{IObjectsController.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("loadObject")) {
                        loadedObjects.add(args);
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

        // A map without tiles, no strike lands on water. To a fire shot only the tiles in waterTiles are water
        IMapDataInformation<?> mapData = (IMapDataInformation<?>) Proxy.newProxyInstance(IMapDataInformation.class.getClassLoader(), new Class<?>[]{IMapDataInformation.class},
                (proxy, method, args) -> null);
        IMapController mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(), new Class<?>[]{IMapController.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getMapData" -> mapData;
                    case "isWater" -> waterTiles.contains((Point) args[0]);
                    default -> null;
                });
        GameEventService gameEventService = (GameEventService) Proxy.newProxyInstance(GameEventService.class.getClassLoader(), new Class<?>[]{GameEventService.class},
                (proxy, method, args) -> method.getName().equals("emitEffect") ? effects.add(args) : null);

        IEntityPositionLookup entityPositionLookup = (IEntityPositionLookup) Proxy.newProxyInstance(IEntityPositionLookup.class.getClassLoader(), new Class<?>[]{IEntityPositionLookup.class},
                (proxy, method, args) -> args.length == 1 ? deadBodies.stream()
                        .filter(entity -> ((Point) args[0]).equals(WorldUtils.vectorToPoint(entityData.getComponent(entity, Position.class).position)))
                        .toList()
                        : Stream.concat(entityData.findEntities(null, Position.class).stream()
                                .filter(entity -> new Point((int) args[0], (int) args[1]).equals(WorldUtils.vectorToPoint(entityData.getComponent(entity, Position.class).position))),
                                pickedUpThings.stream()).toList());
        IGameController gameController = (IGameController) Proxy.newProxyInstance(IGameController.class.getClassLoader(), new Class<?>[]{IGameController.class},
                (proxy, method, args) -> method.getName().equals("getEntityLookupService") ? entityPositionLookup : null);
        ICreaturesController creaturesController = (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(), new Class<?>[]{ICreaturesController.class},
                (proxy, method, args) -> method.getName().equals("turnCreatureIntoAnother") ? raised.add(args) : null);

        shotsController = new ShotsController(kwdFile, entityData, null, () -> gameTime, gameController, mapController, null, objectsController, creaturesController, gameEventService);
    }

    @Test
    void keeperLightningStrikes() {
        EntityId creature = createCreature();

        shotsController.createShot(ShotFixtures.THUNDERBOLT_ID, 150, 0, PLAYER_ID, new Vector3f(), creature, null);

        assertEquals(150, entityData.getComponent(creature, Damage.class).damage);
        assertEquals(1, countShotEntities());
    }

    @Test
    void creatureLightningIsLeftToTheCreature() {
        EntityId creature = createCreature();
        EntityId mistress = createCreature();

        // The creature applies its own damage for now, the shot must not hit a second time
        shotsController.createShot(ShotFixtures.THUNDERBOLT_ID, 300, 0, PLAYER_ID, new Vector3f(), creature, mistress);

        assertNull(entityData.getComponent(creature, Damage.class));
        assertEquals(0, countShotEntities());
    }

    @Test
    void healingShotTakesTheDamageAway() {
        EntityId creature = entityData.createEntity();

        shotsController.createShot(HEAL_ID, 50, 0, PLAYER_ID, new Vector3f(), creature, null);

        assertEquals(-50, entityData.getComponent(creature, Damage.class).damage);
    }

    @Test
    void objectShotCreatesTheObjectForThePlayer() {
        Vector3f position = new Vector3f(1.5f, 1f, 2.5f);

        shotsController.createShot(CREATE_OBJECT_ID, 7, 0, PLAYER_ID, position, null, null);

        assertEquals(1, loadedObjects.size());
        assertEquals((short) 7, loadedObjects.get(0)[0]);
        assertEquals(PLAYER_ID, loadedObjects.get(0)[1]);
        assertEquals(position, loadedObjects.get(0)[2]);
    }

    @Test
    void goldObjectShotCreatesAPileOfTheShotsAmount() {

        // Create Gold: object 1 (Gold), the second value is the amount of gold
        shotsController.createShot(CREATE_OBJECT_ID, GOLD_OBJECT_ID, 1000, PLAYER_ID, new Vector3f(4.2f, 0f, 7.4f), null, null);

        assertEquals(1, loadedObjects.size());
        Object[] args = loadedObjects.get(0);
        assertEquals(GOLD_OBJECT_ID, args[0]);
        assertEquals(PLAYER_ID, args[1]);
        assertEquals(4, args[2]);
        assertEquals(7, args[3]);
        assertEquals(1000, args[4]);
    }

    @Test
    void notImplementedProcessTypeDoesNothing() {
        shotsController.createShot(SIGHT_OF_EVIL_ID, 5, 0, PLAYER_ID, new Vector3f(), null, null);

        assertEquals(0, countShotEntities());
        assertTrue(loadedObjects.isEmpty());
    }

    @Test
    void creatureFireballFliesAtTheTarget() {
        EntityId target = createCreature();
        EntityId warlock = entityData.createEntity();
        Vector3f start = new Vector3f(1.5f, 1f, 4.5f);

        assertTrue(shotsController.fireShot(ShotFixtures.FIREBALL_ID, 50, 0, PLAYER_ID, start, target, warlock, 4));

        ShotFlight flight = getShotFlight();
        assertEquals(0.9f, flight.velocity.length(), DELTA);
        assertEquals(-0.9f, flight.velocity.z, DELTA);
        assertEquals(warlock, flight.shooter);
        assertEquals(4, flight.maxRange, DELTA);
        assertEquals(60, flight.health);
        assertNull(entityData.getComponent(target, Damage.class));
    }

    @Test
    void creatureLightningDoesntFly() {
        EntityId target = createCreature();

        assertFalse(shotsController.fireShot(ShotFixtures.THUNDERBOLT_ID, 300, 0, PLAYER_ID, new Vector3f(), target,
                entityData.createEntity(), 3));

        assertEquals(0, countShotEntities());
    }

    @Test
    void shotIsAimedAtTheMiddleOfTheTarget() {
        EntityId target = createCreature();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.height = 0.8f;
        entityData.setComponent(target, creatureComponent);

        shotsController.fireShot(ShotFixtures.FIREBALL_ID, 50, 0, PLAYER_ID, new Vector3f(1.5f, 1.4f, 4.5f), target,
                entityData.createEntity(), 4);

        assertEquals(0, getShotFlight().velocity.y, DELTA);
    }

    @Test
    void fireballHitDamagesByTheShotData() {
        EntityId target = createCreature();
        shotsController.fireShot(ShotFixtures.FIREBALL_ID, 50, 0, PLAYER_ID, new Vector3f(1.5f, 1f, 4.5f), target,
                entityData.createEntity(), 4);
        EntityId shot = entityData.findEntity(null, ShotFlight.class);

        shotsController.hit(shot, target);

        assertEquals(50, entityData.getComponent(target, Damage.class).damage);
    }

    @Test
    void fireballSetsABadlyHurtCreatureAlightForTenTicks() {
        EntityId target = createCreature(ENEMY_ID, 10, 10);
        entityData.setComponent(target, new Health(20, 100));
        gameTime = 5;

        hitWithFireball(target);

        assertEquals(5 + 10.0 / TICKS_PER_SECOND, entityData.getComponent(target, OnFire.class).endTime, DELTA);
    }

    @Test
    void fireballDoesNotSetACreatureWithMoreHealthAlight() {
        EntityId target = createCreature(ENEMY_ID, 10, 10);
        entityData.setComponent(target, new Health(21, 100));

        hitWithFireball(target);

        assertNull(entityData.getComponent(target, OnFire.class));
    }

    @Test
    void creatureOnWaterDoesNotCatchFire() {
        EntityId target = createCreature(ENEMY_ID, 10, 10);
        entityData.setComponent(target, new Health(10, 100));
        waterTiles.add(new Point(10, 10));

        hitWithFireball(target);

        assertNull(entityData.getComponent(target, OnFire.class));
        assertEquals(50, entityData.getComponent(target, Damage.class).damage);
    }

    @Test
    void raiseDeadRaisesTheDeadBodiesOnTheTileForTheCaster() {
        EntityId hit = createDeadBody(new Vector3f(2.8f, 1f, 4f));
        EntityId onTheTile = createDeadBody(new Vector3f(3.2f, 1f, 3.9f));
        createDeadBody(new Vector3f(4f, 1f, 4f));
        EntityId living = createCreature();
        entityData.setComponent(living, new Position(0, new Vector3f(3f, 1f, 4f)));
        deadBodies.add(living);
        shotsController.fireShot(ShotFixtures.RAISE_DEAD_ID, SKELETON_ID, 0, PLAYER_ID, new Vector3f(1f, 1f, 4f), hit,
                entityData.createEntity(), 3);
        EntityId shot = entityData.findEntity(null, ShotFlight.class);

        shotsController.hit(shot, hit);

        assertEquals(2, raised.size());
        assertArrayEquals(new Object[]{hit, PLAYER_ID, SKELETON_ID}, raised.get(0));
        assertArrayEquals(new Object[]{onTheTile, PLAYER_ID, SKELETON_ID}, raised.get(1));
        assertEquals(2, effects.size());
        assertEquals(RAISE_DEAD_EFFECT_ID, effects.get(0)[0]);
        assertEquals(new Vector3f(2.8f, 1f, 4f), effects.get(0)[1]);
    }

    private EntityId createDeadBody(Vector3f position) {
        EntityId deadBody = entityData.createEntity();
        entityData.setComponents(deadBody, new Position(0, position), new CreatureComponent(), new Death(0));
        deadBodies.add(deadBody);
        return deadBody;
    }

    @Test
    void shotThatHitsAThingHurtsItsTargetAtOnce() {
        EntityId target = createCreature(ENEMY_ID, 13, 10);
        EntityId bystander = createCreature(ENEMY_ID, 13, 10);

        shotsController.createShot(DOOR_FIREBALL_ID, 150, 0, PLAYER_ID, new Vector3f(10, 1, 10), target, null);
        shotsController.createShot(DOOR_FIREBALL_ID, 150, 0, PLAYER_ID, new Vector3f(10, 1, 10), target, null);
        shotsController.createShot(DOOR_FIREBALL_ID, 150, 0, PLAYER_ID, new Vector3f(10, 1, 10), null, null);

        assertEquals(300, entityData.getComponent(target, Damage.class).damage);
        assertNull(entityData.getComponent(bystander, Damage.class));
    }

    @Test
    void constantDamageShotHurtsEveryoneOnItsTileButTheOwnersCreatures() {
        EntityId target = createCreature(ENEMY_ID, 10, 10);
        EntityId other = createCreature(ENEMY_ID, 10.3f, 9.8f);
        EntityId own = createCreature(PLAYER_ID, 10, 10);
        EntityId nextTile = createCreature(ENEMY_ID, 11, 10);

        // The Spike Trap
        shotsController.createShot(SPIKE_ID, 750, 1, PLAYER_ID, new Vector3f(10, 1, 10), target, null);

        assertEquals(750, entityData.getComponent(target, Damage.class).damage);
        assertEquals(750, entityData.getComponent(other, Damage.class).damage);
        assertNull(entityData.getComponent(own, Damage.class));
        assertNull(entityData.getComponent(nextTile, Damage.class));
    }

    @Test
    void constantDamageShotPassesOverAThingPickedUpThisTick() {
        EntityId target = createCreature(ENEMY_ID, 10, 10);
        // Still in the position lookup, but picked up so it has no position
        EntityId pickedUp = entityData.createEntity();
        entityData.setComponents(pickedUp, new CreatureComponent(), new Owner(ENEMY_ID, ENEMY_ID), new Health(100, 100));
        pickedUpThings.add(pickedUp);

        shotsController.createShot(SPIKE_ID, 750, 1, PLAYER_ID, new Vector3f(10, 1, 10), target, null);

        assertEquals(750, entityData.getComponent(target, Damage.class).damage);
        assertNull(entityData.getComponent(pickedUp, Damage.class));
    }

    @Test
    void damageFromOriginHurtsEveryoneWithinTheRadiusButTheOwnersCreatures() {
        EntityId target = createCreature(ENEMY_ID, 11, 10);
        EntityId neutral = createCreature((short) 2, 10, 9);
        EntityId own = createCreature(PLAYER_ID, 10, 11);
        EntityId diagonal = createCreature(ENEMY_ID, 11, 11);
        EntityId farAway = createCreature(ENEMY_ID, 12, 10);

        // The Gas Trap, there is no lingering cloud yet
        shotsController.createShot(GAS_CLOUD_ID, 750, 1, PLAYER_ID, new Vector3f(10, 1, 10), target, null);

        assertEquals(750, entityData.getComponent(target, Damage.class).damage);
        assertEquals(750, entityData.getComponent(neutral, Damage.class).damage);
        assertNull(entityData.getComponent(own, Damage.class));
        assertNull(entityData.getComponent(diagonal, Damage.class));
        assertNull(entityData.getComponent(farAway, Damage.class));
    }

    @Test
    void freezeShotFreezesItsTarget() {
        EntityId target = createCreature(ENEMY_ID, 10, 10);
        EntityId bystander = createCreature(ENEMY_ID, 10, 10);
        gameTime = 5;

        shotsController.createShot(FREEZE_ID, 20, 1, PLAYER_ID, new Vector3f(10, 1, 10), target, null);

        Frozen frozen = entityData.getComponent(target, Frozen.class);
        assertEquals(5, frozen.startTime);
        assertEquals(20, frozen.duration);
        assertNull(entityData.getComponent(bystander, Frozen.class));

        // Freezing again doesn't prolong the freeze
        gameTime = 10;
        shotsController.createShot(FREEZE_ID, 20, 1, PLAYER_ID, new Vector3f(10, 1, 10), target, null);

        assertEquals(5, entityData.getComponent(target, Frozen.class).startTime);
    }

    @Test
    void hornedReaperDoesNotFreeze() {
        EntityId reaper = createCreature(ENEMY_ID, 10, 10);
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = REAPER_ID;
        entityData.setComponent(reaper, creatureComponent);

        shotsController.createShot(FREEZE_ID, 20, 1, PLAYER_ID, new Vector3f(10, 1, 10), reaper, null);

        assertNull(entityData.getComponent(reaper, Frozen.class));
    }

    @Test
    void alarmShotRaisesAnAlarmForItsOwnerWhereItWasFired() {
        Vector3f position = new Vector3f(10, 1, 10);
        gameTime = 5;

        shotsController.createShot(ALARM_ID, 0, 5, PLAYER_ID, position, createCreature(ENEMY_ID, 12, 10), null);

        EntityId alarm = entityData.findEntity(null, Alarm.class);
        assertEquals(PLAYER_ID, entityData.getComponent(alarm, Owner.class).ownerId);
        assertEquals(position, entityData.getComponent(alarm, Position.class).position);
        assertEquals(5, entityData.getComponent(alarm, Decay.class).startTime);
        assertEquals(50, entityData.getComponent(alarm, Decay.class).duration);
    }

    @Test
    void alarmIsNotRaisedAgainWhileTheOwnersLastAlarmLasts() {
        shotsController.createShot(ALARM_ID, 0, 5, PLAYER_ID, new Vector3f(10, 1, 10), null, null);
        shotsController.createShot(ALARM_ID, 0, 5, PLAYER_ID, new Vector3f(20, 1, 20), null, null);

        assertEquals(1, entityData.findEntities(null, Alarm.class).size());

        // Another keeper's alarm doesn't hold back the alarm
        shotsController.createShot(ALARM_ID, 0, 5, ENEMY_ID, new Vector3f(20, 1, 20), null, null);

        assertEquals(2, entityData.findEntities(null, Alarm.class).size());
    }

    @Test
    void drainGivesTheShooterAQuarterOfItsFullHealthFromTheTarget() {
        EntityId vampire = createCreature();
        entityData.setComponent(vampire, new Health(500, 2000));
        EntityId target = createCreature();
        entityData.setComponent(target, new Health(900, 1000));

        // Drain: 25 % of the Vampire's 2000
        shotsController.hit(createDrainShot(vampire), target);

        assertEquals(500, entityData.getComponent(target, Damage.class).damage);
        assertEquals(-500, entityData.getComponent(vampire, Damage.class).damage);
    }

    @Test
    void drainTakesNoMoreThanTheTargetHasLeft() {
        EntityId vampire = createCreature();
        entityData.setComponent(vampire, new Health(500, 2000));
        EntityId target = createCreature();
        entityData.setComponent(target, new Health(120, 1000));

        shotsController.hit(createDrainShot(vampire), target);

        assertEquals(120, entityData.getComponent(target, Damage.class).damage);
        assertEquals(-120, entityData.getComponent(vampire, Damage.class).damage);
    }

    private EntityId createDrainShot(EntityId shooter) {
        EntityId shot = entityData.createEntity();
        entityData.setComponents(shot, new Position(0, new Vector3f(1.5f, 1f, 1.5f)),
                new ShotFlight(DRAIN_ID, PLAYER_ID, shooter, 25, 0, new Vector3f(), new Vector3f(), 3, 128));
        return shot;
    }

    private EntityId createCreature(short ownerId, float x, float y) {
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature, new Position(0, new Vector3f(x, 1f, y)), new CreatureComponent(),
                new Owner(ownerId, ownerId), new Health(100, 100));
        return creature;
    }

    private void hitWithFireball(EntityId target) {
        shotsController.fireShot(ShotFixtures.FIREBALL_ID, 50, 0, PLAYER_ID, new Vector3f(12, 1f, 10), target,
                entityData.createEntity(), 4);
        shotsController.hit(entityData.findEntity(null, ShotFlight.class), target);
    }

    private ShotFlight getShotFlight() {
        EntitySet shots = entityData.getEntities(ShotFlight.class);
        assertEquals(1, shots.size());
        ShotFlight flight = shots.iterator().next().get(ShotFlight.class);
        shots.release();
        return flight;
    }

    private void addShot(short shotId, Shot.ProcessType processType) {
        shotsById.put(shotId, ShotFixtures.shot(shotId, processType, 0, EnumSet.noneOf(Shot.ShotProcessFlag.class)));
    }

    private EntityId createCreature() {
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature, new Position(0, new Vector3f(1.5f, 1f, 1.5f)), new CreatureComponent());
        return creature;
    }

    private int countShotEntities() {
        EntitySet shots = entityData.getEntities(ShotViewState.class);
        int count = shots.size();
        shots.release();
        return count;
    }

}
