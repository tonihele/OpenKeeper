/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

/**
 * Container class for KeeperSpells.kwd
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class KeeperSpell {

//    struct KeeperSpellBlock {
//        char name[32];
//        ArtResource ref1;
//        ArtResource ref3;
//        int xc8;
//        int xcc;
//        int xd0;
//        int xd4;
//        uint16_t xd8;
//        uint8_t xda;
//        uint8_t xdb;
//        int xdc;
//        uint16_t xe0Unreferenced; /* e0 */
//        uint16_t xe2;
//        uint16_t xe4;
//        uint16_t xe6;
//        uint16_t xe8;
//        uint16_t xea;
//        uint16_t xec;
//        uint8_t id; /* ee */
//        uint8_t xef;
//        uint8_t xf0;
//        char yname[32]; /* xf1 */
//        uint16_t x111;
//        uint8_t x113;
//        int x114;
//        int x118;
//        int x11c;
//        ArtResource ref2; /* 120 */
//        char xname[32]; /* 174 */
//        uint8_t x194;
//        uint8_t x195;
//        };
    private String name;
    private ArtResource ref1;
    private ArtResource ref3;
    private int xc8;
    private int xcc;
    private int shotData1;
    private int shotData2;
    private int xd8;
    private short xda;
    private short xdb;
    private int xdc;
    private int xe0Unreferenced; // e0
    private int manaDrain;
    private int xe4;
    private int xe6;
    private int xe8;
    private int xea;
    private int xec;
    private short keeperSpellId; // ee
    private short xef;
    private short xf0;
    private String yName; // xf1
    // Bonus update
    private int bonusRTime;
    private short bonusShotType;
    private int bonusShotData1;
    private int bonusShotData2;
    private int manaCost;
    private ArtResource ref2; // 120
    private String xName; // 174
    private short x194;
    private short x195;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public ArtResource getRef1() {
        return ref1;
    }

    protected void setRef1(ArtResource ref1) {
        this.ref1 = ref1;
    }

    public ArtResource getRef3() {
        return ref3;
    }

    protected void setRef3(ArtResource ref3) {
        this.ref3 = ref3;
    }

    public int getXc8() {
        return xc8;
    }

    protected void setXc8(int xc8) {
        this.xc8 = xc8;
    }

    public int getXcc() {
        return xcc;
    }

    protected void setXcc(int xcc) {
        this.xcc = xcc;
    }

    public int getShotData1() {
        return shotData1;
    }

    protected void setShotData1(int shotData1) {
        this.shotData1 = shotData1;
    }

    public int getShotData2() {
        return shotData2;
    }

    protected void setShotData2(int shotData2) {
        this.shotData2 = shotData2;
    }

    public int getXd8() {
        return xd8;
    }

    protected void setXd8(int xd8) {
        this.xd8 = xd8;
    }

    public short getXda() {
        return xda;
    }

    protected void setXda(short xda) {
        this.xda = xda;
    }

    public short getXdb() {
        return xdb;
    }

    protected void setXdb(short xdb) {
        this.xdb = xdb;
    }

    public int getXdc() {
        return xdc;
    }

    protected void setXdc(int xdc) {
        this.xdc = xdc;
    }

    public int getXe0Unreferenced() {
        return xe0Unreferenced;
    }

    protected void setXe0Unreferenced(int xe0Unreferenced) {
        this.xe0Unreferenced = xe0Unreferenced;
    }

    public int getManaDrain() {
        return manaDrain;
    }

    protected void setManaDrain(int manaDrain) {
        this.manaDrain = manaDrain;
    }

    public int getXe4() {
        return xe4;
    }

    protected void setXe4(int xe4) {
        this.xe4 = xe4;
    }

    public int getXe6() {
        return xe6;
    }

    protected void setXe6(int xe6) {
        this.xe6 = xe6;
    }

    public int getXe8() {
        return xe8;
    }

    protected void setXe8(int xe8) {
        this.xe8 = xe8;
    }

    public int getXea() {
        return xea;
    }

    protected void setXea(int xea) {
        this.xea = xea;
    }

    public int getXec() {
        return xec;
    }

    protected void setXec(int xec) {
        this.xec = xec;
    }

    public short getKeeperSpellId() {
        return keeperSpellId;
    }

    protected void setKeeperSpellId(short keeperSpellId) {
        this.keeperSpellId = keeperSpellId;
    }

    public short getXef() {
        return xef;
    }

    protected void setXef(short xef) {
        this.xef = xef;
    }

    public short getXf0() {
        return xf0;
    }

    protected void setXf0(short xf0) {
        this.xf0 = xf0;
    }

    public String getYName() {
        return yName;
    }

    protected void setYName(String yName) {
        this.yName = yName;
    }

    public int getBonusRTime() {
        return bonusRTime;
    }

    protected void setBonusRTime(int bonusRTime) {
        this.bonusRTime = bonusRTime;
    }

    public short getBonusShotType() {
        return bonusShotType;
    }

    protected void setBonusShotType(short bonusShotType) {
        this.bonusShotType = bonusShotType;
    }

    public int getBonusShotData1() {
        return bonusShotData1;
    }

    protected void setBonusShotData1(int bonusShotData1) {
        this.bonusShotData1 = bonusShotData1;
    }

    public int getBonusShotData2() {
        return bonusShotData2;
    }

    protected void setBonusShotData2(int bonusShotData2) {
        this.bonusShotData2 = bonusShotData2;
    }

    public int getManaCost() {
        return manaCost;
    }

    protected void setManaCost(int manaCost) {
        this.manaCost = manaCost;
    }

    public ArtResource getRef2() {
        return ref2;
    }

    protected void setRef2(ArtResource ref2) {
        this.ref2 = ref2;
    }

    public String getXName() {
        return xName;
    }

    protected void setXName(String xName) {
        this.xName = xName;
    }

    public short getX194() {
        return x194;
    }

    protected void setX194(short x194) {
        this.x194 = x194;
    }

    public short getX195() {
        return x195;
    }

    protected void setX195(short x195) {
        this.x195 = x195;
    }

    @Override
    public String toString() {
        return name;
    }
}
