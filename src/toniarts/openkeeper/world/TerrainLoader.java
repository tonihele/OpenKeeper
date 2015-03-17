/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.world;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.Terrain;

/**
 * Loads up terrain
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TerrainLoader implements ILoader<Terrain> {

    @Override
    public Spatial load(AssetManager assetManager, Terrain object) {

        //Create a root
        Node root = new Node(object.getName());

        // Add the top
        root.attachChild(assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + object.getTopResource().getName() + ".j3o"));

        // The sides
        Spatial side = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + object.getSideResource().getName() + ".j3o");
        root.attachChild(side);

        return root;
    }
}
