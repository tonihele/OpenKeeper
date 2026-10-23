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
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerManaControl;
import toniarts.openkeeper.game.controller.room.TempleController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.TempleFixtures;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.tools.convert.map.Variable.SacrificeRewardType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SacrificeControllerTest {

    private static final short OWNER = 3;
    private static final short IMP = 1;
    private static final short MISTRESS = 4;
    private static final short DARK_ELF = 6;
    private static final short SKELETON = 9;
    private static final short TROLL = 10;
    private static final short CRATE_ALARM = 5;

    private EntityData entityData;
    private TempleController temple;
    private Keeper keeper;
    private final List<Object[]> spawnedCreatures = new ArrayList<>();
    private final List<Object[]> loadedObjects = new ArrayList<>();
    private SacrificeController sacrificeController;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        RoomInstance roomInstance = new RoomInstance(TempleFixtures.temple());
        roomInstance.setOwnerId(OWNER);
        for (int x = 10; x < 15; x++) {
            for (int y = 20; y < 25; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(
                IObjectsController.class.getClassLoader(), new Class<?>[]{IObjectsController.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "loadObject" -> {
                        loadedObjects.add(args);
                        yield entityData.createEntity();
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        temple = new TempleController(entityData.createEntity(), entityData, null, roomInstance, objectsController, () -> 0);
        temple.construct();
        loadedObjects.clear();

        Map<MiscType, MiscVariable> gameSettings = new EnumMap<>(MiscType.class);
        gameSettings.put(MiscType.MAXIMUM_MANA_THRESHOLD, TempleFixtures.miscVariable(MiscType.MAXIMUM_MANA_THRESHOLD, 200000));
        keeper = new Keeper(false, OWNER);
        PlayerManaControl manaControl = new PlayerManaControl(keeper, gameSettings);
        IPlayerController playerController = (IPlayerController) Proxy.newProxyInstance(
                IPlayerController.class.getClassLoader(), new Class<?>[]{IPlayerController.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getManaControl" -> manaControl;
                    default -> throw new UnsupportedOperationException(method.getName());
                });

        List<Variable.Sacrifice> recipes = List.of(
                TempleFixtures.creatureSacrifice(SacrificeRewardType.OBJECT, CRATE_ALARM, IMP, DARK_ELF),
                TempleFixtures.creatureSacrifice(SacrificeRewardType.CREATURE, SKELETON, MISTRESS, MISTRESS),
                TempleFixtures.creatureSacrifice(SacrificeRewardType.MANA, 5000, TROLL, TROLL));
        sacrificeController = new SacrificeController(entityData, recipes, creaturesController(), objectsController,
                null, Map.of(OWNER, playerController));
    }

    @Test
    void theSacrificedCreatureIsGone() {
        EntityId creature = creature(TROLL, 1);

        sacrificeController.sacrificeCreature(temple, creature);

        assertNull(entityData.getComponent(creature, CreatureComponent.class));
    }

    @Test
    void anUnmatchedSacrificeGivesNothing() {
        assertNull(sacrificeController.sacrificeCreature(temple, creature(IMP, 1)));

        assertTrue(spawnedCreatures.isEmpty());
        assertTrue(loadedObjects.isEmpty());
        assertEquals(0, keeper.getMana());
    }

    @Test
    void aCreatureRewardJoinsTheTempleOwnerOnTheTempleFloor() {
        sacrificeController.sacrificeCreature(temple, creature(MISTRESS, 2));
        sacrificeController.sacrificeCreature(temple, creature(MISTRESS, 7));

        assertEquals(1, spawnedCreatures.size());
        Object[] spawn = spawnedCreatures.get(0);
        assertEquals(SKELETON, spawn[0]);
        assertEquals(OWNER, spawn[1]);
        assertEquals(4, spawn[2]);
        assertOnTempleFloor(WorldUtils.vectorToPoint((Vector2f) spawn[3]));
    }

    @Test
    void anObjectRewardAppearsOnTheTempleFloor() {
        sacrificeController.sacrificeCreature(temple, creature(IMP, 1));
        sacrificeController.sacrificeCreature(temple, creature(DARK_ELF, 1));

        assertEquals(1, loadedObjects.size());
        Object[] load = loadedObjects.get(0);
        assertEquals(CRATE_ALARM, load[0]);
        assertEquals(OWNER, load[1]);
        assertOnTempleFloor(new Point((int) load[2], (int) load[3]));
    }

    @Test
    void aManaRewardGoesToTheTempleOwner() {
        sacrificeController.sacrificeCreature(temple, creature(TROLL, 1));
        sacrificeController.sacrificeCreature(temple, creature(TROLL, 1));

        assertEquals(5000, keeper.getMana());
    }

    private void assertOnTempleFloor(Point p) {
        assertTrue(temple.getRoomInstance().getCoordinates().contains(p), p + " is not in the temple");
        assertFalse(temple.isSacrificePool(p), p + " is in the pool");
    }

    private EntityId creature(short creatureId, int level) {
        EntityId entity = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        entityData.setComponent(entity, creatureComponent);
        CreatureExperience experience = new CreatureExperience();
        experience.level = level;
        entityData.setComponent(entity, experience);
        return entity;
    }

    private ICreaturesController creaturesController() {
        return (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "createController" -> creatureController((EntityId) args[0]);
                    case "spawnCreature" -> {
                        spawnedCreatures.add(args);
                        yield entityData.createEntity();
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private ICreatureController creatureController(EntityId entityId) {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityId" -> entityId;
                    case "remove" -> {
                        entityData.removeEntity(entityId);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

}
