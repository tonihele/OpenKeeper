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

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CheatControllerTest {

    private static final short PLAYER_ID = 3;

    private final List<int[]> addedGold = new ArrayList<>();

    @Test
    void addGoldAddsTheGivenAmountToThePlayer() {
        IGameWorldController gameWorldController = (IGameWorldController) Proxy.newProxyInstance(
                IGameWorldController.class.getClassLoader(), new Class<?>[]{IGameWorldController.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getMapController":
                            return null;
                        case "addGold":
                            addedGold.add(new int[]{(Short) args[0], (Integer) args[1]});
                            return (Integer) args[1];
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
        IGameController gameController = (IGameController) Proxy.newProxyInstance(
                IGameController.class.getClassLoader(), new Class<?>[]{IGameController.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getGameWorldController")) {
                        return gameWorldController;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

        new CheatController(null, gameController).addGold(250, PLAYER_ID);

        assertEquals(1, addedGold.size());
        assertEquals(PLAYER_ID, addedGold.get(0)[0]);
        assertEquals(250, addedGold.get(0)[1]);
    }

}
