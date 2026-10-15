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
package toniarts.openkeeper.game.trigger;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import toniarts.openkeeper.game.control.Control;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.creature.IPartyController;
import toniarts.openkeeper.game.controller.creature.PartyType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.data.GameTimer;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.convert.map.TriggerAction.FlagTargetValueActionType;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * The level scope of the trigger system and the base of all the other scopes.
 * <p>
 * A trigger tree is a forest of conditions and actions. The conditions are
 * evaluated, and the actions run, by walking the tree depth first on every
 * tick, see {@link #walk(TriggerGenericData, short)}. Nesting means AND,
 * siblings are independent of each other and see each other's side effects
 * right away. The scopes (level, player, creature, ...) are the subclasses,
 * each one handles what it knows and forwards the rest to its super class:
 * creature -&gt; player -&gt; level. What nobody handles is logged and is not
 * considered done.
 * <p>
 * Events (killed, slapped, created...) are not evaluated by the walk, they are
 * always false there. The scopes raise them with
 * {@link #fire(TriggerGeneric.TargetType, int, int)} when they notice the
 * thing happening.
 * <p>
 * Based on the Dungeon Keeper II v1.70 behaviour described in
 * trigger_system_design_paper.md.
 *
 * @author ArchDemon
 */
public class TriggerControl extends Control {

    private static final Logger logger = System.getLogger(TriggerControl.class.getName());

    private static final short LEVEL_SCORE_FLAG_ID = 128;
    private static final short TIME_LIMIT_TIMER_ID = 16;
    private static final int FLAG_COUNT = 128;
    private static final int TIMER_COUNT = 16;

    /**
     * The trigger times are in seconds, but the original compares them on its
     * game ticks. All the shipped levels run 4 ticks per second, and the
     * quantization matters when "equals" is used, it can only hit one tick.
     */
    private static final int TICKS_PER_SECOND = 4;

    protected final TriggerGenericData root;

    protected final ILevelInfo levelInfo;
    protected final IGameTimer gameTimer;
    protected final IGameController gameController;
    protected final IMapController mapController;
    protected final ICreaturesController creaturesController;
    protected final PlayerService playerService;

    private boolean armed = true;
    private final Set<Integer> reportedUnhandled = new HashSet<>();

    public TriggerControl(final IGameController gameController, final ILevelInfo levelInfo,
            final IGameTimer gameTimer, final IMapController mapController,
            final ICreaturesController creaturesController, final PlayerService playerService,
            final int triggerId) {

        this.gameController = gameController;
        this.levelInfo = levelInfo;
        this.gameTimer = gameTimer;
        this.mapController = mapController;
        this.creaturesController = creaturesController;
        this.playerService = playerService;

        root = new TriggerLoader(levelInfo.getLevelData()).load(triggerId);
        if (root == null) {
            throw new IllegalArgumentException("trigger can not be null");
        }
    }

    @Override
    protected void updateControl(float tpf) {
        if (!isArmed()) {
            return;
        }
        pollEvents();
        walk(root, getRootRepeat());
    }

    /**
     * Walk a chain of siblings, depth first. A condition that holds opens its
     * children, a false one skips its subtree but not its siblings. An action
     * that has been handled disables itself if the repeat passed down is 1.
     * <p>
     * The repeat is the one of the parent condition (or of the root chain for
     * the top level). Any value but 1 (0, 2, 10, 255...) means that the actions
     * run on every walk during which the parent holds. The original has a
     * routine that was meant to count the repeat down and disable the
     * condition, but nothing ever calls it, so "do it 10 times" in a map is
     * really "do it all the time". We do what the original does.
     *
     * @param container the parent whose children are walked
     * @param repeat the repeat for the actions directly under the container
     */
    protected void walk(TriggerGenericData container, short repeat) {
        for (TriggerData node : container.getChildren().getArray()) {
            if (!node.isEnabled()) {
                continue;
            }

            if (node instanceof TriggerGenericData condition) {
                if (isActive(condition)) {
                    walk(condition, condition.getRepeatTimes());
                }
            } else if (node instanceof TriggerActionData action) {
                if (doAction(action) && repeat == 1) {
                    action.setEnabled(false);
                }
            }
        }
    }

    /**
     * Raise an event. Only the top level chain of the tree is searched, an
     * event condition nested under another condition never triggers (this is
     * how the original does it as well). The search also stops at the first
     * action of the chain.
     *
     * @param type the event
     * @param arg1 first event argument, see {@link #eventMatches}
     * @param arg2 second event argument
     */
    protected void fire(TriggerGeneric.TargetType type, int arg1, int arg2) {
        if (!isArmed()) {
            return;
        }
        for (TriggerData node : root.getChildren().getArray()) {
            if (!(node instanceof TriggerGenericData condition)) {
                break;
            }
            if (condition.getType() == type && condition.isEnabled() && eventMatches(condition, arg1, arg2)) {
                walk(condition, condition.getRepeatTimes());
            }
        }
    }

    protected final void fire(TriggerGeneric.TargetType type) {
        fire(type, 0, 0);
    }

    /**
     * Does the event condition match the raised event. Most events carry no
     * data at all and match always.
     *
     * @param condition the event condition
     * @param arg1 first event argument
     * @param arg2 second event argument
     * @return true if the event condition applies
     */
    protected boolean eventMatches(TriggerGenericData condition, int arg1, int arg2) {
        return true;
    }

    /**
     * Called before each walk. The scopes that have to notice events from
     * state changes (the game does not announce them) do that here and call
     * {@link #fire(TriggerGeneric.TargetType, int, int)}.
     */
    protected void pollEvents() {
    }

    /**
     * Is the tree live. A disarmed tree is not walked and does not get events.
     *
     * @return true if live
     */
    protected boolean isArmed() {
        return armed;
    }

    /**
     * The repeat of the actions sitting directly in the root chain
     *
     * @return the repeat
     */
    protected short getRootRepeat() {
        return root.getRepeatTimes();
    }

    /**
     * Arm or disarm the whole tree. Used for the hero parties, whose trees
     * are dormant until the party is created. Arming also forces the repeat of
     * every condition to 1, so all of the actions in a party tree are
     * one-shot, no matter what the map says. Disarming zeroes the repeats and
     * disables everything.
     *
     * @param on true to arm
     */
    protected void setTreeArmed(boolean on) {
        armed = on;
        setTreeArmed(root, on);
    }

    private void setTreeArmed(TriggerGenericData container, boolean on) {
        for (TriggerData node : container.getChildren().getArray()) {
            node.setEnabled(on);
            if (node instanceof TriggerGenericData condition) {
                condition.setRepeatTimes((short) (on ? 1 : 0));
                setTreeArmed(condition, on);
            }
        }
    }

    /**
     * Evaluate a condition in this scope. Event conditions are false here, see
     * {@link #fire(TriggerGeneric.TargetType, int, int)}.
     *
     * @param trigger the condition
     * @return true if the condition holds
     */
    protected boolean isActive(TriggerGenericData trigger) {
        int target;
        int value;

        TriggerGeneric.TargetType targetType = trigger.getType();
        switch (targetType) {
            case FLAG: {
                short targetId = trigger.getUserData("targetId", short.class);
                target = (targetId == LEVEL_SCORE_FLAG_ID) ? levelInfo.getLevelScore() : getFlag(targetId);
                if ((trigger.getUserData("flag", short.class) & 1) != 0) {
                    value = trigger.getUserData("value", int.class);
                } else {
                    value = getFlag(trigger.getUserData("flagId", short.class));
                }
                return compare(trigger, target, value);
            }

            case TIMER: {
                short targetId = trigger.getUserData("targetId", short.class);
                if (targetId == TIME_LIMIT_TIMER_ID) {
                    Float timeLimit = levelInfo.getTimeLimit();
                    target = (timeLimit != null) ? (int) Math.ceil(timeLimit * TICKS_PER_SECOND) : 0;
                    return compare(trigger, target, trigger.getUserData("value", int.class) * TICKS_PER_SECOND);
                }

                // A timer that has never been started is never true, not even for "elapsed >= 0"
                GameTimer timer = getTimer(targetId);
                if (timer == null || !timer.isActive()) {
                    return false;
                }
                target = toTicks(timer.getTime());
                if ((trigger.getUserData("flag", short.class) & 1) != 0) {
                    value = trigger.getUserData("value", int.class) * TICKS_PER_SECOND;
                } else {
                    GameTimer other = getTimer(trigger.getUserData("timerId", short.class));
                    if (other == null || !other.isActive()) {
                        return false;
                    }
                    value = toTicks(other.getTime());
                }
                return compare(trigger, target, value);
            }

            case LEVEL_TIME:
                return compare(trigger, toTicks((float) gameTimer.getGameTime()),
                        trigger.getUserData("value", int.class) * TICKS_PER_SECOND);

            case LEVEL_PAY_DAY:
                // STUB, event: the player was paid. Nothing in the game logic announces a pay day
                logStub(trigger, "pay day is not raised by anybody");
                return false;

            case LEVEL_CREATURES:
                // STUB: creatures of any owner in the world, compared with the value. There is no
                // way to ask the creature controller for a world wide count (only per player), and
                // no shipped level uses this condition.
                logStub(trigger, "no world wide creature count");
                return false;

            case LEVEL_PLAYED:
                // STUB: the number of attempts at this level. The game does not keep any attempt
                // statistics (nothing persists between sessions), so there is nothing to compare to.
                logStub(trigger, "no level attempt statistics");
                return false;

            default:
                reportUnhandled("condition", trigger);
                return false;
        }
    }

    /**
     * Run an action in this scope.
     *
     * @param trigger the action
     * @return true if the action was handled, false if no scope knows it. An
     * action that is not handled stays enabled and is retried on the next
     * walk, this is also what the original does
     */
    protected boolean doAction(TriggerActionData trigger) {
        TriggerAction.ActionType type = trigger.getType();
        switch (type) {

            case CREATE_CREATURE:
                short creatureId = trigger.getUserData("creatureId", short.class);
                short playerId = trigger.getUserData("playerId", short.class);
                short level = trigger.getUserData("level", short.class);
                EnumSet<Creature.CreatureFlag> flags = ConversionUtils.parseFlagValue(trigger.getUserData("flag", short.class), Creature.CreatureFlag.class);
                Point p = new Point(trigger.getUserData("posX", int.class) - 1,
                        trigger.getUserData("posY", int.class) - 1);
                // STUB: the creature flags (will fight, dies instantly, will be attacked, return to
                // lair, free friends on jailbreak, act as dropped, start dying). ICreaturesController
                // hands back only an entity ID and the creature controller has no setters for these
                // behaviours yet, so they can not be applied after the spawn.
                if (!flags.isEmpty()) {
                    logStub(trigger, "creature flags " + flags + " are not applied");
                }
                creaturesController.spawnCreature(creatureId, playerId, level, WorldUtils.pointToVector2f(p), ICreaturesController.SpawnType.PLACE);
                return true;

            case CREATE_PORTAL_GEM:
                // STUB: needs to create a Portal Gem object (object 28 for player 1 in every shipped
                // record) on a tile. The trigger controls do not have access to the objects
                // controller, only the creature and the map controllers
                logStub(trigger, "the objects controller is not available to the triggers");
                return true;

            case DISPLAY_OBJECTIVE:
                // STUB (partly): the objective text of the local player is not shown, the game has no
                // service for the objective panel text. Only the "zoom to action point" part works.
                logStub(trigger, "the objective text is not shown");
                zoomToAP(getLocalPlayerId(), trigger.getUserData("actionPointId", short.class));
                return true;

            case INFORMATION:
                return true; // No-op in the original game as well

            case MAKE:
                makeAvailable(trigger);
                return true;

            case FLAG:
                setFlag(trigger);
                return true;

            case INITIALIZE_TIMER:
                GameTimer timer = getTimer(trigger.getUserData("timerId", short.class));
                if (timer != null) {
                    timer.initialize();
                }
                return true;

            case SET_TIME_LIMIT:
                // The editor always writes 16 for the timer ID, the time limit is the only target
                levelInfo.setTimeLimit(trigger.getUserData("value", int.class));
                return true;

            case FLASH_BUTTON:
                flashButton(trigger, getLocalPlayerId());
                return true;

            case FLASH_ACTION_POINT:
                flashActionPoint(trigger, getLocalPlayerId());
                return true;

            case REVEAL_ACTION_POINT:
                revealActionPoint(trigger, getLocalPlayerId());
                return true;

            case CREATE_HERO_PARTY:
                createHeroParty(trigger);
                return true;

            case SET_ALLIANCE:
                short playerOneId = trigger.getUserData("playerOneId", short.class);
                short playerTwoId = trigger.getUserData("playerTwoId", short.class);
                if (trigger.getUserData("available", short.class) == 0) { // 0 = Create, !0 = Break
                    gameController.createAlliance(playerOneId, playerTwoId);
                } else {
                    gameController.breakAlliance(playerOneId, playerTwoId);
                }
                return true;

            case ALTER_TERRAIN_TYPE:
                p = new Point(trigger.getUserData("posX", int.class) - 1,
                        trigger.getUserData("posY", int.class) - 1);
                mapController.alterTerrain(p, trigger.getUserData("terrainId", short.class),
                        trigger.getUserData("playerId", short.class));
                return true;

            case COLLAPSE_HERO_GATE:
                // STUB: collapse the hero gate room on the tile (1-based). Rooms can be captured
                // (IRoomController.captured) and destroyed, but there is no "collapse" that plays
                // the gate's own collapse sequence and removes the gate for good
                logStub(trigger, "there is no hero gate collapse sequence");
                return true;

            case SET_CREATURE_MOODS:
                levelInfo.setCreatureMoodsEnabled(trigger.getUserData("available", short.class) == 1);
                return true;

            case SET_SYSTEM_MESSAGES:
                levelInfo.setSystemMessagesEnabled(trigger.getUserData("available", short.class) == 1);
                return true;

            case SET_TIMER_SPEECH:
                levelInfo.setTimerSpeechEnabled(trigger.getUserData("available", short.class) == 1);
                return true;

            case CHANGE_ROOM_OWNER:
                p = new Point(trigger.getUserData("posX", int.class) - 1,
                        trigger.getUserData("posY", int.class) - 1);
                IRoomController room = mapController.getRoomControllerByCoordinates(p);
                if (room != null) {
                    room.captured(trigger.getUserData("playerId", short.class));
                }
                return true;

            case DISPLAY_SLAB_OWNER:
                // STUB: highlight the owner of a tile (or switch it off). There is no such
                // highlight in the game's view
                logStub(trigger, "the view has no tile owner highlight");
                return true;

            case DISPLAY_NEXT_ROOM_TYPE:
                // STUB: GUI hint for the next room to build, the HUD has no such hint
                logStub(trigger, "the HUD has no next room hint");
                return true;

            case TOGGLE_EFFECT_GENERATOR:
                // STUB: switch a placed effect generator on/off by its ID. The effect generators are
                // not addressable by their map ID after the level has been loaded
                logStub(trigger, "effect generators can not be looked up by their map ID");
                return true;

            default:
                // Send to action point, win/lose subobjective and modify creature pool are
                // unhandled in the original as well
                reportUnhandled("action", trigger);
                return false;
        }
    }

    /**
     * The player that the level scope actions (the ones that show something
     * to somebody) are for. The level does not have a subject, the original
     * addresses the local human player.
     *
     * @return the player ID
     */
    protected short getLocalPlayerId() {
        return Player.KEEPER1_ID;
    }

    private void flashButton(TriggerActionData trigger, short playerId) {
        TriggerAction.MakeType buttonType = ConversionUtils.parseEnum(trigger.getUserData("type", short.class),
                TriggerAction.MakeType.class);
        short targetId = trigger.getUserData("targetId", short.class);
        TriggerAction.ButtonType targetButtonType = null;
        if (buttonType == TriggerAction.MakeType.MISC_BUTTON) {
            targetButtonType = ConversionUtils.parseEnum(targetId, TriggerAction.ButtonType.class);
        }
        boolean available = trigger.getUserData("available", short.class) != 0;
        int time = trigger.getUserData("value", int.class);
        playerService.flashButton(buttonType, targetId, targetButtonType, available, time, playerId);
    }

    private void flashActionPoint(TriggerActionData trigger, short playerId) {
        ActionPoint ap = levelInfo.getActionPoint(trigger.getUserData("actionPointId", short.class));
        if (ap == null) {
            return;
        }
        int time = trigger.getUserData("value", int.class);
        boolean available = trigger.getUserData("available", short.class) != 0;
        if (available) {
            mapController.flashTiles(ap.getPoints(), playerId, time);
        } else {
            mapController.unFlashTiles(ap.getPoints(), playerId);
        }
    }

    private void revealActionPoint(TriggerActionData trigger, short playerId) {
        ActionPoint ap = levelInfo.getActionPoint(trigger.getUserData("actionPointId", short.class));
        if (ap == null) {
            return;
        }
        boolean conceal = trigger.getUserData("available", short.class) != 0;
        if (conceal) {
            mapController.concealTiles(ap.getPoints(), playerId);
        } else {
            mapController.revealTiles(ap.getPoints(), playerId);
        }
    }

    protected void zoomToAP(short playerId, short actionPointId) {
        ActionPoint ap = levelInfo.getActionPoint(actionPointId);
        if (ap != null) {
            playerService.zoomViewToPoint(WorldUtils.ActionPointToVector3f(ap), playerId);
        }
    }

    private void makeAvailable(TriggerActionData trigger) {
        TriggerAction.MakeType flag = ConversionUtils.parseEnum(trigger.getUserData("type", short.class),
                TriggerAction.MakeType.class);
        boolean available = trigger.getUserData("available", short.class) != 0;
        short playerId = trigger.getUserData("playerId", short.class);
        short targetId = trigger.getUserData("targetId", short.class);
        IKwdFile kwdFile = levelInfo.getLevelData();
        IPlayerController playerController = getPlayerController(playerId);
        switch (flag) {
            case CREATURE ->
                playerController.getCreatureControl().setTypeAvailable(kwdFile.getCreature(targetId), available);
            case DOOR ->
                playerController.getDoorControl().setTypeAvailable(kwdFile.getDoorById(targetId), available);
            case KEEPER_SPELL ->
                playerController.getSpellControl().setTypeAvailable(kwdFile.getKeeperSpellById(targetId), available);
            case ROOM ->
                playerController.getRoomControl().setTypeAvailable(kwdFile.getRoomById(targetId), available);
            case TRAP ->
                playerController.getTrapControl().setTypeAvailable(kwdFile.getTrapById(targetId), available);
            default ->
                logger.log(Level.WARNING, "Availability of {0} can not be set!", flag);
        }
    }

    private void setFlag(TriggerActionData trigger) {
        short flagId = trigger.getUserData("flagId", short.class);
        EnumSet<FlagTargetValueActionType> flagType = ConversionUtils.parseFlagValue(trigger.getUserData("flag", short.class),
                FlagTargetValueActionType.class);
        int value = trigger.getUserData("value", int.class);
        if (flagType.contains(FlagTargetValueActionType.TARGET)) {

            // The editor's "Level Score" source is the flag 128. The original reads past its flag
            // array there (the low byte of the first timer), we read the score the editor means
            value = (value == LEVEL_SCORE_FLAG_ID) ? levelInfo.getLevelScore() : getFlag(value);
        }

        if (flagId == LEVEL_SCORE_FLAG_ID) {
            levelInfo.setLevelScore(getTargetValue(levelInfo.getLevelScore(), value, flagType));
        } else if (flagId >= 0 && flagId < FLAG_COUNT) {

            // The flags are bytes, the arithmetic wraps around
            levelInfo.setFlag(flagId, getTargetValue(levelInfo.getFlag(flagId), value, flagType) & 0xFF);
        }
    }

    /**
     * Spawn a hero party. If the action is for an invasion party, and the
     * party of that kind is still out there, nothing happens.
     */
    private void createHeroParty(TriggerActionData trigger) {
        ActionPoint ap = levelInfo.getActionPoint(trigger.getUserData("actionPointId", short.class));
        short partyId = trigger.getUserData("partyId", short.class);
        PartyType partyType = ConversionUtils.parseEnum(trigger.getUserData("type", short.class), PartyType.class);
        if (partyType != PartyType.NONE && isPartyOut(partyId)) {
            return;
        }
        creaturesController.spawnHeroParty(partyId, partyType, WorldUtils.ActionPointToVector2f(ap));
    }

    private boolean isPartyOut(short partyId) {
        IPartyController party = creaturesController.getParty(partyId);
        if (party == null || !party.isCreated()) {
            return false;
        }
        for (ICreatureController member : party.getActualMembers()) {
            if (!member.isDead() && !member.isImprisoned() && !member.isUnconscious()
                    && member.getOwnerId() == Player.GOOD_PLAYER_ID) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compare the value with the comparison type of the condition
     *
     * @param trigger the condition
     * @param target the measured value
     * @param value the value to compare to
     * @return the result of the comparison, false if the condition has none
     */
    protected final boolean compare(TriggerGenericData trigger, float target, float value) {
        TriggerGeneric.ComparisonType comparisonType = trigger.getComparison();
        if (comparisonType != null && comparisonType != TriggerGeneric.ComparisonType.NONE) {
            return compare(target, comparisonType, value);
        }
        return false;
    }

    protected boolean compare(float target, TriggerGeneric.ComparisonType compare, float value) {
        boolean result = false;
        switch (compare) {
            case EQUAL_TO:
                result = target == value;
                break;
            case GREATER_OR_EQUAL_TO:
                result = target >= value;
                break;
            case GREATER_THAN:
                result = target > value;
                break;
            case LESS_OR_EQUAL_TO:
                result = target <= value;
                break;
            case LESS_THAN:
                result = target < value;
                break;
            case NOT_EQUAL_TO:
                result = target != value;
                break;
            case NONE:
                logger.log(Level.WARNING, "Comparison Type not supported!");
                break;
        }
        return result;
    }

    private int getTargetValue(int base, int value, EnumSet<FlagTargetValueActionType> flagType) {

        if (flagType.contains(FlagTargetValueActionType.EQUAL)) {
            return value;
        } else if (flagType.contains(FlagTargetValueActionType.PLUS)) {
            return base + value;
        } else if (flagType.contains(FlagTargetValueActionType.MINUS)) {
            return base - value;
        }

        logger.log(Level.WARNING, "Unsupported target flag type {0}!", flagType);
        return base;
    }

    /**
     * Read a flag. An index that is out of range reads 0 (the original reads
     * whatever memory is next to the flags).
     */
    private int getFlag(int flagId) {
        return (flagId >= 0 && flagId < FLAG_COUNT) ? levelInfo.getFlag(flagId) : 0;
    }

    private GameTimer getTimer(int timerId) {
        return (timerId >= 0 && timerId < TIMER_COUNT) ? levelInfo.getTimer(timerId) : null;
    }

    private static int toTicks(float seconds) {

        // Epsilon so that the float sums of 0.25s steps do not drop a tick
        return (int) Math.floor(seconds * TICKS_PER_SECOND + 0.0001f);
    }

    /**
     * Log, once per trigger, that no scope can handle the trigger
     */
    protected final void reportUnhandled(String kind, TriggerData trigger) {
        if (reportedUnhandled.add(trigger.getId())) {
            logger.log(Level.WARNING, "Unhandled Trigger {0}: {1}", kind, describe(trigger));
        }
    }

    /**
     * Log, once per trigger, that the trigger is (partly) a stub, with all of
     * its data. Collecting these tells what the maps really use of the
     * unfinished features.
     *
     * @param trigger the condition or action
     * @param reason what is missing
     */
    protected final void logStub(TriggerData trigger, String reason) {
        if (reportedUnhandled.add(trigger.getId())) {
            logger.log(Level.WARNING, "Stub {0}: {1}. Data: {2}", trigger.getClass().getSimpleName().replace("Data", ""), reason, describe(trigger));
        }
    }

    /**
     * Everything the map says about the trigger: ID, type, comparison, repeat,
     * the parsed fields and the owner of the tree
     */
    private String describe(TriggerData trigger) {
        StringBuilder sb = new StringBuilder("#").append(trigger.getId()).append(' ');
        if (trigger instanceof TriggerGenericData generic) {
            sb.append(generic.getType()).append(" comparison=").append(generic.getComparison() == null ? null : generic.getComparison().name());
        } else if (trigger instanceof TriggerActionData action) {
            sb.append(action.getType());
        }
        sb.append(" repeat=").append(trigger.getRepeatTimes()).append(" enabled=").append(trigger.isEnabled());
        sb.append(" tree=").append(getClass().getSimpleName());
        sb.append(" payload=[");
        for (short b : trigger.getPayload()) {
            sb.append(String.format("%02X", b));
        }
        sb.append(']');
        for (String key : new java.util.TreeSet<>(trigger.getUserDataKeys())) {
            Number value = trigger.getUserData(key);
            sb.append(' ').append(key).append('=').append(value);
        }
        return sb.toString();
    }

    protected Keeper getPlayer(short playerId) {
        if (playerId == 0) {
            return levelInfo.getPlayer(Player.KEEPER1_ID); // Current player
        } else {
            return levelInfo.getPlayer(playerId);
        }
    }

    protected IPlayerController getPlayerController(short playerId) {
        if (playerId == 0) {
            return gameController.getPlayerController(Player.KEEPER1_ID); // Current player
        } else {
            return gameController.getPlayerController(playerId);
        }
    }
}
