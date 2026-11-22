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
import com.simsilica.es.filter.FieldFilter;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.GameEnded;
import toniarts.openkeeper.game.component.KeeperComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.RoomMessageShown;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.tools.convert.map.Creature;
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
 * still held. A player whose game has ended, won or lost, gets no more
 * messages: the queue turns them away at its very top. The table also gives each message its tab's icon (entry +6,
 * copied to the queue entry's +0x13), which the message row (0x423BD0) draws:
 * 3 is the fight icon, the heart attack and the defeated keeper here, and 1 the
 * info icon, all the other messages here.
 */
public final class RoomAttackMessages {

    /**
     * DK II's reasons for a creature's mood getting worse, which pick the
     * speech (0x478B80)
     */
    public static final int MOOD_REASON_NO_LAIR = 0;
    public static final int MOOD_REASON_NO_PAY = 2;
    public static final int MOOD_REASON_SLAPPED = 4;

    /**
     * How many messages the queue holds, for all the keepers together
     */
    private static final int QUEUE_SIZE = 256;

    /**
     * How many ticks a message shows its tab, whatever the level's speed
     */
    private static final int TAB_TICKS = 200;

    /**
     * DK II's graphic ids of the tab icons the messages here use
     */
    private static final int INFO_TAB = 1;
    private static final int CREATURE_TAB = 2;
    private static final int FIGHT_TAB = 3;

    private enum Message {

