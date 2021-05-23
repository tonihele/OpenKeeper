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

import java.util.Objects;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.IValueEnum;

/**
 * Container class for *Variables.kld
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @author ArchDemon
 */
public class Variable {

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
    // 17, 19, 29, 43, 44, 56 - 66, 69, 72, 74, 77, 80, 81, 89 - 92, 99,
    // 107 - 109, 117 - 124, 137, 153, 171, 172
    public final static int SACRIFICES_ID = 43;
    public final static int CREATURE_STATS_ID = 65;
    public final static int CREATURE_FIRST_PERSON_ID = 224;

    public final static int UNKNOWN_0 = 0;
    public final static int UNKNOWN_17 = 17;
    public final static int PLAYER_ALLIANCE = 66;
    public final static int UNKNOWN_77 = 77;

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
            CREATURE(2);

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

            UNAVAILABLE(1),
            RESEARCHABLE(2),
            AVAILABLE(3),
            UNAVAILABLE_ROOM(4);

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

        public enum StatType implements IValueEnum {

            HEIGHT_TILES(0), // +
            HEALTH(1),
            FEAR(2),
            THREAT(3),
            MELEE_DAMAGE(4), // +
            PAY(5),
            MAX_GOLD_HELD(6),
            INITIAL_GOLD_HELD(7),
            UNKNOWN_8(8), // FIXME
            HUNGER_FILL_CHICKENS(9),
            MANA_GENERATED_BY_PRAYER_PER_SECOND(10),
            UNKNOWN_11(11), // FIXME
            UNKNOWN_12(12), // FIXME
            EXPERIENCE_POINTS_FOR_NEXT_LEVEL(13),
            UNKNOWN_14(14), // FIXME
            EXPERIENCE_POINTS_PER_SECOND(15),
            EXPERIENCE_POINTS_FROM_TRAINING_PER_SECOND(16),
            RESEARCH_POINTS_PER_SECOND(17),
            MANUFACTURE_POINTS_PER_SECOND(18),
            DECOMPOSE_VALUE(19),
            UNKNOWN_20(20), // FIXME
            SPEED_TILES_PER_SECOND(21),
            RUN_SPEED_TILES_PER_SECOND(22),
            UNKNOWN_23(23), // FIXME
            TORTURE_TIME_TO_CONVERT_SECONDS(24),
            UNKNOWN_25(25), // FIXME
            UNKNOWN_26(26), // FIXME
            POSSESSION_MANA_COST_PER_SECOND(27),
            OWN_LAND_HEALTH_INCREASE_PER_SECOND(28),
            UNKNOWN_29(29), // FIXME
            DISTANCE_CAN_HEAR_TILES(30),
            MELEE_RECHARGE_TIME_SECONDS(31);  // +

            private StatType(int id) {
                this.id = id;
            }

            @Override
            public int getValue() {
                return id;
            }
            private final int id;
        }

        private StatType statId;
        private int value; // Stat Increase Percentages For Level
        private int level;

        public StatType getStatId() {
            return statId;
        }

