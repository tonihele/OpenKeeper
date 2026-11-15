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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CombatPitFighter;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.KeeperAi;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.player.PlayerCreatureControl;
import toniarts.openkeeper.game.controller.player.PlayerHandControl;
import toniarts.openkeeper.game.controller.player.PlayerManaControl;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.tools.convert.map.AI;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.ai;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.player;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.withBuildAreaEndX;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.withRetreatHealth;
import toniarts.openkeeper.tools.convert.map.Creature;
import static toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures.variable;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import static toniarts.openkeeper.tools.convert.map.KeeperSpellFixtures.keeperSpell;
import static toniarts.openkeeper.tools.convert.map.PassiveExperienceFixtures.creature;
import static toniarts.openkeeper.tools.convert.map.PayDayFixtures.gameLevel;
import toniarts.openkeeper.tools.convert.map.Room;
import static toniarts.openkeeper.tools.convert.map.RoomFixtures.room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.flags;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.terrain;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * The computer player on a small map: a 3 x 3 Dungeon Heart (H) with claimed
 * path (.) around it, in rock (#) with a gold seam (G), inside impenetrable
 * rock (X)
 */
class KeeperAiSystemTest {

    private static final short PLAYER_ID = 3;
    private static final short TREASURY = 1;
    private static final short LAIR = 2;
    private static final short HATCHERY = 4;
    private static final short DUNGEON_HEART = 5;
    private static final short GUARD_ROOM = 9;
    private static final int ROOM_COST = 10;
    private static final Point HEART = new Point(3, 6);
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
    private final Map<Character, Terrain> terrains = Map.of(
            'X', terrain((short) 1, flags(TerrainFlag.SOLID, TerrainFlag.IMPENETRABLE), 0, 0, 0, (short) 0),
            '#', terrain((short) 2, flags(TerrainFlag.SOLID, TerrainFlag.TAGGABLE), 0, 0, 0, (short) 0),
            'G', terrain((short) 3, flags(TerrainFlag.SOLID, TerrainFlag.TAGGABLE), 100, 0, 0, (short) 0),
            '.', terrain((short) 4, flags(TerrainFlag.OWNABLE), 0, 0, 0, (short) 0),
            'H', terrain((short) 5, flags(TerrainFlag.OWNABLE, TerrainFlag.ROOM), 0, 0, 0, (short) 0));
    private final Set<Point> tagged = new HashSet<>();
    private final List<Point> built = new ArrayList<>();
    private final List<Point> impSpells = new ArrayList<>();

    private final Map<Short, Room> rooms = Map.of(
            TREASURY, room(TREASURY, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND, Room.RoomFlag.BUILDABLE),
            LAIR, room(LAIR, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND, Room.RoomFlag.BUILDABLE),
            HATCHERY, room(HATCHERY, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND, Room.RoomFlag.BUILDABLE),
            DUNGEON_HEART, room(DUNGEON_HEART, 0, Room.RoomFlag.PLACEABLE_ON_LAND),
            GUARD_ROOM, room(GUARD_ROOM, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND, Room.RoomFlag.BUILDABLE));
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
    private final Map<EntityId, Point> dropped = new HashMap<>();

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

        assertEquals(Map.of(outside, HEART), dropped);
    }

    @Test
    void leavesAHurtCreatureItJustDroppedAloneForAWhile() {
        KeeperAiSystem system = createSystem(withRetreatHealth(ai(0, 0, 9, 1, 50, 0, List.of()), 20), 1000);
        EntityId hurt = createCreature(PLAYER_ID, new Point(1, 8), 5, 100, CreatureState.IDLE);
        system.processTick(0.25f);
        dropped.clear();

        // It walks out of the heart again
        entityData.setComponent(hurt, new Position(0, WorldUtils.pointToVector3f(1, 8)));
        gameTime = 74;
        system.processTick(0.25f);

        assertTrue(dropped.isEmpty());

        gameTime = 75;
        system.processTick(0.25f);

        assertEquals(Map.of(hurt, HEART), dropped);
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

        assertEquals(Map.of(fighter, HEART), dropped);
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

        assertEquals(Map.of(fighter, HEART), dropped);
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
            assertEquals("AI tick 120 player 3 PLAN: next FIND_ROOM_SITE at 3,6 (room 1 is next, looking west)", lines.getLast());
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
    void headsForTheGoldWhenTheRoomCostsTooMuch() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 9 * ROOM_COST - 1);
        roomControl.setTypeAvailable(rooms.get(TREASURY), true, true);

        system.processTick(0.25f);

        assertEquals(KeeperAi.Task.GO_TO_GOLD, getKeeperAi().task);
        assertEquals(new Point(10, 6), getKeeperAi().target);
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

        // West is impenetrable and south too narrow, so it goes north of the heart with a tile in between
        Set<Point> site = new HashSet<>();
        for (int x = 2; x <= 4; x++) {
            for (int y = 1; y <= 3; y++) {
                site.add(new Point(x, y));
            }
        }
        assertEquals(site, new HashSet<>(built));
        assertEquals(9, built.size());
    }

    @Test
    void findsARoomSiteBehindTheGap() {
        KeeperAiRoomSiteFinder finder = new KeeperAiRoomSiteFinder(mapController, PLAYER_ID, ai(0, 0, 9, 1, 50, 0, List.of()));

        KeeperAiRoomSiteFinder.RoomSite site = finder.find(HEART, EAST, 9);

        assertEquals(new Point(6, 6), site.entrance());
        assertEquals(new Point(6, 7), site.corner());
        assertEquals(new Point(8, 5), site.oppositeCorner());
    }

    @Test
    void roomSiteGetsOneMoreRowWhenTheSizeIsNotSquare() {
        KeeperAiRoomSiteFinder finder = new KeeperAiRoomSiteFinder(mapController, PLAYER_ID, ai(0, 0, 12, 1, 50, 0, List.of()));

        KeeperAiRoomSiteFinder.RoomSite site = finder.find(HEART, EAST, 12);

        assertEquals(new Point(6, 7), site.corner());
        assertEquals(new Point(9, 5), site.oppositeCorner());

        // There is no room for a fourth row between the heart and the edge of the map
        assertNull(finder.find(HEART, NORTH, 12));
    }

    @Test
    void roomSiteStaysSquareWhenNoGapIsLeftBetweenRooms() {
        KeeperAiRoomSiteFinder finder = new KeeperAiRoomSiteFinder(mapController, PLAYER_ID, ai(0, 0, 12, 0, 50, 0, List.of()));

        KeeperAiRoomSiteFinder.RoomSite site = finder.find(HEART, EAST, 12);

        assertEquals(new Point(5, 7), site.corner());
        assertEquals(new Point(7, 5), site.oppositeCorner());
    }

    @Test
    void findsNoRoomSiteInImpenetrableRockOrOutsideTheBuildArea() {
        AI ai = ai(0, 0, 9, 1, 50, 0, List.of());

        assertNull(new KeeperAiRoomSiteFinder(mapController, PLAYER_ID, ai).find(HEART, WEST, 9));
        assertNull(new KeeperAiRoomSiteFinder(mapController, PLAYER_ID, withBuildAreaEndX(ai, 8)).find(HEART, EAST, 9));
    }

    @Test
    void findsNoRoomSiteWithTooMuchOpenSpaceAroundIt() {

        // 5 of the 16 tiles around the site are claimed path
        assertNull(new KeeperAiRoomSiteFinder(mapController, PLAYER_ID, ai(0, 0, 9, 1, 30, 0, List.of())).find(HEART, EAST, 9));
    }

    private KeeperAi getKeeperAi() {
        return entityData.getComponent(entityData.findEntity(null, KeeperAi.class), KeeperAi.class);
    }

    private KeeperAiSystem createSystem(AI ai, int gold) {
        keeper = new Keeper(player(PLAYER_ID, ai));
        if (lobbyType != null) {
            keeper = new Keeper(true, PLAYER_ID);
            keeper.setAiType(lobbyType);
        }
        Map<MiscType, Variable.MiscVariable> variables = Map.of(
                MiscType.COMBAT_PIT_MAX_EXPERIENCE_LEVEL, variable(MiscType.COMBAT_PIT_MAX_EXPERIENCE_LEVEL, 8),
                MiscType.MAXIMUM_MANA_THRESHOLD, variable(MiscType.MAXIMUM_MANA_THRESHOLD, 200000));
        PlayerManaControl manaControl = new PlayerManaControl(keeper, variables);
        handControl = new PlayerHandControl(keeper, 8, entityData);
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
            default -> throw new UnsupportedOperationException(method.getName());
        });
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getGameLevel" -> gameLevel(4);
            case "getKeeperSpells" -> List.of(createImp);
            case "getRooms" -> new ArrayList<>(rooms.values());
            case "getVariables" -> variables;
            case "getRoomById" -> rooms.get(((Number) args[0]).shortValue());
            default -> throw new UnsupportedOperationException(method.getName());
        });
        IGameWorldController gameWorldController = (IGameWorldController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IGameWorldController.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getMapController" -> {
                            return mapController;
                        }
                        case "castKeeperSpell" -> {
                            impSpells.add((Point) args[2]);
                            keeper.setMana(keeper.getMana() - createImp.getManaCost());
                        }
                        case "pickUp" ->
                            handControl.push((EntityId) args[0]);
                        case "drop" -> {
                            dropped.put(handControl.pop(), (Point) args[1]);
                            entityData.setComponent((EntityId) args[0], new Position(0, new Vector3f(((Vector2f) args[2]).x, 0, ((Vector2f) args[2]).y)));
                        }
                        case "build" -> {
                            Point p = new Point((int) ((Vector2f) args[0]).x, (int) ((Vector2f) args[0]).y);
                            built.add(p);
                            map[p.y][p.x] = 'H';
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
                    return null;
                });

        return new KeeperAiSystem(entityData, kwdFile, Map.of(PLAYER_ID, playerController), gameWorldController, () -> gameTime, logDecisions);
    }

    private IRoomController createRoom(short roomId, Point centre) {
        RoomInstance roomInstance = new RoomInstance(rooms.get(roomId));
        roomInstance.addCoordinate(centre);

        return (IRoomController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getRoom" -> rooms.get(roomId);
            case "isDungeonHeart" -> roomId == DUNGEON_HEART;
            case "getRoomInstance" -> roomInstance;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException(method.getName());
        });
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
                            return map[((Point) args[0]).y][((Point) args[0]).x] == '.';
                        }
                        case "selectTiles" -> {
                            Vector2f start = (Vector2f) args[0];
                            Vector2f end = (Vector2f) args[1];
                            for (int x = (int) start.x; x <= end.x; x++) {
                                for (int y = (int) start.y; y <= end.y; y++) {
                                    if (getTerrain(new Point(x, y)).getFlags().contains(TerrainFlag.TAGGABLE)) {
                                        tagged.add(new Point(x, y));
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
            case "getOwnerId" -> map[p.y][p.x] == '.' || map[p.y][p.x] == 'H' ? PLAYER_ID : (short) 0;
            case "getRoomId" -> map[p.y][p.x] == 'H' ? new EntityId(1) : null;
            case "isSelected" -> tagged.contains(p);
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }
}