        /**
         * "Your Dungeon Heart is under attack!", message 1. DKII.EXE raises it
         * when the heart object takes damage (0x4DA200)
         */
        HEART_UNDER_ATTACK(120, 346, Map.of(), false, FIGHT_TAB),

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
        KEEPER_DEFEATED(20, 1517, Map.of(), true, FIGHT_TAB),

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
         * Message 0x35: "You do not have enough Mana to maintain your Imps",
         * variant 3 (0x4C1BC0), and "You do not have enough Mana to continue
         * possession", variant 5 (0x4C10E0)
         */
        NOT_ENOUGH_MANA(20, 0, Map.of(3, 1333, 5, 1335)),
        /**
         * "The battle in the Combat Pit is over", message 9, told when a
         * fight is down to one fighter standing (0x4D7A60). It has no tile
         */
        COMBAT_PIT_OVER(50, 354, Map.of(), true, INFO_TAB),
        /**
         * "Your Lair is too small", message 0x2A. The Treasury's and the
         * Hatchery's are held for 240 seconds and the Lair's for 90
         */
        TOO_SMALL(300, 213, Map.ofEntries(Map.entry(1, 198), Map.entry(2, 199), Map.entry(4, 200), Map.entry(6, 210),
                Map.entry(7, 201), Map.entry(9, 211), Map.entry(10, 204), Map.entry(11, 209), Map.entry(12, 208),
                Map.entry(13, 203), Map.entry(14, 206), Map.entry(15, 205), Map.entry(16, 202)),
                Map.of(1, 240, 2, 90, 4, 240), 300),
        /**
         * "You need a Graveyard", message 0x29. A room type without an entry
         * of its own has no speech and isn't told
         */
        NEEDED(300, 0, Map.ofEntries(Map.entry(1, 1307), Map.entry(2, 1308), Map.entry(4, 1309), Map.entry(6, 1506),
                Map.entry(7, 1509), Map.entry(9, 1512), Map.entry(10, 1507), Map.entry(11, 1312), Map.entry(12, 1311),
                Map.entry(13, 1511), Map.entry(14, 1310), Map.entry(15, 1508), Map.entry(16, 1510)),
                Map.of(1, 420, 2, 180, 4, 180, 11, 600, 14, 600), 300),
        /**
         * "An Imp has become unhappy because it has no Lair" and the like,
         * message 0x11. The speech is the creature type's own, by the reason
         * (0x4C4600). It has the creature icon
         */
        CREATURE_UNHAPPY(50, 0, Map.of(), false, CREATURE_TAB),
        /**
         * "An Imp has become angry because it has no Lair" and the like,
         * message 0x12
         */
        CREATURE_ANGRY(50, 0, Map.of(), false, CREATURE_TAB),
        /**
         * The advice for a keeper with a Horned Reaper and little mana,
         * message 0x5F (0x4B9740)
         */
        REAPER_ADVICE(50, 1350, Map.of()),
        /**
         * The mentor's general hints and taunts, message 0x59 variants 1 to
         * 27. Posted even while another one is held (0x4B9740)
         */
        HINT(20, 0, Map.ofEntries(Map.entry(1, 1168), Map.entry(2, 1169), Map.entry(3, 1172), Map.entry(4, 1174),
                Map.entry(5, 1175), Map.entry(6, 1176), Map.entry(7, 1177), Map.entry(8, 1178), Map.entry(9, 1179),
                Map.entry(10, 1180), Map.entry(11, 1181), Map.entry(12, 1182), Map.entry(13, 1183), Map.entry(14, 1184),
                Map.entry(15, 1185), Map.entry(16, 1187), Map.entry(17, 1188), Map.entry(18, 1189), Map.entry(19, 1190),
                Map.entry(20, 1191), Map.entry(21, 1192), Map.entry(22, 1193), Map.entry(23, 1194), Map.entry(24, 1195),
                Map.entry(25, 1196), Map.entry(26, 1197), Map.entry(27, 1199)), true, INFO_TAB),
        /**
         * The hints for a keeper whose creatures are not fighting, message
         * 0x58 variants 1 to 8 (0x4B9740)
         */
        QUIET_HINT(20, 0, Map.of(1, 509, 2, 510, 3, 511, 4, 512, 5, 513, 6, 514, 7, 515, 8, 516), true, INFO_TAB),
        /**
         * "You cannot afford to train creatures", message 0x37 variant 5,
         * held for 240 seconds (0x47B2D0). Variants 1 to 4 of the message
         * are for rooms, traps, doors and the rest
         */
        CANNOT_AFFORD(20, 0, Map.of(5, 1343), Map.of(5, 240), 20),
        /**
         * "Your creatures are unable to get to a Lair", message 0x2B. The
         * Prison's and the Torture Chamber's are held for 360 seconds
         * (variant table at 0x6A0528)
         */
        UNREACHABLE(300, 0, Map.ofEntries(Map.entry(1, 1313), Map.entry(2, 1314), Map.entry(3, 1322), Map.entry(4, 1315),
                Map.entry(6, 1327), Map.entry(7, 1316), Map.entry(9, 1328), Map.entry(10, 1319), Map.entry(11, 1326),
                Map.entry(12, 1325), Map.entry(13, 1318), Map.entry(14, 1321), Map.entry(15, 1320), Map.entry(16, 1317)),
                Map.of(11, 360, 12, 360), 300),
        /**
         * "A creature has been unable to pick up its jackpot winnings",
         * message 0x4D, when the keeper has less gold than the big winner is
         * owed (0x457560)
         */
        JACKPOT_UNPAID(50, 373, Map.of()),
        /**
         * The keeper's own win and loss, messages 0x32 and 0x33 (0x4BE630)
         */
        GAME_WON(50, 1160, Map.of()),
        GAME_LOST(50, 1161, Map.of()),
        /**
         * "Your creatures have been blessed with experience" and the rest,
         * message 0x31, told by each special when it is used (0x4A3FC0 and
         * its handlers). DK II gives each special a variant of its own, here
         * the speech goes by the special's object. Posted even while another
         * one is held
         */
        SPECIAL(20, 0, Map.ofEntries(Map.entry(117, 490), Map.entry(118, 492), Map.entry(119, 493), Map.entry(120, 494),
                Map.entry(121, 495), Map.entry(122, 496), Map.entry(123, 497), Map.entry(124, 498), Map.entry(125, 499),
                Map.entry(126, 500), Map.entry(127, 501), Map.entry(128, 503), Map.entry(129, 502)), true, INFO_TAB);

