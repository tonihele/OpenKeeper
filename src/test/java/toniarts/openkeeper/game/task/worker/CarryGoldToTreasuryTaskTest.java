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
package toniarts.openkeeper.game.task.worker;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A creature putting gold in a Treasury makes the coins clink only when there was room for some of it, as in
 * DK II (0x466AC0)
 */
class CarryGoldToTreasuryTaskTest {

    private static final short KEEPER = 3;
    private static final Point TILE = new Point(5, 6);

    private final List<String> calls = new ArrayList<>();

    @Test
    void storingGoldClinks() {
        task(200).executeTask(creature(300), 0);

        assertEquals(List.of("substractGold 100", "fireEvent CREATURE_STORE_GOLD " + TILE), calls);
    }

    @Test
    void fullTreasuryStaysQuiet() {
        task(300).executeTask(creature(300), 0);

        assertEquals(List.of("substractGold 0"), calls);
    }

    private CarryGoldToTreasuryTask task(int goldLeft) {
        IGameWorldController gameWorldController = (IGameWorldController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IGameWorldController.class},
                (proxy, method, args) -> method.getName().equals("addGold") ? goldLeft : null);
        return new CarryGoldToTreasuryTask(null, null, TILE, KEEPER, null, gameWorldController);
    }

    private ICreatureController creature(int gold) {
        return (ICreatureController) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ICreatureController.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getGold" ->
                gold;
            case "substractGold" -> {
                calls.add("substractGold " + args[0]);
                yield null;
            }
            case "fireEvent" -> {
                calls.add("fireEvent " + args[0] + " " + args[1]);
                yield null;
            }
            default ->
                null;
        });
    }
}
