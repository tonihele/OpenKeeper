/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

/**
 * Container class for Objects.kwd
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Object {

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
    private short material; // 340
    private short unknown3[]; // 341
    private int flags; // 344
    private int hp; // 348
    private int unknown4;
    private int x34c;
    private int x34e;
    private int x350;
    private int x352; // 34a
    private int slapEffect; // 354
    private int deathEffect; // 356
    private int unknown5; // 358
    private short objectId; // 35a
    private short unknown6; // 35b
    private short roomCapacity; // 35c
    private short unknown7; // 35d
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

    public short getMaterial() {
        return material;
    }

    protected void setMaterial(short material) {
        this.material = material;
    }

    public short[] getUnknown3() {
        return unknown3;
    }

    protected void setUnknown3(short[] unknown3) {
        this.unknown3 = unknown3;
    }

    public int getFlags() {
        return flags;
    }

    protected void setFlags(int flags) {
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

    public int getX350() {
        return x350;
    }

    protected void setX350(int x350) {
        this.x350 = x350;
    }

    public int getX352() {
        return x352;
    }

    protected void setX352(int x352) {
        this.x352 = x352;
    }

    public int getSlapEffect() {
        return slapEffect;
    }

    protected void setSlapEffect(int slapEffect) {
        this.slapEffect = slapEffect;
    }

    public int getDeathEffect() {
        return deathEffect;
    }

    protected void setDeathEffect(int deathEffect) {
        this.deathEffect = deathEffect;
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

    public short getUnknown7() {
        return unknown7;
    }

    protected void setUnknown7(short unknown7) {
        this.unknown7 = unknown7;
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
}