        private final double duration;
        private final int defaultSpeechId;
        private final Map<Integer, Integer> speechIdsByRoom;
        private final boolean alwaysPosted;
        private final int graphic;
        private final Map<Integer, Integer> durationsByRoom;
        private final double otherRoomDuration;

        private Message(double duration, int defaultSpeechId, Map<Integer, Integer> speechIdsByRoom) {
            this(duration, defaultSpeechId, speechIdsByRoom, Map.of(), 20);
        }

        /**
         * @param durationsByRoom the room types whose message is held for a
         * time of its own
         * @param otherRoomDuration how long the message is held for a room
         * type without an entry of its own, the table's last entry
         */
        private Message(double duration, int defaultSpeechId, Map<Integer, Integer> speechIdsByRoom,
                Map<Integer, Integer> durationsByRoom, double otherRoomDuration) {
            this.duration = duration;
            this.defaultSpeechId = defaultSpeechId;
            this.speechIdsByRoom = speechIdsByRoom;
            this.alwaysPosted = false;
            this.graphic = INFO_TAB;
            this.durationsByRoom = durationsByRoom;
            this.otherRoomDuration = otherRoomDuration;
        }

        /**
         * @param alwaysPosted posted even while the same message is held
         * (table flag 0x4000)
         * @param graphic the tab's icon, the table's graphic id
         */
        private Message(double duration, int defaultSpeechId, Map<Integer, Integer> speechIdsByRoom, boolean alwaysPosted, int graphic) {
            this.duration = duration;
            this.defaultSpeechId = defaultSpeechId;
            this.speechIdsByRoom = speechIdsByRoom;
            this.alwaysPosted = alwaysPosted;
            this.graphic = graphic;
            this.durationsByRoom = Map.of();
            this.otherRoomDuration = 20;
        }

        /**
         * How long the queue holds the message. A room type without an entry
         * of its own gets the table's last entry
         */
        private double getDuration(short roomId) {
            if (speechIdsByRoom.isEmpty()) {
                return duration;
            }
            return speechIdsByRoom.containsKey((int) roomId) ? durationsByRoom.getOrDefault((int) roomId, (int) duration) : otherRoomDuration;
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
        show(Message.NOT_ENOUGH_MANA, ownerId, (short) 3, null);
    }

    /**
     * The keeper's possession ends for want of mana
     *
     * @param ownerId the keeper
     */
    public void notEnoughManaForPossession(short ownerId) {
        show(Message.NOT_ENOUGH_MANA, ownerId, (short) 5, null);
    }

    /**
     * A fight in the keeper's Combat Pit is down to one fighter standing
     *
     * @param ownerId the owner of the Combat Pit
     */
    public void combatPitOver(short ownerId) {
        show(Message.COMBAT_PIT_OVER, ownerId, (short) 0, null);
    }

    /**
     * The keeper has a Horned Reaper and little mana left
     *
     * @param ownerId the keeper
     */
    public void reaperAdvice(short ownerId) {
        show(Message.REAPER_ADVICE, ownerId, (short) 0, null);
    }

    /**
     * One of the mentor's general hints
     *
     * @param ownerId the keeper
     * @param variant which one, 1 to 27
     */
    public void hint(short ownerId, int variant) {
        show(Message.HINT, ownerId, (short) variant, null);
    }

    /**
     * One of the mentor's hints for a quiet dungeon
     *
     * @param ownerId the keeper
     * @param variant which one, 1 to 8
     */
    public void quietHint(short ownerId, int variant) {
        show(Message.QUIET_HINT, ownerId, (short) variant, null);
    }

    /**
     * A creature that would train found that the keeper can't pay for it
     *
     * @param ownerId the keeper
     */
    public void cannotAffordTraining(short ownerId) {
        show(Message.CANNOT_AFFORD, ownerId, (short) 5, null);
    }

