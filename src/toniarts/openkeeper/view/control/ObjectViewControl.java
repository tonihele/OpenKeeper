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
import com.jme3.scene.Spatial;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.Objects;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.view.animation.AnimationLoader;
import toniarts.openkeeper.view.text.TextParser;

/**
 * View control that is intended specifically for objects
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class ObjectViewControl extends EntityViewControl<GameObject, ObjectViewState> {

    private boolean initialized = false;

    public ObjectViewControl(EntityId entityId, EntityData entityData, GameObject data, ObjectViewState state,
            AssetManager assetManager, TextParser textParser) {
        super(entityId, entityData, data, state, assetManager, textParser);
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);

        // Play the starting animation
        if (spatial != null && !initialized) {

            // Don't play for prison door nor the bar....?
            if (getDataObject().getObjectId() != 109 && getDataObject().getObjectId() != 116) {
                playAnimation(currentState);
            }
            initialized = true;
        }
    }

    @Override
    public String getTooltip(short playerId) {
        String tooltip;
        if (getDataObject().getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_GOLD)) {

            // TODO: better separation between loose and room gold
            if (getDataObject().getObjectId() == 1) {
                tooltip = Utils.getMainTextResourceBundle().getString("2544");
            } else {
                tooltip = Utils.getMainTextResourceBundle().getString("2543");
            }
        } else {
            tooltip = Utils.getMainTextResourceBundle().getString(Integer.toString(getDataObject().getTooltipStringId()));
        }
        return textParser.parseText(tooltip, getEntity(), getDataObject());
    }

    @Override
    public ArtResource getInHandIcon() {
        return getDataObject().getGuiIconResource();
    }

    @Override
    public ArtResource getInHandMesh() {
        return getDataObject().getInHandMeshResource();
    }

    @Override
    public CursorFactory.CursorType getInHandCursor() {
        if (getDataObject().getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_GOLD)) {
            return CursorFactory.CursorType.HOLD_GOLD;
        }
        return CursorFactory.CursorType.HOLD_THING;
    }

    @Override
    public void setTargetState(ObjectViewState state) {
        super.setTargetState(state);

        // Play immediately
        if (!Objects.equals(currentState, targetState)) {
            playAnimation(state);
            currentState = targetState;
        }
    }

    private void playAnimation(ObjectViewState viewState) {
        AnimationLoader.playAnimation(getSpatial(), getAnimationData(viewState), assetManager);
        isAnimationPlaying = true;
    }

    @Override
    protected ArtResource getAnimationData(ObjectViewState state) {
        switch (state.animState) {
            case MESH_RESOURCE:
                return getDataObject().getMeshResource();
            case ADDITIONAL_RESOURCE_1: {
                return getDataObject().getAdditionalResources().get(0);
            }
            case ADDITIONAL_RESOURCE_2: {
                return getDataObject().getAdditionalResources().get(1);
            }
            case ADDITIONAL_RESOURCE_3: {
                return getDataObject().getAdditionalResources().get(2);
            }
            case ADDITIONAL_RESOURCE_4: {
                return getDataObject().getAdditionalResources().get(3);
            }
        }
        return getDataObject().getMeshResource();
    }

}
