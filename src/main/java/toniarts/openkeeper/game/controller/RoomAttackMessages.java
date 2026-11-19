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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.component.RoomMessageShown;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Tells the keeper that his room or his Dungeon Heart is under attack, that he
 * has lost a room or his heart, or that his mana is low. Each room message has
 * a mentor speech of its own per room type.<br>
 * The messages go through DK II's message queue (0x4C4600, table at 0x6A0B00):
 * each message is held for its own time, and most messages are not posted or
 * spoken again while the same one, about the same room type where the message
 * has one per room type, is still held. The queue holds at most 256 messages
 * for all the keepers together, a new one takes the place of the oldest
 * (0x4C4090). Every message in the table has flag 0x2000, which shows it as a
 * message tab, and the per tick update (0x4C41E0) clears that once the message
 * is 200 ticks old, so the tab goes after 200 ticks even while the message is
 * still held.
 */
public final class RoomAttackMessages {

    /**
     * How many messages the queue holds, for all the keepers together
     */
    private static final int QUEUE_SIZE = 256;

    /**
     * How many ticks a message shows its tab, whatever the level's speed
     */
    private static final int TAB_TICKS = 200;

    private enum Message {

        /**
         * "Your Dungeon Heart is under attack!", message 1. DKII.EXE raises it
         * when the heart object takes damage (0x4DA200)
         */
        HEART_UNDER_ATTACK(120, 346, Map.of()),

        /**
         * "Your Dungeon Heart is at half its health", message 0x27. DKII.EXE
         * raises it in the heart room's update when the health is at exactly
         * 50% (0x4D9A60). It has no tile
         */
        HEART_HALF_HEALTH(50, 347, Map.of()),

        /**
         * "Your Dungeon Heart is almost dead", message 0x28, at exactly 10%
         * (0x4D9A60). It has no tile
         */
        HEART_LOW_HEALTH(50, 345, Map.of()),

        /**
         * "Your Dungeon Heart has been destroyed!", message 2, told to the
         * owner when the heart is destroyed (0x4DA560). It has no tile
         */
        HEART_DESTROYED(50, 1291, Map.of()),

        /**
         * "You have defeated an enemy Keeper", message 3, told to the player
         * who destroyed the heart (0x4DA560). It has no tile
         */
        ENEMY_DEFEATED(20, 1162, Map.of()),

