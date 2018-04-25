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
package toniarts.openkeeper.tools.convert.map;

import java.util.EnumSet;
import toniarts.openkeeper.tools.convert.IFlagEnum;
import toniarts.openkeeper.tools.convert.IValueEnum;
import toniarts.openkeeper.tools.convert.ConversionUtils;

/**
 * Actions executed by the actual triggers
 *
 * @see TriggerGeneric
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TriggerAction extends Trigger {

    public enum ActionType implements IValueEnum {

        CREATE_CREATURE(1),
        CREATE_PORTAL_GEM(2), // text like [ X, Y ]
        SEND_TO_AP(3), // or only sent
        DISPLAY_OBJECTIVE(4), // text like X [ Zoom To AP Y ]
        INFORMATION(5), // text like X [ Zoom To AP Y ]
        MAKE(6),
        FLAG(7),
        INITIALIZE_TIMER(8),
        FLASH_BUTTON(9),
        WIN_GAME(10),
        LOSE_GAME(11),
        WIN_SUBOBJECTIVE(12),
        LOSE_SUBOBJECTIVE(13),
        CREATE_HERO_PARTY(14),
        SET_OBJECTIVE(15),
        FLASH_ACTION_POINT(16),
        REVEAL_ACTION_POINT(17),
        SET_ALLIANCE(18), // CREATE_ALLIANCE
        ATTACH_PORTAL_GEM(20),
        FORCE_FIRST_PERSON(21),
        ALTER_TERRAIN_TYPE(22),
        SET_TIME_LIMIT(23), // text like : X Seconds
        PLAY_SPEECH(24),
        DISPLAY_TEXT_STRING(25),
        ZOOM_TO_ACTION_POINT(26),
        ROTATE_AROUND_ACTION_POINT(27),
        GENERATE_CREATURE(28),
        MAKE_HUNGRY(29),
        SHOW_HEALTH_FLOWER(30),
        FOLLOW_CAMERA_PATH(31),
        COLLAPSE_HERO_GATE(32),
        ALTER_SPEED(33),
        SET_MUSIC_LEVEL(34), // text like X
        REMOVE_FROM_MAP(35),
        SET_FIGHT_FLAG(36),
        SET_PORTAL_STATUS(37),
        SET_WIDESCREEN_MODE(38),
        MAKE_OBJECTIVE(42),
        ZOOM_TO(43),
        SET_CREATURE_MOODS(44),
        SET_SYSTEM_MESSAGES(45),
        DISPLAY_SLAB_OWNER(46), // text like [ On | Off ] Of [ X, 1635980 ]
        DISPLAY_NEXT_ROOM_TYPE(47),
        TOGGLE_EFFECT_GENERATOR(48), // text like X [ On | Off ]
        CHANGE_ROOM_OWNER(49),
        SET_SLAPS_LIMIT(50),
        SET_TIMER_SPEECH(51);

        private ActionType(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }

        @Override
        public String toString() {
            String[] splitted = name().split("_");
            String result = "";
            for (String s : splitted) {
                result = result.concat(" ").concat(s.substring(0, 1).toUpperCase()).concat(s.substring(1).toLowerCase());
            }
            return result.trim();
        }
        private final int id;
    }

    public enum CreatureFlag implements IFlagEnum {

        WILL_FIGHT(0x001),
        LEADER(0x002), // FIXME maybe
        DIES_INSTANTLY(0x004),
        WILL_BE_ATTACKED(0x008),
        RETURN_TO_HERO_LAIR(0x010),
        FREE_FRIENDS_ON_JAIL_BREAK(0x020),
        ACT_AS_DROPPED(0x040),
        START_AS_DYING(0x080);
        private final long flagValue;

        private CreatureFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    };

    public enum MakeType implements IValueEnum {

        ROOM(1),
        CREATURE(2),
        DOOR(3),
        TRAP(4),
        KEEPER_SPELL(5);

        private MakeType(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }

        @Override
        public String toString() {
            String[] splitted = name().split("_");
            String result = "";
            for (String s : splitted) {
                result = result.concat(" ").concat(s.substring(0, 1).toUpperCase()).concat(s.substring(1).toLowerCase());
            }
            return result.trim();
        }

        private final int id;
    }

    public enum FlagTargetValueActionType implements IFlagEnum {

        VALUE(0x002, null), // Use value
        TARGET(0x004, null), // Use flag
        EQUAL(0x008, "="),
        PLUS(0x010, "+="),
        MINUS(0x020, "-=");

        private FlagTargetValueActionType(int flagValue, String description) {
            this.flagValue = flagValue;
            this.description = description;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }

        @Override
        public String toString() {
            return description;
        }
        private final int flagValue;
        private final String description;
    }

    private ActionType actionType;

    public TriggerAction(KwdFile kwdFile) {
        super(kwdFile);
    }

    public ActionType getType() {
        return actionType;
    }

    protected void setType(ActionType actionType) {
        this.actionType = actionType;
    }

    @Override
    public String toString() {
        String result = actionType.toString();

        switch(actionType) {
            case CREATE_CREATURE:
                result += " " + kwdFile.getCreature((Short) getUserData("creatureId"))
                        + " [" + getUserData("level") + "] [" + ((Short) getUserData("posX") + 1) + ","
                        + ((Short) getUserData("posY") + 1) + "] [" + kwdFile.getPlayer((Short) getUserData("playerId")) + "]";
                break;
            case CREATE_HERO_PARTY:
                result += " " + ((Short) getUserData("partyId") + 1) + " at AP " + getUserData("actionPointId");
                break;
            case FLAG:
                EnumSet<FlagTargetValueActionType> flagType = ConversionUtils.parseFlagValue((Short) getUserData("flag"), FlagTargetValueActionType.class);
                String operation = null;
                for (FlagTargetValueActionType t : flagType) {
                    operation = t.toString(); // Not very elegant
                    if (operation != null) {
                        break;
                    }
                }
                result += " " + ((Short) getUserData("flagId") + 1) + " " + operation + " "
                        + (flagType.contains(FlagTargetValueActionType.VALUE) ? (Integer) getUserData("value") : actionType
                        + " " + ((Integer) getUserData("value") + 1));
                break;
            case INITIALIZE_TIMER:
                result += " " + ((Short) getUserData("timerId") + 1);
                break;
            case SHOW_HEALTH_FLOWER:
                result += " [ " + getUserData("value") + " Seconds ]";
                break;
            case ALTER_SPEED:
                result += " [ " + ((Short) getUserData("available") == 0 ? "Walk" : "Run") + " ]";
                break;
            case SET_FIGHT_FLAG:
                result += " [ " + ((Short) getUserData("available") == 0 ? "Dont`t Fight" : "Fight") + " ]";
                break;
            case SET_PORTAL_STATUS:
                result += " [ " + ((Short) getUserData("available") == 0 ? "Closed" : "Open") + " ]";
                break;
            case SET_OBJECTIVE:
                result += " [ " + ConversionUtils.parseEnum((Short) getUserData("type"), Creature.JobType.class)
                        + " [ " + kwdFile.getPlayer((Short) getUserData("playerId")).getName() + " ]]";
                break;
            case CREATE_PORTAL_GEM:
                result += " [ " + ((Integer) getUserData("posX") + 1) + ", " + ((Integer) getUserData("posY") + 1) + " ]";
                break;
            case DISPLAY_OBJECTIVE:
            case INFORMATION:
            case SET_MUSIC_LEVEL:
                result += " " + getUserData("value");
                break;
            case TOGGLE_EFFECT_GENERATOR:
                result += " " + ((Short) getUserData("generatorId") + 1)
                        + " [ " + ((Short) getUserData("available") == 0 ? "Off" : "On") + " ]";
                break;
            case DISPLAY_SLAB_OWNER:
                result += " [ " + ((Short) getUserData("available") == 0 ? "Off" : "On") + " ] Of [  , 1635980 ]";
                break;
            case SET_TIME_LIMIT:
                result += ": " + getUserData("value") + " Seconds";
                break;
            case MAKE:
                MakeType type = ConversionUtils.parseEnum((Short) getUserData("type"), MakeType.class);
                result += " " + type;
                switch (type) {
                    case DOOR:
                        result += " [" + kwdFile.getDoorById((Short) getUserData("targetId")).getName() + "]";
                        break;
                    case ROOM:
                        result += " [" + kwdFile.getRoomById((Short) getUserData("targetId")).getName() + "]";
                        break;
                    case TRAP:
                        result += " [" + kwdFile.getTrapById((Short) getUserData("targetId")).getName() + "]";
                        break;
                    default:
                        result += " [" + kwdFile.getKeeperSpellById((Short) getUserData("targetId")).getName() + "]";
                        break;
                }
                result += " " + ((Short) getUserData("available") == 0 ? "Unavailable" : "Available")
                        + " To " + kwdFile.getPlayer((Short) getUserData("playerId")).getName();
                break;
        }

        return result;
    }
}
