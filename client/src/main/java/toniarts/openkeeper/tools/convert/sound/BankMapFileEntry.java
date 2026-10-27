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
package toniarts.openkeeper.tools.convert.sound;

/**
 * *Bank.map file entry structure
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class BankMapFileEntry {

    private long unknown1; // 0xFFFFFFFF in all files. resets to 0 in DK2 source
    private int unknown2; // 6746904, 6746032, 6746020, 6683824
    private int unknown3; // 8539, 8795
    private short unknown4; // 115
    private String name;

    protected void setUnknown1(long unknown1) {
        this.unknown1 = unknown1;
    }

    public int getUnknown2() {
        return unknown2;
    }

    protected void setUnknown2(int unknown2) {
        this.unknown2 = unknown2;
    }

    public int getUnknown3() {
        return unknown3;
    }

    protected void setUnknown3(int unknown3) {
        this.unknown3 = unknown3;
    }

    public short getUnknown4() {
        return unknown4;
    }

    public void setUnknown4(short unknown4) {
        this.unknown4 = unknown4;
    }

    /**
     * Part of filename from "Sfx" directory
     * Without quality "HD" or "HW" and without extension "sdt"
     *
     * @return filename
     */
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }
}
