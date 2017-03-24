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
import java.util.HashMap;
import java.util.List;
import javax.vecmath.Vector3f;
import toniarts.openkeeper.tools.convert.IFlagEnum;
import toniarts.openkeeper.tools.convert.IValueEnum;

/**
 * Container class for Creatures.kwd
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Creature implements Comparable<Creature> {

    public static enum AnimationType {
        WALK,
        RUN,
        BACK_OFF,
        STAND_STILL,
        STEALTH_WALK,
        DEATH_POSE,
        RECOIL_FORWARDS,
        RECOIL_BACKWARDS,
        FALLBACK, // FALL_OVER_AND_UP
        MELEE_ATTACK,
        CAST_SPELL,
        GET_UP,
        IDLE_1,
        IDLE_2,
        DIE,
        HAPPY,
        ANGRY,
        STUNNED,
        IN_HAND,
        DANCE,
        DRUNK,
        ENTRANCE,
        SLEEPING,
        EATING,
        RESEARCHING,
        MANUFACTURING,
        TRAINING,
//        IN_PRISON,
        DRINKING,
        PRAYING,
//        GAMBLING,
        TORTURED_WHEEL,
        TORTURED_CHAIR,
        TORTURED_CHAIR_SKELETON,
        SPECIAL_1,
        SPECIAL_2,
        DRUNKED_WALK,
        DRUNKED_IDLE,
//        FIGHT_VICTORY,
//        FIGHT_IDLE,
        DRAGGED,

        DIG, // Imp
        SWIPE,
        IDLE_3, // Imp
        IDLE_4, // Imp
        IDLE_3_1, // same as IDLE_3
        IDLE_4_1, // same as IDLE_4
        ROAR, // ROAR in Horny, LOOT in Rogue
        NULL_1, // always null
        NULL_2, // always null
        NULL_3, // always null
        NULL_4, // always null
    }

    public static enum OffsetType {
        PORTAL_ENTRANCE,
        PRAYING,
        FALL_BACK_GET_UP,
        CORPSE,
        OFFSET_5,
        OFFSET_6,
        OFFSET_7,
        OFFSET_8,
    }

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
        UNKNOWN_1(0x40), // FIXME unknown flag. In Troll
        IS_EVIL(128), // Obviously otherwise it is good
        UNKNOWN_2(256), // FIXME unknown flag
        IS_IMMUNE_TO_TURNCOAT(512),
        AVAILABLE_VIA_PORTAL(1024),
        UNKNOWN_3(0x800), // FIXME unknown flag. In Imp
        UNKNOWN_4(0x1000), // FIXME unknown flag
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
        UNKNOWN_5(0x20000000), // FIXME unknown flag. In Imp
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

    public enum CreatureFlag3 implements IFlagEnum {
	UNK_1(1),
	UNK_2(2),
	UNK_4(4),
	UNK_8(8);

        private final long flagValue;

        private CreatureFlag3(long flagValue) {
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
        MISTRESS_LIGHTNING(4),
        GAS_CLOUD(5),
        GAS_MISSILE(6),
        DARK_ELF_GUIDED_BOLT(7),
        SLOW(8),
        GRENADE(9),
        DRAIN(10),
        MISTRESS_HAIL_STORM(11),
        DARK_ELF_ARROW(12),
        HASTE_CREATURE(13),
        WARLOCK_HEAL_CREATURE(14),
        MELEE_SCYTHE(15),
        INVULNERABLE(16),
        THIEF_INVISIBLE(17),
        KNIVES(18),
        DISRUPTION(19),
        RAISE_DEAD(20),
        WIND(21),
        SKELETON_ARMY(22),
        CHICKEN_ARROW(23),
        ELF_ARCHER_ARROW(24),
        WIZARD_FIREBALL(25),
        SALAMANDER_FIREBALL(26),
        WIZARD_FIREBOMB_1(27),
        WIZARD_FIREBOMB_2(28),
        FAIRY_FREEZE(29),
        ELVEN_ARCHER_GUIDED_BOLT(30),
        DARK_ANGLE_HAIL_STORM(31),
        MONK_HEAL_CREATURE(32),
        ROGUE_INVISIBLE(33),
        FAIRY_LIGHTNING(34),
        DARK_ANGLE_FIREBOMB(35),
        IMP_TELEPORT(36),
        IMP_HASTE(37),
        REAPER_FIREBALL(38),
        SALAMANDER_SPIT(39),
        MAIDEN_WEB(40),
        MAIDEN_POISON_SPIT(41);
        // WARLOCK_FIREBALL(), // FIXME bug in editor? because it is a spell
        // WARLOCK_FIREBOMB(),
        // MISTRESS_FREEZE(),
        // CAST_ARMOUR(),

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
        DESTROY_WALLS(12),
        STEAL_GOLD(13),
        STEAL_SPELLS(14),
        SULK(15),
        REBEL(16),
        STEAL_MANUFACTURE_CRATES(17),
        KILL_CREATURES(18),
        KILL_PLAYER(19),
        TUNNELLING(20), // FIXME or 21 or both
        WAIT(22),
        SEND_TO_ACTION_POINT(23),
        EXPLORE(24),
        STEAL_ENEMY_GOLD(25),
        COMBAT_PIT_SPECTATE(26),
        JAIL_BREAK(27),
        TOLLING(28);

        private JobType(int id) {
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
        RIGHT_KING_FGBS_SLASH(22),
        UNK_36(36); // FIXME

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

    private String name; // 0
    private byte[] unknown1Resource;
    private final HashMap<AnimationType, ArtResource> animation = new HashMap<>(48);
    private final Attributes attributes = new Attributes();
    private ArtResource icon1Resource;
    private ArtResource icon2Resource;
    private int unkcec; // cec
    private int unkcee; // cee
    private int unkcf2; // cf2
    private short orderInEditor; // cf6
    private int angerStringIdGeneral; // cf7
    private float shotDelay; // cf9
    private int olhiEffectId; // cfd, OLHI, wut?
    private int introductionStringId; // cff
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
    private List<Spell> spells;
    private Resistance resistances[];
    private JobPreference happyJobs[];
    private JobPreference unhappyJobs[];
    private JobPreference angryJobs[];
    private JobType hateJobs[];
    private JobAlternative alternativeJobs[];
    private int unkea0; // ea0
    private float unkea8; // ea8
    private int unk3ab; // eac
    private int unkee0; // ee0
    private int unkee4; // ee4
    private float meleeRange; // eec
    private int unkef0; // ef0
    private float meleeRecharge; // ef8
    private EnumSet<CreatureFlag> flags; // efc
    private JobClass jobClass; // f02
    private FightStyle fightStyle;
    private int meleeDamage; // f14
    private int unk3cb; // f1a
    private float unk3cc; // f20
    private int nameStringId;
    private int tooltipStringId;
    private int entranceEffectId; // f36
    private int generalDescriptionStringId; // f3c
    private int strengthStringId;
    private int weaknessStringId;
    private int slapEffectId; // f3e
    private int deathEffectId; // f40
    private Swipe melee1Swipe; // f42, Swipes, 1st person attacks
    private Swipe melee2Swipe;
    private Swipe melee3Swipe;
    private Swipe spellSwipe; // f45
    private SpecialAbility firstPersonSpecialAbility1; // f46, Special abilities, 1st person
    private SpecialAbility firstPersonSpecialAbility2;
    private short unkf48[]; // f48
    private short creatureId; // f4b
    private short unk3ea[]; // f4c
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
    private Swipe special1Swipe; // 1025
    private Swipe special2Swipe;
    private ArtResource firstPersonMeleeResource; // 1027
    private int unk6; // 107b
    private final HashMap<OffsetType, Vector3f> animationOffsets = new HashMap<>(8);
    private X1323 x1323[];
    private int uniqueNameTextId;
    private int x14e1[]; // 14e1
    private int firstPersonSpecialAbility1Count; // 14e9, available uses or something, not really sure
    private int firstPersonSpecialAbility2Count;
    private ArtResource uniqueResource; // 14f1
    private EnumSet<CreatureFlag3> flags3;
    // When the file is embedded in the globals, there is some extra stuff
    private short unknownExtraBytes[]; // 80
    private EnumSet<CreatureFlag2> flags2; // ???
    private int unknown;
    private float unknown_1;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public byte[] getUnknown1Resource() {
        return unknown1Resource;
    }

    protected void setUnknown1Resource(byte[] unknown1Resource) {
        this.unknown1Resource = unknown1Resource;
    }

    protected void setAnimation(AnimationType type, ArtResource resource) {
        animation.put(type, resource);
    }

    public ArtResource getAnimation(AnimationType type) {
        return animation.get(type);
    }

    public HashMap<AnimationType, ArtResource> getAnimations() {
        return animation;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    /**
     * @deprecated use getAnimation(AnimationType.WALK)
     * @return
     */
    public ArtResource getAnimWalkResource() {
        return animation.get(AnimationType.WALK);
    }

    /**
     * @deprecated use getAnimation(AnimationType.BEING_DRAGGED)
     * @return
     */
    public ArtResource getAnimDraggedPoseResource() {
        return animation.get(AnimationType.DRAGGED);
    }

    /**
     * @deprecated use getAnimation(AnimationType.MELEE_1)
     * @return
     */
    public ArtResource getAnimMelee1Resource() {
        return animation.get(AnimationType.MELEE_ATTACK);
    }

    /**
     * @deprecated use getAnimation(AnimationType.CAST_SPELL)
     * @return
     */
    public ArtResource getAnimMagicResource() {
        return animation.get(AnimationType.CAST_SPELL);
    }

    /**
     * @deprecated use getAnimation(AnimationType.DIE)
     * @return
     */
    public ArtResource getAnimDieResource() {
        return animation.get(AnimationType.DIE);
    }

    /**
     * @deprecated use getAnimation(AnimationType.IN_HAND)
     * @return
     */
    public ArtResource getAnimInHandResource() {
        return animation.get(AnimationType.IN_HAND);
    }

    /**
     * @deprecated use getAnimation(AnimationType.SLEEPING)
     * @return
     */
    public ArtResource getAnimSleepResource() {
        return animation.get(AnimationType.SLEEPING);
    }

    /**
     * @deprecated use getAnimation(AnimationType.EATING)
     * @return
     */
    public ArtResource getAnimEatResource() {
        return animation.get(AnimationType.EATING);
    }

    /**
     * @deprecated use getAnimation(AnimationType.RESEARCHING)
     * @return
     */
    public ArtResource getAnimResearchResource() {
        return animation.get(AnimationType.RESEARCHING);
    }

    /**
     * @deprecated use getAnimation(AnimationType.IDLE_1)
     * @return
     */
    public ArtResource getAnimIdle1Resource() {
        return animation.get(AnimationType.IDLE_1);
    }

    /**
     * @deprecated use getAnimation(AnimationType.FALL_OVER_AND_UP)
     * @return
     */
    public ArtResource getAnimFallbackResource() {
        return animation.get(AnimationType.FALLBACK);
    }

    /**
     * @deprecated use getAnimation(AnimationType.GET_UP)
     * @return
     */
    public ArtResource getAnimGetUpResource() {
        return animation.get(AnimationType.GET_UP);
    }

    /**
     * @deprecated use getAnimation(AnimationType.ENTRANCE)
     * @return
     */
    public ArtResource getAnimEntranceResource() {
        return animation.get(AnimationType.ENTRANCE);
    }

    /**
     * @deprecated use getAnimation(AnimationType.IDLE_2)
     * @return
     */
    public ArtResource getAnimIdle2Resource() {
        return animation.get(AnimationType.IDLE_2);
    }
    
    /**
     * @deprecated use getAnimation(AnimationType.MELEE_2)
     * @return
     */
    public ArtResource getAnimMelee2Resource() {
        return animation.get(AnimationType.SWIPE);
    }
    
    /**
     * @deprecated use getAnimation(AnimationType.DRUNKED_IDLE)
     * @return
     */
    public ArtResource getDrunkIdle() {
        return animation.get(AnimationType.DRUNKED_IDLE);
    }

    /**
     * @deprecated use getAnimation(AnimationType.BACK_OFF)
     * @return
     */
    public ArtResource getAnimWalkbackResource() {
        return animation.get(AnimationType.BACK_OFF);
    }
    
    /**
     * @deprecated use getAnimation(AnimationType.STAND_STILL)
     * @return
     */
    public ArtResource getAnimPoseFrameResource() {
        return animation.get(AnimationType.STAND_STILL);
    }

    /**
     * @deprecated use getAnimation(AnimationType.STEALTH_WALK)
     * @return
     */
    public ArtResource getAnimWalk2Resource() {
        return animation.get(AnimationType.STEALTH_WALK);
    }

    /**
     * @deprecated use getAnimation(AnimationType.DEATH_POSE)
     * @return
     */
    public ArtResource getAnimDiePoseResource() {
        return animation.get(AnimationType.DEATH_POSE);
    }

    public ArtResource getIcon1Resource() {
        return icon1Resource;
    }

    protected void setIcon1Resource(ArtResource icon1Resource) {
        this.icon1Resource = icon1Resource;
    }

    public ArtResource getIcon2Resource() {
        return icon2Resource;
    }

    protected void setIcon2Resource(ArtResource icon2Resource) {
        this.icon2Resource = icon2Resource;
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

    public List<Spell> getSpells() {
        return spells;
    }

    protected void setSpells(List<Spell> spells) {
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

    public JobAlternative[] getAlternativeJobs() {
        return alternativeJobs;
    }

    protected void setAlternativeJobs(JobAlternative[] alternativeJobs) {
        this.alternativeJobs = alternativeJobs;
    }

    public int getUnkea0() {
        return unkea0;
    }

    protected void setUnkea0(int unkea0) {
        this.unkea0 = unkea0;
    }

    public float getUnkea8() {
        return unkea8;
    }

    protected void setUnkea8(float unkea8) {
        this.unkea8 = unkea8;
    }

    public int getUnk3ab() {
        return unk3ab;
    }

    protected void setUnk3ab(int unk3ab) {
        this.unk3ab = unk3ab;
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

    public int getMeleeDamage() {
        return meleeDamage;
    }

    protected void setMeleeDamage(int meleeDamage) {
        this.meleeDamage = meleeDamage;
    }

    public int getUnk3cb() {
        return unk3cb;
    }

    protected void setUnk3cb(int unk3cb) {
        this.unk3cb = unk3cb;
    }

    public float getUnk3cc() {
        return unk3cc;
    }

    protected void setUnk3cc(float unk3cc) {
        this.unk3cc = unk3cc;
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

    public Swipe getMelee3Swipe() {
        return melee3Swipe;
    }

    protected void setMelee3Swipe(Swipe melee3Swipe) {
        this.melee3Swipe = melee3Swipe;
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

    public Vector3f getAnimationOffsets(OffsetType type) {
        return animationOffsets.get(type);
    }

    protected void setAnimationOffsets(OffsetType type, float x, float y, float z) {
        this.animationOffsets.put(type, new Vector3f(x, y, z));
    }

    public X1323[] getX1323() {
        return x1323;
    }

    protected void setX1323(X1323[] x1323) {
        this.x1323 = x1323;
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

    public EnumSet<CreatureFlag3> getFlags3() {
        return flags3;
    }

    protected void setFlags3(EnumSet<CreatureFlag3> flags3) {
        this.flags3 = flags3;
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

    public float getUnknown_1() {
        return unknown_1;
    }

    protected void setUnknown_1(float unknown) {
        this.unknown_1 = unknown;
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

        protected void setShotOffset(float x, float y, float z) {
            this.shotOffset = new Vector3f(x, y, z);
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

    public class JobAlternative {

        private JobType jobType;
        private int moodChange;
        private int manaChange;

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

    public class Attributes {
        private int expForNextLevel; // f00
        private int expPerSecond; // f04
        private int expPerSecondTraining; // f06
        private int researchPerSecond; // researchPointsPerSecond
        private int manufacturePerSecond; // manufacturePointsPerSecond
        private float height; // Tiles
        private float eyeHeight; // Tiles
        private int hp; // health
        private int hpFromChicken; // healthFromChicken
        private int fear; // f10
        private int threat; // f12
        private int manaGenPrayer; // manaGeneratedByPrayer
        private int pay; // f1c
        private int maxGoldHeld; // f1e
        private float speed; // Tiles Per Second
        private float runSpeed; // Tiles Per Second
        private float shuffleSpeed; // d37
        private float distanceCanSee; // Tiles
        private float distanceCanHear; // Tiles
        private int slapDamage; // f16
        private float hungerRate; // ebc
        private short hungerFill;
        private int timeAwake; // Seconds
        private int timeSleep; // sleepDuration Seconds ?
        private short unhappyThreshold; // moodUnhappyThreshold
        private int decomposeValue; // f22

        private short angerNoLair; // f28
        private short angerNoFood; // f2a
        private short angerNoPay; // f2c
        private short angerNoWork; // f2e
        private short angerSlap; // f30
        private short angerInHand; // f32
        private short initialGoldHeld; // f34
        private float tortureTimeToConvert; // Seconds

        private float stunDuration; // Seconds
        private float guardDuration; // Seconds
        private float idleDuration; // Seconds
        private float slapFearlessDuration; // Seconds

        private short possessionManaCost; // Per Second
        private short ownLandHealthIncrease; // Per Second
        private short tortureHpChange; // Per Second
        private short tortureMoodChange; // Per Second
        private float perceptionRange; // Fog Of War

        public float getPerceptionRange() {
            return perceptionRange;
        }

        protected void setPerceptionRange(float perceptionRange) {
            this.perceptionRange = perceptionRange;
        }

        public float getShuffleSpeed() {
            return shuffleSpeed;
        }

        protected void setShuffleSpeed(float shuffleSpeed) {
            this.shuffleSpeed = shuffleSpeed;
        }

        public float getHeight() {
            return height;
        }

        protected void setHeight(float height) {
            this.height = height;
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

        public float getTortureTimeToConvert() {
            return tortureTimeToConvert;
        }

        protected void setTortureTimeToConvert(float tortureTimeToConvert) {
            this.tortureTimeToConvert = tortureTimeToConvert;
        }

        public int getExpForNextLevel() {
            return expForNextLevel;
        }

        protected void setExpForNextLevel(int expForNextLevel) {
            this.expForNextLevel = expForNextLevel;
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

        public int getDecomposeValue() {
            return decomposeValue;
        }

        protected void setDecomposeValue(int decomposeValue) {
            this.decomposeValue = decomposeValue;
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
    }
}
