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
package toniarts.openkeeper.view.map.construction;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Makes an arrow model blink red. Oscillates the material Ambient color
 * between its original value and red. Can be set to solid red when the
 * associated level is hovered.
 *
 * @author OpenKeeper
 */
public class ArrowBlinkControl extends AbstractControl {

    private static final Logger logger = Logger.getLogger(ArrowBlinkControl.class.getName());
    private static final ColorRGBA RED = new ColorRGBA(1f, 0f, 0f, 1f);
    private static final float BLINK_SPEED = 1.5f;

    private final Spatial arrowSpatial;
    private boolean solidRed = false;
    private float elapsed = 0f;

    public ArrowBlinkControl(Spatial arrowSpatial) {
        this.arrowSpatial = arrowSpatial;
    }

    /**
     * When set to true, the arrow stays solid red without blinking. When false,
     * the blinking animation resumes.
     *
     * @param solid true for solid red, false for blinking
     */
    public void setSolidRed(boolean solid) {
        this.solidRed = solid;
        if (solid) {
            applyColor(RED);
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (solidRed) {
            return;
        }

        elapsed += tpf;
        float alpha = (FastMath.sin(elapsed * BLINK_SPEED * FastMath.TWO_PI) + 1f) / 2f;
        ColorRGBA current = new ColorRGBA().interpolateLocal(ColorRGBA.White, RED, alpha);
        applyColor(current);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    private void applyColor(ColorRGBA color) {
        arrowSpatial.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if (!(spatial instanceof Geometry)) {
                    return;
                }

                try {
                    Material material = ((Geometry) spatial).getMaterial();
                    if (material.getMaterialDef().getMaterialParam("Ambient") != null) {
                        material.setColor("Ambient", color);
                    } else {
                        material.setColor("Color", color);
                    }
                    if (material.getMaterialDef().getMaterialParam("UseMaterialColors") != null) {
                        material.setBoolean("UseMaterialColors", true);
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to set arrow material color!", e);
                }
            }
        });
    }
}
