/*
 * Copyright (C) 2026 OpenKeeper
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
package toniarts.openkeeper.view.effect;

import com.jme3.effect.Particle;
import com.jme3.math.FastMath;
import java.util.IdentityHashMap;
import java.util.Map;

/** Random starting scale and authored lifetime multiplier for sprite elements. */
final class ParticleScale {
    private final float min;
    private final float max;
    private final float ratio;
    private final Map<Particle, Float> starts = new IdentityHashMap<>();

    ParticleScale(float min, float max, float ratio) {
        this.min = min;
        this.max = max;
        this.ratio = ratio;
    }

    void update(Particle particle, boolean justSpawned) {
        Float start = starts.get(particle);
        if (start == null || justSpawned) {
            start = min + FastMath.nextRandomFloat() * (max - min);
            starts.put(particle, start);
        }
        float elapsed = FastMath.clamp(1f - particle.life / particle.startlife, 0f, 1f);
        // ParticleTriMesh takes half-extents rather than full sprite dimensions.
        particle.size = start * (1f + (ratio - 1f) * elapsed) / 2f;
    }
}
