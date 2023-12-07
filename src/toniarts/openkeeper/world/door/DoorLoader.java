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
import com.jme3.math.FastMath;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.loader.ILoader;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.control.UnitFlowerControl;

/**
 * Loads doors
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Deprecated
public class DoorLoader implements ILoader<Thing.Door> {
    
    private static final Logger logger = Logger.getLogger(DoorLoader.class.getName());

    private final KwdFile kwdFile;
    private final WorldState worldState;

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
        DoorControl doorControl = new DoorControl(worldState.getMapData().getTile(posX, posY), door, kwdFile.getObject(door.getKeyObjectId()), kwdFile.getTrapById(door.getTrapTypeId()), worldState, assetManager, locked, blueprint);
        Node nodeObject = (Node) AssetUtils.loadModel(assetManager, (door.getFlags().contains(Door.DoorFlag.IS_BARRICADE) ? door.getMesh().getName() : door.getCloseResource().getName()), null);
        nodeObject.addControl(doorControl);

        // Move to the center of the tile
        nodeObject.setLocalTranslation(WorldUtils.pointToVector3f(posX, posY));
        nodeObject.move(0, MapLoader.FLOOR_HEIGHT, 0);

        // Figure out which way we should face, the door probably doesn't need to know it
        // There can be 3 tiles that are solid
        short ownerId = worldState.getMapLoader().getMapData().getTile(posX, posY).getPlayerId();
        if (canTileSupportDoor(posX, posY - 1, ownerId) && canTileSupportDoor(posX, posY + 1, ownerId)) {
            float yAngle = -FastMath.HALF_PI;
            nodeObject.rotate(0, yAngle, 0);
        }
        doorControl.initState();

        // Door flower
        UnitFlowerControl aufc = new UnitFlowerControl(assetManager, doorControl);
        nodeObject.addControl(aufc);

        return nodeObject;
    }

    private boolean canTileSupportDoor(int x, int y, short ownerId) {
        TileData tile = worldState.getMapLoader().getMapData().getTile(x, y);

        return (tile != null && tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.SOLID)
                && ((!tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.OWNABLE))
                || (tile.getPlayerId() == ownerId && tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.OWNABLE))));
    }
}
