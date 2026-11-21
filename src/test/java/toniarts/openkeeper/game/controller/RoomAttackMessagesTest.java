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
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.GameEnded;
import toniarts.openkeeper.game.component.KeeperComponent;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureMoodFixtures;
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
 * for a lost room (table at 0x6A0B00). Damage to the Dungeon Heart raises
 * message 1 (0x4DA200), which is not repeated for 120 seconds. The heart room's
 * update warns at exactly 50% and 10% health (0x4D9A60), and the destroyed heart
 * is told to its owner and to its destroyer (0x4DA560); none of them has a tile.
 * In a skirmish or multiplayer game every other player hears that the keeper is
 * defeated (message 0x57), which is told even while the last one is held. The
 * queue holds at most 256 messages for all the keepers together (0x4C4090). A
 * message shows its tab for at most 200 ticks (table flag 0x2000, 0x4C41E0),
 * with the icon of the table's graphic id: 3, the fight icon, for the heart
 * attack and the defeated keeper, 1, the info icon, for the others here. A
 * player whose game has ended gets no more messages (the top of 0x4C4600)
 */
class RoomAttackMessagesTest {

    private static final Point TILE = new Point(4, 5);
    private static final short KEEPER = Player.KEEPER1_ID;

    private final List<String> messages = new ArrayList<>();
    private final List<Float> seconds = new ArrayList<>();
    private final List<Float> tabSeconds = new ArrayList<>();
    private final List<Integer> graphics = new ArrayList<>();
    private double gameTime = 100;
    private final PlayerService playerService = (PlayerService) Proxy.newProxyInstance(PlayerService.class.getClassLoader(),
            new Class<?>[]{PlayerService.class}, (proxy, method, args) -> {
                if (method.getName().equals("showMentorMessage")) {
                    messages.add(args[0] + "@" + args[1] + ">p" + args[3] + ((Short) args[2] != 0 ? " named p" + args[2] : ""));
                    seconds.add((Float) args[4]);
                    tabSeconds.add((Float) args[5]);
                    graphics.add((Integer) args[6]);
                }
                return null;
            });
    private final IGameTimer gameTimer = (IGameTimer) Proxy.newProxyInstance(IGameTimer.class.getClassLoader(),
            new Class<?>[]{IGameTimer.class}, (proxy, method, args) -> method.getName().equals("getGameTime") ? gameTime : null);
    private final EntityData entityData = new DefaultEntityData();
    private final RoomAttackMessages roomAttackMessages = new RoomAttackMessages(entityData, playerService, gameTimer, 4);
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
    void tellsTheKeeperHisHeartIsUnderAttack() {
        roomAttackMessages.heartAttacked(KEEPER, TILE);

        // "Your Dungeon Heart is under attack!"
        assertEquals(List.of(message(346)), messages);
    }

    @Test
    void attackOnTheHeartIsToldOnceIn120Seconds() {
        roomAttackMessages.heartAttacked(KEEPER, TILE);
        gameTime += 119;
        roomAttackMessages.heartAttacked(KEEPER, TILE);
        assertEquals(1, messages.size());

        gameTime += 1;
        roomAttackMessages.heartAttacked(KEEPER, TILE);
        assertEquals(2, messages.size());
    }

    @Test
    void attackOnTheHeartDoesNotHoldBackRoomMessages() {
        roomAttackMessages.heartAttacked(KEEPER, TILE);
        roomAttackMessages.roomTileAttacked(KEEPER, treasury, TILE);

        assertEquals(List.of(message(346), message(157)), messages);
    }

    @Test
    void eachKeeperHearsOfHisOwnHeart() {
        roomAttackMessages.heartAttacked(KEEPER, TILE);
        roomAttackMessages.heartAttacked(Player.KEEPER2_ID, TILE);

        assertEquals(List.of(message(346), message(346, Player.KEEPER2_ID)), messages);
    }

    @Test
    void tellsTheKeeperWhenHisHeartIsAtHalfAndAtTenPercentOfHealth() {
        roomAttackMessages.heartHealthChanged(KEEPER, 1000, 1000);
        roomAttackMessages.heartHealthChanged(KEEPER, 500, 1000);
        roomAttackMessages.heartHealthChanged(KEEPER, 100, 1000);

        // Both are told without a tile
        assertEquals(List.of("347@null>p" + KEEPER, "345@null>p" + KEEPER), messages);
    }

