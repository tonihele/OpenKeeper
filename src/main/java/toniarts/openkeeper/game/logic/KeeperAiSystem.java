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
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.KeeperAi;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.AI;
import toniarts.openkeeper.tools.convert.map.AiPresets;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * The computer player. Every game tick each computer player gets as many
 * actions as its speed says, and spends them in the order DK II does. It
 * plays through the same actions a human player has: it tags tiles, builds
 * rooms and casts spells.<br>
 * So far it creates imps, mines gold, picks the rooms to build, has their
 * sites dug out and builds them, digs to neutral Portals, and pulls hurt
 * creatures out of fights.<br>
 * With the decision log on it writes a line for everything it decides: the
 * game tick, the player, the task it is on, what it does, where, and why.
 * The lines are there to be compared with DK II tick by tick.
 */
public final class KeeperAiSystem implements IGameLogicUpdatable {

    private static final Logger logger = System.getLogger(KeeperAiSystem.class.getName());

    /**
     * DK II gives up on a task after 1680 game ticks, at 4 ticks a second
     */
    private static final int TASK_TIME_OUT_SECONDS = 420;

    /**
     * DK II lets the tagged gold go 240 game ticks after its last dig order
     */
    private static final int UNTAG_GOLD_SECONDS = 60;
    private static final int CREATE_IMP_DISTANCE_FROM_HEART = 2;

    /**
     * DK II tags the gold in a 7 x 7 tile spiral
     */
    private static final int GOLD_TILES_TO_TAG = 49;

    private static final short ROOM_TREASURY = 1;
    private static final short ROOM_LAIR = 2;
    private static final short ROOM_PORTAL = 3;
    private static final short ROOM_HATCHERY = 4;
    private static final short ROOM_DUNGEON_HEART = 5;
    private static final short ROOM_LIBRARY = 6;
    private static final short ROOM_TRAINING_ROOM = 7;
    private static final short ROOM_WOODEN_BRIDGE = 8;
    private static final short ROOM_GUARD_ROOM = 9;
    private static final short ROOM_WORKSHOP = 10;
    private static final short ROOM_PRISON = 11;
    private static final short ROOM_TORTURE_CHAMBER = 12;
    private static final short ROOM_TEMPLE = 13;
    private static final short ROOM_GRAVEYARD = 14;
    private static final short ROOM_CASINO = 15;
    private static final short ROOM_COMBAT_PIT = 16;
    private static final short ROOM_STONE_BRIDGE = 17;
    private static final short ROOM_HERO_STONE_BRIDGE = 22;
    private static final short ROOM_HERO_PORTAL = 25;
    private static final short LAST_ROOM = 26;

    /**
     * The rooms DK II's computer player never builds
     */
    private static final Set<Short> ROOMS_NOT_BUILT = Set.of(ROOM_PORTAL, ROOM_DUNGEON_HEART, ROOM_WOODEN_BRIDGE,
            ROOM_GUARD_ROOM, ROOM_STONE_BRIDGE, ROOM_HERO_STONE_BRIDGE);

    /**
     * The room DK II's computer player likes to have each room next to
     */
    private static final Map<Short, Short> IDEAL_NEIGHBOURS = Map.ofEntries(
            Map.entry(ROOM_TREASURY, ROOM_DUNGEON_HEART),
            Map.entry(ROOM_LAIR, ROOM_HATCHERY),
            Map.entry(ROOM_HATCHERY, ROOM_LAIR),
            Map.entry(ROOM_LIBRARY, ROOM_LAIR),
            Map.entry(ROOM_TRAINING_ROOM, ROOM_DUNGEON_HEART),
            Map.entry(ROOM_WORKSHOP, ROOM_DUNGEON_HEART),
            Map.entry(ROOM_PRISON, ROOM_TRAINING_ROOM),
            Map.entry(ROOM_TORTURE_CHAMBER, ROOM_PRISON),
            Map.entry(ROOM_TEMPLE, ROOM_TORTURE_CHAMBER),
            Map.entry(ROOM_GRAVEYARD, ROOM_TORTURE_CHAMBER),
            Map.entry(ROOM_CASINO, ROOM_TREASURY),
            Map.entry(ROOM_COMBAT_PIT, ROOM_PRISON));

    /**
     * The directions of a room site search, for the decision log
     */
    private static final String[] DIRECTIONS = {"north", "west", "south", "east"};

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final Map<Short, IPlayerController> playerControllers;
    private final IGameWorldController gameWorldController;
    private final IMapController mapController;
    private final IGameTimer gameTimer;
    private final boolean logDecisions;

    /**
     * The settings of each computer player: its type's in a game set up in
     * the lobby, otherwise the ones the level gives it
     */
    private final Map<Short, AI> ais = new HashMap<>();
    private final KeeperAiBattles battles;
    private final EntitySet keeperAis;

