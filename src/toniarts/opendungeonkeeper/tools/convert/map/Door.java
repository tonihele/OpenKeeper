/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

import java.util.EnumSet;

/**
 * Stub for the container class for the Doors.kwd
 *
 *
 * @author Wizand Petteri Loisko petteri.loisko@gmail.com
 *
 * Thank you https://github.com/werkt
 */
public class Door implements Comparable<Door> {

//struct DoorBlock {
//  char name[32];
//  ArtResource ref[5];
//  uint8_t unk[164];
//};
    /**
     * Door flags
     */
    public enum DoorFlag implements IFlagEnum {

        IS_SECRET(0x0020), // Secret
        IS_BARRICADE(0x0040), // Barricade
        IS_GOOD(0x0080); // Good (as in alignment)
        private final long flagValue;

        private DoorFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    };
    private String name;
    private ArtResource mesh;
    private ArtResource guiIcon;
    private ArtResource editorIcon; // ??
    private ArtResource flowerIcon;
    private ArtResource openResource;
    private ArtResource closeResource;
    private float height; // Fixed point
    private int healthGain;
    private short[] unknown2; // 8
    private Material material;
    private short trapTypeId;
    private EnumSet<DoorFlag> flags;
    private int health;
    private int goldCost;
    private short[] unknown3; // 2
    private int deathEffectId;
    private int manufToBuild; // Maybe 4 bytes?
    private int manaCost;
    private int tooltipStringId;
    private int nameStringId;
    private int generalDescriptionStringId;
    private int strengthStringId;
    private int weaknessStringId;
    private short doorId;
    private short orderInEditor; // introductionIndex in editor
    private short manufCrateObjectId;
    private short keyObjectId;
    private String soundGategory;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public ArtResource getMesh() {
        return mesh;
    }

    protected void setMesh(ArtResource mesh) {
        this.mesh = mesh;
    }

    public ArtResource getGuiIcon() {
        return guiIcon;
    }

    protected void setGuiIcon(ArtResource guiIcon) {
        this.guiIcon = guiIcon;
    }

    public ArtResource getEditorIcon() {
        return editorIcon;
    }

    protected void setEditorIcon(ArtResource editorIcon) {
        this.editorIcon = editorIcon;
    }

    public ArtResource getFlowerIcon() {
        return flowerIcon;
    }

    protected void setFlowerIcon(ArtResource flowerIcon) {
        this.flowerIcon = flowerIcon;
    }

    public ArtResource getOpenResource() {
        return openResource;
    }

    protected void setOpenResource(ArtResource openResource) {
        this.openResource = openResource;
    }

    public ArtResource getCloseResource() {
        return closeResource;
    }

    protected void setCloseResource(ArtResource closeResource) {
        this.closeResource = closeResource;
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

    public short getTrapTypeId() {
        return trapTypeId;
    }

    protected void setTrapTypeId(short trapTypeId) {
        this.trapTypeId = trapTypeId;
    }

    public EnumSet<DoorFlag> getFlags() {
        return flags;
    }

    public void setFlags(EnumSet<DoorFlag> flags) {
        this.flags = flags;
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

    public String getSoundGategory() {
        return soundGategory;
    }

    protected void setSoundGategory(String soundGategory) {
        this.soundGategory = soundGategory;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(Door o) {
        return Short.compare(orderInEditor, o.orderInEditor);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + this.doorId;
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
        final Door other = (Door) obj;
        if (this.doorId != other.doorId) {
            return false;
        }
        return true;
    }
}
