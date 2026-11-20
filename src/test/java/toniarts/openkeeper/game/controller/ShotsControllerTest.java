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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.es.filter.FieldFilter;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Alarm;
import toniarts.openkeeper.game.component.Armour;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Decay;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Frozen;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Invisible;
import toniarts.openkeeper.game.component.Invulnerable;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.OnFire;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.ShotFlight;
import toniarts.openkeeper.game.component.ShotViewState;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShotsControllerTest {

    private static final short HEAL_ID = 4;
    private static final short HEAL_INSTANT_ID = 53;
    private static final short ARMOUR_ID = 9;
    private static final short INVULNERABLE_ID = 7;
    private static final short INVISIBLE_ID = 22;
    private static final short CREATE_OBJECT_ID = 2;
    private static final short CREATE_CREATURE_ID = 10;
    private static final short SKELETON_ARMY_ID = 54;
    private static final short IMP_ID = 1;
    private static final short GOLD_OBJECT_ID = 1;
    private static final short SIGHT_OF_EVIL_ID = 33;
    private static final short SPIKE_ID = 27;
    private static final short GAS_CLOUD_ID = 58;
    private static final short FIREBURST_ID = 14;
    private static final short DOOR_ID = 2;
    private static final short TRAP_ID = 1;
    private static final short DOOR_FIREBALL_ID = 57;
    private static final short FREEZE_ID = 45;
    private static final short ALARM_ID = 13;
    private static final short FEAR_ID = 25;
    private static final short REAPER_ID = 2;
    private static final short PLAYER_ID = 3;
    private static final short ENEMY_ID = 4;
    private static final int TICKS_PER_SECOND = 4;
    private static final float DELTA = 0.0001f;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Short, Shot> shotsById = new HashMap<>();
    private final List<Object[]> loadedObjects = new ArrayList<>();
    private final Set<Point> waterTiles = new HashSet<>();
    private final List<Object[]> spawnedCreatures = new ArrayList<>();
    private final List<List<Object>> playerEvents = new ArrayList<>();
    private final ShotsController shotsController;
    private double gameTime = 0;

    ShotsControllerTest() {
        shotsById.put(ShotFixtures.THUNDERBOLT_ID, ShotFixtures.thunderbolt());
        shotsById.put(ShotFixtures.FIREBALL_ID, ShotFixtures.fireball());
        shotsById.put(ShotFixtures.GAS_CLOUD_ID, ShotFixtures.gasCloud(ShotFixtures.GAS_CLOUD_ID));
        shotsById.put(ShotFixtures.GAS_MISSILE_ID, ShotFixtures.gasMissile());
        shotsById.put(ShotFixtures.GAS_MISSILE_CLOUD_ID, ShotFixtures.gasCloud(ShotFixtures.GAS_MISSILE_CLOUD_ID));
        shotsById.put(ShotFixtures.HAIL_STORM_ID, ShotFixtures.hailStorm());
        addShot(HEAL_ID, Shot.ProcessType.MODIFY_HEALTH);
        shotsById.put(HEAL_INSTANT_ID, ShotFixtures.healInstantShot(HEAL_INSTANT_ID, HEAL_ID));
        addShot(ARMOUR_ID, Shot.ProcessType.MODIFY_ARMOUR);
        addShot(INVULNERABLE_ID, Shot.ProcessType.INVUNERABLE);
        addShot(INVISIBLE_ID, Shot.ProcessType.INVISIBLE);
        addShot(CREATE_OBJECT_ID, Shot.ProcessType.CREATE_OBJECT);
        addShot(CREATE_CREATURE_ID, Shot.ProcessType.CREATE_CREATURE);
        shotsById.put(SKELETON_ARMY_ID, ShotFixtures.skeletonArmyShot(SKELETON_ARMY_ID, CREATE_CREATURE_ID));
        addShot(SIGHT_OF_EVIL_ID, Shot.ProcessType.SIGHT_OF_EVIL);
        shotsById.put(SPIKE_ID, TrapFiringFixtures.shot(SPIKE_ID, Shot.ProcessType.NONE, Shot.DamageType.CONSTANT));
        shotsById.put(GAS_CLOUD_ID, TrapFiringFixtures.shot(GAS_CLOUD_ID, Shot.ProcessType.GAS, Shot.DamageType.RANGE_FROM_ORIGIN));
        shotsById.put(FIREBURST_ID, TrapFiringFixtures.shot(FIREBURST_ID, Shot.ProcessType.NONE, Shot.DamageType.RANGE_FROM_ORIGIN));
        shotsById.put(DOOR_FIREBALL_ID, TrapFiringFixtures.shot(DOOR_FIREBALL_ID, Shot.ProcessType.NONE, Shot.DamageType.HIT_THING));
        shotsById.put(FREEZE_ID, TrapFiringFixtures.shot(FREEZE_ID, Shot.ProcessType.FREEZE, Shot.DamageType.NONE));
        shotsById.put(ALARM_ID, TrapFiringFixtures.shot(ALARM_ID, Shot.ProcessType.ALARM, Shot.DamageType.NONE));
        shotsById.put(FEAR_ID, TrapFiringFixtures.shot(FEAR_ID, Shot.ProcessType.FEAR, Shot.DamageType.NONE));
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
        ICreaturesController creaturesController = (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(), new Class<?>[]{ICreaturesController.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("spawnCreature")) {
                        spawnedCreatures.add(args);
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
        GameEventService gameEventService = (GameEventService) Proxy.newProxyInstance(GameEventService.class.getClassLoader(), new Class<?>[]{GameEventService.class}, (proxy, method, args) -> {
            if (method.getName().equals("emitToPlayer")) {
                playerEvents.add(List.of(args));
            }
            return null;
        });

        IEntityPositionLookup entityPositionLookup = (IEntityPositionLookup) Proxy.newProxyInstance(IEntityPositionLookup.class.getClassLoader(), new Class<?>[]{IEntityPositionLookup.class},
                (proxy, method, args) -> entityData.findEntities(null, Position.class).stream()
                        .filter(entity -> new Point((int) args[0], (int) args[1]).equals(WorldUtils.vectorToPoint(entityData.getComponent(entity, Position.class).position)))
                        .toList());
        IGameController gameController = (IGameController) Proxy.newProxyInstance(IGameController.class.getClassLoader(), new Class<?>[]{IGameController.class},
                (proxy, method, args) -> method.getName().equals("getEntityLookupService") ? entityPositionLookup : null);
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
    void creatureHealLandsThroughItsDeathShot() {
        EntityId creature = createCreature();
        EntityId warlock = createCreature();

        // Warlock Heal Creature: an instant shot on own creatures that dies into Modify Health with the spell's value
        shotsController.createShot(HEAL_INSTANT_ID, 3000, 0, PLAYER_ID, new Vector3f(), creature, warlock);

        assertEquals(-3000, entityData.getComponent(creature, Damage.class).damage);
    }

    @Test
    void creatureSkeletonArmyRisesAtItsTarget() {
        EntityId enemy = createCreature();
        EntityId darkAngel = createCreature();

        // Skeleton Army: creature 9 (Skeleton), three of them, through the death shot of an instant shot
        shotsController.createShot(SKELETON_ARMY_ID, 9, 3, PLAYER_ID, new Vector3f(5.5f, 1f, 5.5f), enemy, darkAngel);

        assertEquals(3, spawnedCreatures.size());
        for (Object[] args : spawnedCreatures) {
            assertArrayEquals(new Object[]{(short) 9, PLAYER_ID, 1, new Vector2f(1.5f, 1.5f), ICreaturesController.SpawnType.CONJURE}, args);
        }
        assertNull(entityData.getComponent(enemy, Damage.class));
    }

    @Test
    void skeletonArmyLeavesItsSkeletonsWhereItDies() {
        EntityId enemy = createCreature();
        EntityId darkAngel = createCreature();
        EntityId shot = entityData.createEntity();
        entityData.setComponent(shot, new ShotFlight(SKELETON_ARMY_ID, PLAYER_ID, darkAngel, enemy, 9, 3, new Vector3f(),
                new Vector3f(), 0, 1));

        // Create Creature does its process at its end, as in the original data
        shotsById.put(CREATE_CREATURE_ID, ShotFixtures.shot(CREATE_CREATURE_ID, Shot.ProcessType.CREATE_CREATURE, 1,
                EnumSet.of(Shot.ShotProcessFlag.AT_END)));
        shotsController.createChildShot(shot, CREATE_CREATURE_ID, new Vector3f(2.5f, 1f, 3.5f));

        assertEquals(3, spawnedCreatures.size());
        for (Object[] args : spawnedCreatures) {
            assertArrayEquals(new Object[]{(short) 9, PLAYER_ID, 1, new Vector2f(2.5f, 3.5f), ICreaturesController.SpawnType.CONJURE}, args);
        }
        assertEquals(0, countShotEntities());
    }

    @Test
    void armourShotLastsItsFirstValueInTicks() {
        EntityId creature = createCreature();

        // Cast Armour: 80 ticks, which is 20 seconds at 4 ticks a second
        shotsController.createShot(ARMOUR_ID, 80, 50, PLAYER_ID, new Vector3f(), creature, null);

        assertEquals(20.0, entityData.getComponent(creature, Armour.class).endTime);
    }

    @Test
    void invulnerableShotLastsItsFirstValueInSeconds() {
        EntityId creature = createCreature();

        shotsController.createShot(INVULNERABLE_ID, 5, 0, PLAYER_ID, new Vector3f(), creature, null);

        assertEquals(5.0, entityData.getComponent(creature, Invulnerable.class).endTime);
    }

    @Test
    void invisibleShotLastsItsFirstValueInSeconds() {
        EntityId creature = createCreature();

        shotsController.createShot(INVISIBLE_ID, 15, 0, PLAYER_ID, new Vector3f(), creature, null);

        assertEquals(15.0, entityData.getComponent(creature, Invisible.class).endTime);
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
    void creatureShotCreatesTheShotsNumberOfCreaturesAtLevelOne() {

        // Create Imp: creature 1 (Imp), the second value is how many
        shotsController.createShot(CREATE_CREATURE_ID, IMP_ID, 3, PLAYER_ID, new Vector3f(4.5f, 1f, 7.5f), null, null);

        assertEquals(3, spawnedCreatures.size());
        for (Object[] args : spawnedCreatures) {
            assertEquals(IMP_ID, args[0]);
            assertEquals(PLAYER_ID, args[1]);
            assertEquals(1, args[2]);
            assertEquals(new Vector2f(4.5f, 7.5f), args[3]);
            assertEquals(ICreaturesController.SpawnType.CONJURE, args[4]);
        }
    }

    @Test
    void fearShotDoesNothingItself() {
        EntityId creature = createCreature();

        shotsController.createShot(FEAR_ID, 0, 0, PLAYER_ID, new Vector3f(), creature, null);

        assertNull(entityData.getComponent(creature, Damage.class));
        assertEquals(0, countShotEntities());
        assertTrue(loadedObjects.isEmpty());
        assertTrue(spawnedCreatures.isEmpty());
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
    void fireballHitDoesAnInvulnerableCreatureNoHarm() {
        EntityId target = createCreature();
        entityData.setComponent(target, new Invulnerable(30));
        shotsController.fireShot(ShotFixtures.FIREBALL_ID, 50, 0, PLAYER_ID, new Vector3f(1.5f, 1f, 4.5f), target,
                entityData.createEntity(), 4);
        EntityId shot = entityData.findEntity(null, ShotFlight.class);

        shotsController.hit(shot, target);

        assertNull(entityData.getComponent(target, Damage.class));
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
    void fireballDoesNotSetAnInvulnerableCreatureAlight() {
        EntityId target = createCreature(ENEMY_ID, 10, 10);
        entityData.setComponent(target, new Health(10, 100));
        entityData.setComponent(target, new Invulnerable(30));

        hitWithFireball(target);

        assertNull(entityData.getComponent(target, OnFire.class));
        assertNull(entityData.getComponent(target, Damage.class));
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
    void hitDoesTheShotsProcessOnlyIfTheShotIsMadeTo() {
        EntityId target = createCreature();
        EntityId shot = entityData.createEntity();
        entityData.setComponent(shot, new ShotFlight(HEAL_ID, PLAYER_ID, null, target, 50, 0, new Vector3f(), new Vector3f(), 0, 1));

        shotsController.hit(shot, target);
        assertNull(entityData.getComponent(target, Damage.class));

        // Modify Health heals what it hits, as in the original data
        shotsById.put(HEAL_ID, ShotFixtures.shot(HEAL_ID, Shot.ProcessType.MODIFY_HEALTH, 0,
                EnumSet.of(Shot.ShotProcessFlag.HIT_COLLIDE_THING)));
        shotsController.hit(shot, target);
        assertEquals(-50, entityData.getComponent(target, Damage.class).damage);
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
    void damageFromOriginRunsDownFromThingToThingRowByRow() {
        EntityId neutral = createCreature((short) 2, 10, 9);
        EntityId own = createCreature(PLAYER_ID, 9, 10);
        EntityId target = createCreature(ENEMY_ID, 10, 10);
        EntityId beside = createCreature(ENEMY_ID, 11, 10);
        EntityId edge = createCreature(ENEMY_ID, 10, 12);

        // The Gas Trap, there is no lingering cloud yet. Each thing's damage is worked out from the one before it
        shotsController.createShot(GAS_CLOUD_ID, 800, 2, PLAYER_ID, new Vector3f(10, 1, 10), target, null);

        assertEquals(400, entityData.getComponent(neutral, Damage.class).damage);
        assertNull(entityData.getComponent(own, Damage.class));
        assertEquals(400, entityData.getComponent(target, Damage.class).damage);
        assertEquals(200, entityData.getComponent(beside, Damage.class).damage);
        assertNull(entityData.getComponent(edge, Damage.class));
    }

    @Test
    void damageFromOriginHitsNothingMoreOnceItHasRunDownToNothing() {
        EntityId first = createCreature(ENEMY_ID, 10, 9);
        EntityId target = createCreature(ENEMY_ID, 10, 10);

        shotsController.createShot(GAS_CLOUD_ID, 1, 2, PLAYER_ID, new Vector3f(10, 1, 10), target, null);

        assertNull(entityData.getComponent(first, Damage.class));
        assertNull(entityData.getComponent(target, Damage.class));
    }

    @Test
    void damageFromOriginHurtsTheObjectsDoorsAndTrapsOfOtherPlayers() {
        EntityId object = createThing(ENEMY_ID, new ObjectComponent(GOLD_OBJECT_ID, null));
        EntityId door = createThing(ENEMY_ID, new DoorComponent(DOOR_ID, false, false));
        EntityId trap = createThing(ENEMY_ID, new TrapComponent(TRAP_ID));
        EntityId ownDoor = createThing(PLAYER_ID, new DoorComponent(DOOR_ID, false, false));

        shotsController.createShot(FIREBURST_ID, 800, 1, PLAYER_ID, new Vector3f(10, 1, 10), null, null);

        assertEquals(800, entityData.getComponent(object, Damage.class).damage);
        assertEquals(800, entityData.getComponent(door, Damage.class).damage);
        assertEquals(800, entityData.getComponent(trap, Damage.class).damage);
        assertNull(entityData.getComponent(ownDoor, Damage.class));
    }

    @Test
    void gasSparesDoorsAndTraps() {
        EntityId object = createThing(ENEMY_ID, new ObjectComponent(GOLD_OBJECT_ID, null));
        EntityId door = createThing(ENEMY_ID, new DoorComponent(DOOR_ID, false, false));
        EntityId trap = createThing(ENEMY_ID, new TrapComponent(TRAP_ID));

        shotsController.createShot(ShotFixtures.GAS_CLOUD_ID, 800, 1, PLAYER_ID, new Vector3f(10, 1, 10), null, null);

        assertEquals(800, entityData.getComponent(object, Damage.class).damage);
        assertNull(entityData.getComponent(door, Damage.class));
        assertNull(entityData.getComponent(trap, Damage.class));
    }

    @Test
    void damageFromOriginWithoutARadiusHurtsNobody() {
        EntityId target = createCreature(ENEMY_ID, 10, 10);

        // Like the Firebomb's explosion, its radius is 0 in the data
        shotsController.createShot(GAS_CLOUD_ID, 750, 0, PLAYER_ID, new Vector3f(10, 1, 10), target, null);

        assertNull(entityData.getComponent(target, Damage.class));
    }

    @Test
    void creatureGasCloudFliesAtTheTargetButATrapsDoesNot() {
        EntityId target = createCreature();
        EntityId bileDemon = createCreature(PLAYER_ID, 1.5f, 3.5f);

        assertTrue(shotsController.fireShot(ShotFixtures.GAS_CLOUD_ID, 60, 1, PLAYER_ID, new Vector3f(1.5f, 1f, 3.5f), target,
                bileDemon, 1));

        ShotFlight flight = getShotFlight();
        assertEquals(1f, flight.velocity.length(), DELTA);
        assertEquals(5, flight.health);
        assertNull(entityData.getComponent(target, Damage.class));

        // A trap's shot flies only if it moves towards its target
        assertFalse(shotsController.fireShot(ShotFixtures.GAS_CLOUD_ID, 60, 1, PLAYER_ID, new Vector3f(1.5f, 1f, 3.5f), target,
                entityData.createEntity(), 0));
    }

    @Test
    void creatureHailStormIsAShotThatStandsStillUntilItGoesOntoItsTarget() {
        EntityId target = createCreature();
        EntityId mistress = createCreature(PLAYER_ID, 1.5f, 4.5f);

        assertTrue(shotsController.fireShot(ShotFixtures.HAIL_STORM_ID, 10, 2, PLAYER_ID, new Vector3f(1.5f, 1f, 4.5f), target,
                mistress, 4));

        ShotFlight flight = getShotFlight();
        assertEquals(0, flight.velocity.length(), DELTA);
        assertEquals(target, flight.target);
        assertEquals(10, flight.shotData1);
        assertEquals(2, flight.shotData2);
        assertNull(entityData.getComponent(target, Damage.class));
    }

    @Test
    void childShotStaysWhereItsParentIsWithTheParentsValues() {
        EntityId target = createCreature();
        EntityId bileDemon = createCreature(PLAYER_ID, 1.5f, 4.5f);
        shotsController.fireShot(ShotFixtures.GAS_MISSILE_ID, 100, 1, PLAYER_ID, new Vector3f(1.5f, 1f, 4.5f), target, bileDemon, 4);
        EntityId missile = entityData.findEntity(null, ShotFlight.class);

        shotsController.createChildShot(missile, ShotFixtures.GAS_MISSILE_CLOUD_ID, new Vector3f(1.5f, 1f, 2f));

        EntityId cloud = entityData.findEntity(new FieldFilter<>(ShotFlight.class, "shotId", ShotFixtures.GAS_MISSILE_CLOUD_ID), ShotFlight.class);
        ShotFlight flight = entityData.getComponent(cloud, ShotFlight.class);
        assertEquals(100, flight.shotData1);
        assertEquals(1, flight.shotData2);
        assertEquals(PLAYER_ID, flight.ownerId);
        assertEquals(bileDemon, flight.shooter);
        assertEquals(target, flight.target);
        assertEquals(Vector3f.ZERO, flight.velocity);
        assertEquals(5, flight.health);
        assertEquals(new Vector3f(1.5f, 1f, 2f), entityData.getComponent(cloud, Position.class).position);
        assertNull(entityData.getComponent(target, Damage.class));
    }

    @Test
    void cloudHurtsTheEnemiesAroundItOnEveryTurnTheLessTheFurtherTheyAre() {
        EntityId near = createCreature(ENEMY_ID, 10, 10);
        EntityId halfway = createCreature(ENEMY_ID, 10.5f, 10);
        EntityId own = createCreature(PLAYER_ID, 10, 10);
        EntityId missile = entityData.createEntity();
        entityData.setComponent(missile, new ShotFlight(ShotFixtures.GAS_MISSILE_ID, PLAYER_ID, null, null, 100, 1, new Vector3f(),
                new Vector3f(), 0, 16));
        shotsController.createChildShot(missile, ShotFixtures.GAS_MISSILE_CLOUD_ID, new Vector3f(10, 1, 10));
        EntityId cloud = entityData.findEntity(new FieldFilter<>(ShotFlight.class, "shotId", ShotFixtures.GAS_MISSILE_CLOUD_ID), ShotFlight.class);

        shotsController.damageArea(cloud);
        shotsController.damageArea(cloud);

        assertEquals(200, entityData.getComponent(near, Damage.class).damage);
        assertEquals(100, entityData.getComponent(halfway, Damage.class).damage);
        assertNull(entityData.getComponent(own, Damage.class));
    }

    @Test
    void flyingShotThatHitsAThingDoesNoAreaDamage() {
        EntityId target = createCreature(ENEMY_ID, 1.5f, 1.5f);
        shotsController.fireShot(ShotFixtures.FIREBALL_ID, 50, 0, PLAYER_ID, new Vector3f(1.5f, 1f, 1.5f), target,
                entityData.createEntity(), 4);

        shotsController.damageArea(entityData.findEntity(null, ShotFlight.class));

        assertNull(entityData.getComponent(target, Damage.class));
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
        assertEquals(List.of(List.of(GameEventType.ALARM_RAISED, PLAYER_ID)), playerEvents);
    }

    @Test
    void alarmIsNotRaisedAgainWhileTheOwnersLastAlarmLasts() {
        shotsController.createShot(ALARM_ID, 0, 5, PLAYER_ID, new Vector3f(10, 1, 10), null, null);
        shotsController.createShot(ALARM_ID, 0, 5, PLAYER_ID, new Vector3f(20, 1, 20), null, null);

        assertEquals(1, entityData.findEntities(null, Alarm.class).size());
        assertEquals(1, playerEvents.size());

        // Another keeper's alarm doesn't hold back the alarm
        shotsController.createShot(ALARM_ID, 0, 5, ENEMY_ID, new Vector3f(20, 1, 20), null, null);

        assertEquals(2, entityData.findEntities(null, Alarm.class).size());
        assertEquals(List.of(GameEventType.ALARM_RAISED, ENEMY_ID), playerEvents.get(1));
    }

    private EntityId createCreature(short ownerId, float x, float y) {
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature, new Position(0, new Vector3f(x, 1f, y)), new CreatureComponent(),
                new Owner(ownerId, ownerId), new Health(100, 100));
        return creature;
    }

    private EntityId createThing(short ownerId, EntityComponent component) {
        EntityId thing = entityData.createEntity();
        entityData.setComponents(thing, new Position(0, new Vector3f(10, 1f, 10)), new Owner(ownerId, ownerId),
                new Health(100, 100), component);
        return thing;
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