        /**
         * "%3 has been defeated", "...crushed", "...banished from this land"
         * or "...demolished", message 0x57, told in a skirmish or multiplayer
         * game to every player but the owner when a heart is destroyed
         * (0x4DA560). %3 is the defeated keeper. The four variants are speech
         * 1517 to 1520. It has no tile
         */
        KEEPER_DEFEATED(20, 1517, Map.of(), true),

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
                Map.entry(17, 123), Map.entry(22, 123))),
        /**
         * The low mana warnings, message 0x34 variants 1 to 4: "Your Mana
         * level is low", "...still too low", "...distressingly low, Keeper"
         * and "...I will not warn you again" (0x4C10E0)
         */
        LOW_MANA(20, 447, Map.of(1, 447, 2, 1329, 3, 1330, 4, 1331)),
        /**
         * "You do not have enough Mana to maintain your Imps", message 0x35
         * variant 3 (0x4C1BC0)
         */
        NOT_ENOUGH_MANA_FOR_IMPS(20, 1333, Map.of());

        private final double duration;
        private final int defaultSpeechId;
        private final Map<Integer, Integer> speechIdsByRoom;
        private final boolean alwaysPosted;

        private Message(double duration, int defaultSpeechId, Map<Integer, Integer> speechIdsByRoom) {
            this(duration, defaultSpeechId, speechIdsByRoom, false);
        }

        /**
         * @param alwaysPosted posted even while the same message is held
         * (table flag 0x4000)
         */
        private Message(double duration, int defaultSpeechId, Map<Integer, Integer> speechIdsByRoom, boolean alwaysPosted) {
            this.duration = duration;
            this.defaultSpeechId = defaultSpeechId;
            this.speechIdsByRoom = speechIdsByRoom;
            this.alwaysPosted = alwaysPosted;
        }

        /**
         * How long the queue holds the message. A room type without an entry
         * of its own gets the table's last entry, 20 seconds
         */
        private double getDuration(short roomId) {
            return speechIdsByRoom.isEmpty() || speechIdsByRoom.containsKey((int) roomId) ? duration : 20;
        }

    }

    private final EntityData entityData;
    private final PlayerService playerService;
    private final IGameTimer gameTimer;
    private final int ticksPerSecond;

    /**
     * Create the room messages
     *
     * @param entityData the entity data, keeps when each message was shown
     * @param playerService the players to tell, may be {@code null} when there
     * is nobody to tell
     * @param gameTimer the game time
     * @param ticksPerSecond the level's ticks per second
     */
    public RoomAttackMessages(EntityData entityData, PlayerService playerService, IGameTimer gameTimer, int ticksPerSecond) {
        this.entityData = entityData;
        this.playerService = playerService;
        this.gameTimer = gameTimer;
        this.ticksPerSecond = ticksPerSecond;
    }

    /**
     * A blow hit the room tile and the room still stands
     *
     * @param ownerId the owner of the room
     * @param room the room type
     * @param tile the tile hit
     */
    public void roomTileAttacked(short ownerId, Room room, Point tile) {
        show(Message.UNDER_ATTACK, ownerId, room.getRoomId(), tile);
    }

    /**
     * The Dungeon Heart took damage
     *
     * @param ownerId the owner of the heart
     * @param tile the tile of the heart
     */
    public void heartAttacked(short ownerId, Point tile) {
        show(Message.HEART_UNDER_ATTACK, ownerId, (short) 0, tile);
    }

    /**
     * The Dungeon Heart's health changed. DKII.EXE checks the heart's health
     * as a whole percent in the heart room's update, and warns at exactly 50%
     * and at exactly 10%, so a change that jumps over them isn't warned about
     *
     * @param ownerId the owner of the heart
     * @param health the health of the heart
     * @param maxHealth the full health of the heart
     */
    public void heartHealthChanged(short ownerId, int health, int maxHealth) {
        int percent = health * 100 / maxHealth;
        if (percent == 50) {
            show(Message.HEART_HALF_HEALTH, ownerId, (short) 0, null);
        } else if (percent == 10) {
            show(Message.HEART_LOW_HEALTH, ownerId, (short) 0, null);
        }
    }

    /**
     * The Dungeon Heart was destroyed
     *
     * @param ownerId the owner of the heart
     * @param destroyerId the player who destroyed it, 0 if not known
     * @param playerIds the players who hear that the owner is defeated, the
     * owner left out, all of them in a skirmish or multiplayer game, else none
     */
    public void heartDestroyed(short ownerId, short destroyerId, Collection<Short> playerIds) {
        show(Message.HEART_DESTROYED, ownerId, (short) 0, null);

        // DK II picks one of the four variants for them all
        int speechId = Message.KEEPER_DEFEATED.defaultSpeechId + ThreadLocalRandom.current().nextInt(4);
        for (short playerId : playerIds) {
            if (playerId != ownerId) {
                show(Message.KEEPER_DEFEATED, playerId, (short) 0, null, speechId, ownerId);
            }
        }
        if (destroyerId != 0 && destroyerId != ownerId) {
            show(Message.ENEMY_DEFEATED, destroyerId, (short) 0, null);
        }
    }

    /**
     * The keeper is warned that his mana is low
     *
     * @param ownerId the keeper
     * @param warning the warning, 1 to 4
     */
    public void lowManaWarning(short ownerId, int warning) {
        show(Message.LOW_MANA, ownerId, (short) warning, null);
    }

    /**
     * An imp of the keeper's is chosen to pop for want of mana
     *
     * @param ownerId the keeper
     */
    public void notEnoughManaForImps(short ownerId) {
        show(Message.NOT_ENOUGH_MANA_FOR_IMPS, ownerId, (short) 0, null);
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
            show(roomsBefore == 1 ? Message.DESTROYED : Message.ONE_DESTROYED, ownerId, room.getRoomId(), tile);
        }
    }

    private void show(Message message, short ownerId, short roomId, Point tile) {
        show(message, ownerId, roomId, tile, message.speechIdsByRoom.getOrDefault((int) roomId, message.defaultSpeechId), (short) 0);
    }

    private void show(Message message, short ownerId, short roomId, Point tile, int speechId, short namedPlayerId) {
        if (playerService == null) {
            return;
        }
        double now = gameTimer.getGameTime();
        EntityId oldest = null;
        int held = 0;
        for (EntityId id : entityData.findEntities(null, RoomMessageShown.class)) {
            RoomMessageShown entry = entityData.getComponent(id, RoomMessageShown.class);
            Message entryMessage = Message.values()[entry.message];
            if (now - entry.shownAt >= entryMessage.getDuration(entry.roomId)) {
                entityData.removeEntity(id);
            } else if (entryMessage == message && !message.alwaysPosted && entry.playerId == ownerId && entry.roomId == roomId) {
                return;
            } else {
                held++;
                if (oldest == null || id.getId() < oldest.getId()) {
                    oldest = id;
                }
            }
        }
        if (held >= QUEUE_SIZE) {
            entityData.removeEntity(oldest);
        }
        entityData.setComponent(entityData.createEntity(), new RoomMessageShown(ownerId, message.ordinal(), roomId, now));
        playerService.showMentorMessage(speechId, tile != null ? WorldUtils.pointToVector3f(tile) : null, namedPlayerId, ownerId,
                (float) Math.min(message.getDuration(roomId), TAB_TICKS / (double) ticksPerSecond));
    }

}
