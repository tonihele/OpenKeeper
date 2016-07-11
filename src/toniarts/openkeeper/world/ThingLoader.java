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
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.ai.creature.CreatureState;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.creature.CreatureLoader;
import toniarts.openkeeper.world.listener.CreatureListener;
import toniarts.openkeeper.world.object.GoldObjectControl;
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
    private final Set<CreatureControl> creatures = new LinkedHashSet<>();
    private final List<ObjectControl> objects = new ArrayList<>();
    private Map<Short, List<CreatureListener>> creatureListeners;

    private static final Logger logger = Logger.getLogger(ThingLoader.class.getName());

    public ThingLoader(WorldState worldHandler, KwdFile kwdFile, AssetManager assetManager) {
        this.worldState = worldHandler;
        this.kwdFile = kwdFile;
        this.assetManager = assetManager;
        creatureLoader = new CreatureLoader(kwdFile, worldState) {

            @Override
            public void onDie(CreatureControl creature) {

                // Remove the creature
                creatures.remove(creature);

                // Notify listeners
                if (creatureListeners != null && creatureListeners.containsKey(creature.getOwnerId())) {
                    for (CreatureListener listener : creatureListeners.get(creature.getOwnerId())) {
                        listener.onDie(creature);
                    }
                }
            }

            @Override
            public void onSpawn(CreatureControl creature) {

                // Notify listeners
                if (creatureListeners != null && creatureListeners.containsKey(creature.getOwnerId())) {
                    for (CreatureListener listener : creatureListeners.get(creature.getOwnerId())) {
                        listener.onSpawn(creature);
                    }
                }
            }

            @Override
            public void onStateChange(CreatureControl creature, CreatureState newState, CreatureState oldState) {

                // Notify listeners
                if (creatureListeners != null && creatureListeners.containsKey(creature.getOwnerId())) {
                    for (CreatureListener listener : creatureListeners.get(creature.getOwnerId())) {
                        listener.onStateChange(creature, newState, oldState);
                    }
                }
            }

        };
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
                    spawnCreature((Thing.Creature) obj, null);
                } else if (obj instanceof Thing.Object) {

                    Thing.Object objectThing = (Thing.Object) obj;
                    Spatial object = objectLoader.load(assetManager, objectThing);
                    objects.add(object.getControl(ObjectControl.class));
                    nodeObjects.attachChild(object);

                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not load Thing.", ex);
            }
        }

        root.attachChild(nodeCreatures);
        root.attachChild(nodeObjects);
        return root;
    }

    /**
     * Spawn a creature
     *
     * @param cr creature data
     * @param position the position to spawn to, may be {@code null}
     * @return the actual spawned creature
     */
    public CreatureControl spawnCreature(Thing.Creature cr, Vector2f position) {
        Spatial creature = creatureLoader.load(assetManager, cr);
        return spawnCreature(creature, position, false);
    }

    /**
     * Spawn a creature
     *
     * @param creatureId the creature ID to generate
     * @param playerId the owner
     * @param level the creature level
     * @param position the position to spawn to, may be {@code null}
     * @param entrance whether this an enrance for the creature (coming out of a
     * portal)
     * @return the actual spawned creature
     */
    public CreatureControl spawnCreature(short creatureId, short playerId, short level, Vector2f position, boolean entrance) {
        Spatial creature = creatureLoader.load(assetManager, creatureId, playerId, level);
        return spawnCreature(creature, position, entrance);
    }

    private CreatureControl spawnCreature(Spatial creature, Vector2f position, boolean entrance) {
        if (position != null) {
            CreatureLoader.setPosition(creature, position);
        }
        CreatureControl creatureControl = creature.getControl(CreatureControl.class);
        if (entrance) {
            creatureControl.getStateMachine().setInitialState(CreatureState.ENTERING_DUNGEON);
        }
        creatures.add(creatureControl);
        nodeCreatures.attachChild(creature);

        // Notify spawn
        creatureControl.onSpawn(creatureControl);

        return creatureControl;
    }

    /**
     * Add room type gold
     *
     * @param p the point to add
     * @param playerId the player id, the owner
     * @param initialAmount the amount of gold
     * @return the gold object
     */
    public GoldObjectControl addRoomGold(Point p, short playerId, int initialAmount) {
        // TODO: the room gold object id..
        Spatial object = objectLoader.load(assetManager, p.x, p.y, 0, initialAmount, 0, (short) 3, playerId);
        GoldObjectControl control = object.getControl(GoldObjectControl.class);
        objects.add(control);
        nodeObjects.attachChild(object);
        return control;
    }

    /**
     * Add an object
     *
     * @param p the point to add
     * @param objectId the object id
     * @param playerId the player id, the owner
     * @return the object contol
     */
    public ObjectControl addObject(Point p, short objectId, short playerId) {
        Spatial object = objectLoader.load(assetManager, p.x, p.y, 0, 0, 0, objectId, playerId);
        ObjectControl control = object.getControl(ObjectControl.class);
        objects.add(control);
        nodeObjects.attachChild(object);
        return control;
    }

    public void onObjectRemoved(ObjectControl object) {
        objects.remove(object);
    }

    public List<CreatureControl> getCreatures() {
        return new ArrayList<>(creatures);
    }

    public List<ObjectControl> getObjects() {
        return objects;
    }

    /**
     * If you want to get notified about the creature changes
     *
     * @param playerId the player id of which creatures you want to assign the
     * listener to
     * @param listener the listener
     */
    public void addListener(short playerId, CreatureListener listener) {
        if (creatureListeners == null) {
            creatureListeners = new HashMap<>();
        }
        List<CreatureListener> listeners = creatureListeners.get(playerId);
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        listeners.add(listener);
        creatureListeners.put(playerId, listeners);
    }

}
