/*
 * Copyright (C) 2014-2015 OpenKeeper
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

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

/**
 *
 * @author ArchDemon
 */


public class FlashTileControl extends AbstractControl {

    private float time;
    private float tick = 0;
    private boolean flashed = false;
    private boolean unlimited = false;
    private final ColorRGBA color = new ColorRGBA(0.3f, 0, 0, 1);
    public final float PERIOD = 0.5f;

    public FlashTileControl() {
    }

    public FlashTileControl(int time) {
        this.time = time;
        if (this.time == 0) {
            unlimited = true;
        }
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);

        flashed = true;
        setColorToGeometries((Node) spatial, flashed);
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (!enabled) {
            return;
        }

        if (tick >= PERIOD) {
            tick -= PERIOD;
            flashed = !flashed;
            setColorToGeometries((Node) spatial, flashed);
        }

        if (time < 0) {
            if (flashed) {
                setColorToGeometries((Node) spatial, false);
            }
            spatial.removeControl(this);
            return;
        }

        tick += tpf;
        if (!unlimited) {
            time -= tpf;
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    private void setColorToGeometries(final Node node, final boolean flashed) {
        if (node == null) {
            return;
        }

        node.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if (spatial instanceof Geometry) {

                    Material material = ((Geometry) spatial).getMaterial();
                    material.setColor("Ambient", color);
                    material.setBoolean("UseMaterialColors", flashed);
                }
            }
        });
    }
}
