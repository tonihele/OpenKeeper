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
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.ShotViewState;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.GameObjectFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShotsControllerTest {

    private static final short HEAL_ID = 1;
    private static final short HEAL_INSTANT_ID = 53;
    private static final short CREATE_OBJECT_ID = 2;
    private static final short CREATE_CREATURE_ID = 4;
    private static final short SKELETON_ARMY_ID = 54;
    private static final short GOLD_OBJECT_ID = 1;
    private static final short SIGHT_OF_EVIL_ID = 33;
    private static final short PLAYER_ID = 3;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Short, Shot> shotsById = new HashMap<>();
    private final List<Object[]> loadedObjects = new ArrayList<>();
    private final List<Object[]> spawnedCreatures = new ArrayList<>();
    private final ShotsController shotsController;

    ShotsControllerTest() {
        shotsById.put(ShotFixtures.THUNDERBOLT_ID, ShotFixtures.thunderbolt());
        addShot(HEAL_ID, Shot.ProcessType.MODIFY_HEALTH);
        shotsById.put(HEAL_INSTANT_ID, ShotFixtures.healInstantShot(HEAL_INSTANT_ID, HEAL_ID));
        addShot(CREATE_OBJECT_ID, Shot.ProcessType.CREATE_OBJECT);
        addShot(CREATE_CREATURE_ID, Shot.ProcessType.CREATE_CREATURE);
        shotsById.put(SKELETON_ARMY_ID, ShotFixtures.skeletonArmyShot(SKELETON_ARMY_ID, CREATE_CREATURE_ID));
        addShot(SIGHT_OF_EVIL_ID, Shot.ProcessType.SIGHT_OF_EVIL);
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getShotById" -> shotsById.get((Short) args[0]);
                    case "getObject" -> (int) args[0] == GOLD_OBJECT_ID ? GameObjectFixtures.gameObject("Gold", GameObject.ObjectFlag.OBJECT_TYPE_GOLD) : null;
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

        // A map without tiles, no strike lands on water
        IMapDataInformation<?> mapData = (IMapDataInformation<?>) Proxy.newProxyInstance(IMapDataInformation.class.getClassLoader(), new Class<?>[]{IMapDataInformation.class},
                (proxy, method, args) -> null);
        IMapController mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(), new Class<?>[]{IMapController.class},
                (proxy, method, args) -> method.getName().equals("getMapData") ? mapData : null);
        GameEventService gameEventService = (GameEventService) Proxy.newProxyInstance(GameEventService.class.getClassLoader(), new Class<?>[]{GameEventService.class}, (proxy, method, args) -> null);

        shotsController = new ShotsController(kwdFile, entityData, null, () -> 0.0, null, mapController, null, objectsController, creaturesController, gameEventService);
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
    void creatureShotCreatesItsSecondValueOfCreatures() {

        // Create Imp: creature 1 (Imp), one of them
        shotsController.createShot(CREATE_CREATURE_ID, 1, 1, PLAYER_ID, new Vector3f(4.5f, 1f, 7.5f), null, null);

        assertEquals(1, spawnedCreatures.size());
        assertArrayEquals(new Object[]{(short) 1, PLAYER_ID, 1, new Vector2f(4.5f, 7.5f), ICreaturesController.SpawnType.CONJURE}, spawnedCreatures.get(0));
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