    /**
     * @param entityData the entities, the computer players keep their state
     * there
     * @param kwdFile the level
     * @param playerControllers all the players by id
     * @param gameWorldController the player actions
     * @param gameTimer the game time, for the decision log
     * @param logDecisions whether to log what the computer players decide
     */
    public KeeperAiSystem(EntityData entityData, IKwdFile kwdFile, Map<Short, IPlayerController> playerControllers,
            IGameWorldController gameWorldController, IGameTimer gameTimer, boolean logDecisions) {
        this.gameTimer = gameTimer;
        this.logDecisions = logDecisions;
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.playerControllers = playerControllers;
        this.gameWorldController = gameWorldController;
        this.mapController = gameWorldController.getMapController();
        battles = new KeeperAiBattles(entityData, kwdFile, gameWorldController, gameTimer, this::log);

        // The keepers the level or the game setup gives to the computer, a loaded game has them already
        keeperAis = entityData.getEntities(KeeperAi.class);
        for (IPlayerController playerController : playerControllers.values()) {
            Keeper keeper = playerController.getKeeper();
            if (keeper.getId() < Player.KEEPER1_ID || !(keeper.isAi() || keeper.getPlayer().isAi())) {
                continue;
            }
            AI ai = keeper.isAi() ? AiPresets.create(AiPresets.getTypeOnMap(keeper.getAiType(), kwdFile.getGameLevel().getName()),
                    kwdFile.getRooms(), (int) kwdFile.getVariables().get(Variable.MiscVariable.MiscType.COMBAT_PIT_MAX_EXPERIENCE_LEVEL).getValue(),
                    mapController.getMapData().getWidth(), mapController.getMapData().getHeight())
                    : keeper.getPlayer().getAiAttributes();
            ais.put(keeper.getId(), ai);
            if (keeperAis.stream().noneMatch(entity -> entity.get(KeeperAi.class).playerId == keeper.getId())) {
                entityData.setComponent(entityData.createEntity(), new KeeperAi(keeper.getId()));

                // A computer player type comes with some mana to start with
                if (keeper.isAi()) {
                    playerController.getManaControl().addMana(ai.getStartingMana());
                }
            }
        }
    }

    @Override
    public void processTick(float tpf) {
        keeperAis.applyChanges();
        battles.applyChanges();
        for (Entity entity : keeperAis) {
            KeeperAi keeperAi = new KeeperAi(entity.get(KeeperAi.class));
            IPlayerController playerController = playerControllers.get(keeperAi.playerId);
            if (playerController == null || !ais.containsKey(keeperAi.playerId) || playerController.getKeeper().isDestroyed()
                    || playerController.getRoomControl().getDungeonHeart() == null) {
                continue;
            }

            processKeeper(keeperAi, playerController);
            entityData.setComponent(entity.getId(), keeperAi);
        }
    }

    private void processKeeper(KeeperAi keeperAi, IPlayerController playerController) {
        AI ai = ais.get(keeperAi.playerId);
        int ticksPerSecond = kwdFile.getGameLevel().getTicksPerSec();
        int actions = ai.getSpeed();

        // A task that takes too long is dropped
        keeperAi.taskTicks++;
        if (keeperAi.taskTicks > TASK_TIME_OUT_SECONDS * ticksPerSecond) {
            setTask(keeperAi, KeeperAi.Task.PLAN, "the task took too long");
            keeperAi.nextTask = KeeperAi.Task.PLAN;
        }

        // The tagged gold is let go a while after the last dig order
        if (keeperAi.untagTicks > 0) {
            keeperAi.untagTicks--;
            if (keeperAi.untagTicks == 0) {
                log(keeperAi, "untag gold", null, "no dig order for a while");
                untagGold(keeperAi.playerId);
            }
        }

        // The Call to Arms flag comes down after its time, and then there is no new one for a while
        if (keeperAi.callToArmsTicks > 0) {
            keeperAi.callToArmsTicks--;
        }
        if (keeperAi.cannotAttackTicks > 0) {
            keeperAi.cannotAttackTicks--;
        }
        battles.liftCallToArms(keeperAi, playerControllers, ai, ticksPerSecond);

        // An attack to answer takes the whole turn
        if (keeperAi.attackLookTicks > 0) {
            keeperAi.attackLookTicks--;
        }
        battles.lookForAttack(keeperAi, playerControllers, ai, ticksPerSecond);
        if (actions > 0 && battles.respondToAttack(keeperAi, playerController)) {
            actions = 0;
        }

        // DK II reads the level a level's computer player trains its creatures to before the level's rules, as 0
        if (actions > 0 && battles.retreatInjured(keeperAi, playerController, ai,
                playerController.getKeeper().isAi() ? ai.getMinLevelForCreatures() : 0)) {
            actions--;
        }

        // In a fight it sees to the fight, otherwise to its imps
        if (battles.isFighting(keeperAi.playerId)) {
            if (actions > 0 && (battles.callToArms(keeperAi, playerController, ai, ticksPerSecond)
                    || battles.castBattleSpell(keeperAi, playerController, ai))) {
                actions--;
            }
        } else if (actions > 0 && createImp(keeperAi, playerController, ai)) {
            actions--;
        }

        // What a failed drop left in its hand is put down, more or less often by its settings
        AI.MoveToResearchPolicy handPolicy = ai.getLikelyhoodToMovingCreaturesToLibraryForResearching();
        if (actions > 0 && (handPolicy == null || handPolicy.ordinal() <= Utils.getRandom().nextInt(AI.MoveToResearchPolicy.values().length))
                && battles.emptyHand(keeperAi, playerController)) {
            actions--;
        }

        while (actions > 0) {
            actions = switch (keeperAi.task) {
                case PLAN -> {
                    planRoom(keeperAi, playerController, ai);
                    if (keeperAi.task == KeeperAi.Task.PLAN) {
                        mineGoldIfLow(keeperAi, playerController, ai);
                    }
                    yield actions - 1;
                }
                case FIND_ROOM_SITE ->
                    findRoomSite(keeperAi, playerController, ai, actions);
                case TAG_ROOM ->
                    tagRoom(keeperAi, ticksPerSecond, actions);
                case BUILD_ROOM ->
                    buildRoom(keeperAi, ticksPerSecond, actions);
                case FINISH -> {
                    mineGoldIfLow(keeperAi, playerController, ai);
                    if (keeperAi.task == KeeperAi.Task.FINISH) {
                        setTask(keeperAi, KeeperAi.Task.PLAN, "done");
                    }
                    yield actions - 1;
                }
                case GO_TO_GOLD -> {

                    // From the nearest room
                    keeperAi.current = getNearestRoom(playerController, keeperAi.target).getRoomInstance().getCenter();
                    setTask(keeperAi, KeeperAi.Task.DIG_PATH, "from the nearest room at " + keeperAi.current.x + "," + keeperAi.current.y);
                    keeperAi.nextTask = KeeperAi.Task.TAG_GOLD;
                    yield actions - 1;
                }
                case TAG_GOLD ->
                    tagGold(keeperAi, ticksPerSecond, actions);
                case DIG_PATH ->
                    digPath(keeperAi, playerController, ai, ticksPerSecond, actions);
            };
        }
    }

