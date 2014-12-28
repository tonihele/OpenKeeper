/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

import java.util.EnumSet;
import toniarts.opendungeonkeeper.tools.convert.map.Object;

/**
 * Container class for Objects.kwd
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Object implements Comparable<Object> {

    /**
     * Object flags
     */
    public enum ObjectFlag implements IFlagEnum {

        DIE_OVER_TIME(0x0000001),
        DIE_OVER_TIME_IF_NOT_IN_ROOM(0x0000002),
        OBJECT_TYPE_SPECIAL(0x0000004),
        OBJECT_TYPE_SPELL_BOOK(0x0000008),
        OBJECT_TYPE_CRATE(0x0000010),
        OBJECT_TYPE_LAIR(0x0000020),
        OBJECT_TYPE_GOLD(0x0000040),
        OBJECT_TYPE_FOOD(0x0000080),
        CAN_BE_PICKED_UP(0x0000100),
        CAN_BE_SLAPPED(0x0000200),
        DIE_WHEN_SLAPPED(0x0000400),
        OBJECT_TYPE_LEVEL_GEM(0x0001000),
        CAN_BE_DROPPED_ON_ANY_LAND(0x0002000),
        OBSTACLE(0x0004000),
        BOUNCE(0x0008000),
        BOULDER_CAN_ROLL_THROUGH(0x0010000),
        BOULDER_DESTROYS(0x0020000),
        PILLAR(0x0040000),
        DOOR_KEY(0x0100000),
        DAMAGEABLE(0x0200000),
        HIGHLIGHTABLE(0x0400000),
        PLACEABLE(0x0800000),
        FIRST_PERSON_OBSTACLE(0x1000000),
        SOLID_OBSTACLE(0x2000000),
        CAST_SHADOWS(0x4000000);
        private final long flagValue;

        private ObjectFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    };
