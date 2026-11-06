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

import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerCreatureControl;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Creature.CreatureFlag;
import toniarts.openkeeper.tools.convert.map.Creature.CreatureFlag2;
import toniarts.openkeeper.tools.convert.map.CreatureAttractionFixtures;
import toniarts.openkeeper.tools.convert.map.GameLevel.LevFlag;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.CreatureAttractionFixtures.room;
import static toniarts.openkeeper.tools.convert.map.PortalChoiceFixtures.creature;

/**
 * In skirmish and multiplayer games a Portal calls in a unique creature when
 * the keeper builds that creature's secret room layout, once per level, before
 * it attracts an ordinary creature
 */
class PortalUniqueCreatureTest {

    private static final short PLAYER_ID = 3;
    private static final short ENEMY_ID = 4;

    private final Room treasury = room((short) 1);
    private final Room lair = room((short) 2);
    private final Room hatchery = room((short) 4);
    private final Room trainingRoom = room((short) 7);
    private final Room guardRoom = room((short) 9);
    private final Room portal = room((short) 3);

    private final Map<Short, Creature> uniques = new HashMap<>();
    private final List<EntityId> creaturesWithoutLair = new ArrayList<>();
    private final PortalCreatureChoice choice = new PortalCreatureChoice(new DefaultEntityData(), new Random(3));
    private PlayerRoomControl rooms;
    private PlayerRoomControl enemyRooms;
    private PlayerCreatureControl creatures;
    private IPlayerController player;
    private IPlayerController enemy;
    private ICreaturesController creaturesController;
    private IKwdFile level;

    @BeforeEach
    void setUp() {
        for (short id = 32; id <= 44; id++) {
            uniques.put(id, creature(id, (short) 0, EnumSet.of(CreatureFlag.IS_UNIQUE, CreatureFlag.AVAILABLE_VIA_PORTAL),
                    EnumSet.noneOf(CreatureFlag2.class)));
        }
        List<Room> roomTypes = List.of(treasury, lair, hatchery, trainingRoom, guardRoom);
        Keeper keeper = new Keeper(false, PLAYER_ID);
        rooms = new PlayerRoomControl(keeper, roomTypes);
        creatures = new PlayerCreatureControl(keeper, null, uniques.values());
        player = player(keeper, rooms, creatures);
        Keeper enemyKeeper = new Keeper(false, ENEMY_ID);
        enemyRooms = new PlayerRoomControl(enemyKeeper, roomTypes);
        enemy = player(enemyKeeper, enemyRooms, new PlayerCreatureControl(enemyKeeper, null, uniques.values()));
        creaturesController = stub(ICreaturesController.class, (method, args) -> switch (method) {
            case "createController" -> {
                EntityId entityId = (EntityId) args[0];
                yield stub(ICreatureController.class, (creatureMethod, creatureArgs) -> switch (creatureMethod) {
                    case "hasLair" ->
                        !creaturesWithoutLair.contains(entityId);
                    default ->
                        throw new AssertionError(creatureMethod);
                });
            }
            default ->
                throw new AssertionError(method);
        });
        level = stub(IKwdFile.class, (method, args) -> switch (method) {
            case "getCreature" ->
                uniques.get((Short) args[0]);
            default ->
                throw new AssertionError(method);
        });
    }

    @Test
    void onlySkirmishAndMultiplayerLevels() {
        assertTrue(PortalCreatureChoice.isUniqueLayoutLevel(EnumSet.of(LevFlag.UNKNOWN, LevFlag.IS_SKIRMISH_LEVEL)));
        assertTrue(PortalCreatureChoice.isUniqueLayoutLevel(EnumSet.of(LevFlag.UNKNOWN, LevFlag.IS_MULTIPLAYER_LEVEL)));
        assertFalse(PortalCreatureChoice.isUniqueLayoutLevel(EnumSet.of(LevFlag.UNKNOWN, LevFlag.HEART_MAKES_GEM)));
    }

    @Test
    void lairsAtTheCornersOfAHatcheryCrossCallInTheUniqueFirefly() {
        buildFireflyLayout(10, 10);

        assertSame(uniques.get((short) 32), choose());
    }

    @Test
    void eachUniqueComesOncePerLevel() {
        buildFireflyLayout(10, 10);
        assertSame(uniques.get((short) 32), choose());

        assertNull(choose());
    }

    @Test
    void incompleteLayoutCallsNobody() {
        build(rooms, lair, 10, 10);
        build(rooms, lair, 12, 10);
        build(rooms, lair, 10, 12);
        build(rooms, lair, 12, 12);
        build(rooms, hatchery, 11, 10, 10, 11, 11, 11, 12, 11);

        assertNull(choose());
    }

