/*
 * Copyright (C) 2014-2019 OpenKeeper
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
package toniarts.openkeeper.gui.nifty.jme;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

/**
 * Small container having the NiftyJmeControl for accessing the Nifty element.
 * Handles spatial visibility.
 *
 * @see AbstractNiftyJmeControl
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @param <T> the type of Nifty control
 */
public class SimpleJmeControl<T extends AbstractNiftyJmeControl> extends AbstractControl {

    private final T jmeControl;

    public SimpleJmeControl(T jmeControl) {
        this.jmeControl = jmeControl;
    }

    protected T getJmeControl() {
        return jmeControl;
    }

    @Override
    protected void controlUpdate(float tpf) {
        setVisibility();
    }

    protected void setVisibility() {
        getSpatial().setCullHint(jmeControl.isControlVisible() ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

}
