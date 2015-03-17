/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.tools.modelviewer;

import com.jme3.math.FastMath;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

/**
 *
 * Simply rotates a spatial
 *
 * @author Toni
 */
public class RotatorControl extends AbstractControl {

    private static final int TURN_RATE = 1;

    @Override
    protected void controlUpdate(float tpf) {
        if (spatial != null) {

            //Rotate
            spatial.rotate(0, FastMath.PI * (int) (TURN_RATE) / 180, 0);
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        /* Optional: rendering manipulation (for advanced users) */
    }
}
