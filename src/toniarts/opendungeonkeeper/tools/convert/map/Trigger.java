/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

import java.util.EnumSet;

/**
 * Container class for *Triggers.kld
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class Trigger {

    private static KwdFile kwdFile; // For to strings

    public Trigger(KwdFile kwdFile) {
        Trigger.kwdFile = kwdFile;
    }

    /**
     * This the actual trigger, all TriggerActions need to be owned (triggered)
     * by these
     */
    public static class TriggerGeneric extends Trigger {

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
            CREATED(3), // Event, This creature is
            KILLED(4), // Event, This creature is
            SLAPPED(5), // Event, This creature is
            ATTACKED(6), // Event, This creature is
            IMPRISONED(7), // Event, This creature is
            TORTURED(8), // Event, This creature is
            CONVERTED(9), // Event, This creature is
            CLAIMED(10), // Event, This creature is
            ANGRY(11), // Event, This creature is
            AFRAID(12), // Event, This creature is
            STEALS(13), // Event, This creature is
            LEAVES(14), // Event, This creature is
            STUNNED(15), // Event, This creature is
            DYING(16), // Event, This creature is
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
            TIME(34), // Attribute, This level's...
            CREATURES(35), // Attribute, This level's...
            HEALTH(36), // Attribute, This creature's...
            GOLD_HELD(37), // Attribute, This creature's...
            CONGREGATE_IN(39), // Action point
            CLAIM_PART_OF(40), // Action point
            CLAIM_ALL_OF(41), // Action point
            SLAP_TYPES(43), // Action point
            PARTY_CREATED(43), // Party
            MEMBERS_KILLED(44), // Party
            MEMBERS_CAPTURED(45), // Party
            PAY_DAY(46), // Event, This level's...
            PLAYER_KILLED(47), // Event, This player's...
            EXPERIENCE_LEVEL(49), // Attribute, This creature's...
            PLAYER_CREATURES_AT_LEVEL(52), // Player
            BUTTON_PRESSED(53), // GUI
            HUNGER_SATED(54), // Event, This creature is
            PICKS_UP_PORTAL_GEM(55), // Event, This creature is
            DUNGEON_BREACHED(56), // Event, This player's...
            ENEMY_BREACHED(57), // Event, This player's...
            CREATURE_PICKED_UP(58), // Event, This player's...
            CREATURE_DROPPED(59), // Event, This player's...
            CREATURE_SLAPPED(60), // Event, This player's...
            CREATURE_SACKED(61), // Event, This player's...
            TAG_PART_OF(62), // Action point
            SACKED(63), // Event, This creature is
            MEMBERS_INCAPACITATED(64), // Party
            PICKED_UP(65), // Event, This creature is
            PLAYED(67), // Attribute, This level's...
            PLAYER_ROOM_FURNITURE(68), // Player
            TAG_ALL_OF(69), // Action point
            POSESSED_CREATURE_ENTERS(70), // Action point
            PLAYER_SLAPS(71), // Player
            TRANSITION_ENDS(72), // GUI
            PLAYER_CREATURES_GROUPED(73), // Player
            PLAYER_CREATURES_DYING(74); // Player

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

            VALUE(0), // Use the value
            VALUE1(1), // This isn't quite right, but this means use the value
            FLAG(2); // As in flag value

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
        private int id; // I assume so
        private int x0a;
        private int x0c;
        private TargetType target;
        private short repeatTimes; // Repeat x times, 255 = always

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

        public int getId() {
            return id;
        }

        protected void setId(int id) {
            this.id = id;
        }

        public int getX0a() {
            return x0a;
        }

        protected void setX0a(int x0a) {
            this.x0a = x0a;
        }

        public int getX0c() {
            return x0c;
        }

        protected void setX0c(int x0c) {
            this.x0c = x0c;
        }

        public TargetType getTarget() {
            return target;
        }

        protected void setTarget(TargetType target) {
            this.target = target;
        }

        public short getRepeatTimes() {
            return repeatTimes;
        }

        protected void setRepeatTimes(short repeatTimes) {
            this.repeatTimes = repeatTimes;
        }

        @Override
        public String toString() {
            return "When " + target + (target == TargetType.FLAG || target == TargetType.TIMER ? " " + (targetFlagId + 1) : "") + (targetValueComparison != ComparisonType.NONE ? " " + targetValueComparison + " " + (targetValueType == TargetValueType.FLAG ? target + " " + (targetValueFlagId + 1) : targetValue) : "");
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + this.id;
            return hash;
        }

        @Override
        public boolean equals(java.lang.Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TriggerGeneric other = (TriggerGeneric) obj;
            if (this.id != other.id) {
                return false;
            }
            return true;
        }
    }

    public static class TriggerAction extends Trigger {

        public enum ActionType implements IValueEnum {

            MAKE_DOOR(0),
            CREATE_CREATURE(1),
            MAKE_ROOM(6), // Or trap??? Or keeper spell ???
            FLAG(7),
            INITIALIZE_TIMER(8),
            CREATE_HERO_PARTY(14),
            SET_ALLIANCE(18),
            ALTER(22),
            COLLAPSE_HERO_GATE(32),
            SET_CREATURE_MOODS(44),
            SET_SYSTEM_MESSAGES(45),
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
        private short unknown2; // Is this the type of the action? Nope, seen 41 & 43
        private int actionTargetValue1; // Short, at least with creatures this is x coordinate, also seems to be the ID of the action point for hero party, with flags this is the value
        private int actionTargetValue2; // Short, at least with creatures y coordinate
        private int flags1;
        private int flags2;
        private short unknown1[]; // 2
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

        public short getUnknown2() {
            return unknown2;
        }

        protected void setUnknown2(short unknown2) {
            this.unknown2 = unknown2;
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

        public int getFlags1() {
            return flags1;
        }

        protected void setFlags1(int flags1) {
            this.flags1 = flags1;
        }

        public int getFlags2() {
            return flags2;
        }

        protected void setFlags2(int flags2) {
            this.flags2 = flags2;
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
         * For flags, the player ID serves another purpose, it means how the
         * flag value is processed
         *
         * @return the action to peform on this flag with the value
         */
        private EnumSet<FlagTargetValueActionType> getFlagTargetValueActionTypes() {
            return KwdFile.parseFlagValue(playerId, FlagTargetValueActionType.class);
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
            }
            return result;
        }
    }
}
