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
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.tools.convert.map.DestroyRoomsFixtures;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The keeper hears about his rooms under attack and lost. DKII.EXE raises
 * message 0x22 when a blow leaves the room standing, 0x24 when the keeper
 * lost his last room of the type and 0x25 when he lost one of them (0x497DF0).
 * The message queue (0x4C4600) doesn't repeat a message of the same type for
 * the same room type while the last one is shown: 30 seconds for an attack, 20
 * for a lost room (table at 0x6A0B00)
 */
class RoomAttackMessagesTest {

    private static final Point TILE = new Point(4, 5);
    private static final short KEEPER = Player.KEEPER1_ID;

    private final List<String> messages = new ArrayList<>();
    private double gameTime = 100;
    private final PlayerService playerService = (PlayerService) Proxy.newProxyInstance(PlayerService.class.getClassLoader(),
            new Class<?>[]{PlayerService.class}, (proxy, method, args) -> {
                if (method.getName().equals("showMentorMessage")) {
                    messages.add(args[0] + "@" + args[1] + ">p" + args[2]);
                }
                return null;
            });
    private final IGameTimer gameTimer = (IGameTimer) Proxy.newProxyInstance(IGameTimer.class.getClassLoader(),
            new Class<?>[]{IGameTimer.class}, (proxy, method, args) -> method.getName().equals("getGameTime") ? gameTime : null);
    private final RoomAttackMessages roomAttackMessages = new RoomAttackMessages(playerService, gameTimer);
    private final Room lair = room("Lair", 2);
    private final Room treasury = room("Treasury", 1);

    @Test
    void tellsTheKeeperWhichRoomIsUnderAttack() {
        roomAttackMessages.roomTileAttacked(KEEPER, lair, TILE);
        roomAttackMessages.roomTileAttacked(KEEPER, treasury, TILE);

        // "Your Lair is under attack", "Your Treasury is under attack"
        assertEquals(List.of(message(158), message(157)), messages);
    }

    @Test
    void roomsWithoutAMessageOfTheirOwnGetTheGeneralOne() {
        roomAttackMessages.roomTileAttacked(KEEPER, room("Casino", 99), TILE);

        // "Your room is under attack"
        assertEquals(List.of(message(172)), messages);
    }

    @Test
    void attackOnTheSameRoomTypeIsToldOnceIn30Seconds() {
        roomAttackMessages.roomTileAttacked(KEEPER, lair, TILE);
        gameTime += 29;
        roomAttackMessages.roomTileAttacked(KEEPER, lair, TILE);
        assertEquals(1, messages.size());

        gameTime += 2;
        roomAttackMessages.roomTileAttacked(KEEPER, lair, TILE);
        assertEquals(2, messages.size());
    }

    @Test
    void eachKeeperHearsOfHisOwnRooms() {
        roomAttackMessages.roomTileAttacked(KEEPER, lair, TILE);
        roomAttackMessages.roomTileAttacked(Player.KEEPER2_ID, lair, TILE);

        assertEquals(List.of(message(158), message(158, Player.KEEPER2_ID)), messages);
    }

    @Test
    void losingTheLastRoomOfTheType() {
        roomAttackMessages.roomLost(KEEPER, lair, 1, 0, TILE);

        // "Your Lair has been destroyed"
        assertEquals(List.of(message(96)), messages);
    }

    @Test
    void losingOneOfTheRoomsOfTheType() {
        roomAttackMessages.roomLost(KEEPER, lair, 2, 1, TILE);

        // "One of your Lairs has been destroyed"
        assertEquals(List.of(message(111)), messages);
    }

    @Test
    void losingATileOfARoomThatStandsIsNotTold() {
        roomAttackMessages.roomLost(KEEPER, lair, 1, 1, TILE);

        // A room split in two by the lost tile is not a lost room either
        roomAttackMessages.roomLost(KEEPER, lair, 1, 2, TILE);

        assertTrue(messages.isEmpty());
    }

    @Test
    void lostRoomsAreToldOnceIn20Seconds() {
        roomAttackMessages.roomLost(KEEPER, lair, 3, 2, TILE);
        gameTime += 19;
        roomAttackMessages.roomLost(KEEPER, lair, 2, 1, TILE);
        roomAttackMessages.roomLost(KEEPER, lair, 1, 0, TILE);
        assertEquals(List.of(message(111), message(96)), messages);

        gameTime += 2;
        roomAttackMessages.roomLost(KEEPER, lair, 2, 1, TILE);
        assertEquals(List.of(message(111), message(96), message(111)), messages);
    }

    @Test
    void worksWithoutPlayers() {
        RoomAttackMessages withoutPlayers = new RoomAttackMessages(null, gameTimer);

        withoutPlayers.roomTileAttacked(KEEPER, lair, TILE);
        withoutPlayers.roomLost(KEEPER, lair, 1, 0, TILE);

        assertTrue(messages.isEmpty());
    }

    private static Room room(String name, int roomId) {
        Room room = DestroyRoomsFixtures.destroyRoomsRoom(name, (short) 0, Room.RoomFlag.BUILDABLE);
        DestroyRoomsFixtures.setRoomId(room, (short) roomId);
        return room;
    }

    private static String message(int speechId) {
        return message(speechId, KEEPER);
    }

    private static String message(int speechId, short playerId) {
        Vector3f position = WorldUtils.pointToVector3f(TILE);
        return speechId + "@" + position + ">p" + playerId;
    }

}
