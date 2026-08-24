/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.tools.convert.sound.sfx;

import java.util.Arrays;

/**
 *
 * @author archdemon
 */
public final class SfxData {

    // private final static byte SIZE = 8; // 8 bytes

    protected int index;
    protected byte[] unknown2 = new byte[4];

    @Override
    public String toString() {
        return "SfxData{" + "index=" + index
                + ", unknown2=" + Arrays.toString(unknown2) + '}';
    }
}