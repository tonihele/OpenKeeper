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

import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Tells the keeper that his room is under attack or that he has lost a room.
 * Each message has a mentor speech of its own per room type, and the same
 * message about the same room type is not repeated while the last one is
 * still shown (DKII.EXE message table at 0x6A0B00, queue at 0x4C4600)
 */
public final class RoomAttackMessages {

    private enum Message {

        /**
         * "Your Lair is under attack", message 0x22
         */
        UNDER_ATTACK(30, 172, Map.ofEntries(Map.entry(1, 157), Map.entry(2, 158), Map.entry(3, 166), Map.entry(4, 159),
                Map.entry(6, 169), Map.entry(7, 160), Map.entry(8, 171), Map.entry(9, 170), Map.entry(10, 163),
                Map.entry(11, 168), Map.entry(12, 167), Map.entry(13, 162), Map.entry(14, 165), Map.entry(15, 164),
                Map.entry(16, 161), Map.entry(17, 171), Map.entry(22, 171))),
        /**
         * "Your Lair has been destroyed", the last of its type, message 0x24
         */
        DESTROYED(20, 109, Map.ofEntries(Map.entry(1, 95), Map.entry(2, 96), Map.entry(4, 97), Map.entry(6, 106),
                Map.entry(7, 98), Map.entry(8, 108), Map.entry(9, 107), Map.entry(10, 101), Map.entry(11, 105),
                Map.entry(12, 104), Map.entry(13, 100), Map.entry(14, 103), Map.entry(15, 102), Map.entry(16, 99),
                Map.entry(17, 108), Map.entry(22, 108))),
        /**
         * "One of your Lairs has been destroyed", message 0x25
         */
        ONE_DESTROYED(20, 124, Map.ofEntries(Map.entry(1, 110), Map.entry(2, 111), Map.entry(4, 112), Map.entry(6, 121),
                Map.entry(7, 113), Map.entry(8, 123), Map.entry(9, 122), Map.entry(10, 116), Map.entry(11, 120),
                Map.entry(12, 119), Map.entry(13, 115), Map.entry(14, 118), Map.entry(15, 117), Map.entry(16, 114),
                Map.entry(17, 123), Map.entry(22, 123)));

        private final double duration;
        private final int defaultSpeechId;
        private final Map<Integer, Integer> speechIdsByRoom;

        private Message(double duration, int defaultSpeechId, Map<Integer, Integer> speechIdsByRoom) {
            this.duration = duration;
            this.defaultSpeechId = defaultSpeechId;
            this.speechIdsByRoom = speechIdsByRoom;
        }

    }

    private record Shown(short playerId, Message message, short roomId) {

    }

    private final PlayerService playerService;
    private final IGameTimer gameTimer;
    private final Map<Shown, Double> shownAt = new HashMap<>();

    /**
     * Create the room messages
     *
     * @param playerService the players to tell, may be {@code null} when there
     * is nobody to tell
     * @param gameTimer the game time
     */
    public RoomAttackMessages(PlayerService playerService, IGameTimer gameTimer) {
        this.playerService = playerService;
        this.gameTimer = gameTimer;
    }

    /**
     * A blow hit the room tile and the room still stands
     *
     * @param ownerId the owner of the room
     * @param room the room type
     * @param tile the tile hit
     */
    public void roomTileAttacked(short ownerId, Room room, Point tile) {
        show(Message.UNDER_ATTACK, ownerId, room, tile);
    }

    /**
     * The room lost a tile. The keeper hears of it if he has fewer rooms of the
     * type than before
     *
     * @param ownerId the owner of the room
     * @param room the room type
     * @param roomsBefore the number of the keeper's rooms of the type before
     * @param roomsAfter the number of the keeper's rooms of the type after
     * @param tile the tile lost
     */
    public void roomLost(short ownerId, Room room, int roomsBefore, int roomsAfter, Point tile) {
        if (roomsAfter < roomsBefore) {
            show(roomsBefore == 1 ? Message.DESTROYED : Message.ONE_DESTROYED, ownerId, room, tile);
        }
    }

    private void show(Message message, short ownerId, Room room, Point tile) {
        if (playerService == null) {
            return;
        }
        Shown shown = new Shown(ownerId, message, room.getRoomId());
        double now = gameTimer.getGameTime();
        Double last = shownAt.get(shown);
        if (last != null && now - last < message.duration) {
            return;
        }
        shownAt.put(shown, now);
        int speechId = message.speechIdsByRoom.getOrDefault((int) room.getRoomId(), message.defaultSpeechId);
        playerService.showMentorMessage(speechId, WorldUtils.pointToVector3f(tile), ownerId);
    }

}
