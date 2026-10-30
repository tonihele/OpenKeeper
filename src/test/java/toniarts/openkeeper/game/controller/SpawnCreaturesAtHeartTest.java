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

import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.controller.room.ICreatureEntrance;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * A defeated keeper's dungeon heart loses its imp generator, so spawning
 * creatures at it must be skipped like when there is no heart.
 */
class SpawnCreaturesAtHeartTest {

    private static final short PLAYER_ID = 3;

    @Test
    void spawningAtDestroyedHeartIsSkipped() {
        PlayerRoomControl rooms = new PlayerRoomControl(new Keeper(false, PLAYER_ID), List.of());
        rooms.onBuild(stub(ICreatureEntrance.class, (method, args) -> switch (method) {
            case "isDungeonHeart", "isDestroyed" -> true;
            case "getRoomInstance" -> new RoomInstance(new Room());
            case "getRoom" -> null;
            case "hashCode" -> 1;
            case "equals" -> args[0] == null;
            default -> throw new IllegalStateException(method);
        }));
        IPlayerController player = stub(IPlayerController.class, (method, args) -> rooms);
        IGameController game = stub(IGameController.class, (method, args) -> player);
        IKwdFile kwdFile = stub(IKwdFile.class, (method, args) -> List.of());
        CreaturesController controller = new CreaturesController(kwdFile, new DefaultEntityData(), null, null, game, null, null);

        assertDoesNotThrow(() -> controller.spawnCreatures((short) 1, PLAYER_ID, 1, 1));
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, java.util.function.BiFunction<String, Object[], Object> handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> handler.apply(method.getName(), args));
    }
}
