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
package toniarts.openkeeper.game.component;

import com.simsilica.es.EntityComponent;

/**
 * Native DKII creature anger. The game keeps independent counters so resolving
 * one complaint does not erase unrelated anger.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CreatureMood implements EntityComponent {

    public static final int REASON_GENERAL = 0;
    public static final int REASON_NO_FOOD = 1;
    public static final int REASON_NO_LAIR = 2;
    public static final int REASON_NO_WORK = 3;
    public static final int REASON_NO_PAY = 4;
    public static final int REASON_OTHER = 5;
    public static final int MAX_ANGER = 65533;

    public int general;
    public int noFood;
    public int noLair;
    public int noWork;
    public int noPay;
    public int other;

    public CreatureMood() {
        // For serialization
    }

    public CreatureMood(int general, int noFood, int noLair, int noWork, int noPay, int other) {
        this.general = general;
        this.noFood = noFood;
        this.noLair = noLair;
        this.noWork = noWork;
        this.noPay = noPay;
        this.other = other;
    }

    /**
     * Adds a signed native anger delta and clamps the selected counter.
     *
     * @param reason anger reason index
     * @param value signed runtime delta
     * @return updated component
     */
    public CreatureMood add(int reason, int value) {
        int[] values = toArray();
        values[reason] = (int) Math.max(0, Math.min(MAX_ANGER, (long) values[reason] + value));
        return fromArray(values);
    }

    /**
     * Clears one anger reason.
     *
     * @param reason anger reason index
     * @return updated component
     */
    public CreatureMood clear(int reason) {
        int[] values = toArray();
        values[reason] = 0;
        return fromArray(values);
    }

    /**
     * Returns one anger counter.
     *
     * @param reason anger reason index
     * @return counter value
     */
    public int get(int reason) {
        return toArray()[reason];
    }

    /**
     * Returns the first reason having the greatest anger value.
     *
     * @return dominant reason index
     */
    public int getDominantReason() {
        int[] values = toArray();
        int result = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] > values[result]) {
                result = i;
            }
        }
        return result;
    }

    /**
     * @return greatest anger counter
     */
    public int getMaximum() {
        return get(getDominantReason());
    }

    /**
     * @return mean of all six counters, as used by native script/stat queries
     */
    public int getAverage() {
        return (general + noFood + noLair + noWork + noPay + other) / 6;
    }

    /**
     * Derives the native mood state from the largest anger counter.
     *
     * @param unhappyThreshold runtime unhappy threshold
     * @return 0 for normal, 1 for unhappy, or 2 for angry
     */
    public int getState(int unhappyThreshold) {
        int maximum = getMaximum();
        if (maximum < unhappyThreshold) {
            return 0;
        }
        return maximum <= 32767 ? 1 : 2;
    }

    /**
     * Converts an authored KWD anger delta to the runtime scale used by DKII.
     *
     * @param value authored value
     * @return runtime value
     */
    public static int toRuntimeValue(int value) {
        return value / 4;
    }

    /**
     * Converts an authored percentage threshold to DKII's runtime anger scale.
     *
     * @param value authored percentage
     * @return runtime threshold
     */
    public static int toRuntimeThreshold(int value) {
        return value * 32767 / 100;
    }

    private int[] toArray() {
        return new int[]{general, noFood, noLair, noWork, noPay, other};
    }

    private static CreatureMood fromArray(int[] values) {
        return new CreatureMood(values[0], values[1], values[2], values[3], values[4], values[5]);
    }

}
