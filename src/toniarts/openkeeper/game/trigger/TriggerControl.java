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
import com.jme3.audio.AudioNode;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.action.ActionPoint;
import toniarts.openkeeper.game.action.ActionPointState;
import toniarts.openkeeper.game.control.Control;
import toniarts.openkeeper.game.player.PlayerCameraControl;
import toniarts.openkeeper.game.state.GameState;
import toniarts.openkeeper.game.state.PlayerState;
import toniarts.openkeeper.game.trigger.TriggerActionData;
import toniarts.openkeeper.game.trigger.TriggerLoader;
import toniarts.openkeeper.game.trigger.TriggerData;
import toniarts.openkeeper.game.trigger.TriggerGenericData;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.convert.map.TriggerAction.FlagTargetValueActionType;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;
import toniarts.openkeeper.view.PlayerCameraState;
import toniarts.openkeeper.world.WorldState;

/**
 *
 * @author ArchDemon
 */

public class TriggerControl extends Control {

    protected TriggerGenericData trigger;
    protected TriggerGenericData root;
    protected Main app;
    protected AppStateManager stateManager;
    private static final Logger logger = Logger.getLogger(TriggerControl.class.getName());

    public TriggerControl() {
    }

    public TriggerControl(final Main app, int triggerId) {
        this.app = app;
        stateManager = app.getStateManager();
        root = new TriggerLoader(this.stateManager.getState(GameState.class).getLevelData()).load(triggerId);
        if (root == null) {
            throw new IllegalArgumentException("trigger can not be null");
        }
        trigger = root;
    }

    @Override
    protected void updateControl(float tpf) {
        TriggerGenericData next = null;

        if (trigger.getQuantity() != 0) {
            ArrayList<Integer> remove = new ArrayList<>();
            System.out.println(String.format("Trigger Generic: %s at %s", trigger, GameState.getGameTime()));
            trigger.subRepeatTimes();

            for (Map.Entry<Integer, TriggerData> entry : trigger.getChildren().entrySet()) {
                TriggerData value = entry.getValue();

                if (value instanceof TriggerGenericData) {

                    if (isActive((TriggerGenericData) value) && next == null && !((TriggerGenericData) trigger).isCycleEnd()) {
                        next = (TriggerGenericData) value;
                    } else if (trigger.getParent() != null && next == null) {
                        next = trigger.getParent();
                    }

                } else if (value instanceof TriggerActionData) {

                    doAction((TriggerActionData) value);
                    if (!trigger.subRepeatTimes()) {
                        remove.add(value.getId());
                    }
                }
            }

            // remove actions
            for (Integer id : remove) {
                trigger.detachChildId(id);
            }
        }

        if (trigger.getQuantity() == 0 && trigger.getParent() != null) {
            if (next == null) {
                next = trigger.getParent();
            }
            trigger.getParent().detachChildId(trigger.getId());
        }

        if (next != null) {
            trigger = next;
        }
    }

    protected boolean isActive(TriggerGenericData trigger) {
        float value = 0;
        float target = 0;
        boolean result = false;

        TriggerGeneric.TargetType targetType = trigger.getType();
        switch (targetType) {
            case FLAG:
                target = stateManager.getState(GameState.class).getFlag((Short) trigger.getUserData("targetId"));
                if ((Short) trigger.getUserData("flag") == 1) {
                    value = (Integer) trigger.getUserData("value");
                } else {
                    value = stateManager.getState(GameState.class).getFlag((Short) trigger.getUserData("flagId"));
                }
                break;

            case TIMER:
                target = stateManager.getState(GameState.class).getTimer((Short) trigger.getUserData("targetId")).getTime();
                if ((Short) trigger.getUserData("flag") == 1) {
                    value = (Integer) trigger.getUserData("value");
                } else {
                    value = stateManager.getState(GameState.class).getTimer((Short) trigger.getUserData("timerId")).getTime();
                }
                break;

            case LEVEL_TIME:
                target = GameState.getGameTime();
                value = (Integer) trigger.getUserData("value");
                break;
            case LEVEL_CREATURES:
                return false;
            case LEVEL_PAY_DAY:
                return false;
            case LEVEL_PLAYED:
                return false;
            case GUI_TRANSITION_ENDS:
                return !GameState.getTransition();
            case GUI_BUTTON_PRESSED:
                return false;
            //default:
            //logger.warning("Target Type not supported");
            //return false;
        }

        TriggerGeneric.ComparisonType comparisonType = trigger.getComparison();
        if (comparisonType != null && comparisonType != TriggerGeneric.ComparisonType.NONE) {
            result = compare(target, comparisonType, value);
        }

        return result;
    }

