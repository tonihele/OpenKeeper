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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import toniarts.openkeeper.tools.convert.map.EffectElement;
import toniarts.openkeeper.utils.WorldUtils;

import java.lang.System.Logger;

import static toniarts.openkeeper.view.effect.EffectControl.calculateVelocity;

/**
 *
 * @author ArchDemon
 */
public abstract class EffectElementControl extends AbstractControl {

    private static final Logger log = System.getLogger(EffectElementControl.class.getName());

    /**
     * Conversion from the file's mass unit (float32, 4096 = 1.0) to
     * tiles/s^2, derived from the effect clock (20 Hz) and the position vs.
     * velocity fixed-point precision difference (16x)
     */
    private static final float GRAVITY_FACTOR = 25f;

    private final EffectElement effect;
    private final int whirlpoolRate;

    private float hpCurrent;
    private float hp;
    private FloatLimit scale;
    private float scaleRatio;
    private Vector3f velocity;
    private float spinX;
    private float spinY;
    private float spinZ;
    private boolean landed;
    private boolean whirled;

    /**
     * For serialization only. Do not use.
     */
    public EffectElementControl() {
        super();
        effect = null;
        whirlpoolRate = 0;
    }

    protected EffectElementControl(EffectElement effect, int spinRateRange, int whirlpoolRate) {
        this.effect = effect;
        this.whirlpoolRate = whirlpoolRate;
        initialize(EffectControl.randomSpinRate(spinRateRange), EffectControl.randomSpinRate(spinRateRange),
                EffectControl.randomSpinRate(spinRateRange));
    }

    /**
     * Re-initializes a single burst element with a spin already rolled
     * elsewhere - used for an in-place {@code deathElementId == self}
     * respawn
     */
    protected EffectElementControl(EffectElement effect, float spinX, float spinY, float spinZ, int whirlpoolRate) {
        this.effect = effect;
        this.whirlpoolRate = whirlpoolRate;
        initialize(spinX, spinY, spinZ);
    }

    private void initialize(float spinX, float spinY, float spinZ) {
        hp = hpCurrent = FastMath.nextRandomInt(effect.getMinHp(), effect.getMaxHp()) / 20f;

        velocity = calculateVelocity(effect);

        this.spinX = spinX;
        this.spinY = spinY;
        this.spinZ = spinZ;

        if (effect.getFlags().contains(EffectElement.EffectElementFlag.SHRINK)) {
            scale = new FloatLimit(effect.getMaxScale());
            scaleRatio = (effect.getMaxScale() - effect.getMinScale()) / hp;
        } else if (effect.getFlags().contains(EffectElement.EffectElementFlag.EXPAND)) {
            scale = new FloatLimit(effect.getMinScale());
            scaleRatio = (effect.getMaxScale() - effect.getMinScale()) / hp;
        } else {
            scale = new FloatLimit(effect.getMinScale() + FastMath.nextRandomFloat() * (effect.getMaxScale() - effect.getMinScale()));
            scaleRatio = 0;
        }
    }

    public float getSpinX() {
        return spinX;
    }

    public float getSpinY() {
        return spinY;
    }

    public float getSpinZ() {
        return spinZ;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);

        if (spatial != null) {
            this.spatial.setLocalScale(scale.getValue());
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (!enabled || spatial == null) {
            return;
        }

        if (effect.getFlags().contains(EffectElement.EffectElementFlag.SHRINK)) {
            scale.sub(scaleRatio * tpf);
            spatial.setLocalScale(scale.getValue());
        } else if (effect.getFlags().contains(EffectElement.EffectElementFlag.EXPAND)) {
            scale.add(scaleRatio * tpf);
            spatial.setLocalScale(scale.getValue());
        }

        if (spinX != 0 || spinY != 0 || spinZ != 0) {
            spatial.rotate(spinX * tpf, spinY * tpf, spinZ * tpf);
        }

        if (!landed && velocity != Vector3f.ZERO) {
            // The parent chain's world Y offset, read before this frame's own
            // translation change so it reflects the last fully-updated scene
            // graph state (valid from the frame after the spatial is attached).
            float floorHeightLocal = WorldUtils.FLOOR_HEIGHT
                    - (spatial.getWorldTranslation().y - spatial.getLocalTranslation().y);
            Vector3f preTickLocation = spatial.getLocalTranslation();
            Vector3f location = preTickLocation.clone().addLocal(velocity.mult(tpf));

            if (whirlpoolRate != 0) {
                // Not whirled on the spawn tick - the first rotation is the
                // tick after spawn, using the annulus radius (plus this
                // tick's own velocity move) as d.
                if (whirled) {
                    Vector3f base = (spatial.getParent() != null) ? spatial.getParent().getLocalTranslation() : Vector3f.ZERO;
                    location.addLocal(EffectControl.whirlpoolDelta(whirlpoolRate,
                            base.x + preTickLocation.x, base.z + preTickLocation.z, tpf));
                }
                whirled = true;
            }

            boolean directionalFriction = effect.getFlags().contains(EffectElement.EffectElementFlag.DIRECTIONAL_FRICTION);

            if (location.y < floorHeightLocal) {
                if (directionalFriction) {
                    // Sticks where it lands instead of bouncing - elasticity
                    // is ignored here, and unlike the bounce below the spin
                    // rates are left untouched so it keeps tumbling in place
                    // until hp runs out.
                    location.y = floorHeightLocal;
                    velocity.set(Vector3f.ZERO);
                    landed = true;
                } else {
                    float e = effect.getElasticity();
                    location.y = floorHeightLocal + (floorHeightLocal - location.y) * e;
                    velocity.x *= e;
                    velocity.z *= e;
                    velocity.y = -velocity.y * e;
                    spinX = spinY = spinZ = 0f;
                }
            }

            spatial.setLocalTranslation(location);

            if (effect.getAirFriction() != 0) {
                velocity.multLocal(FastMath.pow(1f - 16f * effect.getAirFriction(), tpf * 20f));
            }

            if (effect.getMass() != 0) {
                velocity.y -= effect.getMass() * GRAVITY_FACTOR * tpf;
            }
        }

        if (isHit()) {
            hpCurrent = 0;
            onHit(null);
        }

        hpCurrent-= tpf;
        if (hpCurrent <= 0) {
            onDie(spatial.getLocalTranslation());
            spatial.removeFromParent();
            spatial.removeControl(this);
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // nothing
    }

    /**
     * TODO how we get collision ?
     * @return
     */
    private boolean isHit() {
        return false;
    }

    public abstract void onDie(Vector3f location);
    public abstract void onHit(Vector3f location);
}
