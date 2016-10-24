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
package toniarts.openkeeper.world.door;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector2f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import java.util.ResourceBundle;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.control.IInteractiveControl;
import toniarts.openkeeper.world.object.HighlightControl;

/**
 * The door control, handles door stuff, you know... Open sesame!
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class DoorControl extends HighlightControl implements IInteractiveControl {

    public enum DoorState {

        CLOSED, OPEN, BLUEPRINT, DESTROYED;
    }

    private final WorldState worldState;
    private final Door door;
    private final String tooltip;
    private final AssetManager assetManager;
    private boolean locked = false;
    private final TileData tile;
    private DoorState state = DoorState.CLOSED;
    private int health;

    public DoorControl(TileData tile, Door door, WorldState worldState, AssetManager assetManager) {
        this(tile, door, worldState, assetManager, false, false);
    }

    public DoorControl(TileData tile, Door door, WorldState worldState, AssetManager assetManager, boolean locked, boolean blueprint) {
        super();

        this.worldState = worldState;
        this.door = door;
        this.tile = tile;
        this.assetManager = assetManager;
        this.health = door.getHealth();
        this.locked = locked;
        if (blueprint) {
            state = DoorState.BLUEPRINT;
        }

        // Strings
        ResourceBundle bundle = Main.getResourceBundle("Interface/Texts/Text");
        tooltip = bundle.getString(Integer.toString(door.getTooltipStringId()));
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

    @Override
    public boolean isPickable(short playerId) {
        return false;
    }

    @Override
    public boolean isInteractable(short playerId) {
        return (!door.getFlags().contains(Door.DoorFlag.IS_BARRICADE) && getOwnerId() == playerId);
    }

    @Override
    public IInteractiveControl pickUp(short playerId) {
        throw new UnsupportedOperationException("You can't pick up a door!");
    }

    @Override
    public CursorFactory.CursorType getInHandCursor() {
        return null;
    }

    @Override
    public ArtResource getInHandMesh() {
        return null;
    }

    @Override
    public ArtResource getInHandIcon() {
        return null;
    }

    @Override
    public DroppableStatus getDroppableStatus(TileData tile) {
        return null;
    }

    @Override
    public void drop(TileData tile, Vector2f coordinates, IInteractiveControl control) {
        throw new UnsupportedOperationException("You can't drop a door!");
    }

    @Override
    public boolean interact(short playerId) {
        if (state != DoorState.BLUEPRINT && state != DoorState.DESTROYED && getOwnerId() == playerId) {
            if (locked) {
                unlockDoor();
            } else {
                lockDoor();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onHover() {

    }

    @Override
    public short getOwnerId() {
        return tile.getPlayerId();
    }

    private void unlockDoor() {
        locked = false;
        // TODO:
    }

    private void lockDoor() {
        locked = true;
        // TODO :
    }

    private void openDoor() {

        // Start opening animation and mark open
        state = DoorState.OPEN;
    }

    private void closeDoor() {

        // Start closing animation and mark closed
        state = DoorState.CLOSED;
    }

    public DoorState getState() {
        return state;
    }

    public int getHealth() {
        return health;
    }

}
