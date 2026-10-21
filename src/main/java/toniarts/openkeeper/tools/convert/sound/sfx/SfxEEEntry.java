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
 * An SFX.map item: one playable entry of a sound group
 *
 * @author archdemon
 *
 * @see <a href="https://github.com/Dethware/DK2/blob/master/docs/file-formats.md#item-42-bytes">The SFX.map item layout</a>
 */
public final class SfxEEEntry {

    // private final static byte SIZE = 42; // 42 bytes

    private final SfxGroupEntry parent;
    protected int end_pointer_position; // I think not needed
    protected short minVolume;
    protected short maxVolume;
    protected byte minPan; // probably
    protected byte maxPan;
    protected int minDelay;
    protected int maxDelay;
    protected byte[] unknown = new byte[2]; // always 50, 50
    // Two instance parameters (ids) can drive the volume or pan (flags bit 0 / 1), unused by the background groups
    protected int parameter1Id;
    protected int parameter1Flags;
    protected int parameter2Id;
    protected int parameter2Flags;
    protected int weight; // cumulative, used when picking a random entry
    protected int unknown2; // always 0
    protected float unknown3; // 20.0, 100.0 or 0.1
    protected int data_pointer_next; // I think not needed
    protected SfxSoundEntry[] sounds;
    protected SfxData[] data;

    public SfxEEEntry(SfxGroupEntry parent) {
        this.parent = parent;
    }

    public SfxGroupEntry getParent() {
        return parent;
    }

    public SfxSoundEntry[] getSounds() {
        return sounds;
    }

    public SfxData[] getData() {
        return data;
    }

    /**
     * @return minimum volume in percent, the actual volume is picked randomly
     * from the min-max range
     */
    public int getMinVolume() {
        return minVolume;
    }

    /**
     * @return maximum volume in percent
     */
    public int getMaxVolume() {
        return maxVolume;
    }

    /**
     * Delay from the start of this entry to the start of the next one, used by
     * the sequenced one shot sounds (e.g. the dungeon atmosphere).
     *
     * @return minimum delay in milliseconds
     */
    public int getMinDelay() {
        return minDelay;
    }

    /**
     * @return maximum delay in milliseconds
     * @see #getMinDelay()
     */
    public int getMaxDelay() {
        return maxDelay;
    }

    @Override
    public String toString() {
        return "SfxEEEntry{" + "sounds=" + Arrays.toString(sounds)
                + ", data=" + Arrays.toString(data) + "}";
    }
}