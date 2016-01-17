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

import toniarts.openkeeper.tools.convert.IValueEnum;

/**
 * This the actual trigger, all TriggerActions need to be owned (triggered) by
 * these
 *
 * @see TriggerAction
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TriggerGeneric extends Trigger {

    public enum ComparisonType implements IValueEnum {

        NONE(0, null),
        LESS_THAN(1, "<"),
        LESS_OR_EQUAL_TO(2, "\u2264"),
        EQUAL_TO(3, "="),
        GREATER_THAN(4, ">"),
        GREATER_OR_EQUAL_TO(5, "\u2265"),
        NOT_EQUAL_TO(6, "!=");

        private ComparisonType(int id, String description) {
            this.id = id;
            this.description = description;
        }

        @Override
        public int getValue() {
            return id;
        }

        @Override
        public String toString() {
            return description;
        }
        private final int id;
        private final String description;
    }

    public enum TargetType implements IValueEnum {

        NONE(0),
        FLAG(1),
        TIMER(2),
        CREATURE_CREATED(3), // Event, This creature is
        CREATURE_KILLED(4), // Event, This creature is
        CREATURE_SLAPPED(5), // Event, This creature is
        CREATURE_ATTACKED(6), // Event, This creature is
        CREATURE_IMPRISONED(7), // Event, This creature is
        CREATURE_TORTURED(8), // Event, This creature is
        CREATURE_CONVERTED(9), // Event, This creature is
        CREATURE_CLAIMED(10), // Event, This creature is
        CREATURE_ANGRY(11), // Event, This creature is
        CREATURE_AFRAID(12), // Event, This creature is
        CREATURE_STEALS(13), // Event, This creature is
        CREATURE_LEAVES(14), // Event, This creature is
        CREATURE_STUNNED(15), // Event, This creature is
        CREATURE_DYING(16), // Event, This creature is
        PLAYER_CREATURES(19), // Player
        PLAYER_HAPPY_CREATURES(20), // Player
        PLAYER_ANGRY_CREATURES(21), // Player
        PLAYER_CREATURES_KILLED(22), // Player
        PLAYER_KILLS_CREATURES(23), // Player
        PLAYER_ROOM_SLAPS(24), // Player
        PLAYER_ROOMS(25), // Player
        PLAYER_ROOM_SIZE(26), // Player
        PLAYER_DOORS(27), // Player
        PLAYER_TRAPS(28), // Player
        PLAYER_KEEPER_SPELL(29), // Player
        PLAYER_GOLD(30), // Player
        PLAYER_GOLD_MINED(31), // Player
        PLAYER_MANA(32), // Player
        PLAYER_DESTROYS(33), // Player
        LEVEL_TIME(34), // Attribute, This level's...
        LEVEL_CREATURES(35), // Attribute, This level's...
        CREATURE_HEALTH(36), // Attribute, This creature's...
        CREATURE_GOLD_HELD(37), // Attribute, This creature's...
        AP_CONGREGATE_IN(39), // Action point
        AP_CLAIM_PART_OF(40), // Action point
        AP_CLAIM_ALL_OF(41), // Action point
        AP_SLAP_TYPES(42), // Action point
        PARTY_CREATED(43), // Party
        PARTY_MEMBERS_KILLED(44), // Party
        PARTY_MEMBERS_CAPTURED(45), // Party
        LEVEL_PAY_DAY(46), // Event, This level's...
        PLAYER_KILLED(47), // Event, This player's...
        CREATURE_EXPERIENCE_LEVEL(49), // Attribute, This creature's...
        PLAYER_CREATURES_AT_LEVEL(52), // Player
        GUI_BUTTON_PRESSED(53), // GUI
        CREATURE_HUNGER_SATED(54), // Event, This creature is
        CREATURE_PICKS_UP_PORTAL_GEM(55), // Event, This creature is
        PLAYER_DUNGEON_BREACHED(56), // Event, This player's...
        PLAYER_ENEMY_BREACHED(57), // Event, This player's...
        PLAYER_CREATURE_PICKED_UP(58), // Event, This player's...
        PLAYER_CREATURE_DROPPED(59), // Event, This player's...
        PLAYER_CREATURE_SLAPPED(60), // Event, This player's...
        PLAYER_CREATURE_SACKED(61), // Event, This player's...
        AP_TAG_PART_OF(62), // Action point
        CREATURE_SACKED(63), // Event, This creature is
        PARTY_MEMBERS_INCAPACITATED(64), // Party
        CREATURE_PICKED_UP(65), // Event, This creature is
        LEVEL_PLAYED(67), // Attribute, This level's...
        PLAYER_ROOM_FURNITURE(68), // Player
        AP_TAG_ALL_OF(69), // Action point
        AP_POSESSED_CREATURE_ENTERS(70), // Action point
        PLAYER_SLAPS(71), // Player
        GUI_TRANSITION_ENDS(72), // GUI
        PLAYER_CREATURES_GROUPED(73), // Player
        PLAYER_CREATURES_DYING(74);

        // Player
        private TargetType(int id) {
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

    public enum TargetValueType implements IValueEnum {
        // TODO maybe delete section or refactor ?
        TERRAIN_ID(-1), // Slab types point to terrain ID
        VALUE(0), // Use the value
        VALUE1(1), // This isn't quite right, but this means use the value
        FLAG(2),
        UNKNOWN_5(5),
        UNKNOWN_6(6),
        BLUEPRINT(9),
        UNKNOWN_11(11),
        UNKNOWN_12(12),
        UNKNOWN_13(13),
        UNKNOWN_14(14),
        UNKNOWN_15(15),
        UNKNOWN_16(16),
        BUILT(17),
        UNKNOWN_18(18),
        UNKNOWN_21(21),
        UNKNOWN_22(22),
        UNKNOWN_29(29),
        UNKNOWN_31(31);

        // As in flag value
        private TargetValueType(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }
    //    struct TriggerBlock {
    //        int x00;
    //        int x04;
    //        uint16_t x08;
    //        uint16_t x0a;
    //        uint16_t x0c;
    //        uint8_t x0e;
    //        uint8_t x0f;
    //        };
    private ComparisonType targetValueComparison; // Target comparison type
    private short targetFlagId;
    private TargetValueType targetValueType; // targetValueComparison is just byte for sure
    private short targetValueFlagId;
    private int targetValue; // Target value
    private TargetType target;
    private short terrainId; // Slab types have the targetValueType as terrain ID

    public TriggerGeneric(KwdFile kwdFile) {
        super(kwdFile);
    }

    public ComparisonType getTargetValueComparison() {
        return targetValueComparison;
    }

    protected void setTargetValueComparison(ComparisonType targetValueComparison) {
        this.targetValueComparison = targetValueComparison;
    }

    public short getTargetFlagId() {
        return targetFlagId;
    }

    protected void setTargetFlagId(short targetFlagId) {
        this.targetFlagId = targetFlagId;
    }

    public TargetValueType getTargetValueType() {
        return targetValueType;
    }

    protected void setTargetValueType(TargetValueType targetValueType) {
        this.targetValueType = targetValueType;
    }

    public short getTargetValueFlagId() {
        return targetValueFlagId;
    }

    protected void setTargetValueFlagId(short targetValueFlagId) {
        this.targetValueFlagId = targetValueFlagId;
    }

    public int getTargetValue() {
        return targetValue;
    }

    protected void setTargetValue(int targetValue) {
        this.targetValue = targetValue;
    }

    public TargetType getType() {
        return target;
    }

    protected void setType(TargetType target) {
        this.target = target;
    }

    public short getTerrainId() {
        return terrainId;
    }

    protected void setTerrainId(short terrainId) {
        this.terrainId = terrainId;
    }

    private String getTargetValueString() {
        StringBuilder buf = new StringBuilder();
        switch (targetValueType) {
            case FLAG: {
                buf.append(target).append(" ").append(targetValueFlagId + 1);
                break;
            }
            case TERRAIN_ID: {
                Terrain terrain = kwdFile.getTerrain(terrainId);
                buf.append(targetValue).append(" ").append(terrain);
                if (targetFlagId > 0 && terrain != null && terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
                    buf.append(" [").append(kwdFile.getPlayer(targetFlagId)).append("]");
                }
                break;
            }
            default: {
                buf.append(targetValue);
                break;
            }
        }
        return buf.toString();
    }

    public boolean hasChildren() {
        return (getIdChild() != 0);
    }

    @Override
    public String toString() {
        return "When " + target + (target == TargetType.FLAG || target == TargetType.TIMER ? " " + (targetFlagId + 1) : "") + (targetValueComparison != ComparisonType.NONE ? " " + targetValueComparison + " " + getTargetValueString() : "");
    }
}
