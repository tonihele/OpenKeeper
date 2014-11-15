/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

import javax.vecmath.Vector3f;

/**
 * Container class for Creatures.kwd
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Creature {

    public enum AttackType {

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

        /**
         * Get the attack type with ID
         *
         * @param id the id in map file
         * @return AttackType
         */
        public static AttackType getValue(int id) {
            for (AttackType attackType : AttackType.values()) {
                if (attackType.id == id) {
                    return attackType;
                }
            }
            return AttackType.NONE;
        }
        private int id;
    }

    public enum JobType {

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

        /**
         * Get the job type with ID
         *
         * @param id the id in map file
         * @return JobType
         */
        public static JobType getValue(int id) {
            for (JobType jobType : JobType.values()) {
                if (jobType.id == id) {
                    return jobType;
                }
            }
            return JobType.NONE;
        }
        private int id;
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
    private short editorOrder; // cf6
    private int unk2c; // cf7
    private float shotDelay; // cf9
    private int olhiEffectId; // cfd, OLHI, wut?
    private int introductionStringId; // cff
    private int unkd01; // d01
    private int angerStringIdLair;
    private int angerStringIdFood;
    private int angerStringIdPay;
    private int unk2d[]; // d05
    private String unkd17; // d17
    private float shuffleSpeed; // d37
    private short cloneCreatureId;
    private short unk2e[]; // d3b
    private ArtResource ref2; // d40
    private Light light; // d94
    private Attraction attractions[];
    private int unkdbc; // dbc
    private int unkdc0; // dc0
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
    private int unk3ac; // ebc
    private int timeAwake; // ec0
    private int timeSleep; // ec4
    private int unkec8; // ec8
    private int unkecc; // ecc
    private int unked0; // ed0
    private int unked4; // ed4
    private int unked8; // ed8
    private int slapFearlessDuration; // edc
    private int unkee0; // ee0
    private int unkee4; // ee4
    private short possessionManaCost; // ee8
    private short ownLandHealthIncrease; // eea
    private int range; // eec
    private int unkef0; // ef0
    private int unk3af; // ef4
    private float meleeRecharge; // ef8
    private int unkefc; // efc
    private int expForNextLevel; // f00
    private short unk3b[]; // f02
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
    private short unk3d[]; // f42
    private short unkf45; // f45
    private short unk40[]; // f46
    private short unkf48[]; // f48
    private short creatureId; // f4b
    private short unk3ea[]; // f4c
    private short unhappyThreshold; // f4f
    private short unk3eb[]; // f50
    private short lairObjectId; // f52
    private short unk3f[]; // f53
    private String soundGategory; // f56
    private Material material; // f76, armour type
    private ArtResource reff77; // f77
    private int unkfcb; // fcb
    private int unk4; // fcd
    private ArtResource ref3; // fd1
    private short unk5[]; // 1025
    private ArtResource ref4; // 1027
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
    private int x14e9[]; // 14e9
    private ArtResource ref8; // 14f1
    private int unk1545;

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

    public short getEditorOrder() {
        return editorOrder;
    }

    protected void setEditorOrder(short editorOrder) {
        this.editorOrder = editorOrder;
    }

    public int getUnk2c() {
        return unk2c;
    }

    protected void setUnk2c(int unk2c) {
        this.unk2c = unk2c;
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

    public int getUnkd01() {
        return unkd01;
    }

    protected void setUnkd01(int unkd01) {
        this.unkd01 = unkd01;
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

    public int[] getUnk2d() {
        return unk2d;
    }

    protected void setUnk2d(int[] unk2d) {
        this.unk2d = unk2d;
    }

    public String getUnkd17() {
        return unkd17;
    }

    protected void setUnkd17(String unkd17) {
        this.unkd17 = unkd17;
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

    public short[] getUnk2e() {
        return unk2e;
    }

    protected void setUnk2e(short[] unk2e) {
        this.unk2e = unk2e;
    }

    public ArtResource getRef2() {
        return ref2;
    }

    protected void setRef2(ArtResource ref2) {
        this.ref2 = ref2;
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

    public int getUnkdbc() {
        return unkdbc;
    }

    protected void setUnkdbc(int unkdbc) {
        this.unkdbc = unkdbc;
    }

    public int getUnkdc0() {
        return unkdc0;
    }

    protected void setUnkdc0(int unkdc0) {
        this.unkdc0 = unkdc0;
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

    public int getUnk3ac() {
        return unk3ac;
    }

    protected void setUnk3ac(int unk3ac) {
        this.unk3ac = unk3ac;
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

    public int getUnkec8() {
        return unkec8;
    }

    protected void setUnkec8(int unkec8) {
        this.unkec8 = unkec8;
    }

    public int getUnkecc() {
        return unkecc;
    }

    protected void setUnkecc(int unkecc) {
        this.unkecc = unkecc;
    }

    public int getUnked0() {
        return unked0;
    }

    protected void setUnked0(int unked0) {
        this.unked0 = unked0;
    }

    public int getUnked4() {
        return unked4;
    }

    protected void setUnked4(int unked4) {
        this.unked4 = unked4;
    }

    public int getUnked8() {
        return unked8;
    }

    protected void setUnked8(int unked8) {
        this.unked8 = unked8;
    }

    public int getSlapFearlessDuration() {
        return slapFearlessDuration;
    }

    protected void setSlapFearlessDuration(int slapFearlessDuration) {
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

    public int getRange() {
        return range;
    }

    protected void setRange(int range) {
        this.range = range;
    }

    public int getUnkef0() {
        return unkef0;
    }

    protected void setUnkef0(int unkef0) {
        this.unkef0 = unkef0;
    }

    public int getUnk3af() {
        return unk3af;
    }

    protected void setUnk3af(int unk3af) {
        this.unk3af = unk3af;
    }

    public float getMeleeRecharge() {
        return meleeRecharge;
    }

    protected void setMeleeRecharge(float meleeRecharge) {
        this.meleeRecharge = meleeRecharge;
    }

    public int getUnkefc() {
        return unkefc;
    }

    protected void setUnkefc(int unkefc) {
        this.unkefc = unkefc;
    }

    public int getExpForNextLevel() {
        return expForNextLevel;
    }

    protected void setExpForNextLevel(int expForNextLevel) {
        this.expForNextLevel = expForNextLevel;
    }

    public short[] getUnk3b() {
        return unk3b;
    }

    protected void setUnk3b(short[] unk3b) {
        this.unk3b = unk3b;
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

    public short[] getUnk3d() {
        return unk3d;
    }

    protected void setUnk3d(short[] unk3d) {
        this.unk3d = unk3d;
    }

    public short getUnkf45() {
        return unkf45;
    }

    protected void setUnkf45(short unkf45) {
        this.unkf45 = unkf45;
    }

    public short[] getUnk40() {
        return unk40;
    }

    protected void setUnk40(short[] unk40) {
        this.unk40 = unk40;
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

    public short getUnhappyThreshold() {
        return unhappyThreshold;
    }

    protected void setUnhappyThreshold(short unhappyThreshold) {
        this.unhappyThreshold = unhappyThreshold;
    }

    public short[] getUnk3eb() {
        return unk3eb;
    }

    protected void setUnk3eb(short[] unk3eb) {
        this.unk3eb = unk3eb;
    }

    public short getLairObjectId() {
        return lairObjectId;
    }

    protected void setLairObjectId(short lairObjectId) {
        this.lairObjectId = lairObjectId;
    }

    public short[] getUnk3f() {
        return unk3f;
    }

    protected void setUnk3f(short[] unk3f) {
        this.unk3f = unk3f;
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

    public ArtResource getReff77() {
        return reff77;
    }

    protected void setReff77(ArtResource reff77) {
        this.reff77 = reff77;
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

    public short[] getUnk5() {
        return unk5;
    }

    protected void setUnk5(short[] unk5) {
        this.unk5 = unk5;
    }

    public ArtResource getRef4() {
        return ref4;
    }

    protected void setRef4(ArtResource ref4) {
        this.ref4 = ref4;
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

    public int[] getX14e9() {
        return x14e9;
    }

    protected void setX14e9(int[] x14e9) {
        this.x14e9 = x14e9;
    }

    public ArtResource getRef8() {
        return ref8;
    }

    protected void setRef8(ArtResource ref8) {
        this.ref8 = ref8;
    }

    public int getUnk1545() {
        return unk1545;
    }

    protected void setUnk1545(int unk1545) {
        this.unk1545 = unk1545;
    }

    @Override
    public String toString() {
        return name;
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
        private short x0e;
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
        private int moodChange; // not tested
        private int manaChange; // not tested
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

        private int x00;
        private int x04;
        private int x08;

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

        public int getX08() {
            return x08;
        }

        protected void setX08(int x08) {
            this.x08 = x08;
        }
    }

    public class Unk7 {

        private int x00;
        private int x04;
        private int x08;

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
