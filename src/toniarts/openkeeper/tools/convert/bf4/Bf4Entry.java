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
package toniarts.openkeeper.tools.convert.bf4;

import java.awt.image.BufferedImage;
import toniarts.openkeeper.tools.convert.IValueEnum;

/**
 * BF4 entry a.k.a. FontEntry
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Bf4Entry implements Comparable<Bf4Entry> {

    /**
     * Describes the encoding methods used in the font image
     */
    public enum FontEntryFlag implements IValueEnum {

        NONE(0),
        RLE4_DATA(0x0001),
        ONE_BIT_MONOCHROME(0x0002);

        private FontEntryFlag(int value) {
            this.value = value;
        }

        @Override
        public int getValue() {
            return value;
        }

        private final int value;
    };
    private Character character;
    private int unknown1;
    private int dataSize;
    private int totalSize;
    private FontEntryFlag flag;
    private short unknown2;
    private short unknown3;
    private short unknown4;
    private int width;
    private int height;
    private byte offsetX;
    private byte offsetY;
    private int outerWidth;
    private BufferedImage image;

    public Character getCharacter() {
        return character;
    }

    protected void setCharacter(Character character) {
        this.character = character;
    }

    public int getUnknown1() {
        return unknown1;
    }

    protected void setUnknown1(int unknown1) {
        this.unknown1 = unknown1;
    }

    public int getDataSize() {
        return dataSize;
    }

    protected void setDataSize(int dataSize) {
        this.dataSize = dataSize;
    }

    public int getTotalSize() {
        return totalSize;
    }

    protected void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public FontEntryFlag getFlag() {
        return flag;
    }

    protected void setFlag(FontEntryFlag flag) {
        this.flag = flag;
    }

    public short getUnknown2() {
        return unknown2;
    }

    protected void setUnknown2(short unknown2) {
        this.unknown2 = unknown2;
    }

    public short getUnknown3() {
        return unknown3;
    }

    protected void setUnknown3(short unknown3) {
        this.unknown3 = unknown3;
    }

    public short getUnknown4() {
        return unknown4;
    }

    protected void setUnknown4(short unknown4) {
        this.unknown4 = unknown4;
    }

    public int getWidth() {
        return width;
    }

    protected void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    protected void setHeight(int height) {
        this.height = height;
    }

    public byte getOffsetX() {
        return offsetX;
    }

    protected void setOffsetX(byte offsetX) {
        this.offsetX = offsetX;
    }

    public byte getOffsetY() {
        return offsetY;
    }

    protected void setOffsetY(byte offsetY) {
        this.offsetY = offsetY;
    }

    public int getOuterWidth() {
        return outerWidth;
    }

    protected void setOuterWidth(int outerWidth) {
        this.outerWidth = outerWidth;
    }

    public BufferedImage getImage() {
        return image;
    }

    protected void setImage(BufferedImage image) {
        this.image = image;
    }

    @Override
    public String toString() {
        return character.toString();
    }

    @Override
    public int compareTo(Bf4Entry o) {
        return Integer.compare(character, o.character);
    }
}
