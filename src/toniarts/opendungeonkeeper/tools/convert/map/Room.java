/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

import java.awt.Color;

/**
 * Barely started placeholder for the container class for the Rooms
 *
 *
 * @author Wizand Petteri Loisko petteri.loisko@gmail.com
 *
 * Thank you https://github.com/werkt
 */
public class Room {

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
    private String name; // 0
    private ArtResource guiIcon; // 20
    private ArtResource roomIcon; // 74
    private ArtResource complete; // c8
    private ArtResource[] ref; // 11c
    private int unknown1; // 368 - very likely flags
    private int unknown2; // 36c
    private int torchIntensity; // 36e
    private int unknown3; // 370
    private int x374;
    private int x376;
    private int x378;
    private int x37a;
    private int x37c;
    private int x37e;
    private float torchRadius; // 380
    private int[] effects; // 382
    private short roomId; // 392
    private short unknown7; // 393
    private short terrainId; // 394
    private short tileConstruction; // 395
    private short unknown8; // 396
    private Color torchColor; // 397
    private short[] objects; // 39a
    private String soundCategory; // 3a2
    private short x3c2; // 3c2
    private short x3c3; // 3c3
    private int unknown10; // 3c4
    private short unknown11; // 3c6
    private ArtResource torch; // 3c7
    private short x41b; // 41b
    private short x41c; // 41c
    private short x41d; // 41d

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
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

    public int getTorchIntensity() {
        return torchIntensity;
    }

    protected void setTorchIntensity(int torchIntensity) {
        this.torchIntensity = torchIntensity;
    }

    public int getUnknown3() {
        return unknown3;
    }

    protected void setUnknown3(int unknown3) {
        this.unknown3 = unknown3;
    }

    public int getX374() {
        return x374;
    }

    protected void setX374(int x374) {
        this.x374 = x374;
    }

    public int getX376() {
        return x376;
    }

    protected void setX376(int x376) {
        this.x376 = x376;
    }

    public int getX378() {
        return x378;
    }

    protected void setX378(int x378) {
        this.x378 = x378;
    }

    public int getX37a() {
        return x37a;
    }

    protected void setX37a(int x37a) {
        this.x37a = x37a;
    }

    public int getX37c() {
        return x37c;
    }

    protected void setX37c(int x37c) {
        this.x37c = x37c;
    }

    public int getX37e() {
        return x37e;
    }

    protected void setX37e(int x37e) {
        this.x37e = x37e;
    }

    public float getTorchRadius() {
        return torchRadius;
    }

    protected void setTorchRadius(float torchRadius) {
        this.torchRadius = torchRadius;
    }

    public int[] getEffects() {
        return effects;
    }

    protected void setEffects(int[] effects) {
        this.effects = effects;
    }

    public short getRoomId() {
        return roomId;
    }

    protected void setRoomId(short roomId) {
        this.roomId = roomId;
    }

    public short getUnknown7() {
        return unknown7;
    }

    protected void setUnknown7(short unknown7) {
        this.unknown7 = unknown7;
    }

    public short getTerrainId() {
        return terrainId;
    }

    protected void setTerrainId(short terrain) {
        this.terrainId = terrainId;
    }

    public short getTileConstruction() {
        return tileConstruction;
    }

    protected void setTileConstruction(short tileConstruction) {
        this.tileConstruction = tileConstruction;
    }

    public short getUnknown8() {
        return unknown8;
    }

    protected void setUnknown8(short unknown8) {
        this.unknown8 = unknown8;
    }

    public Color getTorchColor() {
        return torchColor;
    }

    protected void setTorchColor(Color torchColor) {
        this.torchColor = torchColor;
    }

    public short[] getObjects() {
        return objects;
    }

    protected void setObjects(short[] objects) {
        this.objects = objects;
    }

    public String getSoundCategory() {
        return soundCategory;
    }

    protected void setSoundCategory(String soundCategory) {
        this.soundCategory = soundCategory;
    }

    public short getX3c2() {
        return x3c2;
    }

    protected void setX3c2(short x3c2) {
        this.x3c2 = x3c2;
    }

    public short getX3c3() {
        return x3c3;
    }

    protected void setX3c3(short x3c3) {
        this.x3c3 = x3c3;
    }

    public int getUnknown10() {
        return unknown10;
    }

    protected void setUnknown10(int unknown10) {
        this.unknown10 = unknown10;
    }

    public short getUnknown11() {
        return unknown11;
    }

    protected void setUnknown11(short unknown11) {
        this.unknown11 = unknown11;
    }

    public short getX41b() {
        return x41b;
    }

    protected void setX41b(short x41b) {
        this.x41b = x41b;
    }

    public short getX41c() {
        return x41c;
    }

    protected void setX41c(short x41c) {
        this.x41c = x41c;
    }

    public short getX41d() {
        return x41d;
    }

    protected void setX41d(short x41d) {
        this.x41d = x41d;
    }

    public ArtResource getGuiIcon() {
        return guiIcon;
    }

    protected void setGuiIcon(ArtResource guiIcon) {
        this.guiIcon = guiIcon;
    }

    public ArtResource getRoomIcon() {
        return roomIcon;
    }

    protected void setRoomIcon(ArtResource roomIcon) {
        this.roomIcon = roomIcon;
    }

    public ArtResource getComplete() {
        return complete;
    }

    protected void setComplete(ArtResource complete) {
        this.complete = complete;
    }

    public ArtResource[] getRef() {
        return ref;
    }

    protected void setRef(ArtResource[] ref) {
        this.ref = ref;
    }

    public ArtResource getTorch() {
        return torch;
    }

    protected void setTorch(ArtResource torch) {
        this.torch = torch;
    }
}
