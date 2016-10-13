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
package toniarts.openkeeper.world.effect;

import com.jme3.effect.Particle;
import com.jme3.light.Light;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;
import toniarts.openkeeper.tools.convert.map.Effect;
import toniarts.openkeeper.tools.convert.map.EffectElement;

/**
 *
 * @author ArchDemon
 */
public abstract class SpatialEmitter extends Node {
    private boolean enabled = true;

    //private static final EmitterShape DEFAULT_SHAPE = new EmitterPointShape(Vector3f.ZERO);
    //private EmitterShape shape = DEFAULT_SHAPE;

    private Spatial spatial;
    private Particle[] particles;
    private float lowLife = 3f;
    private float highLife = 7f;

    private float gravity = 0;
    private float rotateSpeed;
    private Vector3f initialVelocity = Vector3f.ZERO;

    private float startSize = 0.2f;
    private float endSize = 2f;
    //variable that helps with computations
    private EffectElement effectElement;
    private Effect effect;
    private transient Vector3f temp = new Vector3f();

    public SpatialEmitter(String name, EffectElement effectElement, Effect effect) {
        super(name);
        this.effectElement = effectElement;
        this.effect = effect;
        this.setShadowMode(RenderQueue.ShadowMode.Off);
    }

    /**
     * For serialization only. Do not use.
     */
    public SpatialEmitter() {
        super();
    }

    public Spatial getSpatial() {
        return spatial;
    }

    public void setSpatial(Spatial spatial) {
        this.spatial = spatial;
    }

    public float getRotateSpeed() {
        return rotateSpeed;
    }

    public void setRotateSpeed(float rotateSpeed) {
        this.rotateSpeed = rotateSpeed;
    }

    public float getEndSize() {
        return endSize;
    }

    public void setEndSize(float endSize) {
        this.endSize = endSize;
    }

    public float getGravity() {
        return gravity;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public float getHighLife() {
        return highLife;
    }

    public void setHighLife(float highLife) {
        this.highLife = highLife;
    }

    public float getLowLife() {
        return lowLife;
    }

    public void setLowLife(float lowLife) {
        this.lowLife = lowLife;
    }
    public float getStartSize() {
        return startSize;
    }

    public void setStartSize(float startSize) {
        this.startSize = startSize;
    }

    public Vector3f getInitialVelocity() {
        return initialVelocity;
    }

    public void setInitialVelocity(Vector3f initialVelocity) {
        this.initialVelocity = initialVelocity;
    }

    private void emitParticle(int idx, Vector3f min, Vector3f max) {
        Particle p = particles[idx];

        p.startlife = lowLife + FastMath.nextRandomFloat() * (highLife - lowLife);
        p.life = p.startlife;

        p.size = startSize;
        //shape.getRandomPoint(p.position);
        p.velocity.set(initialVelocity);

        if (rotateSpeed != 0) {
            p.rotateSpeed = rotateSpeed * (0.2f + (FastMath.nextRandomFloat() * 2f - 1f) * .8f);
        }

        temp.set(p.position).addLocal(p.size, p.size, p.size);
        max.maxLocal(temp);
        temp.set(p.position).subtractLocal(p.size, p.size, p.size);
        min.minLocal(temp);

    }

    public void emitAllParticles() {
        for (int i = 0; i < effect.getElementsPerTurn(); i++) {
            Spatial s = spatial.clone();
            if (effect.getFlags().contains(Effect.EffectFlag.RANDOM_DISTRIBUTION)) {

            } else if (effect.getFlags().contains(Effect.EffectFlag.UNIFORM_DISTRIBUTION)) {
                // TODO add to all s uniform parameters
            }
            s.addControl(new EffectElementControl(effectElement) {

                @Override
                public void onDie(Vector3f location) {
                    SpatialEmitter.this.onDeath(location);
                }

                @Override
                public void onHit(Vector3f location) {
                    SpatialEmitter.this.onHit(location);
                }
            });

            this.attachChild(s);
        }
    }

    public void killAllParticles() {
        for (Spatial s : this.getChildren()) {
            s.removeFromParent();
        }
    }

    private void killParticle(int index) {
        this.getChild(index).removeFromParent();
    }

    private void updateParticle(Particle p, float tpf, Vector3f min, Vector3f max){
        // applying gravity
        p.velocity.y -= gravity * tpf;
        temp.set(p.velocity).multLocal(tpf);
        p.position.addLocal(temp);

        // affecting color, size and angle
        float b = (p.startlife - p.life) / p.startlife;
        p.size = FastMath.interpolateLinear(b, startSize, endSize);
        p.angle += p.rotateSpeed * tpf;

        // Computing bounding volume
        temp.set(p.position).addLocal(p.size, p.size, p.size);
        max.maxLocal(temp);
        temp.set(p.position).subtractLocal(p.size, p.size, p.size);
        min.minLocal(temp);
    }

    private void updateParticleState(float tpf) {
        // Force world transform to update
        this.getWorldTransform();

        TempVars vars = TempVars.get();

        Vector3f min = vars.vect1.set(Vector3f.POSITIVE_INFINITY);
        Vector3f max = vars.vect2.set(Vector3f.NEGATIVE_INFINITY);

        for (int i = 0; i < particles.length; ++i) {
            Particle p = particles[i];
            if (p.life == 0) { // particle is dead
//                assert i <= firstUnUsed;
                continue;
            }

            p.life -= tpf;
            if (p.life <= 0) {
                this.killParticle(i);
                continue;
            }

            updateParticle(p, tpf, min, max);
        }

        this.setBoundRefresh();

        vars.release();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public abstract void onDeath(Vector3f location);
    public abstract void onHit(Vector3f location);
}
