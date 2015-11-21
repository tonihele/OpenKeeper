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
        DISPLAY_OBJECTIVE(4),
        MAKE(6), // Or trap??? Or keeper spell ???
        FLAG(7),
        INITIALIZE_TIMER(8),
        FLASH_BUTTON(9),
        WIN_GAME(10),
        LOSE_GAME(11),
        CREATE_HERO_PARTY(14),
        UNKNOWN_15(15),
        FLASH_ACTION_POINT(16),
        REVEAL_ACTION_POINT(17),
        SET_ALLIANCE(18),
        UNKNOWN_20(20),
        ALTER_ROOM_TYPE(22),
        PLAY_SPEECH(24),
        DISPLAY_TEXT_MESSAGE(25),
        ZOOM_TO_ACTION_POINT(26),
        ROTATE_AROUND_ACTION_POINT(27),
        GENERATE_CREATURE(28),
        UNKNOWN_30(30), // FIXME
        CAMERA_FOLLOW_PATH(31),
        COLLAPSE_HERO_GATE(32),
        SET_PORTAL_STATUS(37),
        SET_WIDESCREEN_MODE(38),
        MAKE_OBJECTIVE(42),
        UNKNOWN_43(43), // FIXME
        SET_CREATURE_MOODS(44),
        SET_SYSTEM_MESSAGES(45),
        DISPLAY_SLAB_OWNER(46),
        DISPLAY_NEXT_ROOM_TYPE(47),
        CHANGE_ROOM_OWNER(49),
        SET_SLAPS_LIMIT(50),
        SET_TIMER_SPEECH(51),
        UNKNOWN_257(257),
        UNKNOWN_258(258),
        UNKNOWN_260(260),
        UNKNOWN_262(262),
        UNKNOWN_263(263),
        UNKNOWN_264(264),
        UNKNOWN_265(265),
        UNKNOWN_266(266),
        UNKNOWN_267(267),
        UNKNOWN_270(270),
        UNKNOWN_271(271),
        UNKNOWN_272(272),
        UNKNOWN_273(273),
        UNKNOWN_274(274),
        UNKNOWN_276(276),
        UNKNOWN_278(278),
        UNKNOWN_279(279),
        UNKNOWN_280(280),
        UNKNOWN_282(282),
        UNKNOWN_283(283),
        UNKNOWN_284(284),
        UNKNOWN_286(286),
        UNKNOWN_287(287),
        UNKNOWN_288(288),
        UNKNOWN_289(289),
        UNKNOWN_290(290),
        UNKNOWN_291(291),
        UNKNOWN_293(293),
        UNKNOWN_294(294),
        UNKNOWN_298(298),
        UNKNOWN_299(299),
        UNKNOWN_300(300),
        UNKNOWN_301(301),
        UNKNOWN_303(303), // MPD (Not in 6)
        UNKNOWN_305(305),
        UNKNOWN_306(306),
        UNKNOWN_307(307);

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

    public enum MakeType implements IValueEnum {

        ROOM(1),
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
        TARGET(0x002, null), // Use flag
        EQUAL(0x008, "="),
        PLUS(0x010, "+"),
        MINUS(0x020, "-");

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
    private short actionTargetId; // For creatures, creature ID; create hero party, hero party ID; for flags, flag # + 1
    private short playerId; // For flags, this has a special meaning
    private short creatureLevel;
    private short available; // TODO: for Make set Available = 1 or Unavailable = 0. Maybe boolean ? <-- depends, can be also 41, 42 (Create creature) etc
    private int actionTargetValue1; // Short, at least with creatures this is x coordinate, also seems to be the ID of the action point for hero party, with flags this is the value
    private int actionTargetValue2; // Short, at least with creatures y coordinate
    private short[] unknown1; // 2
    private ActionType actionType; // Short, probably just a byte...

    public TriggerAction(KwdFile kwdFile) {
        super(kwdFile);
    }

    public short getActionTargetId() {
        return actionTargetId;
    }

    protected void setActionTargetId(short actionTargetId) {
        this.actionTargetId = actionTargetId;
    }

    public short getPlayerId() {
        return playerId;
    }

    protected void setPlayerId(short playerId) {
        this.playerId = playerId;
    }

    public short getCreatureLevel() {
        return creatureLevel;
    }

    protected void setCreatureLevel(short creatureLevel) {
        this.creatureLevel = creatureLevel;
    }

    public short getAvailable() {
        return available;
    }

    protected void setAvailable(short available) {
        this.available = available;
    }

    public int getActionTargetValue1() {
        return actionTargetValue1;
    }

    protected void setActionTargetValue1(int actionTargetValue1) {
        this.actionTargetValue1 = actionTargetValue1;
    }

    public int getActionTargetValue2() {
        return actionTargetValue2;
    }

    protected void setActionTargetValue2(int actionTargetValue2) {
        this.actionTargetValue2 = actionTargetValue2;
    }

    public short[] getUnknown1() {
        return unknown1;
    }

    protected void setUnknown1(short[] unknown1) {
        this.unknown1 = unknown1;
    }

    public ActionType getActionType() {
        return actionType;
    }

    protected void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    /**
     * For flags, the player ID serves another purpose, it means how the flag
     * value is processed
     *
     * @return the action to peform on this flag with the value
     */
    private EnumSet<FlagTargetValueActionType> getFlagTargetValueActionTypes() {
        return ConversionUtils.parseFlagValue(playerId, FlagTargetValueActionType.class);
    }

    @Override
    public boolean hasChildren() {
        return false; // We never have
    }

    @Override
    public int getIdChild() {
        return 0;
    }

    @Override
    public String toString() {
        String result = actionType.toString();
        if (actionType == ActionType.CREATE_CREATURE) {
            result += " " + kwdFile.getCreature(actionTargetId) + " [" + creatureLevel + "] [" + (actionTargetValue1 + 1) + "," + (actionTargetValue2 + 1) + "] [" + kwdFile.getPlayer(playerId) + "]";
        } else if (actionType == ActionType.CREATE_HERO_PARTY) {
            result += " " + (actionTargetId + 1) + " at AP " + actionTargetValue1;
        } else if (actionType == ActionType.FLAG) {
            EnumSet<FlagTargetValueActionType> flagTargetValueActionTypes = getFlagTargetValueActionTypes();
            String operation = null;
            for (FlagTargetValueActionType type : flagTargetValueActionTypes) {
                operation = type.toString(); // Not very elegant
                if (operation != null) {
                    break;
                }
            }
            result += " " + (actionTargetId + 1) + " = " + (flagTargetValueActionTypes.contains(FlagTargetValueActionType.EQUAL) ? "" : actionType.toString() + " " + (actionTargetId + 1) + " " + operation + " ") + (flagTargetValueActionTypes.contains(FlagTargetValueActionType.VALUE) ? actionTargetValue1 : actionType.toString() + " " + (actionTargetValue1 + 1));
        } else if (actionType == ActionType.INITIALIZE_TIMER) {
            result += " " + (actionTargetId + 1);
        } else if (actionType == ActionType.MAKE) {

            // TODO: Not very elegant
            if (playerId == MakeType.ROOM.getValue()) {
                result += " " + MakeType.ROOM + " [" + kwdFile.getRoomById(creatureLevel).getName() + "]";
            } else if (playerId == MakeType.DOOR.getValue()) {
                result += " " + MakeType.DOOR + " [" + kwdFile.getDoorById(creatureLevel).getName() + "]";
            } else if (playerId == MakeType.TRAP.getValue()) {
                result += " " + MakeType.TRAP + " [" + kwdFile.getTrapById(creatureLevel).getName() + "]";
            } else {
                result += " " + MakeType.KEEPER_SPELL + " [" + kwdFile.getKeeperSpellById(creatureLevel).getName() + "]";
            }
            result += " " + (available == 1 ? "Available" : "Unavailable") + " To " + kwdFile.getPlayer((short) actionTargetId).getName();
        }
        return result;
    }
}
