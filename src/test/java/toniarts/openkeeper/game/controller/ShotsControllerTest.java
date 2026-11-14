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
import toniarts.openkeeper.game.component.Armour;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Invisible;
import toniarts.openkeeper.game.component.Invulnerable;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.ShotViewState;
import toniarts.openkeeper.game.component.SpeedChange;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShotsControllerTest {

    private static final short HEAL_ID = 1;
    private static final short HEAL_INSTANT_ID = 53;
    private static final short ARMOUR_ID = 9;
    private static final short SPEED_ID = 6;
    private static final short INVULNERABLE_ID = 7;
    private static final short INVISIBLE_ID = 22;
    private static final short CREATE_OBJECT_ID = 2;
    private static final short GOLD_OBJECT_ID = 1;
    private static final short SIGHT_OF_EVIL_ID = 33;
    private static final short PLAYER_ID = 3;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Short, Shot> shotsById = new HashMap<>();
    private final List<Object[]> loadedObjects = new ArrayList<>();
    private final ShotsController shotsController;

    ShotsControllerTest() {
        shotsById.put(ShotFixtures.THUNDERBOLT_ID, ShotFixtures.thunderbolt());
        addShot(HEAL_ID, Shot.ProcessType.MODIFY_HEALTH);
        shotsById.put(HEAL_INSTANT_ID, ShotFixtures.healInstantShot(HEAL_INSTANT_ID, HEAL_ID));
        addShot(ARMOUR_ID, Shot.ProcessType.MODIFY_ARMOUR);
        addShot(SPEED_ID, Shot.ProcessType.MODIFY_SPEED);
        addShot(INVULNERABLE_ID, Shot.ProcessType.INVUNERABLE);
        addShot(INVISIBLE_ID, Shot.ProcessType.INVISIBLE);
        addShot(CREATE_OBJECT_ID, Shot.ProcessType.CREATE_OBJECT);
        addShot(SIGHT_OF_EVIL_ID, Shot.ProcessType.SIGHT_OF_EVIL);
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getShotById" -> shotsById.get((Short) args[0]);
                    case "getGameLevel" -> CreatureSleepFixtures.level(4);
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

        // A map without tiles, no strike lands on water
        IMapDataInformation<?> mapData = (IMapDataInformation<?>) Proxy.newProxyInstance(IMapDataInformation.class.getClassLoader(), new Class<?>[]{IMapDataInformation.class},
                (proxy, method, args) -> null);
        IMapController mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(), new Class<?>[]{IMapController.class},
                (proxy, method, args) -> method.getName().equals("getMapData") ? mapData : null);
        GameEventService gameEventService = (GameEventService) Proxy.newProxyInstance(GameEventService.class.getClassLoader(), new Class<?>[]{GameEventService.class}, (proxy, method, args) -> null);

        shotsController = new ShotsController(kwdFile, entityData, null, () -> 0.0, null, mapController, null, objectsController, null, gameEventService);
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
    void hasteSpeedsTheCreatureUpForItsSecondValueInSeconds() {
        EntityId creature = createCreature(1f);

        // Haste Creature: 150 % for 15 seconds
        shotsController.createShot(SPEED_ID, 150, 15, PLAYER_ID, new Vector3f(), creature, null);

        assertEquals(1.5f, entityData.getComponent(creature, Mobile.class).maxSpeed);
        assertEquals(15.0, entityData.getComponent(creature, SpeedChange.class).endTime);

        // A speed change that is still running is left as it is
        shotsController.createShot(SPEED_ID, 20, 30, PLAYER_ID, new Vector3f(), creature, null);

        assertEquals(1.5f, entityData.getComponent(creature, Mobile.class).maxSpeed);
        assertEquals(15.0, entityData.getComponent(creature, SpeedChange.class).endTime);
    }

    @Test
    void speedStaysBetweenAQuarterAndATileATick() {
        EntityId slowed = createCreature(1f);
        EntityId hasted = createCreature(3f);

        // Slow: 20 % for 15 seconds
        shotsController.createShot(SPEED_ID, 20, 15, PLAYER_ID, new Vector3f(), slowed, null);
        shotsController.createShot(SPEED_ID, 150, 15, PLAYER_ID, new Vector3f(), hasted, null);

        assertEquals(0.25f, entityData.getComponent(slowed, Mobile.class).maxSpeed);
        assertEquals(4f, entityData.getComponent(hasted, Mobile.class).maxSpeed);
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

    private EntityId createCreature(float speed) {
        EntityId creature = createCreature();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.speed = speed;
        entityData.setComponents(creature, creatureComponent, new Mobile(false, false, false, speed));
        return creature;
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
