/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

import java.awt.Color;

/**
 * Barely started placeholder for the container class for the Terrain &
 * TerrainBlock
 *
 *
 * @author Wizand Petteri Loisko petteri.loisko@gmail.com
 *
 * Thank you https://github.com/werkt
 */
public class Terrain {

    //
    // struct Terrain { char name[32]; /* 0
    //
    //  ArtResource complete; /* 20 */
    //  ArtResource side; /* 74 */
    //  ArtResource top; /* c8 */
    //  ArtResource tagged; /* 11c */
    //  StringIds string_ids; /* 170 */
    //  uint32_t unk188; /* 188 */
    //  uint32_t light_height; /* 18c */
    //  uint32_t flags; /* 190 */
    //  uint16_t damage; /* 194 */
    //  uint16_t unk196; /* 196 */
    //  uint16_t unk198; /* 198 */
    //  uint16_t gold_value; /* 19a */
    //  uint16_t mana_gain; /* 19c */
    //  uint16_t max_mana_gain; /* 19e */
    //  uint16_t unk1a0; /* 1a0 */
    //  uint16_t unk1a2; /* 1a2 */
    //  uint16_t unk1a4; /* 1a4 */
    //  uint16_t unk1a6; /* 1a6 */
    //  uint16_t unk1a8; /* 1a8 */
    //  uint16_t unk1aa; /* 1aa */
    //  uint16_t unk1ac; /* 1ac */
    //  uint16_t unk1ae[16]; /* 1ae */
    //  uint8_t wibble_h; /* 1ce */
    //  uint8_t lean_h[3]; /* 1cf */
    //  uint8_t wibble_v; /* 1d2 */
    //  uint8_t lean_v[3]; /* 1d3 */
    //  uint8_t id; /* 1d6 */
    //  uint16_t starting_health; /* 1d7 */
    //  uint8_t max_health_type; /* 1d9 */
    //  uint8_t destroyed_type; /* 1da */
    //  uint8_t terrain_light[3]; /* 1db */
    //  uint8_t texture_frames; /* 1de */
    //  char str1[32]; /* 1df */
    //  uint16_t max_health; /* 1ff */
    //  uint8_t ambient_light[3]; /* 201 */
    //  char str2[32]; /* 204 */
    //  uint32_t unk224; /* 224 */
    //};
    private String name;
    private ArtResource complete; // 20
    private ArtResource side; // 74
    private ArtResource top; // c8
    private ArtResource tagged; // 11c
    private StringId stringIds; // 170
    private int unk188; // 188
    private int lightHeight; // 18c
    private int flags; // 190
    private int damage; // 194
    private int unk196; // 196
    private int unk198; // 198
    private int goldValue; // 19a
    private int manaGain; // 19c
    private int maxManaGain; // 19e
    private int unk1a0; // 1a0
    private int unk1a2; // 1a2
    private int unk1a4; // 1a4
    private int unk1a6; // 1a6
    private int unk1a8; // 1a8
    private int unk1aa; // 1aa
    private int unk1ac; // 1ac
    private int[] unk1ae; // 1ae
    private short wibbleH; // 1ce
    private short[] leanH; // 1cf
    private short wibbleV; // 1d2
    private short[] leanV; // 1d3
    private short terrainId; // 1d6
    private int startingHealth; // 1d7
    private short maxHealthType; // 1d9
    private short destroyedType; // 1da
    private Color terrainLight; // 1db
    private short textureFrames; // 1de
    private String str1; // 1df
    private int maxHealth; // 1ff
    private Color ambientLight; // 201
    private String str2; // 204
    private int unk224; // 224

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public int getUnk188() {
        return unk188;
    }

    protected void setUnk188(int unk188) {
        this.unk188 = unk188;
    }

    public int getLightHeight() {
        return lightHeight;
    }

    protected void setLightHeight(int lightHeight) {
        this.lightHeight = lightHeight;
    }

    public int getFlags() {
        return flags;
    }

    protected void setFlags(int flags) {
        this.flags = flags;
    }

    public int getDamage() {
        return damage;
    }

    protected void setDamage(int damage) {
        this.damage = damage;
    }

    public int getUnk196() {
        return unk196;
    }

    protected void setUnk196(int unk196) {
        this.unk196 = unk196;
    }

    public int getUnk198() {
        return unk198;
    }

    protected void setUnk198(int unk198) {
        this.unk198 = unk198;
    }

    public int getGoldValue() {
        return goldValue;
    }

    protected void setGoldValue(int goldValue) {
        this.goldValue = goldValue;
    }

