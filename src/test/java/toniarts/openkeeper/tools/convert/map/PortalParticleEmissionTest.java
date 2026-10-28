/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.tools.convert.map;

import com.jme3.effect.Particle;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.shapes.EmitterPointShape;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.effect.EffectParticleInfluencer;
import toniarts.openkeeper.view.effect.VisualEffect;

import static org.junit.jupiter.api.Assertions.*;

class PortalParticleEmissionTest {
    @Test
    void delayedUpdatesEmitOnePacketAndAdvanceLifeByElapsedNativeSteps() throws Exception {
        Fixture f = new Fixture();
        f.effect.generateThroughoutLifetime();
        assertEquals(0, f.emitter.getParticlesPerSec());
        assertEquals(0, f.emitter.getNumVisibleParticles());
        assertTrue(f.emitter.getMaxNumParticles() >= 6 * 40);
        assertTrue(f.effect.update(0.4f)); // Eight native steps, one packet.
        assertEquals(6, f.emitter.getNumVisibleParticles());
        assertTrue(f.effect.update(0.55f)); // Eleven native steps, one packet.
        assertEquals(12, f.emitter.getNumVisibleParticles());
        assertTrue(f.effect.update(0.6f)); // Twelve native steps; generator expires.
        assertEquals(18, f.emitter.getNumVisibleParticles());
        assertTrue(f.effect.update(0.5f));
        assertEquals(18, f.emitter.getNumVisibleParticles(), "Existing particles survive generator expiry");
    }

    @Test
    void regularUpdatesEmitForThirtyNativeSteps() throws Exception {
        Fixture f = new Fixture();
        f.effect.generateThroughoutLifetime();
        assertTrue(f.effect.update(0.025f));
        assertEquals(0, f.emitter.getNumVisibleParticles());
        assertTrue(f.effect.update(0.025f));
        assertEquals(6, f.emitter.getNumVisibleParticles());
        for (int step = 1; step < 30; step++) {
            assertTrue(f.effect.update(0.05f));
        }
        assertEquals(180, f.emitter.getNumVisibleParticles());
        assertTrue(f.effect.update(0.05f));
        assertEquals(180, f.emitter.getNumVisibleParticles());
    }

    @Test
    void particleUsesAuthoredVerticalSpeedWithoutAdditionalRandomizationOrCap() {
        EffectElement element = new EffectElement();
        element.setMinSpeedYz(3);
        element.setMaxSpeedYz(3);
        Particle particle = new Particle();
        new EffectParticleInfluencer(element).influenceParticle(particle, new EmitterPointShape(Vector3f.ZERO));
        assertEquals(0, particle.velocity.x, 0f);
        assertEquals(3, particle.velocity.y, 0f);
        assertEquals(0, particle.velocity.z, 0f);
    }

    @Test
    void stationaryCoverupRemainsStationary() {
        Particle particle = new Particle();
        new EffectParticleInfluencer(new EffectElement()).influenceParticle(particle, new EmitterPointShape(Vector3f.ZERO));
        assertEquals(0, particle.velocity.length(), 0f);
    }

    @Test
    void removingSourceRemovesCoverupAndStopsEffect() throws Exception {
        Fixture f = new Fixture();
        f.effect.removeEffect();
        assertEquals(0, f.root.getQuantity());
        assertFalse(f.effect.update(0.1f));
    }

    private static final class Fixture {
        final Node root = new Node();
        final ParticleEmitter emitter = new ParticleEmitter("mist", ParticleMesh.Type.Triangle, 6);
        final VisualEffect effect;
        final Effect definition = new Effect();

        @SuppressWarnings("unchecked")
        Fixture() throws Exception {
            definition.setFlags(EnumSet.of(Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS));
            definition.setGenerateIds(List.of());
            definition.setElementsPerTurn((short) 6);
            definition.setMinHp(30);
            definition.setMaxHp(30);
            effect = new VisualEffect(new EffectManagerState(null, null), root, definition);
            EffectElement element = new EffectElement();
            element.setMaxHp(40);
            // Inject a plain jME emitter to test lifecycle without a graphics context or converted DKII assets.
            var field = VisualEffect.class.getDeclaredField("effectElements");
            field.setAccessible(true);
            ((Map<Spatial, EffectElement>) field.get(effect)).put(emitter, element);
            ((Node) root.getChild(0)).attachChild(emitter);
        }
    }
}
