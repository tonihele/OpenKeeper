/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

/**
 * Container class for Shots.kwd<br>
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Shot implements Comparable<Shot> {

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
    private String name;
    private ArtResource meshResource;
    private short unknown1[]; // 32
    private float speed; // Fixed point int
    private int data1; // Unsigned short
    private short unknown2[]; // 10
    private float radius; // Fixed point int
    private short unknown3[]; // 20
    private int health; // Short
    private short shotId; // Byte..?
    private short unknown4[]; // 6
    private DamageType damageType; // Byte
    private CollideType collideType; // Byte
    private short unknown5[]; // 2
    private String soundCategory;
    private int threat; // Short
    private short unknown6[]; // 4

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

    public short[] getUnknown1() {
        return unknown1;
    }

    protected void setUnknown1(short[] unknown1) {
        this.unknown1 = unknown1;
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

    public short[] getUnknown2() {
        return unknown2;
    }

    protected void setUnknown2(short[] unknown2) {
        this.unknown2 = unknown2;
    }

    public float getRadius() {
        return radius;
    }

    protected void setRadius(float radius) {
        this.radius = radius;
    }

    public short[] getUnknown3() {
        return unknown3;
    }

    protected void setUnknown3(short[] unknown3) {
        this.unknown3 = unknown3;
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

    public short[] getUnknown4() {
        return unknown4;
    }

    protected void setUnknown4(short[] unknown4) {
        this.unknown4 = unknown4;
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

    public short[] getUnknown5() {
        return unknown5;
    }

    protected void setUnknown5(short[] unknown5) {
        this.unknown5 = unknown5;
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

    public short[] getUnknown6() {
        return unknown6;
    }

    protected void setUnknown6(short[] unknown6) {
        this.unknown6 = unknown6;
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
