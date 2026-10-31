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
package toniarts.openkeeper.game.logic;

import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.CreatureAttractionFixtures.attraction;
import static toniarts.openkeeper.tools.convert.map.CreatureAttractionFixtures.creature;
import static toniarts.openkeeper.tools.convert.map.CreatureAttractionFixtures.room;

/**
 * The attraction requirements of a creature, as the original checks them: an
 * attraction is a room of at least the given size counting all the keeper's
 * rooms of that type together, an amount of gold or of mana.
 */
class CreatureAttractionTest {

    private static final short PLAYER_ID = 3;
    private static final int ROOM = 1;
    private static final int GOLD = 2;
    private static final int MANA = 3;
    private static final int CREATURE = 4;

    private final Room hatchery = room((short) 4);
    private final Room lair = room((short) 2);
    private Keeper keeper;
    private PlayerRoomControl rooms;
    private IPlayerController player;
    private IKwdFile level;

    @BeforeEach
    void setUp() {
        keeper = new Keeper(false, PLAYER_ID);
        rooms = new PlayerRoomControl(keeper, List.of(hatchery, lair));
        player = stub(IPlayerController.class, (method, args) -> switch (method) {
            case "getKeeper" ->
                keeper;
            case "getRoomControl" ->
                rooms;
            default ->
                throw new AssertionError(method);
        });
        level = stub(IKwdFile.class, (method, args) -> switch (method) {
            case "getRoomById" ->
                ((Number) args[0]).intValue() == hatchery.getRoomId() ? hatchery : ((Number) args[0]).intValue() == lair.getRoomId() ? lair : null;
            default ->
                throw new AssertionError(method);
        });
    }

    @Test
    void roomSizeCountsAllTheRoomsOfTheType() {
        Creature bileDemon = creature((short) 3, attraction(ROOM, hatchery.getRoomId(), 25));
        build(hatchery, 16);
        build(hatchery, 12);

        assertTrue(CreatureAttraction.isSatisfied(bileDemon, player, level));
    }

    @Test
    void roomSizeNotReachedByAllTheRoomsTogether() {
        Creature bileDemon = creature((short) 3, attraction(ROOM, hatchery.getRoomId(), 25));
        build(hatchery, 16);
        build(hatchery, 8);

        assertFalse(CreatureAttraction.isSatisfied(bileDemon, player, level));
    }

    @Test
    void roomWithoutSizeStillNeedsTheRoom() {
        Creature goblin = creature((short) 7, attraction(ROOM, lair.getRoomId(), 0));

        assertFalse(CreatureAttraction.isSatisfied(goblin, player, level));
        build(lair, 1);
        assertTrue(CreatureAttraction.isSatisfied(goblin, player, level));
    }

    @Test
    void goldAttraction() {

        // The amount is the low byte of the first value and the low byte of the second as the high byte
        Creature creature = creature((short) 5, attraction(GOLD, 0x88, 0x13));
        keeper.setGold(0x1387);
        assertFalse(CreatureAttraction.isSatisfied(creature, player, level));

        keeper.setGold(0x1388);
        assertTrue(CreatureAttraction.isSatisfied(creature, player, level));
    }

    @Test
    void manaAttraction() {
        Creature creature = creature((short) 5, attraction(MANA, 0xE8, 0x03));
        keeper.setMana(999);
        assertFalse(CreatureAttraction.isSatisfied(creature, player, level));

        keeper.setMana(1000);
        assertTrue(CreatureAttraction.isSatisfied(creature, player, level));
    }

    @Test
    void creatureAttractionNeverStopsTheCreature() {

        // The original compares the creature count with zero, so this requirement is always met
        Creature creature = creature((short) 5, attraction(CREATURE, 7, 3));

        assertTrue(CreatureAttraction.isSatisfied(creature, player, level));
    }

    @Test
    void noAttractions() {
        Creature creature = creature((short) 5, attraction(0, 0, 0), attraction(0, 0, 0));

        assertTrue(CreatureAttraction.isSatisfied(creature, player, level));
    }

    private void build(Room room, int tiles) {
        RoomInstance instance = new RoomInstance(room);
        instance.setOwnerId(PLAYER_ID);
        for (int i = 0; i < tiles; i++) {
            instance.addCoordinate(new Point(i, rooms.getTypeCount()));
        }
        IRoomController controller = stub(IRoomController.class, (method, args) -> switch (method) {
            case "getRoom" ->
                room;
            case "getRoomInstance" ->
                instance;
            case "isDungeonHeart" ->
                false;
            default ->
                throw new AssertionError(method);
        });
        rooms.onBuild(controller);
    }

    private interface Calls {

        Object call(String method, Object[] args);
    }

    private static <T> T stub(Class<T> type, Calls calls) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> switch (method.getName()) {
            case "hashCode" ->
                System.identityHashCode(proxy);
            case "equals" ->
                proxy == args[0];
            default ->
                calls.call(method.getName(), args);
        }));
    }
}
