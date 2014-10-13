/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

/**
 * Container class for *Triggers.kld
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class Trigger {
//    struct TriggerBlock {
//        int x00;
//        int x04;
//        uint16_t x08;
//        uint16_t x0a;
//        uint16_t x0c;
//        uint8_t x0e;
//        uint8_t x0f;
//        };

    private int x00;
    private int x04;
    private int x08;
    private int x0a;
    private int x0c;
    private short x0e;

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

    public int getX0a() {
        return x0a;
    }

    protected void setX0a(int x0a) {
        this.x0a = x0a;
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

    public class TriggerGeneric extends Trigger {

        private short x0f;

        public short getX0f() {
            return x0f;
        }

        protected void setX0f(short x0f) {
            this.x0f = x0f;
        }
    }

    public class TriggerAction extends Trigger {
    }
}