    private void setTask(KeeperAi keeperAi, KeeperAi.Task task, String reason) {
        setTask(keeperAi, task, keeperAi.target, reason);
    }

    private void setTask(KeeperAi keeperAi, KeeperAi.Task task, Point target, String reason) {
        log(keeperAi, "next " + task, target, reason);
        keeperAi.task = task;
        keeperAi.taskTicks = 0;
    }

    /**
     * Writes a line to the decision log, if it is on
     *
     * @param keeperAi the computer player, with the task it is on
     * @param decision what it does
     * @param target the tile it does it to, if any
     * @param reason why
     */
    private void log(KeeperAi keeperAi, String decision, Point target, String reason) {
        if (logDecisions) {
            logger.log(Level.INFO, String.format("AI tick %d player %d %s: %s%s (%s)",
                    Math.round(gameTimer.getGameTime() * kwdFile.getGameLevel().getTicksPerSec()), keeperAi.playerId,
                    keeperAi.task, decision, target != null ? " at " + target.x + "," + target.y : "", reason));
        }
    }

    /**
     * Creates an imp by the Dungeon Heart when there are fewer imps than the
     * computer player wants for the land it owns
     *
     * @return true if the spell was cast
     */
    private boolean createImp(KeeperAi keeperAi, IPlayerController playerController, AI ai) {
        Keeper keeper = playerController.getKeeper();
        int imps = playerController.getCreatureControl().getImpCount();
        int tilesPerImp = ai.getImpsToTilesRatio();
        if (imps >= ai.getMaximumNumberOfImps() || tilesPerImp == 0
                || imps >= countOwnedTiles(keeper.getId()) / tilesPerImp) {
            return false;
        }

        KeeperSpell createImp = null;
        for (KeeperSpell keeperSpell : kwdFile.getKeeperSpells()) {
            if (keeperSpell.getFlags().contains(KeeperSpell.KeeperSpellFlag.CREATE_WORKER)) {
                createImp = keeperSpell;
                break;
            }
        }
        if (createImp == null) {
            return false;
        }

        // On a corner of the Dungeon Heart
        Point heart = keeper.getDungeonHeartLocation();
        Point tile = new Point(
                heart.x + (Utils.getRandom().nextBoolean() ? CREATE_IMP_DISTANCE_FROM_HEART : -CREATE_IMP_DISTANCE_FROM_HEART),
                heart.y + (Utils.getRandom().nextBoolean() ? CREATE_IMP_DISTANCE_FROM_HEART : -CREATE_IMP_DISTANCE_FROM_HEART));
        int mana = keeper.getMana();
        gameWorldController.castKeeperSpell(createImp.getKeeperSpellId(), null, tile, WorldUtils.pointToVector2f(tile), keeper.getId());

        // The cast has its own rules, the mana tells if it went through
        if (keeper.getMana() < mana) {
            log(keeperAi, "cast Create Imp", tile, imps + " imps, fewer than its land and its limit allow");
            return true;
        }

        return false;
    }

    private int countOwnedTiles(short playerId) {
        int tiles = 0;
        for (int x = 0; x < mapController.getMapData().getWidth(); x++) {
            for (int y = 0; y < mapController.getMapData().getHeight(); y++) {
                if (mapController.getMapData().getTile(x, y).getOwnerId() == playerId) {
                    tiles++;
                }
            }
        }

        return tiles;
    }

