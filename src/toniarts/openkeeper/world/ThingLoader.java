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
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.creature.CreatureLoader;
import toniarts.openkeeper.world.object.ObjectControl;
import toniarts.openkeeper.world.object.ObjectLoader;

/**
 * Loads things, all things
 *
 * @author ArchDemon
 */
public class ThingLoader {

    private final WorldState worldState;
    private final CreatureLoader creatureLoader;
    private final ObjectLoader objectLoader;
    private final KwdFile kwdFile;
    private final AssetManager assetManager;
    private final Node root;
    private final Node nodeCreatures;
    private final Node nodeObjects;
    private final List<CreatureControl> creatures = new ArrayList<>();
    private final List<ObjectControl> objects = new ArrayList<>();

    private static final Logger logger = Logger.getLogger(ThingLoader.class.getName());

    public ThingLoader(WorldState worldHandler, KwdFile kwdFile, AssetManager assetManager) {
        this.worldState = worldHandler;
        this.kwdFile = kwdFile;
        this.assetManager = assetManager;
        creatureLoader = new CreatureLoader(kwdFile, worldState);
        objectLoader = new ObjectLoader(kwdFile, worldState);

        // Create the scene graph
        root = new Node("Things");
        nodeCreatures = new Node("Creatures");
        nodeObjects = new Node("Objects");
    }

    /**
     * Load all the initial things from the level KWD file
     *
     * @return the things node
     */
    public Node loadAll() {

        //Create a root
        for (toniarts.openkeeper.tools.convert.map.Thing obj : kwdFile.getThings()) {
            try {
                if (obj instanceof Thing.Creature) {

                    Thing.Creature cr = (Thing.Creature) obj;
                    Spatial creature = creatureLoader.load(assetManager, cr);
                    creatures.add(creature.getControl(CreatureControl.class));
                    nodeCreatures.attachChild(creature);

                } else if (obj instanceof Thing.Object) {

                    Thing.Object objectThing = (Thing.Object) obj;
                    Spatial object = objectLoader.load(assetManager, objectThing);
                    objects.add(object.getControl(ObjectControl.class));
                    nodeObjects.attachChild(object);

                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not load Thing.", ex.fillInStackTrace());
            }
        }

        root.attachChild(nodeCreatures);
        root.attachChild(nodeObjects);
        return root;
    }

    public List<CreatureControl> getCreatures() {
        return creatures;
    }

    public List<ObjectControl> getObjects() {
        return objects;
    }

}