    protected void doAction(TriggerActionData trigger) {
        System.out.println(String.format("\t Trigger Action: %s", trigger)); // TODO remove this line

        TriggerAction.ActionType type = trigger.getType();
        switch (type) {
            case CREATE_CREATURE:
                break;
            case DISPLAY_OBJECTIVE:
                break;
            case MAKE:
                TriggerAction.MakeType flag = ConversionUtils.parseEnum((Short) trigger.getUserData("type"), TriggerAction.MakeType.class);
                boolean available = (Short) trigger.getUserData("available") != 0;
                short playerId = (Short) trigger.getUserData("playerId");

                switch (flag) {
                    case CREATURE:
                        break;
                    case DOOR:
                        break;
                    case KEEPER_SPELL:
                        break;
                    case ROOM:
                        break;
                    case TRAP:
                        break;
                }
                break;

            case FLAG:
                short flagId = (Short) trigger.getUserData("flagId");
                if (flagId == 128) {
                    EnumSet<FlagTargetValueActionType> flagType = ConversionUtils.parseFlagValue((Short) trigger.getUserData("flag"), FlagTargetValueActionType.class);
                    if (flagType.contains(FlagTargetValueActionType.EQUAL)) {
                        GameState.setGameScore((int) trigger.getUserData("value"));
                    } else if (flagType.contains(FlagTargetValueActionType.PLUS)) {
                        GameState.setGameScore(GameState.getGameScore() + (int) trigger.getUserData("value"));
                    } else if (flagType.contains(FlagTargetValueActionType.MINUS)) {
                        GameState.setGameScore(GameState.getGameScore() - (int) trigger.getUserData("value"));
                    }
                } else {
                    stateManager.getState(GameState.class).setFlag(flagId, (int) trigger.getUserData("value"));
                }
                break;
            case INITIALIZE_TIMER:
                short timerId = (Short) trigger.getUserData("timerId");
                if (timerId == 16) {
                    GameState.setTimeLimit((int) trigger.getUserData("value"));
                } else {
                    stateManager.getState(GameState.class).getTimer((Short) trigger.getUserData("timerId")).setActive(true);
                }
                break;
            case FLASH_BUTTON:
                break;
            case WIN_GAME:
                break;
            case LOSE_GAME:
                break;
            case CREATE_HERO_PARTY:
                break;
            case SET_OBJECTIVE:
                break;
            case FLASH_ACTION_POINT:
                WorldState world = stateManager.getState(WorldState.class);
                ActionPoint ap = getActionPoint((Short) trigger.getUserData("actionPointId"));
                int time = (Integer) trigger.getUserData("value");
                boolean enable = (Short) trigger.getUserData("available") != 0;
                for (int x = (int) ap.getStart().x; x <= (int) ap.getEnd().x; x++) {
                    for (int y = (int) ap.getStart().y; y <= (int) ap.getEnd().y; y++) {
                        world.flashTile(x, y, time, enable);
                    }
                }
                break;

            case REVEAL_ACTION_POINT:
                // remove fog of war from tiles in action point
                // or
                // add fog of war to tiles in action point
                break;
            case SET_ALLIANCE:
                break;
            case ATTACH_PORTAL_GEM:
                break;
            case ALTER_TERRAIN_TYPE:
                Vector2f pos = new Vector2f((int) trigger.getUserData("posX"), (int) trigger.getUserData("posY"));
                short terrainId = (Short) trigger.getUserData("terrainId");
                playerId = (Short) trigger.getUserData("playerId");
                break;

            case PLAY_SPEECH:
                // TODO Sound State
                int speechId = (int) trigger.getUserData("speechId");
                String file = String.format("Sounds/speech_%s/lvlspe%02d.mp2",
                        stateManager.getState(GameState.class).getLevel().toLowerCase(), speechId);
                AudioNode speech = new AudioNode(app.getAssetManager(), file, false);
                speech.setName("speech");
                speech.setLooping(false);
                speech.setPositional(false);
                speech.play();
                app.getRootNode().attachChild(speech);
                break;

            case DISPLAY_TEXT_MESSAGE:
                int textId = (int) trigger.getUserData("textId");
                break;

            case ZOOM_TO_ACTION_POINT:
                PlayerCameraState pcs = stateManager.getState(PlayerCameraState.class);
                ap = getActionPoint((Short) trigger.getUserData("targetId"));
                ap.addControl(new PlayerCameraControl(pcs.getCamera()));
                break;

            case ROTATE_AROUND_ACTION_POINT:
                ap = getActionPoint((Short) trigger.getUserData("targetId"));
                boolean relative = (Short) trigger.getUserData("available") == 0;
                int angle = (Integer) trigger.getUserData("angle");
                time = (Integer) trigger.getUserData("time");

                PlayerCameraState ps = stateManager.getState(PlayerCameraState.class);
                ps.setCameraLookAt(ap);
                ps.addRotation(angle * FastMath.DEG_TO_RAD, time);
                break;

            case GENERATE_CREATURE:
                break;
            case SHOW_HEALTH_FLOWER:
                break;
            case FOLLOW_CAMERA_PATH:
                // TODO disable control
                //GameState.setEnabled(false);
                pcs = stateManager.getState(PlayerCameraState.class);
                ap = getActionPoint((Short) trigger.getUserData("actionPointId"));
                pcs.doTransition((Short) trigger.getUserData("pathId"), ap);
                break;

            case COLLAPSE_HERO_GATE:
                break;
            case SET_PORTAL_STATUS:
                break;
            case SET_WIDESCREEN_MODE:
                boolean state = (Short) trigger.getUserData("available") != 0;
                if (state) {
                    app.getNifty().getNifty().gotoScreen(PlayerState.CINEMATIC_SCREEN_ID);
                } else {
                    app.getNifty().getNifty().gotoScreen(PlayerState.HUD_SCREEN_ID);
                }
                break;

            case MAKE_OBJECTIVE:
                break;
            case ZOOM_TO:
                break;
            case SET_CREATURE_MOODS:
                break;
            case SET_SYSTEM_MESSAGES:
                break;
            case DISPLAY_SLAB_OWNER:
                break;
            case DISPLAY_NEXT_ROOM_TYPE:
                break;
            case CHANGE_ROOM_OWNER:
                break;
            case SET_SLAPS_LIMIT:
                break;
            case SET_TIMER_SPEECH:
                break;
            default:
                logger.warning("Action not supported");
                break;
        }
    }

    protected ActionPoint getActionPoint(int id) {
        return stateManager.getState(ActionPointState.class).getActionPoint(id);
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
}
