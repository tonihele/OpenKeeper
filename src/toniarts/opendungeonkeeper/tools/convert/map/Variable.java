/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

/**
 * Container class for *Variables.kld
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Variable {

//    struct VariableBlock {
//        int x00;
//        int x04;
//        int x08;
//        int x0c;
//        };
    private int x00; // Some kind of ID, but there are duplicates
    private int value;
    private int x08;
    private int x0c;

    public int getX00() {
        return x00;
    }

    protected void setX00(int x00) {
        this.x00 = x00;
    }

    public int getValue() {
        return value;
    }

    protected void setValue(int value) {
        this.value = value;
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
}
