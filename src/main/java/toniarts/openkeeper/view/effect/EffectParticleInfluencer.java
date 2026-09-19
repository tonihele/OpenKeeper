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
package toniarts.openkeeper.view.effect;

import com.jme3.effect.Particle;
import com.jme3.effect.influencers.ParticleInfluencer;
import com.jme3.effect.shapes.EmitterShape;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import toniarts.openkeeper.tools.convert.map.EffectElement;

import java.io.IOException;

/**
 * Draws a fresh, independent velocity sample from the element's own
 * minSpeedXy/maxSpeedXy/minSpeedYz/maxSpeedYz ranges
 * ({@link EffectControl#calculateVelocity}) for every spawned particle,
 * instead of jME3's default {@code DefaultParticleInfluencer} behavior of
 * sharing one velocity vector across the whole burst and only lightly
 * varying it - that made a burst look like a tight clump barely spreading
 * across the configured speed range rather than genuinely scattering, the
 * way mesh-type elements already do via their own per-instance
 * {@code EffectElementControl}.
 */
public class EffectParticleInfluencer implements ParticleInfluencer {

    private EffectElement element;

    /**
     * For serialization only. Do not use.
     */
    public EffectParticleInfluencer() {
    }

    public EffectParticleInfluencer(EffectElement element) {
        this.element = element;
    }

    @Override
    public void influenceParticle(Particle particle, EmitterShape emitterShape) {
        emitterShape.getRandomPoint(particle.position);

        Vector3f velocity = EffectControl.calculateVelocity(element);
        // Capping only the upper half of the vertical launch range tames the
        // rare high-outlier throw that (thanks to gravity having to first
        // cancel it out before the particle can even start falling back
        // down) hangs in the air for far longer than the rest of the burst,
        // without narrowing the horizontal spread this influencer exists for.
        float maxTypicalSpeedYz = (element.getMinSpeedYz() + element.getMaxSpeedYz()) / 8f;
        velocity.y = Math.min(velocity.y, maxTypicalSpeedYz);
        particle.velocity.set(velocity);
    }

    @Override
    public ParticleInfluencer clone() {
        Cloner cloner = new Cloner();
        return cloner.clone(this);
    }

    @Override
    public void setInitialVelocity(Vector3f initialVelocity) {
        // Unused - every particle draws its own velocity sample instead.
    }

    @Override
    public Vector3f getInitialVelocity() {
        return Vector3f.ZERO;
    }

    @Override
    public void setVelocityVariation(float variation) {
        // Unused - every particle draws its own velocity sample instead.
    }

    @Override
    public float getVelocityVariation() {
        return 0f;
    }

    /**
     * Called internally by {@link Cloner}. Do not call directly.
     */
    @Override
    public Object jmeClone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public void cloneFields(Cloner cloner, Object original) {
        // EffectElement is shared, immutable reference data - nothing to clone.
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        // Effects are never serialized to disk.
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        // Effects are never serialized to disk.
    }
}