    @Test
    void healthWarningsAreExactPercentagesAndTold50SecondsApart() {
        roomAttackMessages.heartHealthChanged(KEEPER, 510, 1000);
        roomAttackMessages.heartHealthChanged(KEEPER, 499, 1000);
        roomAttackMessages.heartHealthChanged(KEEPER, 5, 1000);
        assertEquals(List.of(), messages);

        roomAttackMessages.heartHealthChanged(KEEPER, 509, 1000);
        gameTime += 49;
        roomAttackMessages.heartHealthChanged(KEEPER, 500, 1000);
        assertEquals(1, messages.size());

        gameTime += 1;
        roomAttackMessages.heartHealthChanged(KEEPER, 500, 1000);
        assertEquals(2, messages.size());
    }

    @Test
    void destroyedHeartIsToldToItsOwnerAndItsDestroyer() {
        roomAttackMessages.heartDestroyed(KEEPER, Player.KEEPER2_ID, List.of());

        // "Your Dungeon Heart has been destroyed!", "You have defeated an enemy Keeper"
        assertEquals(List.of("1291@null>p" + KEEPER, "1162@null>p" + Player.KEEPER2_ID), messages);
    }

    @Test
    void everyOtherPlayerHearsTheKeeperIsDefeatedByName() {
        roomAttackMessages.heartDestroyed(KEEPER, Player.KEEPER2_ID, List.of(KEEPER, Player.KEEPER2_ID, Player.KEEPER3_ID));

        // "%3 has been defeated", "...crushed", "...banished from this land" or "...demolished", one for all
        String defeated = messages.get(1).substring(0, 4);
        assertTrue(List.of("1517", "1518", "1519", "1520").contains(defeated));
        assertEquals(List.of("1291@null>p" + KEEPER, defeated + "@null>p" + Player.KEEPER2_ID + " named p" + KEEPER,
                defeated + "@null>p" + Player.KEEPER3_ID + " named p" + KEEPER, "1162@null>p" + Player.KEEPER2_ID), messages);
    }

    @Test
    void destroyedHeartWithoutADestroyerIsToldOnlyToItsOwner() {
        roomAttackMessages.heartDestroyed(KEEPER, (short) 0, List.of());
        roomAttackMessages.heartDestroyed(Player.KEEPER2_ID, Player.KEEPER2_ID, List.of());

        assertEquals(List.of("1291@null>p" + KEEPER, "1291@null>p" + Player.KEEPER2_ID), messages);
    }

    @Test
    void everyDefeatIsToldEvenWhileTheLastIsHeld() {
        roomAttackMessages.heartDestroyed(KEEPER, (short) 0, List.of(Player.KEEPER3_ID));
        gameTime += 1;
        roomAttackMessages.heartDestroyed(Player.KEEPER2_ID, (short) 0, List.of(Player.KEEPER3_ID));

        // Message 0x57 has table flag 0x4000, it is always posted
        assertEquals(4, messages.size());
        assertTrue(messages.get(3).endsWith(">p" + Player.KEEPER3_ID + " named p" + Player.KEEPER2_ID));
    }

    @Test
    void eachMessageStaysForItsOwnTime() {
        roomAttackMessages.heartAttacked(KEEPER, TILE);
        roomAttackMessages.roomTileAttacked(KEEPER, lair, TILE);
        roomAttackMessages.roomTileAttacked(KEEPER, room("Casino", 99), TILE);
        roomAttackMessages.lowManaWarning(KEEPER, 1);

        // A room type without an entry of its own in the attack message's table gets its last entry, 20 seconds
        assertEquals(List.of(120f, 30f, 20f, 20f), seconds);

        // The heart attack is held for 120 seconds, but its tab goes after 200 ticks, 50 seconds at 4 ticks a second
        assertEquals(List.of(50f, 30f, 20f, 20f), tabSeconds);
    }

    @Test
    void theTabGoesAfter200TicksWhateverTheLevelSpeed() {
        RoomAttackMessages fasterLevel = new RoomAttackMessages(new DefaultEntityData(), playerService, gameTimer, 10);

        fasterLevel.heartAttacked(KEEPER, TILE);
        fasterLevel.roomTileAttacked(KEEPER, lair, TILE);

        // 200 ticks are 20 seconds at 10 ticks a second
        assertEquals(List.of(20f, 20f), tabSeconds);

        // The heart attack is still held for its 120 seconds
        gameTime += 119;
        fasterLevel.heartAttacked(KEEPER, TILE);
        assertEquals(2, messages.size());
    }

