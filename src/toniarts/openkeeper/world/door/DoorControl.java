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
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.ResourceBundle;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.animation.AnimationControl;
import toniarts.openkeeper.world.animation.AnimationLoader;
import toniarts.openkeeper.world.control.IInteractiveControl;
import toniarts.openkeeper.world.control.IUnitFlowerControl;
import toniarts.openkeeper.world.control.UnitFlowerControl;
import toniarts.openkeeper.world.object.HighlightControl;

/**
 * The door control, handles door stuff, you know... Open sesame!
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class DoorControl extends HighlightControl implements IInteractiveControl, AnimationControl, IUnitFlowerControl {

    public enum DoorState {

        CLOSED, OPEN, BLUEPRINT, DESTROYED;
    }

    private final WorldState worldState;
    private final Door door;
    private final toniarts.openkeeper.tools.convert.map.Object lockObject;
    private final Trap doorTrap;
    private final String name;
    private final AssetManager assetManager;
    private Spatial lockSpatial;
    private boolean locked = false;
    private final TileData tile;
    private DoorState state = DoorState.CLOSED;
    private int health;
    private boolean animating = false;
    private DoorState animatedState = DoorState.CLOSED;
    private final static ResourceBundle BUNDLE = Main.getResourceBundle("Interface/Texts/Text");

    public DoorControl(TileData tile, Door door, toniarts.openkeeper.tools.convert.map.Object lockObject, Trap doorTrap, WorldState worldState, AssetManager assetManager) {
        this(tile, door, lockObject, doorTrap, worldState, assetManager, false, false);
    }

    public DoorControl(TileData tile, Door door, toniarts.openkeeper.tools.convert.map.Object lockObject, Trap doorTrap, WorldState worldState, AssetManager assetManager, boolean locked, boolean blueprint) {
        super();

        this.worldState = worldState;
        this.door = door;
        this.lockObject = lockObject;
        this.doorTrap = doorTrap;
        this.tile = tile;
        this.assetManager = assetManager;
        this.health = door.getHealth();
        this.locked = locked;
        if (blueprint) {
            state = DoorState.BLUEPRINT;
        }
        name = BUNDLE.getString(Integer.toString(door.getNameStringId()));
    }

    @Override
    protected void controlUpdate(float tpf) {

    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    public void initState() {

        // TODO: We should do this initially, no point of having to do extra work perhaps
        if (!door.getFlags().contains(Door.DoorFlag.IS_BARRICADE)) {
            AnimationLoader.playAnimation(spatial, door.getCloseResource(), assetManager, true);
        }
        if (locked) {
            lockDoor();
        } else if (state == DoorState.BLUEPRINT && door.getFlags().contains(Door.DoorFlag.IS_BARRICADE)) {
            AssetUtils.setBlueprint(assetManager, spatial);
        }
    }

    @Override
    public String getTooltip(short playerId) {
        String tooltip;
        if (playerId == getOwnerId()) {
            switch (state) {
                case BLUEPRINT: {
                    tooltip = BUNDLE.getString("2512");
                    break;
                }
                default: {
                    tooltip = BUNDLE.getString("2532");
                    tooltip = tooltip.replaceFirst("%72", locked ? BUNDLE.getString("2516") : BUNDLE.getString("2515"));
                    break;
                }
            }
            tooltip = tooltip.replaceFirst("%68", name);
        } else {
            tooltip = BUNDLE.getString("2540");
        }
        return tooltip.replaceFirst("%37%", Integer.toString(getHealthPercentage()));
    }

    @Override
    public boolean isPickable(short playerId) {
        return false;
    }

    @Override
    public boolean isInteractable(short playerId) {
        return (!door.getFlags().contains(Door.DoorFlag.IS_BARRICADE) && state != DoorState.BLUEPRINT && state != DoorState.DESTROYED && getOwnerId() == playerId);
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
        if (isInteractable(playerId)) {
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
        UnitFlowerControl.showUnitFlower(this, null);
    }

    @Override
    public short getOwnerId() {
        return tile.getPlayerId();
    }

    @Override
    public void onHoverEnd() {
        super.onHoverEnd();

        // Restore blueprint state
        if (state == DoorState.BLUEPRINT) {
            AssetUtils.setBlueprint(assetManager, spatial);
        }
    }

    private void unlockDoor() {
        locked = false;
        if (lockSpatial != null) {
            ((Node) getSpatial()).detachChild(lockSpatial);
            lockSpatial = null;
        }
    }

    protected void lockDoor() {
        locked = true;
        if (lockSpatial == null && lockObject != null) {
            lockSpatial = AssetUtils.loadModel(assetManager, AssetsConverter.MODELS_FOLDER + "/" + lockObject.getMeshResource().getName() + ".j3o", false);
            AssetUtils.resetSpatial(lockSpatial);
            lockSpatial.move(0, 0.75f, 0);
            lockSpatial.setUserData(AssetUtils.USER_DATE_KEY_REMOVABLE, false);
            ((Node) getSpatial()).attachChild(lockSpatial);
        }
        closeDoor();
    }

    private void openDoor() {

        // Start opening animation and mark open
        state = DoorState.OPEN;
        if (!door.getFlags().contains(Door.DoorFlag.IS_BARRICADE)) {
            setAnimation(DoorState.OPEN);
        }
    }

    private void closeDoor() {

        // Start closing animation and mark closed
        state = DoorState.CLOSED;
        if (!door.getFlags().contains(Door.DoorFlag.IS_BARRICADE)) {
            setAnimation(DoorState.CLOSED);
        }
    }

    public DoorState getState() {
        return state;
    }

    private void setAnimation(DoorState doorState) {
        if (!animating && doorState != this.animatedState) {
            startAnimation(doorState);
        }
    }

    private void startAnimation(DoorState doorState) {
        animatedState = doorState;
        AnimationLoader.playAnimation(spatial, doorState == DoorState.CLOSED ? door.getCloseResource() : door.getOpenResource(), assetManager);
    }

    @Override
    public int getHealth() {
        return health;
    }

    @Override
    public void onAnimationStop() {
        animating = false;

        if (state == DoorState.BLUEPRINT) {
            AssetUtils.setBlueprint(assetManager, spatial);
        } else {
            setAnimation(state);
        }
    }

    @Override
    public void onAnimationCycleDone() {
        //
    }

    @Override
    public boolean isStopAnimation() {
        return true; // We stop it always
    }

    @Override
    public int getMaxHealth() {
        return door.getHealth();
    }

    @Override
    public float getHeight() {
        return door.getHeight();
    }

    @Override
    public String getCenterIcon() {
        return ConversionUtils.getCanonicalAssetKey("Textures/" + door.getFlowerIcon().getName() + ".png");
    }

}