    @Test
    void cornerRoomMustBeExactlyItsSize() {
        build(rooms, lair, 10, 10, 9, 10);
        build(rooms, lair, 12, 10);
        build(rooms, lair, 10, 12);
        build(rooms, lair, 12, 12);
        build(rooms, hatchery, 11, 10, 10, 11, 11, 11, 12, 11, 11, 12);

        assertNull(choose());
    }

    @Test
    void layoutIsLaidFromTheRoomsTopLeftCorner() {

        // A ring of Training Room around a 3 by 3 Guard Room calls in the unique Dark Elf
        // Listed from the bottom right, the corner is found anyway
        List<Integer> ring = new ArrayList<>();
        for (int y = 34; y >= 30; y--) {
            for (int x = 24; x >= 20; x--) {
                if (x == 20 || x == 24 || y == 30 || y == 34) {
                    ring.addAll(List.of(x, y));
                }
            }
        }
        build(rooms, trainingRoom, ring.stream().mapToInt(Integer::intValue).toArray());
        build(rooms, guardRoom, 21, 31, 22, 31, 23, 31, 21, 32, 22, 32, 23, 32, 21, 33, 22, 33, 23, 33);

        assertSame(uniques.get((short) 36), choose());
    }

    @Test
    void otherKeepersRoomsCountInTheLayout() {
        build(rooms, lair, 10, 10);
        build(rooms, lair, 12, 10);
        build(rooms, lair, 10, 12);
        build(rooms, lair, 12, 12);
        build(enemyRooms, hatchery, 11, 10, 10, 11, 11, 11, 12, 11, 11, 12);

        assertSame(uniques.get((short) 32), choose());
    }

    @Test
    void onlyTheKeepersOwnRoomsCallIn() {
        build(enemyRooms, lair, 10, 10);
        build(enemyRooms, lair, 12, 10);
        build(enemyRooms, lair, 10, 12);
        build(enemyRooms, lair, 12, 12);
        build(enemyRooms, hatchery, 11, 10, 10, 11, 11, 11, 12, 11, 11, 12);

        assertNull(choose());
    }

    @Test
    void nobodyComesWhileShortOfLairSpace() {
        buildFireflyLayout(10, 10);
        EntityId bedless = new EntityId(1);
        Creature goblin = creature((short) 7, (short) 22, EnumSet.of(CreatureFlag.AVAILABLE_VIA_PORTAL), EnumSet.noneOf(CreatureFlag2.class));
        creatures.onCreatureAdded(bedless, goblin);
        creaturesWithoutLair.add(bedless);

        assertNull(choose());

        // Still to come once there is room
        creaturesWithoutLair.clear();
        assertSame(uniques.get((short) 32), choose());
    }

    @Test
    void withoutALayoutTheOrdinaryCreatureComes() {
        Creature goblin = creature((short) 7, (short) 22, EnumSet.of(CreatureFlag.AVAILABLE_VIA_PORTAL), EnumSet.noneOf(CreatureFlag2.class),
                CreatureAttractionFixtures.attraction(0, 2, 0));
        creatures.setTypeAvailable(goblin, true);
        build(rooms, lair, 10, 10);
        IRoomController portalRoom = stub(IRoomController.class, (method, args) -> switch (method) {
            case "getRoom" ->
                portal;
            default ->
                throw new AssertionError(method);
        });

        assertNull(choose());
        assertSame(goblin, choice.chooseCreature(player, portalRoom, creaturesController, creature -> true));
    }

    private Creature choose() {
        return choice.chooseUniqueCreature(player, List.of(player, enemy), creaturesController, level);
    }

    private void buildFireflyLayout(int x, int y) {
        build(rooms, lair, x, y);
        build(rooms, lair, x + 2, y);
        build(rooms, lair, x, y + 2);
        build(rooms, lair, x + 2, y + 2);
        build(rooms, hatchery, x + 1, y, x, y + 1, x + 1, y + 1, x + 2, y + 1, x + 1, y + 2);
    }

    private static void build(PlayerRoomControl owner, Room room, int... coordinates) {
        RoomInstance instance = new RoomInstance(room);
        for (int i = 0; i < coordinates.length; i += 2) {
            instance.addCoordinate(new Point(coordinates[i], coordinates[i + 1]));
        }
        IRoomController controller = stub(IRoomController.class, (method, args) -> switch (method) {
            case "getRoom" ->
                room;
            case "getRoomInstance" ->
                instance;
            case "isDungeonHeart" ->
                false;
            case "getMaxCapacity", "getUsedCapacity" ->
                0;
            default ->
                throw new AssertionError(method);
        });
        owner.onBuild(controller);
    }

    private static IPlayerController player(Keeper keeper, PlayerRoomControl rooms, PlayerCreatureControl creatures) {
        return stub(IPlayerController.class, (method, args) -> switch (method) {
            case "getKeeper" ->
                keeper;
            case "getRoomControl" ->
                rooms;
            case "getCreatureControl" ->
                creatures;
            default ->
                throw new AssertionError(method);
        });
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
