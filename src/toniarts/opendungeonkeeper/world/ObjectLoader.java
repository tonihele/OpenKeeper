/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.world;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import toniarts.opendungeonkeeper.tools.convert.AssetsConverter;

/**
 * Loads up object
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ObjectLoader implements ILoader<toniarts.opendungeonkeeper.tools.convert.map.Object> {

    @Override
    public Spatial load(AssetManager assetManager, toniarts.opendungeonkeeper.tools.convert.map.Object object) {

        //Create a root
        Node root = new Node(object.getName());

        // The basic mesh
        return assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + object.getMeshResource().getName() + ".j3o");

        // Add the top
//        root.attachChild(assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + object.getTopResource().getName() + ".j3o"));

        // The sides
//        Spatial side = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + object.getSideResource().getName() + ".j3o");
//        root.attachChild(side);

//        return root;
    }
}
