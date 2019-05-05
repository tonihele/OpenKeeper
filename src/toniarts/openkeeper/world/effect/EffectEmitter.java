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

import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.tools.convert.map.Effect;
import toniarts.openkeeper.tools.convert.map.EffectElement;

/**
 *
 * @author ArchDemon
 */
@Deprecated
public abstract class EffectEmitter extends Node {
    private boolean enabled = true;

    //private static final EmitterShape DEFAULT_SHAPE = new EmitterPointShape(Vector3f.ZERO);
    //private EmitterShape shape = DEFAULT_SHAPE;

    private Spatial spatial;

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

    public EffectEmitter(EffectElement effectElement, Effect effect) {
        super(effectElement.getName());
        this.effectElement = effectElement;
        this.effect = effect;
        this.setShadowMode(RenderQueue.ShadowMode.Off);
    }

    /**
     * For serialization only. Do not use.
     */
    public EffectEmitter() {
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
                    EffectEmitter.this.onDeath(location);
                }

                @Override
                public void onHit(Vector3f location) {
                    EffectEmitter.this.onHit(location);
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

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        for (Spatial s : this.getChildren()) {
            s.getControl(EffectElementControl.class).setEnabled(enabled);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public abstract void onDeath(Vector3f location);
    public abstract void onHit(Vector3f location);
}
