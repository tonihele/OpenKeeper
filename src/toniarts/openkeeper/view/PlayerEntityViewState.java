/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.view;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityContainer;
import com.simsilica.es.EntityData;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.component.ObjectEntity;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.view.control.EntityControl;
import toniarts.openkeeper.view.loader.ILoader;
import toniarts.openkeeper.view.loader.ObjectLoader;

/**
 * A state that handles the showing of entities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerEntityViewState extends AbstractAppState {

    private Main app;
    private AppStateManager stateManager;
    private final KwdFile kwdFile;
    private final AssetManager assetManager;
    private final EntityData entityData;

    private final Node root;
    private final Node nodeCreatures;
    private final Node nodeObjects;
    private final Node nodeDoors;
    private final Node nodeTraps;
    private final ModelContainer modelContainer;

    private final ILoader<ObjectEntity> objectLoader;

    private static final Logger logger = Logger.getLogger(PlayerEntityViewState.class.getName());

    public PlayerEntityViewState(KwdFile kwdFile, AssetManager assetManager, EntityData entityData, short playerId) {
        this.kwdFile = kwdFile;
        this.assetManager = assetManager;
        this.entityData = entityData;

        // Init the loaders
        objectLoader = new ObjectLoader(kwdFile);

        // Create the scene graph
        root = new Node("Things");
        nodeCreatures = new Node("Creatures");
        nodeObjects = new Node("Objects");
        nodeDoors = new Node("Doors");
        nodeTraps = new Node("Traps");
        root.attachChild(nodeCreatures);
        root.attachChild(nodeObjects);
        root.attachChild(nodeDoors);
        root.attachChild(nodeTraps);

        // Create the model "listener"
        modelContainer = new ModelContainer(entityData);
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        this.stateManager = stateManager;

        // Attach the entities
        this.app.getRootNode().attachChild(root);

        // Start loading stuff (maybe we should do this earlier...)
        modelContainer.start();
    }

    @Override
    public void update(float tpf) {

        // Update the models
        modelContainer.update();
    }

    @Override
    public void cleanup() {

        // Detach entities
        app.getRootNode().detachChild(root);
        modelContainer.stop();

        super.cleanup();
    }

    /**
     * Gets the entity view root node (no map, just entities)
     *
     * @return the root node
     */
    public Node getRoot() {
        return root;
    }

    private Spatial createModel(Entity e) {

        // We can only draw the few basic types, maybe we can do it like this
        // Kinda cludge perhaps, but doors can have traps, but if a door is found from the entity, draw it as a door
        // Otherwise make an object type thingie
        // Also the syncing, I don't know do we need a "listener" for all these classes then...
        // TODO: object & gold amount are now separate, how to display the different amounts of gold, the model index?
        // The server doesn't need to know it, physics are based on different things I assume, never in the server we load assets
        // Also maybe would be nice to batch up room pillars like before
        Spatial result = new Node("Wat"); // FIXME: Yeah...
        ObjectEntity objectEntity = e.get(ObjectEntity.class);
        if (objectEntity != null) {
            result = objectLoader.load(assetManager, objectEntity);
            EntityControl control = new EntityControl(e.getId(), entityData);
            result.addControl(control);
            nodeObjects.attachChild(result);
        }
        return result;
    }

    private void updateModel(Spatial object, Entity e) {
        Position position = e.get(Position.class);
        object.setLocalTranslation(position.position);
        object.setLocalRotation(object.getLocalRotation().fromAngles(0, position.rotation, 0));
    }

    private void removeModel(Spatial object, Entity e) {
        object.removeFromParent();
    }

    /**
     * Contains the static objects...
     */
    private class ModelContainer extends EntityContainer<Spatial> {

        public ModelContainer(EntityData ed) {
            super(ed, Position.class, ObjectEntity.class); // Stuff with position is on the map
        }

        @Override
        protected Spatial addObject(Entity e) {
            logger.log(Level.FINEST, "ModelContainer.addObject({0})", e);
            Spatial result = createModel(e);
            updateObject(result, e);
            return result;
        }

        @Override
        protected void updateObject(Spatial object, Entity e) {
            logger.log(Level.FINEST, "ModelContainer.updateObject({0})", e);
            updateModel(object, e);
        }

        @Override
        protected void removeObject(Spatial object, Entity e) {
            removeModel(object, e);
        }

    }
}
