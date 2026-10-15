/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.game.trigger.player;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.room.ICreatureEntrance;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.game.trigger.TriggerActionData;
import toniarts.openkeeper.game.trigger.TriggerControl;
import toniarts.openkeeper.game.trigger.TriggerGenericData;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Trigger control that is targeted for specified player. The player scope: it
 * knows the conditions and actions that are about a player, forwards the rest
 * to the level scope.
 *
 * @author ArchDemon
 */
public class PlayerTriggerControl extends TriggerControl {

    private static final Logger logger = System.getLogger(PlayerTriggerControl.class.getName());

    private final short playerId;
    private final Map<TriggerAction.ActionType, Predicate<TriggerActionData>> actionHandlers = createActionHandlers();

    // The statistics seen on the previous poll, the difference is what has happened since
    private final Map<TriggerGeneric.TargetType, Map<Creature, Integer>> seenStats = new EnumMap<>(TriggerGeneric.TargetType.class);

    public PlayerTriggerControl(final IGameController gameController, final ILevelInfo levelInfo, final IGameTimer gameTimer, final IMapController mapController,
            final ICreaturesController creaturesController, final int triggerId, final short playerId,
            final PlayerService playerService) {
        super(gameController, levelInfo, gameTimer, mapController, creaturesController, playerService, triggerId);
        this.playerId = playerId;
    }

    private Map<TriggerAction.ActionType, Predicate<TriggerActionData>> createActionHandlers() {
        Map<TriggerAction.ActionType, Predicate<TriggerActionData>> handlers = new EnumMap<>(TriggerAction.ActionType.class);
        handlers.put(TriggerAction.ActionType.WIN_GAME, handled(this::winGame));
        handlers.put(TriggerAction.ActionType.LOSE_GAME, handled(this::loseGame));
        handlers.put(TriggerAction.ActionType.GENERATE_CREATURE, this::generateCreature);
        handlers.put(TriggerAction.ActionType.SET_PORTAL_STATUS, handled(this::setPortalStatus));
        handlers.put(TriggerAction.ActionType.FOLLOW_CAMERA_PATH, handled(this::followCameraPath));
        handlers.put(TriggerAction.ActionType.MAKE_OBJECTIVE, handled(this::makeObjective));
        handlers.put(TriggerAction.ActionType.ZOOM_TO_ACTION_POINT, handled(this::zoomToActionPoint));
        handlers.put(TriggerAction.ActionType.ROTATE_AROUND_ACTION_POINT, handled(this::rotateAroundActionPoint));
        handlers.put(TriggerAction.ActionType.PLAY_SPEECH, handled(this::playSpeech));
        handlers.put(TriggerAction.ActionType.DISPLAY_TEXT_STRING, handled(this::displayTextString));
        handlers.put(TriggerAction.ActionType.SET_WIDESCREEN_MODE, handled(this::setWidescreenMode));
        handlers.put(TriggerAction.ActionType.SET_MUSIC_LEVEL, handled(this::setMusicLevel));
        handlers.put(TriggerAction.ActionType.SET_SLAPS_LIMIT, handled(this::setSlapsLimit));
        return handlers;
    }

    private static Predicate<TriggerActionData> handled(Consumer<TriggerActionData> action) {
        return trigger -> {
            action.accept(trigger);
            return true;
        };
    }

    public short getPlayerId() {
        return playerId;
    }

    @Override
    protected void pollEvents() {
        super.pollEvents();

        // The player events about the creatures are noticed from the statistics. Fire for each
        // creature type that has got more than on the previous poll
        IPlayerController playerController = getPlayerController(playerId);
        if (isPlayerScope() && playerController != null) {
            pollStatEvent(TriggerGeneric.TargetType.PLAYER_CREATURE_PICKED_UP, playerController.getStatsControl().getPickUps());
            pollStatEvent(TriggerGeneric.TargetType.PLAYER_CREATURE_DROPPED, playerController.getStatsControl().getDrops());
            pollStatEvent(TriggerGeneric.TargetType.PLAYER_CREATURE_SLAPPED, playerController.getStatsControl().getSlaps());
        }
    }

    /**
     * Is this the tree of the player itself. The player events belong to the
     * player's own tree, the trees of the player's things (creatures...) only
     * borrow the player's conditions and actions.
     *
     * @return true if the tree is the player's own
     */
    protected boolean isPlayerScope() {
        return true;
    }

    private void pollStatEvent(TriggerGeneric.TargetType event, Map<Creature, Integer> current) {
        Map<Creature, Integer> previous = seenStats.computeIfAbsent(event, key -> new HashMap<>());
        for (Map.Entry<Creature, Integer> entry : Map.copyOf(current).entrySet()) {
            if (entry.getValue() > previous.getOrDefault(entry.getKey(), 0)) {
                previous.put(entry.getKey(), entry.getValue());
                fire(event, entry.getKey().getCreatureId(), 0);
            }
        }
    }