//    struct ObjectBlock {
//        char name[32]; /* 0 */
//        ArtResource kMeshResource; /* 20 */
//        ArtResource kGuiIconResource; /* 74 */
//        ArtResource kInHandIconResource; /* c8 */
//        ArtResource kInHandMeshResource; /* 11c */
//        ArtResource kUnknownResource; /* 170 */
//        ArtResource kAdditionalResources[4]; /* 1c4 */
//        LightBlock light; /* 314 */
//        uint32_t width; /* 32c fixed 0x1000 */
//        uint32_t height; /* 330 */
//        uint32_t mass; /* 334 */
//        uint32_t unk1; /* 338 */
//        uint32_t unk2; /* 33c */
//        uint8_t material; /* 340 */
//        uint8_t unk3[3]; /* 341 */
//        uint32_t flags; /* 344 */
//        uint16_t hp; /* 348 */
//        uint16_t unk4;
//        uint16_t x34c;
//        uint16_t x34e;
//        uint16_t x350;
//        uint16_t x352; /* 34a */
//        uint16_t slap_effect; /* 354 */
//        uint16_t death_effect; /* 356 */
//        uint16_t unk5; /* 358 */
//        uint8_t id; /* 35a */
//        uint8_t unk6; /* 35b */
//        uint8_t room_capacity; /* 35c */
//        uint8_t unk7; /* 35d */
//        char sound_category[32]; /* 35e */
//        };
    private String name; // 0
    private ArtResource meshResource; // 20
    private ArtResource guiIconResource; // 74
    private ArtResource inHandIconResource; // c8
    private ArtResource inHandMeshResource; // 11c
    private ArtResource unknownResource; // 170
    private ArtResource additionalResources[]; // 1c4
    private Light light; // 314
    private float width; // 32c fixed 0x1000
    private float height; // 330
    private float mass; // 334
    private int unknown1; // 338
    private int unknown2; // 33c
    private Material material; // 340
    private short unknown3[]; // 341
    private EnumSet<ObjectFlag> flags; // 344
    private int hp; // 348
    private int unknown4;
    private int x34c;
    private int x34e;
    private int tooltipStringId;
    private int nameStringId; // 34a
    private int slapEffectId; // 354
    private int deathEffectId; // 356
    private int unknown5; // 358
    private short objectId; // 35a
    private short unknown6; // 35b
    private short roomCapacity; // 35c
    private short pickUpPriority; // 35d
    private String soundCategory; // 35e

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

    public ArtResource getGuiIconResource() {
        return guiIconResource;
    }

    protected void setGuiIconResource(ArtResource guiIconResource) {
        this.guiIconResource = guiIconResource;
    }

    public ArtResource getInHandIconResource() {
        return inHandIconResource;
    }

    protected void setInHandIconResource(ArtResource inHandIconResource) {
        this.inHandIconResource = inHandIconResource;
    }

    public ArtResource getInHandMeshResource() {
        return inHandMeshResource;
    }

    protected void setInHandMeshResource(ArtResource inHandMeshResource) {
        this.inHandMeshResource = inHandMeshResource;
    }

    public ArtResource getUnknownResource() {
        return unknownResource;
    }

    protected void setkUnknownResource(ArtResource unknownResource) {
        this.unknownResource = unknownResource;
    }

    public ArtResource[] getAdditionalResources() {
        return additionalResources;
    }

    protected void setAdditionalResources(ArtResource[] additionalResources) {
        this.additionalResources = additionalResources;
    }

    public Light getLight() {
        return light;
    }

    protected void setLight(Light light) {
        this.light = light;
    }

    public float getWidth() {
        return width;
    }

    protected void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    protected void setHeight(float height) {
        this.height = height;
    }

    public float getMass() {
        return mass;
    }

    protected void setMass(float mass) {
        this.mass = mass;
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

    public Material getMaterial() {
        return material;
    }

    protected void setMaterial(Material material) {
        this.material = material;
    }

    public short[] getUnknown3() {
        return unknown3;
    }

    protected void setUnknown3(short[] unknown3) {
        this.unknown3 = unknown3;
    }

    public EnumSet<ObjectFlag> getFlags() {
        return flags;
    }

    protected void setFlags(EnumSet<ObjectFlag> flags) {
        this.flags = flags;
    }

    public int getHp() {
        return hp;
    }

    protected void setHp(int hp) {
        this.hp = hp;
    }

    public int getUnknown4() {
        return unknown4;
    }

    protected void setUnknown4(int unknown4) {
        this.unknown4 = unknown4;
    }

    public int getX34c() {
        return x34c;
    }

    protected void setX34c(int x34c) {
        this.x34c = x34c;
    }

    public int getX34e() {
        return x34e;
    }

    protected void setX34e(int x34e) {
        this.x34e = x34e;
    }

    public int getTooltipStringId() {
        return tooltipStringId;
    }

    protected void setTooltipStringId(int tooltipStringId) {
        this.tooltipStringId = tooltipStringId;
    }

    public int getNameStringId() {
        return nameStringId;
    }

    protected void setNameStringId(int nameStringId) {
        this.nameStringId = nameStringId;
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

    public int getUnknown5() {
        return unknown5;
    }

    protected void setUnknown5(int unknown5) {
        this.unknown5 = unknown5;
    }

    public short getObjectId() {
        return objectId;
    }

    protected void setObjectId(short objectId) {
        this.objectId = objectId;
    }

    public short getUnknown6() {
        return unknown6;
    }

    protected void setUnknown6(short unknown6) {
        this.unknown6 = unknown6;
    }

    public short getRoomCapacity() {
        return roomCapacity;
    }

    protected void setRoomCapacity(short roomCapacity) {
        this.roomCapacity = roomCapacity;
    }

    public short getPickUpPriority() {
        return pickUpPriority;
    }

    protected void setPickUpPriority(short pickUpPriority) {
        this.pickUpPriority = pickUpPriority;
    }

    public String getSoundCategory() {
        return soundCategory;
    }

    protected void setSoundCategory(String soundCategory) {
        this.soundCategory = soundCategory;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(Object o) {
        return Short.compare(objectId, o.objectId);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + this.objectId;
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
        final Object other = (Object) obj;
        if (this.objectId != other.objectId) {
            return false;
        }
        return true;
    }
}
