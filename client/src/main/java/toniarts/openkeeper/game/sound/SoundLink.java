/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.game.sound;

import javax.annotation.Nullable;

/**
 * A property dependent link to another group of the same event.
 */
public class SoundLink {

    private final int groupIndex;
    private final int propertyMin;
    private final int propertyMax;
    @Nullable
    private SoundEventGroup target;

    public SoundLink(int groupIndex, int propertyMin, int propertyMax) {
        this.groupIndex = groupIndex;
        this.propertyMin = propertyMin;
        this.propertyMax = propertyMax;
    }

    /**
     * 1-based group index inside the owning event, as stored in the file.
     */
    public int getGroupIndex() {
        return groupIndex;
    }

    public int getPropertyMin() {
        return propertyMin;
    }

    public int getPropertyMax() {
        return propertyMax;
    }

    @Nullable
    public SoundEventGroup getTarget() {
        return target;
    }

    void setTarget(@Nullable SoundEventGroup target) {
        this.target = target;
    }

    boolean matchesProperty(int property) {
        if (property < 0) {
            return true;
        }
        return property >= propertyMin && property <= propertyMax;
    }

    @Override
    public String toString() {
        return "SoundLink[->" + groupIndex + ", " + propertyMin + ".." + propertyMax + ']';
    }
}