/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.kld;



/**
 * Barely started placeholder for the container class for the Rooms
 * 
 *
 * @author Wizand Petteri Loisko
 * petteri.loisko@gmail.com
 * 
 * Thank you https://github.com/werkt
 */
public class Room {
    private RoomBlock roomBlock;

    public RoomBlock getRoomBlock() {
        return roomBlock;
    }

    public void setRoomBlock(RoomBlock roomBlock) {
        this.roomBlock = roomBlock;
    }
    
    
        
}
class RoomBlock {

//struct RoomBlock {
//  char name[32]; /* 0 */
//  ArtResource gui_icon; /* 20 */
//  ArtResource room_icon; /* 74 */
//  ArtResource complete; /* c8 */
//  ArtResource ref[7]; /* 11c */
//  uint32_t unk1; /* 368 - very likely flags */
//  uint16_t unk2; /* 36c */
//  uint16_t intensity; /* 36e */
//  uint32_t unk3; /* 370 */
//  uint16_t x374;
//  uint16_t x376;
//  uint16_t x378;
//  uint16_t x37a;
//  uint16_t x37c;
//  uint16_t x37e;
//  uint16_t unk5; /* 380 */
//  uint16_t effects[8]; /* 382 */
//  uint8_t id; /* 392 */
//  uint8_t unk7; /* 393 */
//  uint8_t terrain; /* 394 */
//  uint8_t tile_construction; /* 395 */
//  uint8_t unk8; /* 396 */
//  uint8_t torch_color[3]; /* 397 */
//  uint8_t objects[8]; /* 39a */
//  char sound_category[32]; /* 3a2 */
//  uint8_t x3c2; /* 3c2 */
//  uint8_t x3c3; /* 3c3 */
//  uint16_t unk10; /* 3c4 */
//  uint8_t unk11; /* 3c6 */
//  ArtResource torch; /* 3c7 */
//  uint8_t x41b; /* 41b */
//  uint8_t x41c; /* 41c */
//  short x41d; /* 41d */
//};
    
  private String name; /* 0 */
//  ArtResource gui_icon; /* 20 */
//  ArtResource room_icon; /* 74 */
//  ArtResource complete; /* c8 */
//  ArtResource ref[7]; /* 11c */
  private int unk1; /* 368 - very likely flags */
  private short unk2; /* 36c */
  private short intensity; /* 36e */
  private int unk3; /* 370 */
  private short x374;
  private short x376;
  private short x378;
  private short x37a;
  private short x37c;
  private short x37e;
  private short unk5; /* 380 */
  private short[] effects; /* 382 */
  private short id; /* 392 */
  private short unk7; /* 393 */
  private short terrain; /* 394 */
  private short tile_construction; /* 395 */
  private short unk8; /* 396 */
  private short[] torch_color; /* 397 */
  private short[] objects; /* 39a */
  private String sound_category; /* 3a2 */
  private short x3c2; /* 3c2 */
  private short x3c3; /* 3c3 */
  private short unk10; /* 3c4 */
  private short unk11; /* 3c6 */
//  ArtResource torch; /* 3c7 */
  private short x41b; /* 41b */
  private short x41c; /* 41c */
  short x41d; /* 41d */

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUnk1() {
        return unk1;
    }

    public void setUnk1(int unk1) {
        this.unk1 = unk1;
    }

    public short getUnk2() {
        return unk2;
    }

    public void setUnk2(short unk2) {
        this.unk2 = unk2;
    }

    public short getIntensity() {
        return intensity;
    }

    public void setIntensity(short intensity) {
        this.intensity = intensity;
    }

    public int getUnk3() {
        return unk3;
    }

    public void setUnk3(int unk3) {
        this.unk3 = unk3;
    }

    public short getX374() {
        return x374;
    }

    public void setX374(short x374) {
        this.x374 = x374;
    }

    public short getX376() {
        return x376;
    }

    public void setX376(short x376) {
        this.x376 = x376;
    }

    public short getX378() {
        return x378;
    }

    public void setX378(short x378) {
        this.x378 = x378;
    }

    public short getX37a() {
        return x37a;
    }

    public void setX37a(short x37a) {
        this.x37a = x37a;
    }

    public short getX37c() {
        return x37c;
    }

    public void setX37c(short x37c) {
        this.x37c = x37c;
    }

    public short getX37e() {
        return x37e;
    }

    public void setX37e(short x37e) {
        this.x37e = x37e;
    }

    public short getUnk5() {
        return unk5;
    }

    public void setUnk5(short unk5) {
        this.unk5 = unk5;
    }

    public short[] getEffects() {
        return effects;
    }

    public void setEffects(short[] effects) {
        this.effects = effects;
    }

    public short getId() {
        return id;
    }

    public void setId(short id) {
        this.id = id;
    }

    public short getUnk7() {
        return unk7;
    }

    public void setUnk7(short unk7) {
        this.unk7 = unk7;
    }

    public short getTerrain() {
        return terrain;
    }

    public void setTerrain(short terrain) {
        this.terrain = terrain;
    }

    public short getTile_construction() {
        return tile_construction;
    }

    public void setTile_construction(short tile_construction) {
        this.tile_construction = tile_construction;
    }

    public short getUnk8() {
        return unk8;
    }

    public void setUnk8(short unk8) {
        this.unk8 = unk8;
    }

    public short[] getTorch_color() {
        return torch_color;
    }

    public void setTorch_color(short[] torch_color) {
        this.torch_color = torch_color;
    }

    public short[] getObjects() {
        return objects;
    }

    public void setObjects(short[] objects) {
        this.objects = objects;
    }

    public String getSound_category() {
        return sound_category;
    }

    public void setSound_category(String sound_category) {
        this.sound_category = sound_category;
    }

    public short getX3c2() {
        return x3c2;
    }

    public void setX3c2(short x3c2) {
        this.x3c2 = x3c2;
    }

    public short getX3c3() {
        return x3c3;
    }

    public void setX3c3(short x3c3) {
        this.x3c3 = x3c3;
    }

    public short getUnk10() {
        return unk10;
    }

    public void setUnk10(short unk10) {
        this.unk10 = unk10;
    }

    public short getUnk11() {
        return unk11;
    }

    public void setUnk11(short unk11) {
        this.unk11 = unk11;
    }

    public short getX41b() {
        return x41b;
    }

    public void setX41b(short x41b) {
        this.x41b = x41b;
    }

    public short getX41c() {
        return x41c;
    }

    public void setX41c(short x41c) {
        this.x41c = x41c;
    }

    public short getX41d() {
        return x41d;
    }

    public void setX41d(short x41d) {
        this.x41d = x41d;
    }
    
  
  
}