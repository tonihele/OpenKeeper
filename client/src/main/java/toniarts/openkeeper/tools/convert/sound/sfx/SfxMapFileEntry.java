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
public final class SfxMapFileEntry {

    // private final static byte SIZE = 24; // 24 bytes

    private final SfxMapFile parent;
    protected int unknown_1;
    protected int unknown_2;
    protected int unknown_3;
    protected float minDistance;
    protected float maxDistance;
    protected float scale;
    protected SfxGroupEntry[] groups;

    public SfxMapFileEntry(SfxMapFile parent) {
        this.parent = parent;
    }

    public SfxMapFile getParent() {
        return parent;
    }

    public SfxGroupEntry[] getGroups() {
        return groups;
    }

    /**
     * Sounds are at full volume if closer than this
     *
     * @return min distance
     */
    public float getMinDistance() {
        return minDistance;
    }

    /**
     * Sounds are muted if further away than this
     *
     * @return max distance
     */
    public float getMaxDistance() {
        return maxDistance;
    }

    /**
     * Relative amount to adjust rolloff
     *
     * @return
     */
    public float getScale() {
        return scale;
    }

    @Override
    public String toString() {
        return "SfxEntry{" + "minDistance=" + minDistance
                + ", maxDistance=" + maxDistance + ", scale=" + scale
                + ", entries=" + Arrays.toString(groups) + "}";
    }
}