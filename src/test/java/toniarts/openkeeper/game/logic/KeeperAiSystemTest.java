/*
 * Copyright (C) 2014-2019 OpenKeeper
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

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.event.EventBus;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CombatPitFighter;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Food;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.KeeperAi;
import toniarts.openkeeper.game.component.KeeperAiDropped;
import toniarts.openkeeper.game.component.KeeperAiEnemy;
import toniarts.openkeeper.game.component.KeeperAiNoPrisoner;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerCreatureControl;
import toniarts.openkeeper.game.controller.player.PlayerDoorControl;
import toniarts.openkeeper.game.controller.player.PlayerHandControl;
import toniarts.openkeeper.game.controller.player.PlayerManaControl;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.controller.player.PlayerSpellControl;
import toniarts.openkeeper.game.controller.player.PlayerTrapControl;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.CombatPitController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.ThreeByThreeController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.RoomWanted;
import toniarts.openkeeper.game.event.ServerEvents;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.AI;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.ai;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.battleSpell;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.creatureWithJobs;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.player;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.withAttackResponse;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.withBattleSpells;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.withBuildAreaEndX;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.withCallToArms;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.withInvasion;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.withMoodCare;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.withNeutralRoomRange;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.withPrisonerFate;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.withSightOfEvil;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.withMoreSpace;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.withTraps;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.withRetreatHealth;
import toniarts.openkeeper.tools.convert.map.CombatExperienceFixtures;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.DoorFixtures;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.GameObjectFixtures;
import toniarts.openkeeper.tools.convert.map.HeroGateFixtures;
import static toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures.variable;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import static toniarts.openkeeper.tools.convert.map.KeeperSpellFixtures.keeperSpell;
import static toniarts.openkeeper.tools.convert.map.PassiveExperienceFixtures.creature;
import static toniarts.openkeeper.tools.convert.map.PayDayFixtures.gameLevel;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import static toniarts.openkeeper.tools.convert.map.RoomFixtures.room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.TrapFixtures;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.flags;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.terrain;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * The computer player on a small map: a 3 x 3 Dungeon Heart (H) with claimed
 * path (.) around it, in rock (#) with a gold seam (G), inside impenetrable
 * rock (X)
 */
class KeeperAiSystemTest {

    private static final short PLAYER_ID = 3;
    private static final short ENEMY_ID = 4;
    private static final short SIGHT_OF_EVIL = 1;
    private static final short CALL_TO_ARMS = 3;
    private static final short CREATE_GOLD = 13;
    private static final short TREASURY = 1;
    private static final short LAIR = 2;
    private static final short PORTAL = 3;
    private static final short HATCHERY = 4;
    private static final short DUNGEON_HEART = 5;
    private static final short LIBRARY = 6;
    private static final short TRAINING_ROOM = 7;
    private static final short GUARD_ROOM = 9;
    private static final short WORKSHOP = 10;
    private static final short PRISON = 11;
    private static final short TORTURE_CHAMBER = 12;
    private static final short TEMPLE = 13;
    private static final short COMBAT_PIT = 16;
    private static final short WOODEN_BRIDGE = 8;
    private static final short STONE_BRIDGE = 17;
    private static final short HERO_PORTAL = CreatureSpawnSystem.HERO_PORTAL_ROOM_ID;
    private static final short HORNY = 9;
    private static final short EATER = 8;
    private static final short THUNDERBOLT = 6;
    private static final short HEAL = 10;
    private static final int ROOM_COST = 10;
    private static final Point HEART = new Point(3, 6);
    /**
     * A fighter isn't dropped on the middle 3x3 of the heart, the AI drops it
     * on the heart's edge
     */
    private static final Point HEART_DROP = new Point(HEART.x + 2, HEART.y);
    /**
     * DK II's reference tile of the 5 x 5 heart round {@link #HEART}, one past
     * its middle. The computer player starts its walks and tunnels there
     */
    private static final Point HEART_REFERENCE = new Point(HEART.x + 1, HEART.y + 1);
    private static final int NORTH = 0;
    private static final int WEST = 1;
    private static final int EAST = 3;

    private final char[][] map = {
        "XXXXXXXXXXXXXXXX".toCharArray(),
        "X##############X".toCharArray(),
        "X##############X".toCharArray(),
        "X##############X".toCharArray(),
        "X.....#########X".toCharArray(),
        "X.HHH.#########X".toCharArray(),
        "X.HHH.####GG###X".toCharArray(),
        "X.HHH.#########X".toCharArray(),
        "X.....#########X".toCharArray(),
        "X##############X".toCharArray(),
        "XXXXXXXXXXXXXXXX".toCharArray()};
    private final int[][] tileHealth = new int[map.length][map[0].length];
    private final Map<Character, Terrain> terrains = Map.of(
            'X', terrain((short) 1, flags(TerrainFlag.SOLID, TerrainFlag.IMPENETRABLE), 0, 0, 0, (short) 0),
            '#', terrain((short) 2, flags(TerrainFlag.SOLID, TerrainFlag.TAGGABLE), 0, 0, 0, (short) 0),
            'G', terrain((short) 3, flags(TerrainFlag.SOLID, TerrainFlag.TAGGABLE), 100, 0, 0, (short) 0),
            '.', terrain((short) 4, flags(TerrainFlag.OWNABLE), 0, 0, 0, (short) 0),
            'H', terrain((short) 5, flags(TerrainFlag.OWNABLE, TerrainFlag.ROOM), 0, 0, 0, (short) 0),
            'P', terrain((short) 6, flags(TerrainFlag.OWNABLE, TerrainFlag.ROOM), 0, 0, 0, (short) 0),
            'E', terrain((short) 7, flags(TerrainFlag.OWNABLE), 0, 0, 0, (short) 0),
            'W', terrain((short) 8, flags(TerrainFlag.WATER), 0, 0, 0, (short) 0),
            'L', terrain((short) 9, flags(TerrainFlag.LAVA), 0, 0, 0, (short) 0),
            'D', terrain((short) 10, flags(), 0, 1, 660, (short) 4));
    private final Set<Point> tagged = new HashSet<>();
    private final Set<Point> taggedHistory = new HashSet<>();
    private final List<Point> built = new ArrayList<>();
    private final List<Short> builtRoomIds = new ArrayList<>();
    private final List<Point> buildAttempts = new ArrayList<>();
    private short rejectedRoomBuildId;
    private int rejectedBuildAttempts;
    private boolean chargeGoldForBuild;

    /**
     * Kept out of the level's room list, which sets the lobby build order
     */
    private final Map<Short, Room> bridges = Map.of(
            WOODEN_BRIDGE, room(WOODEN_BRIDGE, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_WATER, Room.RoomFlag.PLACEABLE_ON_LAVA, Room.RoomFlag.BUILDABLE),
            STONE_BRIDGE, room(STONE_BRIDGE, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_WATER, Room.RoomFlag.PLACEABLE_ON_LAVA, Room.RoomFlag.BUILDABLE));
    private final List<Point> impSpells = new ArrayList<>();

    private final Map<Short, Room> rooms = Map.ofEntries(
            Map.entry(TREASURY, room(TREASURY, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND, Room.RoomFlag.BUILDABLE)),
            Map.entry(LAIR, room(LAIR, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND, Room.RoomFlag.BUILDABLE)),
            Map.entry(PORTAL, room(PORTAL, 0, Room.RoomFlag.PLACEABLE_ON_LAND)),
            Map.entry(PRISON, room(PRISON, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND)),
            Map.entry(HATCHERY, room(HATCHERY, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND, Room.RoomFlag.BUILDABLE)),
            Map.entry(DUNGEON_HEART, room(DUNGEON_HEART, 0, Room.RoomFlag.PLACEABLE_ON_LAND)),
            Map.entry(GUARD_ROOM, room(GUARD_ROOM, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND, Room.RoomFlag.BUILDABLE)),
            Map.entry(LIBRARY, room(LIBRARY, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND)),
            Map.entry(WORKSHOP, room(WORKSHOP, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND)),
            Map.entry(TRAINING_ROOM, room(TRAINING_ROOM, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND, Room.RoomFlag.BUILDABLE)),
            Map.entry(TORTURE_CHAMBER, room(TORTURE_CHAMBER, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND)),
            Map.entry(TEMPLE, room(TEMPLE, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND)),
            Map.entry(COMBAT_PIT, room(COMBAT_PIT, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND)),
            Map.entry(HERO_PORTAL, room(HERO_PORTAL, 0, Room.RoomFlag.PLACEABLE_ON_LAND)));
    private final KeeperSpell createImp = keeperSpell("Create Imp", 100, KeeperSpell.KeeperSpellFlag.CREATE_WORKER);
    private final Creature imp = creature((short) 1);
    private final EntityData entityData = new DefaultEntityData();
    private final IMapController mapController = createMapController();
    private Keeper keeper;
    private PlayerRoomControl roomControl;
    private PlayerCreatureControl creatureControl;
    private double gameTime;
    private boolean logDecisions;
    private AI.AIType lobbyType;
    private PlayerHandControl handControl;
    private PlayerSpellControl spellControl;
    private final KeeperSpell callToArms = keeperSpell(CALL_TO_ARMS, 200, 0, (short) 0, 0);
    private final KeeperSpell sightOfEvil = keeperSpell(SIGHT_OF_EVIL, 100, 0, (short) 0, 0);
    private final List<Point> sightOfEvilCasts = new ArrayList<>();
    private final KeeperSpell createGold = keeperSpell(CREATE_GOLD, 100, 0, (short) 0, 0);
    private final List<Point> createGoldCasts = new ArrayList<>();
    private final Trap sentry = TrapFixtures.trap((short) 3, 100);
    private final Door woodenDoor = DoorFixtures.door((short) 1, 100);
    private final Map<Point, String> placed = new HashMap<>();
    private final List<EntityId> pickedUp = new ArrayList<>();
    private PlayerTrapControl trapControl;
    private PlayerDoorControl doorControl;
    private final List<Point> callToArmsCasts = new ArrayList<>();
    private int callToArmsCancels;
    private final Map<EntityId, Point> dropped = new HashMap<>();
    private final Map<EntityId, EntityId> droppedOn = new HashMap<>();
    private final Map<Point, List<EntityId>> hatcheryFood = new HashMap<>();
    private final List<Point> dropAttempts = new ArrayList<>();
    private boolean rejectPreferredHeartDrop;
    private Room dungeonHeartTileRoom;
    private final List<EntityId> slapped = new ArrayList<>();
    private final List<IRoomController> neutralRooms = new ArrayList<>();
    private final KeeperSpell thunderbolt = battleSpell(THUNDERBOLT, 100, KeeperSpell.KeeperSpellFlag.ATTACKING);
    private final KeeperSpell heal = battleSpell(HEAL, 100, KeeperSpell.KeeperSpellFlag.DEFENSIVE);
    private final Map<Short, EntityId> battleCasts = new HashMap<>();
    private PlayerRoomControl enemyRoomControl;
    private boolean reachable;
    private final Set<Point> closedTiles = new HashSet<>();
    private final List<List<Point>> routesAsked = new ArrayList<>();
    private final Set<Short> fullRooms = new HashSet<>();

    @Test
    void digsToANeutralPortalNearItsRoomsWhenThereIsNoRoomToBuild() {
        KeeperAiSystem system = createSystem(withNeutralRoomRange(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 15, 1), 1000);
        createNeutralPortal(new Point(10, 3));

        Set<Point> dug = new HashSet<>();
        for (int tick = 0; tick < 100 && (tick == 0 || getKeeperAi().task == KeeperAi.Task.DIG_PATH); tick++) {
            system.processTick(0.25f);
            if (tick == 0) {
                assertEquals(KeeperAi.Task.DIG_PATH, getKeeperAi().task);
                assertEquals(new Point(10, 3), getKeeperAi().target);
            }

            // The imps dig out and claim what is tagged
            for (Point p : tagged) {
                map[p.y][p.x] = '.';
            }
            dug.addAll(tagged);
            tagged.clear();
        }

        // From the heart's reference tile to the portal, and that is all
        assertEquals(Set.of(new Point(6, 7), new Point(6, 6), new Point(7, 6), new Point(7, 5), new Point(8, 5), new Point(8, 4), new Point(9, 4), new Point(9, 3)), dug);
        assertEquals(KeeperAi.Task.FINISH, getKeeperAi().task);
    }

    @Test
    void plansAPortalAfterEachRoomSoARoomWithoutASiteDoesNotKeepItFromThePortal() {
        KeeperAiSystem system = createSystem(withNeutralRoomRange(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 15, 1), 1000);
        roomControl.setTypeAvailable(rooms.get(TREASURY), true, true);
        createNeutralPortal(new Point(10, 3));

        system.processTick(0.25f);
        assertEquals(KeeperAi.Task.FIND_ROOM_SITE, getKeeperAi().task);
        assertEquals(TREASURY, getKeeperAi().roomId);

        // The search for a site gave up
        planAgain();
        system.processTick(0.25f);
        assertEquals(KeeperAi.Task.DIG_PATH, getKeeperAi().task);
        assertEquals(new Point(10, 3), getKeeperAi().target);

        planAgain();
        system.processTick(0.25f);
        assertEquals(KeeperAi.Task.FIND_ROOM_SITE, getKeeperAi().task);
        assertEquals(TREASURY, getKeeperAi().roomId);
    }

    @Test
    void looksForASmallerRoomWhenTheSearchForASiteGaveUp() {
        KeeperAiSystem system = createSystem(ai(0, 0, 16, 1, 50, 0, List.of(TREASURY)), 1000);
        roomControl.setTypeAvailable(rooms.get(TREASURY), true, true);

        // Rock that can't be dug leaves no side of the heart 4 by 4 tiles behind the gap, as on Level 14 (#1249)
        for (int y = 1; y <= 9; y++) {
            map[y][9] = 'X';
        }

        // The search gives up on 1 in 20 rounds of the four directions
        for (int tick = 0; tick < 4000 && built.size() < 9; tick++) {
            system.processTick(0.25f);
            if (getKeeperAi().task == KeeperAi.Task.FIND_ROOM_SITE && getKeeperAi().roomSitesNotFound == 0) {
                assertTrue(tagged.isEmpty());
            }
            for (Point p : tagged) {
                map[p.y][p.x] = '.';
            }
            tagged.clear();
        }

        // It gave up on the 16 tiles and built 3 by 3, the next room is looked for at its full size again
        assertEquals(9, built.size());
        assertEquals(0, getKeeperAi().roomSitesNotFound);
    }

