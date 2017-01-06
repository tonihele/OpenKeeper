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

import com.jme3.app.state.AppStateManager;
import java.awt.Point;
import java.util.EnumSet;
import java.util.logging.Logger;
import toniarts.openkeeper.game.action.ActionPoint;
import toniarts.openkeeper.game.control.Control;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.player.PlayerCameraControl;
import toniarts.openkeeper.game.state.GameState;
import toniarts.openkeeper.game.state.PlayerState;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.convert.map.TriggerAction.FlagTargetValueActionType;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;
import toniarts.openkeeper.view.PlayerCameraState;
import toniarts.openkeeper.world.ThingLoader;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.creature.Party;

/**
 *
 * @author ArchDemon
 */
public class TriggerControl extends Control {

    private static final short LEVEL_SCORE_FLAG_ID = 128;
    private static final short TIME_LIMIT_TIMER_ID = 16;

    protected TriggerGenericData trigger;
    protected TriggerGenericData root;

    protected AppStateManager stateManager;

    private static final Logger logger = Logger.getLogger(TriggerControl.class.getName());

    /**
     * empty serialization constructor
     */
    public TriggerControl() {
        super();
    }

    public TriggerControl(final AppStateManager stateManager, int triggerId) {

        this.stateManager = stateManager;
        root = new TriggerLoader(this.stateManager.getState(GameState.class).getLevelData()).load(triggerId);
        if (root == null) {
            throw new IllegalArgumentException("trigger can not be null");
        }
        trigger = root;
    }

    @Override
    protected void updateControl(float tpf) {
        TriggerGenericData next = null;
        trigger.subRepeatTimes();

        for (int i = trigger.getLastTriggerIndex() + 1; i < trigger.getQuantity(); i++) {
            TriggerData value = trigger.getChild(i);

            if (value == null) {
                logger.warning("Trigger is null");

            } else if (value instanceof TriggerGenericData) {

                if (next == null && isActive((TriggerGenericData) value)) {
                    trigger.setLastTrigger((TriggerGenericData) value);
                    next = (TriggerGenericData) value;
                }

            } else if (value instanceof TriggerActionData) {

                //System.out.println(String.format("%s: %d %s", this.getClass().getSimpleName(), trigger.getId(), trigger.getType()));
                doAction((TriggerActionData) value);
                if (!trigger.isRepeateable()) {
                    trigger.detachChild(value);
                    i--;
                }
            }
        }

        if (trigger.getQuantity() == 0 && trigger.getParent() != null) {
            next = trigger.getParent();
            trigger.detachFromParent();
        }

        if (next == null) {
            trigger.setLastTrigger(null);
            trigger = (trigger.getParent() != null) ? trigger.getParent() : root;
        } else {
            trigger = next;
        }
    }

    protected boolean isActive(TriggerGenericData trigger) {
        boolean result = false;

        int value = 0;
        int target = 0;

        TriggerGeneric.TargetType targetType = trigger.getType();
        switch (targetType) {
            case FLAG:
                short targetId = (Short) trigger.getUserData("targetId");
                if (targetId == LEVEL_SCORE_FLAG_ID) {

                    // A special value, level score
                    target = stateManager.getState(GameState.class).getLevelScore();
                } else {
                    target = stateManager.getState(GameState.class).getFlag(targetId);
                }
                if ((Short) trigger.getUserData("flag") == 1) {
                    value = (Integer) trigger.getUserData("value");
                } else {
                    value = stateManager.getState(GameState.class).getFlag((Short) trigger.getUserData("flagId"));
                }
                break;

            case TIMER:
                target = (int) stateManager.getState(GameState.class).getTimer((Short) trigger.getUserData("targetId")).getTime();
                if ((Short) trigger.getUserData("flag") == 1) {
                    value = (Integer) trigger.getUserData("value");
                } else {
                    value = (int) Math.floor(stateManager.getState(GameState.class).getTimer((Short) trigger.getUserData("timerId")).getTime());
                }
                break;

            case LEVEL_TIME:
                target = (int) Math.floor(stateManager.getState(GameState.class).getGameTime());
                value = (Integer) trigger.getUserData("value");
                break;
            case LEVEL_CREATURES:
                return false;
            case LEVEL_PAY_DAY:
                return false;
            case LEVEL_PLAYED:
                return false;
            default:
                logger.warning("Target Type not supported");
                return false;
        }

        TriggerGeneric.ComparisonType comparisonType = trigger.getComparison();
        if (comparisonType != null && comparisonType != TriggerGeneric.ComparisonType.NONE) {
            result = compare(target, comparisonType, value);
        }

        return result;
    }

