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
package toniarts.openkeeper.world.trap;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.world.ILoader;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.control.UnitFlowerControl;

/**
 * Loads traps. FIXME: bluebrint setting under the editor doesn't seem to stick,
 * so I don't know where it is set
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TrapLoader implements ILoader<Thing.Trap> {

    private final KwdFile kwdFile;
    private final WorldState worldState;
    private static final Logger logger = Logger.getLogger(TrapLoader.class.getName());

    public TrapLoader(KwdFile kwdFile, WorldState worldState) {
        this.kwdFile = kwdFile;
        this.worldState = worldState;
    }

    @Override
    public Spatial load(AssetManager assetManager, Thing.Trap trap) {
        return load(assetManager, trap.getPosX(), trap.getPosY(), trap.getTrapId(), false);
    }

    private Spatial load(AssetManager assetManager, int posX, int posY, short trapId, boolean blueprint) {
        toniarts.openkeeper.tools.convert.map.Trap trap = kwdFile.getTrapById(trapId);

        // Load
        TrapControl trapControl = new TrapControl(worldState.getMapData().getTile(posX, posY), trap, worldState, assetManager, blueprint);
        Node nodeObject = (Node) AssetUtils.loadModel(assetManager, AssetsConverter.MODELS_FOLDER + "/" + trap.getMeshResource().getName() + ".j3o", false);
        nodeObject.addControl(trapControl);

        // Move to the center of the tile
        AssetUtils.resetSpatial(nodeObject);
        nodeObject.setLocalTranslation(
                posX * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f,
                0,
                posY * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f);

        trapControl.initState();

        // Trap flower
        UnitFlowerControl aufc = new UnitFlowerControl(assetManager, trapControl);
        nodeObject.addControl(aufc);

        return nodeObject;
    }

}
