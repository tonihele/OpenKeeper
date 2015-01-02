/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

/**
 * Container class for the Trap
 *
 *
 * @author Wizand Petteri Loisko petteri.loisko@gmail.com, Toni Helenius
 * <helenius.toni@gmail.com>
 *
 * Thank you https://github.com/werkt
 */
public class Trap {
//  char name[32];
//  ArtResource ref[5];
//  uint8_t data[127];

    private String name;
    private ArtResource meshResource;
    private ArtResource guiIcon;
    private ArtResource editorIcon; // ??
    private ArtResource flowerIcon;
    private ArtResource fireResource;
    private float height;
    private float rechargeTime;
    private short[] unknown1; // 67
    private String soundCategory;
    private short[] unknown2; // 20

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

    public ArtResource getFireResource() {
        return fireResource;
    }

    protected void setFireResource(ArtResource fireResource) {
        this.fireResource = fireResource;
    }

    public float getHeight() {
        return height;
    }

    protected void setHeight(float height) {
        this.height = height;
    }

    public float getRechargeTime() {
        return rechargeTime;
    }

    protected void setRechargeTime(float rechargeTime) {
        this.rechargeTime = rechargeTime;
    }

    public short[] getUnknown1() {
        return unknown1;
    }

    protected void setUnknown1(short[] unknown1) {
        this.unknown1 = unknown1;
    }

    public String getSoundCategory() {
        return soundCategory;
    }

    protected void setSoundCategory(String soundCategory) {
        this.soundCategory = soundCategory;
    }

    public short[] getUnknown2() {
        return unknown2;
    }

    protected void setUnknown2(short[] unknown2) {
        this.unknown2 = unknown2;
    }

    @Override
    public String toString() {
        return name;
    }
}
