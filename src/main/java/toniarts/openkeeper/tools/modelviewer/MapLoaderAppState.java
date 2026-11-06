/*
 * Copyright (C) 2014-2021 OpenKeeper
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
package toniarts.openkeeper.tools.modelviewer;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.input.InputManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import toniarts.openkeeper.game.event.GameEvent;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.controller.GameController;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.PlayerEntityViewState;
import toniarts.openkeeper.view.lighting.LightSource;
import toniarts.openkeeper.view.lighting.LightRules;
import toniarts.openkeeper.view.map.MapViewController;
import toniarts.openkeeper.view.text.TextParser;

import java.util.Collections;

/**
 * Our current map loader only functions with the game data. This creates fake
 * games to load a map.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class MapLoaderAppState extends AbstractAppState {

    protected AssetManager assetManager;
    protected AppStateManager stateManager;
    protected InputManager inputManager;

    private EntityData mapEntityData;
    private MapLoaderAppState.MapEntityViewState mainMenuEntityViewState;
    private GameController gameController;
    private volatile MapViewController mapViewController;
    private Application app;
    private final Set<LightSource.Group> disabledLightGroups = EnumSet.noneOf(LightSource.Group.class);
    private final ColorRGBA ambientScratch = new ColorRGBA();
    private boolean lightGroupsDirty = true;

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);

        this.app = app;
        this.stateManager = stateManager;
        assetManager = app.getAssetManager();
        inputManager = app.getInputManager();
    }

    public Node loadMap(IKwdFile kwdFile, boolean litSolidRock, boolean terrainAmbient, boolean ceilingVisible) {
        unloadMap();

        mapEntityData = new DefaultEntityData();

        Node mapNode = new Node("Map");
        gameController = new GameController(kwdFile, Collections.emptyList(), mapEntityData, kwdFile.getVariables(), new MapLoaderAppState.MapPlayerService());
        gameController.createNewGame();

        // Create the actual map
        MapViewController mapLoader = new MapViewController(assetManager, kwdFile, gameController.getGameWorldController().getMapController(), Player.KEEPER1_ID, true, litSolidRock, true) {

            @Override
            protected void updateProgress(float progress) {

            }

        };
        mapLoader.setTerrainAmbientEnabled(terrainAmbient);
        mapLoader.setCeilingVisible(ceilingVisible);
        mapNode.attachChild(mapLoader.load(assetManager, kwdFile));
        mapLoader.getLightRules().publishReady();

        // No fog of war here (MapViewController's ALWAYS_VISIBLE), so the whole
        // map is lit; update() below drives the lights after publishReady().
        mapViewController = mapLoader;

        mainMenuEntityViewState = new MapLoaderAppState.MapEntityViewState(kwdFile, assetManager, mapEntityData, Player.KEEPER1_ID, null, mapNode, () -> mapViewController);
        stateManager.attach(mainMenuEntityViewState);

        return mapNode;
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        // Lighting: the terrain materials are only lit once the light data is
        // uploaded every frame (flicker, pulse, ...), as PlayerMapViewState does
        MapViewController controller = mapViewController;
        if (controller != null) {
            LightRules lightRules = controller.getLightRules();
            if (!lightRules.isReady()) {
                return;
            }
            Settings settings = Main.getUserSettings();
            float ambient = settings.getFloat(Settings.Setting.AMBIENT_LIGHT);
            ambientScratch.set(ambient, ambient, ambient, 1f);
            lightRules.setGlobalAmbient(ambientScratch);
            lightRules.setCheapLighting(settings.getBoolean(Settings.Setting.CHEAP_LIGHTING));
            if (lightGroupsDirty) {
                for (LightSource.Group group : LightSource.Group.values()) {
                    lightRules.setGroupEnabled(group, !disabledLightGroups.contains(group));
                }
                lightGroupsDirty = false;
            }
            lightRules.update(tpf, app.getCamera());
        }
    }

    boolean isLightGroupEnabled(LightSource.Group group) {
        return !disabledLightGroups.contains(group);
    }

    /**
     * Applies to the current and any later loaded map, no reload needed.
     */
    void setLightGroupEnabled(LightSource.Group group, boolean enabled) {
        boolean changed = enabled ? disabledLightGroups.remove(group) : disabledLightGroups.add(group);
        if (changed) {
            lightGroupsDirty = true;
        }
    }

    private void unloadMap() {
        if (mapViewController != null) {
            mapViewController.getLightRules().dispose();
            mapViewController = null;
        }
        if (mainMenuEntityViewState != null) {
            stateManager.detach(mainMenuEntityViewState);
            mainMenuEntityViewState = null;
        }
        if (gameController != null) {
            gameController.stop();
            gameController = null;
        }
        if (mapEntityData != null) {
            mapEntityData.close();
            mapEntityData = null;
        }
    }

    /**
     * Shows or hides the currently loaded map's ceiling, if any map is
     * loaded.
     *
     * @param visible show the ceiling
     */
    public void setCeilingVisible(boolean visible) {
        if (mapViewController != null) {
            mapViewController.setCeilingVisible(visible);
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();

        unloadMap();
    }

    private static final class MapEntityViewState extends PlayerEntityViewState {

        private final Supplier<MapViewController> mapViewController;

        public MapEntityViewState(IKwdFile kwdFile, AssetManager assetManager, EntityData entityData, short playerId, TextParser textParser, Node rootNode, Supplier<MapViewController> mapViewController) {
            super(kwdFile, assetManager, entityData, playerId, textParser, rootNode);
            this.mapViewController = mapViewController;

            setId("Map: " + kwdFile.getGameLevel().getName());
        }

        /**
         * The viewer has no PlayerMapViewState, the light rules are the loaded map's own
         */
        @Override
        protected LightRules getLightRules() {
            MapViewController controller = mapViewController.get();
            if (controller == null) {
                return null;
            }
            LightRules lightRules = controller.getLightRules();
            return lightRules.isReady() ? lightRules : null;
        }
    }

    private static final class MapPlayerService implements PlayerService {

        public MapPlayerService() {
        }

        @Override
        public void setWidescreen(boolean enable, short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void playSpeech(int speechId, boolean showText, boolean introduction, int pathId, short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isInTransition() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void doTransition(short pathId, Vector3f start, short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void flashButton(TriggerAction.MakeType buttonType, short targetId, TriggerAction.ButtonType targetButtonType, boolean enabled, int time, short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void rotateViewAroundPoint(Vector3f point, boolean relative, int angle, int time, short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void showMessage(int textId, short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void zoomViewToPoint(Vector3f point, short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void zoomViewToEntity(EntityId entityId, short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setGamePaused(boolean paused) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void showUnitFlower(EntityId entityId, int interval, short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setPossession(EntityId target, short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void endGame(boolean win, short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void sendEvent(GameEventType type, GameEvent.ThingFlag subjectKind, int subjectId, GameEvent.ThingFlag targetKind, int targetId, EntityId entity, Point position, Short playerId) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

}
