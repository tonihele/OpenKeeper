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
package toniarts.openkeeper.game.state;

import com.jme3.app.state.AbstractAppState;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Trigger;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

/**
 *
 * @author ArchDemon
 */

public abstract class TriggerState extends AbstractAppState {
    private int triggerId;
    private int triggerStartId;
    private final KwdFile kwdFile;
    private Map<Integer, Trigger> triggers;
    private static final Logger logger = Logger.getLogger(TriggerState.class.getName());

    public TriggerState(KwdFile level) {
        kwdFile = level;
        triggers = kwdFile.getTriggers();
        triggerStartId = kwdFile.getPlayer((short) 3).getTriggerId();
        //this.triggerStartId = kwdFile.getGameLevel().getTriggerId();
        this.triggerId = triggerStartId;
    }

    @Override
    public void update(float tpf) {

        Trigger value = triggers.get(triggerId);

        checkTrigger(value, false, value.isClosed());

        if (value.hasNext()) {
            triggerId = value.getIdNext();
        } else {
            triggerId = triggerStartId;
        }
        super.update(tpf);
    }

    private void checkTrigger(Trigger value, boolean next, boolean closed) {

        if (value instanceof TriggerGeneric) {

            TriggerGeneric trigger = (TriggerGeneric) value;

            if (isActive(trigger)) {
                System.out.println(String.format("Trigger Generic: %s at %s", value, GameState.getGameTime()));

                if (value.hasChildren()) {
                    int childId = value.getIdChild();
                    checkTrigger(triggers.get(childId), true, value.isClosed());
                }

                value.subRepeatTimes();
            }

        } else if (value instanceof TriggerAction) {

            if (!closed) {
                TriggerAction trigger = (TriggerAction) value;
                doAction(trigger);
            }

        } else {
            logger.log(Level.SEVERE, "Unkwown trigger class {0}", value);
            // throw new RuntimeException("Unkwown trigger class");
            return;
        }

        if (value.hasNext() && next) {
            int nextId = value.getIdNext();
            checkTrigger(triggers.get(nextId), true, closed);
        }
    }

    private boolean isActive(TriggerGeneric trigger) {
        TriggerGeneric.TargetType targetType = trigger.getType();
        float target;
        boolean result = false;

        switch (targetType) {
            case FLAG:
                target = GameState.getFlag(trigger.getTargetFlagId());
                break;
            case TIMER:
                target = GameState.getTimer(trigger.getTargetFlagId()).getTime();
                break;
            case CREATURE_CREATED:
                return false;
            case CREATURE_KILLED:
                return false;
            case CREATURE_SLAPPED:
                return false;
            case CREATURE_ATTACKED:
                return false;
            case CREATURE_IMPRISONED:
                return false;
            case CREATURE_TORTURED:
                return false;
            case CREATURE_CONVERTED:
                return false;
            case CREATURE_CLAIMED:
                return false;
            case CREATURE_ANGRY:
                return false;
            case CREATURE_AFRAID:
                return false;
            case CREATURE_STEALS:
                return false;
            case CREATURE_LEAVES:
                return false;
            case CREATURE_STUNNED:
                return false;
            case CREATURE_DYING:
                return false;
            case PLAYER_CREATURES:
                return false;
            case PLAYER_HAPPY_CREATURES:
                return false;
            case PLAYER_ANGRY_CREATURES:
                return false;
            case PLAYER_CREATURES_KILLED:
                return false;
            case PLAYER_KILLS_CREATURES:
                return false;
            case PLAYER_ROOM_SLAPS:
                return false;
            case PLAYER_ROOMS:
                return false;
            case PLAYER_ROOM_SIZE:
                return false;
            case PLAYER_DOORS:
                return false;
            case PLAYER_TRAPS:
                return false;
            case PLAYER_KEEPER_SPELL:
                return false;
            case PLAYER_GOLD:
                return false;
            case PLAYER_GOLD_MINED:
                return false;
            case PLAYER_MANA:
                return false;
            case PLAYER_DESTROYS:
                return false;
            case LEVEL_TIME:
                target = GameState.getGameTime();
                break;
            case LEVEL_CREATURES:
                return false;
            case CREATURE_HEALTH:
                return false;
            case CREATURE_GOLD_HELD:
                return false;
            case AP_CONGREGATE_IN:
                return false;
            case AP_CLAIM_PART_OF:
                return false;
            case AP_CLAIM_ALL_OF:
                return false;
            case AP_SLAP_TYPES:
                return false;
            case PARTY_CREATED:
                return false;
            case PARTY_MEMBERS_KILLED:
                return false;
            case PARTY_MEMBERS_CAPTURED:
                return false;
            case LEVEL_PAY_DAY:
                return false;
            case PLAYER_KILLED:
                return false;
            case CREATURE_EXPERIENCE_LEVEL:
                return false;
            case PLAYER_CREATURES_AT_LEVEL:
                return false;
            case GUI_BUTTON_PRESSED:
                return false;
            case CREATURE_HUNGER_SATED:
                return false;
            case CREATURE_PICKS_UP_PORTAL_GEM:
                return false;
            case PLAYER_DUNGEON_BREACHED:
                return false;
            case PLAYER_ENEMY_BREACHED:
                return false;
            case PLAYER_CREATURE_PICKED_UP:
                return false;
            case PLAYER_CREATURE_DROPPED:
                return false;
            case PLAYER_CREATURE_SLAPPED:
                return false;
            case PLAYER_CREATURE_SACKED:
                return false;
            case AP_TAG_PART_OF:
                return false;
            case CREATURE_SACKED:
                return false;
            case PARTY_MEMBERS_INCAPACITATED:
                return false;
            case CREATURE_PICKED_UP:
                return false;
            case LEVEL_PLAYED:
                return false;
            case PLAYER_ROOM_FURNITURE:
                return false;
            case AP_TAG_ALL_OF:
                return false;
            case AP_POSESSED_CREATURE_ENTERS:
                return false;
            case PLAYER_SLAPS:
                return false;
            case GUI_TRANSITION_ENDS:
                return !GameState.getTransition();
            case PLAYER_CREATURES_GROUPED:
                return false;
            case PLAYER_CREATURES_DYING:
                return false;
            default:
                logger.warning("Target Type not supported");
                return false;
        }


        TriggerGeneric.ComparisonType comparisonType = trigger.getTargetValueComparison();
        if (comparisonType != null) {
            result = compare(target, comparisonType, (int) trigger.getUserData("value"));
        }

        return result;
    }

