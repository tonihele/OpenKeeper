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
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParticleScaleTest {
    @Test
    void growsTowardsAuthoredMultipleOfStartingSize() {
        var scale = new ParticleScale(.2f, .2f, 4f);
        var particle = new Particle();
        particle.startlife = 2f;
        particle.life = 2f;
        scale.update(particle, true);
        assertEquals(.1f, particle.size, 1e-6);
        particle.life = 1f;
        scale.update(particle, false);
        assertEquals(.25f, particle.size, 1e-6);
        particle.life = 0f;
        scale.update(particle, false);
        assertEquals(.4f, particle.size, 1e-6);
    }

    @Test
    void constantScaleSamplesTheStartingRangeForEachReusedParticle() {
        var scale = new ParticleScale(.2f, .6f, 1f);
        var particle = new Particle();
        particle.startlife = particle.life = 1f;
        scale.update(particle, true);
        float initial = particle.size;
        assertTrue(initial >= .1f && initial <= .3f);
        particle.life = .1f;
        scale.update(particle, false);
        assertEquals(initial, particle.size);
        var replacement = new ParticleScale(.8f, .8f, 1f);
        replacement.update(particle, true);
        assertEquals(.4f, particle.size, 1e-6);
    }
}