        protected void setStatId(StatType statId) {
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
        public int hashCode() {
            int hash = 3;
            hash = 59 * hash + Objects.hashCode(this.statId);
            hash = 59 * hash + this.level;
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
            final CreatureStats other = (CreatureStats) obj;
            if (this.statId != other.statId) {
                return false;
            }
            if (this.level != other.level) {
                return false;
            }
            return true;
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

    public static class MiscVariable extends Variable {

        public enum MiscType implements IValueEnum {

            ENTRANCE_GENERATION_SPEED_SECONDS(3), // value=25
            CLAIM_TILE_HEALTH(4), // value=825
            ATTACK_TILE_HEALTH(5), // value=-140
            REPAIR_TILE_HEALTH(6), // value=125
            MINE_GOLD_HEALTH(7), // value=-300
            DIG_ROCK_HEALTH(8), // value=-5000
            DIG_OWN_WALL_HEALTH(9), // value=-130
            DIG_ENEMY_WALL_HEALTH(10), // value=-8
            FILL_IN_HEALTH(11), // value=5
            REINFORCE_WALL_HEALTH(12), // value=50
            REPAIR_WALL_HEALTH(13), // value=22
            CONVERT_ROOM_HEALTH(14), // value=-4000
            ATTACK_ROOM_HEALTH(15), // value=-1500
            REPAIR_ROOM_HEALTH(16), // value=20000
            CHICKEN_GENERATION_TIME_PER_HATCHERY(18), // value=16
            GOLD_MINED_FROM_GEMS(20), // value=25
            TIME_BETWEEN_REATTEMPTING_HUNGER_SECONDS(21), // value=4
            TIME_BETWEEN_REATTEMPTING_SLEEP_SECONDS(22), // value=4
            DEAD_BODY_DIES_AFTER_SECONDS(23), // value=60
            MAX_GOLD_PER_TREASURY_TILE(24), // value=3000
            DECOMPOSE_VALUE_NEEDED_FOR_VAMPIRE(25), // value=1000
            CREATURE_DYING_STATE_DURATION_SECONDS(26), // value=60
            SPECIAL_INCREASE_GOLD_AMOUNT(27), // value=10000
            SPECIAL_INCREASE_MANA_AMOUNT(28), // value=50000
            IMP_IDLE_DELAY_BEFORE_REEVALUATION_SECONDS(30), // value=5
            DELAY_BEFORE_PAY_REEVALUATION_SECONDS(31), // value=4
            MAX_GOLD_PILE_OUTSIDE_TREASURY(32), // value=2000
            PAY_DAY_FREQUENCY_SECONDS(33), // value=600
            MOTIONLESS_WHILE_RESEARCHING_SECONDS(34), // value=70
            MOTIONLESS_WHILE_MANUFACTURING_SECONDS(35), // value=5
            STATE_COUNTER_DELAY_BEFORE_REATTEMPT_EATING(36), // value=5
            STATE_COUNTER_WHILE_GUARDING(37), // value=10
            MOTIONLESS_WHILE_PRAYING_SECONDS(38), // value=30
            MOTIONLESS_IN_PRISON_SECONDS(39), // value=8
            MOTIONLESS_WHILE_TRAINING_SECONDS(40), // value=5
            MOTIONLESS_IN_CASINO_SECONDS(41), // value=13
            EXCESS_MANA_DECREASE_RATE_PER_SECOND(42), // value=5
            DEFAULT_HORIZONTAL_TERRAIN_WIBBLE(45), // value=24576
            DEFAULT_VERTICAL_TERRAIN_WIBBLE(46), // value=12288
            REBEL_MAX_FOLLOWERS(47), // value=2
            REBEL_LEAVING_PERCENTAGE(48), // value=70
            REBEL_BECOME_GOOD_PERCENTAGE(49), // value=15
            REBEL_DEFECT_PERCENTAGE(50), // value=15
            TIME_BEFORE_FIRST_PAY_DAY_SECONDS(51), // value=600
            DEFAULT_TORCH_LIGHT_RED(52), // value=300
            DEFAULT_TORCH_LIGHT_GREEN(53), // value=200
            DEFAULT_TORCH_LIGHT_BLUE(54), // value=100
            PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND(55), // value=-3
            DEFAULT_CELLING_HEIGHT_TILES(67), // value=0
            PAY_DAY_CUT_OFF_TIME_SECONDS(68), // value=120
            CASINO_MODIFY_CREATURE_GOLD_PER_SECOND(70), // value=-50
            CASINO_BIG_WIN_KEEPER_LOSS_PERCENTAGE(71), // value=0
            CREATURES_SUPPORTED_BY_FIRST_PORTAL(73), // value=15
            MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND(75), // value=100
            MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND(76), // value=-550
            MODIFY_ANGER_IN_COMPANY_OF_HATED_CREATURES_PER_SECOND(78), // value=150
            FORCE_APPLIED_TO_SLAPPED_CREATURE(79), // value=1024
            CANNOT_SLEEP_MODIFY_CREATURE_HEALTH_PER_SECOND(82), // value=0
            CANNOT_LEAVE_MODIFY_CREATURE_HEALTH_PER_SECOND(83), // value=0
            PLAYER_RESCAN_INTERVAL_SECONDS(84), // value=-1
            PLAYER_RESCAN_RANGE(85), // value=-1
            MAX_FREE_RANGE_CHICKENS_PER_PLAYER(86), // value=12
            WOOD_BRIDGE_LIFE_ON_LAVA_SECONDS(87), // value=30
            CREATURE_SLEEPS_WHEN_BELOW_PERCENT_HEALTH(88), // value=50
            DOOR_PICKED_TIME_SECONDS(93), // value=5
            MINIMUM_IMP_THRESHOLD(94), // value=4
            TIME_BEFORE_FREE_IMP_GENERATED_SECONDS(95), // value=5
            CREATURE_STUNNED_TIME_SECONDS(96), // value=2
            CREATURE_STUNNED_EFFECT_DELAY_SECONDS(97), // value=1
            FIRST_PERSON_STAT_INCREASE_PERCENTAGE(98), // value=12
            MANA_COST_IN_FIRST_PERSON_PER_SECOND(100), // value=5
            HEALTH_DECREASE_WHEN_ON_FIRE_PER_SECOND(101), // value=-150
            MODIFY_CREATURE_ANGER_WHILE_PRAYING_PER_SECOND(102), // value=-275
            ROOM_SELL_VALUE_PERCENTAGE_OF_COST(104), // value=50
            DOOR_SELL_VALUE_PERCENTAGE_OF_COST(105), // value=50
            TRAP_SELL_VALUE_PERCENTAGE_OF_COST(106), // value=50
            INSUFFICIENT_MANA_IMP_EVALUATION_PERIOD_SECONDS(110), // value=10
            DEATH_WHEN_POSSESSING_MANA_REDUCTION(111), // value=100
            PERCENTAGE_OF_MANA_GAINED_BY_SACRIFICING(112), // value=0
            POSSESSION_FRIENDLY_STAT_INCREASE_PERCENTAGE(113), // value=6
            POSSESSION_FRIENDLY_HEALTH_INCREASE_PERCENTAGE(114), // value=6
            MODIFY_CREATURE_HEALTH_WHILE_PRAYING_PER_SECOND(115), // value=33
            HYPNOTISM_MANA_DRAIN_PER_SECOND(116), // value=0
            LOW_MANA_WARNING_THRESHOLD(125), // value=100
            TRAINING_ROOM_MAX_EXPERIENCE_LEVEL(126), // value=4
            STUNNED_DAMAGE_INCREASE_PERCENTAGE(127), // value=100
            MAX_NUMBER_OF_THINGS_IN_HAND(128), // value=64
            RUBBER_BAND_AREA_LIMIT(129), // value=8
            GRAVITY_CONSTANT(130), // value=240
            DUNGEON_HEART_HEALTH_REGENERATION_PER_SECOND(131), // value=25
            E_P_LOSS_SECOND_WHILE_NOT_TRAINING(132), // value=0
            MODIFY_ANGER_WHILE_SOLITARY_IN_PIT_PER_SECOND(133), // value=275
            MODIFY_ANGER_OF_COMBAT_PIT_VICTOR(134), // value=-16400
            MODIFY_ANGER_OF_PIT_SPECTATOR_PER_SECOND(135), // value=-275
            CONVERT_NEUTRAL_ROOM_HEALTH(136), // value=-20000
            MANUFACTURE_POINT_LOSS_WHILE_NO_WORKERS_PER_SECOND(138), // value=1
            CASINO_BIG_WINNER_INCREASED_HAPPINESS_PERCENTAGE(139), // value=100
            DECREASE_ENTRANCE_PERCENTAGE_OF_LAST_SACKED_CREATURE_TYPE(140), // value=33
            INCREASE_ENTRANCE_PERCENTAGE_OF_NEW_CREATURE_TYPES(141), // value=33
            INCREASED_WORK_RATE_PERCENTAGE_OF_PLAYERS_TORTURED_CREATURE_TYPES(142), // value=25
            INCREASED_WORK_RATE_PERCENTAGE_WHEN_CASINO_BIG_WIN_OCCURS(143), // value=35
            IMP_POP_COUNTDOWN_TIME_SECONDS(144), // value=10
            NUMBER_OF_RANDOM_WALL_DETERIORATIONS_PER_SECOND(145), // value=0
            WALL_DETERIORATION_DAMAGE(146), // value=0
            WALL_DAMAGE_FROM_SHOT(147), // value=-10
            PERCENTAGE_CHANCE_OF_KILLING_CREATURE_IN_PIT(148), // value=10
            MODIFY_PLAYER_GOLD_WHILE_TRAINING_PER_SECOND(149), // value=-5
            SKELETON_ARMY_DURATION_SECONDS(150), // value=30
            OVERCROWDING_JAIL_BREAK_PERCENTAGE_PER_EXTRA_CREATURE(151), // value=50
            MAX_TIME_IN_GUARD_ROOM_BEFORE_WANDERING_TO_GUARD_POST_SECONDS(152), // value=60
            AMOUNT_OF_GOLD_STOLEN_IN_FIRST_PERSON(154), // value=100
            BOULDER_DETERIORATION_DAMAGE_PERCENTAGE_PER_SECOND(155), // value=10240
            DRUNKEN_DURATION_SECONDS(156), // value=15
            BOULDER_INITIAL_HEALTH(157), // value=4000
            BOULDER_SPEED_TILES_PER_SECOND(158), // value=4915
            BOULDER_TO_CREATURE_DAMAGE_PERCENTAGE(159), // value=50
            BOULDER_SPEED_TO_HEALTH_RATIO(160), // value=50
            BOULDER_FEAR_AMOUNT(161), // value=701
            BOULDER_FEAR_RANGE(162), // value=4
            DRUNK_CHANCE_PERCENTAGE_PER_DRINK(163), // value=5
            INCREASED_WORK_RATE_PECENTAGE_FROM_SLAPPING(164), // value=50
            INCREASED_WORK_RATE_DURATION_FROM_SLAPPING_SECONDS(165), // value=60
            DUNGEON_HEART_MAX_MANA_INCREASE(166), // value=32000
            DUNGEON_HEART_MANA_GENERATION_INCREASE_PER_SECOND(167), // value=30
            TREMOR_DAMAGE_PER_TERRAIN_TILE_PER_SECOND(168), // value=436
            EXTERNAL_GUARDING_DURATION_SECONDS(169), // value=15
            MINIMUM_JOB_DURATION_SECONDS(170), // value=15
            CREATURE_CRITICAL_HEALTH_PERCENTAGE_OF_MAX(173), // value=20
            DUNGEON_HEART_OBJECT_HEALTH(174), // value=100000
            CLAIM_NEUTRAL_MANA_VAULT_HEALTH(175), // value=4
            CLAIM_ENEMY_MANA_VAULT_HEALTH(176), // value=20
            CREATURES_SUPPORTED_PER_ADDITIONAL_PORTAL(177), // value=5
            MAX_GOLD_PER_DUNGEON_HEART_TILE(178), // value=1000
            IMP_EXPERIENCE_GAIN_PER_SECOND(179), // value=20
            DUNGEON_HEART_CLAIM_SCAN_RADIUS_TILES(180), // value=15
            STUN_TIME_PERCENTAGE_INCREASE_WHEN_HIT(181), // value=25
            CALL_TO_ARMS_FIGHT_DISTANCE_TILES(182), // value=5
            DUNGEON_HEART_REPORTING_DISTANCE_TILES(183), // value=4
            DWARF_DIGGING_MULTIPLIER(184), // value=4
            TRIGGER_TRAP_TRIGGER_SPEED_TILES_PER_SECOND(185), // value=4096
            FIRST_PERSON_ACCURACY_ANGLE_DEGREES(187), // value=45
            SPECIAL_RECEIVE_IMPS_AMOUNT(188), // value=10
            DUNGEON_HEART_CREATURE_GATHERING_DISTANCE_TILES(189), // value=7
            GUARD_ROOM_REPORTING_DISTANCE_TILES(190), // value=7
            GUARD_POST_REPORTING_DISTANCE_TILES(191), // value=7
            COMBAT_PIT_MAX_EXPERIENCE_LEVEL(192), // value=8
            TIME_IN_HAND_BEFORE_CREATURES_BECOME_ANGRY_SECONDS(193), // value=1000
            SLAP_WORK_FASTER_DURATION_SECONDS(194), // value=25
            SLAP_SPEED_UP_DURATION_SECONDS(195), // value=25
            TORTURE_SPEED_UP_DURATION_SECONDS(196), // value=40
            CASINO_WIN_SPEED_UP_DURATION_SECONDS(197), // value=60
            CRITICAL_HEALTH_PERCENTAGE_FOR_HERO_LAIR(198), // value=20
            GAMBLING_MODIFY_CREATURE_MONEY_SMILES_PER_SECOND(199), // value=1
            GAMBLING_MODIFY_CREATURE_MONEY_MONEY_PER_SECOND(200), // value=-8
            GAMBLING_MODIFY_CREATURE_ANGER_SMILES_PER_SECOND(201), // value=-2000
            GAMBLING_MODIFY_CREATURE_ANGER_MONEY_PER_SECOND(202), // value=100
            GAMBLING_DELAY_BEFORE_BECOMING_FEARLESS_SECONDS(203), // value=120
            GAMBLING_FEARLESS_DURATION_SECONDS(204), // value=120
            GAMBLING_CREATURE_JACKPOT_CHANCE_SMILES_PER_SECOND(205), // value=5000
            GAMBLING_CREATURE_JACKPOT_CHANCE_MONEY_PER_SECOND(206), // value=20000
            GAMBLING_JACKPOT_PAY_TIME_LIMIT(207), // value=240
            GAMBLING_JACKPOT_PAY_TIME_LIMIT_MISSED_ANGER_PERCENTAGE_MODIFIER(208), // value=100
            GAMBLING_JACKPOT_BASIC_AMOUNT(209), // value=1000
            FIRST_PERSON_RECHARGE_MODIFY_PERCENTAGE(210), // value=100
            BACK_OFF_MINIMUM_DURATION_SECONDS(211), // value=0
            BACK_OFF_MAXIMUM_DURATION_SECONDS(212), // value=30
            GOOD_CREATURES_CHASE_ENEMY_DURATION_SECONDS(213), // value=3
            EVIL_CREATURES_CHASE_ENEMY_DURATION_SECONDS(214), // value=7
            MUSIC_LEVEL_TWO_THREAT_THRESHOLD(215), // value=100
            MUSIC_LEVEL_THREE_THREAT_THRESHOLD(216), // value=500
            MUSIC_LEVEL_FOUR_THREAT_THRESHOLD(217), // value=1500
            TIME_BEFORE_DUNGEON_HEART_CONSTRUCTION_BEGINS(218), // value=0
            DEFAULT_TORCH_LIGHT_INTENSITY(219), // value=180
            DEFAULT_TORCH_LIGHT_RADIUS_TILES(220), // value=6144
            DEFAULT_TORCH_LIGHT_HEIGHT_TILES(221), // value=6144
            MAXIMUM_MANA_THRESHOLD(222), // value=200000
            MAXIMUM_GROUP_NUMBER_OF_FOLLOWERS(223), // value=8
            COMBAT_PIT_MELEE_DAMAGE_MODIFIER_PERCENTAGE(225), // value=100
            UPGRADED_POSSESSION_COST_PERCENTAGE(226), // value=70
            UPGRADED_CALL_TO_ARMS_COST_PERCENTAGE(227), // value=70
            MPD_SCORE_HERO_KILLED(228), // value=50
            MPD_SCORE_LAND_OWNED(229), // value=5
            MPD_SCORE_GOLD_SLABS_MINED(230), // value=1
            MPD_SCORE_ITEM_MANUFACTURED(231), // value=10
            MPD_SCORE_CREATURE_ENTERED(232), // value=20
            TORTURE_CHANCE_OF_DYING_WHEN_CONVERTED(234), // value=0
            MAXIMUM_MANA_GAIN_PER_SECOND(235), // value=500
            CLAIM_SCAN_LIGHT_RED(236), // value=200
            CLAIM_SCAN_LIGHT_GREEN(237), // value=80
            CLAIM_SCAN_LIGHT_BLUE(238), // value=35
            PIT_PERCENTAGE_DAMAGE_TAKEN_OF_NORMAL_COMBAT(239), // value=20
            BOULDER_SLAP_DAMAGE(240), // value=400

            GAME_TICKS(74), // value=4
            LEVEL_RATING(241),
            AVERAGE_TIME(242),
            UNKNOWN_233(233), // value=40
            UNKNOWN_61(61), // value=305493641, unknown_1=-1413218304, unknown_2=0}
            UNKNOWN_62(62), // value=-377003726, unknown_1=1420541952, unknown_2=0}
            UNKNOWN_63(63); // value=305498471, unknown_1=-878624768, unknown_2=0}

            private MiscType(int id) {
                this.id = id;
            }

            @Override
            public int getValue() {
                return id;
            }
            private final int id;
        }

        private final static int gameTicks = 1363;
        private MiscType variableId;
        private float value;
        private int unknown1;
        private int unknown2;

        public MiscType getVariableId() {
            return variableId;
        }

        protected void setVariableId(MiscType variableId) {
            this.variableId = variableId;
        }

        public float getValue() {
            return value;
        }

        // TODO how save int or float values ???
        protected void setValue(int value) {
            float result = value;
            if (this.variableId == MiscType.GAME_TICKS) {
                result = MiscVariable.gameTicks / value;
            } else if (this.variableId == MiscType.LEVEL_RATING) {
                result = value >> 12; // FIXME Why ???
            } else if (this.variableId == MiscType.BOULDER_DETERIORATION_DAMAGE_PERCENTAGE_PER_SECOND
                    || this.variableId == MiscType.BOULDER_SPEED_TILES_PER_SECOND
                    || this.variableId == MiscType.TRIGGER_TRAP_TRIGGER_SPEED_TILES_PER_SECOND
                    || this.variableId == MiscType.DEFAULT_TORCH_LIGHT_RADIUS_TILES
                    || this.variableId == MiscType.DEFAULT_TORCH_LIGHT_HEIGHT_TILES) {
                result = value / ConversionUtils.FLOAT;
            }
            this.value = result;
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
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + Objects.hashCode(this.variableId);
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
            final MiscVariable other = (MiscVariable) obj;
            if (this.variableId != other.variableId) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "MiscVariable{" + "variableId=" + variableId + ", value=" + value + ", unknown_1=" + unknown1 + ", unknown_2=" + unknown2 + '}';
        }
    }

    public static class Unknown extends Variable {

        public int variableId;
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
        public int hashCode() {
            int hash = 5;
            hash = 83 * hash + this.value;
            hash = 83 * hash + this.unknown1;
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
            final Unknown other = (Unknown) obj;
            if (this.value != other.value) {
                return false;
            }
            if (this.unknown1 != other.unknown1) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Unknown{" + "variableId=" + variableId + ", value=" + value + ", unknown1=" + unknown1 + ", unknown2=" + unknown2 + '}';
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
            return "Sacrifice{" + "type_1=" + type1 + ", id_1=" + id1 + ", type_2=" + type2 + ", id_2=" + id2
                    + ", type_3=" + type3 + ", id_3=" + id3 + ", rewardType=" + rewardType
                    + ", speeachId=" + speechId + ", rewardValue=" + rewardValue + '}';
        }
    }

    public static class PlayerAlliance extends Variable {

        private int playerIdOne;
        private int playerIdTwo;
        private int unknown1;

        public int getPlayerIdOne() {
            return playerIdOne;
        }

        protected void setPlayerIdOne(int playerIdOne) {
            this.playerIdOne = playerIdOne;
        }

        public int getPlayerIdTwo() {
            return playerIdTwo;
        }

        protected void setPlayerIdTwo(int playerIdTwo) {
            this.playerIdTwo = playerIdTwo;
        }

        public int getUnknown1() {
            return unknown1;
        }

        protected void setUnknown1(int unknown1) {
            this.unknown1 = unknown1;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 37 * hash + this.playerIdOne;
            hash = 37 * hash + this.playerIdTwo;
            hash = 37 * hash + this.unknown1;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PlayerAlliance other = (PlayerAlliance) obj;
            if (this.playerIdOne != other.playerIdOne) {
                return false;
            }
            if (this.playerIdTwo != other.playerIdTwo) {
                return false;
            }
            if (this.unknown1 != other.unknown1) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "PlayerAlliance{" + "playerIdOne=" + playerIdOne + ", playerIdTwo=" + playerIdTwo + ", unknown1=" + unknown1 + '}';
        }

    }
}