    @Test
    void eachMessageHasTheTabIconOfItsTableEntry() {
        roomAttackMessages.heartAttacked(KEEPER, TILE);
        roomAttackMessages.roomTileAttacked(KEEPER, lair, TILE);
        roomAttackMessages.roomLost(KEEPER, lair, 1, 0, TILE);
        roomAttackMessages.heartHealthChanged(KEEPER, 50, 100);
        roomAttackMessages.lowManaWarning(KEEPER, 1);
        roomAttackMessages.heartDestroyed(KEEPER, Player.KEEPER2_ID, List.of(Player.KEEPER3_ID));

        // The heart under attack and the defeated keeper have the fight icon (3), the rest the info icon (1)
        assertEquals(List.of(3, 1, 1, 1, 1, 1, 3, 1), graphics);
    }

    @Test
    void eachSpecialTellsWhatItDidEvenWhileTheLastIsHeld() {
        roomAttackMessages.specialUsed(KEEPER, (short) 117);
        roomAttackMessages.specialUsed(KEEPER, (short) 117);
        roomAttackMessages.specialUsed(KEEPER, (short) 123);
        roomAttackMessages.specialUsed(KEEPER, (short) 129);

        // Message 0x31, flag 0x4000: "...blessed with experience" twice, "Enemy Imps have been stunned", "A hidden realm..."
        assertEquals(List.of("490@null>p" + KEEPER, "490@null>p" + KEEPER, "497@null>p" + KEEPER, "502@null>p" + KEEPER), messages);
        assertEquals(List.of(20f, 20f, 20f, 20f), seconds);
    }

    @Test
    void lowManaWarningsAreHeldEachOnItsOwn() {
        roomAttackMessages.lowManaWarning(KEEPER, 3);
        gameTime += 10;
        roomAttackMessages.lowManaWarning(KEEPER, 3);
        roomAttackMessages.lowManaWarning(KEEPER, 4);
        gameTime += 10;
        roomAttackMessages.lowManaWarning(KEEPER, 3);

        // Message 0x34 has a variant table (flag 0x8000), so only the same warning is held back, for 20 seconds
        assertEquals(List.of("1330@null>p" + KEEPER, "1331@null>p" + KEEPER, "1330@null>p" + KEEPER), messages);
    }

    @Test
    void aFullQueueDropsTheOldestMessage() {
        roomAttackMessages.heartAttacked(KEEPER, TILE);
        List<Short> players = new ArrayList<>();
        for (short playerId = 10; playerId < 264; playerId++) {
            players.add(playerId);
        }
        roomAttackMessages.heartDestroyed(Player.KEEPER2_ID, (short) 0, players);

        // 256 messages are held, the heart attack among them
        roomAttackMessages.heartAttacked(KEEPER, TILE);
        assertEquals(256, messages.size());

        // One more takes the place of the oldest, so the heart attack can be told again
        roomAttackMessages.heartDestroyed(Player.KEEPER2_ID, (short) 0, List.of((short) 300));
        roomAttackMessages.heartAttacked(KEEPER, TILE);
        assertEquals(258, messages.size());
        assertEquals(message(346), messages.get(257));
    }

    /**
     * DKII.EXE 0x478B80 and 0x4C4600: anger that turns a creature unhappy or
     * angry is told to its keeper, messages 0x11 and 0x12. The speech is the
     * creature type's first unhappy or angry speech + 1 + the reason: for the
     * Imp 586 "An Imp has become unhappy because it has no Lair" and 858 "An
     * Imp has become angry because it hasn't been paid". Each is held for 50
     * seconds and has the creature icon
     */
    @Test
    void tellsTheKeeperOfACreatureThatTurnsUnhappyOrAngry() {
        Creature imp = CreatureMoodFixtures.creature((short) 1, "Imp", (short) 0, (short) 0, (short) 50, 585, 855);
        CreatureMood content = new CreatureMood();
        CreatureMood unhappy = content.add(CreatureMood.REASON_NO_LAIR, 16383);
        CreatureMood angry = unhappy.add(CreatureMood.REASON_NO_PAY, 32768);

        roomAttackMessages.creatureMoodWorsened(new EntityId(1), KEEPER, imp, content, unhappy, RoomAttackMessages.MOOD_REASON_NO_LAIR);
        roomAttackMessages.creatureMoodWorsened(new EntityId(1), KEEPER, imp, unhappy, angry, RoomAttackMessages.MOOD_REASON_NO_PAY);

        assertEquals(List.of("586@null>p" + KEEPER, "858@null>p" + KEEPER), messages);
        assertEquals(List.of(50f, 50f), seconds);
        assertEquals(List.of(2, 2), graphics);
    }

