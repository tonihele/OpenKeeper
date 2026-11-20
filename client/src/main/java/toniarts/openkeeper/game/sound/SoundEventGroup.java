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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One selectable variant group of a {@link SoundEvent}, together with its
 * samples and property links.
 */
public class SoundEventGroup {

    private final int volumeMin;
    private final int volumeMax;
    private final int pitchMin;
    private final int pitchMax;
    private final int delayMin;
    private final int delayMax;
    private final int propertyKey1;
    private final int propertyFlags1;
    private final int propertyKey2;
    private final int propertyFlags2;
    private final int cumulativeWeight;
    private final float maxDistance;
    private final List<SoundSample> samples;
    private final List<SoundLink> links;

    public SoundEventGroup(int volumeMin, int volumeMax, int pitchMin, int pitchMax, int delayMin, int delayMax,
            int propertyKey1, int propertyFlags1, int propertyKey2, int propertyFlags2, int cumulativeWeight,
            float maxDistance, List<SoundSample> samples, List<SoundLink> links) {
        this.volumeMin = volumeMin;
        this.volumeMax = volumeMax;
        this.pitchMin = pitchMin;
        this.pitchMax = pitchMax;
        this.delayMin = delayMin;
        this.delayMax = delayMax;
        this.propertyKey1 = propertyKey1;
        this.propertyFlags1 = propertyFlags1;
        this.propertyKey2 = propertyKey2;
        this.propertyFlags2 = propertyFlags2;
        this.cumulativeWeight = cumulativeWeight;
        this.maxDistance = maxDistance;
        this.samples = samples;
        this.links = links;
    }

    public int getVolumeMin() {
        return volumeMin;
    }

    public int getVolumeMax() {
        return volumeMax;
    }

    public int getPitchMin() {
        return pitchMin;
    }

    public int getPitchMax() {
        return pitchMax;
    }

    public int getDelayMin() {
        return delayMin;
    }

    public int getDelayMax() {
        return delayMax;
    }

    public int getPropertyKey1() {
        return propertyKey1;
    }

    public int getPropertyFlags1() {
        return propertyFlags1;
    }

    public int getPropertyKey2() {
        return propertyKey2;
    }

    public int getPropertyFlags2() {
        return propertyFlags2;
    }

    public int getCumulativeWeight() {
        return cumulativeWeight;
    }

    public float getMaxDistance() {
        return maxDistance;
    }

    public List<SoundSample> getSamples() {
        return Collections.unmodifiableList(samples);
    }

    public List<SoundLink> getLinks() {
        return Collections.unmodifiableList(links);
    }

    public SoundSample pickSample() {
        return SoundRng.pick(samples, SoundSample::getCumulativeWeight);
    }

    /**
     * All groups reachable from this one through property links, this group
     * included, without duplicates.
     */
    public List<SoundEventGroup> resolve() {
        List<SoundEventGroup> resolved = new ArrayList<>();
        collect(resolved);
        return resolved;
    }

    private void collect(List<SoundEventGroup> resolved) {
        if (resolved.contains(this)) {
            return;
        }
        resolved.add(this);
        for (SoundLink link : links) {
            SoundEventGroup target = link.getTarget();
            if (target != null) {
                target.collect(resolved);
            }
        }
    }

    @Override
    public String toString() {
        return "SoundEventGroup[vol=" + volumeMin + ".." + volumeMax + ", pitch=" + pitchMin + ".." + pitchMax
                + ", samples=" + samples.size() + ", links=" + links.size() + ']';
    }
}