    private void doAction(TriggerAction trigger) {
        System.out.println(String.format("\t Trigger Action: %s", trigger)); // TODO remove this line
        TriggerAction.ActionType type = trigger.getType();

        switch (type) {
            case CREATE_CREATURE:
                break;
            case DISPLAY_OBJECTIVE:
                break;
            case MAKE:
                break;
            case FLAG:
                // TODO make not only value and gameScore
                GameState.setFlag((int) trigger.getUserData("flagId"), (int) trigger.getUserData("value"));
                break;
            case INITIALIZE_TIMER:
                // TODO make time limit
                GameState.getTimer((int) trigger.getUserData("timerId")).setActive(true);
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
                Thing.ActionPoint ap = getActionPoint((int) trigger.getUserData("actionPointId"));
                onFlashActionPoint(ap, (int) trigger.getUserData("available") != 0);
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
                break;
            case PLAY_SPEECH:
                onPlaySpeech((int) trigger.getUserData("speechId"));
                break;
            case DISPLAY_TEXT_MESSAGE:
                break;
            case ZOOM_TO_ACTION_POINT:
                ap = getActionPoint((int) trigger.getUserData("targetId"));
                onZoomToActionPoint(ap);
                break;
            case ROTATE_AROUND_ACTION_POINT:
                break;
            case GENERATE_CREATURE:
                break;
            case SHOW_HEALTH_FLOWER:
                break;
            case FOLLOW_CAMERA_PATH:
                ap = getActionPoint((int) trigger.getUserData("actionPointId"));
                onCameraFollow((int) trigger.getUserData("pathId"), ap);
                break;
            case COLLAPSE_HERO_GATE:
                break;
            case SET_PORTAL_STATUS:
                break;
            case SET_WIDESCREEN_MODE:
                onWideScreenMode((int) trigger.getUserData("available") != 0);
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

    private Thing.ActionPoint getActionPoint(int id) {
        for (Thing thing : kwdFile.getThings()) {
            if (thing instanceof Thing.ActionPoint
                    && ((Thing.ActionPoint) thing).getId() == id) {
                return (Thing.ActionPoint) thing;
            }
        }
        return null;
    }

    private boolean compare(float target, TriggerGeneric.ComparisonType compare, float value) {
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

    public abstract void onWideScreenMode(boolean state);

    public abstract void onCameraFollow(int path, Thing.ActionPoint point);

    public abstract void onFlashActionPoint(Thing.ActionPoint point, boolean state);

    public abstract void onZoomToActionPoint(Thing.ActionPoint point);

    public abstract void onPlaySpeech(int id);
}
