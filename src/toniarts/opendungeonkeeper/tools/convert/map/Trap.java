/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

import javax.vecmath.Vector3f;

/**
 * Container class for the Trap
 *
 *
 * @author Wizand Petteri Loisko petteri.loisko@gmail.com, Toni Helenius
 * <helenius.toni@gmail.com>
 *
 * Thank you https://github.com/werkt
 */
public class Trap implements Comparable<Trap> {
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
    private float chargeTime;
    private float threatDuration;
    private short[] unknown1; // 59
    private String soundCategory;
    private Material material;
    private short orderInEditor; // Byte
    private Vector3f shotOffset; // 4 bytes fixed point, x - y - z
    private float shotDelay; // Short
    private int unknown2; // Short
    private int healthGain; // Short

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

    public float getChargeTime() {
        return chargeTime;
    }

    protected void setChargeTime(float chargeTime) {
        this.chargeTime = chargeTime;
    }

    public float getThreatDuration() {
        return threatDuration;
    }

    protected void setThreatDuration(float threatDuration) {
        this.threatDuration = threatDuration;
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

    public Material getMaterial() {
        return material;
    }

    protected void setMaterial(Material material) {
        this.material = material;
    }

    public short getOrderInEditor() {
        return orderInEditor;
    }

    protected void setOrderInEditor(short orderInEditor) {
        this.orderInEditor = orderInEditor;
    }

    public Vector3f getShotOffset() {
        return shotOffset;
    }

    protected void setShotOffset(Vector3f shotOffset) {
        this.shotOffset = shotOffset;
    }

    public float getShotDelay() {
        return shotDelay;
    }

    protected void setShotDelay(float shotDelay) {
        this.shotDelay = shotDelay;
    }

    public int getUnknown2() {
        return unknown2;
    }

    protected void setUnknown2(int unknown2) {
        this.unknown2 = unknown2;
    }

    public int getHealthGain() {
        return healthGain;
    }

    protected void setHealthGain(int healthGain) {
        this.healthGain = healthGain;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(Trap o) {
        return Short.compare(orderInEditor, o.orderInEditor);
    }
}
