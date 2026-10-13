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
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A {@link ParticleEmitter} extended with the per-tick air friction and
 * DIRECTIONAL_FRICTION sway/stick physics that mesh-based effect elements
 * already get from {@link EffectElementControl} - jME3's own particle update
 * only ever applies a constant gravity vector to each particle's velocity
 * (see {@code ParticleEmitter.updateParticle}) and has no friction or
 * per-particle lateral drift of its own, which otherwise leaves ALPHA/SPRITE
 * elements (e.g. feathers) drifting in a straight line forever instead of
 * settling to the floor like their mesh-based counterparts.
 */
public class EffectParticleEmitter extends ParticleEmitter {

    /**
     * Tunable: no confirmed source value for how strongly elasticity should
     * translate into sway magnitude; adjust by playtesting.
     */
    private static final float DIRECTIONAL_FRICTION_DRIFT_AMPLITUDE = 0.2f;

    private final float airFriction;
    private final float elasticity;
    private final boolean directionalFriction;
    private final int whirlpoolRate;

    public EffectParticleEmitter(String name, ParticleMesh.Type type, int numParticles,
            float airFriction, float elasticity, boolean directionalFriction, int whirlpoolRate) {
        super(name, type, numParticles);
        this.airFriction = airFriction;
        this.elasticity = elasticity;
        this.directionalFriction = directionalFriction;
        this.whirlpoolRate = whirlpoolRate;
    }

    @Override
    protected void updateParticle(Particle p, float tpf, Vector3f min, Vector3f max) {
        if (airFriction != 0) {
            p.velocity.multLocal(FastMath.pow(1f - 16f * airFriction, tpf * 20f));
        }

        // The parent emitter's world Y offset - particles are stored in
        // the emitter's local space (see VisualEffect.createParticleElement's
        // setInWorldSpace(false)), same convention as EffectElementControl.
        float floorHeightLocal = WorldUtils.FLOOR_HEIGHT
                - (getWorldTranslation().y - getLocalTranslation().y);

        // A particle's first-ever update call always has life == startlife -
        // tpf (jME3 decrements life by tpf before calling updateParticle, in
        // every emission path), so this is the "just spawned" check - not
        // whirled on the spawn tick, same as EffectElementControl.
        boolean justSpawned = (p.startlife - p.life) <= tpf + 1e-4f;
        // Unlike EffectElementControl's persistent `landed` flag, a stuck
        // DIRECTIONAL_FRICTION particle re-derives floor contact every tick
        // from its position alone - checked pre-tick here so a landed
        // particle doesn't get nudged sideways by whirl forever.
        boolean wasLanded = directionalFriction && p.position.y <= floorHeightLocal;
        float preX = p.position.x;
        float preZ = p.position.z;

        // Gravity + position integration + color/size/angle/bounding volume
        // upkeep are all private to the base class, so they still have to
        // run through here rather than being reimplemented.
        super.updateParticle(p, tpf, min, max);

        if (whirlpoolRate != 0 && !justSpawned && !wasLanded) {
            Vector3f base = getLocalTranslation();
            Vector3f delta = EffectControl.whirlpoolDelta(whirlpoolRate / 4, base.x + preX, base.z + preZ, tpf);
            p.position.x += delta.x;
            p.position.z += delta.z;
        }

        if (directionalFriction) {
            if (p.position.y < floorHeightLocal) {
                // Sticks where it lands instead of bouncing or sinking
                // through the floor, and stops spinning rather than
                // tumbling in place forever.
                p.position.y = floorHeightLocal;
                p.velocity.set(Vector3f.ZERO);
                p.rotateSpeed = 0f;
            } else {
                // Falls down in a sine wave, like a rubber band - elasticity
                // sets how wide the swing is - with each feather swinging at
                // its own random frequency and in its own random direction
                // (the particle pool has no room for custom fields, so its
                // own identity hash stands in for those two random values).
                float elapsed = p.startlife - p.life;
                int hash = System.identityHashCode(p);
                float frequency = 1f + (hash % 100) / 25f;
                float direction = (hash / 100 % 360) * FastMath.DEG_TO_RAD;
                float sway = FastMath.sin(elapsed * frequency) * DIRECTIONAL_FRICTION_DRIFT_AMPLITUDE * elasticity * tpf;
                p.position.x += sway * FastMath.cos(direction);
                p.position.z += sway * FastMath.sin(direction);
            }
        }
    }
}
