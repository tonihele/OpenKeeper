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
package toniarts.openkeeper.game.task.creature;

import com.simsilica.es.EntityData;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.controller.WorkshopOrders;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.TrapFixtures;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The task manager asks a task's validity without a creature when it decides
 * whether the task can be removed
 */
class ManufactureValidityTest {

    private static final short PLAYER_ID = 3;

    @Test
    void workshopWithSomethingToMakeIsValidWithoutACreature() {
        Manufacture task = manufacture(true);

        assertTrue(task.isValid(null));
        assertFalse(task.isRemovable());
    }

    @Test
    void workshopWithNothingToMakeIsRemovable() {
        Manufacture task = manufacture(false);

        assertTrue(task.isRemovable());
    }

    @Test
    void creatureThatCantManufactureCantTakeTheTask() {
        Manufacture task = manufacture(true);
        ICreatureController imp = stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getManufacturePerSecond" -> 0;
            default -> null;
        });

        assertFalse(task.isValid(imp));
    }

    private static Manufacture manufacture(boolean somethingToMake) {
        IRoomObjectControl workbenches = stub(IRoomObjectControl.class, (name, args) -> switch (name) {
            case "isFullCapacity" -> false;
            default -> null;
        });
        IRoomController workshop = stub(IRoomController.class, (name, args) -> switch (name) {
            case "isRemoved", "isDestroyed" -> false;
            case "getOwnerId" -> PLAYER_ID;
            case "getObjectControl" -> workbenches;
            default -> null;
        });

        // A trap blueprint waiting for its crate is something to make
        Trap trap = TrapFixtures.trapToManufacture((short) 1, Integer.MAX_VALUE);
        EntityData entityData = new DefaultEntityData();
        if (somethingToMake) {
            entityData.setComponents(entityData.createEntity(), new TrapComponent(trap.getTrapId()), new TrapViewState(trap.getTrapId(), true),
                    new Owner(PLAYER_ID, PLAYER_ID));
        }
        IKwdFile kwdFile = stub(IKwdFile.class, (name, args) -> switch (name) {
            case "getTrapById" -> trap;
            default -> null;
        });

        return new Manufacture(null, null, new Point(5, 5), PLAYER_ID, workshop, null, null, new WorkshopOrders(entityData, kwdFile), null);
    }

    private interface Answer {

        Object answer(String methodName, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.answer(method.getName(), args));
    }

}