    @Override
    protected boolean eventMatches(TriggerGenericData condition, int arg1, int arg2) {
        switch (condition.getType()) {
            case PLAYER_CREATURE_PICKED_UP, PLAYER_CREATURE_DROPPED, PLAYER_CREATURE_SLAPPED -> {

                // The creature type filter, 0 = any. The shipped maps all use 0 here
                int creatureId = condition.hasUserDataKey("creatureId") ? condition.getUserData("creatureId", short.class) : 0;
                return creatureId == 0 || creatureId == arg1;
            }
            default -> {
                return super.eventMatches(condition, arg1, arg2);
            }
        }
    }

    @Override
    protected boolean isActive(TriggerGenericData trigger) {
        int target;
        int value;

        TriggerGeneric.TargetType targetType = trigger.getType();
        switch (targetType) {
            case PLAYER_CREATURES: {
                short creatureId = trigger.getUserData("creatureId", short.class);
                target = getCreaturesCount(playerId, creatureId);
                if (isConstant(trigger)) {
                    value = trigger.getUserData("value", int.class);
                } else {
                    value = getCreaturesCount(trigger.getUserData("playerId", short.class), creatureId);
                }
                return compare(trigger, target, value);
            }

            case PLAYER_ROOM_SLABS: {
                short roomId = trigger.getUserData("roomId", short.class);
                target = getRoomSlabsCount(playerId, roomId);
                if (isConstant(trigger)) {
                    value = trigger.getUserData("value", int.class);
                } else {
                    value = getRoomSlabsCount(trigger.getUserData("playerId", short.class), roomId);
                }
                return compare(trigger, target, value);
            }

            case PLAYER_ROOMS: {
                short roomId = trigger.getUserData("roomId", short.class);
                target = getRoomCount(playerId, roomId);
                if (isConstant(trigger)) {
                    value = trigger.getUserData("value", int.class);
                } else {
                    value = getRoomCount(trigger.getUserData("playerId", short.class), roomId);
                }
                return compare(trigger, target, value);
            }

            case PLAYER_KEEPER_SPELL: {

                // Is the spell available. Nothing is compared here, the original ignores the flag
                // and the value bytes
                KeeperSpell keeperSpell = levelInfo.getLevelData().getKeeperSpellById(trigger.getUserData("targetId", short.class));
                return getPlayerController(playerId).getSpellControl().isAvailable(keeperSpell);
            }

            case PLAYER_ROOM_SIZE: {
                short roomId = trigger.getUserData("roomId", short.class);
                target = getLargestRoomSize(playerId, roomId);
                value = isConstant(trigger) ? trigger.getUserData("value", int.class)
                        : getLargestRoomSize(trigger.getUserData("playerId", short.class), roomId);
                return compare(trigger, target, value);
            }

            case PLAYER_ROOM_FURNITURE: {
                short roomId = trigger.getUserData("roomId", short.class);
                target = getFurnitureCount(playerId, roomId);
                value = isConstant(trigger) ? trigger.getUserData("value", int.class)
                        : getFurnitureCount(trigger.getUserData("playerId", short.class), roomId);
                return compare(trigger, target, value);
            }

            case PLAYER_SLAPS:
                target = getSlapCount(playerId);
                value = isConstant(trigger) ? trigger.getUserData("value", int.class)
                        : getSlapCount(trigger.getUserData("playerId", short.class));
                return compare(trigger, target, value);

            case PLAYER_GOLD:
                target = getPlayer().getGold();
                value = isConstant(trigger) ? trigger.getUserData("value", int.class)
                        : getPlayer(trigger.getUserData("playerId", short.class)).getGold();
                return compare(trigger, target, value);

            case PLAYER_GOLD_MINED:
                target = getPlayer().getGoldMined();
                value = isConstant(trigger) ? trigger.getUserData("value", int.class)
                        : getPlayer(trigger.getUserData("playerId", short.class)).getGoldMined();
                return compare(trigger, target, value);

            case PLAYER_MANA:
                target = getPlayer().getMana();
                value = isConstant(trigger) ? trigger.getUserData("value", int.class)
                        : getPlayer(trigger.getUserData("playerId", short.class)).getMana();
                return compare(trigger, target, value);

            case GUI_TRANSITION_ENDS:
                return !playerService.isInTransition();

            // Events. They are always false when the tree is walked, see pollEvents() for the ones we notice
            case PLAYER_CREATURE_PICKED_UP:
            case PLAYER_CREATURE_DROPPED:
            case PLAYER_CREATURE_SLAPPED:
                return false;

            case GUI_BUTTON_PRESSED:
                // STUB, event: a GUI button was clicked, matched by the button group and button
                // (userdata targetType / targetId). The HUD buttons do not report their clicks to
                // the game logic, and the map's button IDs would need a mapping to our own buttons.
                // The place to raise it from is fire(GUI_BUTTON_PRESSED, group, button)
                return false;

            case PLAYER_DUNGEON_BREACHED:
            case PLAYER_ENEMY_BREACHED:
                // STUB, events: an enemy broke into this dungeon / this player broke into one. Nothing
                // in the game logic detects a dungeon breach, so there is nothing to raise these from
                return false;

            case PLAYER_CREATURE_SACKED:
            case LEVEL_PAY_DAY:
                // STUB, events: a creature left the player / the player was paid. Nothing in the game
                // logic announces either, there is no pay day processing to hook into
                return false;

            case PLAYER_HAPPY_CREATURES:
            case PLAYER_ANGRY_CREATURES:
                // STUB: non-worker creatures that are happy / angry. Creatures only have anger counters
                // per complaint (CreatureMood), the happy (0) and angry (2) mood values of the original
                // and the thresholds that turn the counters into them are not known
                return false;

            case PLAYER_CREATURES_KILLED:
            case PLAYER_KILLS_CREATURES:
                // STUB: creatures lost / killed, optionally by a certain opponent. A creature's death
                // is not attributed to anybody, so there is nothing to count
                return false;

            case PLAYER_DOORS:
            case PLAYER_TRAPS:
                // STUB: owned doors / traps by type, blueprints and built ones separately (with the
                // availability check when there is no comparison). The door and trap controls only know
                // the availability, their count is a constant 0 and nothing tells blueprints from built
                return false;

            case PLAYER_CREATURES_AT_LEVEL:
            case PLAYER_CREATURES_DYING:
            case PLAYER_CREATURES_GROUPED:
                // STUB: creatures by level / in the dying state / the size of the current creature
                // group. The player creature control only keeps the creature IDs by type, going through
                // them for these needs the creature controller of every one of them
                return false;

            case PLAYER_DESTROYS:
                return true; // Always true in the original

            case PLAYER_KILLED:
                // STUB: has this player been defeated, by a certain player (userdata playerId). The
                // game does not record who defeated a player
                return false;

            default:
                return super.isActive(trigger);
        }
    }

