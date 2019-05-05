/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.view.loader;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.utils.AssetUtils;

/**
 * Loads up doors
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class DoorLoader implements ILoader<DoorViewState> {

    private final KwdFile kwdFile;
    private static final Logger LOGGER = Logger.getLogger(DoorLoader.class.getName());

    public DoorLoader(KwdFile kwdFile) {
        this.kwdFile = kwdFile;
    }

    @Override
    public Spatial load(AssetManager assetManager, DoorViewState door) {
        try {
            Door doorRecord = kwdFile.getDoorById(door.doorId);
            Node nodeObject = (Node) AssetUtils.loadModel(assetManager, doorRecord.getFlags().contains(Door.DoorFlag.IS_BARRICADE) ? doorRecord.getMesh().getName() : doorRecord.getCloseResource().getName());
            return nodeObject;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load door " + door + "!", e);
        }
        return null;
    }
}
