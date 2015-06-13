/*
 * Copyright (C) 2014-2015 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.tools.convert.textures.enginetextures;

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