    public int getManaGain() {
        return manaGain;
    }

    protected void setManaGain(int manaGain) {
        this.manaGain = manaGain;
    }

    public int getMaxManaGain() {
        return maxManaGain;
    }

    protected void setMaxManaGain(int maxManaGain) {
        this.maxManaGain = maxManaGain;
    }

    public int getUnk1a0() {
        return unk1a0;
    }

    protected void setUnk1a0(int unk1a0) {
        this.unk1a0 = unk1a0;
    }

    public int getUnk1a2() {
        return unk1a2;
    }

    protected void setUnk1a2(int unk1a2) {
        this.unk1a2 = unk1a2;
    }

    public int getUnk1a4() {
        return unk1a4;
    }

    protected void setUnk1a4(int unk1a4) {
        this.unk1a4 = unk1a4;
    }

    public int getUnk1a6() {
        return unk1a6;
    }

    protected void setUnk1a6(int unk1a6) {
        this.unk1a6 = unk1a6;
    }

    public int getUnk1a8() {
        return unk1a8;
    }

    protected void setUnk1a8(int unk1a8) {
        this.unk1a8 = unk1a8;
    }

    public int getUnk1aa() {
        return unk1aa;
    }

    protected void setUnk1aa(int unk1aa) {
        this.unk1aa = unk1aa;
    }

    public int getUnk1ac() {
        return unk1ac;
    }

    protected void setUnk1ac(int unk1ac) {
        this.unk1ac = unk1ac;
    }

    public int[] getUnk1ae() {
        return unk1ae;
    }

    protected void setUnk1ae(int[] unk1ae) {
        this.unk1ae = unk1ae;
    }

    public short getWibbleH() {
        return wibbleH;
    }

    protected void setWibbleH(short wibbleH) {
        this.wibbleH = wibbleH;
    }

    public short[] getLeanH() {
        return leanH;
    }

    protected void setLeanH(short[] leanH) {
        this.leanH = leanH;
    }

    public short getWibbleV() {
        return wibbleV;
    }

    protected void setWibbleV(short wibbleV) {
        this.wibbleV = wibbleV;
    }

    public short[] getLeanV() {
        return leanV;
    }

    protected void setLeanV(short[] leanV) {
        this.leanV = leanV;
    }

    public short getTerrainId() {
        return terrainId;
    }

    protected void setTerrainId(short terrainId) {
        this.terrainId = terrainId;
    }

    public int getStartingHealth() {
        return startingHealth;
    }

    protected void setStartingHealth(int startingHealth) {
        this.startingHealth = startingHealth;
    }

    public short getMaxHealthType() {
        return maxHealthType;
    }

    protected void setMaxHealthType(short maxHealthType) {
        this.maxHealthType = maxHealthType;
    }

    public short getDestroyedType() {
        return destroyedType;
    }

    protected void setDestroyedType(short destroyedType) {
        this.destroyedType = destroyedType;
    }

    public Color getTerrainLight() {
        return terrainLight;
    }

    protected void setTerrainLight(Color terrainLight) {
        this.terrainLight = terrainLight;
    }

    public short getTextureFrames() {
        return textureFrames;
    }

    protected void setTextureFrames(short textureFrames) {
        this.textureFrames = textureFrames;
    }

    public String getStr1() {
        return str1;
    }

    protected void setStr1(String str1) {
        this.str1 = str1;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    protected void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    public Color getAmbientLight() {
        return ambientLight;
    }

    protected void setAmbientLight(Color ambientLight) {
        this.ambientLight = ambientLight;
    }

    public String getStr2() {
        return str2;
    }

    protected void setStr2(String str2) {
        this.str2 = str2;
    }

    public int getUnk224() {
        return unk224;
    }

    protected void setUnk224(int unk224) {
        this.unk224 = unk224;
    }

    public ArtResource getComplete() {
        return complete;
    }

    protected void setComplete(ArtResource complete) {
        this.complete = complete;
    }

    public ArtResource getSide() {
        return side;
    }

    protected void setSide(ArtResource side) {
        this.side = side;
    }

    public ArtResource getTop() {
        return top;
    }

    protected void setTop(ArtResource top) {
        this.top = top;
    }

    public ArtResource getTagged() {
        return tagged;
    }

    protected void setTagged(ArtResource tagged) {
        this.tagged = tagged;
    }

    public StringId getStringIds() {
        return stringIds;
    }

    protected void setStringIds(StringId stringIds) {
        this.stringIds = stringIds;
    }
}
