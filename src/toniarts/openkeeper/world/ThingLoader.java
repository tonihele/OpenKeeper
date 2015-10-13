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
package toniarts.openkeeper.world;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Object;
import toniarts.openkeeper.tools.convert.map.Thing;

/**
 *
 * @author ArchDemon
 */
public class ThingLoader {

    public Spatial load(BulletAppState bulletAppState, AssetManager assetManager, KwdFile kwdFile) {

        //Create a root
        Node root = new Node("Things");
        Node nodeCreatures = new Node("Creatures");
        Node nodeObjects = new Node("Objects");
        for (toniarts.openkeeper.tools.convert.map.Thing obj : kwdFile.getThings()) {
            try {
                if (obj instanceof Thing.Creature) {

                    Thing.Creature cr = (Thing.Creature) obj;
                    GameCreature creature = new GameCreature(bulletAppState, assetManager, (Thing.Creature) obj, kwdFile);

                    nodeCreatures.attachChild(creature);

                } else if (obj instanceof Thing.Object) {

                    Thing.Object objectThing = (Thing.Object) obj;
                    Object object = kwdFile.getObject(objectThing.getObjectId());

                    Node nodeObject = (Node) assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + object.getMeshResource().getName() + ".j3o");
                    nodeObject.setLocalTranslation(
                            objectThing.getPosX() * MapLoader.TILE_WIDTH,
                            0 * MapLoader.TILE_HEIGHT,
                            objectThing.getPosY() * MapLoader.TILE_WIDTH);
                    nodeObjects.attachChild(nodeObject);

                }
            } catch (Exception ex) {
                System.err.println(ex);
            }
        }

        root.attachChild(nodeCreatures);
        root.attachChild(nodeObjects);
        return root;

    }
}
