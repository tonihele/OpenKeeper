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
package toniarts.openkeeper.world.object;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import java.util.ResourceBundle;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.control.IInteractiveControl;

/**
 * Control for object
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ObjectControl extends AbstractControl implements IInteractiveControl {

    private final WorldState worldState;
    private final toniarts.openkeeper.tools.convert.map.Object object;
    private final String tooltip;

    public ObjectControl(Thing.Object objectInstance, toniarts.openkeeper.tools.convert.map.Object object, WorldState worldState) {
        super();

        this.worldState = worldState;
        this.object = object;

        // Strings
        ResourceBundle bundle = Main.getResourceBundle("Interface/Texts/Text");
        tooltip = bundle.getString(Integer.toString(object.getTooltipStringId()));
    }

    @Override
    protected void controlUpdate(float tpf) {

    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

    @Override
    public String getTooltip() {
        return tooltip;
    }

    @Override
    public boolean isPickable(short playerId) {
        return object.getFlags().contains(toniarts.openkeeper.tools.convert.map.Object.ObjectFlag.CAN_BE_PICKED_UP);
    }

    @Override
    public boolean isInteractable(short playerId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean pickUp(short playerId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean interact(short playerId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
