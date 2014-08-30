/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.enginetextures;

/**
 * Holds an Engine Texture entry<br>
 * Thanks to "Rheini" from http://forum.xentax.com/viewtopic.php?t=2580
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class EngineTextureEntry {

    private int resX;
    private int resY;
    private int size;
    private short sResX;
    private short sResY;
    private int unknown; // Compression identifier?
    private long dataStartLocation;

    public int getResX() {
        return resX;
    }

    protected void setResX(int resX) {
        this.resX = resX;
    }

    public int getResY() {
        return resY;
    }

    protected void setResY(int resY) {
        this.resY = resY;
    }

    public int getSize() {
        return size;
    }

    protected void setSize(int size) {
        this.size = size;
    }

    public short getsResX() {
        return sResX;
    }

    protected void setsResX(short sResX) {
        this.sResX = sResX;
    }

    public short getsResY() {
        return sResY;
    }

    protected void setsResY(short sResY) {
        this.sResY = sResY;
    }

    public int getUnknown() {
        return unknown;
    }

    protected void setUnknown(int unknown) {
        this.unknown = unknown;
    }

    protected long getDataStartLocation() {
        return dataStartLocation;
    }

    protected void setDataStartLocation(long dataStartLocation) {
        this.dataStartLocation = dataStartLocation;
    }
}
