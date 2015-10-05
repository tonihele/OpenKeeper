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
 * Container class for *Variables.kld
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @author ArchDemon
 */
public class Variable { //    struct VariableBlock {
//        int x00;
//        int x04;
//        int x08;
//        int x0c;
//        };

    public enum SacrificeType implements IValueEnum {

        NONE(0),
        CREATURE(1),
        OBJECT(2),
        KEEPER_SPELL(3);

        private SacrificeType(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    public enum SacrificeRewardType implements IValueEnum {

        NONE(0),
        CREATURE(1),
        OBJECT(2),
        GOLD(3), // TODO: not tested
        MANA(4), // TODO: not tested
        KEEPER_SPELL(5), // TODO: not tested
        CREATURES_HAPPY(6), // TODO: not tested
        CREATURES_UNHAPPY(7), // TODO: not tested
        CREATURES_ANGRY(8), // TODO: not tested
        EASTER_EGG(9); // TODO: not tested

        private SacrificeRewardType(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }
    // Variable IDs
    public final static int CREATURE_POOL = 1;
    public final static int AVAILABILITY = 2;
    public final static int UNKNOWN_2 = 17;
    // 1, 2, 17, 19, 29, 43, 44, 56 - 66, 69, 72, 74, 77, 80, 81, 89 - 92, 99,
    // 107 - 109, 117 - 124, 137, 153, 171, 172, 224
    public final static int SACRIFICES_ID = 43;
    public final static int CREATURE_STATS_ID = 65;
    public final static int CREATURE_FIRST_PERSON_ID = 224;
    public final static int LEVEL_RATING = 241;  // TODO: need value >> 12
    public final static int AVERAGE_TIME = 242;
    //

    public static class CreaturePool extends Variable {

        private int creatureId;
        private int value;
        private int playerId;

        public int getCreatureId() {
            return creatureId;
        }

        protected void setCreatureId(int creatureId) {
            this.creatureId = creatureId;
        }

        public int getValue() {
            return value;
        }

        protected void setValue(int value) {
            this.value = value;
        }

        public int getPlayerId() {
            return playerId;
        }

        protected void setPlayerId(int playerId) {
            this.playerId = playerId;
        }

        @Override
        public String toString() {
            return "CreaturePool{" + "creatureId=" + creatureId + ", value=" + value + ", playerId=" + playerId + '}';
        }
    }

    public static class Availability extends Variable {

        public enum AvailabilityType implements IValueEnum {

            ROOM(1),
            DOOR(4),
            TRAP(3),
            SPELL(5),
            CREATYRE(2);

            private AvailabilityType(int id) {
                this.id = id;
            }

            @Override
            public int getValue() {
                return id;
            }
            private final int id;
        }

        public enum AvailabilityValue implements IValueEnum {

            DISABLE(2),
            EMPTY(1),
            ENABLE(3),
            EMPTY_ROOM(4);

            private AvailabilityValue(int id) {
                this.id = id;
            }

            @Override
            public int getValue() {
                return id;
            }
            private final int id;
        }
        //public int variableId = AVAILABILITY;
        private AvailabilityType type;
        private int playerId;
        private AvailabilityValue value;
        private int typeId;

        public AvailabilityType getType() {
            return type;
        }

        protected void setType(AvailabilityType type) {
            this.type = type;
        }

        public int getPlayerId() {
            return playerId;
        }

        protected void setPlayerId(int playerId) {
            this.playerId = playerId;
        }

        public AvailabilityValue getValue() {
            return value;
        }

        protected void setValue(AvailabilityValue value) {
            this.value = value;
        }

        public int getTypeId() {
            return typeId;
        }

        protected void setTypeId(int typeId) {
            this.typeId = typeId;
        }

        @Override
        public String toString() {
            return "Availability{" + "type=" + type + ", playerId=" + playerId + ", typeId=" + typeId + ", value=" + value + '}';
        }
    }

    public static class CreatureStats extends Variable {
        //public int variableId = CREATURE_STATS_ID; // Variable Id always = 65

        private int statId; // Creature Stat. 0 - Height (Tiles), 1 - Health ...
        private int value; // Stat Increase Percentages For Level
        private int level;

        public int getStatId() {
            return statId;
        }

        protected void setStatId(int statId) {
            this.statId = statId;
        }

        public int getValue() {
            return value;
        }

        protected void setValue(int value) {
            this.value = value;
        }

        public int getLevel() {
            return level;
        }

        protected void setLevel(int level) {
            this.level = level;
        }

        @Override
        public String toString() {
            return "CreatureStats{" + "statId=" + statId + ", value=" + value + ", level=" + level + '}';
        }
    }

    public static class CreatureFirstPerson extends CreatureStats {

        @Override
        public String toString() {
            return "CreatureFirstPerson{" + "statId=" + getStatId() + ", value=" + getValue() + ", level=" + getLevel() + '}';
        }
    }

    /*
     * x00: 3 unknown: 25 x08: 0 x0c: 0 Entrance Generation Speed (Seconds)
     * x00: 4 unknown: 825 x08: 0 x0c: 0 Claim Tile Health
     * x00: 5 unknown: -140 x08: 0 x0c: 0 Attack Tile Health
     * x00: 6 unknown: 125 x08: 0 x0c: 0 Repair Tile Health
     * x00: 7 unknown: -300 x08: 0 x0c: 0 Mine Gold Health
     * ....
     * Boulder: Slap Damage x00: 240 unknown: 400 x08: 0 x0c: 0
     */
    public static class MiscVariable extends Variable {

        private int variableId;
        private int value;
        private int unknown1;
        private int unknown2;

        public int getVariableId() {
            return variableId;
        }

        protected void setVariableId(int variableId) {
            this.variableId = variableId;
        }

        public int getValue() {
            return value;
        }

        protected void setValue(int value) {
            this.value = value;
        }

        public int getUnknown1() {
            return unknown1;
        }

        protected void setUnknown1(int unknown1) {
            this.unknown1 = unknown1;
        }

        public int getUnknown2() {
            return unknown2;
        }

        protected void setUnknown2(int unknown2) {
            this.unknown2 = unknown2;
        }

        @Override
        public String toString() {
            return "MiscVariable{" + "variableId=" + variableId + ", value=" + value + ", unknown_1=" + unknown1 + ", unknown_2=" + unknown2 + '}';
        }
    }

    public static class Sacrifice extends Variable {

        private SacrificeType type1; // byte
        private short id1; // byte
        private SacrificeType type2; // byte
        private short id2; // byte
        private SacrificeType type3; // byte
        private short id3; // byte
        private SacrificeRewardType rewardType; // byte
        private short speechId; // byte
        private int rewardValue; // 4 byte

        public SacrificeType getType1() {
            return type1;
        }

        protected void setType1(SacrificeType type1) {
            this.type1 = type1;
        }

        public short getId1() {
            return id1;
        }

        protected void setId1(short id1) {
            this.id1 = id1;
        }

        public SacrificeType getType2() {
            return type2;
        }

        protected void setType2(SacrificeType type2) {
            this.type2 = type2;
        }

        public short getId2() {
            return id2;
        }

        protected void setId2(short id2) {
            this.id2 = id2;
        }

        public SacrificeType getType3() {
            return type3;
        }

        protected void setType3(SacrificeType type3) {
            this.type3 = type3;
        }

        public short getId3() {
            return id3;
        }

        protected void setId3(short id3) {
            this.id3 = id3;
        }

        public SacrificeRewardType getRewardType() {
            return rewardType;
        }

        protected void setRewardType(SacrificeRewardType rewardType) {
            this.rewardType = rewardType;
        }

        public short getSpeechId() {
            return speechId;
        }

        protected void setSpeechId(short speechId) {
            this.speechId = speechId;
        }

        public int getRewardValue() {
            return rewardValue;
        }

        protected void setRewardValue(int rewardValue) {
            this.rewardValue = rewardValue;
        }

        @Override
        public String toString() {
            return "Sacrifice{" + "type_1=" + type1 + ", id_1=" + id1 + ", type_2=" + type2 + ", id_2=" + id2 + ", type_3=" + type3 + ", id_3=" + id3 + ", rewardType=" + rewardType + ", speeachId=" + speechId + ", rewardValue=" + rewardValue + '}';
        }
    }
    /*
     *
     * x00: 61 unknown: 305493641 x08: -1413218304 x0c: 0
     * x00: 62 unknown: -377003726 x08: 1420541952 x0c: 0
     * x00: 63 unknown: 305498471 x08: -878624768 x0c: 0
     */
}
