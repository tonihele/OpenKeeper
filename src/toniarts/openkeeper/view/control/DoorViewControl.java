/*
 * Copyright (C) 2014-2018 OpenKeeper
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

import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.Objects;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.world.animation.AnimationLoader;

/**
 * View control that is intended specifically for doors
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class DoorViewControl extends EntityViewControl<Door, DoorViewState> {

    private final GameObject lockObject;
    private Spatial lockSpatial;
    private boolean initialized = false;

    public DoorViewControl(EntityId entityId, EntityData entityData, Door data, DoorViewState viewState, AssetManager assetManager,
            GameObject lockObject) {
        super(entityId, entityData, data, viewState, assetManager);

        this.lockObject = lockObject;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);

        // Play the starting animation
        if (spatial != null && !initialized) {
            setState(null, targetState);
            initialized = true;
        }
    }

    @Override
    public boolean isStopAnimation() {
        return true;
    }

    private void playAnimation(DoorViewState viewState) {
        if (!getDataObject().getFlags().contains(Door.DoorFlag.IS_BARRICADE)) {
            AnimationLoader.playAnimation(getSpatial(), getAnimationData(viewState), assetManager);
            isAnimationPlaying = true;
        }
    }

    @Override
    public void setTargetState(DoorViewState viewState) {
        super.setTargetState(viewState);

        setState(currentState, targetState);
    }

    private void setState(DoorViewState current, DoorViewState target) {
        Objects.requireNonNull(target, "You need to give the target state!");

        if (!getDataObject().getFlags().contains(Door.DoorFlag.IS_BARRICADE)) {
            if (current == null || current.open != target.open) {
                playAnimation(target);
            }
            if (current == null || current.locked != target.locked) {
                if (target.locked) {
                    lockDoor();
                } else {
                    unlockDoor();
                }
            }
        }
        if (current == null || current.blueprint != target.blueprint) {
            if (target.blueprint) {
                AssetUtils.setBlueprint(assetManager, spatial);
            } else {
                // TODO: Unblueprint?
            }
        }
        currentState = target;
    }

    @Override
    public CursorFactory.CursorType getInHandCursor() {
        return CursorFactory.CursorType.HOLD_THING;
    }

    @Override
    protected ArtResource getAnimationData(DoorViewState viewState) {
        return viewState.open ? getDataObject().getOpenResource() : getDataObject().getCloseResource();
    }

    @Override
    public void onHoverEnd(short playerId) {
        super.onHoverEnd(playerId);

        // Restore blueprint state
        if (currentState.blueprint) {
            AssetUtils.setBlueprint(assetManager, spatial);
        }
    }

    private void unlockDoor() {
        if (lockSpatial != null) {
            ((Node) getSpatial()).detachChild(lockSpatial);
            lockSpatial = null;
        }
    }

    private void lockDoor() {
        if (lockSpatial == null && lockObject != null) {
            lockSpatial = AssetUtils.loadModel(assetManager, lockObject.getMeshResource().getName());
            lockSpatial.setUserData(AssetUtils.USER_DATA_KEY_REMOVABLE, false);
            ((Node) getSpatial()).attachChild(lockSpatial);
        }
    }
}
