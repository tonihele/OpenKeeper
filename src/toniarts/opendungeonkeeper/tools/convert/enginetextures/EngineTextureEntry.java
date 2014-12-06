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
    private int sResX;
    private int sResY;
    private boolean alphaFlag;
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

    public int getsResX() {
        return sResX;
    }

    protected void setsResX(int sResX) {
        this.sResX = sResX;
    }

    public int getsResY() {
        return sResY;
    }

    protected void setsResY(int sResY) {
        this.sResY = sResY;
    }

    public boolean isAlphaFlag() {
        return alphaFlag;
    }

    protected void setAlphaFlag(boolean alphaFlag) {
        this.alphaFlag = alphaFlag;
    }

    protected long getDataStartLocation() {
        return dataStartLocation;
    }

    protected void setDataStartLocation(long dataStartLocation) {
        this.dataStartLocation = dataStartLocation;
    }
}
