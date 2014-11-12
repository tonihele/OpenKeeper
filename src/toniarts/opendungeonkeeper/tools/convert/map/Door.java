/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

/**
 * Stub for the container class for the Doors.kwd
 *
 *
 * @author Wizand Petteri Loisko petteri.loisko@gmail.com
 *
 * Thank you https://github.com/werkt
 */
public class Door {
//struct DoorBlock {
//  char name[32];
//  ArtResource ref[5];
//  uint8_t unk[164];
//};

    private String name;
    private ArtResource[] ref;
    private float height; // Fixed point
    private int healthGain;
    private short[] unknown2; // 8
    private Material material;
    private short[] unknown25; // 5
    private int health;
    private int goldCost;
    private short[] unknown3; // 2
    private int deathEffectId;
    private int manufToBuild; // Maybe 4 bytes?
    private int manaCost;
    private short[] unknown5; // 10
    private short doorId;
    private short orderInEditor;
    private short manufCrateObjectId;
    private short keyObjectId;
    private String xName;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public ArtResource[] getRef() {
        return ref;
    }

    protected void setRef(ArtResource[] ref) {
        this.ref = ref;
    }

    public float getHeight() {
        return height;
    }

    protected void setHeight(float height) {
        this.height = height;
    }

    public int getHealthGain() {
        return healthGain;
    }

    protected void setHealthGain(int healthGain) {
        this.healthGain = healthGain;
    }

    public short[] getUnknown2() {
        return unknown2;
    }

    protected void setUnknown2(short[] unknown2) {
        this.unknown2 = unknown2;
    }

    public Material getMaterial() {
        return material;
    }

    protected void setMaterial(Material material) {
        this.material = material;
    }

    public short[] getUnknown25() {
        return unknown25;
    }

    protected void setUnknown25(short[] unknown25) {
        this.unknown25 = unknown25;
    }

    public int getHealth() {
        return health;
    }

    protected void setHealth(int health) {
        this.health = health;
    }

    public int getGoldCost() {
        return goldCost;
    }

    protected void setGoldCost(int goldCost) {
        this.goldCost = goldCost;
    }

    public int getDeathEffectId() {
        return deathEffectId;
    }

    protected void setDeathEffectId(int deathEffectId) {
        this.deathEffectId = deathEffectId;
    }

    public short[] getUnknown3() {
        return unknown3;
    }

    protected void setUnknown3(short[] unknown3) {
        this.unknown3 = unknown3;
    }

    public int getManufToBuild() {
        return manufToBuild;
    }

    protected void setManufToBuild(int manufToBuild) {
        this.manufToBuild = manufToBuild;
    }

    public int getManaCost() {
        return manaCost;
    }

    protected void setManaCost(int manaCost) {
        this.manaCost = manaCost;
    }

    public short[] getUnknown5() {
        return unknown5;
    }

    protected void setUnknown5(short[] unknown5) {
        this.unknown5 = unknown5;
    }

    public short getDoorId() {
        return doorId;
    }

    protected void setDoorId(short doorId) {
        this.doorId = doorId;
    }

    public short getOrderInEditor() {
        return orderInEditor;
    }

    protected void setOrderInEditor(short orderInEditor) {
        this.orderInEditor = orderInEditor;
    }

    public short getManufCrateObjectId() {
        return manufCrateObjectId;
    }

    protected void setManufCrateObjectId(short manufCrateObjectId) {
        this.manufCrateObjectId = manufCrateObjectId;
    }

    public short getKeyObjectId() {
        return keyObjectId;
    }

    protected void setKeyObjectId(short keyObjectId) {
        this.keyObjectId = keyObjectId;
    }

    public String getxName() {
        return xName;
    }

    protected void setxName(String xName) {
        this.xName = xName;
    }

    @Override
    public String toString() {
        return name;
    }
}
