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
import com.simsilica.es.EntityData;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.controller.WorkshopOrders;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerManufactureControl;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.TrapFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManufactureTest {

    private static final int TICKS_PER_SECOND = 4;
    private static final int TROLL = 60;

    @Test
    void creatureWorksOnceASecond() {
        PlayerManufactureControl manufactureControl = new PlayerManufactureControl();
        Trap trap = TrapFixtures.trapToManufacture((short) 1, Integer.MAX_VALUE);
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(
                IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getTrapById" -> trap;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        EntityData entityData = new DefaultEntityData();
        entityData.setComponents(entityData.createEntity(), new TrapComponent(trap.getTrapId()), new TrapViewState(trap.getTrapId(), true),
                new Owner((short) 1, (short) 1));
        WorkshopOrders workshopOrders = new WorkshopOrders(entityData, kwdFile);
        ICreatureController creature = (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getManufacturePerSecond" -> TROLL;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        Manufacture task = new Manufacture(null, null, new Point(3, 4), (short) 1, null, null, manufactureControl, workshopOrders, null);
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

}
