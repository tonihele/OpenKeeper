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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.KeeperAi;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.TileWork;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;
import toniarts.openkeeper.tools.convert.map.AI;
import toniarts.openkeeper.tools.convert.map.AiPresets;
import toniarts.openkeeper.tools.convert.map.GameObject;
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
 * sites dug out and builds them, digs to neutral Portals, fights, invades,
 * trains its creatures, puts idle ones to work, slaps them, sees to its
 * prisoners, places traps and doors, uses Specials and looks round with
 * Sight of Evil.<br>
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
    private static final int NOTHING_TO_DO_LOG_SECONDS = 60;

    /**
     * How long a neutral room or a gold seam it could not dig to is left
     * alone. DK II has no such wait, it digs to the same room again on its
     * next Portal plan and to the same seam when it next needs gold
     */
    private static final int FAILED_TARGET_WAIT_SECONDS = 120;

    /**
     * A room it found no site for at its default size is looked for smaller,
     * down to 3 by 3 tiles, and then also with fewer tiles left between the
     * rooms, down to none. DK II does neither
     */
    private static final int SMALLEST_ROOM_SIDE = 3;

    /**
     * DK II looks at its army and the training on half of the ticks
     */
    private static final int TRAINING_CHANCE_PERCENTAGE = 50;

    /**
     * DK II looks to place traps and doors on a tenth of the ticks
     */
    private static final int TRAPS_CHANCE_PERCENTAGE = 10;
    private static final int CREATE_IMP_DISTANCE_FROM_HEART = 2;
    private static final short SPELL_SIGHT_OF_EVIL = 1;
    private static final short SPELL_CREATE_GOLD = 13;

    /**
     * The specials DK II's computer player uses: all but Kill Creatures,
     * Transfer Creature and the one not used
     */
    private static final Set<Short> SPECIALS_USED = Set.of((short) 117, (short) 118, (short) 119, (short) 120, (short) 121,
            (short) 122, (short) 123, (short) 124, (short) 125, (short) 126, (short) 128, (short) 129);

    /**
     * DK II looks at a random tile each tick and casts Sight of Evil on it
     * with 2 chances in a hundred, or on enemy land with 20
     */
    private static final int SIGHT_OF_EVIL_RANDOM_PERCENTAGE = 2;
    private static final int SIGHT_OF_EVIL_ENEMY_LAND_PERCENTAGE = 20;

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
     * DK II plans this in place of a room it is still researching: a site is
     * dug out for it and nothing is built there
     */
    private static final short ROOM_NOT_RESEARCHED = 229;

    /**
     * The rooms DK II's computer player never builds
     */
    private static final Set<Short> ROOMS_NOT_BUILT = Set.of(ROOM_PORTAL, ROOM_DUNGEON_HEART, ROOM_WOODEN_BRIDGE,
            ROOM_GUARD_ROOM, ROOM_STONE_BRIDGE, ROOM_HERO_STONE_BRIDGE);

    /**
     * The rooms whose lack of space DK II tells its computer player about
     */
    private static final short[] ROOMS_TO_GROW = {ROOM_TREASURY, ROOM_LAIR, ROOM_PRISON, ROOM_GRAVEYARD};

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
     * DK II weighs up each way round rock for up to 24 tiles to dig
     */
    private static final int WALL_TILES_TO_DIG = 24;

    /**
     * The steps a path takes heading north, east, south and west
     */
    private static final int[][] HEADINGS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    /**
     * The directions of a room site search, for the decision log
     */
    private static final String[] DIRECTIONS = {"north", "west", "south", "east"};

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final Map<Short, IPlayerController> playerControllers;
    private final IGameWorldController gameWorldController;
    private final IMapController mapController;
    private final INavigationService navigationService;
    private final IGameTimer gameTimer;
    private final boolean logDecisions;

    /**
     * The settings of each computer player: its type's in a game set up in
     * the lobby, otherwise the ones the level gives it
     */
    private final Map<Short, AI> ais = new HashMap<>();
    private final KeeperAiBattles battles;
    private final KeeperAiInvasions invasions;
    private final KeeperAiCreatures creatures;
    private final KeeperAiTraps traps;
    private final EntitySet keeperAis;
    private final EntitySet objects;

    /**
     * @param entityData the entities, the computer players keep their state
     * there
     * @param kwdFile the level
     * @param playerControllers all the players by id
     * @param gameWorldController the player actions
     * @param navigationService the ways creatures can walk
     * @param gameTimer the game time, for the decision log
     * @param logDecisions whether to log what the computer players decide
     */
    public KeeperAiSystem(EntityData entityData, IKwdFile kwdFile, Map<Short, IPlayerController> playerControllers,
            IGameWorldController gameWorldController, INavigationService navigationService, IGameTimer gameTimer, boolean logDecisions) {
        this.navigationService = navigationService;
        this.gameTimer = gameTimer;
        this.logDecisions = logDecisions;
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.playerControllers = playerControllers;
        this.gameWorldController = gameWorldController;
        this.mapController = gameWorldController.getMapController();
        battles = new KeeperAiBattles(entityData, kwdFile, gameWorldController, gameTimer, this::log);
        invasions = new KeeperAiInvasions(entityData, kwdFile, gameWorldController, navigationService, battles, this::log);
        creatures = new KeeperAiCreatures(entityData, kwdFile, gameWorldController, battles, invasions, this::log);
        traps = new KeeperAiTraps(entityData, kwdFile, gameWorldController, this::log);

        // The keepers the level or the game setup gives to the computer, a loaded game has them already
        keeperAis = entityData.getEntities(KeeperAi.class);
        objects = entityData.getEntities(ObjectComponent.class, Position.class);
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
                KeeperAi keeperAi = new KeeperAi(keeper.getId());
                keeperAi.portals = countPortals(playerController);
                entityData.setComponent(entityData.createEntity(), keeperAi);

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
        traps.applyChanges();
        objects.applyChanges();
        for (Entity entity : keeperAis) {
            KeeperAi keeperAi = new KeeperAi(entity.get(KeeperAi.class));
            IPlayerController playerController = playerControllers.get(keeperAi.playerId);

            // A keeper without its Dungeon Heart is out of the game, DK II stops its computer player then
            IRoomController heart = playerController != null ? playerController.getRoomControl().getDungeonHeart() : null;
            if (heart == null || !ais.containsKey(keeperAi.playerId) || playerController.getKeeper().isDestroyed()
                    || heart.getRoomInstance().isDestroyed()) {
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

        // A task that takes too long is dropped, and a path that does leaves its target alone for a while
        keeperAi.taskTicks++;
        if (keeperAi.taskTicks > TASK_TIME_OUT_SECONDS * ticksPerSecond) {
            if (keeperAi.task == KeeperAi.Task.DIG_PATH) {
                leaveTargetAlone(keeperAi, "the path took too long");
            }
            setTask(keeperAi, KeeperAi.Task.PLAN, "the task took too long");
            keeperAi.wallFollowing = false;
            keeperAi.nextTask = KeeperAi.Task.PLAN;
        }

        // The tagged gold is let go a while after the last dig order
        if (keeperAi.untagTicks > 0) {
            keeperAi.untagTicks--;
            if (keeperAi.untagTicks == 0) {
                finishGoldAttempt(keeperAi);
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

        // A neutral room it could not dig to is left alone for a while
        for (int i = 0; i < keeperAi.failedTargetTicks.length; i++) {
            if (keeperAi.failedTargetTicks[i] > 0) {
                keeperAi.failedTargetTicks[i]--;
            }
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

        // Its creatures are called to arms against an enemy it invades
        if (actions > 0 && invasions.callToArms(keeperAi, playerControllers, ai)) {
            actions--;
        }

        // DK II reads the level a level's computer player trains its creatures to before the level's rules, as 0
        int creatureLevel = playerController.getKeeper().isAi() ? ai.getMinLevelForCreatures() : 0;
        if (actions > 0 && battles.retreatInjured(keeperAi, playerController, ai, creatureLevel)) {
            actions--;
        }

        // A Special lying on its land is used, that takes no action
        useSpecial(keeperAi, playerController);

        // On half of the ticks, while its army is not ready, it drops creatures where they train
        if (Utils.getRandom().nextInt(100) < TRAINING_CHANCE_PERCENTAGE && actions > 0
                && !invasions.isArmyReady(keeperAi.playerId, ai, creatureLevel)) {
            actions = creatures.dropIntoTraining(keeperAi, playerController, creatureLevel, actions);
        }

        // On a tenth of the ticks it sees to traps and doors
        if (actions > 0 && Utils.getRandom().nextInt(100) < TRAPS_CHANCE_PERCENTAGE) {
            actions = traps.placeTrapsAndDoors(keeperAi, playerController, ai, actions);
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

        // More or less often by its settings it sees to its research and its hand
        AI.MoveToResearchPolicy handPolicy = ai.getLikelyhoodToMovingCreaturesToLibraryForResearching();
        if (actions > 0 && (handPolicy == null || handPolicy.ordinal() <= Utils.getRandom().nextInt(AI.MoveToResearchPolicy.values().length))) {
            if (invasions.hasSpellsToResearch(playerController)) {
                creatures.makeRoomForResearcher(keeperAi, playerController);
            }

            // What is in its hand is put down, and now and then idle creatures are picked up to be put to work
            if (creatures.emptyHand(keeperAi, playerController)) {
                actions--;
            }
            actions = creatures.pickUpIdleCreatures(keeperAi, playerController, ai, creatureLevel, actions);
        }

        // Idle creatures get a slap now and then
        if (actions > 0) {
            actions = creatures.slapIdleCreatures(keeperAi, actions);
        }

        // A prisoner meets its fate
        if (actions > 0) {
            actions = creatures.seeToPrisoners(keeperAi, playerController, ai, actions);
        }

        // It has a look round with Sight of Evil
        if (actions > 0 && castSightOfEvil(keeperAi, playerController, ai)) {
            actions--;
        }

        while (actions > 0) {
            actions = switch (keeperAi.task) {
                case PLAN -> {

                    // An invasion first, else a room
                    boolean allRoomsPlaced = getNextRoom(playerController, ai, true) == null;
                    String nothingPlanned = null;
                    if (!invasions.planInvasion(keeperAi, playerControllers, ai, creatureLevel, allRoomsPlaced, ticksPerSecond)) {
                        nothingPlanned = planRoom(keeperAi, playerController, ai);
                    }
                    if (keeperAi.task == KeeperAi.Task.PLAN) {
                        mineGoldIfLow(keeperAi, playerController, ai);
                    }

                    // Why it does nothing, when it starts to and then once a minute
                    if (keeperAi.task == KeeperAi.Task.PLAN && nothingPlanned != null
                            && keeperAi.taskTicks % (NOTHING_TO_DO_LOG_SECONDS * ticksPerSecond) == 1) {
                        log(keeperAi, "nothing to do", null, (allRoomsPlaced ? "no room left to build, " : "") + nothingPlanned
                                + ", " + playerController.getKeeper().getGold() + " gold, "
                                + (keeperAi.callToArms ? "its Call to Arms is up"
                                        : keeperAi.cannotAttackTicks > 0 ? keeperAi.cannotAttackTicks / ticksPerSecond + " s before it may attack again"
                                        : "not ready to invade"));
                    }
                    yield actions - 1;
                }
                case FIND_ROOM_SITE ->
                    findRoomSite(keeperAi, playerController, ai, actions);
                case TAG_ROOM ->
                    tagRoom(keeperAi, ticksPerSecond, actions);
                case BUILD_ROOM ->
                    buildRoom(keeperAi, playerController, ticksPerSecond, actions);
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
        if (!playerController.getSpellControl().isAvailable(createImp)) {
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

    /**
     * Casts Sight of Evil on a random tile of the map, now and then, or more
     * often on enemy land, by its settings
     *
     * @return true if the spell was cast
     */
    private boolean castSightOfEvil(KeeperAi keeperAi, IPlayerController playerController, AI ai) {
        AI.SightOfEvilUsagePolicy policy = ai.getUseSightOfEvil();
        if (policy == null || policy == AI.SightOfEvilUsagePolicy.NONE) {
            return false;
        }

        Keeper keeper = playerController.getKeeper();
        Point tile = new Point(Utils.getRandom().nextInt(mapController.getMapData().getWidth()),
                Utils.getRandom().nextInt(mapController.getMapData().getHeight()));
        short ownerId = mapController.getMapData().getTile(tile).getOwnerId();
        if (policy == AI.SightOfEvilUsagePolicy.RANDOM ? Utils.getRandom().nextInt(100) >= SIGHT_OF_EVIL_RANDOM_PERCENTAGE
                : Utils.getRandom().nextInt(100) >= SIGHT_OF_EVIL_ENEMY_LAND_PERCENTAGE || ownerId < Player.GOOD_PLAYER_ID || !keeper.isEnemy(ownerId)) {
            return false;
        }
        KeeperSpell sightOfEvil = kwdFile.getKeeperSpellById(SPELL_SIGHT_OF_EVIL);
        ResearchableEntity availability = sightOfEvil != null ? playerController.getSpellControl().getTypes().get(sightOfEvil) : null;
        if (availability == null || !availability.isDiscovered()) {
            return false;
        }

        // The cast has its own rules, the mana tells if it went through
        int mana = keeper.getMana();
        gameWorldController.castKeeperSpell(SPELL_SIGHT_OF_EVIL, null, tile, WorldUtils.pointToVector2f(tile), keeper.getId());
        if (keeper.getMana() < mana) {
            log(keeperAi, "cast Sight of Evil", tile, policy == AI.SightOfEvilUsagePolicy.RANDOM ? "a random look" : "enemy land");
            return true;
        }

        return false;
    }

    /**
     * Uses the last Special it finds lying on its own land, as a human
     * player does with the hand, unless it is one DK II's computer player
     * leaves alone
     */
    private void useSpecial(KeeperAi keeperAi, IPlayerController playerController) {
        Entity special = null;
        for (Entity object : objects) {
            Point tile = WorldUtils.vectorToPoint(object.get(Position.class).position);
            IMapTileInformation mapTile = mapController.getMapData().getTile(tile);
            if (mapTile != null && mapTile.getOwnerId() == keeperAi.playerId && entityData.getComponent(object.getId(), InHand.class) == null
                    && kwdFile.getObject(object.get(ObjectComponent.class).objectId).getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_SPECIAL)
                    && (special == null || object.getId().getId() > special.getId().getId())) {
                special = object;
            }
        }
        if (special != null && SPECIALS_USED.contains(special.get(ObjectComponent.class).objectId)) {
            log(keeperAi, "use Special " + special.get(ObjectComponent.class).objectId, WorldUtils.vectorToPoint(special.get(Position.class).position),
                    "it lies on its land");
            gameWorldController.pickUp(special.getId(), keeperAi.playerId);
        }
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
        if (playerController.getKeeper().getGold() < ai.getMineGoldUntilGoldHeldIsGreaterThan()
                && !mineGold(keeperAi, playerController) && getTaggedGold(keeperAi.playerId).isEmpty()) {

            // No seam it knows of, it makes its own gold by the Dungeon Heart when it has the spell
            Keeper keeper = playerController.getKeeper();
            KeeperSpell createGold = kwdFile.getKeeperSpellById(SPELL_CREATE_GOLD);
            ResearchableEntity availability = createGold != null ? playerController.getSpellControl().getTypes().get(createGold) : null;
            if (availability != null && availability.isDiscovered()) {
                Point tile = new Point(keeper.getDungeonHeartLocation().x + 1, keeper.getDungeonHeartLocation().y + 1);
                int mana = keeper.getMana();
                gameWorldController.castKeeperSpell(SPELL_CREATE_GOLD, null, tile, WorldUtils.pointToVector2f(tile), keeper.getId());
                if (keeper.getMana() < mana) {
                    log(keeperAi, "cast Create Gold", tile, "low on gold with " + keeper.getGold() + " and no seam to mine");
                }
            }
        }
    }

    /**
     * Heads for the nearest gold seam it found the last time it looked,
     * unless there is gold tagged already, and looks for the seams again
     * for the next time. So what it heads for is one look old, as in DK II.
     * A seam it could not dig a path to a short while ago is passed over
     *
     * @return true if it heads for gold
     */
    private boolean mineGold(KeeperAi keeperAi, IPlayerController playerController) {
        if (!getTaggedGold(keeperAi.playerId).isEmpty()) {
            return false;
        }
        Point gold = null;
        for (Point deposit : keeperAi.goldDeposits) {
            if (deposit != null && !isWaitingAfterFailedTarget(keeperAi, deposit)) {
                gold = deposit;
                break;
            }
        }
        keeperAi.goldDeposits = findGold(keeperAi, playerController);
        if (gold != null) {
            if (!gold.equals(keeperAi.goldAttemptTarget)) {
                keeperAi.goldAttemptTarget = gold;
                keeperAi.goldAttemptWorkAtStart = getGoldAttemptWorkRemaining(gold);
                keeperAi.goldAttemptLooked = false;
            }
            keeperAi.target = gold;
            setTask(keeperAi, KeeperAi.Task.GO_TO_GOLD, "low on gold with " + playerController.getKeeper().getGold()
                    + ", the nearest seam of its last look, the next look has "
                    + (keeperAi.goldDeposits[0] != null ? keeperAi.goldDeposits[0].x + "," + keeperAi.goldDeposits[0].y : "none"));
            keeperAi.goldTiles = 0;
        }

        return gold != null;
    }

    /**
     * Groups the gold of the whole map into seams and takes a gold tile near
     * the center of each of the four seams nearest to one of the player's rooms.
     * The path planner must target the seam itself so it can dig up to it; a
     * nearby non-solid tile does not prove that an imp can reach the gold.
     *
     * @return the middles, the nearest first, {@code null} where there are
     * fewer seams
     */
    private Point[] findGold(KeeperAi keeperAi, IPlayerController playerController) {
        int width = mapController.getMapData().getWidth();
        int height = mapController.getMapData().getHeight();
        boolean[][] seen = new boolean[width][height];
        Point[] nearest = new Point[4];
        int[] distances = new int[nearest.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (seen[x][y] || !hasGold(x, y)) {
                    continue;
                }

                // The whole seam
                int sumX = 0;
                int sumY = 0;
                List<Point> seam = new ArrayList<>();
                seam.add(new Point(x, y));
                seen[x][y] = true;
                for (int i = 0; i < seam.size(); i++) {
                    Point p = seam.get(i);
                    sumX += p.x;
                    sumY += p.y;
                    for (Point neighbour : new Point[]{new Point(p.x - 1, p.y), new Point(p.x + 1, p.y), new Point(p.x, p.y - 1), new Point(p.x, p.y + 1)}) {
                        if (hasGold(neighbour.x, neighbour.y) && !seen[neighbour.x][neighbour.y]) {
                            seen[neighbour.x][neighbour.y] = true;
                            seam.add(neighbour);
                        }
                    }
                }

                Point centre = new Point(sumX / seam.size(), sumY / seam.size());
                Point target = null;
                int nearestDistance = Integer.MAX_VALUE;
                for (Point p : seam) {
                    int distance = getDistance(centre, p);
                    if (distance < nearestDistance) {
                        target = p;
                        nearestDistance = distance;
                    }
                }
                if (target == null || isWaitingAfterFailedTarget(keeperAi, target)) {
                    continue;
                }
                int distance = getDistance(getNearestRoom(playerController, target).getRoomInstance().getCenter(), target);
                for (int i = 0; i < nearest.length; i++) {
                    if (nearest[i] == null || distance < distances[i]) {
                        System.arraycopy(nearest, i, nearest, i + 1, nearest.length - 1 - i);
                        System.arraycopy(distances, i, distances, i + 1, nearest.length - 1 - i);
                        nearest[i] = target;
                        distances[i] = distance;
                        break;
                    }
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
     * The tile itself, or else the first tile on the spiral around it that
     * is not impenetrable, as DK II looks. A seam's own gold is on the
     * spiral, so the search ends there at the latest
     */
    private Point getNearestReachableTile(Point p) {
        int side = 2 * Math.max(mapController.getMapData().getWidth(), mapController.getMapData().getHeight()) + 1;
        for (int i = 0; i < side * side; i++) {
            Point spiral = getSpiralTile(p, i);
            IMapTileInformation tile = mapController.getMapData().getTile(spiral);
            if (tile != null && !mapController.getTerrain(tile).getFlags().contains(Terrain.TerrainFlag.IMPENETRABLE)) {
                return spiral;
            }
        }

        return null;
    }

    /**
     * A tile on DK II's spiral around a centre: the centre, one tile east,
     * then round and round with every second side one tile longer
     *
     * @param index how many steps along the spiral
     */
    private static Point getSpiralTile(Point centre, int index) {
        int x = centre.x;
        int y = centre.y;
        int dx = 0;
        int dy = 1;
        int sideX = 1;
        int sideY = 1;
        boolean vertical = true;
        if (index > 0) {
            x++;
        }
        for (int i = 1; i < index; i++) {
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

        return new Point(x, y);
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
            Point tile = getSpiralTile(keeperAi.target, keeperAi.goldTiles);
            if (hasGold(tile.x, tile.y) && mapController.isTaggable(tile)) {
                tag(keeperAi, tile, tile, ticksPerSecond, "gold");
            }
            keeperAi.goldTiles++;
            if (keeperAi.goldTiles == GOLD_TILES_TO_TAG) {
                keeperAi.goldAttemptLooked = keeperAi.target.equals(keeperAi.goldAttemptTarget);
                keeperAi.untagTicks = UNTAG_GOLD_SECONDS * ticksPerSecond;
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
     *
     * @return why it plans nothing, or null when it plans something
     */
    private String planRoom(KeeperAi keeperAi, IPlayerController playerController, AI ai) {
        keeperAi.roomId = 0;
        wantMoreSpace(keeperAi, playerController, ai);
        if (!keeperAi.roomWanted) {
            Room wanted = getNextRoom(playerController, ai, true);
            keeperAi.wantedRoomId = wanted != null ? wanted.getRoomId()
                    : getNextRoom(playerController, ai, false) != null ? ROOM_NOT_RESEARCHED : ROOM_PORTAL;
            keeperAi.roomWanted = true;
        }
        short roomId = keeperAi.wantedRoomId;
        boolean expand = keeperAi.wantedRoomExpand;
        keeperAi.wantedRoomExpand = false;
        if (roomId == ROOM_PORTAL) {
            keeperAi.roomWanted = false;
        } else {
            keeperAi.wantedRoomId = ROOM_PORTAL;
        }
        if (!expand && digToNeutralRoom(keeperAi, playerController, ai, roomId)) {
            return null;
        }

        Room room = roomId == ROOM_PORTAL ? null : kwdFile.getRoomById(roomId);
        if (roomId != ROOM_NOT_RESEARCHED && room == null || ai.getDefaultSize() < 1) {
            return roomId == ROOM_PORTAL ? "no neutral Portal to dig to" : "can't build room " + roomId;
        }

        // The room at its default size has to be affordable, otherwise mine gold first
        if (room != null && room.getCost() * ai.getDefaultSize() > playerController.getKeeper().getGold()) {
            mineGold(keeperAi, playerController);
            return "room " + roomId + " costs more than its gold and there is no gold to mine";
        }

        keeperAi.roomId = roomId;

        // Its first room of the kind grows where it can, without a path to dig. If it can't, another one is built
        if (expand) {
            for (IRoomController own : playerController.getRoomControl().getRoomControllers().getOrDefault(room, Set.of())) {
                KeeperAiRoomSiteFinder.RoomSite site = new KeeperAiRoomSiteFinder(mapController, keeperAi.playerId, ai).expand(own.getRoomInstance());
                if (site != null) {
                    keeperAi.corner = site.corner();
                    keeperAi.oppositeCorner = site.oppositeCorner();
                    setTask(keeperAi, KeeperAi.Task.TAG_ROOM, site.corner(), "room " + roomId + " needs more space, it grows to "
                            + site.corner().x + "," + site.corner().y + " to " + site.oppositeCorner().x + "," + site.oppositeCorner().y);
                    return null;
                }
                break;
            }
        }
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

        return null;
    }

    /**
     * Lines a room up to be planned next when it needs more space: the Lair
     * when a Portal was claimed, and a Treasury, Lair, Prison or Graveyard
     * that is full. By its settings the room is grown or another one is
     * built. DK II's computer player is told by the messages a player gets;
     * here it looks at the rooms itself
     */
    private void wantMoreSpace(KeeperAi keeperAi, IPlayerController playerController, AI ai) {
        AI.RoomExpandPolicy policy = ai.getWhenMoreSpaceInRoomRequired();
        if (policy == null || policy == AI.RoomExpandPolicy.IGNORE || keeperAi.roomWanted && keeperAi.wantedRoomId != ROOM_PORTAL) {
            return;
        }

        int portals = countPortals(playerController);
        short roomId = portals > keeperAi.portals && ai.isBuildBiggerLairAfterClaimingPortal() ? ROOM_LAIR : 0;
        keeperAi.portals = portals;
        for (int i = 0; i < ROOMS_TO_GROW.length && roomId == 0; i++) {

            // Gems never run out, so a full Treasury is left as it is while gems are mined
            if (ROOMS_TO_GROW[i] == ROOM_TREASURY && getTaggedGold(keeperAi.playerId).stream().anyMatch(tile
                    -> mapController.getTerrain(mapController.getMapData().getTile(tile)).getFlags().contains(Terrain.TerrainFlag.IMPENETRABLE))) {
                continue;
            }
            for (IRoomController room : playerController.getRoomControl().getRoomControllers().getOrDefault(kwdFile.getRoomById(ROOMS_TO_GROW[i]), Set.of())) {
                if (!room.getRoomInstance().isDestroyed() && room.isFullCapacity()) {
                    roomId = ROOMS_TO_GROW[i];
                }
            }
        }
        Room room = kwdFile.getRoomById(roomId);
        ResearchableEntity availability = room != null ? playerController.getRoomControl().getTypes().get(room) : null;
        if (availability != null && availability.isDiscovered()) {
            keeperAi.wantedRoomId = roomId;
            keeperAi.wantedRoomExpand = policy == AI.RoomExpandPolicy.EXPAND_EXISTING;
            keeperAi.roomWanted = true;
        }
    }

    private int countPortals(IPlayerController playerController) {
        int portals = 0;
        for (short roomId : new short[]{ROOM_PORTAL, ROOM_HERO_PORTAL}) {
            Room room = kwdFile.getRoomById(roomId);
            if (room != null) {
                portals += playerController.getRoomControl().getTypeCount(room);
            }
        }

        return portals;
    }

    /**
     * Has a path dug to the nearest neutral room of a kind, so that the
     * imps claim it. A Portal has to be near one of the computer player's
     * rooms, any other room near its Dungeon Heart, or else some of its
     * land has to lie on the edge of a square around the room. A room it
     * could not dig to a short while ago is passed over
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
                    && (portal ? id == ROOM_PORTAL || id == ROOM_HERO_PORTAL : id == roomId) && !isWaitingAfterFailedTarget(keeperAi, centre)
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

    private static boolean isWaitingAfterFailedTarget(KeeperAi keeperAi, Point target) {
        for (int i = 0; i < keeperAi.failedTargets.length; i++) {
            if (keeperAi.failedTargetTicks[i] > 0 && target.equals(keeperAi.failedTargets[i])) {
                return true;
            }
        }

        return false;
    }

    private int getGoldAttemptWorkRemaining(Point centre) {
        int work = 0;
        for (int i = 0; i < GOLD_TILES_TO_TAG; i++) {
            Point point = getGoldLookTile(centre, i);
            IMapTileInformation tile = mapController.getMapData().getTile(point);
            if (tile != null && mapController.getTerrain(tile).getGoldValue() > 0) {
                work += tile.getHealth();
            }
        }
        return work;
    }

    /** The center tile followed by the same spiral used when tagging gold. */
    private static Point getGoldLookTile(Point centre, int index) {
        int x = centre.x;
        int y = centre.y;
        int dx = 0;
        int dy = 1;
        int sideX = 1;
        int sideY = 1;
        boolean vertical = true;
        if (index > 0) {
            x++;
        }
        for (int i = 1; i < index; i++) {
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
        return new Point(x, y);
    }

    private void finishGoldAttempt(KeeperAi keeperAi) {
        Point target = keeperAi.goldAttemptTarget;
        if (target == null || !keeperAi.goldAttemptLooked) {
            return;
        }
        if (getGoldAttemptWorkRemaining(target) >= keeperAi.goldAttemptWorkAtStart) {
            rememberFailedTarget(keeperAi, target, "the gold seam made no mining progress");
        }
        keeperAi.goldAttemptTarget = null;
        keeperAi.goldAttemptWorkAtStart = 0;
        keeperAi.goldAttemptLooked = false;
    }

    private void rememberFailedTarget(KeeperAi keeperAi, Point target, String reason) {
        int slot = 0;
        for (int i = 0; i < keeperAi.failedTargetTicks.length; i++) {
            if (target.equals(keeperAi.failedTargets[i])) {
                slot = i;
                break;
            }
            if (keeperAi.failedTargetTicks[i] < keeperAi.failedTargetTicks[slot]) {
                slot = i;
            }
        }
        keeperAi.failedTargets[slot] = target;
        keeperAi.failedTargetTicks[slot] = FAILED_TARGET_WAIT_SECONDS * kwdFile.getGameLevel().getTicksPerSec();
        log(keeperAi, "leave the target alone for " + FAILED_TARGET_WAIT_SECONDS + " s", target, reason);
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
     * rest of the rooms, that it doesn't have yet and may build, or is
     * still researching. For the rooms it is still researching DK II starts
     * at the second room of the build order
     */
    private Room getNextRoom(IPlayerController playerController, AI ai, boolean researched) {
        List<Short> roomIds = new ArrayList<>(ai.getBuildOrder());
        for (short roomId = (short) (roomIds.size() + 1); roomId <= LAST_ROOM; roomId++) {
            roomIds.add(roomId);
        }
        if (!researched) {
            roomIds.removeFirst();
        }
        for (short roomId : roomIds) {
            Room room = kwdFile.getRoomById(roomId);
            if (room == null || ROOMS_NOT_BUILT.contains(roomId)) {
                continue;
            }
            ResearchableEntity availability = playerController.getRoomControl().getTypes().get(room);
            if (availability != null && availability.isDiscovered() == researched && playerController.getRoomControl().getTypeCount(room) == 0) {
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
     * room. Each time it gives up, the next search is for a smaller room,
     * and once the room is as small as it gets, a site with a tile less
     * between the rooms is taken too. DK II does not do that: it looks for the
     * default size and gap for ever
     */
    private int findRoomSite(KeeperAi keeperAi, IPlayerController playerController, AI ai, int actions) {
        int size = ai.getDefaultSize();
        int gap = ai.getTilesLeftBetweenRooms();
        for (int i = 0; i < keeperAi.roomSitesNotFound; i++) {
            if (size > SMALLEST_ROOM_SIDE * SMALLEST_ROOM_SIDE) {
                int side = Math.max(SMALLEST_ROOM_SIDE, (int) Math.sqrt(size) - 1);
                size = side * side;
            } else if (gap > 0) {
                gap--;
            }
        }
        KeeperAiRoomSiteFinder finder = new KeeperAiRoomSiteFinder(mapController, keeperAi.playerId, ai);
        KeeperAiRoomSiteFinder.RoomSite site = null;
        for (int i = ai.getTilesLeftBetweenRooms(); site == null && i >= gap; i--) {
            site = finder.find(keeperAi.current, keeperAi.direction, size, i);
        }
        if (site != null) {
            keeperAi.roomSitesNotFound = 0;

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
            keeperAi.roomSitesNotFound++;
            setTask(keeperAi, KeeperAi.Task.PLAN, "no site for room " + keeperAi.roomId + " of " + size + " tiles, leaving " + gap + " tiles to the other rooms");
        }

        return 0;
    }

    /**
     * Digs the path tile by tile, each tile is tagged only when the one
     * before it has been dug out. Water and lava get a bridge
     */
    private int digPath(KeeperAi keeperAi, IPlayerController playerController, AI ai, int ticksPerSecond, int actions) {

        // No paths out of the build area to a room site or to gold
        if (keeperAi.nextTask != KeeperAi.Task.PLAN && keeperAi.nextTask != KeeperAi.Task.FINISH
                && (keeperAi.target.x < ai.getBuildAreaStartX() || keeperAi.target.x > ai.getBuildAreaEndX()
                || keeperAi.target.y < ai.getBuildAreaStartY() || keeperAi.target.y > ai.getBuildAreaEndY())) {
            setTask(keeperAi, KeeperAi.Task.FINISH, "the path leads out of the build area");
            keeperAi.nextTask = KeeperAi.Task.FINISH;
            return 0;
        }

        while (actions > 0) {
            actions--;
            if (keeperAi.current.equals(keeperAi.target)) {
                finishPath(keeperAi, true, "the path is dug");
                return actions;
            }

            // A tunnel to an enemy stops at the enemy's land, to break through later
            if (keeperAi.invasionTargetId != 0 && invasions.stopTunnel(keeperAi, playerController.getKeeper())) {
                finishPath(keeperAi, true, "the tunnel is dug up to the enemy's land");
                return actions;
            }

            IMapTileInformation tile = mapController.getMapData().getTile(keeperAi.current);
            Terrain terrain = mapController.getTerrain(tile);
            if (terrain.getFlags().contains(Terrain.TerrainFlag.WATER) || terrain.getFlags().contains(Terrain.TerrainFlag.LAVA)) {
                Room bridge = getBridge(playerController, terrain);
                if (bridge == null) {
                    finishPath(keeperAi, false, "no bridge to cross with");
                    return actions;
                }

                // A bridge it can't build yet, short of gold say, keeps it on this tile (DK II doesn't try the other bridge)
                if (mapController.isBuildable(keeperAi.current, keeperAi.playerId, bridge.getRoomId())) {
                    Vector2f position = new Vector2f(keeperAi.current.x, keeperAi.current.y);
                    gameWorldController.build(position, position, keeperAi.playerId, bridge.getRoomId());
                    log(keeperAi, "build room " + bridge.getRoomId(), keeperAi.current, "the path crosses water or lava");
                    if (mapController.getRoomControllerByCoordinates(keeperAi.current) != null && !stepOnPath(keeperAi)) {
                        finishPath(keeperAi, !keeperAi.wallFollowing, "no way on from here");
                        return actions;
                    }
                }
            } else if (!terrain.getFlags().contains(Terrain.TerrainFlag.TAGGABLE)
                    || !TileWork.canDamageTile(terrain, tile.getOwnerId(), keeperAi.playerId)) {

                // On open land the path is done as soon as a creature can walk the rest
                if (navigationService.findPath(keeperAi.target, keeperAi.current, getWalker(keeperAi.playerId)) != null) {
                    keeperAi.current = keeperAi.target;
                    finishPath(keeperAi, true, "there is a way to walk from here");
                    return actions;
                }
                if (!stepOnPath(keeperAi)) {
                    finishPath(keeperAi, !keeperAi.wallFollowing, "the way on is open, or there is none");
                    return actions;
                }
            } else if (!tile.isSelected(keeperAi.playerId)) {
                tag(keeperAi, keeperAi.current, keeperAi.current, ticksPerSecond, "next tile of the path");
            }
        }

        return 0;
    }

    /**
     * Ends the path and goes on with the next task. A path that could not go
     * on, with no bridge to cross with or no way round rock, has its target
     * left alone for a while. Given up or not, DK II (0x4FDC50) goes on to
     * the task the path was for, so the room site or the gold is still
     * tagged: the imps may get there another way
     *
     * @param reached whether the path got through, a rock follow that gave
     * up is still on when the stepper finds no way
     */
    private void finishPath(KeeperAi keeperAi, boolean reached, String reason) {
        if (!reached) {
            leaveTargetAlone(keeperAi, reason);
        }
        keeperAi.invasionTargetId = 0;
        keeperAi.wallFollowing = false;
        setTask(keeperAi, keeperAi.nextTask, reason);
        keeperAi.nextTask = KeeperAi.Task.FINISH;
    }

    /**
     * Keeps the path's target, a neutral room or a gold seam, in the slot that
     * frees up first, so that it is passed over for a while
     */
    private void leaveTargetAlone(KeeperAi keeperAi, String reason) {
        rememberFailedTarget(keeperAi, keeperAi.target, reason);
    }

    /**
     * Moves the path on to the next tile that takes work, as DK II does:
     * straight at the target along the longer way first, over open land, up
     * to rock to dig or water or lava to bridge. Rock that can't be dug is
     * followed along its edge to the tile where the straight way goes on
     * behind it, on the side that takes less digging
     *
     * @return false if the way to the target is open, or if there is no way
     */
    private boolean stepOnPath(KeeperAi keeperAi) {
        Point pos = keeperAi.current;
        if (!keeperAi.wallFollowing) {
            int heading;
            Set<Terrain.TerrainFlag> flags;
            do {
                if (pos.equals(keeperAi.target)) {
                    return false;
                }
                heading = getHeading(pos, keeperAi.target);
                pos = move(pos, heading);
                flags = getFlags(pos);
            } while (!flags.contains(Terrain.TerrainFlag.SOLID) && !flags.contains(Terrain.TerrainFlag.WATER) && !flags.contains(Terrain.TerrainFlag.LAVA));
            if (!isRock(pos)) {
                keeperAi.current = pos;
                return true;
            }

            // Where the straight way comes out of the rock
            while (isRock(pos) && !pos.equals(keeperAi.target)) {
                pos = move(pos, getHeading(pos, keeperAi.target));
            }
            keeperAi.wallFollowing = true;
            keeperAi.wallStart = keeperAi.current;
            keeperAi.wallExit = pos;

            // The side that gets there with less to dig, or else gets nearer
            int[] left = traceWall(keeperAi, (heading + 1) & 3, true, WALL_TILES_TO_DIG);
            keeperAi.wallOnLeft = false;
            if (left[0] >= 0) {
                int[] right = traceWall(keeperAi, (heading - 1) & 3, false, left[0] == 0 ? left[1] : WALL_TILES_TO_DIG);
                keeperAi.wallOnLeft = right[0] < 0 || left[0] < right[0];
            }
            keeperAi.wallHeading = (heading + (keeperAi.wallOnLeft ? 1 : -1)) & 3;
            log(keeperAi, "follow the rock", keeperAi.wallExit, "rock that can't be dug in the way, round it "
                    + DIRECTIONS[(4 - keeperAi.wallHeading) & 3] + " to where the straight way goes on");
            pos = keeperAi.current;
        }

        int heading = keeperAi.wallHeading;
        for (int i = 0; !pos.equals(keeperAi.wallExit); i++) {
            heading = getWallHeading(pos, heading, keeperAi.wallOnLeft);
            if (heading < 0 || i > 4 * mapController.getMapData().getWidth() * mapController.getMapData().getHeight()) {
                return false;
            }
            pos = move(pos, heading);
            if (isDiggable(pos)) {
                keeperAi.current = pos;
                keeperAi.wallHeading = heading;
                return true;
            }
            if (pos.equals(keeperAi.wallStart)) {
                return false;
            }
        }
        keeperAi.current = pos;
        keeperAi.wallFollowing = false;

        return true;
    }

    /**
     * Follows the rock one way without digging, to see what that way takes
     *
     * @return 0 and the tiles to dig when it gets to where the straight way
     * goes on, the nearest it got to the target when it takes too many
     * tiles to dig, below 0 when it leads back to where it started
     */
    private int[] traceWall(KeeperAi keeperAi, int heading, boolean wallOnLeft, int tilesToDig) {
        Point pos = keeperAi.current;
        int dug = 0;
        int nearest = Integer.MAX_VALUE;
        for (int i = 0; !pos.equals(keeperAi.wallExit); i++) {
            nearest = Math.min(nearest, getDistance(pos, keeperAi.target));
            heading = getWallHeading(pos, heading, wallOnLeft);
            if (heading < 0 || i > 4 * mapController.getMapData().getWidth() * mapController.getMapData().getHeight()) {
                return new int[]{-1, dug};
            }
            pos = move(pos, heading);
            if (isDiggable(pos)) {
                dug++;
                if (dug > tilesToDig) {
                    return new int[]{Math.max(nearest, 1), dug};
                }
            } else if (pos.equals(keeperAi.wallStart)) {
                return new int[]{-1, dug};
            }
        }

        return new int[]{0, dug};
    }

    /**
     * The way on along the rock: towards the rock if it can, else straight
     * on, else away from it, else back
     *
     * @return the heading, or below 0 when it is walled in
     */
    private int getWallHeading(Point pos, int heading, boolean wallOnLeft) {
        for (int i = -1; i < 3; i++) {
            int next = (heading + (wallOnLeft ? i : -i)) & 3;
            Point tile = move(pos, next);
            if (isDiggable(tile) || !getFlags(tile).contains(Terrain.TerrainFlag.SOLID) && !getFlags(tile).contains(Terrain.TerrainFlag.LAVA)) {
                return next;
            }
        }

        return -1;
    }

    private static int getHeading(Point from, Point to) {
        if (Math.abs(to.y - from.y) < Math.abs(to.x - from.x)) {
            return to.x < from.x ? 3 : 1;
        }

        return to.y < from.y ? 0 : 2;
    }

    private static Point move(Point p, int heading) {
        return new Point(p.x + HEADINGS[heading][0], p.y + HEADINGS[heading][1]);
    }

    /**
     * What a tile is like, off the map it is rock that can't be dug
     */
    private Set<Terrain.TerrainFlag> getFlags(Point p) {
        IMapTileInformation tile = mapController.getMapData().getTile(p);

        return tile != null ? mapController.getTerrain(tile).getFlags() : EnumSet.of(Terrain.TerrainFlag.SOLID, Terrain.TerrainFlag.IMPENETRABLE);
    }

    private boolean isDiggable(Point p) {
        return getFlags(p).contains(Terrain.TerrainFlag.SOLID) && !getFlags(p).contains(Terrain.TerrainFlag.IMPENETRABLE);
    }

    private boolean isRock(Point p) {
        return getFlags(p).contains(Terrain.TerrainFlag.SOLID) && getFlags(p).contains(Terrain.TerrainFlag.IMPENETRABLE);
    }

    /**
     * A creature of the player that walks, to see where there is a way
     */
    private static INavigable getWalker(short playerId) {
        return new INavigable() {
            @Override
            public short getOwnerId() {
                return playerId;
            }

            @Override
            public boolean canFly() {
                return false;
            }

            @Override
            public boolean canWalkOnWater() {
                return false;
            }

            @Override
            public boolean canWalkOnLava() {
                return false;
            }

            @Override
            public boolean canMoveDiagonally() {
                return false;
            }
        };
    }

    /**
     * A wooden bridge over water if the player has it, otherwise a stone
     * bridge. Over lava only a stone bridge: DK II's computer player never
     * puts a wooden one there, although a player can and it burns away
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
        if (keeperAi.roomId == ROOM_NOT_RESEARCHED) {
            setTask(keeperAi, KeeperAi.Task.PLAN, "the site is tagged, the room for it is still being researched");
            return actions - 1;
        }
        setTask(keeperAi, KeeperAi.Task.BUILD_ROOM, "the site is tagged");
        keeperAi.current = keeperAi.corner;

        return actions - 1;
    }

    /**
     * Goes through the room site tile by tile. It waits for the imps to dig
     * out and claim the tile, then places the room on it
     */
    private int buildRoom(KeeperAi keeperAi, IPlayerController playerController, int ticksPerSecond, int actions) {
        while (actions > 0) {
            actions--;
            if (mapController.isBuildable(keeperAi.current, keeperAi.playerId, keeperAi.roomId)) {
                Vector2f position = new Vector2f(keeperAi.current.x, keeperAi.current.y);
                gameWorldController.build(position, position, keeperAi.playerId, keeperAi.roomId);
                if (!mapController.isBuildable(keeperAi.current, keeperAi.playerId, keeperAi.roomId)) {
                    log(keeperAi, "build room " + keeperAi.roomId, keeperAi.current, "the tile is dug out and claimed");
                    return actions;
                }

                // DK II's gold check is inside the build call (0x4FD2B0, 0x4C5B40): short of gold the tile isn't built and is tried again next tick
                if (playerController.getKeeper().getGold() < kwdFile.getRoomById(keeperAi.roomId).getCost()) {
                    return actions;
                }

                log(keeperAi, "skip room tile", keeperAi.current, "room placement was rejected");
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
        objects.release();
        battles.release();
        traps.release();
    }

}