    /**
     * The keeper used a special
     *
     * @param ownerId the keeper
     * @param objectId the special's object
     */
    public void specialUsed(short ownerId, short objectId) {
        show(Message.SPECIAL, ownerId, objectId, null);
    }

    /**
     * The big winner of the keeper's Casino came for its winnings and the
     * keeper has less gold than it is owed
     *
     * @param ownerId the keeper
     */
    public void jackpotUnpaid(short ownerId) {
        show(Message.JACKPOT_UNPAID, ownerId, (short) 0, null);
    }

    /**
     * The keeper's game has ended. DK II posts the win or loss message only
     * after it marks the game ended, so its queue turns them away and they are
     * never heard (0x4BE630, 0x4C4600). Here they come first, as meant
     *
     * @param ownerId the keeper
     * @param won whether the keeper won
     */
    public void gameEnded(short ownerId, boolean won) {
        show(won ? Message.GAME_WON : Message.GAME_LOST, ownerId, (short) 0, null);
    }

    /**
     * A creature can't get to the keeper's rooms of the type
     *
     * @param ownerId the keeper
     * @param room the room type
     */
    public void roomUnreachable(short ownerId, Room room) {
        show(Message.UNREACHABLE, ownerId, room.getRoomId(), null);
    }

    /**
     * A creature found no free place in the keeper's rooms of the type
     *
     * @param ownerId the keeper
     * @param room the room type
     */
    public void roomTooSmall(short ownerId, Room room) {
        show(Message.TOO_SMALL, ownerId, room.getRoomId(), null);
    }

    /**
     * The keeper has no room of a type that there is work for
     *
     * @param ownerId the keeper
     * @param room the room type
     */
    public void roomNeeded(short ownerId, Room room) {
        show(Message.NEEDED, ownerId, room.getRoomId(), null);
    }

    /**
     * A creature's anger went up. The keeper is told when that turns the
     * creature unhappy or angry (DKII.EXE 0x478B80, messages 0x11 and 0x12).
     * The speech is the creature type's: its first unhappy or angry speech
     * plus the reason (0x4C4600). The level data has the two speech numbers
     * under the names of the lair and the food anger strings
     *
     * @param creatureId the creature
     * @param ownerId the creature's keeper
     * @param creature the creature type
     * @param before the mood before the anger was added
     * @param after the mood after
     * @param reason DK II's reason, one of the MOOD_REASON values
     */
    public void creatureMoodWorsened(EntityId creatureId, short ownerId, Creature creature, CreatureMood before, CreatureMood after, int reason) {
        int unhappyThreshold = CreatureMood.toRuntimeThreshold(creature.getAttributes().getUnhappyThreshold());
        int state = after.getState(unhappyThreshold);
        int firstSpeechId = state == 1 ? creature.getAngerStringIdLair() : creature.getAngerStringIdFood();
        if (state <= before.getState(unhappyThreshold) || firstSpeechId == 0) {
            return;
        }
        Position position = entityData.getComponent(creatureId, Position.class);
        show(state == 1 ? Message.CREATURE_UNHAPPY : Message.CREATURE_ANGRY, ownerId, (short) 0,
                position != null ? WorldUtils.vectorToPoint(position.position) : null, firstSpeechId + 1 + reason, (short) 0);
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
        if (playerService == null || speechId == 0
                || entityData.findEntity(new FieldFilter<>(KeeperComponent.class, "playerId", ownerId), KeeperComponent.class, GameEnded.class) != null) {
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
        double duration = message.getDuration(roomId);
        playerService.showMentorMessage(speechId, tile != null ? WorldUtils.pointToVector3f(tile) : null, namedPlayerId, ownerId,
                (float) duration, (float) Math.min(duration, TAB_TICKS / (double) ticksPerSecond), message.graphic);
    }

}
