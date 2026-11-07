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
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityContainer;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.RoomEffect;
import toniarts.openkeeper.game.component.ShotViewState;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Creature.CreatureFlag;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Light;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;
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
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.effect.VisualEffect;
import toniarts.openkeeper.view.loader.CreatureLoader;
import toniarts.openkeeper.view.loader.DoorLoader;
import toniarts.openkeeper.view.loader.ILoader;
import toniarts.openkeeper.view.loader.ObjectLoader;
import toniarts.openkeeper.view.loader.ShotLoader;
import toniarts.openkeeper.view.loader.TrapLoader;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;
import toniarts.openkeeper.view.lighting.LightConstants;
import toniarts.openkeeper.view.lighting.LightRules;
import toniarts.openkeeper.view.lighting.LightSource;
import toniarts.openkeeper.view.text.TextParser;

/**
 * A state that handles the showing of entities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerEntityViewState extends AbstractAppState {

    private static final Logger logger = System.getLogger(PlayerEntityViewState.class.getName());

    /**
     * Used where no fog of war applies: everything is simply always visible.
     */
    private static final IFogOfWarInformation ALWAYS_VISIBLE = new IFogOfWarInformation() {
        @Override
        public boolean isVisible(Point p) {
            return true;
        }

        @Override
        public boolean isExplored(Point p) {
            return true;
        }

        @Override
        public boolean isPerceived(Point p) {
            return true;
        }

        @Override
        public boolean isHighlightable(Point p) {
            return true;
        }

        @Override
        public boolean isPendingTagged(Point p) {
            return false;
        }
    };

    private AppStateManager stateManager;
    private final IKwdFile kwdFile;
    private final AssetManager assetManager;
    private final EntityData entityData;
    private final short playerId;
    private final Node rootNode;
    private final IMapDataInformation<? extends IMapTileInformation> mapData;
    private final IFogOfWarInformation fogOfWarInformation;

    private final TextParser textParser;
    private final Node root;
    private final Node nodeCreatures;
    private final Node nodeObjects;
    private final Node nodeDoors;
    private final Node nodeTraps;
    private final Node nodeShots;
    private final ObjectModelContainer objectModelContainer;
    private final CreatureModelContainer creatureModelContainer;
    private final DoorModelContainer doorModelContainer;
    private final TrapModelContainer trapModelContainer;
    private final RoomEffectContainer roomEffectContainer;
    private final ShotModelContainer shotModelContainer;

    private final ILoader<ObjectViewState> objectLoader;
    private final ILoader<CreatureViewState> creatureLoader;
    private final ILoader<DoorViewState> doorLoader;
    private final ILoader<TrapViewState> trapLoader;
    private final ILoader<ShotViewState> shotLoader;

    private final Map<EntityId, IUnitFlowerControl> flowerControls = new HashMap<>();
    private final Map<EntityId, IEntityViewControl> entityViewControls = new HashMap<>();
    private Spatial hiddenSpatial;
    private final Map<EntityId, Short> lightOwners = new HashMap<>(); // Owner a PLAYER_COLOURED light was built for
    /** Arches, big steps, plug and obstacle */
    private static final Set<Integer> DUNGEON_HEART_PART_IDS = Set.of(86, 88, 96, 100);
    private final Map<EntityId, TrackedLight> trackedLights = new HashMap<>(); // Things with a light, driven every frame

    /**
     * A thing whose light is created and positioned every frame (see
     * {@link #update(float)}), not only when its entity changes.
     */
    private record TrackedLight(Spatial spatial, Light light, float intensity, boolean honourPulse, boolean objectLight) {
    }

    public PlayerEntityViewState(IKwdFile kwdFile, AssetManager assetManager, EntityData entityData, short playerId, TextParser textParser, Node rootNode) {
        this(kwdFile, assetManager, entityData, playerId, textParser, rootNode, null, ALWAYS_VISIBLE);
    }

    public PlayerEntityViewState(IKwdFile kwdFile, AssetManager assetManager, EntityData entityData, short playerId,
            TextParser textParser, Node rootNode, IMapDataInformation<? extends IMapTileInformation> mapData) {
        this(kwdFile, assetManager, entityData, playerId, textParser, rootNode, mapData, ALWAYS_VISIBLE);
    }

    public PlayerEntityViewState(IKwdFile kwdFile, AssetManager assetManager, EntityData entityData, short playerId,
            TextParser textParser, Node rootNode, IMapDataInformation<? extends IMapTileInformation> mapData,
            IFogOfWarInformation fogOfWarInformation) {
        super(Short.toString(playerId));
        this.kwdFile = kwdFile;
        this.assetManager = assetManager;
        this.entityData = entityData;
        this.playerId = playerId;
        this.textParser = textParser;
        this.rootNode = rootNode;
        this.mapData = mapData;
        this.fogOfWarInformation = fogOfWarInformation;

        // Init the loaders
        objectLoader = new ObjectLoader(kwdFile);
        creatureLoader = new CreatureLoader(kwdFile);
        doorLoader = new DoorLoader(kwdFile);
        trapLoader = new TrapLoader(kwdFile);
        shotLoader = new ShotLoader(kwdFile);

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

        // Not under the root, the root is what the cursor picks from. Bolts and particles are nothing to point
        // at, and they would sit right in front of the creature the player keeps on striking
        nodeShots = new Node("Shots");

        // Create the model "listener"
        objectModelContainer = new ObjectModelContainer(entityData);
        creatureModelContainer = new CreatureModelContainer(entityData);
        doorModelContainer = new DoorModelContainer(entityData);
        trapModelContainer = new TrapModelContainer(entityData);
        roomEffectContainer = new RoomEffectContainer(entityData);
        shotModelContainer = new ShotModelContainer(entityData);
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.stateManager = stateManager;

        // Attach the entities
        rootNode.attachChild(root);
        rootNode.attachChild(nodeShots);

        // Start loading stuff (maybe we should do this earlier...)
        objectModelContainer.start();
        creatureModelContainer.start();
        doorModelContainer.start();
        trapModelContainer.start();
        roomEffectContainer.start();
        shotModelContainer.start();
    }

    @Override
    public void update(float tpf) {

        // Update the models
        objectModelContainer.update();
        creatureModelContainer.update();
        doorModelContainer.update();
        trapModelContainer.update();
        roomEffectContainer.update();
        shotModelContainer.update();

        // Lighting: re-derive attached-light positions and bind the
        // vertex-lit models every frame -- lights on other things
        // (a nearby flickering torch) change even for a thing that hasn't
        // itself moved, so this can't rely on EntityContainer's own
        // change-driven updateObject().
        LightRules lightRules = getLightRules();
        if (lightRules != null) {
            // Create missing thing lights and follow the (possibly moving)
            // owners: the local translation is what updateModelPosition just
            // set, whereas the world one lags a frame behind
            for (Map.Entry<EntityId, TrackedLight> entry : new ArrayList<>(trackedLights.entrySet())) {
                TrackedLight tracked = entry.getValue();
                applyThingLight(entry.getKey(), tracked.spatial(), tracked.light(), tracked.intensity(), tracked.honourPulse(), tracked.objectLight());
            }
            for (Spatial object : objectModelContainer.getModels()) {
                // The parts of the dungeon heart take no light from objects
                // (the heart's own glow, the arches' light), only from
                // effects, the hand, creatures, etc.
                ObjectViewControl control = object.getControl(ObjectViewControl.class);
                int objectId = control != null ? control.getDataObject().getObjectId() : -1;

                AssetUtils.applyVertexLight(assetManager, object, lightRules, false);
            }
            for (Spatial object : creatureModelContainer.getModels()) {
                // Creatures take no room / terrain ambient colour, only lights
                AssetUtils.applyVertexLight(assetManager, object, lightRules, false, false);
            }
            for (Spatial object : doorModelContainer.getModels()) {
                AssetUtils.applyVertexLight(assetManager, object, lightRules, false);
            }
            for (Spatial object : trapModelContainer.getModels()) {
                AssetUtils.applyVertexLight(assetManager, object, lightRules, false);
            }
        }
    }

    protected LightRules getLightRules() {
        PlayerMapViewState mapViewState = stateManager.getState(PlayerMapViewState.class);
        // Guard against racing the map loader thread's LightRules writes
        // (see PlayerMapViewState.isMapLoaded()) with a ConcurrentModificationException.
        return mapViewState != null && mapViewState.isMapLoaded() ? mapViewState.getMapLoader().getLightRules() : null;
    }

    @Override
    public void cleanup() {
        objectModelContainer.stop();
        creatureModelContainer.stop();
        doorModelContainer.stop();
        trapModelContainer.stop();
        roomEffectContainer.stop();
        shotModelContainer.stop();

        // Detach entities
        rootNode.detachChild(root);
        rootNode.detachChild(nodeShots);

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

    /**
     * Gets the model of an entity
     *
     * @param entityId the entity
     * @return the model, or {@code null} if the entity has no model (yet)
     */
    public Spatial getEntitySpatial(EntityId entityId) {
        IEntityViewControl control = entityViewControls.get(entityId);
        return control != null ? control.getSpatial() : null;
    }

    /**
     * Gets the view control of an entity, already holding the locally
     * resolved game data for the entity (e.g. the {@link Creature} for a
     * creature entity), so callers don't need to query the server for it
     *
     * @param entityId the entity
     * @return the view control, or {@code null} if the entity has no view
     * (yet)
     */
    public IEntityViewControl getEntityViewControl(EntityId entityId) {
        return entityViewControls.get(entityId);
    }

    /**
     * Hides the model of an entity regardless of fog of war, i.e. the creature
     * we are looking out of while possessing it
     *
     * @param entityId the entity to hide, {@code null} to show the previously
     * hidden one again
     */
    public void setHiddenEntity(EntityId entityId) {
        if (hiddenSpatial != null) {
            hiddenSpatial.setCullHint(Spatial.CullHint.Inherit);
        }
        Spatial previous = hiddenSpatial;
        hiddenSpatial = entityId != null ? getEntitySpatial(entityId) : null;
        if (hiddenSpatial != null) {
            hiddenSpatial.setCullHint(Spatial.CullHint.Always);
        }

        // The light's player colour depends on possession, drop the lights so
        // update() rebuilds them
        LightRules lightRules = getLightRules();
        if (lightRules != null) {
            for (Map.Entry<EntityId, TrackedLight> entry : trackedLights.entrySet()) {
                Spatial spatial = entry.getValue().spatial();
                if (spatial == previous || spatial == hiddenSpatial) {
                    lightRules.removeThingLight(entry.getKey());
                    lightOwners.remove(entry.getKey());
                }
            }
        }
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
                Creature lairCreatureType = objectViewState.lairCreatureTypeId != 0
                        ? kwdFile.getCreature(objectViewState.lairCreatureTypeId) : null;
                boolean uniqueCreatureBed = lairCreatureType != null
                        && lairCreatureType.getFlags().contains(CreatureFlag.IS_UNIQUE);
                EntityViewControl control = new ObjectViewControl(e.getId(), entityData,
                        kwdFile.getObject(objectViewState.objectId), objectViewState, assetManager,
                        textParser != null ? textParser.getObjectTextParser() : null, mapData,
                        uniqueCreatureBed);
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

    private boolean isWater(Vector3f position) {
        if (mapData == null) {
            return false;
        }
        IMapTileInformation tile = mapData.getTile(WorldUtils.vectorToPoint(position));
        if (tile == null) {
            return false;
        }
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        return terrain != null && terrain.getFlags().contains(Terrain.TerrainFlag.WATER);
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
                CreatureViewControl control = new CreatureViewControl(e.getId(), entityData, creature, creatureViewState.state, assetManager, textParser != null ? textParser.getCreatureTextParser() : null);
                control.setWaterCheck(this::isWater);
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
            EntityViewControl control = new DoorViewControl(e.getId(), entityData, door, doorViewState, assetManager, textParser != null ? textParser.getDoorTextParser() : null, kwdFile.getObject(door.getKeyObjectId()));
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
            EntityViewControl control = new TrapViewControl(e.getId(), entityData, trap, trapViewState, assetManager, textParser != null ? textParser.getTrapTextParser() : null);
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

    private Spatial createShotModel(Entity e) {
        ShotViewState shotViewState = e.get(ShotViewState.class);
        Spatial result = shotLoader.load(assetManager, shotViewState);
        if (result == null) {
            result = new Node("Wat"); // FIXME: Yeah...
        }
        nodeShots.attachChild(result);

        Vector3f position = e.get(Position.class).position;
        if (fogOfWarInformation.isVisible(WorldUtils.vectorToPoint(position))) {
            playShotEffects(kwdFile.getShotById(shotViewState.shotId), position);
        }

        return result;
    }

    /**
     * Plays the effects a shot starts with. They are not part of the shot
     * model, the sparks and the smoke of a strike outlast the bolt itself.
     */
    private void playShotEffects(Shot shot, Vector3f position) {
        EffectManagerState effectManagerState = stateManager.getState(EffectManagerState.class);
        if (effectManagerState == null) {
            return;
        }

        effectManagerState.load(nodeShots, position.clone(), shot.getCreationEffectId(), false);

        // A shot processed at its start hits there, a lightning strike for example
        if (shot.getShotProcessFlags().contains(Shot.ShotProcessFlag.AT_START)) {
            effectManagerState.load(nodeShots, position.clone(), shot.getHitThingEffectId(), false);
        }
    }

    private void updateCreatureModelAnimation(Spatial object, Entity e) {
        CreatureViewState viewState = e.get(CreatureViewState.class);

        Creature creature = kwdFile.getCreature(viewState.creatureId);
        applyThingLight(e.getId(), object, creature.getLight(), LightConstants.CREATURE_LIGHT_INTENSITY);
        IEntityViewControl control = object.getControl(IEntityViewControl.class);
        boolean wasAlreadyDead = control.getCurrentState() == Creature.AnimationType.DEATH_POSE;
        control.setTargetState(viewState.state);

        // Trigger the death effect once, when the creature actually dies
        if (!wasAlreadyDead && viewState.state == Creature.AnimationType.DEATH_POSE) {
            int deathEffectId = control.getDeathEffectId();
            if (deathEffectId == 0) {
                // use the default puddle of blood
                deathEffectId = 129;
            }
            stateManager.getState(EffectManagerState.class).load(nodeCreatures, object.getWorldTranslation(), deathEffectId, true, control.getOwnerId());
        }

        // Dead bodies are charred
        if (viewState.state == Creature.AnimationType.DEATH_POSE) {
            CharredOverlay.apply(object, assetManager);
        }
        applyFogCullHint(object, WorldUtils.vectorToPoint(e.get(Position.class).position), true);
    }

    private void updateDoorModelState(Spatial object, Entity e) {
        DoorViewState viewState = e.get(DoorViewState.class);
        object.getControl(DoorViewControl.class).setTargetState(viewState);
        applyFogCullHint(object, WorldUtils.vectorToPoint(e.get(Position.class).position), true);
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
        applyFogCullHint(object, WorldUtils.vectorToPoint(e.get(Position.class).position), viewState.visible);

        GameObject gameObject = kwdFile.getObject(viewState.objectId);
        if (LightConstants.OBJECT_LIGHTS_ENABLED) {
            applyThingLight(e.getId(), object, gameObject.getLight(), LightConstants.OBJECT_LIGHT_INTENSITY,
                    LightConstants.OBJECT_LIGHTS_PULSE, true);
        }
    }

    /**
     * The thing-light formula, for objects/creatures. Shots
     * and slab lights aren't wired here: there is no shot rendering and no slab-light data in this
     * codebase's parser. Everything else is implemented: the
     * colour-non-zero enable rule, over-bright bits, the player colour of
     * {@code PLAYER_COLOURED} lights, radius scale and pulse/flicker.
     */
    private void applyThingLight(EntityId id, Spatial object, Light light, float intensity) {
        applyThingLight(id, object, light, intensity, true, false);
    }

    /**
     * @param honourPulse false drops the data's PULSE/FLICKER request, as the
     * original does for object lights (the dungeon heart glow is constant)
     */
    private void applyThingLight(EntityId id, Spatial object, Light light, float intensity, boolean honourPulse, boolean objectLight) {
        LightRules lightRules = getLightRules();
        if (!LightConstants.isLightEnabled(light)) {
            trackedLights.remove(id);
            lightOwners.remove(id);
            if (lightRules != null) {
                lightRules.removeThingLight(id);
            }
            return;
        }
        trackedLights.put(id, new TrackedLight(object, light, intensity, honourPulse, objectLight));
        if (lightRules == null) {
            // Map still loading: update() creates the light as soon as the
            // map is ready, without waiting for the entity to change again
            // (an idle imp might never)
            return;
        }

        // A PLAYER_COLOURED light takes the owner's colour, so a change of
        // owner (e.g. a converted creature) needs the light rebuilt
        // ...except the creature being possessed, whose light stays neutral
        boolean possessed = object == hiddenSpatial;
        boolean playerColoured = !possessed && light.getFlags().contains(Light.LightFlag.PLAYER_COLOURED);
        // The view control already watches the entity's Owner
        IEntityViewControl viewControl = playerColoured ? entityViewControls.get(id) : null;
        Short ownerId = viewControl != null ? Short.valueOf(viewControl.getOwnerId()) : null;
        if (playerColoured && lightRules.getThingLight(id) != null
                && !Objects.equals(lightOwners.get(id), ownerId)) {
            lightRules.removeThingLight(id);
        }

        Vector3f offset = LightConstants.toJmeOffset(light.getmKPos());
        if (lightRules.getThingLight(id) == null) {
            if (playerColoured) {
                lightOwners.put(id, ownerId);
            }
            LightSource source = lightRules.addThingLight(id, object.getLocalTranslation(), light, ownerId,
                    intensity, honourPulse, !possessed);
            source.setObjectLight(objectLight);
            if (objectLight) {
                source.setRadius(Math.min(source.getRadius() * LightConstants.OBJECT_LIGHT_RADIUS_SCALE,
                        LightConstants.MAX_LIGHT_RADIUS_TILES));
            }
        } else {
            lightRules.updateThingLightPosition(id, object.getLocalTranslation(), offset);
        }
    }

    /**
     * Hides things standing on non-visible tiles.
     * There is no "visible through fog" per-thing flag anywhere in this
     * codebase's creature/object/door/trap data, so this applies uniformly,
     * with no exceptions.
     */
    private void applyFogCullHint(Spatial object, Point tile, boolean baseVisible) {
        if (object == hiddenSpatial) {
            object.setCullHint(Spatial.CullHint.Always);
            return;
        }
        boolean visible = baseVisible;
        if (visible) {
            visible = fogOfWarInformation.isVisible(tile);
        }
        object.setCullHint(visible ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
    }

    /**
     * Fog-of-war state isn't an entity component, so a tile transitioning
     * between explored/unexplored produces no {@link Position}/view-state
     * component change on the (typically stationary) entities standing on
     * it - the normal {@code updateObject*} paths above, which only run on
     * such a change, would otherwise never re-run for them. This is called
     * whenever fog notifies of newly-dirty tiles (mirroring how
     * {@code MapViewController} re-derives terrain for the same tiles) to
     * directly re-evaluate cull hint for every currently-tracked entity
     * sitting on one of them.
     */
    public void onTilesDirty(Point[] points) {
        if (points.length == 0 || entityViewControls.isEmpty()) {
            return;
        }
        Set<Point> dirty = new HashSet<>(Arrays.asList(points));
        for (IEntityViewControl control : entityViewControls.values()) {

            // From the control: its spatial stands at the entity's position, and an object's control has the latest view state
            Spatial spatial = control.getSpatial();
            Point tile = WorldUtils.vectorToPoint(spatial.getLocalTranslation());
            if (!dirty.contains(tile)) {
                continue;
            }
            boolean baseVisible = !(control instanceof ObjectViewControl objectViewControl) || objectViewControl.getCurrentState().visible;
            applyFogCullHint(spatial, tile, baseVisible);
        }
    }

    private void updateModelPosition(Spatial object, Entity e) {
        Position position = e.get(Position.class);
        object.setLocalTranslation(position.position);
        object.setLocalRotation(object.getLocalRotation().fromAngles(0, position.rotation, 0));
    }

    private void removeModel(Spatial spatial, Entity e) {
        spatial.removeFromParent();

        // Stop driving the light first, or update() would re-create it
        trackedLights.remove(e.getId());
        lightOwners.remove(e.getId());
        LightRules lightRules = getLightRules();
        if (lightRules != null) {
            lightRules.removeThingLight(e.getId());
        }

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
    private final class ObjectModelContainer extends EntityContainer<Spatial> {

        public ObjectModelContainer(EntityData ed) {
            super(ed, Position.class, ObjectViewState.class); // Stuff with position is on the map
        }

        @Override
        protected Spatial addObject(Entity e) {
            logger.log(Level.TRACE, "ObjectModelContainer.addObject({0})", e);
            Spatial result = createObjectModel(e);
            updateObject(result, e);
            return result;
        }

        @Override
        protected void updateObject(Spatial object, Entity e) {
            logger.log(Level.TRACE, "ObjectModelContainer.updateObject({0})", e);
            updateModelPosition(object, e);
            updateObjectModelState(object, e);
        }

        @Override
        protected void removeObject(Spatial object, Entity e) {
            removeModel(object, e);
        }

        Spatial[] getModels() {
            return getArray();
        }

    }

    /**
     * Contains the creatures...
     */
    private final class CreatureModelContainer extends EntityContainer<Spatial> {

        public CreatureModelContainer(EntityData ed) {
            super(ed, Position.class, CreatureViewState.class); // Stuff with position is on the map
        }

        @Override
        protected Spatial addObject(Entity e) {
            logger.log(Level.TRACE, "CreatureModelContainer.addObject({0})", e);
            Spatial result = createCreatureModel(e);
            updateObject(result, e);
            return result;
        }

        @Override
        protected void updateObject(Spatial object, Entity e) {
            logger.log(Level.TRACE, "CreatureModelContainer.updateObject({0})", e);
            updateModelPosition(object, e);
            updateCreatureModelAnimation(object, e);
        }

        @Override
        protected void removeObject(Spatial object, Entity e) {
            removeModel(object, e);
        }

        Spatial[] getModels() {
            return getArray();
        }

    }

    /**
     * Contains the static doors...
     */
    private final class DoorModelContainer extends EntityContainer<Spatial> {

        public DoorModelContainer(EntityData ed) {
            super(ed, Position.class, DoorViewState.class); // Stuff with position is on the map
        }

        @Override
        protected Spatial addObject(Entity e) {
            logger.log(Level.TRACE, "DoorModelContainer.addObject({0})", e);
            Spatial result = createDoorModel(e);
            updateObject(result, e);
            return result;
        }

        @Override
        protected void updateObject(Spatial object, Entity e) {
            logger.log(Level.TRACE, "DoorModelContainer.updateObject({0})", e);
            updateModelPosition(object, e); // LOL, but ok
            updateDoorModelState(object, e);
        }

        @Override
        protected void removeObject(Spatial object, Entity e) {
            removeModel(object, e);
        }

        Spatial[] getModels() {
            return getArray();
        }
    }

    /**
     * Contains the static traps...
     */
    private final class TrapModelContainer extends EntityContainer<Spatial> {

        public TrapModelContainer(EntityData ed) {
            super(ed, Position.class, TrapViewState.class); // Stuff with position is on the map
        }

        @Override
        protected Spatial addObject(Entity e) {
            logger.log(Level.TRACE, "TrapModelContainer.addObject({0})", e);
            Spatial result = createTrapModel(e);
            updateObject(result, e);
            return result;
        }

        @Override
        protected void updateObject(Spatial object, Entity e) {
            logger.log(Level.TRACE, "TrapModelContainer.updateObject({0})", e);
            updateModelPosition(object, e);
            //updateModelAnimation(object, e);
            applyFogCullHint(object, WorldUtils.vectorToPoint(e.get(Position.class).position), true);
        }

        @Override
        protected void removeObject(Spatial object, Entity e) {
            removeModel(object, e);
        }

        Spatial[] getModels() {
            return getArray();
        }
    }

    /**
     * Watches for room-tied effects (currently only the hero gate's gem
     * holder swirl) and spawns them - fires identically regardless of
     * whether the map was loaded for the main menu or a real game, since
     * both attach a PlayerEntityViewState (or MainMenuEntityViewState).
     */
    private final class RoomEffectContainer extends EntityContainer<Boolean> {

        public RoomEffectContainer(EntityData ed) {
            super(ed, RoomEffect.class);
        }

        @Override
        protected Boolean addObject(Entity e) {
            logger.log(Level.TRACE, "RoomEffectContainer.addObject({0})", e);
            RoomEffect roomEffect = e.get(RoomEffect.class);
            // Effect 350 sits half a tile further than the gem holder's own
            // tile centre
            Vector3f location = WorldUtils.pointToVector3f(roomEffect.location)
                    .addLocal(0, WorldUtils.FLOOR_HEIGHT, 0.5f + roomEffect.offsetZ);
            VisualEffect visualEffect = stateManager.getState(EffectManagerState.class).load(root, location, roomEffect.effectId, true);
            if (visualEffect != null && roomEffect.yaw != 0) {
                visualEffect.getEffectNode().rotate(0, roomEffect.yaw, 0);
            }
            return Boolean.TRUE;
        }

        @Override
        protected void updateObject(Boolean object, Entity e) {
            // Location/effect id never change after construction
        }

        @Override
        protected void removeObject(Boolean object, Entity e) {
            // Not individually removable today - only
            // EffectManagerState.clearActiveEffects() clears everything
        }
    }

    /**
     * Contains the shots, spells and missiles on their way or striking
     */
    private final class ShotModelContainer extends EntityContainer<Spatial> {

        public ShotModelContainer(EntityData ed) {
            super(ed, Position.class, ShotViewState.class);
        }

        @Override
        protected Spatial addObject(Entity e) {
            logger.log(Level.TRACE, "ShotModelContainer.addObject({0})", e);
            Spatial result = createShotModel(e);
            updateObject(result, e);
            return result;
        }

        @Override
        protected void updateObject(Spatial object, Entity e) {
            logger.log(Level.TRACE, "ShotModelContainer.updateObject({0})", e);
            updateModelPosition(object, e);
            applyFogCullHint(object, WorldUtils.vectorToPoint(e.get(Position.class).position), true);
        }

        @Override
        protected void removeObject(Spatial object, Entity e) {
            removeModel(object, e);
        }
    }
}
