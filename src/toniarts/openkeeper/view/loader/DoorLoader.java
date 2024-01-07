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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.utils.AssetUtils;

/**
 * Loads up doors
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class DoorLoader implements ILoader<DoorViewState> {

    private static final Logger logger = System.getLogger(DoorLoader.class.getName());
    
    private final KwdFile kwdFile;

    public DoorLoader(KwdFile kwdFile) {
        this.kwdFile = kwdFile;
    }

    @Override
    public Spatial load(AssetManager assetManager, DoorViewState door) {
        try {
            Door doorRecord = kwdFile.getDoorById(door.doorId);
            ArtResource artResource = doorRecord.getFlags().contains(Door.DoorFlag.IS_BARRICADE) ? doorRecord.getMesh() : doorRecord.getCloseResource();
            Node nodeObject = (Node) AssetUtils.loadModel(assetManager, artResource.getName(), artResource);
            return nodeObject;
        } catch (Exception e) {
            logger.log(Level.ERROR, "Failed to load door " + door + "!", e);
        }
        return null;
    }
}