    @Override
    protected boolean doAction(TriggerActionData trigger) {
        Predicate<TriggerActionData> handler = actionHandlers.get(trigger.getType());
        if (handler != null) {
            return handler.test(trigger);
        }
        return super.doAction(trigger);
    }

    /**
     * Is the value compared to a constant (the bit 0 of the flag byte), or to
     * the same measure of another player. The editor always writes the former.
     */
    private static boolean isConstant(TriggerGenericData trigger) {
        return (trigger.getUserData("flag", short.class) & 1) != 0;
    }

    private void winGame(TriggerActionData trigger) { // Game part. only for keeper x
        gameController.endGame(playerId, true);
    }

    private void loseGame(TriggerActionData trigger) { // Game part. only for keeper x
        gameController.endGame(playerId, false);
    }

    private boolean generateCreature(TriggerActionData trigger) { // Creature part. Only for keeper x
        short creatureId = trigger.getUserData("creatureId", short.class);
        short level = trigger.getUserData("level", short.class);

        // Get first spawn point of the player (this flag is only for the players)
        Set<IRoomController> rooms = getPlayerController(playerId).getRoomControl().getRoomControllers().get(levelInfo.getLevelData().getPortal());
        if (rooms == null || rooms.isEmpty()) {
            logger.log(Level.WARNING, "Generate creature triggered but no entrances found!");

            // Not handled, it is tried again on the next walk. A portal may be built later
            return false;
        }
        ICreatureEntrance room = ((ICreatureEntrance) rooms.iterator().next());
        creaturesController.spawnCreature(creatureId, playerId, level, WorldUtils.pointToVector2f(room.getEntranceCoordinate()), ICreaturesController.SpawnType.ENTRANCE);
        return true;
    }

    private void setPortalStatus(TriggerActionData trigger) { // Creature part. Only for keeper x
        boolean available = trigger.getUserData("available", short.class) != 0;
        getPlayerController(playerId).getRoomControl().setPortalsOpen(available);
    }

    private void followCameraPath(TriggerActionData trigger) { // gui part. Only for keeper x
        ActionPoint ap = levelInfo.getActionPoint(trigger.getUserData("actionPointId", short.class));
        playerService.doTransition(trigger.getUserData("pathId", short.class), WorldUtils.ActionPointToVector3f(ap), playerId);
    }

    private void makeObjective(TriggerActionData trigger) { // Game part
        short targetId = trigger.getUserData("targetId", short.class);
        if (targetId == 0) { // 0 = Off
            makeObjectiveOff();
        } else {
            logger.log(Level.WARNING, "Unsupported MAKE_OBJECTIVE target {0}", targetId);
        }
    }

