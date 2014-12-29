/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

import java.util.EnumSet;

/**
 * Container class for Shots.kwd<br>
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Shot implements Comparable<Shot> {

    /**
     * Shot flags
     */
    public enum ShotFlag implements IFlagEnum {

        DIE_WHEN_HIT_SOLID(0x0001),
        DIE_WHEN_HIT_THING(0x0002),
        DIE_OVER_TIME(0x0004),
        BOUNCE(0x0008),
        THROWN_PROJECTILE(0x0010),
        MOVE_TOWARDS_TARGET(0x0020),
        KILL_EFFECTS_WHEN_DEAD(0x0040),
        PASS_ON_SHOT_VELOCITY_TO_CREATION_EFFECT(0x0080),
        RECOIL(0x0100);
        private final long flagValue;

        private ShotFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    };

    /**
     * Shot process flags
     */
    public enum ShotProcessFlag implements IFlagEnum {

        NO_PROCESSING(0x0001), // ??
        CONSTANT(0x0002),
        HIT_COLLIDE_THING(0x0004),
        HIT_SOLID(0x0008),
        HIT_WATER(0x0010),
        HIT_LAVA(0x0020),
        AT_START(0x0040),
        AT_END(0x0080),
        HIT_THING(0x0100);
        private final long flagValue;

        private ShotProcessFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    };

    public enum DamageType implements IValueEnum {

        NONE(0),
        CONSTANT(1),
        RANGE_FROM_ORIGIN(2),
        TIME(3),
        RING_CONSTANT(4),
        AT_START(5),
        AT_END(6),
        HIT_THING(7),
        CONSTANT_FOR_TREMOR(8);

        private DamageType(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private int id;
    }

    public enum CollideType implements IValueEnum {

        NONE(0),
        OWN_CREATURES(1),
        ENEMY_CREATURES(2),
        ALL_CREATURES(3),
        DEAD_BODIES(4);

        private CollideType(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private int id;
    }

    public enum ProcessType implements IValueEnum {

        NONE(0),
        CREATE_CREATURE(1),
        CREATE_OBJECT(2),
        MODIFY_HEALTH(3),
        MODIFY_SPEED(4),
        MODIFY_ARMOUR(5),
        MODIFY_MANA(6),
        MODIFY_EXPERIENCE_LEVEL(7),
        MODIFY_HUNGER(8),
        LIGHTNING(9),
        INVUNERABLE(10),
        FREEZE(11),
        ALARM(12),
        TURNCOAT(13),
        CONCEAL(14),
        CHICKEN(15),
        SCAPEGOAT(16),
        WIND(17),
        RAISE_DEAD(18),
        OBEY(19),
        INVISIBLE(20),
        FEAR(21),
        BOULDER(22),
        ALERT(23),
        MESMERISE(24),
        SIGHT_OF_EVIL(25),
        POSSESS_CREATURE(26),
        TREMOR(27),
        GUIDED(28),
        DECKEM(29),
        INFERNO(30),
        GAS(31),
        TRIGGER(32),
        SUMMON_REAPER(33),
        INSTANTANEOUS_HIT(34),
        DRAIN(35),
        CREATE_SINGLE_CREATURE(36),
        WEB_AREA_EFFECT(37),
        POISON_SPIT(38),
        JACK_IN_THE_BOX(39);

        private ProcessType(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private int id;
    }

    public enum AttackCategory implements IValueEnum {

        FIRE(4),
        MOVEMENT(5),
        LIGHTNING(6),
        GAS(7),
        PROJECTILE(8),
        ENCHANCE_OTHER(9),
        ENCHANCE_SELF(10),
        CLOSE_COMBAT(11),
        GENERATION(12),
        PHYSICAL_TRAP(13),
        NON_LETHAL_TRAP(14),
        MELEE_SCYTHE(15);

        private AttackCategory(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private int id;
    }
    private String name;
    private ArtResource meshResource;
    private Light light;
    private float airFriction;
    private float mass;
    private float speed; // Fixed point int
    private int data1; // Unsigned short
    private short unknown1[]; // 2
    private int data2; // Unsigned short
    private short unknown2[]; // 2
    private EnumSet<ShotProcessFlag> shotProcessFlags; // Int
    private float radius; // Fixed point int
    private EnumSet<ShotFlag> flags; // Int
    private int generalEffectId;
    private int creationEffectId;
    private int deathEffectId;
    private int timedEffectId;
    private int hitSolidEffectId;
    private int hitLavaEffectId;
    private int hitWaterEffect;
    private int hitThingEffectId;
    private int health; // Short
    private short shotId; // Byte
    private short deathShotId;
    private short timedDelay; // For timed effect
    private short hitSolidShotId;
    private short hitLavaShotId;
    private short hitWaterShotId;
    private short hitThingShotId;
    private DamageType damageType; // Byte
    private CollideType collideType; // Byte
    private ProcessType processType; // Byte
    private AttackCategory attackCategory; // Byte
    private String soundCategory;
    private int threat; // Short
    private float burnDuration;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public ArtResource getMeshResource() {
        return meshResource;
    }

    protected void setMeshResource(ArtResource meshResource) {
        this.meshResource = meshResource;
    }

    public Light getLight() {
        return light;
    }

    protected void setLight(Light light) {
        this.light = light;
    }

    public float getAirFriction() {
        return airFriction;
    }

    protected void setAirFriction(float airFriction) {
        this.airFriction = airFriction;
    }

    public float getMass() {
        return mass;
    }

    protected void setMass(float mass) {
        this.mass = mass;
    }

    public float getSpeed() {
        return speed;
    }

    protected void setSpeed(float speed) {
        this.speed = speed;
    }

    public int getData1() {
        return data1;
    }

    protected void setData1(int data1) {
        this.data1 = data1;
    }

    public short[] getUnknown1() {
        return unknown1;
    }

    protected void setUnknown1(short[] unknown1) {
        this.unknown1 = unknown1;
    }

    public int getData2() {
        return data2;
    }

    protected void setData2(int data2) {
        this.data2 = data2;
    }

    public short[] getUnknown2() {
        return unknown2;
    }

    protected void setUnknown2(short[] unknown2) {
        this.unknown2 = unknown2;
    }

    public EnumSet<ShotProcessFlag> getShotProcessFlags() {
        return shotProcessFlags;
    }

    protected void setShotProcessFlags(EnumSet<ShotProcessFlag> shotProcessFlags) {
        this.shotProcessFlags = shotProcessFlags;
    }

    public float getRadius() {
        return radius;
    }

    protected void setRadius(float radius) {
        this.radius = radius;
    }

    public EnumSet<ShotFlag> getFlags() {
        return flags;
    }

    protected void setFlags(EnumSet<ShotFlag> flags) {
        this.flags = flags;
    }

    public int getGeneralEffectId() {
        return generalEffectId;
    }

    protected void setGeneralEffectId(int generalEffectId) {
        this.generalEffectId = generalEffectId;
    }

    public int getCreationEffectId() {
        return creationEffectId;
    }

    protected void setCreationEffectId(int creationEffectId) {
        this.creationEffectId = creationEffectId;
    }

    public int getDeathEffectId() {
        return deathEffectId;
    }

    protected void setDeathEffectId(int deathEffectId) {
        this.deathEffectId = deathEffectId;
    }

    public int getTimedEffectId() {
        return timedEffectId;
    }

    protected void setTimedEffectId(int timedEffectId) {
        this.timedEffectId = timedEffectId;
    }

    public int getHitSolidEffectId() {
        return hitSolidEffectId;
    }

    protected void setHitSolidEffectId(int hitSolidEffectId) {
        this.hitSolidEffectId = hitSolidEffectId;
    }

    public int getHitLavaEffectId() {
        return hitLavaEffectId;
    }

    protected void setHitLavaEffectId(int hitLavaEffectId) {
        this.hitLavaEffectId = hitLavaEffectId;
    }

    public int getHitWaterEffect() {
        return hitWaterEffect;
    }

    protected void setHitWaterEffect(int hitWaterEffect) {
        this.hitWaterEffect = hitWaterEffect;
    }

    public int getHitThingEffectId() {
        return hitThingEffectId;
    }

    protected void setHitThingEffectId(int hitThingEffectId) {
        this.hitThingEffectId = hitThingEffectId;
    }

    public int getHealth() {
        return health;
    }

    protected void setHealth(int health) {
        this.health = health;
    }

    public short getShotId() {
        return shotId;
    }

    protected void setShotId(short shotId) {
        this.shotId = shotId;
    }

    public short getDeathShotId() {
        return deathShotId;
    }

    protected void setDeathShotId(short deathShotId) {
        this.deathShotId = deathShotId;
    }

    public short getTimedDelay() {
        return timedDelay;
    }

    protected void setTimedDelay(short timedDelay) {
        this.timedDelay = timedDelay;
    }

    public short getHitSolidShotId() {
        return hitSolidShotId;
    }

    protected void setHitSolidShotId(short hitSolidShotId) {
        this.hitSolidShotId = hitSolidShotId;
    }

    public short getHitLavaShotId() {
        return hitLavaShotId;
    }

    protected void setHitLavaShotId(short hitLavaShotId) {
        this.hitLavaShotId = hitLavaShotId;
    }

    public short getHitWaterShotId() {
        return hitWaterShotId;
    }

    protected void setHitWaterShotId(short hitWaterShotId) {
        this.hitWaterShotId = hitWaterShotId;
    }

    public short getHitThingShotId() {
        return hitThingShotId;
    }

    protected void setHitThingShotId(short hitThingShotId) {
        this.hitThingShotId = hitThingShotId;
    }

    public DamageType getDamageType() {
        return damageType;
    }

    protected void setDamageType(DamageType damageType) {
        this.damageType = damageType;
    }

    public CollideType getCollideType() {
        return collideType;
    }

    protected void setCollideType(CollideType collideType) {
        this.collideType = collideType;
    }

    public ProcessType getProcessType() {
        return processType;
    }

    protected void setProcessType(ProcessType processType) {
        this.processType = processType;
    }

    public AttackCategory getAttackCategory() {
        return attackCategory;
    }

    protected void setAttackCategory(AttackCategory attackCategory) {
        this.attackCategory = attackCategory;
    }

    public String getSoundCategory() {
        return soundCategory;
    }

    protected void setSoundCategory(String soundCategory) {
        this.soundCategory = soundCategory;
    }

    public int getThreat() {
        return threat;
    }

    protected void setThreat(int threat) {
        this.threat = threat;
    }

    public float getBurnDuration() {
        return burnDuration;
    }

    protected void setBurnDuration(float burnDuration) {
        this.burnDuration = burnDuration;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(Shot o) {
        return Short.compare(shotId, o.shotId);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + this.shotId;
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
        final Shot other = (Shot) obj;
        if (this.shotId != other.shotId) {
            return false;
        }
        return true;
    }
}
