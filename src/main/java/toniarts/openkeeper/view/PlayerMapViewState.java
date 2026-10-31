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
import com.jme3.math.ColorRGBA;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.listener.MapListener;
import toniarts.openkeeper.game.listener.PlayerActionListener;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.map.IRoomsInformation;
import toniarts.openkeeper.game.map.MapInformation;
import toniarts.openkeeper.game.sound.GameEventSoundPlayer;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.modelviewer.Debug;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.fogofwar.FogOfWarController;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;
import toniarts.openkeeper.view.map.FlashTileViewState;
import toniarts.openkeeper.view.map.MapRoomContainer;
import toniarts.openkeeper.view.map.MapTileContainer;
import toniarts.openkeeper.view.map.MapViewController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Handles the handling of game world for a player, visually
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class PlayerMapViewState extends AbstractAppState implements MapListener, PlayerActionListener {

    private Main app;
    private AppStateManager stateManager;
    private final IMapInformation mapInformation;
    private final MapViewController mapLoader;
    private final IKwdFile kwdFile;
    private final AssetManager assetManager;
    private final MapTileContainer mapTileContainer;
    private Node worldNode;
    private final EffectManagerState effectManager;
    private final FlashTileViewState flashTileControl;
    private final MapRoomContainer mapRoomContainer;
    private short[][] lastTerrainIds;
    private final FogOfWarController fogOfWarController;
    private final GameEventSoundPlayer gameEventSoundPlayer;

    // Creature vision (and the initial fog seeding) can produce tile updates
    // before MapViewController.load() has built its scene graph - queue those
    // and flush them once the map actually exists, instead of crashing
    private volatile boolean mapLoaded = false;
    private final ConcurrentLinkedQueue<Point> pendingTileUpdates = new ConcurrentLinkedQueue<>();
    private final Object tileUpdateLock = new Object();

    protected PlayerMapViewState(Main app, final IKwdFile kwdFile, final AssetManager assetManager, Collection<Keeper> players, EntityData entityData, short playerId, boolean ceilingEnabled, ILoadCompleteNotifier loadCompleteNotifier, Consumer<EntityId> enemySightedNotifier) {
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

                // Fog must be seeded before the initial geometry is built, since
                // the renderer consults it while assembling the map
                fogOfWarController.seedLevelStart();

                // Snapshot the initial terrain of every tile, so we can later detect
                // claim/repair transitions (terrain type changing to its max-health variant)
                lastTerrainIds = new short[getWidth()][getHeight()];
                for (int x = 0; x < getWidth(); x++) {
                    for (int y = 0; y < getHeight(); y++) {
                        lastTerrainIds[x][y] = getTile(x, y).getTerrainId();
                    }
                }

                // Don't block the caller, might be called from the render thread...
                Thread mapLoaderThread = new Thread(() -> {

                    Spatial map = mapLoader.load(assetManager, kwdFile);

                    // Apply any tile updates (fog reveals from the seeding above,
                    // or from creatures already ticking) that arrived too early to
                    // be applied directly, while the map wasn't attached yet
                    // Loop until the queue is empty *while holding the lock that
                    // updateTiles() also takes*, and flip the flag in that same
                    // critical section: otherwise an update arriving between the
                    // last drain and the flag would be queued and never applied.
                    // Only after the flip is mapLoader done being mutated off the
                    // render thread: update(tpf) reads its LightRules (registered
                    // terrain materials) every frame and gates on this flag.
                    while (true) {
                        List<Point> pending = new ArrayList<>();
                        synchronized (tileUpdateLock) {
                            Point p;
                            while ((p = pendingTileUpdates.poll()) != null) {
                                pending.add(p);
                            }
                            if (pending.isEmpty()) {
                                mapLoaded = true;
                                break;
                            }
                        }
                        mapLoader.updateTiles(pending.toArray(new Point[0]));
                    }

                    app.enqueue(() -> {
                        worldNode.attachChild(map);

                        loadCompleteNotifier.onLoadComplete();
                    });
                }, "GameClientMapLoader");
                mapLoaderThread.start();
            }

        };

        mapInformation = new MapInformation(mapTileContainer, kwdFile, players);

        fogOfWarController = new FogOfWarController(entityData, kwdFile, mapTileContainer, playerId,
                this::updateTiles, enemySightedNotifier);

        // Effect manager
        effectManager = new EffectManagerState(kwdFile, assetManager);

        // Create the actual map
        mapLoader = new MapViewController(assetManager, kwdFile, mapInformation, fogOfWarController, playerId,
                Main.getUserSettings().getBoolean(Settings.Setting.TORCHES_ENABLED), ceilingEnabled) {

            @Override
            protected void updateProgress(float progress) {
                PlayerMapViewState.this.updateProgress(progress);
            }

        };

        // The ceiling is only shown while possessing a creature
        mapLoader.setCeilingVisible(false);

        flashTileControl = new FlashTileViewState(mapLoader);

        gameEventSoundPlayer = new GameEventSoundPlayer(kwdFile);

        // Start collecting the map entities
        mapRoomContainer.start();
        mapTileContainer.start();
        fogOfWarController.start();
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        this.stateManager = stateManager;

        // Effects
        this.stateManager.attach(effectManager);

        // Sounds
        this.gameEventSoundPlayer.register();

        // Tile flash state
        this.stateManager.attach(flashTileControl);

        // Attach the world
        this.app.getRootNode().attachChild(worldNode);

        // Lighting: the cursor light lives as long as the world does
        mapLoader.getLightRules().createCursorLight();
    }

    @Override
    public void cleanup() {

        // Lighting
        mapLoader.getLightRules().destroyCursorLight();

        // Detach our map
        if (worldNode != null) {
            app.getRootNode().detachChild(worldNode);
            worldNode = null;
        }

        // Tile flash state
        stateManager.detach(flashTileControl);

        // Effects
        stateManager.detach(effectManager);

        // Sounds
        this.gameEventSoundPlayer.unregister();

        // The actual map data
        mapRoomContainer.stop();
        mapTileContainer.stop();
        fogOfWarController.stop();

        super.cleanup();
    }

    @Override
    public void update(float tpf) {

        // Always process rooms before the map tiles
        mapRoomContainer.update();
        mapTileContainer.update();
        fogOfWarController.update(tpf);

        // Lighting: pick up live settings changes, advance modulators
        // (pulse/fade/flicker) and re-upload the terrain materials they
        // touched. Skipped until the map is
        // done loading - the loader thread is still registering terrain
        // materials into the same LightRules up to that point, and reading
        // it here concurrently with that would race (ConcurrentModificationException).
        if (mapLoaded) {
            Settings settings = Main.getUserSettings();
            float ambient = settings.getFloat(Settings.Setting.AMBIENT_LIGHT);
            mapLoader.getLightRules().setGlobalAmbient(new ColorRGBA(ambient, ambient, ambient, 1f));
            mapLoader.getLightRules().setCheapLighting(settings.getBoolean(Settings.Setting.CHEAP_LIGHTING));
            mapLoader.getLightRules().update(tpf, app.getCamera());
        }
    }

    public MapViewController getMapLoader() {
        return mapLoader;
    }

    /**
     * {@code false} while the "GameClientMapLoader" background thread is
     * still populating {@code mapLoader}'s {@code LightRules} (registering
     * terrain materials, torches, etc.). Anything outside this class that
     * touches {@code LightRules} on the render thread (entity/effect flat
     * lighting) must gate on this the same way {@link #update(float)} above
     * does, or it races those writes with a {@code ConcurrentModificationException}.
     */
    public boolean isMapLoaded() {
        return mapLoaded;
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
        fogOfWarController.onTileOwnerChanged(updatedTiles);
    }

    @Override
    public void onBuild(short keeperId, List<Point> tiles) {
        fogOfWarController.onRoomBuilt(keeperId, tiles);
    }

    @Override
    public void onSold(short keeperId, List<Point> tiles) {

        // Not wired: by the time this fires the terrain has typically already
        // reverted to non-room, so the room's TileConstruction
    }

    @Override
    public void onTileFlash(List<Point> points, boolean enabled, short keeperId) {
        flashTileControl.attach(points, enabled);
    }

    @Override
    public void onFogOfWarDisabled(short keeperId) {
        fogOfWarController.disableFogOfWar();
    }

    @Override
    public void onFogOfWarReset(short keeperId) {
        fogOfWarController.resetToLevelStart();
    }

    @Override
    public void onTilesReveal(List<Point> points, boolean explore, short keeperId) {
        fogOfWarController.revealActionPointTiles(points, explore);
    }

    public IMapInformation getMapInformation() {
        return mapInformation;
    }

    public IFogOfWarInformation getFogOfWarInformation() {
        return fogOfWarController;
    }

    public void markPendingTaggedTiles(List<Point> points, boolean tagged) {
        fogOfWarController.markPendingTagged(points, tagged);
    }

    public void setCeilingVisible(boolean visible) {
        mapLoader.setCeilingVisible(visible);
    }

    public void setPossessedCreature(EntityId entityId) {
        fogOfWarController.setPossessedCreature(entityId);
    }

    public void addFogOfWarTilesDirtyListener(Consumer<Point[]> listener) {
        fogOfWarController.addTilesDirtyListener(listener);
    }

    private void updateTiles(Point[] points) {
        synchronized (tileUpdateLock) {
            if (!mapLoaded) {
                pendingTileUpdates.addAll(Arrays.asList(points));
                return;
            }
        }

        app.enqueue(() -> {
            for (Point point : points) {
                IMapTileInformation tile = mapInformation.getMapData().getTile(point);
                short newTerrainId = tile.getTerrainId();
                short oldTerrainId = lastTerrainIds[point.x][point.y];
                if (newTerrainId != oldTerrainId) {
                    Terrain oldTerrain = kwdFile.getTerrain(oldTerrainId);
                    if (newTerrainId == oldTerrain.getMaxHealthTypeTerrainId()) {
                        effectManager.load(worldNode,
                                WorldUtils.pointToVector3f(point).addLocal(0, WorldUtils.FLOOR_HEIGHT, 0),
                                oldTerrain.getMaxHealthEffectId(), false, tile.getOwnerId());
                    } else if (newTerrainId == oldTerrain.getDestroyedTypeTerrainId()) {
                        effectManager.load(worldNode,
                                WorldUtils.pointToVector3f(point).addLocal(0, WorldUtils.FLOOR_HEIGHT, 0),
                                oldTerrain.getDestroyedEffectId(), false);
                    }
                    lastTerrainIds[point.x][point.y] = newTerrainId;
                }
            }
            mapLoader.updateTiles(points);
        });
    }

    public IRoomsInformation getRoomsInformation() {
        return mapRoomContainer;
    }

    public interface ILoadCompleteNotifier {

        void onLoadComplete();

    }

}
