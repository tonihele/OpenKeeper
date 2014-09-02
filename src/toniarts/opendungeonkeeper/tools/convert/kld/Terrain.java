/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.kld;

/**
 * Barely started placeholder for the container class for the Terrain & TerrainBlock
 * 
 *
 * @author Wizand Petteri Loisko
 * petteri.loisko@gmail.com
 * 
 * Thank you https://github.com/werkt
 */

public class Terrain {
    
    private TerrainBlock terrainBlock;
      
    public TerrainBlock getTerrainBlock() {
        return terrainBlock;
    }
    
    public void setTerrainblock(TerrainBlock block) {
        this.terrainBlock = block;
    }

}
class TerrainBlock {
    /**
     * struct Terrain {
  char name[32]; /* 0 */
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
//  ArtResource complete; /* 20 */
//  ArtResource side; /* 74 */
//  ArtResource top; /* c8 */
//  ArtResource tagged; /* 11c */
//  StringIds string_ids; /* 170 */
  private int unk188; /* 188 */
  private int light_height; /* 18c */
  private int flags; /* 190 */
  private short damage; /* 194 */
  private short unk196; /* 196 */
  private short unk198; /* 198 */
  private short gold_value; /* 19a */
  private short mana_gain; /* 19c */
  private short max_mana_gain; /* 19e */
  private short unk1a0; /* 1a0 */
  private short unk1a2; /* 1a2 */
  private short unk1a4; /* 1a4 */
  private short unk1a6; /* 1a6 */
  private short unk1a8; /* 1a8 */
  private short unk1aa; /* 1aa */
  private short unk1ac; /* 1ac */
  private short[] unk1ae; /* 1ae */
  private byte wibble_h; /* 1ce */
  private byte[] lean_h; /* 1cf */
  private byte wibble_v; /* 1d2 */
  private byte[] lean_v; /* 1d3 */
  private byte id; /* 1d6 */
  private short starting_health; /* 1d7 */
  private byte max_health_type; /* 1d9 */
  private byte destroyed_type; /* 1da */
  private byte[] terrain_light; /* 1db */
  private byte texture_frames; /* 1de */
  private String str1; /* 1df */
  private short max_health; /* 1ff */
  private byte[] ambient_light; /* 201 */
  private String str2; /* 204 */
  private int unk224; /* 224 */

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUnk188() {
        return unk188;
    }

    public void setUnk188(int unk188) {
        this.unk188 = unk188;
    }

    public int getLight_height() {
        return light_height;
    }

    public void setLight_height(int light_height) {
        this.light_height = light_height;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public short getDamage() {
        return damage;
    }

    public void setDamage(short damage) {
        this.damage = damage;
    }

    public short getUnk196() {
        return unk196;
    }

    public void setUnk196(short unk196) {
        this.unk196 = unk196;
    }

    public short getUnk198() {
        return unk198;
    }

    public void setUnk198(short unk198) {
        this.unk198 = unk198;
    }

    public short getGold_value() {
        return gold_value;
    }

    public void setGold_value(short gold_value) {
        this.gold_value = gold_value;
    }

    public short getMana_gain() {
        return mana_gain;
    }

    public void setMana_gain(short mana_gain) {
        this.mana_gain = mana_gain;
    }

    public short getMax_mana_gain() {
        return max_mana_gain;
    }

    public void setMax_mana_gain(short max_mana_gain) {
        this.max_mana_gain = max_mana_gain;
    }

    public short getUnk1a0() {
        return unk1a0;
    }

    public void setUnk1a0(short unk1a0) {
        this.unk1a0 = unk1a0;
    }

    public short getUnk1a2() {
        return unk1a2;
    }

    public void setUnk1a2(short unk1a2) {
        this.unk1a2 = unk1a2;
    }

    public short getUnk1a4() {
        return unk1a4;
    }

    public void setUnk1a4(short unk1a4) {
        this.unk1a4 = unk1a4;
    }

    public short getUnk1a6() {
        return unk1a6;
    }

    public void setUnk1a6(short unk1a6) {
        this.unk1a6 = unk1a6;
    }

    public short getUnk1a8() {
        return unk1a8;
    }

    public void setUnk1a8(short unk1a8) {
        this.unk1a8 = unk1a8;
    }

    public short getUnk1aa() {
        return unk1aa;
    }

    public void setUnk1aa(short unk1aa) {
        this.unk1aa = unk1aa;
    }

    public short getUnk1ac() {
        return unk1ac;
    }

    public void setUnk1ac(short unk1ac) {
        this.unk1ac = unk1ac;
    }

    public short[] getUnk1ae() {
        return unk1ae;
    }

    public void setUnk1ae(short[] unk1ae) {
        this.unk1ae = unk1ae;
    }

    public byte getWibble_h() {
        return wibble_h;
    }

    public void setWibble_h(byte wibble_h) {
        this.wibble_h = wibble_h;
    }

    public byte[] getLean_h() {
        return lean_h;
    }

    public void setLean_h(byte[] lean_h) {
        this.lean_h = lean_h;
    }

    public byte getWibble_v() {
        return wibble_v;
    }

    public void setWibble_v(byte wibble_v) {
        this.wibble_v = wibble_v;
    }

    public byte[] getLean_v() {
        return lean_v;
    }

    public void setLean_v(byte[] lean_v) {
        this.lean_v = lean_v;
    }

    public byte getId() {
        return id;
    }

    public void setId(byte id) {
        this.id = id;
    }

    public short getStarting_health() {
        return starting_health;
    }

    public void setStarting_health(short starting_health) {
        this.starting_health = starting_health;
    }

    public byte getMax_health_type() {
        return max_health_type;
    }

    public void setMax_health_type(byte max_health_type) {
        this.max_health_type = max_health_type;
    }

    public byte getDestroyed_type() {
        return destroyed_type;
    }

    public void setDestroyed_type(byte destroyed_type) {
        this.destroyed_type = destroyed_type;
    }

    public byte[] getTerrain_light() {
        return terrain_light;
    }

    public void setTerrain_light(byte[] terrain_light) {
        this.terrain_light = terrain_light;
    }

    public byte getTexture_frames() {
        return texture_frames;
    }

    public void setTexture_frames(byte texture_frames) {
        this.texture_frames = texture_frames;
    }

    public String getStr1() {
        return str1;
    }

    public void setStr1(String str1) {
        this.str1 = str1;
    }

    public short getMax_health() {
        return max_health;
    }

    public void setMax_health(short max_health) {
        this.max_health = max_health;
    }

    public byte[] getAmbient_light() {
        return ambient_light;
    }

    public void setAmbient_light(byte[] ambient_light) {
        this.ambient_light = ambient_light;
    }

    public String getStr2() {
        return str2;
    }

    public void setStr2(String str2) {
        this.str2 = str2;
    }

    public int getUnk224() {
        return unk224;
    }

    public void setUnk224(int unk224) {
        this.unk224 = unk224;
    }
    
    
  
  
}
