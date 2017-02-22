/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.world.control;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import toniarts.openkeeper.world.MapLoader;

/**
 *
 * @author ArchDemon
 */
public abstract class LandingControl extends AbstractControl {
    private static final float GRAVITY = 2.0f;
    private final Vector2f position;

    public LandingControl(Vector2f position) {
        super();

        this.position = position;
        enabled = spatial != null;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);

        if (spatial != null) {
            spatial.setLocalTranslation(position.x, MapLoader.TILE_WIDTH * 2f, position.y);
        }
        enabled = spatial != null;
    }

    @Override
    protected void controlUpdate(float tpf) {
        Vector3f pos = spatial.getLocalTranslation();
        // FIXME set real height by BoundingBox height
        if (pos.y > 0.3f) {
            spatial.move(0, -tpf * GRAVITY, 0);
        } else {
            enabled = false;
            spatial.setLocalTranslation(pos.x, 0.3f, pos.z);
            spatial.removeControl(this);
            onLanded();
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    public abstract void onLanded();
}
