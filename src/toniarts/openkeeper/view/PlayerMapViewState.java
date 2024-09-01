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
import com.simsilica.es.EntityData;
import toniarts.openkeeper.utils.Point;
import java.lang.System.Logger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.listener.MapListener;
import toniarts.openkeeper.game.listener.PlayerActionListener;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IRoomsInformation;
import toniarts.openkeeper.game.map.MapInformation;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.modelviewer.Debug;
import toniarts.openkeeper.view.map.FlashTileViewState;
import toniarts.openkeeper.view.map.MapRoomContainer;
import toniarts.openkeeper.view.map.MapTileContainer;
import toniarts.openkeeper.view.map.MapViewController;
import toniarts.openkeeper.world.effect.EffectManagerState;
import toniarts.openkeeper.world.listener.RoomListener;
import toniarts.openkeeper.world.listener.TileChangeListener;

/**
 * Handles the handling of game world for a player, visually
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class PlayerMapViewState extends AbstractAppState implements MapListener, PlayerActionListener {
    
    private static final Logger logger = System.getLogger(PlayerMapViewState.class.getName());

    private Main app;
    private AppStateManager stateManager;
    private final IMapInformation mapInformation;
    private final MapViewController mapLoader;
    private final KwdFile kwdFile;
    private final AssetManager assetManager;
    private final MapTileContainer mapTileContainer;
    private Node worldNode;
    private final EffectManagerState effectManager;
    private List<TileChangeListener> tileChangeListener;
    private Map<Short, List<RoomListener>> roomListeners;
    private final FlashTileViewState flashTileControl;
    private final MapRoomContainer mapRoomContainer;

    public PlayerMapViewState(Main app, final KwdFile kwdFile, final AssetManager assetManager, Collection<Keeper> players, EntityData entityData, short playerId, ILoadCompleteNotifier loadCompleteNotifier) {
        this.app = app;
        this.kwdFile = kwdFile;
        this.assetManager = assetManager;

        // World node
        worldNode = new Node("World");
        if (Main.isDebug()) {
            Debug.showNodeAxes(assetManager, worldNode, 10);
        }

        // Load and update rooms
        mapRoomContainer = new MapRoomContainer(entityData, kwdFile);

        // Make sure we load the whole map before we continue
        mapTileContainer = new MapTileContainer(entityData, kwdFile, this::updateTiles) {

            @Override
            protected void onLoadComplete() {

                // Don't block the caller, might be called from the render thread...
                Thread mapLoaderThread = new Thread(() -> {

                    Spatial map = mapLoader.load(assetManager, kwdFile);
                    app.enqueue(() -> {
                        worldNode.attachChild(map);

                        loadCompleteNotifier.onLoadComplete();
                    });
                }, "GameClientMapLoader");
                mapLoaderThread.start();
            }

        };

        mapInformation = new MapInformation(mapTileContainer, kwdFile, players);

        // Effect manager
        effectManager = new EffectManagerState(kwdFile, assetManager);

        // Create the actual map
        mapLoader = new MapViewController(assetManager, kwdFile, mapInformation, playerId) {

            @Override
            protected void updateProgress(float progress) {
                PlayerMapViewState.this.updateProgress(progress);
            }

        };

        flashTileControl = new FlashTileViewState(mapLoader);

        // Start collecting the map entities
        mapRoomContainer.start();
        mapTileContainer.start();
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        this.stateManager = stateManager;

        // Effects
        this.stateManager.attach(effectManager);

        // Tile flash state
        this.stateManager.attach(flashTileControl);

        // Attach the world
        this.app.getRootNode().attachChild(worldNode);
    }

    @Override
    public void cleanup() {

        // Detach our map
        if (worldNode != null) {
            app.getRootNode().detachChild(worldNode);
            worldNode = null;
        }

        // Tile flash state
        stateManager.detach(flashTileControl);

        // Effects
        stateManager.detach(effectManager);

        // The actual map data
        mapRoomContainer.stop();
        mapTileContainer.stop();

        super.cleanup();
    }

    @Override
    public void update(float tpf) {

        // Always process rooms before the map tiles
        mapRoomContainer.update();
        mapTileContainer.update();
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }

    /**
     * If you want to monitor the map loading progress, use this method
     *
     * @param progress current progress from 0.0 to 1.0
     */
    protected abstract void updateProgress(final float progress);

    @Override
    public void onTilesChange(List<Point> updatedTiles) {
//        Point[] points = new Point[updatedTiles.size()];
//        for (int i = 0; i < updatedTiles.size(); i++) {
//            IMapTileInformation mapTile = updatedTiles.get(i);
//            points[i] = new Point(mapTile.getX(), mapTile.getY());
//        }
//
//        // FIXME: See in what thread we are, perhaps even do everything ready, just the attaching in render thread
//        app.enqueue(() -> {
//            mapLoader.updateTiles(points);
//        });
    }

    @Override
    public void onBuild(short keeperId, List<Point> tiles) {
//        mapClientService.setTiles(tiles);
//        Point[] updatableTiles = new Point[tiles.size()];
//        for (int i = 0; i < tiles.size(); i++) {
//            updatableTiles[i] = tiles.get(i).getLocation();
//        }
//
//        // FIXME: See in what thread we are, perhaps even do everything ready, just the attaching in render thread
//        app.enqueue(() -> {
//            mapLoader.updateTiles(updatableTiles);
//        });
    }

    @Override
    public void onSold(short keeperId, List<Point> tiles) {

        // For now there is no difference between buying and selling
//        onBuild(keeperId, tiles);
    }

    @Override
    public void onTileFlash(List<Point> points, boolean enabled, short keeperId) {
        flashTileControl.attach(points, enabled);
    }

    public IMapInformation getMapInformation() {
        return mapInformation;
    }

    private void updateTiles(Point[] points) {
        mapLoader.updateTiles(points);
    }

    public IRoomsInformation getRoomsInformation() {
        return mapRoomContainer;
    }

    public interface ILoadCompleteNotifier {

        void onLoadComplete();

    }

}
