/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

import javax.vecmath.Vector3f;

/**
 * Container class for *Things.kld
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class Thing {

//    struct ActionPointBlock {
//        int32_t x00;
//        int32_t x04;
//        int32_t x08;
//        int32_t x0c;
//        int32_t x10;
//        int16_t x14;
//        uint8_t id; /* 16 */
//        uint8_t x17;
//        char name[32]; /* 18 */
//        };
    public class ActionPoint extends Thing {

        private int x00;
        private int x04;
        private int x08;
        private int x0c;
        private int x10;
        private int x14;
        private short id; // 16
        private short x17;
        private String name; // 18

        public int getX00() {
            return x00;
        }

        protected void setX00(int x00) {
            this.x00 = x00;
        }

        public int getX04() {
            return x04;
        }

        protected void setX04(int x04) {
            this.x04 = x04;
        }

        public int getX08() {
            return x08;
        }

        protected void setX08(int x08) {
            this.x08 = x08;
        }

        public int getX0c() {
            return x0c;
        }

        protected void setX0c(int x0c) {
            this.x0c = x0c;
        }

        public int getX10() {
            return x10;
        }

        protected void setX10(int x10) {
            this.x10 = x10;
        }

        public int getX14() {
            return x14;
        }

        protected void setX14(int x14) {
            this.x14 = x14;
        }

        public short getId() {
            return id;
        }

        protected void setId(short id) {
            this.id = id;
        }

        public short getX17() {
            return x17;
        }

        protected void setX17(short x17) {
            this.x17 = x17;
        }

        public String getName() {
            return name;
        }

        protected void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

//    struct Thing03Block {
//        int32_t pos[3];
//        uint16_t x0c;
//        uint8_t x0e; /* level */
//        uint8_t x0f; /* likely flags */
//        int32_t x10;
//        int32_t x14;
//        uint16_t x18;
//        uint8_t id; /* 1a */
//        uint8_t x1b; /* player id */
//        };
    public class Thing03 extends Thing {

        private Vector3f pos;
        private int x0c;
        private short x0e; // level
        private short x0f; // likely flags
        private int x10;
        private int x14;
        private int x18;
        private short id; // 1a
        private short x1b; // player id

        public Vector3f getPos() {
            return pos;
        }

        protected void setPos(Vector3f pos) {
            this.pos = pos;
        }

        public int getX0c() {
            return x0c;
        }

        protected void setX0c(int x0c) {
            this.x0c = x0c;
        }

        public short getX0e() {
            return x0e;
        }

        protected void setX0e(short x0e) {
            this.x0e = x0e;
        }

        public short getX0f() {
            return x0f;
        }

        protected void setX0f(short x0f) {
            this.x0f = x0f;
        }

        public int getX10() {
            return x10;
        }

        protected void setX10(int x10) {
            this.x10 = x10;
        }

        public int getX14() {
            return x14;
        }

        protected void setX14(int x14) {
            this.x14 = x14;
        }

        public int getX18() {
            return x18;
        }

        protected void setX18(int x18) {
            this.x18 = x18;
        }

        public short getId() {
            return id;
        }

        protected void setId(short id) {
            this.id = id;
        }

        public short getX1b() {
            return x1b;
        }

        protected void setX1b(short x1b) {
            this.x1b = x1b;
        }
    }

//    struct Thing11Block {
//        Thing11Block()
//        : x00(0), x04(0), x08(0), x0c(0), x0e(0), x0f(0), x10(0), x12(0), x13(0) { }
//        int32_t x00;
//        int32_t x04;
//        int32_t x08;
//        int16_t x0c;
//        uint8_t x0e;
//        uint8_t x0f; /* maybe padding */
//        int16_t x10;
//        uint8_t x12;
//        uint8_t x13;
//        };
    public class Thing11 extends Thing {

        private int x00;
        private int x04;
        private int x08;
        private int x0c;
        private short x0e;
        private short x0f; // maybe padding
        private int x10;
        private short x12;
        private short x13;

        public int getX00() {
            return x00;
        }

        protected void setX00(int x00) {
            this.x00 = x00;
        }

        public int getX04() {
            return x04;
        }

        protected void setX04(int x04) {
            this.x04 = x04;
        }

        public int getX08() {
            return x08;
        }

        protected void setX08(int x08) {
            this.x08 = x08;
        }

        public int getX0c() {
            return x0c;
        }

        protected void setX0c(int x0c) {
            this.x0c = x0c;
        }

        public short getX0e() {
            return x0e;
        }

        protected void setX0e(short x0e) {
            this.x0e = x0e;
        }

        public short getX0f() {
            return x0f;
        }

        protected void setX0f(short x0f) {
            this.x0f = x0f;
        }

        public int getX10() {
            return x10;
        }

        protected void setX10(int x10) {
            this.x10 = x10;
        }

        public short getX12() {
            return x12;
        }

        protected void setX12(short x12) {
            this.x12 = x12;
        }

        public short getX13() {
            return x13;
        }

        protected void setX13(short x13) {
            this.x13 = x13;
        }
    }
//    struct Thing12Block {
//        Position3D x00;
//        Position3D x0c;
//        Position3D x18;
//        int32_t x24;
//        int32_t x28;
//        int32_t x2c;
//        int32_t x30;
//        int32_t x34;
//        int32_t x38;
//        int32_t x3c;
//        int32_t x40;
//        int32_t x44;
//        int32_t x48; /* flags */
//        uint16_t x4c;
//        uint16_t x4e;
//        uint16_t x50;
//        uint8_t x52;
//        };

    public class Thing12 extends Thing {

        private Vector3f x00;
        private Vector3f x0c;
        private Vector3f x18;
        private int x24;
        private int x28;
        private int x2c;
        private int x30;
        private int x34;
        private int x38;
        private int x3c;
        private int x40;
        private int x44;
        private int x48; // flags
        private int x4c;
        private int x4e;
        private int x50;
        private short x52;

        public Vector3f getX00() {
            return x00;
        }

        protected void setX00(Vector3f x00) {
            this.x00 = x00;
        }

        public Vector3f getX0c() {
            return x0c;
        }

        protected void setX0c(Vector3f x0c) {
            this.x0c = x0c;
        }

        public Vector3f getX18() {
            return x18;
        }

        protected void setX18(Vector3f x18) {
            this.x18 = x18;
        }

        public int getX24() {
            return x24;
        }

        protected void setX24(int x24) {
            this.x24 = x24;
        }

        public int getX28() {
            return x28;
        }

        protected void setX28(int x28) {
            this.x28 = x28;
        }

        public int getX2c() {
            return x2c;
        }

        protected void setX2c(int x2c) {
            this.x2c = x2c;
        }

        public int getX30() {
            return x30;
        }

        protected void setX30(int x30) {
            this.x30 = x30;
        }

        public int getX34() {
            return x34;
        }

        protected void setX34(int x34) {
            this.x34 = x34;
        }

        public int getX38() {
            return x38;
        }

        protected void setX38(int x38) {
            this.x38 = x38;
        }

        public int getX3c() {
            return x3c;
        }

        protected void setX3c(int x3c) {
            this.x3c = x3c;
        }

        public int getX40() {
            return x40;
        }

        protected void setX40(int x40) {
            this.x40 = x40;
        }

        public int getX44() {
            return x44;
        }

        protected void setX44(int x44) {
            this.x44 = x44;
        }

        public int getX48() {
            return x48;
        }

        protected void setX48(int x48) {
            this.x48 = x48;
        }

        public int getX4c() {
            return x4c;
        }

        protected void setX4c(int x4c) {
            this.x4c = x4c;
        }

        public int getX4e() {
            return x4e;
        }

        protected void setX4e(int x4e) {
            this.x4e = x4e;
        }

        public int getX50() {
            return x50;
        }

        protected void setX50(int x50) {
            this.x50 = x50;
        }

        public short getX52() {
            return x52;
        }

        protected void setX52(short x52) {
            this.x52 = x52;
        }
    }

//struct Thing08Block { /* hero party */
//    char name[32];
//    int16_t x20;
//    uint8_t x22;
//    int32_t x23; /* these two are unreferenced... */
//    int32_t x27;
//    HeroPartyData x2b[16];
//    };
    public class Thing08 extends Thing {

        private String name;
        private int x20;
        private short x22;
        private int x23; // these two are unreferenced...
        private int x27;
        HeroPartyData x2b[];

        public String getName() {
            return name;
        }

        protected void setName(String name) {
            this.name = name;
        }

        public int getX20() {
            return x20;
        }

        protected void setX20(int x20) {
            this.x20 = x20;
        }

        public short getX22() {
            return x22;
        }

        protected void setX22(short x22) {
            this.x22 = x22;
        }

        public int getX23() {
            return x23;
        }

        protected void setX23(int x23) {
            this.x23 = x23;
        }

        public int getX27() {
            return x27;
        }

        protected void setX27(int x27) {
            this.x27 = x27;
        }

        public HeroPartyData[] getX2b() {
            return x2b;
        }

        protected void setX2b(HeroPartyData[] x2b) {
            this.x2b = x2b;
        }

        @Override
        public String toString() {
            return name;
        }

//        struct HeroPartyData {
//            int32_t x00;
//            int32_t x04;
//            int32_t x08;
//            int16_t goldHeld;
//            uint8_t x0e; /* level */
//            uint8_t x0f;
//            int32_t x10;
//            int32_t initialHealth;
//            int16_t x18; /* trigger root */
//            uint8_t x1a;
//            uint8_t x1b;
//            uint8_t x1c;
//            uint8_t x1d;
//            uint8_t x1e;
//            uint8_t x1f;
//            };
        public class HeroPartyData {

            private int x00;
            private int x04;
            private int x08;
            private int goldHeld;
            private short x0e; // level
            private short x0f;
            private int x10;
            private int initialHealth;
            private int x18; // trigger root
            private short x1a;
            private short x1b;
            private short x1c;
            private short x1d;
            private short x1e;
            private short x1f;

            public int getX00() {
                return x00;
            }

            protected void setX00(int x00) {
                this.x00 = x00;
            }

            public int getX04() {
                return x04;
            }

            protected void setX04(int x04) {
                this.x04 = x04;
            }

            public int getX08() {
                return x08;
            }

            protected void setX08(int x08) {
                this.x08 = x08;
            }

            public int getGoldHeld() {
                return goldHeld;
            }

            protected void setGoldHeld(int goldHeld) {
                this.goldHeld = goldHeld;
            }

            public short getX0e() {
                return x0e;
            }

            protected void setX0e(short x0e) {
                this.x0e = x0e;
            }

            public short getX0f() {
                return x0f;
            }

            protected void setX0f(short x0f) {
                this.x0f = x0f;
            }

            public int getX10() {
                return x10;
            }

            protected void setX10(int x10) {
                this.x10 = x10;
            }

            public int getInitialHealth() {
                return initialHealth;
            }

            protected void setInitialHealth(int initialHealth) {
                this.initialHealth = initialHealth;
            }

            public int getX18() {
                return x18;
            }

            protected void setX18(int x18) {
                this.x18 = x18;
            }

            public short getX1a() {
                return x1a;
            }

            protected void setX1a(short x1a) {
                this.x1a = x1a;
            }

            public short getX1b() {
                return x1b;
            }

            protected void setX1b(short x1b) {
                this.x1b = x1b;
            }

            public short getX1c() {
                return x1c;
            }

            protected void setX1c(short x1c) {
                this.x1c = x1c;
            }

            public short getX1d() {
                return x1d;
            }

            protected void setX1d(short x1d) {
                this.x1d = x1d;
            }

            public short getX1e() {
                return x1e;
            }

            protected void setX1e(short x1e) {
                this.x1e = x1e;
            }

            public short getX1f() {
                return x1f;
            }

            protected void setX1f(short x1f) {
                this.x1f = x1f;
            }
        }
    }

//    struct Thing10Block {
//        int32_t x00;
//        int32_t x04;
//        int32_t x08;
//        int32_t x0c;
//        int16_t x10;
//        int16_t x12;
//        int16_t x14[4];
//        uint8_t x1c;
//        uint8_t x1d;
//        uint8_t pad[6];
//        };
    public class Thing10 extends Thing {

        private int x00;
        private int x04;
        private int x08;
        private int x0c;
        private int x10;
        private int x12;
        private int x14[];
        private short x1c;
        private short x1d;
        private short pad[];

        public int getX00() {
            return x00;
        }

        protected void setX00(int x00) {
            this.x00 = x00;
        }

        public int getX04() {
            return x04;
        }

        protected void setX04(int x04) {
            this.x04 = x04;
        }

        public int getX08() {
            return x08;
        }

        protected void setX08(int x08) {
            this.x08 = x08;
        }

        public int getX0c() {
            return x0c;
        }

        protected void setX0c(int x0c) {
            this.x0c = x0c;
        }

        public int getX10() {
            return x10;
        }

        protected void setX10(int x10) {
            this.x10 = x10;
        }

        public int getX12() {
            return x12;
        }

        protected void setX12(int x12) {
            this.x12 = x12;
        }

        public int[] getX14() {
            return x14;
        }

        protected void setX14(int[] x14) {
            this.x14 = x14;
        }

        public short getX1c() {
            return x1c;
        }

        protected void setX1c(short x1c) {
            this.x1c = x1c;
        }

        public short getX1d() {
            return x1d;
        }

        protected void setX1d(short x1d) {
            this.x1d = x1d;
        }

        public short[] getPad() {
            return pad;
        }

        protected void setPad(short[] pad) {
            this.pad = pad;
        }
    }
}
