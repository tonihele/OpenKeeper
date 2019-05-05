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
package toniarts.openkeeper.world.effect;

/**
 *
 * @author ArchDemon
 */
@Deprecated
public class FloatLimit {
    private Float upperLimit = null;
    private Float lowerLimit = null;
    private float value;

    public FloatLimit(float value) {
        this.value = value;
    }

    public FloatLimit(float value, float upperLimit, float lowerLimit) {
        this(value);
        this.upperLimit = upperLimit;
        this.lowerLimit = lowerLimit;
    }

    public Float getUpperLimit() {
        return upperLimit;
    }

    public Float getLowerLimit() {
        return lowerLimit;
    }

    public float getValue() {
        return value;
    }

    public void add(float value) {
        this.value += value;
        if (upperLimit != null && this.value > upperLimit) {
            this.value = upperLimit;
        }
    }

    public void sub(float value) {
        this.value -= value;
        if (lowerLimit != null && this.value < lowerLimit) {
            this.value = lowerLimit;
        }
    }
}
