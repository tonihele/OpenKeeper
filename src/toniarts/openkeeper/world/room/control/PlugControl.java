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
package toniarts.openkeeper.world.room.control;

import com.jme3.math.FastMath;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

/**
 *
 * @author ArchDemon
 */
public class PlugControl extends AbstractControl {

    private static final float GRAVITY = 9.81f;
    private float velocity = 7f;
    private float tick = 0;
    private Spatial plug;
    private Node plugDecay;

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);

        if (spatial != null) {
            plug = ((Node) spatial).getChild("plug");
            plugDecay = (Node) ((Node) spatial).getChild("plug_decay");
            for (Spatial piece : plugDecay.getChildren()) {
                piece.setUserData("rotate", FastMath.rand.nextFloat());
                piece.setUserData("velocity", velocity * FastMath.rand.nextFloat());
            }
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
        // TODO add normal physics control
        if (tick > 11) {
            plugDecay.removeFromParent();
            spatial.removeControl(this);
        } else if (tick > 9) {
            velocity -= GRAVITY * tpf;
            for (Spatial piece : plugDecay.getChildren()) {
                float rotate = (float) piece.getUserData("rotate") * 10 * tpf;
                float pv = (float) piece.getUserData("velocity") - GRAVITY * tpf;
                piece.setUserData("velocity", pv);
                piece.move(0, pv * tpf, 0);
                //float step = (float) piece.getUserData("yAngle");
                //piece.move(tpf * FastMath.cos(step), velocity * tpf, tpf * FastMath.sin(step));
                piece.rotate(rotate, rotate, rotate);
            }
        } else if (tick > 6) {
            plug.removeFromParent();
            plugDecay.setCullHint(Spatial.CullHint.Inherit);
        }

        tick += tpf;
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }
}
