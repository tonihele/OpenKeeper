/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

import java.awt.Color;

/**
 * Container class for EffectElements.kwd
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class EffectElement {

//    struct EffectElementBlock {
//        char m_sName[32];
//        ArtResource m_kArtResource;
//        int32_t mass; /* 74 */
//        uint32_t air_friction; /* 78 */
//        uint32_t elasticity; /* 7c */
//        int32_t min_speed_xy; /* 80 */
//        int32_t max_speed_xy; /* 84 */
//        int32_t min_speed_yz; /* 88 says it's yz, but appears to only be Z */
//        int32_t max_speed_yz; /* 8c z speed at end */
//        uint32_t min_scale; /* 90 */
//        uint32_t max_scale; /* 94 */
//        uint32_t scale_ratio; /* 98 */
//        uint32_t flags; /* 9c */
//        uint16_t m_wId; /* a0 */
//        uint16_t min_hp; /* a2 */
//        uint16_t max_hp; /* a4 */
//        uint16_t death_element; /* a6 */
//        uint16_t hit_solid_element; /* a8 */
//        uint16_t hit_water_element; /* aa */
//        uint16_t hit_lava_element; /* ac */
//        uint8_t color[3]; /* ae */
//        uint8_t random_color_index; /* b1 */
//        uint8_t table_color_index; /* b2 */
//        uint8_t fade_percentage; /* b3 */
//        uint16_t next_effect; /* b4 */
//        };
    private String name;
    private ArtResource artResource;
    private float mass; // 74
    private float airFriction; // 78
    private float elasticity; // 7c
    private float minSpeedXy; // 80
    private float maxSpeedXy; // 84
    private float minSpeedYz; // 88 says it's yz, but appears to only be Z
    private float maxSpeedYz; // 8c z speed at end
    private float minScale; // 90
    private float maxScale; // 94
    private float scaleRatio; // 98
    private int flags; // 9c
    private int effectElementId; // a0
    private int minHp; // a2
    private int maxHp; // a4
    private int deathElement; // a6
    private int hitSolidElement; // a8
    private int hitWaterElement; // aa
    private int hitLavaElement; // ac
    private Color color; // ae
    private short randomColorIndex; // b1
    private short tableColorIndex; // b2
    private short fadePercentage; // b3
    private int nextEffect; // b4

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public ArtResource getArtResource() {
        return artResource;
    }

    protected void setArtResource(ArtResource artResource) {
        this.artResource = artResource;
    }

    public float getMass() {
        return mass;
    }

    protected void setMass(float mass) {
        this.mass = mass;
    }

    public float getAirFriction() {
        return airFriction;
    }

    protected void setAirFriction(float airFriction) {
        this.airFriction = airFriction;
    }

    public float getElasticity() {
        return elasticity;
    }

    protected void setElasticity(float elasticity) {
        this.elasticity = elasticity;
    }

    public float getMinSpeedXy() {
        return minSpeedXy;
    }

    protected void setMinSpeedXy(float minSpeedXy) {
        this.minSpeedXy = minSpeedXy;
    }

    public float getMaxSpeedXy() {
        return maxSpeedXy;
    }

    protected void setMaxSpeedXy(float maxSpeedXy) {
        this.maxSpeedXy = maxSpeedXy;
    }

    public float getMinSpeedYz() {
        return minSpeedYz;
    }

    protected void setMinSpeedYz(float minSpeedYz) {
        this.minSpeedYz = minSpeedYz;
    }

    public float getMaxSpeedYz() {
        return maxSpeedYz;
    }

    protected void setMaxSpeedYz(float maxSpeedYz) {
        this.maxSpeedYz = maxSpeedYz;
    }

    public float getMinScale() {
        return minScale;
    }

    protected void setMinScale(float minScale) {
        this.minScale = minScale;
    }

    public float getMaxScale() {
        return maxScale;
    }

    protected void setMaxScale(float maxScale) {
        this.maxScale = maxScale;
    }

    public float getScaleRatio() {
        return scaleRatio;
    }

    protected void setScaleRatio(float scaleRatio) {
        this.scaleRatio = scaleRatio;
    }

    public int getFlags() {
        return flags;
    }

    protected void setFlags(int flags) {
        this.flags = flags;
    }

    public int getEffectElementId() {
        return effectElementId;
    }

    protected void setEffectElementId(int effectElementId) {
        this.effectElementId = effectElementId;
    }

    public int getMinHp() {
        return minHp;
    }

    protected void setMinHp(int minHp) {
        this.minHp = minHp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    protected void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public int getDeathElement() {
        return deathElement;
    }

    protected void setDeathElement(int deathElement) {
        this.deathElement = deathElement;
    }

    public int getHitSolidElement() {
        return hitSolidElement;
    }

    protected void setHitSolidElement(int hitSolidElement) {
        this.hitSolidElement = hitSolidElement;
    }

    public int getHitWaterElement() {
        return hitWaterElement;
    }

    protected void setHitWaterElement(int hitWaterElement) {
        this.hitWaterElement = hitWaterElement;
    }

    public int getHitLavaElement() {
        return hitLavaElement;
    }

    protected void setHitLavaElement(int hitLavaElement) {
        this.hitLavaElement = hitLavaElement;
    }

    public Color getColor() {
        return color;
    }

    protected void setColor(Color color) {
        this.color = color;
    }

    public short getRandomColorIndex() {
        return randomColorIndex;
    }

    protected void setRandomColorIndex(short randomColorIndex) {
        this.randomColorIndex = randomColorIndex;
    }

    public short getTableColorIndex() {
        return tableColorIndex;
    }

    protected void setTableColorIndex(short tableColorIndex) {
        this.tableColorIndex = tableColorIndex;
    }

    public short getFadePercentage() {
        return fadePercentage;
    }

    protected void setFadePercentage(short fadePercentage) {
        this.fadePercentage = fadePercentage;
    }

    public int getNextEffect() {
        return nextEffect;
    }

    protected void setNextEffect(int nextEffect) {
        this.nextEffect = nextEffect;
    }

    @Override
    public String toString() {
        return name;
    }
}