    private void untagGold(short playerId) {
        for (Point tile : getTaggedGold(playerId)) {
            mapController.selectTiles(new Vector2f(tile.x, tile.y), new Vector2f(tile.x, tile.y), false, playerId);
        }
    }

    private List<Point> getTaggedGold(short playerId) {
        List<Point> tiles = new ArrayList<>();
        for (int x = 0; x < mapController.getMapData().getWidth(); x++) {
            for (int y = 0; y < mapController.getMapData().getHeight(); y++) {
                IMapTileInformation tile = mapController.getMapData().getTile(x, y);
                if (tile.isSelected(playerId) && mapController.getTerrain(tile).getGoldValue() > 0) {
                    tiles.add(tile.getLocation());
                }
            }
        }

        return tiles;
    }

    private void mineGoldIfLow(KeeperAi keeperAi, IPlayerController playerController, AI ai) {
        if (playerController.getKeeper().getGold() < ai.getMineGoldUntilGoldHeldIsGreaterThan()) {
            mineGold(keeperAi, playerController);
        }
    }

    /**
     * Heads for the nearest gold, unless there is gold tagged already
     */
    private void mineGold(KeeperAi keeperAi, IPlayerController playerController) {
        if (!getTaggedGold(keeperAi.playerId).isEmpty()) {
            return;
        }
        Point gold = findGold(playerController);
        if (gold != null) {
            keeperAi.target = gold;
            setTask(keeperAi, KeeperAi.Task.GO_TO_GOLD, "low on gold with " + playerController.getKeeper().getGold());
            keeperAi.goldTiles = 0;
        }
    }

