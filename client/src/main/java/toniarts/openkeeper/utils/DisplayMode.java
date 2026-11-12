/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.utils;

import com.jme3.system.AppSettings;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * Our own presentation of display mode. Groups everything under resolution.
 *
 * @author ArchDemon
 */
public final class DisplayMode implements Comparable<DisplayMode> {

    private final int width;
    private final int height;
    private final Set<Integer> refreshRates = new TreeSet<>();
    private final Set<Integer> bitDepths = new TreeSet<>();

    public DisplayMode(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public DisplayMode(AppSettings settings) {
        height = settings.getHeight();
        width = settings.getWidth();
        bitDepths.add(settings.getBitsPerPixel());
        refreshRates.add(settings.getFrequency());
    }

    protected void addRefreshRate(Integer refreshRate) {
        refreshRates.add(refreshRate);
    }

    protected void addBitDepth(Integer bitDepth) {
        bitDepths.add(bitDepth);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Collection<Integer> getBitDepths() {
        return bitDepths;
    }

    public Collection<Integer> getRefreshRates() {
        return refreshRates;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DisplayMode other = (DisplayMode) obj;
        if (this.height != other.height) {
            return false;
        }
        return this.width == other.width;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + this.height;
        hash = 37 * hash + this.width;
        return hash;
    }

    @Override
    public String toString() {
        return width + " x " + height;
    }

    @Override
    public int compareTo(DisplayMode o) {
        int result = Integer.compare(width, o.width);
        if (result == 0) {
            result = Integer.compare(height, o.height);
        }
        return result;
    }
}