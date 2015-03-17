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
class BankMapFileEntry {

    private long unknown1;
    private int unknown2;
    private short unknown3[]; // 3

    public long getUnknown1() {
        return unknown1;
    }

    protected void setUnknown1(long unknown1) {
        this.unknown1 = unknown1;
    }

    public int getUnknown2() {
        return unknown2;
    }

    protected void setUnknown2(int unknown2) {
        this.unknown2 = unknown2;
    }

    public short[] getUnknown3() {
        return unknown3;
    }

    protected void setUnknown3(short[] unknown3) {
        this.unknown3 = unknown3;
    }
}
