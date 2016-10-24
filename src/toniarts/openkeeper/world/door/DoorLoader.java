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
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.world.ILoader;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.WorldState;

/**
 * Loads doors
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class DoorLoader implements ILoader<Thing.Door> {

    private final KwdFile kwdFile;
    private final WorldState worldState;
    private static final Logger logger = Logger.getLogger(DoorLoader.class.getName());

    public DoorLoader(KwdFile kwdFile, WorldState worldState) {
        this.kwdFile = kwdFile;
        this.worldState = worldState;
    }

    @Override
    public Spatial load(AssetManager assetManager, Thing.Door door) {
        return load(assetManager, door.getPosX(), door.getPosY(), door.getDoorId(), door.getFlag() == Thing.Door.DoorFlag.LOCKED, door.getFlag() == Thing.Door.DoorFlag.BLUEPRINT);
    }

    private Spatial load(AssetManager assetManager, int posX, int posY, short doorId, boolean locked, boolean blueprint) {
        toniarts.openkeeper.tools.convert.map.Door door = kwdFile.getDoorById(doorId);

        // Load
        DoorControl doorControl = new DoorControl(worldState.getMapData().getTile(posX, posY), door, worldState, assetManager, locked, blueprint);
        Node nodeObject = (Node) AssetUtils.loadModel(assetManager, AssetsConverter.MODELS_FOLDER + "/" + (door.getFlags().contains(Door.DoorFlag.IS_BARRICADE) ? door.getMesh().getName() : door.getCloseResource().getName()) + ".j3o", false);
        nodeObject.addControl(doorControl);

        // Move to the center of the tile
        nodeObject.setLocalTranslation(
                posX * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f,
                0,
                posY * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f);

        return nodeObject;
    }
}
