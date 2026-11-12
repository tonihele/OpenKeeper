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
import java.io.File;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.utils.AssetUtils;

/**
 * Unit flower control for traps
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class TrapFlowerControl extends UnitFlowerControl<Trap> {

    public TrapFlowerControl(EntityId entityId, EntityData entityData, Trap trap, AssetManager assetManager) {
        super(entityId, entityData, trap, assetManager);
    }

    @Override
    public float getHeight() {
        return getDataObject().getHeight();
    }

    @Override
    public String getCenterIcon() {

        // FIXME Jack In The Box have trap.getFlowerIcon() == null
        String result = null;

        if (getDataObject().getFlowerIcon() != null) {
            result = AssetUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER
                    + File.separator + getDataObject().getFlowerIcon().getName() + ".png");
        }

        return result;
    }

}