    /**
     * Groups the gold of the whole map into seams and takes the middle of
     * the seam that is nearest to one of the player's rooms
     */
    private Point findGold(IPlayerController playerController) {
        int width = mapController.getMapData().getWidth();
        int height = mapController.getMapData().getHeight();
        boolean[][] seen = new boolean[width][height];
        Point nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (seen[x][y] || !hasGold(x, y)) {
                    continue;
                }

                // The whole seam
                int tiles = 0;
                int sumX = 0;
                int sumY = 0;
                List<Point> open = new ArrayList<>();
                open.add(new Point(x, y));
                seen[x][y] = true;
                while (!open.isEmpty()) {
                    Point p = open.removeLast();
                    tiles++;
                    sumX += p.x;
                    sumY += p.y;
                    for (Point neighbour : new Point[]{new Point(p.x - 1, p.y), new Point(p.x + 1, p.y), new Point(p.x, p.y - 1), new Point(p.x, p.y + 1)}) {
                        if (hasGold(neighbour.x, neighbour.y) && !seen[neighbour.x][neighbour.y]) {
                            seen[neighbour.x][neighbour.y] = true;
                            open.add(neighbour);
                        }
                    }
                }

                Point middle = getNearestReachableTile(new Point(sumX / tiles, sumY / tiles));
                if (middle == null) {
                    continue;
                }
                int distance = getDistance(getNearestRoom(playerController, middle).getRoomInstance().getCenter(), middle);
                if (distance < nearestDistance) {
                    nearest = middle;
                    nearestDistance = distance;
                }
            }
        }

        return nearest;
    }

    private boolean hasGold(int x, int y) {
        IMapTileInformation tile = mapController.getMapData().getTile(x, y);

        return tile != null && mapController.getTerrain(tile).getGoldValue() > 0;
    }

    /**
     * The tile itself, or the nearest tile around it that is not
     * impenetrable
     */
    private Point getNearestReachableTile(Point p) {
        int furthest = Math.max(mapController.getMapData().getWidth(), mapController.getMapData().getHeight());
        for (int distance = 0; distance < furthest; distance++) {
            for (int x = p.x - distance; x <= p.x + distance; x++) {
                for (int y = p.y - distance; y <= p.y + distance; y++) {
                    IMapTileInformation tile = mapController.getMapData().getTile(x, y);
                    if (Math.max(Math.abs(x - p.x), Math.abs(y - p.y)) == distance && tile != null
                            && !mapController.getTerrain(tile).getFlags().contains(Terrain.TerrainFlag.IMPENETRABLE)) {
                        return tile.getLocation();
                    }
                }
            }
        }

        return null;
    }

    private static int getDistance(Point from, Point to) {
        return (from.x - to.x) * (from.x - to.x) + (from.y - to.y) * (from.y - to.y);
    }

    private IRoomController getNearestRoom(IPlayerController playerController, Point p) {
        IRoomController nearest = playerController.getRoomControl().getDungeonHeart();
        int nearestDistance = Integer.MAX_VALUE;
        for (Set<IRoomController> rooms : playerController.getRoomControl().getRoomControllers().values()) {
            for (IRoomController room : rooms) {
                int distance = getDistance(room.getRoomInstance().getCenter(), p);
                if (distance < nearestDistance) {
                    nearest = room;
                    nearestDistance = distance;
                }
            }
        }

        return nearest;
    }

    /**
     * Looks at one tile around the gold a turn, in a spiral from the middle
     * out, and tags the ones with gold
     */
    private int tagGold(KeeperAi keeperAi, int ticksPerSecond, int actions) {
        while (actions > 0 && keeperAi.task == KeeperAi.Task.TAG_GOLD) {
            actions--;

            // Down one tile, then round and round with every second side one tile longer
            int x = keeperAi.target.x;
            int y = keeperAi.target.y;
            int dx = 0;
            int dy = 1;
            int sideX = 1;
            int sideY = 1;
            boolean vertical = true;
            if (keeperAi.goldTiles > 0) {
                x++;
            }
            for (int i = 1; i < keeperAi.goldTiles; i++) {
                if (vertical) {
                    y += dy;
                    sideY--;
                    if (sideY == 0) {
                        sideX++;
                        sideY = sideX;
                        dx = -dy;
                        vertical = false;
                    }
                } else {
                    x += dx;
                    sideX--;
                    if (sideX == 0) {
                        sideX = sideY;
                        dy = dx;
                        vertical = true;
                    }
                }
            }

            Point tile = new Point(x, y);
            if (hasGold(x, y) && mapController.isTaggable(tile)) {
                tag(keeperAi, tile, tile, ticksPerSecond, "gold");
            }
            keeperAi.goldTiles++;
            if (keeperAi.goldTiles == GOLD_TILES_TO_TAG) {
                setTask(keeperAi, KeeperAi.Task.PLAN, "looked at all the tiles around the gold");
            }
        }

        return actions;
    }

    /**
     * Picks the next room to build and where to start looking for a place
     * for it. Every other plan is for a Portal: after each room it picks,
     * and when there is no room to pick, it looks for a neutral Portal to
     * dig to. A neutral room of the kind it wants is dug to as well, rather
     * than built
     */
    private void planRoom(KeeperAi keeperAi, IPlayerController playerController, AI ai) {
        keeperAi.roomId = 0;
        if (!keeperAi.roomWanted) {
            Room wanted = getNextRoom(playerController, ai);
            keeperAi.wantedRoomId = wanted != null ? wanted.getRoomId() : ROOM_PORTAL;
            keeperAi.roomWanted = true;
        }
        short roomId = keeperAi.wantedRoomId;
        if (roomId == ROOM_PORTAL) {
            keeperAi.roomWanted = false;
        } else {
            keeperAi.wantedRoomId = ROOM_PORTAL;
        }
        if (digToNeutralRoom(keeperAi, playerController, ai, roomId)) {
            return;
        }

        Room room = roomId == ROOM_PORTAL ? null : kwdFile.getRoomById(roomId);
        if (room == null || ai.getDefaultSize() < 1) {
            keeperAi.taskTicks = 0;
            return;
        }

        // The room at its default size has to be affordable, otherwise mine gold first
        if (room.getCost() * ai.getDefaultSize() > playerController.getKeeper().getGold()) {
            mineGold(keeperAi, playerController);
            return;
        }

        keeperAi.roomId = room.getRoomId();
        keeperAi.direction = getPreferredDirection(keeperAi.roomId, playerController.getKeeper());
        if (!isCoreRoom(keeperAi.roomId)) {
            keeperAi.direction = (keeperAi.direction + Utils.getRandom().nextInt(3) - 1) & 3;
        }

        // Next to a room it goes well with, or next to any room
        keeperAi.current = null;
        Short idealNeighbour = IDEAL_NEIGHBOURS.get(keeperAi.roomId);
        if (ai.getDistanceBetweenRoomsThatShouldBeCloseMan() == AI.Distance.CLOSE && idealNeighbour != null) {
            List<IRoomController> neighbours = new ArrayList<>();
            for (IRoomController neighbour : playerController.getRoomControl().getRoomControllers().getOrDefault(kwdFile.getRoomById(idealNeighbour), Set.of())) {
                if (!neighbour.getRoomInstance().isDestroyed()) {
                    neighbours.add(neighbour);
                }
            }
            if (!neighbours.isEmpty()) {
                keeperAi.current = Utils.getRandomItem(neighbours).getRoomInstance().getCenter();
            }
        }
        if (keeperAi.current == null) {
            keeperAi.current = getRandomRoomLocation(playerController);
        }
        setTask(keeperAi, KeeperAi.Task.FIND_ROOM_SITE, keeperAi.current, "room " + keeperAi.roomId + " is next, looking " + DIRECTIONS[keeperAi.direction]);
    }

    /**
     * Has a path dug to the nearest neutral room of a kind, so that the
     * imps claim it. A Portal has to be near one of the computer player's
     * rooms, any other room near its Dungeon Heart, or else some of its
     * land has to lie on the edge of a square around the room
     *
     * @return true if it digs to a room
     */
    private boolean digToNeutralRoom(KeeperAi keeperAi, IPlayerController playerController, AI ai, short roomId) {
        boolean portal = roomId == ROOM_PORTAL || roomId == ROOM_HERO_PORTAL;
        Point heart = playerController.getKeeper().getDungeonHeartLocation();
        Point target = null;
        for (IRoomController room : mapController.getRoomControllers()) {
            short id = room.getRoomInstance().getRoom().getRoomId();
            Point centre = room.getRoomInstance().getCenter();
            if (room.getRoomInstance().getOwnerId() == Player.NEUTRAL_PLAYER_ID && !room.getRoomInstance().isDestroyed()
                    && (portal ? id == ROOM_PORTAL || id == ROOM_HERO_PORTAL : id == roomId)
                    && (target == null || getDistance(heart, centre) < getDistance(heart, target))) {
                target = centre;
            }
        }
        if (target == null || target.x < ai.getBuildAreaStartX() || target.x > ai.getBuildAreaEndX()
                || target.y < ai.getBuildAreaStartY() || target.y > ai.getBuildAreaEndY()) {
            return false;
        }

        Point start = getNearestRoom(playerController, target).getRoomInstance().getCenter();
        int range = ai.getDigToNeutralRoomsWithinTilesOfHeart();
        if (portal ? getDistance(start, target) > range * range : getDistance(heart, target) >= range * range) {
            start = getNearestOwnTileAround(keeperAi.playerId, target, ai.getDigToNeutralRoomsWithinTilesOfClaimedArea() + 1);
            if (start == null) {
                return false;
            }
        }

        keeperAi.roomId = roomId;
        keeperAi.current = start;
        setTask(keeperAi, KeeperAi.Task.DIG_PATH, target, "a neutral room " + roomId + " to claim, from " + start.x + "," + start.y);
        keeperAi.target = target;
        keeperAi.nextTask = KeeperAi.Task.FINISH;

        return true;
    }

    /**
     * The player's tile nearest to a place, of the tiles on the edge of a
     * square around the place
     */
    private Point getNearestOwnTileAround(short playerId, Point centre, int distance) {
        Point nearest = null;
        for (int x = centre.x - distance; x <= centre.x + distance; x++) {
            for (int y = centre.y - distance; y <= centre.y + distance; y++) {
                IMapTileInformation tile = mapController.getMapData().getTile(x, y);
                if (Math.max(Math.abs(x - centre.x), Math.abs(y - centre.y)) == distance && tile != null && tile.getOwnerId() == playerId
                        && (nearest == null || getDistance(tile.getLocation(), centre) < getDistance(nearest, centre))) {
                    nearest = tile.getLocation();
                }
            }
        }

        return nearest;
    }

    /**
     * The first room of the computer player's build order, and then of the
     * rest of the rooms, that it may build and doesn't have yet
     */
    private Room getNextRoom(IPlayerController playerController, AI ai) {
        List<Short> roomIds = new ArrayList<>(ai.getBuildOrder());
        for (short roomId = (short) (roomIds.size() + 1); roomId <= LAST_ROOM; roomId++) {
            roomIds.add(roomId);
        }
        for (short roomId : roomIds) {
            Room room = kwdFile.getRoomById(roomId);
            if (room == null || ROOMS_NOT_BUILT.contains(roomId)) {
                continue;
            }
            ResearchableEntity availability = playerController.getRoomControl().getTypes().get(room);
            if (availability != null && availability.isDiscovered() && playerController.getRoomControl().getTypeCount(room) == 0) {
                return room;
            }
        }

        return null;
    }

    private Point getRandomRoomLocation(IPlayerController playerController) {
        List<IRoomController> rooms = new ArrayList<>();
        for (Map.Entry<Room, Set<IRoomController>> entry : playerController.getRoomControl().getRoomControllers().entrySet()) {
            if (entry.getKey().getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAND)) {
                rooms.addAll(entry.getValue());
            }
        }
        if (rooms.isEmpty()) {
            return playerController.getKeeper().getDungeonHeartLocation();
        }

        return Utils.getRandomItem(rooms).getRoomInstance().getCenter();
    }

    private static boolean isCoreRoom(short roomId) {
        return roomId <= ROOM_DUNGEON_HEART;
    }

    /**
     * The treasury, lair and hatchery go on the side of the Dungeon Heart
     * that faces the nearest edge of the map, the other rooms on the side
     * that faces the middle of the map
     *
     * @return 0 north, 1 west, 2 south, 3 east
     */
    private int getPreferredDirection(short roomId, Keeper keeper) {
        int dx = keeper.getDungeonHeartLocation().x - mapController.getMapData().getWidth() / 2;
        int dy = keeper.getDungeonHeartLocation().y - mapController.getMapData().getHeight() / 2;
        int direction;
        if (Math.abs(dy) < Math.abs(dx)) {
            direction = dx < 1 ? 1 : 3;
        } else {
            direction = dy > 0 ? 2 : 0;
        }

        return isCoreRoom(roomId) ? direction : (direction + 2) & 3;
    }

    /**
     * Tries one direction a tick: the preferred one, then both sides and
     * then the opposite one. After that it mostly tries again from another
     * room
     */
    private int findRoomSite(KeeperAi keeperAi, IPlayerController playerController, AI ai, int actions) {
        KeeperAiRoomSiteFinder.RoomSite site = new KeeperAiRoomSiteFinder(mapController, keeperAi.playerId, ai)
                .find(keeperAi.current, keeperAi.direction, ai.getDefaultSize());
        if (site != null) {

            // First the path to the site
            keeperAi.target = site.entrance();
            keeperAi.corner = site.corner();
            keeperAi.oppositeCorner = site.oppositeCorner();
            setTask(keeperAi, KeeperAi.Task.DIG_PATH, "site for room " + keeperAi.roomId + " from " + site.corner().x + "," + site.corner().y
                    + " to " + site.oppositeCorner().x + "," + site.oppositeCorner().y + ", " + DIRECTIONS[keeperAi.direction] + " of " + keeperAi.current.x + "," + keeperAi.current.y);
            keeperAi.nextTask = KeeperAi.Task.TAG_ROOM;
            return actions - 1;
        }

        int preferred = getPreferredDirection(keeperAi.roomId, playerController.getKeeper());
        if (keeperAi.direction == preferred) {
            keeperAi.direction = (preferred + 1) & 3;
        } else if (keeperAi.direction == ((preferred + 1) & 3)) {
            keeperAi.direction = (preferred - 1) & 3;
        } else if (keeperAi.direction == ((preferred - 1) & 3)) {
            keeperAi.direction = (preferred - 2) & 3;
        } else if (Utils.getRandom().nextInt(100) > 4) {
            keeperAi.direction = preferred;
            keeperAi.current = getRandomRoomLocation(playerController);
        } else {
            setTask(keeperAi, KeeperAi.Task.PLAN, "no site for room " + keeperAi.roomId);
        }

        return 0;
    }

    /**
     * Digs the path tile by tile, each tile is tagged only when the one
     * before it has been dug out. Water and lava get a bridge
     */
    private int digPath(KeeperAi keeperAi, IPlayerController playerController, AI ai, int ticksPerSecond, int actions) {

        // No paths out of the build area
        if (keeperAi.target.x < ai.getBuildAreaStartX() || keeperAi.target.x > ai.getBuildAreaEndX()
                || keeperAi.target.y < ai.getBuildAreaStartY() || keeperAi.target.y > ai.getBuildAreaEndY()) {
            setTask(keeperAi, KeeperAi.Task.FINISH, "the path leads out of the build area");
            keeperAi.nextTask = KeeperAi.Task.FINISH;
            return 0;
        }

        while (actions > 0) {
            actions--;
            if (keeperAi.current.equals(keeperAi.target)) {
                finishPath(keeperAi, "the path is dug");
                return actions;
            }

            IMapTileInformation tile = mapController.getMapData().getTile(keeperAi.current);
            Terrain terrain = mapController.getTerrain(tile);
            if (terrain.getFlags().contains(Terrain.TerrainFlag.WATER) || terrain.getFlags().contains(Terrain.TerrainFlag.LAVA)) {
                Room bridge = getBridge(playerController, terrain);
                if (bridge == null) {
                    finishPath(keeperAi, "no bridge to cross with");
                    return actions;
                }
                if (mapController.isBuildable(keeperAi.current, keeperAi.playerId, bridge.getRoomId())) {
                    Vector2f position = new Vector2f(keeperAi.current.x, keeperAi.current.y);
                    gameWorldController.build(position, position, keeperAi.playerId, bridge.getRoomId());
                    log(keeperAi, "build room " + bridge.getRoomId(), keeperAi.current, "the path crosses water or lava");
                    if (mapController.getRoomControllerByCoordinates(keeperAi.current) != null && !stepOnPath(keeperAi)) {
                        finishPath(keeperAi, "no way on from here");
                        return actions;
                    }
                }
            } else if (!terrain.getFlags().contains(Terrain.TerrainFlag.TAGGABLE)) {
                if (!stepOnPath(keeperAi)) {
                    finishPath(keeperAi, "no way on from here");
                    return actions;
                }
            } else if (!tile.isSelected(keeperAi.playerId)) {
                tag(keeperAi, keeperAi.current, keeperAi.current, ticksPerSecond, "next tile of the path");
            }
        }

        return 0;
    }

    private void finishPath(KeeperAi keeperAi, String reason) {
        setTask(keeperAi, keeperAi.nextTask, reason);
        keeperAi.nextTask = KeeperAi.Task.FINISH;
    }

    /**
     * One tile closer to the target, along the longer way first. Rock that
     * can't be dug is walked round the shortest way
     *
     * @return false if there is no way to the target
     */
    private boolean stepOnPath(KeeperAi keeperAi) {
        int width = mapController.getMapData().getWidth();
        int height = mapController.getMapData().getHeight();

        // How far each tile is from the target, as far out as the tile the path is on
        int[][] distances = new int[width][height];
        List<Point> open = new ArrayList<>();
        open.add(keeperAi.target);
        distances[keeperAi.target.x][keeperAi.target.y] = 1;
        for (int i = 0; i < open.size() && distances[keeperAi.current.x][keeperAi.current.y] == 0; i++) {
            Point p = open.get(i);
            for (Point neighbour : new Point[]{new Point(p.x - 1, p.y), new Point(p.x + 1, p.y), new Point(p.x, p.y - 1), new Point(p.x, p.y + 1)}) {
                IMapTileInformation tile = mapController.getMapData().getTile(neighbour);
                if (tile == null || distances[neighbour.x][neighbour.y] != 0) {
                    continue;
                }
                Set<Terrain.TerrainFlag> flags = mapController.getTerrain(tile).getFlags();
                if (neighbour.equals(keeperAi.current) || !flags.contains(Terrain.TerrainFlag.SOLID) || flags.contains(Terrain.TerrainFlag.TAGGABLE)) {
                    distances[neighbour.x][neighbour.y] = distances[p.x][p.y] + 1;
                    open.add(neighbour);
                }
            }
        }

        int distance = distances[keeperAi.current.x][keeperAi.current.y];
        int dx = keeperAi.target.x - keeperAi.current.x;
        int dy = keeperAi.target.y - keeperAi.current.y;
        Point alongX = new Point(keeperAi.current.x + (dx < 0 ? -1 : 1), keeperAi.current.y);
        Point alongY = new Point(keeperAi.current.x, keeperAi.current.y + (dy < 0 ? -1 : 1));
        boolean xFirst = Math.abs(dx) > Math.abs(dy);
        for (Point next : new Point[]{xFirst ? alongX : alongY, xFirst ? alongY : alongX,
            new Point(2 * keeperAi.current.x - alongX.x, keeperAi.current.y), new Point(keeperAi.current.x, 2 * keeperAi.current.y - alongY.y)}) {
            if (distance > 1 && next.x >= 0 && next.y >= 0 && next.x < width && next.y < height && distances[next.x][next.y] == distance - 1) {
                keeperAi.current = next;
                return true;
            }
        }

        return false;
    }

    /**
     * A wooden bridge over water if the player has it, otherwise a stone
     * bridge
     */
    private Room getBridge(IPlayerController playerController, Terrain terrain) {
        for (short roomId : new short[]{ROOM_WOODEN_BRIDGE, ROOM_STONE_BRIDGE}) {
            if (roomId == ROOM_WOODEN_BRIDGE && !terrain.getFlags().contains(Terrain.TerrainFlag.WATER)) {
                continue;
            }
            Room room = kwdFile.getRoomById(roomId);
            ResearchableEntity availability = room != null ? playerController.getRoomControl().getTypes().get(room) : null;
            if (availability != null && availability.isDiscovered()) {
                return room;
            }
        }

        return null;
    }

    private void tag(KeeperAi keeperAi, Point from, Point to, int ticksPerSecond, String reason) {
        log(keeperAi, from.equals(to) ? "tag" : "tag to " + to.x + "," + to.y, from, reason);
        mapController.selectTiles(new Vector2f(Math.min(from.x, to.x), Math.min(from.y, to.y)),
                new Vector2f(Math.max(from.x, to.x), Math.max(from.y, to.y)), true, keeperAi.playerId);
        keeperAi.untagTicks = UNTAG_GOLD_SECONDS * ticksPerSecond;
    }

    private int tagRoom(KeeperAi keeperAi, int ticksPerSecond, int actions) {
        tag(keeperAi, keeperAi.corner, keeperAi.oppositeCorner, ticksPerSecond, "the site of room " + keeperAi.roomId);
        setTask(keeperAi, KeeperAi.Task.BUILD_ROOM, "the site is tagged");
        keeperAi.current = keeperAi.corner;

        return actions - 1;
    }

    /**
     * Goes through the room site tile by tile. It waits for the imps to dig
     * out and claim the tile, then places the room on it
     */
    private int buildRoom(KeeperAi keeperAi, int ticksPerSecond, int actions) {
        while (actions > 0) {
            actions--;
            if (mapController.isBuildable(keeperAi.current, keeperAi.playerId, keeperAi.roomId)) {
                Vector2f position = new Vector2f(keeperAi.current.x, keeperAi.current.y);
                gameWorldController.build(position, position, keeperAi.playerId, keeperAi.roomId);
                log(keeperAi, "build room " + keeperAi.roomId, keeperAi.current, "the tile is dug out and claimed");
                return actions;
            }

            IMapTileInformation tile = mapController.getMapData().getTile(keeperAi.current);
            if (mapController.isSolid(keeperAi.current) || tile.getOwnerId() != keeperAi.playerId) {

                // Not ours yet, see that it is still tagged
                if (!tile.isSelected(keeperAi.playerId) && mapController.isTaggable(keeperAi.current)) {
                    tag(keeperAi, keeperAi.current, keeperAi.current, ticksPerSecond, "a tile of the site lost its tag");
                }
            } else if (keeperAi.current.x != keeperAi.oppositeCorner.x) {
                keeperAi.current = new Point(keeperAi.current.x + Integer.signum(keeperAi.oppositeCorner.x - keeperAi.corner.x), keeperAi.current.y);
            } else if (keeperAi.current.y != keeperAi.oppositeCorner.y) {
                keeperAi.current = new Point(keeperAi.corner.x, keeperAi.current.y + Integer.signum(keeperAi.oppositeCorner.y - keeperAi.corner.y));
            } else {
                setTask(keeperAi, KeeperAi.Task.FINISH, "room " + keeperAi.roomId + " is built");
                return 0;
            }
        }

        return 0;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        keeperAis.release();
        battles.release();
    }

}
