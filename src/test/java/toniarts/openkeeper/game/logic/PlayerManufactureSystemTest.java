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
package toniarts.openkeeper.game.logic;

import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Crate;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.ManufactureOrders;
import toniarts.openkeeper.game.controller.player.PlayerManufactureControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.DoorFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.TrapFixtures;
import toniarts.openkeeper.tools.convert.map.Variable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlayerManufactureSystemTest {

    private static final short PLAYER = 3;
    private static final short ENEMY = 4;
    private static final short SENTRY = 1;
    private static final short BOULDER = 2;
    private static final short WOODEN_DOOR = 1;
    private static final float TICK = 0.25f;

    private final DefaultEntityData entityData = new DefaultEntityData();
    private final PlayerManufactureControl manufactureControl = new PlayerManufactureControl();
    private double gameTime = 0;

    @Test
    void oldestBlueprintWithoutACrateIsNext() {
        addBlueprint(SENTRY, PLAYER);
        addBlueprint(BOULDER, PLAYER);
        PlayerManufactureSystem system = createSystem();

        assertEquals(SENTRY, getTrapToManufacture());

        addCrate(ResearchableType.TRAP, SENTRY, PLAYER);
        system.processTick(0);
        assertEquals(BOULDER, getTrapToManufacture());

        addCrate(ResearchableType.TRAP, BOULDER, PLAYER);
        system.processTick(0);
        assertNull(manufactureControl.getOrder());
    }

    @Test
    void armedTrapIsNoOrder() {
        EntityId blueprint = addBlueprint(SENTRY, PLAYER);
        PlayerManufactureSystem system = createSystem();

        entityData.setComponent(blueprint, new TrapViewState(SENTRY, false));
        system.processTick(0);

        assertNull(manufactureControl.getOrder());
    }

    @Test
    void removedCrateIsManufacturedAgain() {
        addBlueprint(SENTRY, PLAYER);
        EntityId crate = addCrate(ResearchableType.TRAP, SENTRY, PLAYER);
        PlayerManufactureSystem system = createSystem();
        assertNull(manufactureControl.getOrder());

        entityData.removeEntity(crate);
        system.processTick(0);

        assertEquals(SENTRY, getTrapToManufacture());
    }

    @Test
    void onlyTheKeepersOwnTrapCratesCount() {
        addBlueprint(BOULDER, ENEMY);
        addBlueprint(SENTRY, PLAYER);
        addCrate(ResearchableType.TRAP, SENTRY, ENEMY);
        addCrate(ResearchableType.DOOR, SENTRY, PLAYER);
        createSystem();

        assertEquals(SENTRY, getTrapToManufacture());
    }

    @Test
    void doorBlueprintIsAnOrder() {
        addDoorBlueprint(PLAYER);
        createSystem();

        ManufactureOrders.Order order = manufactureControl.getOrder();

        assertEquals(ResearchableType.DOOR, order.type());
        assertEquals(WOODEN_DOOR, order.typeId());
        assertEquals(1000, order.manufToBuild());
        assertEquals(136, order.crateObjectId());
    }

    @Test
    void trapsAndDoorsAreManufacturedInTheOrderTheyWerePlaced() {
        addDoorBlueprint(PLAYER);
        addBlueprint(SENTRY, PLAYER);
        PlayerManufactureSystem system = createSystem();

        assertEquals(ResearchableType.DOOR, manufactureControl.getOrder().type());

        // The door has its crate, next is the trap
        addCrate(ResearchableType.DOOR, WOODEN_DOOR, PLAYER);
        system.processTick(0);

        assertEquals(SENTRY, getTrapToManufacture());
    }

    @Test
    void trapCrateDoesntFulfillADoorOrder() {
        addDoorBlueprint(PLAYER);
        addCrate(ResearchableType.TRAP, WOODEN_DOOR, PLAYER);
        createSystem();

        assertEquals(ResearchableType.DOOR, manufactureControl.getOrder().type());
    }

    @Test
    void builtDoorIsNoOrder() {
        EntityId door = addDoorBlueprint(PLAYER);
        PlayerManufactureSystem system = createSystem();

        entityData.setComponent(door, new DoorComponent(WOODEN_DOOR, false, false));
        system.processTick(0);

        assertNull(manufactureControl.getOrder());
    }

    @Test
    void doorBlueprintsOfOtherKeepersAreNoOrders() {
        addDoorBlueprint(ENEMY);
        createSystem();

        assertNull(manufactureControl.getOrder());
    }

    @Test
    void pointsDropOnceASecondWhileNobodyManufactures() {
        PlayerManufactureSystem system = createSystem();
        manufactureControl.manufacture(10, 1500);

        runSeconds(system, 3);
        assertEquals(7, manufactureControl.getProgress());

        runSeconds(system, 10);
        assertEquals(0, manufactureControl.getProgress());
    }

    @Test
    void pointsStayWhileACreatureManufactures() {
        PlayerManufactureSystem system = createSystem();
        manufactureControl.manufacture(10, 1500);
        EntityId worker = addCreature(TaskType.MANUFACTURE, true, PLAYER);

        runSeconds(system, 3);
        assertEquals(10, manufactureControl.getProgress());

        entityData.removeComponent(worker, TaskComponent.class);
        runSeconds(system, 3);
        assertEquals(7, manufactureControl.getProgress());
    }

    @Test
    void creaturesOnTheWayOrOfOtherKeepersDontStopTheLoss() {
        PlayerManufactureSystem system = createSystem();
        manufactureControl.manufacture(10, 1500);
        addCreature(TaskType.MANUFACTURE, false, PLAYER);
        addCreature(TaskType.RESEARCH, true, PLAYER);
        addCreature(TaskType.MANUFACTURE, true, ENEMY);

        runSeconds(system, 3);

        assertEquals(7, manufactureControl.getProgress());
    }

    private void runSeconds(PlayerManufactureSystem system, int seconds) {
        for (int i = 0; i < seconds / TICK; i++) {
            gameTime += TICK;
            system.processTick(TICK);
        }
    }

    private PlayerManufactureSystem createSystem() {
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getTrapById" -> TrapFixtures.trap(((Integer) args[0]).shortValue(), 0);
                    case "getDoorById" -> DoorFixtures.doorToManufacture(((Integer) args[0]).shortValue(), 1000, (short) 136);
                    case "getVariables" -> Map.of(Variable.MiscVariable.MiscType.MANUFACTURE_POINT_LOSS_WHILE_NO_WORKERS_PER_SECOND,
                            DoorFixtures.miscVariable(Variable.MiscVariable.MiscType.MANUFACTURE_POINT_LOSS_WHILE_NO_WORKERS_PER_SECOND, 1));
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        Keeper keeper = new Keeper(false, PLAYER);
        IPlayerController player = (IPlayerController) Proxy.newProxyInstance(IPlayerController.class.getClassLoader(),
                new Class<?>[]{IPlayerController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getKeeper" -> keeper;
                    case "getManufactureControl" -> manufactureControl;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return new PlayerManufactureSystem(entityData, kwdFile, List.of(player), () -> gameTime);
    }

    /**
     * @return the trap to manufacture next, -1 if the next order is none or a
     * door
     */
    private short getTrapToManufacture() {
        ManufactureOrders.Order order = manufactureControl.getOrder();
        return order != null && order.type() == ResearchableType.TRAP ? order.typeId() : -1;
    }

    private EntityId addBlueprint(short trapId, short ownerId) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new TrapComponent(trapId), new TrapViewState(trapId, true), new Owner(ownerId, ownerId));
        return entity;
    }

    private EntityId addDoorBlueprint(short ownerId) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new DoorComponent(WOODEN_DOOR, false, true), new Owner(ownerId, ownerId));
        return entity;
    }

    private EntityId addCreature(TaskType taskType, boolean taskStarted, short ownerId) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new TaskComponent(1, null, null, taskType, 0, taskStarted), new Owner(ownerId, ownerId));
        return entity;
    }

    private EntityId addCrate(ResearchableType type, short typeId, short ownerId) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new Crate(type, typeId), new Owner(ownerId, ownerId));
        return entity;
    }

}