    @Test
    void leavesLessGapBetweenRoomsWhenNoSmallestRoomFits() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 2, 30, 0, List.of(TREASURY)), 1000);
        roomControl.setTypeAvailable(rooms.get(TREASURY), true, true);

        // Rock that can't be dug leaves no site with a gap between the rooms, as on Level 14 (#1259)
        for (int i = 1; i <= 9; i++) {
            map[i][8] = 'X';
            map[1][i] = 'X';
        }

        for (int tick = 0; tick < 4000 && built.size() < 9; tick++) {
            system.processTick(0.25f);
            for (Point p : tagged) {
                map[p.y][p.x] = '.';
            }
            tagged.clear();
        }

        // Right next to the heart a 3 by 3 room fits. Its open sides don't count then, as for a computer player that leaves no gap
        assertEquals(9, built.size());
        assertEquals(0, getKeeperAi().roomSitesNotFound);
    }

    @Test
    void digsToAPortalFarFromItsRoomsOnlyFromLandItHasNearThePortal() {
        KeeperAiSystem system = createSystem(withNeutralRoomRange(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 5, 1), 1000);
        createNeutralPortal(new Point(10, 3));

        system.processTick(0.25f);
        assertEquals(KeeperAi.Task.PLAN, getKeeperAi().task);

        // Land of its own two tiles from the portal
        map[3][8] = '.';
        system.processTick(0.25f);
        assertEquals(KeeperAi.Task.DIG_PATH, getKeeperAi().task);
        assertEquals(new Point(8, 3), getKeeperAi().current);
    }

    @Test
    void measuresAPortalFromItsTopLeftTile() {
        KeeperAiSystem system = createSystem(withNeutralRoomRange(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 7, 1), 1000);
        roomControl.onBuild(createRoom(LAIR, new Point(14, 8)));

        // The Lair is the nearest room, 41 from the middle of the Portal but 61 from its top left tile, 49 is in range
        createNeutralPortal(new Point(10, 3));
        system.processTick(0.25f);

        assertEquals(KeeperAi.Task.PLAN, getKeeperAi().task);
    }

    @Test
    void leavesAPortalOutsideItsBuildAreaAlone() {
        KeeperAiSystem system = createSystem(withBuildAreaEndX(withNeutralRoomRange(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 15, 1), 9), 1000);
        createNeutralPortal(new Point(10, 3));

        system.processTick(0.25f);

        assertEquals(KeeperAi.Task.PLAN, getKeeperAi().task);
    }

    @Test
    void digsAPathRoundRockThatCanNotBeDug() {
        KeeperAiSystem system = createSystem(withNeutralRoomRange(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 15, 1), 1000);
        createNeutralPortal(new Point(10, 7));
        map[6][8] = 'X';
        map[7][8] = 'X';
        map[8][8] = 'X';

        Set<Point> dug = new HashSet<>();
        for (int tick = 0; tick < 100 && (tick == 0 || getKeeperAi().task == KeeperAi.Task.DIG_PATH); tick++) {
            system.processTick(0.25f);
            for (Point p : tagged) {
                map[p.y][p.x] = '.';
            }
            dug.addAll(tagged);
            tagged.clear();
        }

        // Both ways round take as much digging, DK II then goes round with the rock on its right
        assertEquals(Set.of(new Point(6, 7), new Point(7, 7), new Point(7, 6), new Point(7, 5), new Point(8, 5), new Point(9, 5), new Point(9, 6), new Point(9, 7)), dug);

        // The Portal behind the last tile is open land, the path ends there
        assertEquals(new Point(9, 7), getKeeperAi().current);
        assertNotEquals(KeeperAi.Task.DIG_PATH, getKeeperAi().task);
    }

    @Test
    void digsNoPathWhereACreatureCanWalkAlready() {
        KeeperAiSystem system = createSystem(withNeutralRoomRange(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 15, 1), 1000);
        createNeutralPortal(new Point(10, 6));
        reachable = true;

        system.processTick(0.25f);
        assertEquals(KeeperAi.Task.DIG_PATH, getKeeperAi().task);
        system.processTick(0.25f);

        assertEquals(KeeperAi.Task.FINISH, getKeeperAi().task);
        assertEquals(new Point(10, 6), getKeeperAi().current);
        assertTrue(tagged.isEmpty());
    }

    @Test
    void walksAcrossTaggableTerrainThatCannotBeDug() {
        KeeperAiSystem system = createSystem(withNeutralRoomRange(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 15, 1), 1000);
        createNeutralPortal(new Point(10, 6));
        map[6][6] = 'D';

        system.processTick(0.25f);

        assertFalse(tagged.contains(new Point(6, 6)));
        assertNotEquals(new Point(6, 6), getKeeperAi().current);
    }

    @Test
    void followsRockThatWallsAPlaceInAllTheWayRoundAndThenGivesUp() {
        KeeperAiSystem system = createSystem(withNeutralRoomRange(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 15, 1), 1000);
        createNeutralPortal(new Point(10, 6));
        for (int y = 1; y < 10; y++) {
            map[y][8] = 'X';
        }

        Set<Point> dug = new HashSet<>();
        for (int tick = 0; tick < 100 && (tick == 0 || getKeeperAi().task == KeeperAi.Task.DIG_PATH); tick++) {
            system.processTick(0.25f);
            for (Point p : tagged) {
                map[p.y][p.x] = '.';
            }
            dug.addAll(tagged);
            tagged.clear();
        }

        // Up along the rock, along the edge of the map and back to where it met the rock
        assertNotEquals(KeeperAi.Task.DIG_PATH, getKeeperAi().task);
        assertTrue(dug.containsAll(Set.of(new Point(7, 5), new Point(7, 1), new Point(1, 1), new Point(1, 9), new Point(7, 9), new Point(7, 7))));
        assertEquals(24, dug.size());
        assertFalse(getKeeperAi().wallFollowing);
    }

    @Test
    void bridgesWaterWithTheWoodenBridgeWhenThatIsTheOnlyBridgeItHas() {
        KeeperAiSystem system = createSystem(withNeutralRoomRange(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 15, 1), 1000);
        roomControl.setTypeAvailable(bridges.get(WOODEN_BRIDGE), true, true);
        createNeutralPortal(new Point(9, 7));
        map[7][7] = 'W';

        digPath(system);

        // The rock up to the water is dug, the water bridged and the rock behind it dug
        assertEquals(List.of(new Point(7, 7)), built);
        assertEquals(List.of(WOODEN_BRIDGE), builtRoomIds);
        assertEquals('.', map[7][8]);
        assertEquals(KeeperAi.Task.FINISH, getKeeperAi().task);
    }

    @Test
    void waitsOnTheTileWhileItIsShortOfGoldForTheBridge() {
        KeeperAiSystem system = createSystem(withNeutralRoomRange(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 15, 1), 1000);
        roomControl.setTypeAvailable(bridges.get(WOODEN_BRIDGE), true, true);
        createNeutralPortal(new Point(9, 7));
        map[7][7] = 'W';
        chargeGoldForBuild = true;
        keeper.setGold(ROOM_COST - 1);

        for (int tick = 0; tick < 20; tick++) {
            system.processTick(0.25f);
            for (Point p : tagged) {
                map[p.y][p.x] = '.';
            }
            tagged.clear();
        }

        // DK II stays on the tile until it can build the bridge
        assertTrue(built.isEmpty());
        assertEquals(KeeperAi.Task.DIG_PATH, getKeeperAi().task);
        assertEquals(new Point(7, 7), getKeeperAi().current);
        assertEquals(0, getKeeperAi().failedTargetTicks[0]);

        keeper.setGold(ROOM_COST);
        for (int tick = 0; tick < 5 && built.isEmpty(); tick++) {
            system.processTick(0.25f);
        }

        assertEquals(List.of(new Point(7, 7)), built);
        assertEquals(List.of(WOODEN_BRIDGE), builtRoomIds);
    }

    @Test
    void buildsABridgeWhenItHasExactlyEnoughGold() {
        KeeperAiSystem system = createSystem(withNeutralRoomRange(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 15, 1), 1000);
        roomControl.setTypeAvailable(bridges.get(WOODEN_BRIDGE), true, true);
        createNeutralPortal(new Point(9, 7));
        map[7][7] = 'W';
        chargeGoldForBuild = true;
        keeper.setGold(ROOM_COST);

        digPath(system);

        assertEquals(List.of(new Point(7, 7)), built);
        assertEquals(List.of(WOODEN_BRIDGE), builtRoomIds);
        assertEquals(0, keeper.getGold());
        assertEquals(KeeperAi.Task.FINISH, getKeeperAi().task);
    }

    @Test
    void staysOnTheTileWhenTheBridgeItPickedIsNotPlacedAndDoesNotTryTheOther() {
        KeeperAiSystem system = createSystem(withNeutralRoomRange(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 15, 1), 1000);
        roomControl.setTypeAvailable(bridges.get(WOODEN_BRIDGE), true, true);
        roomControl.setTypeAvailable(bridges.get(STONE_BRIDGE), true, true);
        createNeutralPortal(new Point(9, 7));
        map[7][7] = 'W';
        rejectedRoomBuildId = WOODEN_BRIDGE;

        for (int tick = 0; tick < 20; tick++) {
            system.processTick(0.25f);
            for (Point p : tagged) {
                map[p.y][p.x] = '.';
            }
            tagged.clear();
        }

        assertTrue(rejectedBuildAttempts > 1);
        assertTrue(built.isEmpty());
        assertEquals(KeeperAi.Task.DIG_PATH, getKeeperAi().task);
        assertEquals(new Point(7, 7), getKeeperAi().current);
        assertEquals(0, getKeeperAi().failedTargetTicks[0]);
    }

    @Test
    void stillTagsTheGoldWhenThePathIsGivenUp() {
        KeeperAiSystem system = createSystem(withNeutralRoomRange(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 15, 1), 1000);
        createNeutralPortal(new Point(9, 7));
        map[7][7] = 'W';
        map[8][9] = 'G';

        system.processTick(0.25f);
        assertEquals(KeeperAi.Task.DIG_PATH, getKeeperAi().task);
        getKeeperAi().nextTask = KeeperAi.Task.TAG_GOLD;

        for (int tick = 0; tick < 100 && getKeeperAi().task == KeeperAi.Task.DIG_PATH; tick++) {
            system.processTick(0.25f);
            for (Point p : tagged) {
                map[p.y][p.x] = '.';
            }
            tagged.clear();
        }
        for (int tick = 0; tick < 10 && getKeeperAi().task == KeeperAi.Task.TAG_GOLD; tick++) {
            system.processTick(0.25f);
        }

        // No bridge to cross the water with, DK II still goes on to the task the path was for
        assertTrue(getKeeperAi().failedTargetTicks[0] > 0);
        assertTrue(taggedHistory.contains(new Point(9, 8)));
    }

    @Test
    void leavesAPortalBehindLavaAloneForAWhileWhenItHasOnlyTheWoodenBridge() {
        KeeperAiSystem system = createSystem(withNeutralRoomRange(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 15, 1), 1000);
        roomControl.setTypeAvailable(bridges.get(WOODEN_BRIDGE), true, true);
        createNeutralPortal(new Point(9, 7));
        map[7][7] = 'L';

        // DK II's computer player bridges lava only with the Stone Bridge
        digPath(system);
        assertTrue(built.isEmpty());
        assertEquals(new Point(7, 7), getKeeperAi().current);

        // The plans after that pass the Portal over, 2 minutes at 4 ticks a second
        for (int tick = 0; tick < 470; tick++) {
            system.processTick(0.25f);
            assertNotEquals(KeeperAi.Task.DIG_PATH, getKeeperAi().task);
        }

        // Then it digs there again, and now it has the Stone Bridge
        roomControl.setTypeAvailable(bridges.get(STONE_BRIDGE), true, true);
        for (int tick = 0; tick < 100 && built.isEmpty(); tick++) {
            system.processTick(0.25f);
        }
        assertEquals(List.of(new Point(7, 7)), built);
        assertEquals(List.of(STONE_BRIDGE), builtRoomIds);
    }

    /**
     * Runs the computer player until the path it starts on the first tick
     * ends, with the imps digging out what is tagged at once
     */
    private void digPath(KeeperAiSystem system) {
        for (int tick = 0; tick < 100 && (tick == 0 || getKeeperAi().task == KeeperAi.Task.DIG_PATH); tick++) {
            system.processTick(0.25f);
            if (tick == 0) {
                assertEquals(KeeperAi.Task.DIG_PATH, getKeeperAi().task);
            }
            for (Point p : tagged) {
                map[p.y][p.x] = '.';
            }
            tagged.clear();
        }
    }

    @Test
    void countsRestedFightersAgainstAnAttackOnItsLandWithoutDroppingThem() {
        EntityId fighter = createCreature(PLAYER_ID, new Point(5, 4), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(createCreature(ENEMY_ID, new Point(6, 4), 100, 100, CreatureState.FIGHT)));
        createCreature(ENEMY_ID, new Point(8, 4), 100, 100, CreatureState.IDLE);
        EntityId horny = createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.IDLE);
        CreatureComponent hornyComponent = new CreatureComponent();
        hornyComponent.creatureId = HORNY;
        entityData.setComponent(horny, hornyComponent);
        EntityId worker = createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.IDLE);
        CreatureComponent workerComponent = new CreatureComponent();
        workerComponent.worker = true;
        entityData.setComponent(worker, workerComponent);
        createCreature(PLAYER_ID, new Point(12, 8), 50, 100, CreatureState.IDLE);
        createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.SLEEPING);
        EntityId first = createCreature(PLAYER_ID, new Point(12, 8), 51, 100, CreatureState.IDLE);
        createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.WORK);
        createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.IDLE);
        createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.IDLE);

        // On the map from before the computer player started, so none of them just came
        List<String> lines = logDecisions(() -> {
            KeeperAiSystem system = createSystem(withAttackResponse(ai(0, 0, 9, 1, 50, 0, List.of()), 175, true, 0), 1000);
            gameTime = 100;

            // 175 % of the enemy's 200 less its own 100 near the fight, and the first one is counted at once
            system.processTick(0.25f);
            assertEquals(1, getKeeperAi().attacks);
            assertEquals(150, getKeeperAi().attackNeeds[0]);
            assertEquals(ENEMY_ID, getKeeperAi().attackerId);
            system.processTick(0.25f);
        });

        // The attack is on its own land, so DK II moves nobody (0x4FE0B0). Not the horny one, the worker, the one at
        // half its health or the sleeper: the first fighter it may take makes up the 150, and as nobody moves it is
        // the one counted again for the next attack
        assertEquals(List.of(first.getId(), first.getId()), countedCreatures(lines));
        assertTrue(dropped.isEmpty());
    }

    @Test
    void dropsAFighterOnItsNearestLandOnlyWhenTheLandOfTheAttackIsLost() {
        EntityId first = createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.IDLE);
        createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.IDLE);
        KeeperAiSystem system = createSystem(withAttackResponse(ai(0, 0, 9, 1, 50, 0, List.of()), 175, true, 0), 1000);
        gameTime = 100;
        system.processTick(0.25f);
        EntityId entityId = entityData.findEntity(null, KeeperAi.class);
        KeeperAi keeperAi = new KeeperAi(entityData.getComponent(entityId, KeeperAi.class));
        keeperAi.attacks = 1;
        keeperAi.attackNeeds[0] = 150;
        keeperAi.attackTiles[0] = new Point(5, 4);
        entityData.setComponent(entityId, keeperAi);
        map[4][5] = 'E';

        // The first goes onto the nearest of its land, which the second then is counted on
        system.processTick(0.25f);
        assertEquals(Map.of(first, new Point(4, 4)), dropped);
        assertEquals(new Point(4, 4), getKeeperAi().attackTiles[0]);
        system.processTick(0.25f);
        assertEquals(Map.of(first, new Point(4, 4)), dropped);
        assertEquals(0, getKeeperAi().attacks);
    }

    /**
     * Runs the computer player with its decision log on
     *
     * @return the lines it logged
     */
    private List<String> logDecisions(Runnable run) {
        List<String> lines = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                lines.add(logRecord.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger logger = Logger.getLogger(KeeperAiSystem.class.getName());
        logger.addHandler(handler);
        try {
            logDecisions = true;
            run.run();
        } finally {
            logger.removeHandler(handler);
        }

        return lines;
    }

    private static List<Long> countedCreatures(List<String> lines) {
        List<Long> counted = new ArrayList<>();
        for (String line : lines) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("count creature (\\d+) against an attack").matcher(line);
            if (matcher.find()) {
                counted.add(Long.valueOf(matcher.group(1)));
            }
        }
        return counted;
    }

    @Test
    void countsNoCreatureThatRestsFleesOrHeedsTheCallToArmsAgainstAnAttackButOneItJustDropped() {
        EntityId fighter = createCreature(PLAYER_ID, new Point(5, 4), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(createCreature(ENEMY_ID, new Point(6, 4), 100, 100, CreatureState.FIGHT)));
        createCreature(ENEMY_ID, new Point(8, 4), 100, 100, CreatureState.IDLE);
        createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.EATING);
        entityData.setComponent(createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.WORK),
                new TaskComponent(1, null, new Point(1, 4), TaskType.COLLECT_PAY, 0, true));
        entityData.setComponent(createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.WORK),
                new TaskComponent(2, null, new Point(1, 4), TaskType.CLAIM_LAIR, 0, true));
        createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.FLEE);
        createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.CALL_TO_ARMS);
        entityData.setComponent(createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.IDLE), new CreatureFall());
        EntityId justDropped = createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.IDLE);
        entityData.setComponent(justDropped, new KeeperAiDropped(90));
        List<String> lines = logDecisions(() -> {
            KeeperAiSystem system = createSystem(withAttackResponse(ai(0, 0, 9, 1, 50, 0, List.of()), 175, true, 0), 1000);
            gameTime = 100;
            for (int tick = 0; tick < 10; tick++) {
                system.processTick(0.25f);
            }
        });

        // DK II's 0x4FE0B0 has no wait after a drop, unlike its other hand work
        assertTrue(countedCreatures(lines).stream().allMatch(id -> id == justDropped.getId()));
        assertFalse(countedCreatures(lines).isEmpty());
        assertTrue(dropped.isEmpty());
    }

    @Test
    void leavesItsCreaturesInAnEnemyPrisonAlone() {
        KeeperAiSystem system = createSystem(withRetreatHealth(ai(4, 10, 9, 1, 50, 0, List.of()), 20), 1000);
        EntityId prisoner = createCreature(PLAYER_ID, new Point(8, 3), 5, 100, CreatureState.IMPRISONED);
        entityData.setComponent(prisoner, new Owner(PLAYER_ID, ENEMY_ID));
        EntityId stunned = createCreature(PLAYER_ID, new Point(8, 3), 5, 100, CreatureState.STUNNED);

        system.processTick(0.25f);

        // The turn went to an imp
        assertTrue(dropped.isEmpty());
        assertEquals(1, impSpells.size());
        assertFalse(pickedUp.contains(stunned));
    }

    @Test
    void healsAHurtCreatureNearAFightWhenItDefends() {
        KeeperAiSystem system = createSystem(withBattleSpells(ai(0, 0, 9, 1, 50, 0, List.of()), 0, 4, 0), 1000);
        spellControl.setTypeAvailable(heal, true, true);
        spellControl.setTypeAvailable(thunderbolt, true, true);
        EntityId fighter = createCreature(PLAYER_ID, new Point(5, 4), 60, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(createCreature(ENEMY_ID, new Point(6, 4), 100, 100, CreatureState.FIGHT)));
        createCreature(PLAYER_ID, new Point(12, 9), 10, 100, CreatureState.IDLE);
        EntityId hurt = createCreature(PLAYER_ID, new Point(4, 4), 49, 100, CreatureState.IDLE);

        system.processTick(0.25f);

        // Not the fighter with more than half its health, nor the one far from the fight
        assertEquals(Map.of(HEAL, hurt), battleCasts);
    }

    @Test
    void healsNoCreatureThatIsKnockedOut() {
        KeeperAiSystem system = createSystem(withBattleSpells(ai(0, 0, 9, 1, 50, 0, List.of()), 0, 4, 0), 1000);
        spellControl.setTypeAvailable(heal, true, true);
        EntityId fighter = createCreature(PLAYER_ID, new Point(5, 4), 60, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(createCreature(ENEMY_ID, new Point(6, 4), 100, 100, CreatureState.FIGHT)));
        EntityId knockedOut = createCreature(PLAYER_ID, new Point(4, 4), 0, 100, CreatureState.UNCONSCIOUS);
        entityData.setComponent(knockedOut, new Unconscious(0));

        system.processTick(0.25f);

        assertTrue(battleCasts.isEmpty());

        // A hurt one still standing gets it
        EntityId hurt = createCreature(PLAYER_ID, new Point(4, 5), 49, 100, CreatureState.IDLE);
        system.processTick(0.25f);

        assertEquals(Map.of(HEAL, hurt), battleCasts);
    }

    @Test
    void strikesAnEnemyNearAFightWhenItAttacksAndTheSpellMakesItsRoll() {
        KeeperAiSystem system = createSystem(withBattleSpells(ai(0, 0, 9, 1, 50, 0, List.of()), 0, 0, 4), 1000);
        spellControl.setTypeAvailable(heal, true, true);
        spellControl.setTypeAvailable(thunderbolt, true, true);
        EntityId fighter = createCreature(PLAYER_ID, new Point(5, 4), 40, 100, CreatureState.FIGHT);
        EntityId enemy = createCreature(ENEMY_ID, new Point(6, 4), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(enemy));
        createCreature(ENEMY_ID, new Point(12, 9), 100, 100, CreatureState.IDLE);

        system.processTick(0.25f);

        assertEquals(Map.of(THUNDERBOLT, enemy), battleCasts);
    }

    @Test
    void castsNoSpellInAFightWhenToldNotToOrWhenTheSpellIsTooCheapForIt() {
        KeeperAiSystem system = createSystem(withBattleSpells(ai(0, 0, 9, 1, 50, 0, List.of()), 4, 0, 4), 1000);
        spellControl.setTypeAvailable(thunderbolt, true, true);
        EntityId fighter = createCreature(PLAYER_ID, new Point(5, 4), 40, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(createCreature(ENEMY_ID, new Point(6, 4), 100, 100, CreatureState.FIGHT)));

        system.processTick(0.25f);
        assertTrue(battleCasts.isEmpty());

        // The die of a spell of 100 mana has two sides, that is never the 4 it wants
        entityData.removeEntity(entityData.findEntity(null, KeeperAi.class));
        system = createSystem(withBattleSpells(ai(0, 0, 9, 1, 50, 0, List.of()), 0, 0, 0), 1000);
        spellControl.setTypeAvailable(thunderbolt, true, true);
        system.processTick(0.25f);
        assertTrue(battleCasts.isEmpty());
    }

    @Test
    void dropsACreatureLeftInItsHandInTheRoomForAJobItLikes() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        roomControl.onBuild(createRoom(HATCHERY, new Point(4, 8)));
        roomControl.onBuild(createRoom(LIBRARY, new Point(2, 4)));
        EntityId creature = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.IDLE);
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = EATER;
        entityData.setComponent(creature, creatureComponent);
        handControl.push(creature, 0.0);

        system.processTick(0.25f);

        // DK II's computer player takes no creature to the Hatchery
        assertEquals(Map.of(creature, new Point(2, 4)), dropped);
    }

    @Test
    void dropsWhatElseIsLeftInItsHandAtTheDungeonHeart() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        roomControl.onBuild(createRoom(LAIR, new Point(2, 4)));
        EntityId creature = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.IDLE);
        handControl.push(creature, 0.0);

        system.processTick(0.25f);

        assertEquals(Map.of(creature, HEART_DROP), dropped);
    }

    @Test
    void dropsCreatureOnNearbyOwnLandWhenThePreferredHeartTileRejectsIt() {
        for (int y = HEART.y - 1; y <= HEART.y + 1; y++) {
            for (int x = HEART.x - 1; x <= HEART.x + 1; x++) {
                map[y][x] = 'H';
            }
        }
        map[HEART_DROP.y][HEART_DROP.x] = 'X';
        dungeonHeartTileRoom = HeroGateFixtures.heroGateRoom(DUNGEON_HEART, "Dungeon Heart", Room.TileConstruction._5_BY_5_ROTATED);
        rejectPreferredHeartDrop = true;
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        EntityId creature = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.IDLE);
        handControl.push(creature, 0.0);

        system.processTick(0.25f);

        assertEquals(HEART_DROP, dropAttempts.get(0));
        assertEquals(2, dropAttempts.size());
        assertNotEquals(HEART, dropped.get(creature));
        assertNull(handControl.peek());
    }

    @Test
    void anAttackToAnswerTakesTheWholeTurn() {
        KeeperAiSystem system = createSystem(withAttackResponse(ai(4, 10, 9, 1, 50, 0, List.of()), 175, true, 0), 1000);
        EntityId fighter = createCreature(PLAYER_ID, new Point(5, 4), 100, 100, CreatureState.WORK);
        createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.IDLE);
        entityData.setComponent(createCreature(ENEMY_ID, new Point(6, 4), 100, 100, CreatureState.FIGHT), new AttackTarget(fighter));

        system.processTick(0.25f);

        // Counted against it where it stands, the attack being on its own land
        assertTrue(dropped.isEmpty());
        assertEquals(0, getKeeperAi().attacks);
        assertTrue(impSpells.isEmpty());
    }

    @Test
    void carriesItsCreaturesHomeFromAFightItIsLosingAwayFromItsLand() {
        KeeperAiSystem system = createSystem(withAttackResponse(ai(0, 0, 9, 1, 50, 0, List.of()), 175, true, 0), 1000);
        EntityId fighter = createCreature(PLAYER_ID, new Point(12, 3), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(createCreature(ENEMY_ID, new Point(13, 3), 100, 100, CreatureState.FIGHT)));
        createCreature(ENEMY_ID, new Point(13, 4), 100, 100, CreatureState.FIGHT);
        EntityId near = createCreature(PLAYER_ID, new Point(10, 3), 100, 100, CreatureState.IDLE);
        EntityId far = createCreature(PLAYER_ID, new Point(12, 9), 100, 100, CreatureState.IDLE);

        system.processTick(0.25f);
        assertEquals(-100, getKeeperAi().attackNeeds[0]);
        for (int tick = 0; tick < 10; tick++) {
            system.processTick(0.25f);
        }

        // Every creature it has within 5 tiles of the fight, the others stay
        assertEquals(Map.of(fighter, HEART_DROP, near, HEART_DROP), dropped);
        assertTrue(!dropped.containsKey(far));
        assertEquals(0, getKeeperAi().attacks);
    }

    @Test
    void leavesAFightAwayFromItsLandAloneWhenItIsNotOutnumberedOrDoesNotPullOut() {
        KeeperAiSystem system = createSystem(withAttackResponse(ai(0, 0, 9, 1, 50, 0, List.of()), 100, true, 0), 1000);
        EntityId fighter = createCreature(PLAYER_ID, new Point(12, 3), 100, 100, CreatureState.FIGHT);
        EntityId enemy = createCreature(ENEMY_ID, new Point(13, 3), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(enemy));

        system.processTick(0.25f);
        assertEquals(0, getKeeperAi().attacks);

        // Outnumbered now, but told not to pull out
        entityData.removeEntity(entityData.findEntity(null, KeeperAi.class));
        system = createSystem(withAttackResponse(ai(0, 0, 9, 1, 50, 0, List.of()), 175, false, 0), 1000);
        system.processTick(0.25f);
        assertEquals(0, getKeeperAi().attacks);
        assertTrue(dropped.isEmpty());
    }

    @Test
    void decidesOnAnAttackForEachEnemyCreatureWhetherToTakeItPrisonerWhenItHasAPrison() {
        KeeperAiSystem system = createSystem(withAttackResponse(ai(0, 0, 9, 1, 50, 0, List.of()), 0, false, 0), 1000);
        EntityId fighter = createCreature(PLAYER_ID, new Point(5, 4), 100, 100, CreatureState.FIGHT);
        EntityId enemy = createCreature(ENEMY_ID, new Point(6, 4), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(enemy));

        // No Prison, no decision
        system.processTick(0.25f);
        assertNull(entityData.getComponent(enemy, KeeperAiNoPrisoner.class));

        // It never takes prisoners
        roomControl.onBuild(createRoom(PRISON, new Point(1, 4)));
        for (int tick = 0; tick < 41; tick++) {
            system.processTick(0.25f);
        }
        assertEquals(1 << PLAYER_ID, entityData.getComponent(enemy, KeeperAiNoPrisoner.class).players);

        // It always takes prisoners
        entityData.removeEntity(entityData.findEntity(null, KeeperAi.class));
        system = createSystem(withAttackResponse(ai(0, 0, 9, 1, 50, 0, List.of()), 0, false, 100), 1000);
        roomControl.onBuild(createRoom(PRISON, new Point(1, 4)));
        system.processTick(0.25f);
        assertEquals(0, entityData.getComponent(enemy, KeeperAiNoPrisoner.class).players);
    }

    private IRoomController createNeutralPortal(Point centre) {
        IRoomController portal = createRoom(PORTAL, centre);
        portal.getRoomInstance().setOwnerId(Player.NEUTRAL_PLAYER_ID);
        neutralRooms.add(portal);
        map[centre.y][centre.x] = 'P';

        return portal;
    }

    private void planAgain() {
        KeeperAi keeperAi = new KeeperAi(getKeeperAi());
        keeperAi.task = KeeperAi.Task.PLAN;
        entityData.setComponent(entityData.findEntity(null, KeeperAi.class), keeperAi);
    }

    @Test
    void carriesACreatureThatIsBadlyHurtInAFightToTheLair() {
        KeeperAiSystem system = createSystem(withRetreatHealth(ai(4, 10, 9, 1, 50, 0, List.of()), 20), 1000);
        roomControl.onBuild(createRoom(LAIR, new Point(2, 4)));
        EntityId hurt = createCreature(PLAYER_ID, new Point(8, 3), 19, 100, CreatureState.FIGHT);
        entityData.setComponent(hurt, new AttackTarget(new EntityId(99)));
        EntityId healthy = createCreature(PLAYER_ID, new Point(8, 3), 20, 100, CreatureState.FIGHT);
        entityData.setComponent(healthy, new AttackTarget(new EntityId(99)));

        system.processTick(0.25f);

        assertEquals(Map.of(hurt, new Point(2, 4)), dropped);

        // That was the action of this tick
        assertTrue(impSpells.isEmpty());
    }

    @Test
    void carriesAHurtCreatureThatIsNotFightingToTheHeartOnlyFromOutsideTheRooms() {
        KeeperAiSystem system = createSystem(withRetreatHealth(ai(0, 0, 9, 1, 50, 0, List.of()), 20), 1000);
        createCreature(PLAYER_ID, new Point(3, 6), 5, 100, CreatureState.IDLE);
        createCreature(PLAYER_ID, new Point(1, 4), 5, 100, CreatureState.SLEEPING);
        createCreature((short) 4, new Point(1, 8), 5, 100, CreatureState.IDLE);

        system.processTick(0.25f);

        assertTrue(dropped.isEmpty());

        EntityId outside = createCreature(PLAYER_ID, new Point(1, 8), 5, 100, CreatureState.IDLE);
        system.processTick(0.25f);

        assertEquals(Map.of(outside, HEART_DROP), dropped);
    }

    @Test
    void leavesAHurtCreatureItJustDroppedAloneForAWhile() {
        EntityId hurt = createCreature(PLAYER_ID, new Point(1, 8), 5, 100, CreatureState.IDLE);
        KeeperAiSystem system = createSystem(withRetreatHealth(ai(0, 0, 9, 1, 50, 0, List.of()), 20), 1000);
        gameTime = 100;
        system.processTick(0.25f);
        dropped.clear();

        // It lands a tick later, then walks out of the heart again
        gameTime = 100.25;
        system.processTick(0.25f);
        entityData.removeComponent(hurt, CreatureFall.class);
        entityData.setComponent(hurt, new Position(0, WorldUtils.pointToVector3f(1, 8)));
        gameTime = 100.5;
        system.processTick(0.25f);
        gameTime = 175.25;
        system.processTick(0.25f);

        assertTrue(dropped.isEmpty());

        gameTime = 175.5;
        system.processTick(0.25f);

        assertEquals(Map.of(hurt, HEART_DROP), dropped);
    }

    @Test
    void leavesACreatureAnyHandDroppedAloneWhileItFallsAndForAWhileAfter() {
        EntityId hurt = createCreature(PLAYER_ID, new Point(1, 8), 5, 100, CreatureState.IDLE);
        KeeperAiSystem system = createSystem(withRetreatHealth(ai(0, 0, 9, 1, 50, 0, List.of()), 20), 1000);

        // Another keeper drops it there
        entityData.setComponent(hurt, new CreatureFall());
        gameTime = 100;
        system.processTick(0.25f);
        entityData.removeComponent(hurt, CreatureFall.class);
        gameTime = 100.25;
        system.processTick(0.25f);
        gameTime = 175;
        system.processTick(0.25f);

        assertTrue(dropped.isEmpty());

        gameTime = 175.25;
        system.processTick(0.25f);

        assertEquals(Map.of(hurt, HEART_DROP), dropped);
    }

    @Test
    void leavesANewCreatureAloneForAWhileButNotInTheFirst75SecondsOfTheGame() {
        KeeperAiSystem system = createSystem(withRetreatHealth(ai(0, 0, 9, 1, 50, 0, List.of()), 20), 1000);
        EntityId early = createCreature(PLAYER_ID, new Point(1, 8), 5, 100, CreatureState.IDLE);
        gameTime = 75;
        system.processTick(0.25f);

        // At the start every creature is new, DK II leaves that out
        assertEquals(Map.of(early, HEART_DROP), dropped);

        // One that arrives later is left alone
        entityData.removeEntity(early);
        dropped.clear();
        gameTime = 100;
        EntityId late = createCreature(PLAYER_ID, new Point(1, 8), 5, 100, CreatureState.IDLE);
        system.processTick(0.25f);
        gameTime = 174.75;
        system.processTick(0.25f);

        assertTrue(dropped.isEmpty());

        gameTime = 175;
        system.processTick(0.25f);

        assertEquals(Map.of(late, HEART_DROP), dropped);
    }

    @Test
    void takesACombatPitFighterOutOfThePitAtHalfItsHealth() {
        lobbyType = AI.AIType.MASTER_KEEPER;
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        creatureControl.onCreatureAdded(new EntityId(50), imp);
        creatureControl.onCreatureAdded(new EntityId(51), imp);
        EntityId fighter = createCreature(PLAYER_ID, new Point(8, 3), 50, 100, CreatureState.COMBAT_PIT);
        entityData.setComponent(fighter, new CombatPitFighter(new EntityId(98)));
        entityData.setComponent(fighter, new CreatureExperience(3, 0, 0, 0, 0));

        system.processTick(0.25f);

        assertTrue(dropped.isEmpty());

        entityData.setComponent(fighter, new Health(49, 100));
        system.processTick(0.25f);

        assertEquals(Map.of(fighter, HEART_DROP), dropped);
    }

    @Test
    void takesACombatPitFighterOutOfThePitAtTheLevelItTrainsTo() {
        lobbyType = AI.AIType.CONQUEROR;
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        creatureControl.onCreatureAdded(new EntityId(50), imp);
        EntityId fighter = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.COMBAT_PIT);
        entityData.setComponent(fighter, new CombatPitFighter(new EntityId(98)));
        entityData.setComponent(fighter, new CreatureExperience(4, 0, 0, 0, 0));

        system.processTick(0.25f);

        assertTrue(dropped.isEmpty());

        // A Conqueror trains its creatures to level 5
        entityData.setComponent(fighter, new CreatureExperience(5, 0, 0, 0, 0));
        system.processTick(0.25f);

        assertEquals(Map.of(fighter, HEART_DROP), dropped);
    }

    @Test
    void castsCallToArmsOnAFighterThatFacesMoreThreatThanItTakesOnWithoutTheFlag() {
        KeeperAiSystem system = createSystem(withCallToArms(ai(0, 0, 9, 1, 50, 0, List.of()), 150, 240, 1, 5, 1, 90), 1000);
        reachable = true;
        spellControl.setTypeAvailable(callToArms, true, true);
        EntityId fighter = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.FIGHT);
        EntityId enemy = createCreature(ENEMY_ID, new Point(9, 3), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(enemy));

        // One enemy is a threat of 100
        gameTime = 10;
        system.processTick(0.25f);

        assertTrue(callToArmsCasts.isEmpty());

        // Only every 10 seconds
        createCreature(ENEMY_ID, new Point(8, 8), 100, 100, CreatureState.IDLE);
        gameTime = 10.25f;
        system.processTick(0.25f);

        assertTrue(callToArmsCasts.isEmpty());

        gameTime = 20;
        system.processTick(0.25f);

        assertEquals(List.of(new Point(8, 3)), callToArmsCasts);
        assertTrue(getKeeperAi().callToArms);
        assertEquals(240, getKeeperAi().callToArmsTicks);

        // One flag at a time
        gameTime = 30;
        system.processTick(0.25f);

        assertEquals(1, callToArmsCasts.size());
        assertEquals(0, callToArmsCancels);
    }

    @Test
    void castsNoCallToArmsOnAFightItsCreaturesCannotWalkTo() {
        KeeperAiSystem system = createSystem(withCallToArms(ai(0, 0, 9, 1, 50, 0, List.of()), 150, 240, 1, 5, 1, 90), 1000);
        spellControl.setTypeAvailable(callToArms, true, true);
        EntityId fighter = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(createCreature(ENEMY_ID, new Point(9, 3), 100, 100, CreatureState.FIGHT)));
        createCreature(ENEMY_ID, new Point(8, 8), 100, 100, CreatureState.IDLE);
        gameTime = 10;
        system.processTick(0.25f);

        assertTrue(callToArmsCasts.isEmpty());

        // The way is asked from the tile 2 east of the heart's centre to the fighter
        reachable = true;
        gameTime = 20;
        system.processTick(0.25f);

        assertTrue(routesAsked.contains(List.of(HEART_DROP, new Point(8, 3))));
        assertEquals(List.of(new Point(8, 3)), callToArmsCasts);
    }

    @Test
    void liftsTheCallToArmsAfterItsTimeAndWaitsBeforeTheNext() {
        KeeperAiSystem system = createSystem(withCallToArms(ai(0, 0, 9, 1, 50, 0, List.of()), 50, 2, 1, 5, 1, 90), 1000);
        reachable = true;
        spellControl.setTypeAvailable(callToArms, true, true);
        EntityId fighter = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(createCreature(ENEMY_ID, new Point(9, 3), 100, 100, CreatureState.FIGHT)));
        gameTime = 10;
        system.processTick(0.25f);

        assertEquals(1, callToArmsCasts.size());

        system.processTick(0.25f);

        assertEquals(0, callToArmsCancels);

        system.processTick(0.25f);

        assertEquals(1, callToArmsCancels);
        assertEquals(360, getKeeperAi().cannotAttackTicks);

        gameTime = 20;
        system.processTick(0.25f);

        assertEquals(1, callToArmsCasts.size());
    }

    @Test
    void liftsTheCallToArmsWhenTheEnemiesAroundTheFlagAreGone() {
        KeeperAiSystem system = createSystem(withCallToArms(ai(0, 0, 9, 1, 50, 0, List.of()), 50, 240, 1, 5, 1, 90), 1000);
        reachable = true;
        spellControl.setTypeAvailable(callToArms, true, true);
        EntityId fighter = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.FIGHT);
        EntityId enemy = createCreature(ENEMY_ID, new Point(9, 3), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(enemy));
        gameTime = 10;
        system.processTick(0.25f);
        system.processTick(0.25f);

        assertEquals(1, callToArmsCasts.size());
        assertEquals(0, callToArmsCancels);

        // 5 tiles away is out of range
        entityData.setComponent(enemy, new Position(0, WorldUtils.pointToVector3f(13, 3)));
        system.processTick(0.25f);

        assertEquals(1, callToArmsCancels);
    }

    @Test
    void liftsTheCallToArmsWhenItHasTooFewFightersLeft() {
        KeeperAiSystem system = createSystem(withCallToArms(ai(0, 0, 9, 1, 50, 0, List.of()), 50, 240, 1, 5, 2, 90), 1000);
        reachable = true;
        spellControl.setTypeAvailable(callToArms, true, true);
        EntityId fighter = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(createCreature(ENEMY_ID, new Point(9, 3), 100, 100, CreatureState.FIGHT)));
        EntityId second = createCreature(PLAYER_ID, new Point(3, 6), 100, 100, CreatureState.IDLE);
        gameTime = 10;
        system.processTick(0.25f);
        system.processTick(0.25f);

        assertEquals(0, callToArmsCancels);

        entityData.removeEntity(second);
        system.processTick(0.25f);

        assertEquals(1, callToArmsCancels);
    }

    @Test
    void looksAtLiftingTheCallToArmsOnlyOnATurnWithoutAnAttackToAnswer() {
        KeeperAiSystem system = createSystem(withCallToArms(ai(0, 0, 9, 1, 50, 0, List.of()), 50, 240, 1, 5, 1, 90), 1000);
        system.processTick(0.25f);
        EntityId entityId = entityData.findEntity(null, KeeperAi.class);
        KeeperAi keeperAi = new KeeperAi(entityData.getComponent(entityId, KeeperAi.class));
        keeperAi.callToArms = true;
        keeperAi.callToArmsTile = new Point(8, 3);
        keeperAi.callToArmsTicks = 240;
        keeperAi.attacks = 1;
        keeperAi.attackNeeds[0] = 100;
        keeperAi.attackTiles[0] = new Point(5, 4);
        entityData.setComponent(entityId, keeperAi);

        // No enemy is near the flag, but the attack takes the whole turn (0x500C20)
        system.processTick(0.25f);

        assertEquals(0, callToArmsCancels);
        assertEquals(0, getKeeperAi().attacks);

        system.processTick(0.25f);

        assertEquals(1, callToArmsCancels);
    }

    @Test
    void createsNoImpsWhileItsCreaturesFight() {
        KeeperAiSystem system = createSystem(ai(4, 10, 9, 1, 50, 0, List.of()), 1000);
        EntityId fighter = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(createCreature(ENEMY_ID, new Point(9, 3), 100, 100, CreatureState.FIGHT)));

        system.processTick(0.25f);

        assertTrue(impSpells.isEmpty());

        entityData.removeComponent(fighter, AttackTarget.class);
        system.processTick(0.25f);

        assertEquals(1, impSpells.size());
    }

    @Test
    void dropsItsCreaturesIntoTheTrainingRoomWhileItsArmyIsNotReady() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        roomControl.onBuild(createRoom(TRAINING_ROOM, new Point(1, 4)));
        EntityId creature = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.IDLE);
        EntityId trainee = createCreature(PLAYER_ID, new Point(8, 4), 100, 100, CreatureState.WORK);
        entityData.setComponent(trainee, new TaskComponent(1, null, new Point(1, 4), TaskType.TRAIN, 0, true));

        // One fighter is an army when it wants more than none
        for (int tick = 0; tick < 60; tick++) {
            system.processTick(0.25f);
        }
        assertTrue(dropped.isEmpty());

        entityData.removeEntity(entityData.findEntity(null, KeeperAi.class));
        system = createSystem(withInvasion(ai(0, 0, 9, 1, 50, 0, List.of()), 5, 1, AI.CallToArmsUsagePolicy.AT_HEART, 0), 1000);
        roomControl.onBuild(createRoom(TRAINING_ROOM, new Point(1, 4)));
        for (int tick = 0; tick < 60 && dropped.isEmpty(); tick++) {
            system.processTick(0.25f);
        }

        // The one in training is left to it
        assertEquals(Map.of(creature, new Point(1, 4)), dropped);
    }

    @Test
    void dropsACreatureNearItsNextLevelIntoTheCombatPitWhenItIsFit() {
        KeeperAiSystem system = createSystem(withInvasion(ai(0, 0, 9, 1, 50, 0, List.of()), 5, 1, AI.CallToArmsUsagePolicy.AT_HEART, 0), 1000);
        roomControl.onBuild(createRoom(TRAINING_ROOM, new Point(1, 4)));
        roomControl.onBuild(createRoom(COMBAT_PIT, new Point(5, 4)));
        EntityId hurt = createCreature(PLAYER_ID, new Point(8, 3), 75, 100, CreatureState.IDLE);
        entityData.setComponent(hurt, new CreatureExperience(2, 751, 1000, 0, 0));
        EntityId fit = createCreature(PLAYER_ID, new Point(8, 4), 76, 100, CreatureState.IDLE);
        entityData.setComponent(fit, new CreatureExperience(2, 751, 1000, 0, 0));
        for (int tick = 0; tick < 60 && dropped.isEmpty(); tick++) {
            system.processTick(0.25f);
        }

        // Neither goes to the Training Room
        assertEquals(Map.of(fit, new Point(5, 4)), dropped);
        for (int tick = 0; tick < 60; tick++) {
            system.processTick(0.25f);
        }
        assertEquals(Map.of(fit, new Point(5, 4)), dropped);
    }

    @Test
    void dropsACreatureOnItsFullPortalToMakeRoomForOneThatResearches() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        roomControl.onBuild(createRoom(LIBRARY, new Point(1, 4)));
        roomControl.onBuild(createRoom(PORTAL, new Point(5, 4)));
        spellControl.setTypeAvailable(heal, true, false);
        EntityId researcher = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.WORK);
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = EATER;
        entityData.setComponent(researcher, creatureComponent);
        EntityId other = createCreature(PLAYER_ID, new Point(8, 4), 100, 100, CreatureState.IDLE);
        creatureControl.onCreatureAdded(researcher, creatureWithJobs(EATER, Creature.JobType.RESEARCH));
        system.processTick(0.25f);

        // There is room for one more
        assertTrue(dropped.isEmpty());

        creatureControl.onCreatureAdded(other, CombatExperienceFixtures.creature((short) 0));
        system.processTick(0.25f);

        assertEquals(Map.of(other, new Point(5, 4)), dropped);
    }

    @Test
    void theCreatureItDropsOnItsPortalToMakeRoomForAResearcherLeavesTheDungeon() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        roomControl.onBuild(createRoom(LIBRARY, new Point(1, 4)));
        ThreeByThreeController portal = createPortal(new Point(11, 2));
        roomControl.onBuild(portal);
        spellControl.setTypeAvailable(heal, true, false);
        EntityId researcher = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.WORK);
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = EATER;
        entityData.setComponent(researcher, creatureComponent);
        EntityId other = createCreature(PLAYER_ID, new Point(8, 4), 100, 100, CreatureState.IDLE);
        creatureControl.onCreatureAdded(researcher, creatureWithJobs(EATER, Creature.JobType.RESEARCH));
        creatureControl.onCreatureAdded(other, CombatExperienceFixtures.creature((short) 0));

        system.processTick(0.25f);

        assertEquals(Set.of(other), dropped.keySet());
        assertTrue(portal.isLeavingDungeon(dropped.get(other), PLAYER_ID, false));
    }

    @Test
    void makesNoRoomForAResearcherWithNothingToResearch() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        roomControl.onBuild(createRoom(LIBRARY, new Point(1, 4)));
        roomControl.onBuild(createRoom(PORTAL, new Point(5, 4)));
        spellControl.setTypeAvailable(heal, true, true);
        EntityId one = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.IDLE);
        EntityId other = createCreature(PLAYER_ID, new Point(8, 4), 100, 100, CreatureState.IDLE);
        creatureControl.onCreatureAdded(one, CombatExperienceFixtures.creature((short) 0));
        creatureControl.onCreatureAdded(other, CombatExperienceFixtures.creature((short) 0));
        system.processTick(0.25f);

        assertTrue(dropped.isEmpty());
    }

    @Test
    void picksUpAnIdleCreatureToResearchWhileThereIsResearchToDo() {
        EntityId creature = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.IDLE);
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = EATER;
        entityData.setComponent(creature, creatureComponent);
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        roomControl.onBuild(createRoom(LIBRARY, new Point(1, 4)));
        spellControl.setTypeAvailable(heal, true, true);
        gameTime = 100;
        for (int tick = 0; tick < 200; tick++) {
            system.processTick(0.25f);
        }

        // Nothing to research
        assertTrue(handControl.isEmpty());
        assertTrue(dropped.isEmpty());

        spellControl.setTypeAvailable(thunderbolt, true, false);
        for (int tick = 0; tick < 200 && dropped.isEmpty(); tick++) {
            system.processTick(0.25f);
        }

        assertEquals(Map.of(creature, new Point(1, 4)), dropped);

        // Still idle, but just dropped: it falls and lands
        dropped.clear();
        system.processTick(0.25f);
        entityData.removeComponent(creature, CreatureFall.class);
        for (int tick = 0; tick < 200; tick++) {
            system.processTick(0.25f);
        }
        assertTrue(dropped.isEmpty());

        gameTime = 175;
        for (int tick = 0; tick < 200 && dropped.isEmpty(); tick++) {
            system.processTick(0.25f);
        }
        assertEquals(Map.of(creature, new Point(1, 4)), dropped);
    }

    @Test
    void slapsAnIdleCreatureWithMoreThanHalfItsHealthNowAndThen() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        EntityId idle = createCreature(PLAYER_ID, new Point(8, 3), 51, 100, CreatureState.IDLE);
        createCreature(PLAYER_ID, new Point(8, 4), 50, 100, CreatureState.IDLE);
        createCreature(PLAYER_ID, new Point(8, 5), 100, 100, CreatureState.WORK);
        entityData.setComponent(createCreature(PLAYER_ID, new Point(8, 6), 100, 100, CreatureState.IDLE), new Slapped(0));
        for (int tick = 0; tick < 2000 && slapped.isEmpty(); tick++) {
            system.processTick(0.25f);
        }

        assertEquals(List.of(idle), slapped);
    }

    @Test
    void slapsNoCreatureThatIsUnhappyOrFleesAsIdle() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        EntityId unhappy = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.IDLE);
        entityData.setComponent(unhappy, new CreatureMood().add(CreatureMood.REASON_NO_PAY, 1000));
        createCreature(PLAYER_ID, new Point(8, 4), 100, 100, CreatureState.FLEE);
        for (int tick = 0; tick < 2000; tick++) {
            system.processTick(0.25f);
        }
        assertTrue(slapped.isEmpty());

        // DK II gives an unhappy creature a status of its own, once content it is idle
        entityData.removeComponent(unhappy, CreatureMood.class);
        for (int tick = 0; tick < 2000 && slapped.isEmpty(); tick++) {
            system.processTick(0.25f);
        }
        assertEquals(List.of(unhappy), slapped);
    }

    @Test
    void takesAnEnemyPrisonerToTheTortureChamberNowAndThen() {
        KeeperAiSystem system = createSystem(withPrisonerFate(ai(0, 0, 9, 1, 50, 0, List.of()), AI.ImprisonedCreatureFatePolicy.TORTURE), 1000);
        roomControl.onBuild(createRoom(PRISON, new Point(1, 4)));
        roomControl.onBuild(createRoom(TORTURE_CHAMBER, new Point(5, 4)));
        EntityId prisoner = createPrisoner(new Point(1, 4));

        // Not one that walks about free in its land
        EntityId free = createPrisoner(new Point(1, 8));
        entityData.removeComponent(free, CreatureImprisoned.class);
        for (int tick = 0; tick < 400 && dropped.isEmpty(); tick++) {
            system.processTick(0.25f);
        }

        assertEquals(Map.of(prisoner, new Point(5, 4)), dropped);
    }

    @Test
    void dropsAnEnemyPrisonerIntoTheCombatPitToFight() {
        KeeperAiSystem system = createSystem(withPrisonerFate(ai(0, 0, 9, 1, 50, 0, List.of()), AI.ImprisonedCreatureFatePolicy.PLACE_IN_PIT), 1000);
        roomControl.onBuild(createRoom(PRISON, new Point(1, 4)));
        RoomInstance pit = new RoomInstance(rooms.get(COMBAT_PIT));
        pit.setOwnerId(PLAYER_ID);
        for (int x = 3; x <= 7; x++) {
            for (int y = 2; y <= 6; y++) {
                pit.addCoordinate(new Point(x, y));
            }
        }
        roomControl.onBuild(new CombatPitController(entityData.createEntity(), entityData, null, pit, null));
        EntityId prisoner = createPrisoner(new Point(1, 4));
        for (int tick = 0; tick < 400 && dropped.isEmpty(); tick++) {
            system.processTick(0.25f);
        }

        // On the pit's reference tile, as DK II, not any of the 9 tiles the creatures fight on
        assertEquals(Map.of(prisoner, new Point(5, 4)), dropped);
    }

    @Test
    void sacrificesAnEnemyPrisonerOrLeavesItToDieByItsSettings() {
        KeeperAiSystem system = createSystem(withPrisonerFate(ai(0, 0, 9, 1, 50, 0, List.of()), AI.ImprisonedCreatureFatePolicy.LEAVE_TO_DIE), 1000);
        roomControl.onBuild(createRoom(PRISON, new Point(1, 4)));
        roomControl.onBuild(createRoom(TEMPLE, new Point(5, 4)));
        EntityId prisoner = createPrisoner(new Point(1, 4));
        for (int tick = 0; tick < 400; tick++) {
            system.processTick(0.25f);
        }
        assertTrue(dropped.isEmpty());

        entityData.removeEntity(entityData.findEntity(null, KeeperAi.class));
        system = createSystem(withPrisonerFate(ai(0, 0, 9, 1, 50, 0, List.of()), AI.ImprisonedCreatureFatePolicy.SACRIFICE), 1000);
        roomControl.onBuild(createRoom(PRISON, new Point(1, 4)));
        roomControl.onBuild(createRoom(TEMPLE, new Point(5, 4)));
        for (int tick = 0; tick < 400 && dropped.isEmpty(); tick++) {
            system.processTick(0.25f);
        }

        assertEquals(Map.of(prisoner, new Point(5, 4)), dropped);
    }

    @Test
    void dropsItsPayOnACreatureThatTurnsUnhappyOverItsPayOnce() {
        KeeperAiSystem system = createSystem(withMoodCare(ai(0, 0, 9, 1, 50, 0, List.of()), true, false, false), 1000);
        roomControl.onBuild(createRoom(TREASURY, new Point(1, 4)));
        EntityId creature = createUnhappyCreature(CreatureMood.REASON_NO_PAY, 1000);
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = EATER;
        creatureComponent.pay = 150;
        entityData.setComponent(creature, creatureComponent);

        system.processTick(0.25f);

        assertEquals(850, keeper.getGold());
        assertEquals(1, droppedOn.size());
        EntityId gold = droppedOn.keySet().iterator().next();
        assertEquals(150, entityData.getComponent(gold, Gold.class).gold);
        assertEquals(creature, droppedOn.get(gold));
        assertEquals(new Point(8, 3), dropped.get(gold));

        // DK II's computer player hears of it once, when the mood turns
        system.processTick(0.25f);
        assertEquals(850, keeper.getGold());

        // Happy and then unhappy again is news again
        entityData.setComponent(creature, new CreatureMood());
        system.processTick(0.25f);
        entityData.setComponent(creature, new CreatureMood().add(CreatureMood.REASON_GENERAL, 1000));
        system.processTick(0.25f);
        assertEquals(700, keeper.getGold());
    }

    @Test
    void dropsNoPayWithoutATreasuryOrWhenItDoesNotLookAfterTheUnhappy() {
        KeeperAiSystem system = createSystem(withMoodCare(ai(0, 0, 9, 1, 50, 0, List.of()), true, false, false), 1000);
        createUnhappyCreature(CreatureMood.REASON_NO_PAY, 100);
        system.processTick(0.25f);
        assertTrue(dropped.isEmpty());

        entityData.removeEntity(entityData.findEntity(null, KeeperAi.class));
        system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        roomControl.onBuild(createRoom(TREASURY, new Point(1, 4)));
        createUnhappyCreature(CreatureMood.REASON_NO_PAY, 100);
        system.processTick(0.25f);
        assertTrue(dropped.isEmpty());
    }

    @Test
    void dropsAChickenFromItsHatcheryOnAHungryUnhappyCreature() {
        KeeperAiSystem system = createSystem(withMoodCare(ai(0, 0, 9, 1, 50, 0, List.of()), true, false, false), 1000);
        roomControl.onBuild(createRoom(HATCHERY, new Point(1, 4)));
        EntityId chicken = createObject((short) 1, new Point(1, 4));
        entityData.setComponent(chicken, new Food());
        hatcheryFood.put(new Point(1, 4), List.of(chicken));
        EntityId creature = createUnhappyCreature(CreatureMood.REASON_NO_FOOD, 100);

        system.processTick(0.25f);

        assertEquals(Map.of(chicken, new Point(8, 3)), dropped);
        assertEquals(Map.of(chicken, creature), droppedOn);
    }

    @Test
    void carriesAnUnhappyCreatureWithoutALairToItsLair() {
        KeeperAiSystem system = createSystem(withMoodCare(ai(0, 0, 9, 1, 50, 0, List.of()), true, false, false), 1000);
        roomControl.onBuild(createRoom(LAIR, new Point(1, 4)));
        EntityId creature = createUnhappyCreature(CreatureMood.REASON_NO_LAIR, 100);

        system.processTick(0.25f);

        assertEquals(Map.of(creature, new Point(1, 4)), dropped);
    }

    @Test
    void carriesAnUnhappyCreatureWithoutWorkToTheRoomForAJobItLikesUnlessItJustDroppedIt() {
        EntityId justDropped = createUnhappyCreature(CreatureMood.REASON_NO_WORK, 1000);
        EntityId creature = createUnhappyCreature(CreatureMood.REASON_NO_WORK, 1000);
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = EATER;
        entityData.setComponent(justDropped, creatureComponent);
        entityData.setComponent(creature, creatureComponent);
        KeeperAiSystem system = createSystem(withMoodCare(ai(0, 0, 9, 1, 50, 0, List.of()), true, false, false), 1000);
        roomControl.onBuild(createRoom(LIBRARY, new Point(1, 4)));
        entityData.setComponent(justDropped, new KeeperAiDropped(30));
        gameTime = 100;

        system.processTick(0.25f);

        assertEquals(Map.of(creature, new Point(1, 4)), dropped);
    }

    @Test
    void getsRidOfAnAngryCreatureInItsTempleOrElseOnAPortalWhenItsSettingsSaySo() {
        KeeperAiSystem system = createSystem(withMoodCare(ai(0, 0, 9, 1, 50, 0, List.of()), true, true, true), 1000);
        roomControl.onBuild(createRoom(PORTAL, new Point(1, 8)));
        EntityId creature = createUnhappyCreature(CreatureMood.REASON_NO_PAY, 40000);

        system.processTick(0.25f);

        assertEquals(Map.of(creature, new Point(1, 8)), dropped);

        // Calming down to unhappy is no news, turning angry again is
        dropped.clear();
        entityData.setComponent(creature, new CreatureMood().add(CreatureMood.REASON_NO_PAY, 100));
        system.processTick(0.25f);
        assertTrue(dropped.isEmpty());

        roomControl.onBuild(createRoom(TEMPLE, new Point(5, 4)));
        entityData.setComponent(creature, new CreatureMood().add(CreatureMood.REASON_NO_PAY, 40000));
        gameTime = 100;
        system.processTick(0.25f);
        assertEquals(Map.of(creature, new Point(5, 4)), dropped);
    }

    @Test
    void theAngryCreatureItDropsOnItsPortalLeavesTheDungeon() {
        KeeperAiSystem system = createSystem(withMoodCare(ai(0, 0, 9, 1, 50, 0, List.of()), true, true, true), 1000);
        ThreeByThreeController portal = createPortal(new Point(11, 2));
        roomControl.onBuild(portal);
        EntityId creature = createUnhappyCreature(CreatureMood.REASON_NO_PAY, 40000);

        system.processTick(0.25f);

        assertEquals(Set.of(creature), dropped.keySet());
        assertTrue(portal.isLeavingDungeon(dropped.get(creature), PLAYER_ID, false));
    }

    @Test
    void makesAnAngryCreatureHappyLikeAnUnhappyOneWhenItDoesNotGetRidOfThem() {
        KeeperAiSystem system = createSystem(withMoodCare(ai(0, 0, 9, 1, 50, 0, List.of()), false, true, false), 1000);
        roomControl.onBuild(createRoom(LAIR, new Point(1, 4)));
        roomControl.onBuild(createRoom(PORTAL, new Point(1, 8)));
        createUnhappyCreature(CreatureMood.REASON_NO_LAIR, 100);
        EntityId angry = createUnhappyCreature(CreatureMood.REASON_NO_LAIR, 40000);

        system.processTick(0.25f);

        assertEquals(Map.of(angry, new Point(1, 4)), dropped);
    }

    /**
     * A working creature of the computer player at (8, 3) with one reason
     * for its anger. The creature type with jobs is unhappy from an anger
     * of 327, the others from any
     */
    private EntityId createUnhappyCreature(int reason, int anger) {
        EntityId creature = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.WORK);
        entityData.setComponent(creature, new CreatureMood().add(reason, anger));

        return creature;
    }

    @Test
    void castsSightOfEvilOnEnemyLandNowAndThenOnceItHasTheSpell() {
        for (int y = 1; y < 10; y++) {
            for (int x = 7; x < 15; x++) {
                map[y][x] = 'E';
            }
        }
        KeeperAiSystem system = createSystem(withSightOfEvil(ai(0, 0, 9, 1, 50, 0, List.of()), AI.SightOfEvilUsagePolicy.ENEMY_LAND), 1000);
        for (int tick = 0; tick < 200; tick++) {
            system.processTick(0.25f);
        }
        assertTrue(sightOfEvilCasts.isEmpty());

        spellControl.setTypeAvailable(sightOfEvil, true, true);
        for (int tick = 0; tick < 200; tick++) {
            system.processTick(0.25f);
            keeper.setMana(1000);
        }

        assertFalse(sightOfEvilCasts.isEmpty());
        for (Point tile : sightOfEvilCasts) {
            assertEquals('E', map[tile.y][tile.x]);
        }
    }

    @Test
    void placesTrapsAndDoorsInTheCorridorsOnTheEdgeOfABoxRoundItsRooms() {
        for (int y = 1; y < 10; y++) {
            map[y][8] = '.';
        }
        KeeperAiSystem system = createSystem(withTraps(ai(0, 0, 9, 1, 50, 0, List.of()), 100, 0, 2), 1000000);
        trapControl.setTypeAvailable(sentry, true, true);
        for (int tick = 0; tick < 3000; tick++) {
            system.processTick(0.25f);
        }

        // Not without a Workshop
        assertTrue(placed.isEmpty());

        roomControl.onBuild(createRoom(WORKSHOP, HEART));
        for (int tick = 0; tick < 3000 && placed.size() < 3; tick++) {
            system.processTick(0.25f);
        }

        // Only in the corridor, three tiles apart, and traps when it prefers them
        assertEquals(3, placed.size());
        for (Point tile : placed.keySet()) {
            assertEquals(8, tile.x);
            assertEquals("placeTrap", placed.get(tile));
            for (Point other : placed.keySet()) {
                assertTrue(tile == other || Math.abs(tile.y - other.y) >= 3);
            }
        }
    }

    @Test
    void usesTheLastSpecialLyingOnItsLandButNotEverySpecial() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        createObject((short) 1, new Point(1, 4));
        createObject((short) 118, new Point(8, 3));
        EntityId killCreatures = createObject((short) 127, new Point(1, 8));
        system.processTick(0.25f);

        // The last it finds is one it leaves alone
        assertTrue(pickedUp.isEmpty());

        entityData.removeEntity(killCreatures);
        EntityId revealMap = createObject((short) 118, new Point(1, 8));
        system.processTick(0.25f);

        assertEquals(List.of(revealMap), pickedUp);
    }

    private EntityId createObject(short objectId, Point tile) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponents(entityId, new ObjectComponent(objectId, null), new Position(0, WorldUtils.pointToVector3f(tile.x, tile.y)));

        return entityId;
    }

    private EntityId createPrisoner(Point tile) {
        EntityId prisoner = createCreature(ENEMY_ID, tile, 100, 100, CreatureState.IMPRISONED);
        entityData.setComponents(prisoner, new Owner(ENEMY_ID, PLAYER_ID), new CreatureImprisoned(0, 0));

        return prisoner;
    }

    private EntityId createCreature(short playerId, Point tile, int health, int maxHealth, CreatureState state) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponents(entityId, new CreatureComponent(), new Owner(playerId, playerId), new Health(health, maxHealth),
                new CreatureAi(0, state, (short) 2), new Position(0, WorldUtils.pointToVector3f(tile.x, tile.y)),
                new Threat(100));

        return entityId;
    }

    @Test
    void aComputerPlayerSetUpInTheLobbyPlaysByItsTypeAndNotByTheLevel() {
        lobbyType = AI.AIType.MASTER_KEEPER;
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);

        // The level says no imps, a Master Keeper wants one for every 15 of its 25 tiles
        system.processTick(0.25f);

        assertEquals(1, impSpells.size());
    }

    @Test
    void aPsychoticWantsMoreLandForAnImpThanAMasterKeeper() {
        lobbyType = AI.AIType.PSYCHOTIC;
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);

        system.processTick(0.25f);

        assertTrue(impSpells.isEmpty());
    }

    @Test
    void aComputerPlayerTypeStartsWithSomeMana() {
        lobbyType = AI.AIType.PARANOID;
        createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);

        assertEquals(1100, keeper.getMana());
    }

    @Test
    void logsWhatItDecidesWithTheTickAndTheReasonWhenTheLogIsOn() {
        List<String> lines = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                lines.add(logRecord.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger logger = Logger.getLogger(KeeperAiSystem.class.getName());
        logger.addHandler(handler);
        try {
            logDecisions = true;
            gameTime = 30;
            KeeperAiSystem system = createSystem(ai(4, 10, 9, 1, 50, 0, List.of(TREASURY)), 1000);
            roomControl.setTypeAvailable(rooms.get(TREASURY), true, true);
            creatureControl.onCreatureAdded(new EntityId(1), imp);
            system.processTick(0.25f);

            // The second imp, at 4 ticks a second
            assertEquals(1, lines.size());
            assertTrue(lines.getFirst().matches("AI tick 120 player 3 PLAN: cast Create Imp at \\d+,\\d+ \\(1 imps, .*\\)"), lines.getFirst());

            creatureControl.onCreatureAdded(new EntityId(2), imp);
            system.processTick(0.25f);

            assertEquals(2, lines.size());
            assertEquals("AI tick 120 player 3 PLAN: next FIND_ROOM_SITE at 4,7 (room 1 is next, looking west)", lines.getLast());
        } finally {
            logger.removeHandler(handler);
        }
    }

    @Test
    void logsWhyItPlansNothingWhenItStartsToAndNotOnEveryTick() {
        List<String> lines = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                lines.add(logRecord.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger logger = Logger.getLogger(KeeperAiSystem.class.getName());
        logger.addHandler(handler);
        try {
            logDecisions = true;
            gameTime = 30;
            KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
            system.processTick(0.25f);
            system.processTick(0.25f);

            assertEquals(List.of("AI tick 120 player 3 PLAN: nothing to do (no room left to build, no neutral Portal to dig to, 1000 gold, not ready to invade)"), lines);
        } finally {
            logger.removeHandler(handler);
        }
    }

    @Test
    void logsNothingWhenTheLogIsOff() {
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                records.add(logRecord);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger logger = Logger.getLogger(KeeperAiSystem.class.getName());
        logger.addHandler(handler);
        try {
            KeeperAiSystem system = createSystem(ai(4, 10, 9, 1, 50, 0, List.of(TREASURY)), 1000);
            creatureControl.onCreatureAdded(new EntityId(1), imp);
            system.processTick(0.25f);

            assertEquals(1, impSpells.size());
            assertTrue(records.isEmpty());
        } finally {
            logger.removeHandler(handler);
        }
    }

    @Test
    void createsAnImpOnACornerOfTheHeartWhenItsLandAllowsMore() {
        KeeperAiSystem system = createSystem(ai(4, 10, 9, 1, 50, 0, List.of()), 1000);
        creatureControl.onCreatureAdded(new EntityId(1), imp);

        // 25 owned tiles at 10 tiles an imp allow 2 imps
        system.processTick(0.25f);

        assertEquals(1, impSpells.size());
        assertEquals(2, Math.abs(impSpells.getFirst().x - HEART.x));
        assertEquals(2, Math.abs(impSpells.getFirst().y - HEART.y));
    }

    @Test
    void doesNotTryToCreateAnImpBeforeTheSpellIsAvailable() {
        KeeperAiSystem system = createSystem(ai(4, 10, 9, 1, 50, 0, List.of()), 1000);
        creatureControl.onCreatureAdded(new EntityId(1), imp);
        spellControl.setTypeAvailable(createImp, false, false);

        system.processTick(0.25f);

        assertTrue(impSpells.isEmpty());
    }

    @Test
    void digsATunnelToTheEnemyHeartOnceItHasMoreFightersThanItsSettingsAskFor() {
        KeeperAiSystem system = createSystem(withInvasion(ai(0, 0, 9, 1, 50, 0, List.of()), 1, 50, AI.CallToArmsUsagePolicy.AT_HEART, 0), 1000);
        enemyRoomControl.onBuild(createRoom(DUNGEON_HEART, new Point(13, 6)));
        createCreature(PLAYER_ID, new Point(1, 4), 100, 100, CreatureState.WORK);

        system.processTick(0.25f);

        // One fighter is not more than the setting of one
        assertEquals(KeeperAi.Task.PLAN, getKeeperAi().task);

        createCreature(PLAYER_ID, new Point(1, 8), 100, 100, CreatureState.WORK);
        system.processTick(0.25f);

        // From its own heart to the enemy's, both their reference tiles, by the settings
        assertEquals(KeeperAi.Task.DIG_PATH, getKeeperAi().task);
        assertEquals(HEART_REFERENCE, getKeeperAi().current);
        assertEquals(new Point(14, 7), getKeeperAi().target);
        assertEquals(ENEMY_ID, getKeeperAi().invasionTargetId);

        // DK II keeps 4 bits of the 50 tunnels, so 2, one of them dug now
        assertEquals(1, getEnemy().tunnelsLeft);
        assertTrue(getEnemy().invaded);
    }

    @Test
    void neverPicksTheHeroesAsTheEnemyToInvade() {
        KeeperAiSystem system = createSystem(withInvasion(ai(0, 0, 9, 1, 50, 0, List.of()), 1, 50,
                AI.CallToArmsUsagePolicy.AT_HEART, 0), 1000, Player.GOOD_PLAYER_ID);
        enemyRoomControl.onBuild(createRoom(DUNGEON_HEART, new Point(11, 6)));
        enemyRoomControl.onBuild(createRoom(HERO_PORTAL, new Point(13, 6)));
        createCreature(PLAYER_ID, new Point(1, 4), 100, 100, CreatureState.WORK);
        createCreature(PLAYER_ID, new Point(1, 8), 100, 100, CreatureState.WORK);

        system.processTick(0.25f);

        // DK II's enemy loop skips player 1, even with a heart
        assertNotEquals(KeeperAi.Task.DIG_PATH, getKeeperAi().task);
        assertEquals(0, getKeeperAi().invasionTargetId);
        assertNull(getEnemy(Player.GOOD_PLAYER_ID));
    }

    @Test
    void doesNotInvadeAKeeperWhoseDungeonHeartIsDestroyed() {
        KeeperAiSystem system = createSystem(withInvasion(ai(0, 0, 9, 1, 50, 0, List.of()), 1, 50,
                AI.CallToArmsUsagePolicy.AT_HEART, 0), 1000);
        IRoomController heart = createRoom(DUNGEON_HEART, new Point(11, 6));
        heart.getRoomInstance().setDestroyed(true);
        enemyRoomControl.onBuild(heart);
        enemyRoomControl.onBuild(createRoom(TREASURY, new Point(13, 6)));
        createCreature(PLAYER_ID, new Point(1, 4), 100, 100, CreatureState.WORK);
        createCreature(PLAYER_ID, new Point(1, 8), 100, 100, CreatureState.WORK);

        system.processTick(0.25f);

        assertNotEquals(KeeperAi.Task.DIG_PATH, getKeeperAi().task);
        assertEquals(0, getKeeperAi().invasionTargetId);
        assertNull(getEnemy());
    }

    @Test
    void stopsTheTunnelAtTheEnemysLandAndBreaksThroughWhenAllTunnelsAreDug() {
        KeeperAiSystem system = createSystem(withInvasion(ai(0, 0, 9, 1, 50, 0, List.of()), 0, 1, AI.CallToArmsUsagePolicy.AT_HEART, 5), 1000);
        for (int y = 4; y <= 8; y++) {
            for (int x = 12; x <= 14; x++) {
                map[y][x] = 'E';
            }
        }
        enemyRoomControl.onBuild(createRoom(DUNGEON_HEART, new Point(13, 6)));
        createCreature(PLAYER_ID, new Point(1, 4), 100, 100, CreatureState.WORK);

        for (int tick = 0; tick < 100 && (getEnemy() == null || getEnemy().breachTiles[0] == null); tick++) {
            system.processTick(0.25f);

            // The imps dig out what is tagged
            for (Point p : tagged) {
                map[p.y][p.x] = '.';
            }
            tagged.clear();
        }

        // The tunnel along the row of the heart's reference tile stops next to the enemy's land, that tile is not dug
        assertEquals(new Point(11, 7), getEnemy().breachTiles[0]);
        assertEquals(new Point(14, 7), getEnemy().breachTargets[0]);
        assertEquals('#', map[7][11]);
        assertEquals(0, getKeeperAi().invasionTargetId);

        // That was its only tunnel, so it breaks through and then waits
        for (int tick = 0; tick < 10 && getEnemy().breachTiles[0] != null; tick++) {
            system.processTick(0.25f);
        }

        assertEquals(KeeperAi.Task.DIG_PATH, getKeeperAi().task);
        assertEquals(new Point(11, 7), getKeeperAi().current);
        assertEquals(new Point(14, 7), getKeeperAi().target);
        assertEquals(0, getKeeperAi().invasionTargetId);
        assertEquals(5 * 4, getKeeperAi().cannotAttackTicks);
    }

    @Test
    void callsItsCreaturesToArmsAtTheEnemyHeartOnceTheyCanWalkThereAndDropsHalfItsThreat() {
        KeeperAiSystem system = createSystem(withInvasion(ai(0, 0, 9, 1, 50, 0, List.of()), 5, 1,
                AI.CallToArmsUsagePolicy.AT_HEART_WITH_CREATURES, 0), 1000);
        spellControl.setTypeAvailable(callToArms, true, true);
        enemyRoomControl.onBuild(createRoom(DUNGEON_HEART, new Point(7, 6)));
        KeeperAiEnemy invaded = new KeeperAiEnemy(PLAYER_ID, ENEMY_ID, 0);
        invaded.invaded = true;
        entityData.setComponent(entityData.createEntity(), invaded);
        createCreature(PLAYER_ID, new Point(1, 4), 100, 100, CreatureState.WORK);
        createCreature(PLAYER_ID, new Point(1, 8), 100, 100, CreatureState.WORK);

        system.processTick(0.25f);

        assertTrue(callToArmsCasts.isEmpty());

        reachable = true;
        system.processTick(0.25f);

        // 2 tiles from the enemy heart's reference tile (8, 7) on both axes
        assertEquals(1, callToArmsCasts.size());
        assertEquals(2, Math.abs(callToArmsCasts.getFirst().x - 8));
        assertEquals(2, Math.abs(callToArmsCasts.getFirst().y - 7));
        assertTrue(getKeeperAi().callToArms);

        // Its land near the flag gets half of its threat of 200
        assertEquals(1, getKeeperAi().attacks);
        assertEquals(100, getKeeperAi().attackNeeds[0]);
    }

    @Test
    void callsItsCreaturesToArmsInTheMiddleOfAWalledInEnemyHeartOnceTheyCanWalkToTheHeart() {
        KeeperAiSystem system = createSystem(withInvasion(ai(0, 0, 9, 1, 50, 0, List.of()), 5, 1,
                AI.CallToArmsUsagePolicy.AT_HEART, 0), 1000);
        spellControl.setTypeAvailable(callToArms, true, true);
        enemyRoomControl.onBuild(createRoom(DUNGEON_HEART, new Point(7, 6)));
        KeeperAiEnemy invaded = new KeeperAiEnemy(PLAYER_ID, ENEMY_ID, 0);
        invaded.invaded = true;
        entityData.setComponent(entityData.createEntity(), invaded);
        createCreature(PLAYER_ID, new Point(1, 4), 100, 100, CreatureState.WORK);
        reachable = true;

        // The heart's middle and the walls round it can't be walked to, so of
        // DK II's four tiles 2 from the reference tile (8, 7) only (6, 5), in
        // the heart's middle, is left
        for (int x = 4; x <= 10; x++) {
            for (int y = 3; y <= 9; y++) {
                if (Math.max(Math.abs(x - 7), Math.abs(y - 6)) != 2) {
                    closedTiles.add(new Point(x, y));
                }
            }
        }

        for (int tick = 0; tick < 100 && callToArmsCasts.isEmpty(); tick++) {
            system.processTick(0.25f);
        }

        // The flag goes on DK II's tile, the way is asked to the nearest tile
        // of the heart a creature can stand on
        assertEquals(List.of(new Point(6, 5)), callToArmsCasts);
        assertTrue(routesAsked.stream().anyMatch(route -> route.get(1).equals(new Point(6, 4))));
        assertTrue(getKeeperAi().callToArms);
    }

    @Test
    void callsItsCreaturesToArmsAtAHostileRoomWhenThereIsNoEnemyHeart() {
        KeeperAiSystem system = createSystem(withInvasion(ai(0, 0, 9, 1, 50, 0, List.of()), 5, 1,
                AI.CallToArmsUsagePolicy.AT_HEART, 0), 1000);
        spellControl.setTypeAvailable(callToArms, true, true);
        enemyRoomControl.onBuild(createRoom(HERO_PORTAL, new Point(13, 6)));
        KeeperAiEnemy invaded = new KeeperAiEnemy(PLAYER_ID, ENEMY_ID, 0);
        invaded.invaded = true;
        entityData.setComponent(entityData.createEntity(), invaded);
        createCreature(PLAYER_ID, new Point(1, 4), 100, 100, CreatureState.WORK);
        reachable = true;

        system.processTick(0.25f);

        assertEquals(List.of(new Point(13, 6)), callToArmsCasts);
        assertTrue(getKeeperAi().callToArms);
    }

    @Test
    void goesOnPlanningRoomsWhileItInvadesWithoutCallToArms() {
        KeeperAiSystem system = createSystem(withInvasion(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 5, 1,
                AI.CallToArmsUsagePolicy.AT_HEART_WITH_CREATURES, 0), 1000);
        roomControl.setTypeAvailable(rooms.get(TREASURY), true, true);
        enemyRoomControl.onBuild(createRoom(DUNGEON_HEART, new Point(8, 6)));
        KeeperAiEnemy invaded = new KeeperAiEnemy(PLAYER_ID, ENEMY_ID, 0);
        invaded.invaded = true;
        entityData.setComponent(entityData.createEntity(), invaded);
        createCreature(PLAYER_ID, new Point(1, 4), 100, 100, CreatureState.WORK);
        reachable = true;

        system.processTick(0.25f);

        // Call to Arms is still to be researched, so the turn goes to the next room
        assertTrue(callToArmsCasts.isEmpty());
        assertEquals(TREASURY, getKeeperAi().roomId);
        assertEquals(KeeperAi.Task.FIND_ROOM_SITE, getKeeperAi().task);
    }

    @Test
    void movesTheFlagOnTowardsTheEnemyHeartOnceItsCreaturesHaveCome() {
        KeeperAiSystem system = createSystem(withInvasion(ai(0, 0, 9, 1, 50, 0, List.of()), 5, 1,
                AI.CallToArmsUsagePolicy.TOWARDS_HEART, 0), 1000);
        spellControl.setTypeAvailable(callToArms, true, true);
        enemyRoomControl.onBuild(createRoom(DUNGEON_HEART, new Point(13, 6)));
        KeeperAiEnemy invaded = new KeeperAiEnemy(PLAYER_ID, ENEMY_ID, 0);
        invaded.invaded = true;
        entityData.setComponent(entityData.createEntity(), invaded);
        createCreature(PLAYER_ID, new Point(4, 4), 100, 100, CreatureState.WORK);
        createCreature(PLAYER_ID, new Point(4, 8), 100, 100, CreatureState.WORK);
        reachable = true;
        system.processTick(0.25f);
        EntityId entityId = entityData.findEntity(null, KeeperAi.class);
        KeeperAi keeperAi = new KeeperAi(entityData.getComponent(entityId, KeeperAi.class));
        keeperAi.callToArms = true;
        keeperAi.callToArmsTile = new Point(5, 6);
        keeperAi.callToArmsTicks = 100;
        keeperAi.rallyTile = new Point(5, 6);
        entityData.setComponent(entityId, keeperAi);

        system.processTick(0.25f);

        // Both creatures are near the flag, so it goes 2 tiles on
        assertEquals(1, callToArmsCasts.size());
        Point moved = callToArmsCasts.getFirst();
        assertEquals(2, Math.abs(moved.x - 5) + Math.abs(moved.y - 6));
        assertEquals(moved, getKeeperAi().rallyTile);
    }

    @Test
    void doesNotLookAtLiftingTheFlagOnTheTurnItMovesIt() {
        KeeperAiSystem system = createSystem(withInvasion(ai(0, 0, 9, 1, 50, 0, List.of()), 5, 1,
                AI.CallToArmsUsagePolicy.TOWARDS_HEART, 0), 1000);
        spellControl.setTypeAvailable(callToArms, true, true);
        enemyRoomControl.onBuild(createRoom(DUNGEON_HEART, new Point(13, 6)));
        KeeperAiEnemy invaded = new KeeperAiEnemy(PLAYER_ID, ENEMY_ID, 0);
        invaded.invaded = true;
        entityData.setComponent(entityData.createEntity(), invaded);
        createCreature(PLAYER_ID, new Point(4, 4), 100, 100, CreatureState.WORK);
        createCreature(PLAYER_ID, new Point(4, 8), 100, 100, CreatureState.WORK);
        reachable = true;
        system.processTick(0.25f);
        EntityId entityId = entityData.findEntity(null, KeeperAi.class);
        KeeperAi keeperAi = new KeeperAi(entityData.getComponent(entityId, KeeperAi.class));
        keeperAi.callToArms = true;
        keeperAi.callToArmsTile = new Point(5, 6);
        keeperAi.rallyTile = new Point(5, 6);
        entityData.setComponent(entityId, keeperAi);

        // The flag's time is up, but moving it takes the action and gives it its time again (0x4FECE0)
        system.processTick(0.25f);

        assertEquals(0, callToArmsCancels);
        assertEquals(1, callToArmsCasts.size());
        assertTrue(getKeeperAi().callToArms);
    }

    @Test
    void doesNothingOnceItsDungeonHeartIsGone() {
        KeeperAiSystem system = createSystem(ai(4, 10, 9, 1, 50, 0, List.of()), 1000);
        creatureControl.onCreatureAdded(new EntityId(1), imp);
        roomControl.getDungeonHeart().getRoomInstance().setDestroyed(true);

        system.processTick(0.25f);

        assertTrue(impSpells.isEmpty());

        roomControl.getDungeonHeart().getRoomInstance().setDestroyed(false);
        keeper.setDestroyed((short) 0);
        system.processTick(0.25f);

        assertTrue(impSpells.isEmpty());
    }

    @Test
    void createsNoImpWhenItsLandAllowsNoMore() {
        KeeperAiSystem system = createSystem(ai(4, 10, 9, 1, 50, 0, List.of()), 1000);
        creatureControl.onCreatureAdded(new EntityId(1), imp);
        creatureControl.onCreatureAdded(new EntityId(2), imp);

        system.processTick(0.25f);

        assertTrue(impSpells.isEmpty());
    }

    @Test
    void createsNoImpAtItsMaximum() {
        KeeperAiSystem system = createSystem(ai(1, 1, 9, 1, 50, 0, List.of()), 1000);
        creatureControl.onCreatureAdded(new EntityId(1), imp);

        system.processTick(0.25f);

        assertTrue(impSpells.isEmpty());
    }

    @Test
    void picksTheFirstRoomOfTheBuildOrderItMayBuildAndDoesNotHave() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY, LAIR, GUARD_ROOM, HATCHERY)), 1000);
        roomControl.setTypeAvailable(rooms.get(TREASURY), true, true);
        roomControl.onBuild(createRoom(TREASURY, new Point(1, 4)));
        roomControl.setTypeAvailable(rooms.get(LAIR), true, false); // Still to be researched
        roomControl.setTypeAvailable(rooms.get(GUARD_ROOM), true, true); // Never built by the computer player
        roomControl.setTypeAvailable(rooms.get(HATCHERY), true, true);

        system.processTick(0.25f);

        assertEquals(HATCHERY, getKeeperAi().roomId);
        assertEquals(KeeperAi.Task.FIND_ROOM_SITE, getKeeperAi().task);
    }

    @Test
    void hasASiteDugForARoomItIsStillResearchingAndBuildsNothingOnIt() {

        // The way it turns and its 5 % chance of giving up a site it can't find are random
        ((Random) Utils.getRandom()).setSeed(1);
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY, LAIR)), 0);
        roomControl.setTypeAvailable(rooms.get(TREASURY), true, true);
        roomControl.onBuild(createRoom(TREASURY, new Point(1, 4)));
        roomControl.setTypeAvailable(rooms.get(LAIR), true, false);

        system.processTick(0.25f);

        assertEquals(KeeperAi.Task.FIND_ROOM_SITE, getKeeperAi().task);
        assertEquals(229, getKeeperAi().roomId);

        Set<Point> dug = new HashSet<>();
        for (int tick = 0; tick < 100 && getKeeperAi().task != KeeperAi.Task.PLAN; tick++) {
            system.processTick(0.25f);
            for (Point p : tagged) {
                map[p.y][p.x] = '.';
            }
            dug.addAll(tagged);
            tagged.clear();
        }

        assertEquals(KeeperAi.Task.PLAN, getKeeperAi().task);
        assertTrue(dug.size() >= 9);
        assertTrue(built.isEmpty());
    }

    @Test
    void headsForTheGoldWhenTheRoomCostsTooMuch() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 9 * ROOM_COST - 1);
        roomControl.setTypeAvailable(rooms.get(TREASURY), true, true);

        // The first time it only looks where the gold is
        system.processTick(0.25f);

        assertEquals(KeeperAi.Task.PLAN, getKeeperAi().task);
        assertEquals(new Point(10, 6), getKeeperAi().goldDeposits[0]);

        // The plan in between is for a Portal
        system.processTick(0.25f);
        system.processTick(0.25f);

        assertEquals(KeeperAi.Task.GO_TO_GOLD, getKeeperAi().task);
        assertEquals(new Point(10, 6), getKeeperAi().target);
    }

    @Test
    void headsForTheSeamOfItsLastLookAndKeepsTheNearestSeamsForTheNextTime() {
        map[2][13] = 'G';
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 1000, List.of()), 0);
        system.processTick(0.25f);

        assertArrayEquals(new Point[]{new Point(10, 6), new Point(13, 2), null, null}, getKeeperAi().goldDeposits);

        // The near seam is gone by the time it needs gold, it still goes there and learns of it then
        map[6][10] = '#';
        map[6][11] = '#';
        system.processTick(0.25f);

        assertEquals(KeeperAi.Task.GO_TO_GOLD, getKeeperAi().task);
        assertEquals(new Point(10, 6), getKeeperAi().target);
        assertArrayEquals(new Point[]{new Point(13, 2), null, null, null}, getKeeperAi().goldDeposits);
    }

    @Test
    void takesTheFirstTileOfTheSpiralWhenTheMiddleOfASeamIsImpenetrable() {
        map[6][10] = '#';
        map[6][11] = '#';
        map[3][10] = 'X';
        map[3][11] = 'G';
        map[4][11] = 'G';
        map[4][10] = 'G';
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 1000, List.of()), 0);
        system.processTick(0.25f);

        // The spiral starts one tile east, on the gold, not on the rock north west of the middle
        assertEquals(new Point(11, 3), getKeeperAi().goldDeposits[0]);
    }

    @Test
    void aimsAtTheMiddleOfASeamEvenWhenTheMiddleHasNoGold() {
        map[6][10] = '#';
        map[6][11] = '#';
        for (int y = 2; y <= 4; y++) {
            for (int x = 9; x <= 11; x++) {
                map[y][x] = 'G';
            }
        }
        map[3][10] = '#';
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 1000, List.of()), 0);
        system.processTick(0.25f);

        // DK II takes the mean of the seam's tiles, rock it can dig is fine
        assertEquals(new Point(10, 3), getKeeperAi().goldDeposits[0]);
    }

    @Test
    void headsForTheNextSeamWhenThePathToTheNearestTookTooLong() {
        map[2][13] = 'G';
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 1000, List.of()), 0);
        system.processTick(0.25f);
        system.processTick(0.25f);

        assertEquals(new Point(10, 6), getKeeperAi().target);

        // The imps never dig the path, so it is dropped after 7 minutes
        for (int tick = 0; tick < 500 * 4 && getKeeperAi().target.equals(new Point(10, 6)); tick++) {
            system.processTick(0.25f);
        }

        assertEquals(KeeperAi.Task.GO_TO_GOLD, getKeeperAi().task);
        assertEquals(new Point(13, 2), getKeeperAi().target);
    }

    @Test
    void makesItsOwnGoldWhenItKnowsOfNoSeamAndHasTheSpell() {
        map[6][10] = '#';
        map[6][11] = '#';
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 1000, List.of()), 0);
        system.processTick(0.25f);

        assertTrue(createGoldCasts.isEmpty());

        spellControl.setTypeAvailable(createGold, true, true);
        system.processTick(0.25f);

        assertEquals(List.of(new Point(HEART.x + 1, HEART.y + 1)), createGoldCasts);
        assertEquals(KeeperAi.Task.PLAN, getKeeperAi().task);
    }

    @Test
    void letsGoOfItsNewestGoldTagOnlyAndHeadsForNoGoldWhileTheOthersStayTagged() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 1000, List.of()), 0);
        tagGoldAt(new Point(10, 6));

        // The seam's two tiles are tagged on the first two ticks, the newer one is let go 240 ticks after
        system.processTick(0.25f);
        system.processTick(0.25f);
        for (int tick = 0; tick < 239; tick++) {
            system.processTick(0.25f);
        }

        assertEquals(Set.of(new Point(10, 6), new Point(11, 6)), tagged);

        system.processTick(0.25f);

        assertEquals(Set.of(new Point(10, 6)), tagged);

        // With gold still tagged it starts no gold trip, however low it is on gold
        for (int tick = 0; tick < 2000; tick++) {
            system.processTick(0.25f);
            assertNotEquals(KeeperAi.Task.GO_TO_GOLD, getKeeperAi().task);
        }
        assertEquals(Set.of(new Point(10, 6)), tagged);

        // Once that tile is mined out it heads for gold again
        map[6][10] = '.';
        system.processTick(0.25f);
        system.processTick(0.25f);

        assertEquals(KeeperAi.Task.GO_TO_GOLD, getKeeperAi().task);
    }

    @Test
    void letsGoOfTheNewestGoldTagThatIsStillThere() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 1000, List.of()), 0);
        tagGoldAt(new Point(10, 6));
        system.processTick(0.25f);
        system.processTick(0.25f);

        // The newest tile is mined out before the pass, so the pass lets go of the one tagged before it
        map[6][11] = '.';
        tagged.remove(new Point(11, 6));
        for (int tick = 0; tick < 240; tick++) {
            system.processTick(0.25f);
        }

        assertTrue(tagged.isEmpty());
    }

    /**
     * Has the computer player tag the gold round a tile from its next tick on
     */
    private void tagGoldAt(Point target) {
        KeeperAi keeperAi = new KeeperAi(getKeeperAi());
        keeperAi.task = KeeperAi.Task.TAG_GOLD;
        keeperAi.target = target;
        entityData.setComponent(entityData.findEntity(null, KeeperAi.class), keeperAi);
    }

    @Test
    void hasTheRoomSiteDugOutAndBuildsTheRoomOnIt() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 1000);
        roomControl.setTypeAvailable(rooms.get(TREASURY), true, true);

        for (int tick = 0; tick < 100 && built.size() < 9; tick++) {
            system.processTick(0.25f);

            // The imps dig out and claim what is tagged
            for (Point p : tagged) {
                map[p.y][p.x] = '.';
            }
            tagged.clear();
        }

        // West is impenetrable and south too narrow, so it goes north of the heart with a tile in between,
        // on the line through the heart's reference tile
        Set<Point> site = new HashSet<>();
        for (int x = 3; x <= 5; x++) {
            for (int y = 1; y <= 3; y++) {
                site.add(new Point(x, y));
            }
        }
        assertEquals(site, new HashSet<>(built));
        assertEquals(9, built.size());
    }

    @Test
    void triesTheSameRoomTileAgainEachTickWhileShortOfGold() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 1000);
        roomControl.setTypeAvailable(rooms.get(TREASURY), true, true);
        chargeGoldForBuild = true;

        for (int tick = 0; tick < 100 && built.isEmpty(); tick++) {
            system.processTick(0.25f);
            for (Point p : tagged) {
                map[p.y][p.x] = '.';
            }
            tagged.clear();
        }

        assertFalse(built.isEmpty());
        int buildsBeforeGoldWasSpent = built.size();
        keeper.setGold(0);

        for (int tick = 0; tick < 20; tick++) {
            system.processTick(0.25f);
            for (Point p : tagged) {
                map[p.y][p.x] = '.';
            }
            tagged.clear();
        }

        // It stays on the room, trying the one tile, and builds the rest once the gold comes in
        assertEquals(KeeperAi.Task.BUILD_ROOM, getKeeperAi().task);
        assertEquals(buildsBeforeGoldWasSpent, built.size());
        assertTrue(rejectedBuildAttempts > 1);
        assertEquals(1, new HashSet<>(buildAttempts.subList(buildsBeforeGoldWasSpent, buildAttempts.size())).size());

        keeper.setGold(1000);
        for (int tick = 0; tick < 100 && getKeeperAi().task == KeeperAi.Task.BUILD_ROOM; tick++) {
            system.processTick(0.25f);
        }

        assertEquals(9, built.size());
    }

    @Test
    void skipsARejectedRoomTileInsteadOfRetryingItEveryTick() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 1000);
        roomControl.setTypeAvailable(rooms.get(TREASURY), true, true);
        rejectedRoomBuildId = TREASURY;

        for (int tick = 0; tick < 200 && buildAttempts.size() < 2; tick++) {
            system.processTick(0.25f);
            for (Point p : tagged) {
                map[p.y][p.x] = '.';
            }
            tagged.clear();
        }

        assertEquals(2, buildAttempts.size());
        assertNotEquals(buildAttempts.get(0), buildAttempts.get(1));
        assertEquals(2, rejectedBuildAttempts);
    }

    @Test
    void triesOneDirectionATickForARoomSite() {
        KeeperAiSystem system = createSystem(ai(0, 0, 100, 2, 50, 0, List.of(TREASURY)), 1000);
        roomControl.setTypeAvailable(rooms.get(TREASURY), true, true);
        for (int tick = 0; tick < 10 && getKeeperAi().task != KeeperAi.Task.FIND_ROOM_SITE; tick++) {
            system.processTick(0.25f);
        }
        int preferred = getKeeperAi().direction;
        Point start = getKeeperAi().current;

        // No 10 by 10 site fits anywhere: each tick tries the next direction at the default size, from the same room
        for (int direction : new int[]{preferred + 1, preferred - 1, preferred + 2}) {
            system.processTick(0.25f);
            assertEquals(KeeperAi.Task.FIND_ROOM_SITE, getKeeperAi().task);
            assertEquals(direction & 3, getKeeperAi().direction);
            assertEquals(start, getKeeperAi().current);
            assertEquals(0, getKeeperAi().roomSitesNotFound);
        }
    }

    @Test
    void findsARoomSiteBehindTheGap() {
        KeeperAiRoomSiteFinder finder = new KeeperAiRoomSiteFinder(mapController, PLAYER_ID, ai(0, 0, 9, 1, 50, 0, List.of()));

        KeeperAiRoomSiteFinder.RoomSite site = finder.find(HEART, EAST, 9, 1);

        assertEquals(new Point(6, 6), site.entrance());
        assertEquals(new Point(6, 7), site.corner());
        assertEquals(new Point(8, 5), site.oppositeCorner());
    }

    @Test
    void findsARoomSiteOnPathNobodyHasClaimed() {

        // DK II takes terrain that is claimed at full health, a dirt path or a mana vault, as a site tile
        for (int y = 5; y <= 7; y++) {
            map[y][7] = 'D';
        }

        KeeperAiRoomSiteFinder.RoomSite site = new KeeperAiRoomSiteFinder(mapController, PLAYER_ID, ai(0, 0, 9, 1, 50, 0, List.of()))
                .find(HEART, EAST, 9, 1);

        assertEquals(new Point(6, 7), site.corner());
        assertEquals(new Point(8, 5), site.oppositeCorner());
    }

    @Test
    void roomSiteGetsOneMoreRowWhenTheSizeIsNotSquare() {
        KeeperAiRoomSiteFinder finder = new KeeperAiRoomSiteFinder(mapController, PLAYER_ID, ai(0, 0, 12, 1, 50, 0, List.of()));

        KeeperAiRoomSiteFinder.RoomSite site = finder.find(HEART, EAST, 12, 1);

        assertEquals(new Point(6, 7), site.corner());
        assertEquals(new Point(9, 5), site.oppositeCorner());

        // There is no room for a fourth row between the heart and the edge of the map
        assertNull(finder.find(HEART, NORTH, 12, 1));
    }

    @Test
    void roomSiteStaysSquareWhenNoGapIsLeftBetweenRooms() {
        KeeperAiRoomSiteFinder finder = new KeeperAiRoomSiteFinder(mapController, PLAYER_ID, ai(0, 0, 12, 0, 50, 0, List.of()));

        KeeperAiRoomSiteFinder.RoomSite site = finder.find(HEART, EAST, 12, 0);

        assertEquals(new Point(5, 7), site.corner());
        assertEquals(new Point(7, 5), site.oppositeCorner());
    }

    @Test
    void findsNoRoomSiteInImpenetrableRockOrOutsideTheBuildArea() {
        AI ai = ai(0, 0, 9, 1, 50, 0, List.of());

        assertNull(new KeeperAiRoomSiteFinder(mapController, PLAYER_ID, ai).find(HEART, WEST, 9, 1));
        assertNull(new KeeperAiRoomSiteFinder(mapController, PLAYER_ID, withBuildAreaEndX(ai, 8)).find(HEART, EAST, 9, 1));
    }

    @Test
    void findsNoRoomSiteWithTooMuchOpenSpaceAroundIt() {

        // 5 of the 16 tiles around the site are claimed path
        assertNull(new KeeperAiRoomSiteFinder(mapController, PLAYER_ID, ai(0, 0, 9, 1, 30, 0, List.of())).find(HEART, EAST, 9, 1));
    }

    @Test
    void growsAFullRoomOnEverySideThatHasRockBehindIt() {
        KeeperAiSystem system = createSystem(withMoreSpace(ai(0, 0, 9, 1, 50, 0, List.of(LAIR)), AI.RoomExpandPolicy.EXPAND_EXISTING, false), 1000);
        roomControl.setTypeAvailable(rooms.get(LAIR), true, true);
        roomControl.onBuild(createRoom(LAIR, new Point(9, 2), new Point(11, 4)));
        fullRooms.add(LAIR);

        system.processTick(0.25f);

        // North of it the rock is only one tile deep, so it grows south, west and east
        assertEquals(KeeperAi.Task.TAG_ROOM, getKeeperAi().task);
        assertEquals(LAIR, getKeeperAi().roomId);
        assertEquals(new Point(8, 2), getKeeperAi().corner);
        assertEquals(new Point(12, 5), getKeeperAi().oppositeCorner);

        // The whole box is tagged and the new tiles are built on as they are dug out and claimed
        for (int tick = 0; tick < 100 && built.size() < 11; tick++) {
            system.processTick(0.25f);
            for (Point p : tagged) {
                map[p.y][p.x] = '.';
            }
            tagged.clear();
        }
        assertEquals(11, built.size());
        assertTrue(built.contains(new Point(8, 2)) && built.contains(new Point(12, 5)) && !built.contains(new Point(10, 3)));
    }

    @Test
    void growsAFullTrainingRoomToo() {
        KeeperAiSystem system = createSystem(withMoreSpace(ai(0, 0, 9, 1, 50, 0, List.of(TRAINING_ROOM)), AI.RoomExpandPolicy.EXPAND_EXISTING, false), 1000);
        roomControl.setTypeAvailable(rooms.get(TRAINING_ROOM), true, true);
        roomControl.onBuild(createRoom(TRAINING_ROOM, new Point(9, 2), new Point(11, 4)));
        fullRooms.add(TRAINING_ROOM);

        system.processTick(0.25f);

        assertEquals(KeeperAi.Task.TAG_ROOM, getKeeperAi().task);
        assertEquals(TRAINING_ROOM, getKeeperAi().roomId);
    }

    @Test
    void buildsAnotherRoomOrNoneForAFullRoomByItsSettings() {
        AI ai = withMoreSpace(ai(0, 0, 9, 1, 50, 0, List.of(LAIR)), AI.RoomExpandPolicy.IGNORE, false);
        KeeperAiSystem system = createSystem(ai, 1000);
        roomControl.setTypeAvailable(rooms.get(LAIR), true, true);
        roomControl.onBuild(createRoom(LAIR, new Point(9, 2), new Point(11, 4)));
        fullRooms.add(LAIR);

        system.processTick(0.25f);

        assertEquals(KeeperAi.Task.PLAN, getKeeperAi().task);

        withMoreSpace(ai, AI.RoomExpandPolicy.CREATE_NEW, false);
        system.processTick(0.25f);

        assertEquals(KeeperAi.Task.FIND_ROOM_SITE, getKeeperAi().task);
        assertEquals(LAIR, getKeeperAi().roomId);
    }

    @Test
    void wantsABiggerLairWhenItHasTakenAPortalFromAnotherPlayerButNotWhenItClaimsANeutralOne() {
        IRoomController neutral = createNeutralPortal(new Point(13, 4));
        KeeperAiSystem system = createSystem(withMoreSpace(ai(0, 0, 9, 1, 50, 0, List.of(LAIR)), AI.RoomExpandPolicy.EXPAND_EXISTING, true), 1000);
        roomControl.setTypeAvailable(rooms.get(LAIR), true, true);
        roomControl.onBuild(createRoom(LAIR, new Point(9, 2), new Point(11, 4)));
        neutral.getRoomInstance().setOwnerId(PLAYER_ID);
        roomControl.onBuild(neutral);

        system.processTick(0.25f);

        assertEquals(KeeperAi.Task.PLAN, getKeeperAi().task);

        roomControl.onBuild(createRoom(PORTAL, new Point(13, 8)));
        system.processTick(0.25f);

        assertEquals(KeeperAi.Task.TAG_ROOM, getKeeperAi().task);
        assertEquals(LAIR, getKeeperAi().roomId);
    }

    @Test
    void queuesTheRoomsItsCreaturesAskForAndPlansThemInTurnBeforeItsBuildOrder() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 1000);
        roomControl.setTypeAvailable(rooms.get(TREASURY), true, true);
        roomControl.setTypeAvailable(rooms.get(LAIR), true, true);
        roomControl.setTypeAvailable(rooms.get(TRAINING_ROOM), true, true);
        system.start();
        try {

            // The first ask is planned before the Treasury of its build order, the next one waits its turn
            EventBus.publish(ServerEvents.ROOM_WANTED, new RoomWanted(PLAYER_ID, ObjectType.LAIR));
            EventBus.publish(ServerEvents.ROOM_WANTED, new RoomWanted(PLAYER_ID, ObjectType.TRAINEE));
            system.processTick(0.25f);

            assertEquals(KeeperAi.Task.FIND_ROOM_SITE, getKeeperAi().task);
            assertEquals(LAIR, getKeeperAi().roomId);
            assertEquals(TRAINING_ROOM, getKeeperAi().roomAsks[0]);

            // It takes the place of the Portal plan lined up next. The Lair isn't asked for again while its ask
            // lasts, and nothing is queued for a room it may not build
            EventBus.publish(ServerEvents.ROOM_WANTED, new RoomWanted(PLAYER_ID, ObjectType.GOLD));
            EventBus.publish(ServerEvents.ROOM_WANTED, new RoomWanted(PLAYER_ID, ObjectType.LAIR));
            EventBus.publish(ServerEvents.ROOM_WANTED, new RoomWanted(PLAYER_ID, ObjectType.WORSHIPPER));
            system.processTick(0.25f);

            assertEquals(TRAINING_ROOM, getKeeperAi().wantedRoomId);
            assertEquals(TREASURY, getKeeperAi().roomAsks[0]);
            assertEquals(0, getKeeperAi().roomAsks[1]);

            // The Treasury asked for before it had one is planned in its turn all the same
            roomControl.onBuild(createRoom(TREASURY, new Point(9, 2), new Point(11, 4)));
            planAgain();
            system.processTick(0.25f);

            assertEquals(TRAINING_ROOM, getKeeperAi().roomId);

            system.processTick(0.25f);

            assertEquals(TREASURY, getKeeperAi().wantedRoomId);
            assertEquals(0, getKeeperAi().roomAsks[0]);

            planAgain();
            system.processTick(0.25f);

            assertEquals(TREASURY, getKeeperAi().roomId);
        } finally {
            system.stop();
        }
    }

    private KeeperAi getKeeperAi() {
        return entityData.getComponent(entityData.findEntity(null, KeeperAi.class), KeeperAi.class);
    }

    private KeeperAiSystem createSystem(AI ai, int gold) {
        return createSystem(ai, gold, ENEMY_ID);
    }

    private KeeperAiSystem createSystem(AI ai, int gold, short enemyId) {
        keeper = new Keeper(player(PLAYER_ID, ai));
        if (lobbyType != null) {
            keeper = new Keeper(true, PLAYER_ID);
            keeper.setAiType(lobbyType);
        }
        Map<MiscType, Variable.MiscVariable> variables = Map.of(
                MiscType.COMBAT_PIT_MAX_EXPERIENCE_LEVEL, variable(MiscType.COMBAT_PIT_MAX_EXPERIENCE_LEVEL, 8),
                MiscType.TRAINING_ROOM_MAX_EXPERIENCE_LEVEL, variable(MiscType.TRAINING_ROOM_MAX_EXPERIENCE_LEVEL, 4),
                MiscType.CREATURES_SUPPORTED_BY_FIRST_PORTAL, variable(MiscType.CREATURES_SUPPORTED_BY_FIRST_PORTAL, 2),
                MiscType.CREATURES_SUPPORTED_PER_ADDITIONAL_PORTAL, variable(MiscType.CREATURES_SUPPORTED_PER_ADDITIONAL_PORTAL, 1),
                MiscType.MAXIMUM_MANA_THRESHOLD, variable(MiscType.MAXIMUM_MANA_THRESHOLD, 200000));
        PlayerManaControl manaControl = new PlayerManaControl(keeper, variables);
        handControl = new PlayerHandControl(keeper, 8, entityData);
        trapControl = new PlayerTrapControl(keeper, List.of(sentry));
        doorControl = new PlayerDoorControl(keeper, List.of(woodenDoor));
        spellControl = new PlayerSpellControl(keeper, List.of(createImp, callToArms, thunderbolt, heal, sightOfEvil, createGold));
        spellControl.setTypeAvailable(createImp, true, true);
        Keeper enemyKeeper = new Keeper(false, enemyId);
        enemyKeeper.setPlayer(new Player());
        enemyRoomControl = new PlayerRoomControl(enemyKeeper, new ArrayList<>(rooms.values()));
        IPlayerController enemyController = (IPlayerController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IPlayerController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getKeeper" -> enemyKeeper;
            case "getRoomControl" -> enemyRoomControl;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> {
                    if (!"cancelCallToArms".equals(method.getName())) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    callToArmsCancels++;
                    return null;
                });
        keeper.setGold(gold);
        keeper.setMana(1000);
        roomControl = new PlayerRoomControl(keeper, new ArrayList<>(rooms.values()));
        roomControl.onBuild(createRoom(DUNGEON_HEART, HEART));
        creatureControl = new PlayerCreatureControl(keeper, imp, List.of(imp));
        IPlayerController playerController = (IPlayerController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IPlayerController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getKeeper" -> keeper;
            case "getRoomControl" -> roomControl;
            case "getCreatureControl" -> creatureControl;
            case "getManaControl" -> manaControl;
            case "getHandControl" -> handControl;
            case "getSpellControl" -> spellControl;
            case "getTrapControl" -> trapControl;
            case "getDoorControl" -> doorControl;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getGameLevel" -> gameLevel(4);
            case "getKeeperSpells" -> List.of(createImp, heal, thunderbolt);
            case "getCreature" -> ((Number) args[0]).shortValue() == EATER ? creatureWithJobs(EATER, Creature.JobType.RESEARCH, Creature.JobType.EAT)
                    : ((Number) args[0]).shortValue() == HORNY
                    ? CombatExperienceFixtures.creature(HORNY, Creature.CreatureFlag.IS_HORNY) : CombatExperienceFixtures.creature((short) 0);
            case "getRooms" -> new ArrayList<>(rooms.values());
            case "getTraps" -> List.of(sentry);
            case "getObject" -> ((Number) args[0]).shortValue() == 1 ? GameObjectFixtures.gameObject("Gold")
                    : GameObjectFixtures.gameObject("Special", GameObject.ObjectFlag.OBJECT_TYPE_SPECIAL);
            case "getDoors" -> List.of(woodenDoor);
            case "getVariables" -> variables;
            case "getRoomById" -> rooms.getOrDefault(((Number) args[0]).shortValue(), bridges.get(((Number) args[0]).shortValue()));
            case "getRoomByTerrain" -> dungeonHeartTileRoom != null && ((Number) args[0]).shortValue() == DUNGEON_HEART ? dungeonHeartTileRoom : null;
            case "getKeeperSpellById" -> ((Number) args[0]).shortValue() == CALL_TO_ARMS ? callToArms
                    : ((Number) args[0]).shortValue() == SIGHT_OF_EVIL ? sightOfEvil
                    : ((Number) args[0]).shortValue() == CREATE_GOLD ? createGold : null;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        ICreatureController creatureController = (ICreatureController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> {
                    throw new UnsupportedOperationException(method.getName());
                });
        ICreaturesController creaturesController = (ICreaturesController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> {
                    if (!"createController".equals(method.getName())) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    return creatureController;
                });
        IGameWorldController gameWorldController = (IGameWorldController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IGameWorldController.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getMapController" -> {
                            return mapController;
                        }
                        case "getObjectsController" -> {
                            return objectsController;
                        }
                        case "getCreaturesController" -> {
                            return creaturesController;
                        }
                        case "castKeeperSpell" -> {
                            if ((short) args[0] == SIGHT_OF_EVIL || (short) args[0] == CREATE_GOLD) {
                                ((short) args[0] == SIGHT_OF_EVIL ? sightOfEvilCasts : createGoldCasts).add((Point) args[2]);
                                keeper.setMana(keeper.getMana() - 100);
                                return null;
                            }
                            if ((short) args[0] == HEAL || (short) args[0] == THUNDERBOLT) {
                                battleCasts.put((short) args[0], (EntityId) args[1]);
                                keeper.setMana(keeper.getMana() - 100);
                                return null;
                            }
                            boolean imp = (short) args[0] == createImp.getKeeperSpellId();
                            (imp ? impSpells : callToArmsCasts).add((Point) args[2]);
                            keeper.setMana(keeper.getMana() - (imp ? createImp : callToArms).getManaCost());
                        }
                        case "pickUp" -> {
                            pickedUp.add((EntityId) args[0]);
                            if (entityData.getComponent((EntityId) args[0], CreatureComponent.class) != null
                                    || entityData.getComponent((EntityId) args[0], Food.class) != null) {
                                handControl.push((EntityId) args[0], 0.0);
                            }
                        }
                        case "interact" ->
                            slapped.add((EntityId) args[0]);
                        case "getGold" -> {
                            EntityId pay = createObject((short) 1, new Point(0, 0));
                            entityData.setComponent(pay, new Gold((int) args[0], (int) args[0]));
                            keeper.setGold(keeper.getGold() - (int) args[0]);
                            handControl.push(pay, 0.0);
                        }
                        case "placeTrap", "placeDoor" -> {
                            Point tile = (Point) args[1];
                            placed.put(tile, method.getName());
                            keeper.setGold(keeper.getGold() - 100);
                            entityData.setComponents(entityData.createEntity(), "placeTrap".equals(method.getName())
                                    ? new TrapComponent((short) args[0]) : new DoorComponent((short) args[0], false, true),
                                    new Owner(PLAYER_ID, PLAYER_ID), new Position(0, WorldUtils.pointToVector3f(tile.x, tile.y)));
                        }
                        case "drop" -> {
                            Point tile = (Point) args[1];
                            dropAttempts.add(tile);
                            if (!rejectPreferredHeartDrop || !HEART_DROP.equals(tile)) {
                                dropped.put(handControl.pop(), tile);
                                if (args[3] != null) {
                                    droppedOn.put((EntityId) args[0], (EntityId) args[3]);
                                }
                                entityData.setComponent((EntityId) args[0], new Position(0, new Vector3f(((Vector2f) args[2]).x, 0, ((Vector2f) args[2]).y)));
                                if (entityData.getComponent((EntityId) args[0], CreatureComponent.class) != null) {
                                    entityData.setComponent((EntityId) args[0], new CreatureFall());
                                }
                            }
                        }
                        case "build" -> {
                            Point p = new Point((int) ((Vector2f) args[0]).x, (int) ((Vector2f) args[0]).y);
                            short roomId = (short) args[3];
                            buildAttempts.add(p);
                            Room room = rooms.getOrDefault(roomId, bridges.get(roomId));
                            if (roomId == rejectedRoomBuildId || chargeGoldForBuild && keeper.getGold() < room.getCost()) {
                                rejectedBuildAttempts++;
                                return null;
                            }
                            built.add(p);
                            builtRoomIds.add(roomId);
                            if (chargeGoldForBuild) {
                                keeper.setGold(keeper.getGold() - room.getCost());
                            }
                            map[p.y][p.x] = 'H';
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
                    return null;
                });

        INavigationService navigationService = (INavigationService) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{INavigationService.class}, (proxy, method, args) -> {
                    if (!"findPath".equals(method.getName())) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    routesAsked.add(List.of((Point) args[0], (Point) args[1]));
                    return findPath((Point) args[0], (Point) args[1]);
                });

        return new KeeperAiSystem(entityData, kwdFile, Map.of(PLAYER_ID, playerController, enemyId, enemyController), gameWorldController,
                navigationService, () -> gameTime, logDecisions);
    }

    /**
     * A room round a tile: a Dungeon Heart of 5 x 5 tiles, a Portal of 3 x 3,
     * any other room just the tile. Only the tile is marked on the map
     */
    private IRoomController createRoom(short roomId, Point centre) {
        IRoomController room = createRoom(roomId, centre, centre);
        int size = roomId == DUNGEON_HEART ? 2 : roomId == PORTAL || roomId == HERO_PORTAL ? 1 : 0;
        for (int x = centre.x - size; x <= centre.x + size; x++) {
            for (int y = centre.y - size; y <= centre.y + size; y++) {
                room.getRoomInstance().addCoordinate(new Point(x, y));
            }
        }

        return room;
    }

    private IRoomController createRoom(short roomId, Point corner, Point oppositeCorner) {
        RoomInstance roomInstance = new RoomInstance(rooms.get(roomId));
        for (int x = corner.x; x <= oppositeCorner.x; x++) {
            for (int y = corner.y; y <= oppositeCorner.y; y++) {
                roomInstance.addCoordinate(new Point(x, y));
                if (!corner.equals(oppositeCorner)) {
                    map[y][x] = 'H';
                }
            }
        }

        return (IRoomController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getRoom" -> rooms.get(roomId);
            case "isDungeonHeart" -> roomId == DUNGEON_HEART;
            case "getRoomInstance" -> roomInstance;

            // As in OpenKeeper, the middle 3 x 3 of the Dungeon Heart can't be stood on
            case "isTileAccessible" -> roomId != DUNGEON_HEART
                    || Math.abs(((Point) args[1]).x - roomInstance.getCenter().x) > 1
                    || Math.abs(((Point) args[1]).y - roomInstance.getCenter().y) > 1;
            case "getAccessibleTile" -> InvocationHandler.invokeDefault(proxy, method, args);
            case "isFullCapacity" -> fullRooms.contains(roomId);
            case "hasObjectControl" -> roomId == HATCHERY;
            case "getObjectControl" -> Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{IRoomObjectControl.class}, (control, controlMethod, controlArgs) -> hatcheryFood.get((Point) controlArgs[0]));
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    /**
     * A Portal of 3 x 3 tiles that sends a creature dropped on its middle out
     * of the dungeon
     */
    private ThreeByThreeController createPortal(Point corner) {
        RoomInstance roomInstance = new RoomInstance(rooms.get(PORTAL));
        roomInstance.setOwnerId(PLAYER_ID);
        for (int x = corner.x; x < corner.x + 3; x++) {
            for (int y = corner.y; y < corner.y + 3; y++) {
                roomInstance.addCoordinate(new Point(x, y));
                map[y][x] = 'H';
            }
        }

        return new ThreeByThreeController(entityData.createEntity(), entityData, null, roomInstance, null, () -> gameTime);
    }

    private IMapController createMapController() {
        IMapDataInformation<?> mapData = (IMapDataInformation<?>) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IMapDataInformation.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getWidth" -> map[0].length;
            case "getHeight" -> map.length;
            case "getTile" -> args.length == 1 ? createTile((Point) args[0]) : createTile(new Point((int) args[0], (int) args[1]));
            default -> throw new UnsupportedOperationException(method.getName());
        });

        return (IMapController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getMapData" -> {
                            return mapData;
                        }
                        case "getTerrain" -> {
                            return getTerrain(((IMapTileController) args[0]).getLocation());
                        }
                        case "getRoomControllers" -> {
                            return neutralRooms;
                        }
                        case "getRoomControllerByCoordinates" -> {
                            Point p = (Point) args[0];
                            return map[p.y][p.x] == 'H' ? roomControl.getDungeonHeart() : null;
                        }
                        case "isSolid" -> {
                            return getTerrain((Point) args[0]).getFlags().contains(TerrainFlag.SOLID);
                        }
                        case "isTaggable" -> {
                            return getTerrain((Point) args[0]).getFlags().contains(TerrainFlag.TAGGABLE);
                        }
                        case "isBuildable" -> {
                            return ".WL".indexOf(map[((Point) args[0]).y][((Point) args[0]).x]) >= 0;
                        }
                        case "selectTiles" -> {
                            Vector2f start = (Vector2f) args[0];
                            Vector2f end = (Vector2f) args[1];
                            boolean select = (boolean) args[2];
                            for (int x = (int) start.x; x <= end.x; x++) {
                                for (int y = (int) start.y; y <= end.y; y++) {
                                    if (getTerrain(new Point(x, y)).getFlags().contains(TerrainFlag.TAGGABLE)) {
                                        Point tile = new Point(x, y);
                                        if (select) {
                                            tagged.add(tile);
                                            taggedHistory.add(tile);
                                        } else {
                                            tagged.remove(tile);
                                        }
                                    }
                                }
                            }
                            return null;
                        }
                        default ->
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    /**
     * A way along x and then along y, when the way is open
     */
    private GraphPath<IMapTileInformation> findPath(Point from, Point to) {
        if (!reachable || closedTiles.contains(to)) {
            return null;
        }
        DefaultGraphPath<IMapTileInformation> path = new DefaultGraphPath<>();
        Point p = from;
        path.add(createTile(p));
        while (!p.equals(to)) {
            p = p.x != to.x ? new Point(p.x + Integer.signum(to.x - p.x), p.y) : new Point(p.x, p.y + Integer.signum(to.y - p.y));
            path.add(createTile(p));
        }

        return path;
    }

    private KeeperAiEnemy getEnemy() {
        return getEnemy(ENEMY_ID);
    }

    private KeeperAiEnemy getEnemy(short enemyId) {
        for (EntityId entityId : entityData.findEntities(null, KeeperAiEnemy.class)) {
            KeeperAiEnemy enemy = entityData.getComponent(entityId, KeeperAiEnemy.class);
            if (enemy.enemyId == enemyId) {
                return enemy;
            }
        }

        return null;
    }

    private Terrain getTerrain(Point p) {
        return terrains.get(map[p.y][p.x]);
    }

    private IMapTileController createTile(Point p) {
        if (p.x < 0 || p.y < 0 || p.x >= map[0].length || p.y >= map.length) {
            return null;
        }

        return (IMapTileController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IMapTileController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getLocation" -> p;
            case "getOwnerId" -> map[p.y][p.x] == '.' || map[p.y][p.x] == 'H' ? PLAYER_ID : map[p.y][p.x] == 'E' ? ENEMY_ID : (short) 0;
            case "getTerrainId" -> getTerrain(p).getTerrainId();
            case "getRoomId" -> map[p.y][p.x] == 'H' ? new EntityId(1) : null;
            case "isSelected" -> tagged.contains(p);
            case "getHealth" -> map[p.y][p.x] == 'G' ? (tileHealth[p.y][p.x] == 0 ? 100 : tileHealth[p.y][p.x]) : 0;
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }
}
