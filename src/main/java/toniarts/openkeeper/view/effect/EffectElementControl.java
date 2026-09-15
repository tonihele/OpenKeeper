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
     * velocity fixed-point precision difference (16x). See
     * dig_rubble_effect.md §4.
     */
    private static final float GRAVITY_FACTOR = 25f;

    private final EffectElement effect;
    private final int spinRateRange;

    private float hpCurrent;
    private float hp;
    private float height;
    private FloatLimit scale;
    private float scaleRatio;
    private Vector3f velocity;
    private float spinX;
    private float spinY;
    private float spinZ;
    private float floorHeightLocal;

    /**
     * For serialization only. Do not use.
     */
    public EffectElementControl() {
        super();
        effect = null;
        spinRateRange = 0;
    }

    public EffectElementControl(EffectElement effect, int spinRateRange) {
        this.effect = effect;
        this.spinRateRange = spinRateRange;
        initiazize();
    }

    private void initiazize() {
        hp = hpCurrent = FastMath.nextRandomInt(effect.getMinHp(), effect.getMaxHp()) / 20f;

        velocity = calculateVelocity(effect);

        float r = spinRateRange * 8f;
        spinX = randSpin(r);
        spinY = randSpin(r);
        spinZ = randSpin(r);

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

    /**
     * A random per-axis spin rate in rad/s, converted from the file's
     * 2048-per-turn, per-tick unit ({@code rand(r) - r/2}) via the 20 Hz
     * effect clock.
     */
    private static float randSpin(float r) {
        if (r == 0) {
            return 0;
        }
        return (FastMath.nextRandomFloat() * r - r / 2f) * FastMath.TWO_PI / 2048f * 20f;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);

        if (spatial != null) {
            this.spatial.setLocalScale(scale.getValue());
            floorHeightLocal = WorldUtils.FLOOR_HEIGHT
                    - (spatial.getWorldTranslation().y - spatial.getLocalTranslation().y);
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

        if (velocity != Vector3f.ZERO) {
            Vector3f location = spatial.getLocalTranslation().clone().addLocal(velocity.mult(tpf));
            if (location.y > height) {
                location.y = height;
            }

            spatial.setLocalTranslation(location);
        }

        if (effect.getAirFriction() != 0) {
            velocity.multLocal(FastMath.pow(1f - 16f * effect.getAirFriction(), tpf * 20f));
        }

        if (effect.getMass() != 0) {
            velocity.y -= effect.getMass() * GRAVITY_FACTOR * tpf;
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
