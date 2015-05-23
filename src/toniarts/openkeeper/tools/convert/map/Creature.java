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
import toniarts.openkeeper.tools.convert.IFlagEnum;
import java.util.EnumSet;
import javax.vecmath.Vector3f;

/**
 * Container class for Creatures.kwd
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Creature implements Comparable<Creature> {

    /**
     * Creature flags
     */
    public enum CreatureFlag implements IFlagEnum {

        IS_WORKER(1),
        CAN_BE_PICKED_UP(2),
        CAN_BE_SLAPPED(4),
        ALWAYS_FLEE(8),
        CAN_WALK_ON_WATER(16),
        CAN_WALK_ON_LAVA(32),
        IS_EVIL(128), // Obviously otherwise it is good
        IS_IMMUNE_TO_TURNCOAT(512),
        AVAILABLE_VIA_PORTAL(1024),
        CAN_FLY(8192),
        IS_HORNY(16384),
        GENERATE_DEAD_BODY(32768),
        CAN_BE_HYPNOTIZED(65536),
        IS_IMMUNE_TO_CHICKEN(131072),
        IS_FEARLESS(262144),
        CAN_BE_ELECTROCUTED(524288),
        NEED_BODY_FOR_FIGHT_IDLE(1048576),
        TRAIN_WHEN_IDLE(2097152),
        ONLY_ATTACKABLE_BY_HORNY(4194304),
        CAN_BE_RESURECTED(8388608),
        DOESNT_GET_ANGRY_WITH_ENEMIES(16777216),
        FREES_FRIENDS_ON_JAILBREAK(33554432),
        REVEALS_ADJACENT_TRAPS(67108864),
        IS_UNIQUE(134217728),
        CAMERA_ROLLS_WHEN_TURNING(268435456), // 1st person movement flag
        MPD_RANDOM_INVADER(1073741824), // My Pet Dungeon?
        IS_MALE(2147483648l);  // Obviously otherwise it is female
        private final long flagValue;

        private CreatureFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    };

    /**
     * Creature flags, extended
     */
    public enum CreatureFlag2 implements IFlagEnum {

        IS_IMMUNE_TO_LIGHTNING(1),
        IS_STONE_KNIGHT(2),
        IS_EMOTIONLESS(4),
        AVAILABLE_VIA_HERO_PORTAL(8);
        private final long flagValue;

        private CreatureFlag2(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    };

    public enum AttackType implements IValueEnum {

        NONE(0),
        MELEE_BLADE(1),
        MELEE_BLUNT(2),
        MELEE_BODY(3),
        FIRE(4),
        MOVEMENT(5),
        LIGHTNING(6),
        GAS(7),
        PROJECTILE(8),
        ENHANCE_OTHER(9),
        ENHANCE_SELF(10),
        CLOSE_COMBAT(11),
        GENERATION(12),
        PHYSICAL_TRAP(13),
        NON_LETHAL_TRAP(14),
        MELEE_SCYTHE(15);

        private AttackType(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    public enum JobType implements IValueEnum {

        NONE(0),
        SLEEP(1),
        EAT(2),
        RESEARCH(3),
        TRAIN(4),
        MANUFACTURE(5),
        GUARD(6),
        TORTURE(7),
        PRAY(8),
        DRINK(9),
        LEAVE(10),
        DESTROY_ENEMY_ROOMS(11),
        TOLLING(12),
        STEAL_ENEMY_GOLD(13),
        SULK(15),
        REBEL(16),
        EXPLORE(24);

        private JobType(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private int id;
    }

    public enum JobClass implements IValueEnum {

        ARTISAN(0),
        THINKER(1),
        FIGHTER(2),
        SCOUT(3);

        private JobClass(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    public enum FightStyle implements IValueEnum {

        NON_FIGHTER(0),
        BLITZER(1),
        BLOCKER(2),
        FLANKER(3),
        SUPPORT(4);

        private FightStyle(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    public enum DeathFallDirection implements IValueEnum {

        NORTH(0),
        NORTH_EAST(1),
        EAST(2),
        SOUTH_EAST(3),
        SOUTH(4),
        SOUTH_WEST(5),
        WEST(6),
        NORTH_WEST(7);

        private DeathFallDirection(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    public enum Swipe implements IValueEnum {

        NONE(0),
        BITE(1),
        CLAW_SLASH(2),
        CLUB_SMASH(3),
        DARK_FGBS_SLASH(4),
        KING_FGBS_SLASH(5),
        LEFT_PUNCH(6),
        RIGHT_PUNCH(7),
        LEFT_SCYTHE_SLASH(8),
        RIGHT_SCYTHE_SLASH(9),
        MISTRESS_SLASH(10),
        SMALL_SLASH(11),
        STAFF_STRIKE(12),
        SWORD_SLASH(13),
        LIGHT_ARROW(14),
        DARK_ARROW(15),
        LEFT_BILE_SMASH(16),
        RIGHT_BILE_SMASH(17),
        HYPNOTISE(18),
        LEFT_SMALL_PUNCH(19),
        RIGHT_SMALL_PUNCH(20),
        LEFT_KING_FGBS_SLASH(21),
        RIGHT_KING_FGBS_SLASH(22);

        private Swipe(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    public enum SpecialAbility implements IValueEnum {

        NONE(0),
        PICK_UP_WORKERS(1),
        SNIPER_MODE(2),
        TURN_TO_BAT(3),
        FLY(4),
        HYPNOTISE(5),
        DISGUISE_N_STEAL(6),
        PRAY(7),
        PICK_LOCKS(8),
        FEAR_ATTACK(9),
        GHOST_POSESSION(10);

        private SpecialAbility(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    public enum GammaEffect implements IValueEnum {

        NORMAL(0),
        VAMPIRE_RED(1),
        DARK_ELF_PURPLE(2),
        SKELETON_BLACK_N_WHITE(3),
        SALAMANDER_INFRARED(4),
        DARK_ANGER_BRIGHT_BLUE(5),
        WHITEOUT(6),
        BLACKOUT(7),
        REDOUT(8),
        DEATH_VIEW_HALF_RED(9),
        MARTINS_TEST_1(10),
        MARTINS_TEST_2(11),
        MARTINS_TEST_3(12),
        MARTINS_TEST_4(13),
        FREEZE_VIEW_ONLY_BLUE(14),
        MAIDEN(15),
        GHOST(16);

        private GammaEffect(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }
//    struct CreatureBlock {
//        char name[32]; /* 0 */
//        ArtResource ref1[39]; /* 20 */
//        uint16_t unkcec; /* cec */
//        uint32_t unkcee; /* cee */
//        uint32_t unkcf2; /* cf2 */
//        uint8_t editor_order; /* cf6 */
//        uint16_t unk2c; /* cf7 */
//        int32_t shot_delay; /* cf9 */
//        uint16_t unkcfd; /* cfd */
//        uint16_t unkcff; /* cff */
//        uint32_t unkd01; /* d01 */
//        uint16_t unk2d[9]; /* d05 */
//        char unkd17[32]; /* d17 */
//        int32_t shuffle_speed; /* d37 */
//        uint8_t unk2e[5]; /* d3b */
//        ArtResource ref2; /* d40 */
//        LightBlock light; /* d94 */
//        struct {
//        uint32_t present; /* dac */
//        uint32_t room_id_and_size; /* db0 */
//        } attraction[2];
//        uint32_t unkdbc; /* dbc */
//        uint32_t unkdc0; /* dc0 */
//        struct { /* bytes in these structs might be product of padding */
//        uint32_t x00;
//        uint32_t x04;
//        uint32_t x08;
//        uint8_t x0c;
//        uint8_t x0d;
//        uint8_t x0e;
//        uint8_t x0f;
//        uint32_t x10;
//        uint8_t x14;
//        uint8_t x15;
//        uint8_t x16;
//        uint8_t x17;
//        } xdc4[3];
//        struct {
//        uint8_t x00;
//        uint8_t x01;
//        } xe0c[4];
//        struct {
//        uint32_t x00;
//        uint16_t x04;
//        uint16_t x06;
//        uint8_t x08;
//        uint8_t x09;
//        uint8_t x0a;
//        uint8_t x0b;
//        } xe14[3], xe38[2], xe50[3];
//        uint32_t xe74[2];
//        struct {
//        uint32_t x00;
//        uint16_t x04;
//        uint16_t x06;
//        } xe7c[3];
//        struct {
//        uint32_t x00;
//        uint32_t x04;
//        uint32_t x08;
//        } xe94;
//        int32_t unkea0; /* ea0 */
//        int32_t height; /* ea4 */
//        uint32_t unkea8; /* ea8 */
//        uint32_t unk3ab; /* eac */
//        int32_t eye_height; /* eb0 */
//        int32_t speed; /* eb4 */
//        int32_t run_speed; /* eb8 */
//        uint32_t unk3ac; /* ebc */
//        uint32_t time_awake; /* ec0 */
//        uint32_t time_sleep; /* ec4 */
//        uint32_t unkec8; /* ec8 */
//        uint32_t unkecc; /* ecc */
//        uint32_t unked0; /* ed0 */
//        uint32_t unked4; /* ed4 */
//        uint32_t unked8; /* ed8 */
//        int32_t slap_fearless_duration; /* edc */
//        int32_t unkee0; /* ee0 */
//        int32_t unkee4; /* ee4 */
//        short possession_mana_cost; /* ee8 */
//        short own_land_health_increase; /* eea */
//        int32_t range; /* eec */
//        uint32_t unkef0; /* ef0 */
//        uint32_t unk3af; /* ef4 */
//        int32_t melee_recharge; /* ef8 */
//        uint32_t unkefc; /* efc */
//        uint16_t exp_for_next_level; /* f00 */
//        uint8_t unk3b[2]; /* f02 */
//        uint16_t exp_per_second; /* f04 */
//        uint16_t exp_per_second_training; /* f06 */
//        uint16_t research_per_second; /* f08 */
//        uint16_t manufacture_per_second; /* f0a */
//        uint16_t hp; /* f0c */
//        uint16_t hp_from_chicken; /* f0e */
//        uint16_t fear; /* f10 */
//        uint16_t threat; /* f12 */
//        uint16_t melee_damage; /* f14 */
//        uint16_t slap_damage; /* f16 */
//        uint16_t mana_gen_prayer; /* f18 */
//        uint16_t unk3cb; /* f1a */
//        uint16_t pay; /* f1c */
//        uint16_t max_gold_held; /* f1e */
//        uint16_t unk3cc; /* f20 */
//        uint16_t decompose_value; /* f22 */
//        uint16_t unk3cd[2]; /* f24 */
//        short anger_no_lair; /* f28 */
//        short anger_no_food; /* f2a */
//        short anger_no_pay; /* f2c */
//        short anger_no_work; /* f2e */
//        short anger_slap; /* f30 */
//        short anger_in_hand; /* f32 */
//        short initial_gold_held; /* f34 */
//        uint16_t unk3ce[3]; /* f36 */
//        uint16_t slap_effect_id; /* f3c */
//        uint16_t death_effect_id; /* f3e */
//        uint16_t unkf40; /* f40 */
//        uint8_t unk3d[3]; /* f42 */
//        uint8_t unkf45; /* f45 */
//        uint8_t unk40[2]; /* f46 */
//        uint8_t unkf48[3]; /* f48 */
//        uint8_t id; /* f4b */
//        uint8_t unk3ea[3]; /* f4c */
//        uint8_t unhappy_threshold; /* f4f */
//        uint8_t unk3eb[2]; /* f50 */
//        uint8_t lair_object_id; /* f52 */
//        uint8_t unk3f[3]; /* f53 */
//        char xname[32]; /* f56 */
//        uint8_t material; /* f76 */
//        ArtResource reff77; /* f77 */
//        uint16_t unkfcb; /* fcb */
//        uint32_t unk4; /* fcd */
//        ArtResource ref3; /* fd1 */
//        uint8_t unk5[2]; /* 1025 */
//        ArtResource ref4; /* 1027 */
//        uint32_t unk6; /* 107b */
//        short torture_hp_change; /* 107f */
//        short torture_mood_change; /* 1081 */
//        ArtResource ref5[6]; /* 1083 */
//        struct {
//        uint32_t x00;
//        uint32_t x04;
//        uint32_t x08;
//        } unk7[7]; /* 127b */
//        ArtResource ref6; /* 12cf */
//        struct {
//        uint16_t x00;
//        uint16_t x02;
//        } x1323[48];
//        ArtResource ref7[3]; /* 13e3 */
//        uint16_t unk14df;
//        uint32_t x14e1[2]; /* 14e1 */
//        uint32_t x14e9[2]; /* 14e9 */
//        ArtResource ref8; /* 14f1 */
//        uint32_t unk1545;
//        };
    private String name; // 0
    private ArtResource ref1[]; // 20
    private int unkcec; // cec
    private int unkcee; // cee
    private int unkcf2; // cf2
    private short orderInEditor; // cf6
    private int angerStringIdGeneral; // cf7
    private float shotDelay; // cf9
    private int olhiEffectId; // cfd, OLHI, wut?
    private int introductionStringId; // cff
    private float perceptionRange; // d01, Fog of war
    private int angerStringIdLair;
    private int angerStringIdFood;
    private int angerStringIdPay;
    private int angerStringIdWork;
    private int angerStringIdSlap;
    private int angerStringIdHeld;
    private int angerStringIdLonely;
    private int angerStringIdHatred;
    private int angerStringIdTorture;
    private String translationSoundGategory; // d17
    private float shuffleSpeed; // d37
    private short cloneCreatureId;
    private GammaEffect firstPersonGammaEffect; // d3b
    private short firstPersonWalkCycleScale; // Movement
    private short introCameraPathIndex;
    private short unk2e2;
    private ArtResource portraitResource; // d40
    private Light light; // d94
    private Attraction attractions[];
    private float firstPersonWaddleScale; // dbc Movement
    private float firstPersonOscillateScale; // dc0 Movement
    private Spell spells[];
    private Resistance resistances[];
    private JobPreference happyJobs[];
    private JobPreference unhappyJobs[];
    private JobPreference angryJobs[];
    private JobType hateJobs[];
    private Xe7c xe7c[];
    private Xe94 xe94;
    private int unkea0; // ea0
    private float height; // ea4
    private int unkea8; // ea8
    private int unk3ab; // eac
    private float eyeHeight; // eb0
    private float speed; // eb4
    private float runSpeed; // eb8
    private float hungerRate; // ebc
    private int timeAwake; // ec0
    private int timeSleep; // ec4
    private float distanceCanSee; // ec8, tiles
    private float distanceCanHear; // ecc, tiles
    private float stunDuration; // ed0, seconds
    private float guardDuration; // ed4, seconds
    private float idleDuration; // ed8, seconds
    private float slapFearlessDuration; // edc
    private int unkee0; // ee0
    private int unkee4; // ee4
    private short possessionManaCost; // ee8
    private short ownLandHealthIncrease; // eea
    private float meleeRange; // eec
    private int unkef0; // ef0
    private float tortureTimeToConvert; // ef4, seconds
    private float meleeRecharge; // ef8
    private EnumSet<CreatureFlag> flags; // efc
    private int expForNextLevel; // f00
    private JobClass jobClass; // f02
    private FightStyle fightStyle;
    private int expPerSecond; // f04
    private int expPerSecondTraining; // f06
    private int researchPerSecond; // f08
    private int manufacturePerSecond; // f0a
    private int hp; // f0c
    private int hpFromChicken; // f0e
    private int fear; // f10
    private int threat; // f12
    private int meleeDamage; // f14
    private int slapDamage; // f16
    private int manaGenPrayer; // f18
    private int unk3cb; // f1a
    private int pay; // f1c
    private int maxGoldHeld; // f1e
    private int unk3cc; // f20
    private int decomposeValue; // f22
    private int nameStringId;
    private int tooltipStringId;
    private short angerNoLair; // f28
    private short angerNoFood; // f2a
    private short angerNoPay; // f2c
    private short angerNoWork; // f2e
    private short angerSlap; // f30
    private short angerInHand; // f32
    private short initialGoldHeld; // f34
    private int entranceEffectId; // f36
    private int generalDescriptionStringId; // f3c
    private int strengthStringId;
    private int weaknessStringId;
    private int slapEffectId; // f3e
    private int deathEffectId; // f40
    private Swipe melee1Swipe; // f42, Swipes, 1st person attacks
    private Swipe melee2Swipe;
    private short unk3d3;
    private Swipe spellSwipe; // f45
    private SpecialAbility firstPersonSpecialAbility1; // f46, Special abilities, 1st person
    private SpecialAbility firstPersonSpecialAbility2;
    private short unkf48[]; // f48
    private short creatureId; // f4b
    private short unk3ea[]; // f4c
    private short hungerFill;
    private short unhappyThreshold; // f4f
    private AttackType meleeAttackType; // f50
    private short unk3eb2;
    private short lairObjectId; // f52
    private short unk3f1; // f53
    private DeathFallDirection deathFallDirection;
    private short unk3f2;
    private String soundGategory; // f56
    private Material material; // f76, armour type
    private ArtResource firstPersonFilterResource; // f77
    private int unkfcb; // fcb
    private int unk4; // fcd
    private ArtResource ref3; // fd1
    private Swipe special1Swipe; // 1025
    private Swipe special2Swipe;
    private ArtResource firstPersonMeleeResource; // 1027
    private int unk6; // 107b
    private short tortureHpChange; // 107f
    private short tortureMoodChange; // 1081
    private ArtResource ref5[]; // 1083
    private Unk7 unk7[]; // 127b
    private ArtResource ref6; // 12cf
    private X1323 x1323[];
    private ArtResource ref7[]; // 13e3
    private int uniqueNameTextId;
    private int x14e1[]; // 14e1
    private int firstPersonSpecialAbility1Count; // 14e9, available uses or something, not really sure
    private int firstPersonSpecialAbility2Count;
    private ArtResource uniqueResource; // 14f1
    private int unk1545;
    // When the file is embedded in the globals, there is some extra stuff
    private short unknownExtraBytes[]; // 80
    private EnumSet<CreatureFlag2> flags2; // ???
    private int unknown;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public ArtResource[] getRef1() {
        return ref1;
    }

    protected void setRef1(ArtResource ref1[]) {
        this.ref1 = ref1;
    }

    public int getUnkcec() {
        return unkcec;
    }

    protected void setUnkcec(int unkcec) {
        this.unkcec = unkcec;
    }

    public int getUnkcee() {
        return unkcee;
    }

    protected void setUnkcee(int unkcee) {
        this.unkcee = unkcee;
    }

    public int getUnkcf2() {
        return unkcf2;
    }

    protected void setUnkcf2(int unkcf2) {
        this.unkcf2 = unkcf2;
    }

    public short getOrderInEditor() {
        return orderInEditor;
    }

    protected void setOrderInEditor(short orderInEditor) {
        this.orderInEditor = orderInEditor;
    }

    public int getAngerStringIdGeneral() {
        return angerStringIdGeneral;
    }

    protected void setAngerStringIdGeneral(int angerStringIdGeneral) {
        this.angerStringIdGeneral = angerStringIdGeneral;
    }

    public float getShotDelay() {
        return shotDelay;
    }

    protected void setShotDelay(float shotDelay) {
        this.shotDelay = shotDelay;
    }

    public int getOlhiEffectId() {
        return olhiEffectId;
    }

    protected void setOlhiEffectId(int olhiEffectId) {
        this.olhiEffectId = olhiEffectId;
    }

    public int getIntroductionStringId() {
        return introductionStringId;
    }

    protected void setIntroductionStringId(int introductionStringId) {
        this.introductionStringId = introductionStringId;
    }

    public float getPerceptionRange() {
        return perceptionRange;
    }

    protected void setPerceptionRange(float perceptionRange) {
        this.perceptionRange = perceptionRange;
    }

    public int getAngerStringIdLair() {
        return angerStringIdLair;
    }

    protected void setAngerStringIdLair(int angerStringIdLair) {
        this.angerStringIdLair = angerStringIdLair;
    }

    public int getAngerStringIdFood() {
        return angerStringIdFood;
    }

    protected void setAngerStringIdFood(int angerStringIdFood) {
        this.angerStringIdFood = angerStringIdFood;
    }

    public int getAngerStringIdPay() {
        return angerStringIdPay;
    }

    protected void setAngerStringIdPay(int angerStringIdPay) {
        this.angerStringIdPay = angerStringIdPay;
    }

    public int getAngerStringIdWork() {
        return angerStringIdWork;
    }

    protected void setAngerStringIdWork(int angerStringIdWork) {
        this.angerStringIdWork = angerStringIdWork;
    }

    public int getAngerStringIdSlap() {
        return angerStringIdSlap;
    }

    protected void setAngerStringIdSlap(int angerStringIdSlap) {
        this.angerStringIdSlap = angerStringIdSlap;
    }

    public int getAngerStringIdHeld() {
        return angerStringIdHeld;
    }

    protected void setAngerStringIdHeld(int angerStringIdHeld) {
        this.angerStringIdHeld = angerStringIdHeld;
    }

    public int getAngerStringIdLonely() {
        return angerStringIdLonely;
    }

    protected void setAngerStringIdLonely(int angerStringIdLonely) {
        this.angerStringIdLonely = angerStringIdLonely;
    }

    public int getAngerStringIdHatred() {
        return angerStringIdHatred;
    }

    protected void setAngerStringIdHatred(int angerStringIdHatred) {
        this.angerStringIdHatred = angerStringIdHatred;
    }

    public int getAngerStringIdTorture() {
        return angerStringIdTorture;
    }

    protected void setAngerStringIdTorture(int angerStringIdTorture) {
        this.angerStringIdTorture = angerStringIdTorture;
    }

    public String getTranslationSoundGategory() {
        return translationSoundGategory;
    }

    protected void setTranslationSoundGategory(String translationSoundGategory) {
        this.translationSoundGategory = translationSoundGategory;
    }

    public float getShuffleSpeed() {
        return shuffleSpeed;
    }

    protected void setShuffleSpeed(float shuffleSpeed) {
        this.shuffleSpeed = shuffleSpeed;
    }

    public short getCloneCreatureId() {
        return cloneCreatureId;
    }

    protected void setCloneCreatureId(short cloneCreatureId) {
        this.cloneCreatureId = cloneCreatureId;
    }

    public GammaEffect getFirstPersonGammaEffect() {
        return firstPersonGammaEffect;
    }

    protected void setFirstPersonGammaEffect(GammaEffect firstPersonGammaEffect) {
        this.firstPersonGammaEffect = firstPersonGammaEffect;
    }

    public short getFirstPersonWalkCycleScale() {
        return firstPersonWalkCycleScale;
    }

    protected void setFirstPersonWalkCycleScale(short firstPersonWalkCycleScale) {
        this.firstPersonWalkCycleScale = firstPersonWalkCycleScale;
    }

    public short getIntroCameraPathIndex() {
        return introCameraPathIndex;
    }

    protected void setIntroCameraPathIndex(short introCameraPathIndex) {
        this.introCameraPathIndex = introCameraPathIndex;
    }

    public short getUnk2e2() {
        return unk2e2;
    }

    protected void setUnk2e2(short unk2e2) {
        this.unk2e2 = unk2e2;
    }

    public ArtResource getPortraitResource() {
        return portraitResource;
    }

    protected void setPortraitResource(ArtResource portraitResource) {
        this.portraitResource = portraitResource;
    }

    public Light getLight() {
        return light;
    }

    protected void setLight(Light light) {
        this.light = light;
    }

    public Attraction[] getAttractions() {
        return attractions;
    }

    protected void setAttractions(Attraction[] attractions) {
        this.attractions = attractions;
    }

    public float getFirstPersonWaddleScale() {
        return firstPersonWaddleScale;
    }

    protected void setFirstPersonWaddleScale(float firstPersonWaddleScale) {
        this.firstPersonWaddleScale = firstPersonWaddleScale;
    }

    public float getFirstPersonOscillateScale() {
        return firstPersonOscillateScale;
    }

    protected void setFirstPersonOscillateScale(float firstPersonOscillateScale) {
        this.firstPersonOscillateScale = firstPersonOscillateScale;
    }

    public Spell[] getSpells() {
        return spells;
    }

    protected void setSpells(Spell[] spells) {
        this.spells = spells;
    }

    public Resistance[] getResistances() {
        return resistances;
    }

    protected void setResistances(Resistance[] resistances) {
        this.resistances = resistances;
    }

    public JobPreference[] getHappyJobs() {
        return happyJobs;
    }

    protected void setHappyJobs(JobPreference[] happyJobs) {
        this.happyJobs = happyJobs;
    }

    public JobPreference[] getUnhappyJobs() {
        return unhappyJobs;
    }

    protected void setUnhappyJobs(JobPreference[] unhappyJobs) {
        this.unhappyJobs = unhappyJobs;
    }

    public JobPreference[] getAngryJobs() {
        return angryJobs;
    }

    protected void setAngryJobs(JobPreference[] angryJobs) {
        this.angryJobs = angryJobs;
    }

    public JobType[] getHateJobs() {
        return hateJobs;
    }

    protected void setHateJobs(JobType[] hateJobs) {
        this.hateJobs = hateJobs;
    }

    public Xe7c[] getXe7c() {
        return xe7c;
    }

    protected void setXe7c(Xe7c[] xe7c) {
        this.xe7c = xe7c;
    }

    public Xe94 getXe94() {
        return xe94;
    }

    protected void setXe94(Xe94 xe94) {
        this.xe94 = xe94;
    }

    public int getUnkea0() {
        return unkea0;
    }

    protected void setUnkea0(int unkea0) {
        this.unkea0 = unkea0;
    }

    public float getHeight() {
        return height;
    }

    protected void setHeight(float height) {
        this.height = height;
    }

    public int getUnkea8() {
        return unkea8;
    }

    protected void setUnkea8(int unkea8) {
        this.unkea8 = unkea8;
    }

    public int getUnk3ab() {
        return unk3ab;
    }

    protected void setUnk3ab(int unk3ab) {
        this.unk3ab = unk3ab;
    }

    public float getEyeHeight() {
        return eyeHeight;
    }

    protected void setEyeHeight(float eyeHeight) {
        this.eyeHeight = eyeHeight;
    }

    public float getSpeed() {
        return speed;
    }

    protected void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getRunSpeed() {
        return runSpeed;
    }

    protected void setRunSpeed(float runSpeed) {
        this.runSpeed = runSpeed;
    }

    public float getHungerRate() {
        return hungerRate;
    }

    protected void setHungerRate(float hungerRate) {
        this.hungerRate = hungerRate;
    }

    public int getTimeAwake() {
        return timeAwake;
    }

    protected void setTimeAwake(int timeAwake) {
        this.timeAwake = timeAwake;
    }

    public int getTimeSleep() {
        return timeSleep;
    }

    protected void setTimeSleep(int timeSleep) {
        this.timeSleep = timeSleep;
    }

    public float getDistanceCanSee() {
        return distanceCanSee;
    }

    protected void setDistanceCanSee(float distanceCanSee) {
        this.distanceCanSee = distanceCanSee;
    }

    public float getDistanceCanHear() {
        return distanceCanHear;
    }

    protected void setDistanceCanHear(float distanceCanHear) {
        this.distanceCanHear = distanceCanHear;
    }

    public float getStunDuration() {
        return stunDuration;
    }

    protected void setStunDuration(float stunDuration) {
        this.stunDuration = stunDuration;
    }

    public float getGuardDuration() {
        return guardDuration;
    }

    protected void setGuardDuration(float guardDuration) {
        this.guardDuration = guardDuration;
    }

    public float getIdleDuration() {
        return idleDuration;
    }

    protected void setIdleDuration(float idleDuration) {
        this.idleDuration = idleDuration;
    }

    public float getSlapFearlessDuration() {
        return slapFearlessDuration;
    }

    protected void setSlapFearlessDuration(float slapFearlessDuration) {
        this.slapFearlessDuration = slapFearlessDuration;
    }

    public int getUnkee0() {
        return unkee0;
    }

    protected void setUnkee0(int unkee0) {
        this.unkee0 = unkee0;
    }

    public int getUnkee4() {
        return unkee4;
    }

    protected void setUnkee4(int unkee4) {
        this.unkee4 = unkee4;
    }

    public short getPossessionManaCost() {
        return possessionManaCost;
    }

    protected void setPossessionManaCost(short possessionManaCost) {
        this.possessionManaCost = possessionManaCost;
    }

    public short getOwnLandHealthIncrease() {
        return ownLandHealthIncrease;
    }

    protected void setOwnLandHealthIncrease(short ownLandHealthIncrease) {
        this.ownLandHealthIncrease = ownLandHealthIncrease;
    }

    public float getMeleeRange() {
        return meleeRange;
    }

    protected void setMeleeRange(float meleeRange) {
        this.meleeRange = meleeRange;
    }

    public int getUnkef0() {
        return unkef0;
    }

    protected void setUnkef0(int unkef0) {
        this.unkef0 = unkef0;
    }

    public float getTortureTimeToConvert() {
        return tortureTimeToConvert;
    }

    protected void setTortureTimeToConvert(float tortureTimeToConvert) {
        this.tortureTimeToConvert = tortureTimeToConvert;
    }

    public float getMeleeRecharge() {
        return meleeRecharge;
    }

    protected void setMeleeRecharge(float meleeRecharge) {
        this.meleeRecharge = meleeRecharge;
    }

    public EnumSet<CreatureFlag> getFlags() {
        return flags;
    }

    protected void setFlags(EnumSet<CreatureFlag> flags) {
        this.flags = flags;
    }

    public int getExpForNextLevel() {
        return expForNextLevel;
    }

    protected void setExpForNextLevel(int expForNextLevel) {
        this.expForNextLevel = expForNextLevel;
    }

    public JobClass getJobClass() {
        return jobClass;
    }

    protected void setJobClass(JobClass jobClass) {
        this.jobClass = jobClass;
    }

    public FightStyle getFightStyle() {
        return fightStyle;
    }

    protected void setFightStyle(FightStyle fightStyle) {
        this.fightStyle = fightStyle;
    }

    public int getExpPerSecond() {
        return expPerSecond;
    }

    protected void setExpPerSecond(int expPerSecond) {
        this.expPerSecond = expPerSecond;
    }

    public int getExpPerSecondTraining() {
        return expPerSecondTraining;
    }

    protected void setExpPerSecondTraining(int expPerSecondTraining) {
        this.expPerSecondTraining = expPerSecondTraining;
    }

    public int getResearchPerSecond() {
        return researchPerSecond;
    }

    protected void setResearchPerSecond(int researchPerSecond) {
        this.researchPerSecond = researchPerSecond;
    }

    public int getManufacturePerSecond() {
        return manufacturePerSecond;
    }

    protected void setManufacturePerSecond(int manufacturePerSecond) {
        this.manufacturePerSecond = manufacturePerSecond;
    }

    public int getHp() {
        return hp;
    }

    protected void setHp(int hp) {
        this.hp = hp;
    }

    public int getHpFromChicken() {
        return hpFromChicken;
    }

    protected void setHpFromChicken(int hpFromChicken) {
        this.hpFromChicken = hpFromChicken;
    }

    public int getFear() {
        return fear;
    }

    protected void setFear(int fear) {
        this.fear = fear;
    }

    public int getThreat() {
        return threat;
    }

    protected void setThreat(int threat) {
        this.threat = threat;
    }

    public int getMeleeDamage() {
        return meleeDamage;
    }

    protected void setMeleeDamage(int meleeDamage) {
        this.meleeDamage = meleeDamage;
    }

    public int getSlapDamage() {
        return slapDamage;
    }

    protected void setSlapDamage(int slapDamage) {
        this.slapDamage = slapDamage;
    }

    public int getManaGenPrayer() {
        return manaGenPrayer;
    }

    protected void setManaGenPrayer(int manaGenPrayer) {
        this.manaGenPrayer = manaGenPrayer;
    }

    public int getUnk3cb() {
        return unk3cb;
    }

    protected void setUnk3cb(int unk3cb) {
        this.unk3cb = unk3cb;
    }

    public int getPay() {
        return pay;
    }

    protected void setPay(int pay) {
        this.pay = pay;
    }

    public int getMaxGoldHeld() {
        return maxGoldHeld;
    }

    protected void setMaxGoldHeld(int maxGoldHeld) {
        this.maxGoldHeld = maxGoldHeld;
    }

    public int getUnk3cc() {
        return unk3cc;
    }

    protected void setUnk3cc(int unk3cc) {
        this.unk3cc = unk3cc;
    }

    public int getDecomposeValue() {
        return decomposeValue;
    }

    protected void setDecomposeValue(int decomposeValue) {
        this.decomposeValue = decomposeValue;
    }

    public int getNameStringId() {
        return nameStringId;
    }

    protected void setNameStringId(int nameStringId) {
        this.nameStringId = nameStringId;
    }

    public int getTooltipStringId() {
        return tooltipStringId;
    }

    protected void setTooltipStringId(int tooltipStringId) {
        this.tooltipStringId = tooltipStringId;
    }

    public short getAngerNoLair() {
        return angerNoLair;
    }

    protected void setAngerNoLair(short angerNoLair) {
        this.angerNoLair = angerNoLair;
    }

    public short getAngerNoFood() {
        return angerNoFood;
    }

    protected void setAngerNoFood(short angerNoFood) {
        this.angerNoFood = angerNoFood;
    }

    public short getAngerNoPay() {
        return angerNoPay;
    }

    protected void setAngerNoPay(short angerNoPay) {
        this.angerNoPay = angerNoPay;
    }

    public short getAngerNoWork() {
        return angerNoWork;
    }

    protected void setAngerNoWork(short angerNoWork) {
        this.angerNoWork = angerNoWork;
    }

    public short getAngerSlap() {
        return angerSlap;
    }

    protected void setAngerSlap(short angerSlap) {
        this.angerSlap = angerSlap;
    }

    public short getAngerInHand() {
        return angerInHand;
    }

    protected void setAngerInHand(short angerInHand) {
        this.angerInHand = angerInHand;
    }

    public short getInitialGoldHeld() {
        return initialGoldHeld;
    }

    protected void setInitialGoldHeld(short initialGoldHeld) {
        this.initialGoldHeld = initialGoldHeld;
    }

    public int getEntranceEffectId() {
        return entranceEffectId;
    }

    protected void setEntranceEffectId(int entranceEffectId) {
        this.entranceEffectId = entranceEffectId;
    }

    public int getGeneralDescriptionStringId() {
        return generalDescriptionStringId;
    }

    protected void setGeneralDescriptionStringId(int generalDescriptionStringId) {
        this.generalDescriptionStringId = generalDescriptionStringId;
    }

    public int getStrengthStringId() {
        return strengthStringId;
    }

    protected void setStrengthStringId(int strengthStringId) {
        this.strengthStringId = strengthStringId;
    }

    public int getWeaknessStringId() {
        return weaknessStringId;
    }

    protected void setWeaknessStringId(int weaknessStringId) {
        this.weaknessStringId = weaknessStringId;
    }

    public int getSlapEffectId() {
        return slapEffectId;
    }

    protected void setSlapEffectId(int slapEffectId) {
        this.slapEffectId = slapEffectId;
    }

    public int getDeathEffectId() {
        return deathEffectId;
    }

    protected void setDeathEffectId(int deathEffectId) {
        this.deathEffectId = deathEffectId;
    }

    public Swipe getMelee1Swipe() {
        return melee1Swipe;
    }

    protected void setMelee1Swipe(Swipe melee1Swipe) {
        this.melee1Swipe = melee1Swipe;
    }

    public Swipe getMelee2Swipe() {
        return melee2Swipe;
    }

    protected void setMelee2Swipe(Swipe melee2Swipe) {
        this.melee2Swipe = melee2Swipe;
    }

    public short getUnk3d3() {
        return unk3d3;
    }

    protected void setUnk3d3(short unk3d3) {
        this.unk3d3 = unk3d3;
    }

    public Swipe getSpellSwipe() {
        return spellSwipe;
    }

    protected void setSpellSwipe(Swipe spellSwipe) {
        this.spellSwipe = spellSwipe;
    }

    public SpecialAbility getFirstPersonSpecialAbility1() {
        return firstPersonSpecialAbility1;
    }

    protected void setFirstPersonSpecialAbility1(SpecialAbility firstPersonSpecialAbility1) {
        this.firstPersonSpecialAbility1 = firstPersonSpecialAbility1;
    }

    public SpecialAbility getFirstPersonSpecialAbility2() {
        return firstPersonSpecialAbility2;
    }

    protected void setFirstPersonSpecialAbility2(SpecialAbility firstPersonSpecialAbility2) {
        this.firstPersonSpecialAbility2 = firstPersonSpecialAbility2;
    }

    public short[] getUnkf48() {
        return unkf48;
    }

    protected void setUnkf48(short[] unkf48) {
        this.unkf48 = unkf48;
    }

    public short getCreatureId() {
        return creatureId;
    }

    protected void setCreatureId(short creatureId) {
        this.creatureId = creatureId;
    }

    public short[] getUnk3ea() {
        return unk3ea;
    }

    protected void setUnk3ea(short[] unk3ea) {
        this.unk3ea = unk3ea;
    }

    public short getHungerFill() {
        return hungerFill;
    }

    protected void setHungerFill(short hungerFill) {
        this.hungerFill = hungerFill;
    }

    public short getUnhappyThreshold() {
        return unhappyThreshold;
    }

    protected void setUnhappyThreshold(short unhappyThreshold) {
        this.unhappyThreshold = unhappyThreshold;
    }

    public AttackType getMeleeAttackType() {
        return meleeAttackType;
    }

    protected void setMeleeAttackType(AttackType meleeAttackType) {
        this.meleeAttackType = meleeAttackType;
    }

    public short getUnk3eb2() {
        return unk3eb2;
    }

    protected void setUnk3eb2(short unk3eb2) {
        this.unk3eb2 = unk3eb2;
    }

    public short getLairObjectId() {
        return lairObjectId;
    }

    protected void setLairObjectId(short lairObjectId) {
        this.lairObjectId = lairObjectId;
    }

    public short getUnk3f1() {
        return unk3f1;
    }

    protected void setUnk3f1(short unk3f1) {
        this.unk3f1 = unk3f1;
    }

    public DeathFallDirection getDeathFallDirection() {
        return deathFallDirection;
    }

    protected void setDeathFallDirection(DeathFallDirection deathFallDirection) {
        this.deathFallDirection = deathFallDirection;
    }

    public short getUnk3f2() {
        return unk3f2;
    }

    protected void setUnk3f2(short unk3f2) {
        this.unk3f2 = unk3f2;
    }

    public String getSoundGategory() {
        return soundGategory;
    }

    protected void setSoundGategory(String soundGategory) {
        this.soundGategory = soundGategory;
    }

    public Material getMaterial() {
        return material;
    }

    protected void setMaterial(Material material) {
        this.material = material;
    }

    public ArtResource getFirstPersonFilterResource() {
        return firstPersonFilterResource;
    }

    protected void setFirstPersonFilterResource(ArtResource firstPersonFilterResource) {
        this.firstPersonFilterResource = firstPersonFilterResource;
    }

    public int getUnkfcb() {
        return unkfcb;
    }

    protected void setUnkfcb(int unkfcb) {
        this.unkfcb = unkfcb;
    }

    public int getUnk4() {
        return unk4;
    }

    protected void setUnk4(int unk4) {
        this.unk4 = unk4;
    }

    public ArtResource getRef3() {
        return ref3;
    }

    protected void setRef3(ArtResource ref3) {
        this.ref3 = ref3;
    }

    public Swipe getSpecial1Swipe() {
        return special1Swipe;
    }

    protected void setSpecial1Swipe(Swipe special1Swipe) {
        this.special1Swipe = special1Swipe;
    }

    public Swipe getSpecial2Swipe() {
        return special2Swipe;
    }

    protected void setSpecial2Swipe(Swipe special2Swipe) {
        this.special2Swipe = special2Swipe;
    }

    public ArtResource getFirstPersonMeleeResource() {
        return firstPersonMeleeResource;
    }

    protected void setFirstPersonMeleeResource(ArtResource firstPersonMeleeResource) {
        this.firstPersonMeleeResource = firstPersonMeleeResource;
    }

    public int getUnk6() {
        return unk6;
    }

    protected void setUnk6(int unk6) {
        this.unk6 = unk6;
    }

    public short getTortureHpChange() {
        return tortureHpChange;
    }

    protected void setTortureHpChange(short tortureHpChange) {
        this.tortureHpChange = tortureHpChange;
    }

    public short getTortureMoodChange() {
        return tortureMoodChange;
    }

    protected void setTortureMoodChange(short tortureMoodChange) {
        this.tortureMoodChange = tortureMoodChange;
    }

    public ArtResource[] getRef5() {
        return ref5;
    }

    protected void setRef5(ArtResource[] ref5) {
        this.ref5 = ref5;
    }

    public Unk7[] getUnk7() {
        return unk7;
    }

    protected void setUnk7(Unk7[] unk7) {
        this.unk7 = unk7;
    }

    public ArtResource getRef6() {
        return ref6;
    }

    protected void setRef6(ArtResource ref6) {
        this.ref6 = ref6;
    }

    public X1323[] getX1323() {
        return x1323;
    }

    protected void setX1323(X1323[] x1323) {
        this.x1323 = x1323;
    }

    public ArtResource[] getRef7() {
        return ref7;
    }

    protected void setRef7(ArtResource[] ref7) {
        this.ref7 = ref7;
    }

    public int getUniqueNameTextId() {
        return uniqueNameTextId;
    }

    protected void setUniqueNameTextId(int uniqueNameTextId) {
        this.uniqueNameTextId = uniqueNameTextId;
    }

    public int[] getX14e1() {
        return x14e1;
    }

    protected void setX14e1(int[] x14e1) {
        this.x14e1 = x14e1;
    }

    public int getFirstPersonSpecialAbility1Count() {
        return firstPersonSpecialAbility1Count;
    }

    protected void setFirstPersonSpecialAbility1Count(int firstPersonSpecialAbility1Count) {
        this.firstPersonSpecialAbility1Count = firstPersonSpecialAbility1Count;
    }

    public int getFirstPersonSpecialAbility2Count() {
        return firstPersonSpecialAbility2Count;
    }

    protected void setFirstPersonSpecialAbility2Count(int firstPersonSpecialAbility2Count) {
        this.firstPersonSpecialAbility2Count = firstPersonSpecialAbility2Count;
    }

    public ArtResource getUniqueResource() {
        return uniqueResource;
    }

    protected void setUniqueResource(ArtResource uniqueResource) {
        this.uniqueResource = uniqueResource;
    }

    public int getUnk1545() {
        return unk1545;
    }

    protected void setUnk1545(int unk1545) {
        this.unk1545 = unk1545;
    }

    public short[] getUnknownExtraBytes() {
        return unknownExtraBytes;
    }

    protected void setUnknownExtraBytes(short[] unknownExtraBytes) {
        this.unknownExtraBytes = unknownExtraBytes;
    }

    public EnumSet<CreatureFlag2> getFlags2() {
        return flags2;
    }

    protected void setFlags2(EnumSet<CreatureFlag2> flags2) {
        this.flags2 = flags2;
    }

    public int getUnknown() {
        return unknown;
    }

    protected void setUnknown(int unknown) {
        this.unknown = unknown;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(Creature o) {
        return Short.compare(orderInEditor, o.orderInEditor);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + this.creatureId;
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
        final Creature other = (Creature) obj;
        if (this.creatureId != other.creatureId) {
            return false;
        }
        return true;
    }

    public class Attraction {

        private int present; // dac
        private int roomId; // db0
        private int roomSize;

        public int getPresent() {
            return present;
        }

        protected void setPresent(int present) {
            this.present = present;
        }

        public int getRoomId() {
            return roomId;
        }

        protected void setRoomId(int roomId) {
            this.roomId = roomId;
        }

        public int getRoomSize() {
            return roomSize;
        }

        protected void setRoomSize(int roomSize) {
            this.roomSize = roomSize;
        }
    }

    public class Spell { // bytes in these structs might be product of padding

        private Vector3f shotOffset;
        private short x0c;
        private boolean playAnimation;
        private short x0e; // This is rather weird, this seems to change with the perception range value
        private short x0f;
        private float shotDelay;
        private short x14;
        private short x15;
        private short creatureSpellId;
        private short levelAvailable;

        public Vector3f getShotOffset() {
            return shotOffset;
        }

        protected void setShotOffset(Vector3f shotOffset) {
            this.shotOffset = shotOffset;
        }

        public short getX0c() {
            return x0c;
        }

        protected void setX0c(short x0c) {
            this.x0c = x0c;
        }

        public boolean isPlayAnimation() {
            return playAnimation;
        }

        protected void setPlayAnimation(boolean playAnimation) {
            this.playAnimation = playAnimation;
        }

        public short getX0e() {
            return x0e;
        }

        protected void setX0e(short x0e) {
            this.x0e = x0e;
        }

        public short getX0f() {
            return x0f;
        }

        protected void setX0f(short x0f) {
            this.x0f = x0f;
        }

        public float getShotDelay() {
            return shotDelay;
        }

        protected void setShotDelay(float shotDelay) {
            this.shotDelay = shotDelay;
        }

        public short getX14() {
            return x14;
        }

        protected void setX14(short x14) {
            this.x14 = x14;
        }

        public short getX15() {
            return x15;
        }

        protected void setX15(short x15) {
            this.x15 = x15;
        }

        public short getCreatureSpellId() {
            return creatureSpellId;
        }

        protected void setCreatureSpellId(short creatureSpellId) {
            this.creatureSpellId = creatureSpellId;
        }

        public short getLevelAvailable() {
            return levelAvailable;
        }

        protected void setLevelAvailable(short levelAvailable) {
            this.levelAvailable = levelAvailable;
        }
    }

    public class Resistance {

        private AttackType attackType;
        private short value;

        public AttackType getAttackType() {
            return attackType;
        }

        protected void setAttackType(AttackType attackType) {
            this.attackType = attackType;
        }

        public short getValue() {
            return value;
        }

        protected void setValue(short value) {
            this.value = value;
        }
    }

    public class JobPreference {

        private JobType jobType;
        private int moodChange;
        private int manaChange;
        private short chance; // of happening, percentage
        private short x09;
        private short x0a;
        private short x0b;

        public JobType getJobType() {
            return jobType;
        }

        protected void setJobType(JobType jobType) {
            this.jobType = jobType;
        }

        public int getMoodChange() {
            return moodChange;
        }

        protected void setMoodChange(int moodChange) {
            this.moodChange = moodChange;
        }

        public int getManaChange() {
            return manaChange;
        }

        protected void setManaChange(int manaChange) {
            this.manaChange = manaChange;
        }

        public short getChance() {
            return chance;
        }

        protected void setChance(short chance) {
            this.chance = chance;
        }

        public short getX09() {
            return x09;
        }

        protected void setX09(short x09) {
            this.x09 = x09;
        }

        public short getX0a() {
            return x0a;
        }

        protected void setX0a(short x0a) {
            this.x0a = x0a;
        }

        public short getX0b() {
            return x0b;
        }

        protected void setX0b(short x0b) {
            this.x0b = x0b;
        }
    }

    public class Xe7c {

        private int x00;
        private int x04;
        private int x06;

        public int getX00() {
            return x00;
        }

        protected void setX00(int x00) {
            this.x00 = x00;
        }

        public int getX04() {
            return x04;
        }

        protected void setX04(int x04) {
            this.x04 = x04;
        }

        public int getX06() {
            return x06;
        }

        protected void setX06(int x06) {
            this.x06 = x06;
        }
    }

    public class Xe94 {

        private long x00;
        private int x04;
        private int x08;

        public long getX00() {
            return x00;
        }

        protected void setX00(long x00) {
            this.x00 = x00;
        }

        public int getX04() {
            return x04;
        }

        protected void setX04(int x04) {
            this.x04 = x04;
        }

        public int getX08() {
            return x08;
        }

        protected void setX08(int x08) {
            this.x08 = x08;
        }
    }

    public class Unk7 {

        private int x00;
        private long x04;
        private int x08;

        public int getX00() {
            return x00;
        }

        protected void setX00(int x00) {
            this.x00 = x00;
        }

        public long getX04() {
            return x04;
        }

        protected void setX04(long x04) {
            this.x04 = x04;
        }

        public int getX08() {
            return x08;
        }

        protected void setX08(int x08) {
            this.x08 = x08;
        }
    }

    public class X1323 {

        private int x00;
        private int x02;

        public int getX00() {
            return x00;
        }

        protected void setX00(int x00) {
            this.x00 = x00;
        }

        public int getX02() {
            return x02;
        }

        protected void setX02(int x02) {
            this.x02 = x02;
        }
    }
}
