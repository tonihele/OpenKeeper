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

import java.util.List;
import java.util.Random;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;

/**
 * Weighted random selection and parameter randomisation.
 * <p>
 * Weights are stored as running totals, exactly like the original game: the
 * total weight of a group is the cumulative weight of its last entry.
 */
public final class SoundRng {

    private static Random random = new Random();

    private SoundRng() {
    }

    public static void setRandom(Random newRandom) {
        random = newRandom;
    }

    public static Random getRandom() {
        return random;
    }

    public static float nextFloat() {
        return random.nextFloat();
    }

    public static int nextInt(int bound) {
        return random.nextInt(bound);
    }

    public static boolean nextBoolean() {
        return random.nextBoolean();
    }

    /**
     * Total weight of a weighted list, i.e. the cumulative weight of its last
     * entry.
     */
    public static <T> int totalWeightOf(List<? extends T> items, ToIntFunction<T> cumulativeWeight) {
        return items.isEmpty() ? 0 : Math.max(0, cumulativeWeight.applyAsInt(items.get(items.size() - 1)));
    }

    /**
     * Picks one item using the DKII running-total scheme, i.e. the weight of an
     * entry is the total weight of all entries up to and including it.
     */
    @Nullable
    public static <T> T pick(List<T> items, ToIntFunction<T> cumulativeWeight) {
        int total = totalWeightOf(items, cumulativeWeight);
        if (total <= 0) {
            return items.isEmpty() ? null : items.get(0);
        }
        int roll = random.nextInt(total);
        for (T item : items) {
            if (roll < cumulativeWeight.applyAsInt(item)) {
                return item;
            }
        }
        return items.get(items.size() - 1);
    }

    /**
     * Group volume, an integer percentage where 100 is unity.
     */
    public static int volume(SoundEventGroup group) {
        return randomVolume(group.getVolumeMin(), group.getVolumeMax());
    }

    public static int randomVolume(int min, int max) {
        int volume = 100 + nextInt(min - 100, max - 100 + 1);
        return clampVolume(volume);
    }

    public static int clampVolume(int volume) {
        if (volume < 0) {
            return 0;
        }
        return volume > 127 ? 127 : volume;
    }

    /**
     * Group pitch in percent, 100 being the unmodified playback rate.
     */
    public static float pitch(SoundEventGroup group) {
        float pitch = 100.0f + nextInt(group.getPitchMin(), group.getPitchMax() + 1);
        if (pitch < 1.0f) {
            pitch = 1.0f;
        }
        return pitch;
    }

    public static int delay(SoundEventGroup group) {
        if (group.getDelayMax() <= 0) {
            return 0;
        }
        return group.getDelayMin() + nextInt(group.getDelayMax() - group.getDelayMin() + 1);
    }

    private static int nextInt(int min, int bound) {
        if (bound <= min) {
            return min;
        }
        return min + random.nextInt(bound - min);
    }
}