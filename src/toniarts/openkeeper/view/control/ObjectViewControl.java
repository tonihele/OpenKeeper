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
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.view.text.TextParser;

/**
 * View control that is intended specifically for objects
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ObjectViewControl extends EntityViewControl<GameObject, GameObject.State> {

    public ObjectViewControl(EntityId entityId, EntityData entityData, GameObject data, GameObject.State state,
            AssetManager assetManager, TextParser textParser) {
        super(entityId, entityData, data, state, assetManager, textParser);
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
        return textParser.parseText(tooltip, getEntityId(), getDataObject());
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
    protected ArtResource getAnimationData(GameObject.State state) {
        return getDataObject().getMeshResource();
    }

}
