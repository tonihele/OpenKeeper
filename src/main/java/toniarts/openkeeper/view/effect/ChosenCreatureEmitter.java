/*
 * Copyright (C) 2026 OpenKeeper
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package toniarts.openkeeper.view.effect;

import com.jme3.effect.Particle;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import toniarts.openkeeper.tools.convert.map.Effect;
import toniarts.openkeeper.tools.convert.map.EffectElement;

/** The point-emitted, rising sparkles of the possession creation effect. */
final class ChosenCreatureEmitter extends ParticleEmitter {

    private static final float STEP = 1f / 20;
    private static final float POSITION_UNITS = 4096;
    private static final float VELOCITY_TO_SECONDS = 20f / (4096 * 16);
    private final EffectElement element;
    private final int packetSize;
    private final int emissionTicks;
    private final float drag;
    private final int mass;
    private float remainder;
    private int elapsedTicks;
    private boolean started;

    ChosenCreatureEmitter(Effect effect, EffectElement element) {
        super(element.getName(), ParticleMesh.Type.Triangle,
                Math.max(1, effect.getElementsPerTurn() * Math.min(effect.getMaxHp(), element.getMaxHp())));
        this.element = element;
        packetSize = effect.getElementsPerTurn();
        emissionTicks = effect.getMaxHp();
        drag = Math.max(0, 1 - element.getAirFriction() * 16);
        mass = Math.round(element.getMass() * POSITION_UNITS);
        setInWorldSpace(true);
        setParticlesPerSec(0);
        setLowLife(element.getMinHp() * STEP);
        setHighLife(element.getMaxHp() * STEP);
        setStartSize(element.getMinScale() / 2);
        setEndSize(element.getMinScale() / 2);
        var color = element.getColor();
        setStartColor(new ColorRGBA(color.getRed() / 255f, color.getGreen() / 255f,
                color.getBlue() / 255f, 1));
        setEndColor(getStartColor());
        setParticleInfluencer(new EffectParticleInfluencer(element) {
            @Override
            public void influenceParticle(Particle particle, com.jme3.effect.shapes.EmitterShape shape) {
                // The native capture starts at the creature, without a height-band offset.
                particle.position.set(Vector3f.ZERO);
                int angle = FastMath.nextRandomInt(0, 2047);
                float radians = angle * FastMath.TWO_PI / 2048;
                int speed = Math.round((element.getMinSpeedXy() + FastMath.nextRandomFloat()
                        * (element.getMaxSpeedXy() - element.getMinSpeedXy())) * POSITION_UNITS);
                int horizontalX = (int) (FastMath.cos(radians) * speed);
                int horizontalZ = (int) (FastMath.sin(radians) * speed);
                particle.velocity.set(horizontalX * VELOCITY_TO_SECONDS, 0,
                        horizontalZ * VELOCITY_TO_SECONDS);
                particle.rotateSpeed = EffectControl.randomSpinRate(effect.getSpriteSpinRateRange());
            }
        });
        setRandomAngle(true);
    }

    @Override
    public void emitAllParticles() {
        if (!started) {
            started = true;
            emitParticles(packetSize);
        }
    }

    @Override
    public void updateFromControl(float tpf) {
        if (!started || tpf <= 0) {
            return;
        }
        remainder += tpf;
        while (remainder + 1e-6f >= STEP) {
            remainder = Math.max(0, remainder - STEP);
            super.updateFromControl(STEP);
            if (++elapsedTicks < emissionTicks) {
                emitParticles(packetSize);
            }
        }
    }

    @Override
    protected void updateParticle(Particle particle, float tpf, Vector3f min, Vector3f max) {
        // Native velocities use four more fractional bits than positions.
        // Apply buoyancy before drag, then the arithmetic shift used by movement.
        int vx = (int) (Math.round(particle.velocity.x / VELOCITY_TO_SECONDS) * drag);
        int vy = (int) ((Math.round(particle.velocity.y / VELOCITY_TO_SECONDS) - mass) * drag);
        int vz = (int) (Math.round(particle.velocity.z / VELOCITY_TO_SECONDS) * drag);
        particle.position.x += (vx >> 4) / POSITION_UNITS;
        particle.position.y += (vy >> 4) / POSITION_UNITS;
        particle.position.z += (vz >> 4) / POSITION_UNITS;
        particle.velocity.set(vx * VELOCITY_TO_SECONDS, vy * VELOCITY_TO_SECONDS, vz * VELOCITY_TO_SECONDS);

        int life = Math.round(particle.life / STEP);
        if (element.getFlags().contains(EffectElement.EffectElementFlag.FADE) && life > 0) {
            // The native 100% fade setting darkens RGB; it leaves alpha opaque.
            int r = Math.round(particle.color.r * 255);
            int g = Math.round(particle.color.g * 255);
            int b = Math.round(particle.color.b * 255);
            if (Math.max(r, Math.max(g, b)) >= life) {
                particle.color.set((r * (life - 1) / life) / 255f,
                        (g * (life - 1) / life) / 255f, (b * (life - 1) / life) / 255f, 1);
            }
        }
        particle.angle += particle.rotateSpeed * tpf;
        if (!isSelectRandomImage()) {
            particle.imageIndex = Math.min(getImagesX() * getImagesY() - 1,
                    (int) ((1 - particle.life / particle.startlife) * getImagesX() * getImagesY()));
        }
        min.x = Math.min(min.x, particle.position.x - particle.size);
        min.y = Math.min(min.y, particle.position.y - particle.size);
        min.z = Math.min(min.z, particle.position.z - particle.size);
        max.x = Math.max(max.x, particle.position.x + particle.size);
        max.y = Math.max(max.y, particle.position.y + particle.size);
        max.z = Math.max(max.z, particle.position.z + particle.size);
    }
}
