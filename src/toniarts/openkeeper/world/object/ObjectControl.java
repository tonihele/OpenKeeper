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
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import java.util.ResourceBundle;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.control.IInteractiveControl;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.room.control.RoomObjectControl;

/**
 * Control for object
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ObjectControl extends AbstractControl implements IInteractiveControl {

    private final WorldState worldState;
    private final toniarts.openkeeper.tools.convert.map.Object object;
    private final String tooltip;

    // Owners
    private RoomObjectControl roomObjectControl;
    private CreatureControl creature;

    public ObjectControl(int ownerId, toniarts.openkeeper.tools.convert.map.Object object, WorldState worldState) {
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
    public String getTooltip(short playerId) {
        return tooltip;
    }

    /**
     * Remove object
     */
    public void removeObject() {

        // Physically from the view
        Spatial us = getSpatial();
        us.removeFromParent();

        // From the things registry
        worldState.getThingLoader().onObjectRemoved(this);

        // If we belong to a creature, remove us
        if (creature != null) {
            creature.removeObject(this);
        }

        // If we belong to a room, remove us
        if (roomObjectControl != null) {
            roomObjectControl.removeItem(this);
        }
    }

    public void setCreature(CreatureControl creature) {
        this.creature = creature;
    }

    public void setRoomObjectControl(RoomObjectControl roomObjectControl) {
        this.roomObjectControl = roomObjectControl;
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
