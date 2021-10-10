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
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.util.Collections;
import toniarts.openkeeper.game.controller.GameController;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.view.PlayerEntityViewState;
import toniarts.openkeeper.view.map.MapViewController;
import toniarts.openkeeper.view.text.TextParser;

/**
 * Our current map loader only functions with the game data. This creates fake
 * games to load a map.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MapLoaderAppState extends AbstractAppState {

    protected AssetManager assetManager;
    protected AppStateManager stateManager;
    protected InputManager inputManager;

    private EntityData mapEntityData;
    private MapLoaderAppState.MapEntityViewState mainMenuEntityViewState;
    private GameController gameController;

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);

        this.stateManager = stateManager;
        assetManager = app.getAssetManager();
        inputManager = app.getInputManager();
    }

    public Node loadMap(KwdFile kwdFile) {
        unloadMap();

        mapEntityData = new DefaultEntityData();

        Node mapNode = new Node("Map");
        gameController = new GameController(kwdFile, Collections.emptyList(), mapEntityData, kwdFile.getVariables(), new MapLoaderAppState.MapPlayerService());
        gameController.createNewGame();

        // Create the actual map
        MapViewController mapLoader = new MapViewController(assetManager, kwdFile, gameController.getGameWorldController().getMapController(), Player.KEEPER1_ID) {

            @Override
            protected void updateProgress(float progress) {

            }

        };
        mapNode.attachChild(mapLoader.load(assetManager, kwdFile));

        mainMenuEntityViewState = new MapLoaderAppState.MapEntityViewState(kwdFile, assetManager, mapEntityData, Player.KEEPER1_ID, null, mapNode);
        stateManager.attach(mainMenuEntityViewState);

        return mapNode;
    }

    private void unloadMap() {
        if (mainMenuEntityViewState != null) {
            stateManager.detach(mainMenuEntityViewState);
            mainMenuEntityViewState = null;
        }
        if (gameController != null) {
            gameController.close();
            gameController = null;
        }
        if (mapEntityData != null) {
            mapEntityData.close();
            mapEntityData = null;
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();

        unloadMap();
    }

    private static class MapEntityViewState extends PlayerEntityViewState {

        public MapEntityViewState(KwdFile kwdFile, AssetManager assetManager, EntityData entityData, short playerId, TextParser textParser, Node rootNode) {
            super(kwdFile, assetManager, entityData, playerId, textParser, rootNode);

            setId("Map: " + kwdFile.getGameLevel().getName());
        }
    }

    private static class MapPlayerService implements PlayerService {

        public MapPlayerService() {
        }

        @Override
        public void setWidescreen(boolean enable, short playerId) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void playSpeech(int speechId, boolean showText, boolean introduction, int pathId, short playerId) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isInTransition() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void doTransition(short pathId, Vector3f start, short playerId) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void flashButton(TriggerAction.MakeType buttonType, short targetId, TriggerAction.ButtonType targetButtonType, boolean enabled, int time, short playerId) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void rotateViewAroundPoint(Vector3f point, boolean relative, int angle, int time, short playerId) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void showMessage(int textId, short playerId) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void zoomViewToPoint(Vector3f point, short playerId) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void zoomViewToEntity(EntityId entityId, short playerId) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setGamePaused(boolean paused) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void showUnitFlower(EntityId entityId, int interval, short playerId) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

}