    private void zoomToActionPoint(TriggerActionData trigger) { // AP part
        zoomToAP(playerId, trigger.getUserData("targetId", short.class));
    }

    private void rotateAroundActionPoint(TriggerActionData trigger) { // AP part
        ActionPoint ap = levelInfo.getActionPoint(trigger.getUserData("actionPointId", short.class));
        boolean isRelative = trigger.getUserData("available", short.class) == 0;
        int angle = (short) (int) trigger.getUserData("angle", int.class); // Signed 16 bit degrees
        int time = trigger.getUserData("time", int.class);
        playerService.rotateViewAroundPoint(WorldUtils.ActionPointToVector3f(ap), isRelative, angle, time, playerId);
    }

    private void playSpeech(TriggerActionData trigger) { // Info part
        int speechId = trigger.getUserData("speechId", int.class);
        int pathId = trigger.getUserData("pathId", int.class);
        // text show when Cinematic camera by pathId
        boolean introduction = trigger.getUserData("introduction", short.class) != 0;
        boolean showText = trigger.getUserData("text", short.class) == 0;
        playerService.playSpeech(speechId, showText, introduction, pathId, playerId);
    }

    private void displayTextString(TriggerActionData trigger) { // Info part
        int textId = trigger.getUserData("textId", int.class);
        playerService.showMessage(textId, playerId);
    }

    private void setWidescreenMode(TriggerActionData trigger) { // Info part
        boolean available = trigger.getUserData("available", short.class) != 0;
        playerService.setWidescreen(available, playerId);
    }

    private void setMusicLevel(TriggerActionData trigger) {
        // No-op in the original game as well, the music intensity is never changed by the trigger
    }

    private void setSlapsLimit(TriggerActionData trigger) {
        // STUB: limit the number of slaps of the player (value 0 = off). The slap system does not
        // have a limit to set, the player can slap without any restrictions
        logStub(trigger, "the slap system has no limit");
    }

    protected Keeper getPlayer() {
        return super.getPlayer(playerId);
    }

    private int getCreaturesCount(short playerId, short creatureId) {
        if (creatureId == 0) {
            return getPlayerController(playerId).getCreatureControl().getTypeCount();
        } else {
            return getPlayerController(playerId).getCreatureControl().getTypeCount(levelInfo.getLevelData().getCreature(creatureId));
        }
    }

    private int getRoomSlabsCount(short playerId, short roomId) {
        if (roomId == 0) {
            return getPlayerController(playerId).getRoomControl().getRoomSlabsCount();
        } else {
            return getPlayerController(playerId).getRoomControl().getRoomSlabsCount(levelInfo.getLevelData().getRoomById(roomId));
        }
    }

    private int getLargestRoomSize(short playerId, short roomId) {
        Room room = roomId == 0 ? null : levelInfo.getLevelData().getRoomById(roomId);
        int size = 0;
        for (Map.Entry<Room, Set<IRoomController>> entry : getPlayerController(playerId).getRoomControl().getRoomControllers().entrySet()) {
            if (room == null || room.equals(entry.getKey())) {
                for (IRoomController roomController : new ArrayList<>(entry.getValue())) {
                    size = Math.max(size, roomController.getRoomInstance().getCoordinates().size());
                }
            }
        }
        return size;
    }

    private int getFurnitureCount(short playerId, short roomId) {
        Room room = roomId == 0 ? null : levelInfo.getLevelData().getRoomById(roomId);
        int count = 0;
        for (Map.Entry<Room, Set<IRoomController>> entry : getPlayerController(playerId).getRoomControl().getRoomControllers().entrySet()) {
            if (room == null || room.equals(entry.getKey())) {
                for (IRoomController roomController : new ArrayList<>(entry.getValue())) {
                    count += roomController.getFloorFurnitureCount() + roomController.getWallFurnitureCount();
                }
            }
        }
        return count;
    }

    private int getSlapCount(short playerId) {
        return getPlayerController(playerId).getStatsControl().getSlaps().values().stream().mapToInt(Integer::intValue).sum();
    }

    private int getRoomCount(short playerId, short roomId) {
        if (roomId == 0) {
            return getPlayerController(playerId).getRoomControl().getTypeCount();
        } else {
            return getPlayerController(playerId).getRoomControl().getTypeCount(levelInfo.getLevelData().getRoomById(roomId));
        }
    }

    protected void makeObjectiveOff() {
        // STUB: remove the objective marker of the player. There are no objective markers to
        // remove, the creatures' MAKE_OBJECTIVE only flags the creature (see the creature scope)
    }

    protected PlayerService getPlayerService() {
        return playerService;
    }

}