    protected void doAction(TriggerActionData trigger) {
        //System.out.println(String.format("\t Action: %d %s", trigger.getId(), trigger.getType())); // TODO remove this line

        TriggerAction.ActionType type = trigger.getType();
        switch (type) {

            case CREATE_CREATURE:
                // TODO this
                short creatureId = trigger.getUserData("creatureId", short.class);
                short playerId = trigger.getUserData("playerId", short.class);
                short level = trigger.getUserData("level", short.class);
                EnumSet<Creature.CreatureFlag> flags = ConversionUtils.parseFlagValue(trigger.getUserData("flag", short.class), Creature.CreatureFlag.class);
                Point p = new Point(trigger.getUserData("posX", int.class) - 1,
                        trigger.getUserData("posY", int.class) - 1);
                break;

            case MAKE:
                TriggerAction.MakeType flag = ConversionUtils.parseEnum(trigger.getUserData("type", short.class),
                        TriggerAction.MakeType.class);
                boolean available = trigger.getUserData("available", short.class) != 0;
                playerId = trigger.getUserData("playerId", short.class);
                Keeper keeper = getPlayer(playerId);
                KwdFile kwdFile = stateManager.getState(GameState.class).getLevelData();
                short targetId = trigger.getUserData("targetId", short.class);
                // TODO this
                switch (flag) {
                    case CREATURE:
                        keeper.getCreatureControl().setTypeAvailable(kwdFile.getCreature(targetId), available);
                        break;
                    case DOOR:
                        break;
                    case KEEPER_SPELL:
                        keeper.getSpellControl().setTypeAvailable(kwdFile.getKeeperSpellById(targetId), available);
                        break;
                    case ROOM:
                        keeper.getRoomControl().setTypeAvailable(kwdFile.getRoomById(targetId), available);
                        break;
                    case TRAP:
                        break;
                }
                break;

            case FLAG:
                short flagId = trigger.getUserData("flagId", short.class);
                EnumSet<FlagTargetValueActionType> flagType = ConversionUtils.parseFlagValue(trigger.getUserData("flag", short.class),
                        FlagTargetValueActionType.class);
                int value = trigger.getUserData("value", int.class);
                if (flagType.contains(FlagTargetValueActionType.TARGET)) {
                    value = stateManager.getState(GameState.class).getFlag(value);
                }

                if (flagId == LEVEL_SCORE_FLAG_ID) {
                    GameState gs = stateManager.getState(GameState.class);
                    gs.setLevelScore(getTargetValue(gs.getLevelScore(), value, flagType));
                } else {
                    int base = stateManager.getState(GameState.class).getFlag(flagId);
                    stateManager.getState(GameState.class).setFlag(flagId, getTargetValue(base, value, flagType));
                }
                break;

            case INITIALIZE_TIMER:
                short timerId = trigger.getUserData("timerId", short.class);
                if (timerId == TIME_LIMIT_TIMER_ID) {
                    value = trigger.getUserData("value", int.class);
                    stateManager.getState(GameState.class).setTimeLimit(value);
                } else {
                    stateManager.getState(GameState.class).getTimer(timerId).setActive(true);
                }
                break;

            case CREATE_HERO_PARTY:
                ThingLoader loader = stateManager.getState(WorldState.class).getThingLoader();
                Party party = loader.getParty(trigger.getUserData("partyId", short.class));
                party.setType(ConversionUtils.parseEnum(trigger.getUserData("type", short.class), Party.Type.class));
                ActionPoint ap = getActionPoint(trigger.getUserData("actionPointId", short.class));

                // Load the party members
                for (Thing.GoodCreature creature : party.getMembers()) {
                    CreatureControl creatureInstance = loader.spawnCreature(creature, ap.getCenter(), stateManager.getApplication());
                    creatureInstance.setParty(party);
                    party.addMemberInstance(creatureInstance);

                    // Also add to the creature trigger control
                    if (creature.getTriggerId() != 0) {
                        stateManager.getState(GameState.class).getCreatureTriggerState().setThing(creature.getTriggerId(), creatureInstance);
                    }
                }
                party.setCreated(true);
                break;

            case SET_ALLIANCE:
                // TODO this
                short playerOneId = trigger.getUserData("playerOneId", short.class);
                short playerTwoId = trigger.getUserData("playerTwoId", short.class);
                available = trigger.getUserData("available", short.class) == 0; // 0 = Create, !0 = Break
                break;

            case ALTER_TERRAIN_TYPE:
                p = new Point(trigger.getUserData("posX", int.class) - 1,
                        trigger.getUserData("posY", int.class) - 1);
                short terrainId = trigger.getUserData("terrainId", short.class);
                playerId = trigger.getUserData("playerId", short.class);
                stateManager.getState(WorldState.class).alterTerrain(p, terrainId, playerId, true);
                break;

            case COLLAPSE_HERO_GATE:
                // TODO this
                p = new Point(trigger.getUserData("posX", int.class) - 1,
                        trigger.getUserData("posY", int.class) - 1);
                break;

            case SET_CREATURE_MOODS:
                // TODO this
                available = trigger.getUserData("available", short.class) != 0;
                break;

            case SET_SYSTEM_MESSAGES:
                // TODO this
                available = trigger.getUserData("available", short.class) != 0;
                break;

            case CHANGE_ROOM_OWNER:
                // TODO this
                playerId = trigger.getUserData("playerId", short.class);
                p = new Point(trigger.getUserData("posX", int.class) - 1,
                        trigger.getUserData("posY", int.class) - 1);
                break;

            case SET_SLAPS_LIMIT:
                // TODO this
                value = trigger.getUserData("value", int.class); // 0 = Off
                break;

            case SET_TIMER_SPEECH:
                // TODO this
                available = trigger.getUserData("available", short.class) != 0;
                break;

            default:
                logger.warning("Trigger Action not supported!");
                break;
        }
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
                logger.warning("Comparison Type not supported");
                break;
        }
        return result;
    }

    protected ActionPoint getActionPoint(int id) {
        return stateManager.getState(GameState.class).getActionPointState().getActionPoint(id);
    }

    private int getTargetValue(int base, int value, EnumSet<FlagTargetValueActionType> flagType) {

        if (flagType.contains(FlagTargetValueActionType.EQUAL)) {
            return value;
        } else if (flagType.contains(FlagTargetValueActionType.PLUS)) {
            return base + value;
        } else if (flagType.contains(FlagTargetValueActionType.MINUS)) {
            return base - value;
        }

        logger.warning("Unsupported target flag type");
        return 0;
    }

    protected Keeper getPlayer(short playerId) {
        GameState gameState = stateManager.getState(GameState.class);
        if (playerId == 0) {
            return gameState.getPlayer(this.stateManager.getState(PlayerState.class).getPlayerId()); // Current player
        } else {
            return gameState.getPlayer(playerId);
        }
    }

    protected void zoomToAP(int apId) {
        if (apId != 0) {
            PlayerCameraState pcs = stateManager.getState(PlayerCameraState.class);
            getActionPoint(apId).addControl(new PlayerCameraControl(pcs.getCamera()));
        }
    }
}
