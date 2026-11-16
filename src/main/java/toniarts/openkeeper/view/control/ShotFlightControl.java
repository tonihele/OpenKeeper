/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.view.control;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

/**
 * Flies a shot on between the game turns. The server moves the shot only once
 * a turn, each new position from it puts the shot back on its track.
 */
public final class ShotFlightControl extends AbstractControl {

    private final Vector3f velocity;
    private final Vector3f move = new Vector3f();

    /**
     * @param velocity the shot's velocity, in tiles per second
     */
    public ShotFlightControl(Vector3f velocity) {
        this.velocity = velocity;
    }

    @Override
    protected void controlUpdate(float tpf) {
        spatial.move(velocity.mult(tpf, move));
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

}
