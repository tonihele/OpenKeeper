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
import toniarts.openkeeper.tools.convert.map.EffectElement;
import static toniarts.openkeeper.world.effect.EffectControl.calculateVelocity;

/**
 *
 * @author ArchDemon
 */
@Deprecated
public abstract class EffectElementControl extends AbstractControl {
    private EffectElement effect;

    private float hpCurrent;
    private float hp;
    private float height;
    private FloatLimit scale;
    private float scaleRatio;
    private Vector3f velocity;

    private static final Logger log = Logger.getLogger(EffectElementControl.class.getName());
    /**
     * For serialization only. Do not use.
     */
    public EffectElementControl() {
        super();
    }

    public EffectElementControl(EffectElement effect) {
        this.effect = effect;
        initiazize();
    }

    private void initiazize() {
        hp = hpCurrent = FastMath.nextRandomInt(effect.getMaxHp(), effect.getMaxHp());

        velocity = calculateVelocity(effect);
        //height = FastMath.nextRandomInt(effect.getLowerHeightLimit(), effect.getUpperHeightLimit());

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
            scale.sub(scaleRatio);
            spatial.setLocalScale(scale.getValue());
        } else if (effect.getFlags().contains(EffectElement.EffectElementFlag.EXPAND)) {
            scale.add(scaleRatio);
            spatial.setLocalScale(scale.getValue());
        }

        if (velocity != Vector3f.ZERO) {
            Vector3f location = spatial.getLocalTranslation().clone().addLocal(velocity);
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
