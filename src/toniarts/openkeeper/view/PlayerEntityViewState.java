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
import com.simsilica.es.EntityId;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.view.control.CreatureFlowerControl;
import toniarts.openkeeper.view.control.CreatureViewControl;
import toniarts.openkeeper.view.control.DoorFlowerControl;
import toniarts.openkeeper.view.control.DoorViewControl;
import toniarts.openkeeper.view.control.EntityViewControl;
import toniarts.openkeeper.view.control.IEntityViewControl;
import toniarts.openkeeper.view.control.IUnitFlowerControl;
import toniarts.openkeeper.view.control.ObjectViewControl;
import toniarts.openkeeper.view.control.TrapFlowerControl;
import toniarts.openkeeper.view.control.TrapViewControl;
import toniarts.openkeeper.view.loader.CreatureLoader;
import toniarts.openkeeper.view.loader.DoorLoader;
import toniarts.openkeeper.view.loader.ILoader;
import toniarts.openkeeper.view.loader.ObjectLoader;
import toniarts.openkeeper.view.loader.TrapLoader;
import toniarts.openkeeper.view.text.TextParser;

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
    private final short playerId;

    private final TextParser textParser;
    private final Node root;
    private final Node nodeCreatures;
    private final Node nodeObjects;
    private final Node nodeDoors;
    private final Node nodeTraps;
    private final ObjectModelContainer objectModelContainer;
    private final CreatureModelContainer creatureModelContainer;
    private final DoorModelContainer doorModelContainer;
    private final TrapModelContainer trapModelContainer;

    private final ILoader<ObjectViewState> objectLoader;
    private final ILoader<CreatureViewState> creatureLoader;
    private final ILoader<DoorViewState> doorLoader;
    private final ILoader<TrapViewState> trapLoader;

    private final Map<EntityId, IUnitFlowerControl> flowerControls = new HashMap<>();
    private final Map<EntityId, IEntityViewControl> entityViewControls = new HashMap<>();

    private static final Logger LOGGER = Logger.getLogger(PlayerEntityViewState.class.getName());

    public PlayerEntityViewState(KwdFile kwdFile, AssetManager assetManager, EntityData entityData, short playerId, TextParser textParser) {
        this.kwdFile = kwdFile;
        this.assetManager = assetManager;
        this.entityData = entityData;
        this.playerId = playerId;
        this.textParser = textParser;

        // Init the loaders
        objectLoader = new ObjectLoader(kwdFile);
        creatureLoader = new CreatureLoader(kwdFile);
        doorLoader = new DoorLoader(kwdFile);
        trapLoader = new TrapLoader(kwdFile);

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
        objectModelContainer = new ObjectModelContainer(entityData);
        creatureModelContainer = new CreatureModelContainer(entityData);
        doorModelContainer = new DoorModelContainer(entityData);
        trapModelContainer = new TrapModelContainer(entityData);
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        this.stateManager = stateManager;

        // Attach the entities
        this.app.getRootNode().attachChild(root);

        // Start loading stuff (maybe we should do this earlier...)
        objectModelContainer.start();
        creatureModelContainer.start();
        doorModelContainer.start();
        trapModelContainer.start();
    }

    @Override
    public void update(float tpf) {

        // Update the models
        objectModelContainer.update();
        creatureModelContainer.update();
        doorModelContainer.update();
        trapModelContainer.update();
    }

    @Override
    public void cleanup() {
        objectModelContainer.stop();
        creatureModelContainer.stop();
        doorModelContainer.stop();
        trapModelContainer.stop();

        // Detach entities
        app.getRootNode().detachChild(root);

        for (IEntityViewControl entityViewControl : entityViewControls.values()) {
            entityViewControl.cleanup();
        }
        for (IUnitFlowerControl flowerControl : flowerControls.values()) {
            flowerControl.cleanup();
        }

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

    private Spatial createObjectModel(Entity e) {

        // We can only draw the few basic types, maybe we can do it like this
        // Kinda cludge perhaps, but doors can have traps, but if a door is found from the entity, draw it as a door
        // Otherwise make an object type thingie
        // Also the syncing, I don't know do we need a "listener" for all these classes then...
        // TODO: object & gold amount are now separate, how to display the different amounts of gold, the model index?
        // The server doesn't need to know it, physics are based on different things I assume, never in the server we load assets
        // Also maybe would be nice to batch up room pillars like before
        Spatial result = null;
        ObjectViewState objectViewState = e.get(ObjectViewState.class);
        if (objectViewState != null) {
            result = objectLoader.load(assetManager, objectViewState);
            if (result != null) {
                EntityViewControl control = new ObjectViewControl(e.getId(), entityData, kwdFile.getObject(objectViewState.objectId), objectViewState, assetManager, textParser);
                result.addControl(control);

                result.setCullHint(objectViewState.visible ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);

                entityViewControls.put(e.getId(), control);
            }
        }
        if (result == null) {
            result = new Node("Wat"); // FIXME: Yeah...
        }
        nodeObjects.attachChild(result);
        return result;
    }

    private Spatial createCreatureModel(Entity e) {

        // We can only draw the few basic types, maybe we can do it like this
        // Kinda cludge perhaps, but doors can have traps, but if a door is found from the entity, draw it as a door
        // Otherwise make an object type thingie
        // Also the syncing, I don't know do we need a "listener" for all these classes then...
        // TODO: object & gold amount are now separate, how to display the different amounts of gold, the model index?
        // The server doesn't need to know it, physics are based on different things I assume, never in the server we load assets
        // Also maybe would be nice to batch up room pillars like before
        Spatial result = null;
        CreatureViewState creatureViewState = e.get(CreatureViewState.class);
        if (creatureViewState != null) {
            Creature creature = kwdFile.getCreature(creatureViewState.creatureId);
            result = creatureLoader.load(assetManager, creatureViewState);
            if (result != null) {
                EntityViewControl control = new CreatureViewControl(e.getId(), entityData, creature, creatureViewState.state, assetManager, textParser);
                result.addControl(control);

                CreatureFlowerControl flowerControl = new CreatureFlowerControl(e.getId(), entityData, creature, assetManager);
                result.addControl(flowerControl);

                entityViewControls.put(e.getId(), control);
                flowerControls.put(e.getId(), flowerControl);
            }
        }
        if (result == null) {
            result = new Node("Wat"); // FIXME: Yeah...
        }
        nodeCreatures.attachChild(result);
        return result;
    }

    private Spatial createDoorModel(Entity e) {

        // We can only draw the few basic types, maybe we can do it like this
        // Kinda cludge perhaps, but doors can have traps, but if a door is found from the entity, draw it as a door
        // Otherwise make an object type thingie
        // Also the syncing, I don't know do we need a "listener" for all these classes then...
        // TODO: object & gold amount are now separate, how to display the different amounts of gold, the model index?
        // The server doesn't need to know it, physics are based on different things I assume, never in the server we load assets
        // Also maybe would be nice to batch up room pillars like before
        Spatial result = null;
        DoorViewState doorViewState = e.get(DoorViewState.class);
        if (doorViewState != null) {
            Door door = kwdFile.getDoorById(doorViewState.doorId);
            result = doorLoader.load(assetManager, doorViewState);
            EntityViewControl control = new DoorViewControl(e.getId(), entityData, door, doorViewState, assetManager, textParser, kwdFile.getObject(door.getKeyObjectId()));
            result.addControl(control);

            DoorFlowerControl flowerControl = new DoorFlowerControl(e.getId(), entityData, door, assetManager);
            result.addControl(flowerControl);

            entityViewControls.put(e.getId(), control);
            flowerControls.put(e.getId(), flowerControl);
        }
        if (result == null) {
            result = new Node("Wat"); // FIXME: Yeah...
        }
        nodeDoors.attachChild(result);
        return result;
    }

    private Spatial createTrapModel(Entity e) {

        // We can only draw the few basic types, maybe we can do it like this
        // Kinda cludge perhaps, but doors can have traps, but if a door is found from the entity, draw it as a door
        // Otherwise make an object type thingie
        // Also the syncing, I don't know do we need a "listener" for all these classes then...
        // TODO: object & gold amount are now separate, how to display the different amounts of gold, the model index?
        // The server doesn't need to know it, physics are based on different things I assume, never in the server we load assets
        // Also maybe would be nice to batch up room pillars like before
        Spatial result = null;
        TrapViewState trapViewState = e.get(TrapViewState.class);
        if (trapViewState != null) {
            Trap trap = kwdFile.getTrapById(trapViewState.trapId);
            result = trapLoader.load(assetManager, trapViewState);
            EntityViewControl control = new TrapViewControl(e.getId(), entityData, trap, trapViewState, assetManager, textParser);
            result.addControl(control);

            TrapFlowerControl flowerControl = new TrapFlowerControl(e.getId(), entityData, trap, assetManager);
            result.addControl(flowerControl);

            entityViewControls.put(e.getId(), control);
            flowerControls.put(e.getId(), flowerControl);
        }
        if (result == null) {
            result = new Node("Wat"); // FIXME: Yeah...
        }
        nodeTraps.attachChild(result);
        return result;
    }

    private void updateCreatureModelAnimation(Spatial object, Entity e) {
        CreatureViewState viewState = e.get(CreatureViewState.class);
        object.getControl(IEntityViewControl.class).setTargetState(viewState.state);
    }

    private void updateDoorModelState(Spatial object, Entity e) {
        DoorViewState viewState = e.get(DoorViewState.class);
        object.getControl(DoorViewControl.class).setTargetState(viewState);
    }

    private void updateObjectModelState(Spatial object, Entity e) {
        ObjectViewState viewState = e.get(ObjectViewState.class);
        ObjectViewControl control = object.getControl(ObjectViewControl.class);

        if (control != null) {

            // Phew, maybe should be dynamic...
            if (control.getDataObject().getObjectId() != viewState.objectId) {
                control.setDataObject(kwdFile.getObject(viewState.objectId));
            }

            control.setTargetState(viewState);
        }
        object.setCullHint(viewState.visible ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
    }

    private void updateModelPosition(Spatial object, Entity e) {
        Position position = e.get(Position.class);
        object.setLocalTranslation(position.position);
        object.setLocalRotation(object.getLocalRotation().fromAngles(0, position.rotation, 0));
    }

    private void removeModel(Spatial spatial, Entity e) {
        spatial.removeFromParent();

        IEntityViewControl entityViewControl = entityViewControls.remove(e.getId());
        if (entityViewControl != null) {
            entityViewControl.cleanup();
        }
        IUnitFlowerControl unitFlowerControl = flowerControls.remove(e.getId());
        if (unitFlowerControl != null) {
            unitFlowerControl.cleanup();
        }
    }

    public void showUnitFlower(EntityId entityId, int interval) {

        // FIXME: We may not yet have the entity as visible, is this a problem?
        IUnitFlowerControl flowerControl = flowerControls.get(entityId);
        if (flowerControl != null) {
            flowerControl.show(interval);
        }
    }

    /**
     * Contains the static(ish) objects...
     */
    private class ObjectModelContainer extends EntityContainer<Spatial> {

        public ObjectModelContainer(EntityData ed) {
            super(ed, Position.class, ObjectViewState.class); // Stuff with position is on the map
        }

        @Override
        protected Spatial addObject(Entity e) {
            LOGGER.log(Level.FINEST, "ObjectModelContainer.addObject({0})", e);
            Spatial result = createObjectModel(e);
            updateObject(result, e);
            return result;
        }

        @Override
        protected void updateObject(Spatial object, Entity e) {
            LOGGER.log(Level.FINEST, "ObjectModelContainer.updateObject({0})", e);
            updateModelPosition(object, e);
            updateObjectModelState(object, e);
        }

        @Override
        protected void removeObject(Spatial object, Entity e) {
            removeModel(object, e);
        }

    }

    /**
     * Contains the creatures...
     */
    private class CreatureModelContainer extends EntityContainer<Spatial> {

        public CreatureModelContainer(EntityData ed) {
            super(ed, Position.class, CreatureViewState.class); // Stuff with position is on the map
        }

        @Override
        protected Spatial addObject(Entity e) {
            LOGGER.log(Level.FINEST, "CreatureModelContainer.addObject({0})", e);
            Spatial result = createCreatureModel(e);
            updateObject(result, e);
            return result;
        }

        @Override
        protected void updateObject(Spatial object, Entity e) {
            LOGGER.log(Level.FINEST, "CreatureModelContainer.updateObject({0})", e);
            updateModelPosition(object, e);
            updateCreatureModelAnimation(object, e);
        }

        @Override
        protected void removeObject(Spatial object, Entity e) {
            removeModel(object, e);
        }

    }

    /**
     * Contains the static doors...
     */
    private class DoorModelContainer extends EntityContainer<Spatial> {

        public DoorModelContainer(EntityData ed) {
            super(ed, Position.class, DoorViewState.class); // Stuff with position is on the map
        }

        @Override
        protected Spatial addObject(Entity e) {
            LOGGER.log(Level.FINEST, "DoorModelContainer.addObject({0})", e);
            Spatial result = createDoorModel(e);
            updateObject(result, e);
            return result;
        }

        @Override
        protected void updateObject(Spatial object, Entity e) {
            LOGGER.log(Level.FINEST, "DoorModelContainer.updateObject({0})", e);
            updateModelPosition(object, e); // LOL, but ok
            updateDoorModelState(object, e);
        }

        @Override
        protected void removeObject(Spatial object, Entity e) {
            removeModel(object, e);
        }
    }

    /**
     * Contains the static traps...
     */
    private class TrapModelContainer extends EntityContainer<Spatial> {

        public TrapModelContainer(EntityData ed) {
            super(ed, Position.class, TrapViewState.class); // Stuff with position is on the map
        }

        @Override
        protected Spatial addObject(Entity e) {
            LOGGER.log(Level.FINEST, "TrapModelContainer.addObject({0})", e);
            Spatial result = createTrapModel(e);
            updateObject(result, e);
            return result;
        }

        @Override
        protected void updateObject(Spatial object, Entity e) {
            LOGGER.log(Level.FINEST, "TrapModelContainer.updateObject({0})", e);
            updateModelPosition(object, e);
            //updateModelAnimation(object, e);
        }

        @Override
        protected void removeObject(Spatial object, Entity e) {
            removeModel(object, e);
        }
    }
}
