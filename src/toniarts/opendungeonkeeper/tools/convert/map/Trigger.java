/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

/**
 * Container class for *Triggers.kld
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class Trigger {

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
            TIME(34), // Attribute, This level's...
            CREATURES(35), // Attribute, This level's...
            HEALTH(36), // Attribute, This creature's...
            GOLD_HELD(37), // Attribute, This creature's...
            PAY_DAY(46), // Event, This level's...
            EXPERIENCE_LEVEL(49), // Attribute, This creature's...
            HUNGER_SATED(54), // Event, This creature is
            PICKS_UP_PORTAL_GEM(55), // Event, This creature is
            SACKED(63), // Event, This creature is
            PICKED_UP(65), // Event, This creature is
            PLAYED(67); // Attribute, This level's...

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
        private int targetValue; // Target value
        private int id; // I assume so
        private int x0a;
        private int x0c;
        private TargetType target;
        private short repeatTimes; // Repeat x times, 255 = always

        public ComparisonType getTargetValueComparison() {
            return targetValueComparison;
        }

        protected void setTargetValueComparison(ComparisonType targetValueComparison) {
            this.targetValueComparison = targetValueComparison;
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
            return "When " + target + (targetValueComparison != ComparisonType.NONE ? " " + targetValueComparison + " " + targetValue : "");
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

        private short unknown1[]; // 16

        public short[] getUnknown1() {
            return unknown1;
        }

        protected void setUnknown1(short[] unknown1) {
            this.unknown1 = unknown1;
        }
    }
}