    @Test
    void aMoodThatDoesNotGetWorseIsNotTold() {
        Creature imp = CreatureMoodFixtures.creature((short) 1, "Imp", (short) 0, (short) 0, (short) 50, 585, 855);
        CreatureMood unhappy = new CreatureMood().add(CreatureMood.REASON_NO_LAIR, 16383);

        // Still unhappy, and calming down
        roomAttackMessages.creatureMoodWorsened(new EntityId(1), KEEPER, imp, unhappy, unhappy.add(CreatureMood.REASON_NO_LAIR, 10), RoomAttackMessages.MOOD_REASON_NO_LAIR);
        roomAttackMessages.creatureMoodWorsened(new EntityId(1), KEEPER, imp, unhappy, new CreatureMood(), RoomAttackMessages.MOOD_REASON_NO_LAIR);

        assertTrue(messages.isEmpty());
    }

    @Test
    void unhappyCreaturesAreToldOnceIn50Seconds() {
        Creature imp = CreatureMoodFixtures.creature((short) 1, "Imp", (short) 0, (short) 0, (short) 50, 585, 855);
        CreatureMood content = new CreatureMood();
        CreatureMood unhappy = content.add(CreatureMood.REASON_OTHER, 16383);

        roomAttackMessages.creatureMoodWorsened(new EntityId(1), KEEPER, imp, content, unhappy, RoomAttackMessages.MOOD_REASON_SLAPPED);
        gameTime += 49;
        roomAttackMessages.creatureMoodWorsened(new EntityId(2), KEEPER, imp, content, unhappy, RoomAttackMessages.MOOD_REASON_NO_LAIR);
        assertEquals(List.of("590@null>p" + KEEPER), messages);

        gameTime += 1;
        roomAttackMessages.creatureMoodWorsened(new EntityId(2), KEEPER, imp, content, unhappy, RoomAttackMessages.MOOD_REASON_NO_LAIR);
        assertEquals(List.of("590@null>p" + KEEPER, "586@null>p" + KEEPER), messages);
    }

    @Test
    void theKeeperHearsItsOwnWinOrLossBeforeTheMessagesStop() {
        entityData.setComponent(entityData.createEntity(), new KeeperComponent(KEEPER));
        entityData.setComponent(entityData.createEntity(), new KeeperComponent(Player.KEEPER2_ID));

        roomAttackMessages.gameEnded(KEEPER, true);
        roomAttackMessages.gameEnded(Player.KEEPER2_ID, false);

        assertEquals(List.of("1160@null>p" + KEEPER, "1161@null>p" + Player.KEEPER2_ID), messages);
    }

    @Test
    void aKeeperWhoseGameHasEndedGetsNoMoreMessages() {
        EntityId loser = entityData.createEntity();
        entityData.setComponent(loser, new KeeperComponent(KEEPER));
        entityData.setComponent(entityData.createEntity(), new KeeperComponent(Player.KEEPER2_ID));
        roomAttackMessages.roomTileAttacked(KEEPER, lair, TILE);
        assertEquals(1, messages.size());

        entityData.setComponent(loser, new GameEnded(false));
        roomAttackMessages.roomTileAttacked(KEEPER, treasury, TILE);
        roomAttackMessages.heartHealthChanged(KEEPER, 500, 1000);
        roomAttackMessages.hint(KEEPER, 1);
        assertEquals(1, messages.size());

        // The others still hear of theirs, and of the keeper's defeat
        roomAttackMessages.roomTileAttacked(Player.KEEPER2_ID, lair, TILE);
        roomAttackMessages.heartDestroyed(KEEPER, (short) 0, List.of(KEEPER, Player.KEEPER2_ID));
        assertEquals(3, messages.size());
        assertTrue(messages.get(2).endsWith(">p" + Player.KEEPER2_ID + " named p" + KEEPER));
    }

    @Test
    void worksWithoutPlayers() {
        RoomAttackMessages withoutPlayers = new RoomAttackMessages(new DefaultEntityData(), null, gameTimer, 4);

        withoutPlayers.roomTileAttacked(KEEPER, lair, TILE);
        withoutPlayers.roomLost(KEEPER, lair, 1, 0, TILE);
        withoutPlayers.heartAttacked(KEEPER, TILE);
        withoutPlayers.heartHealthChanged(KEEPER, 500, 1000);
        withoutPlayers.heartDestroyed(KEEPER, Player.KEEPER2_ID, List.of(Player.KEEPER2_ID));

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
