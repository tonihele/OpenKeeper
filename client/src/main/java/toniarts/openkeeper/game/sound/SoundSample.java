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
 * One playable clip inside a {@link SoundEventGroup}.
 */
public class SoundSample {

    public static final SoundSample NO_SAMPLE = new SoundSample(-1, 0, 0, 0, null);

    private final int clip;
    private final int cumulativeWeight;
    private final int durationMs;
    private final int bankRef;
    @Nullable
    private final SoundArchive archive;

    public SoundSample(int clip, int cumulativeWeight, int durationMs, int bankRef, @Nullable SoundArchive archive) {
        this.clip = clip;
        this.cumulativeWeight = cumulativeWeight;
        this.durationMs = durationMs;
        this.bankRef = bankRef;
        this.archive = archive;
    }

    public int getClip() {
        return clip;
    }

    public int getCumulativeWeight() {
        return cumulativeWeight;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public int getBankRef() {
        return bankRef;
    }

    @Nullable
    public SoundArchive getArchive() {
        return archive;
    }

    @Nullable
    public String getAssetPath() {
        return archive == null ? null : archive.getClipAssetPath(clip);
    }

    @Override
    public String toString() {
        return "SoundSample[" + (archive == null ? "?" : archive.getName()) + '#' + clip + ", w=" + cumulativeWeight
                + ", dur=" + durationMs + ']';
    }
}