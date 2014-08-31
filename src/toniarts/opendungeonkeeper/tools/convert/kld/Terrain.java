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
class TerrainBlock {/**
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
    
    
}
