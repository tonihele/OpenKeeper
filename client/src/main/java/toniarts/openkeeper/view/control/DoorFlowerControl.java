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
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.utils.AssetUtils;

/**
 * Unit flower control for doors
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class DoorFlowerControl extends UnitFlowerControl<Door> {

    public DoorFlowerControl(EntityId entityId, EntityData entityData, Door door, AssetManager assetManager) {
        super(entityId, entityData, door, assetManager);
    }

    @Override
    public float getHeight() {
        return getDataObject().getHeight();
    }

    @Override
    public String getCenterIcon() {
        return AssetUtils.getCanonicalAssetKey("Textures/" + getDataObject().getFlowerIcon().getName() + ".png");
    }

}
