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

import com.jme3.math.FastMath;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import java.awt.Point;
import java.util.ResourceBundle;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.control.IInteractiveControl;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.room.control.RoomObjectControl;

/**
 * Control for object
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ObjectControl extends HighlightControl implements IInteractiveControl {

    public enum ObjectState {

        NORMAL, PICKED_UP, OWNED_BY_CREATURE, STORED_IN_ROOM;
    }

    protected final WorldState worldState;
    protected final toniarts.openkeeper.tools.convert.map.Object object;
    private final String tooltip;
    protected TileData tile;
    private ObjectState state = ObjectState.NORMAL;

    // Owners
    protected RoomObjectControl roomObjectControl;
    private CreatureControl creature;
    protected short pickedUpBy;
    protected final ResourceBundle bundle;

    public ObjectControl(TileData tile, toniarts.openkeeper.tools.convert.map.Object object, WorldState worldState) {
        super();

        this.worldState = worldState;
        this.object = object;
        this.tile = tile;

        // Strings
        bundle = Main.getResourceBundle("Interface/Texts/Text");
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
        if (creature != null) {
            setState(ObjectState.OWNED_BY_CREATURE);
        }
    }

    public void setRoomObjectControl(RoomObjectControl roomObjectControl) {
        this.roomObjectControl = roomObjectControl;
        if (roomObjectControl != null) {
            setState(ObjectState.STORED_IN_ROOM);
        }
    }

    protected ArtResource getResource() {
        if (isAdditionalResources()) {
            int resourceIndex = FastMath.nextRandomInt(0, getResourceCount() - 1);
            if (resourceIndex == object.getAdditionalResources().size()) {
                return object.getMeshResource();
            }
            return object.getAdditionalResources().get(resourceIndex);
        }
        return object.getMeshResource();
    }

    public int getOrientation() {
        if (object.getMaxAngle() == 0) {
            return 0;
        }

        // Take a random angle
        return FastMath.nextRandomInt(0, object.getMaxAngle());
    }

    protected boolean isAdditionalResources() {
        return !object.getAdditionalResources().isEmpty();
    }

    protected int getResourceCount() {
        return object.getAdditionalResources().size() + 1;
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
    public IInteractiveControl pickUp(short playerId) {
        setEnabled(false);
        pickedUpBy = playerId;
        setState(ObjectState.PICKED_UP);

        // If we are a part of room, we need to detach
        if (roomObjectControl != null) {
            roomObjectControl.removeItem(this);
        } else {

            // We are part of the world
            worldState.getThingLoader().onObjectRemoved(this);
        }

        // Remove from view
        getSpatial().removeFromParent();

        return this;
    }

    @Override
    public boolean interact(short playerId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onHover() {

    }

    @Override
    public void onHoverStart() {
        super.onHoverStart();
    }

    @Override
    public void onHoverEnd() {
        super.onHoverEnd();
    }

    @Override
    public CursorFactory.CursorType getInHandCursor() {
        return CursorFactory.CursorType.HOLD_THING;
    }

    @Override
    public ArtResource getInHandMesh() {
        return object.getInHandMeshResource();
    }

    @Override
    public ArtResource getInHandIcon() {
        return object.getGuiIconResource();
    }

    @Override
    public DroppableStatus getDroppableStatus(TileData tile) {
        return !tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.SOLID)
                && (object.getFlags().contains(toniarts.openkeeper.tools.convert.map.Object.ObjectFlag.CAN_BE_DROPPED_ON_ANY_LAND)
                || ((tile.getPlayerId() == getOwnerId() && tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.OWNABLE))))
                        ? DroppableStatus.DROPPABLE : DroppableStatus.NOT_DROPPABLE;
    }

    @Override
    public void drop(TileData tile) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public short getOwnerId() {
        switch (state) {
            case STORED_IN_ROOM:
            case NORMAL: {
                return tile.getPlayerId();
            }
            case OWNED_BY_CREATURE: {
                return creature.getOwnerId();
            }
            case PICKED_UP: {
                return pickedUpBy;
            }
        }
        return 0;
    }

    /**
     * Get the object coordinates, in tile coordinates
     *
     * @return the tile coordinates
     */
    public Point getObjectCoordinates() {
        return worldState.getTileCoordinates(getSpatial().getWorldTranslation());
    }

    public ObjectState getState() {
        return state;
    }

    public void setState(ObjectState state) {
        this.state = state;
    }

}
