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
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.creature.CreatureLoader;
import toniarts.openkeeper.world.object.ObjectLoader;

/**
 *
 * @author ArchDemon
 */
public class ThingLoader {

    private final WorldState worldState;
    private static final Logger logger = Logger.getLogger(ThingLoader.class.getName());

    public ThingLoader(WorldState worldHandler) {
        this.worldState = worldHandler;
    }

    public Spatial load(BulletAppState bulletAppState, AssetManager assetManager, KwdFile kwdFile) {

        // Create a creature loader
        CreatureLoader creatureLoader = new CreatureLoader(kwdFile, worldState);
        ObjectLoader objectLoader = new ObjectLoader(kwdFile, worldState);

        //Create a root
        Node root = new Node("Things");
        Node nodeCreatures = new Node("Creatures");
        Node nodeObjects = new Node("Objects");
        for (toniarts.openkeeper.tools.convert.map.Thing obj : kwdFile.getThings()) {
            try {
                if (obj instanceof Thing.Creature) {

                    Thing.Creature cr = (Thing.Creature) obj;
//                    GameCreature creature = new GameCreature(bulletAppState, assetManager, cr, kwdFile);

                    nodeCreatures.attachChild(creatureLoader.load(assetManager, cr));

                } else if (obj instanceof Thing.Object) {

                    Thing.Object objectThing = (Thing.Object) obj;

                    nodeObjects.attachChild(objectLoader.load(assetManager, objectThing));

                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not load Thing.", ex.fillInStackTrace());
            }
        }

        root.attachChild(nodeCreatures);
        root.attachChild(nodeObjects);
        return root;

    }
}
