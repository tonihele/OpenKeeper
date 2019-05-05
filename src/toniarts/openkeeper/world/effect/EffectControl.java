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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.map.Effect;

/**
 *
 * @author ArchDemon
 */
@Deprecated
public abstract class EffectControl extends AbstractControl {
    private Effect effect;

    private float hpCurrent;
    private float hp;
    private float height;
    private FloatLimit scale;
    private float scaleRatio;
    private Vector3f velocity;

    private static final Logger log = Logger.getLogger(EffectControl.class.getName());
    /**
     * For serialization only. Do not use.
     */
    public EffectControl() {
        super();
    }

    public EffectControl(Effect effect) {
        this.effect = effect;
        initiazize();
    }

    private void initiazize() {
        hp = hpCurrent = FastMath.nextRandomInt(effect.getMaxHp(), effect.getMaxHp());

        velocity = calculateVelocity(effect);
        height = FastMath.nextRandomInt(effect.getLowerHeightLimit(), effect.getUpperHeightLimit());

        if (effect.getFlags().contains(Effect.EffectFlag.SHRINK)) {
            scale = new FloatLimit(effect.getMaxScale(), effect.getMaxScale(), effect.getMinScale());
            scaleRatio = (effect.getMaxScale() - effect.getMinScale()) / hp;
        } else if (effect.getFlags().contains(Effect.EffectFlag.EXPAND)) {
            scale = new FloatLimit(effect.getMinScale(), effect.getMaxScale(), effect.getMinScale());
            scaleRatio = (effect.getMaxScale() - effect.getMinScale()) / hp;
        } else if (effect.getFlags().contains(Effect.EffectFlag.EXPAND_THEN_SHRINK)) {
            scale  = new FloatLimit(effect.getMinScale(), effect.getMaxScale(), effect.getMinScale());
            scaleRatio = (effect.getMaxScale() - effect.getMinScale()) * 2 / hp;
        } else {
            scale = new FloatLimit(effect.getMinScale() + FastMath.nextRandomFloat() * (effect.getMaxScale() - effect.getMinScale()));
            scaleRatio = 0;
        }
    }

    public static Vector3f calculateVelocity(IEffect speed) {

        float xzAngle = FastMath.nextRandomFloat() * FastMath.TWO_PI;
        float xzSpeed = speed.getMinSpeedXy()+ FastMath.nextRandomFloat() * (speed.getMaxSpeedXy() - speed.getMinSpeedXy());
        Vector3f vel = new Vector3f(-(float) Math.sin(xzAngle), 0, (float) Math.cos(xzAngle)).multLocal(xzSpeed);

        // float zyAngle = FastMath.nextRandomFloat() * FastMath.TWO_PI;
        float zySpeed = speed.getMinSpeedYz() + FastMath.nextRandomFloat() * (speed.getMaxSpeedYz() - speed.getMinSpeedYz());
        vel.y = FastMath.nextRandomFloat() * zySpeed;

        return vel;
    }

//    public static Vector3f calculateVelocity(IEffect speed) {
//
//        float zySpeed = speed.getMinSpeedYz() + FastMath.nextRandomFloat() * (speed.getMaxSpeedYz() - speed.getMinSpeedYz());
//        float zyAngle = FastMath.nextRandomFloat() * FastMath.TWO_PI;
//
//        Vector3f result = new Vector3f(-(float) Math.sin(zyAngle), 0, (float) Math.cos(zyAngle)).multLocal(zySpeed);
//
//        float xzSpeed = speed.getMinSpeedXy()+ FastMath.nextRandomFloat() * (speed.getMaxSpeedXy() - speed.getMinSpeedXy());
//        //float xzAngle = FastMath.nextRandomFloat() * FastMath.TWO_PI;
//        result.y = FastMath.nextRandomFloat() * xzSpeed;
//
//        return result;
//    }

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

        if (effect.getFlags().contains(Effect.EffectFlag.SHRINK)) {
            scale.sub(scaleRatio * tpf);
            spatial.setLocalScale(scale.getValue());
        } else if (effect.getFlags().contains(Effect.EffectFlag.EXPAND)) {
            scale.add(scaleRatio * tpf);
            spatial.setLocalScale(scale.getValue());
        } else if (effect.getFlags().contains(Effect.EffectFlag.EXPAND_THEN_SHRINK)) {
            if (hpCurrent > hp / 2) {
                scale.add(scaleRatio * tpf * 2);
            } else {
                scale.sub(scaleRatio * tpf * 2);
            }
            spatial.setLocalScale(scale.getValue());
            //System.out.println(hpCurrent + ": " + scale.getValue());
        }

        if (velocity != Vector3f.ZERO) {
            Vector3f location = spatial.getLocalTranslation().clone().addLocal(velocity.mult(tpf));
            if (location.y > height) {
                location.y = height;
            }
            spatial.setLocalTranslation(location);
            //System.out.println(location);
        }

        if (effect.getAirFriction() != 0) {
            velocity.x -= effect.getAirFriction() * tpf;
            velocity.y -= effect.getAirFriction() * tpf;
            velocity.z -= effect.getAirFriction() * tpf;
        }

        if (effect.getMass() != 0) {
            velocity.y -= effect.getMass() * tpf;
        }

        if (isHit()) {
            hpCurrent = 0;
            onHit(spatial.getLocalTranslation());
            spatial.removeFromParent();
            spatial.removeControl(this);
        }

        //hpCurrent -= 1.0f / 4; // FIXME
        hpCurrent -= tpf * 4; // FIXME
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

    private boolean isHit() {
        return false;
    }

    public abstract void onDie(Vector3f location);
    public abstract void onHit(Vector3f location);
}
