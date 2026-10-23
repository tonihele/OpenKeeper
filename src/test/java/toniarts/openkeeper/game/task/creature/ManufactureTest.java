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

import com.jme3.math.Vector2f;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.ITrapsController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerManufactureControl;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.TrapFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManufactureTest {

    private static final int TICKS_PER_SECOND = 4;
    private static final int TROLL = 60;

    @Test
    void creatureWorksOnceASecond() {
        PlayerManufactureControl manufactureControl = new PlayerManufactureControl();
        Trap trap = TrapFixtures.trapToManufacture((short) 1, Integer.MAX_VALUE);
        ITrapsController trapsController = (ITrapsController) Proxy.newProxyInstance(
                ITrapsController.class.getClassLoader(), new Class<?>[]{ITrapsController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getTrapToManufacture" -> trap;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        ICreatureController creature = (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getManufacturePerSecond" -> TROLL;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        Manufacture task = new Manufacture(null, null, new Point(3, 4), (short) 1, null, null, manufactureControl, trapsController, null);
        task.assign(creature, false);

        // A minute of logic ticks
        float taskDuration = 0;
        for (int tick = 0; tick < 60 * TICKS_PER_SECOND; tick++) {
            taskDuration += 1.0f / TICKS_PER_SECOND;
            task.executeTask(creature, taskDuration);
        }

        assertEquals(60 * TROLL, manufactureControl.getProgress());
    }

    @Test
    void creatureStandsHalfwayToTheWorkbench() {
        Vector2f workPosition = Manufacture.getWorkPosition(new Vector2f(4, 5), new Point(5, 5));

        assertEquals(new Vector2f(4.5f, 5), workPosition);
    }

    @Test
    void taskManagerChecksTheTaskWithoutACreature() {
        assertFalse(manufactureInWorkshop(false).isRemovable());
        assertTrue(manufactureInWorkshop(true).isRemovable());
    }

    private static Manufacture manufactureInWorkshop(boolean full) {
        IRoomObjectControl<?> crates = (IRoomObjectControl<?>) Proxy.newProxyInstance(
                IRoomObjectControl.class.getClassLoader(), new Class<?>[]{IRoomObjectControl.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isFullCapacity" -> full;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        IRoomController workshop = (IRoomController) Proxy.newProxyInstance(
                IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isRemoved", "isDestroyed" -> false;
                    case "getOwnerId" -> (short) 1;
                    case "getObjectControl" -> crates;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        Trap trap = TrapFixtures.trapToManufacture((short) 1, Integer.MAX_VALUE);
        ITrapsController trapsController = (ITrapsController) Proxy.newProxyInstance(
                ITrapsController.class.getClassLoader(), new Class<?>[]{ITrapsController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getTrapToManufacture" -> trap;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return new Manufacture(null, null, new Point(3, 4), (short) 1, workshop, null, new PlayerManufactureControl(), trapsController, null);
    }

}
