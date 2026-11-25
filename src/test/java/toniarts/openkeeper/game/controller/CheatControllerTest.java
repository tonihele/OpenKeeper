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
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.player.PlayerDoorControl;
import toniarts.openkeeper.game.controller.player.PlayerTrapControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.state.CheatState;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.DoorFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.TrapFixtures;

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

        new CheatController(null, gameController, new com.simsilica.es.base.DefaultEntityData(), null).addGold(250, PLAYER_ID);

        assertEquals(1, addedGold.size());
        assertEquals(PLAYER_ID, addedGold.get(0)[0]);
        assertEquals(250, addedGold.get(0)[1]);
    }

    @Test
    void unlockingDoorsAndTrapsLeavesOutTheHeroes() {
        List<Door> doors = List.of(DoorFixtures.door((short) 1, 0), DoorFixtures.door((short) 7, 0, Door.DoorFlag.IS_GOOD));
        List<Trap> traps = List.of(TrapFixtures.trapWithFlags((short) 3), TrapFixtures.trapWithFlags((short) 14, Trap.TrapFlag.IS_GOOD));
        Keeper keeper = new Keeper(false, PLAYER_ID);
        PlayerDoorControl doorControl = new PlayerDoorControl(keeper, doors);
        PlayerTrapControl trapControl = new PlayerTrapControl(keeper, traps);
        IKwdFile kwdFile = stub(IKwdFile.class, name -> switch (name) {
            case "getDoors" -> doors;
            case "getTraps" -> traps;
            default -> throw new UnsupportedOperationException(name);
        });
        IPlayerController player = stub(IPlayerController.class, name -> switch (name) {
            case "getDoorControl" -> doorControl;
            case "getTrapControl" -> trapControl;
            default -> throw new UnsupportedOperationException(name);
        });
        IGameWorldController gameWorldController = stub(IGameWorldController.class, name -> null);
        IGameController gameController = stub(IGameController.class, name -> switch (name) {
            case "getGameWorldController" -> gameWorldController;
            case "getPlayerController" -> player;
            default -> throw new UnsupportedOperationException(name);
        });

        new CheatController(kwdFile, gameController, new com.simsilica.es.base.DefaultEntityData(), null)
                .onCheat(CheatState.CheatType.UNLOCK_DOORS_TRAPS, PLAYER_ID);

        // The Hero Barricade and the Hero Sentry Trap have no text, their Workshop icons said "Jelly"
        assertEquals(List.of((short) 1), keeper.getAvailableDoors().stream().map(ResearchableEntity::getId).toList());
        assertEquals(List.of((short) 3), keeper.getAvailableTraps().stream().map(ResearchableEntity::getId).toList());
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Function<String, Object> answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.apply(method.getName()));
    }

}
