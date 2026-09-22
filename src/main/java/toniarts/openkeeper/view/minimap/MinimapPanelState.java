/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.view.minimap;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector2f;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.elements.Element;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.map.IRoomInformation;
import toniarts.openkeeper.game.map.IRoomsInformation;
import toniarts.openkeeper.game.map.MapColourClassifier;
import toniarts.openkeeper.game.map.MapColourGrid;
import toniarts.openkeeper.game.map.PlayerNumbers;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.view.PlayerCamera;
import toniarts.openkeeper.view.PlayerCameraState;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/**
 * Owns the panel minimap's live raster (minimap_jmonkey.md Steps 4/5/6):
 * builds the colour-class grid, periodically rasterises fit-mode or zoomed
 * geometry depending on the current {@link #zoom} level, rotates the
 * octagon's UVs every frame to track the camera's yaw, and keeps a
 * {@link MinimapView} overlay positioned over the GameHUD's map panel
 * element in place of the static placeholder image that used to sit there.
 *
 * <p>
 * No frustum overlay, no markers, no click input yet - see
 * minimap_jmonkey.md's step ordering for what those later steps add. The
 * rebuild is a brute-force full {@code recomputeRect} on a fixed interval
 * rather than fine-grained per-mutation invalidation (design §3.4's
 * intended eager model) - correct, just not yet wired to the tile-mutation
 * call sites minimap_jmonkey.md Step 2 identified
 * ({@code PlayerMapViewState.onTilesChange}/
 * {@code addFogOfWarTilesDirtyListener}); that's a follow-up optimisation,
 * not required for these steps' "get pixels on screen" goal.
 */
public final class MinimapPanelState extends AbstractAppState {

    private static final Logger LOGGER = System.getLogger(MinimapPanelState.class.getName());

    /**
     * How often the raster is rebuilt. No authoritative source value exists
     * for this in the original engine (minimap_jmonkey.md §0) - matches
     * {@code FogOfWarController.VISION_UPDATE_INTERVAL}'s own precedent for
     * an approximated, unbacked cadence constant.
     */
    private static final float REBUILD_INTERVAL = 0.15f;

    private static final int MIN_ZOOM = -1; // fit
    private static final int MAX_ZOOM = 4; // 16px/tile

    private static final String MAP_IMAGE_ELEMENT_ID = "minimapImage";

    private final Main app;
    private final MapColourGrid grid;
    private final short neutralPlayerNumber;

    private AppStateManager stateManager;
    private MinimapAssets assets;
    private MinimapView view;
    private byte[] rasterBgr;

    private int zoom = MIN_ZOOM;
    // design §5.1/§5.3: the original keeps one throttle per mode, so
    // switching zoom modes doesn't skip a legitimately due rebuild of
    // whichever mode you switch back to.
    private float timeSinceLastFitRebuild = Float.MAX_VALUE;
    private float timeSinceLastZoomedRebuild = Float.MAX_VALUE;

    public MinimapPanelState(Main app, IMapInformation<? extends IMapTileInformation> mapInformation,
            IFogOfWarInformation fogOfWarInformation, IRoomsInformation<? extends IRoomInformation> roomsInformation) {
        this.app = app;
        MapColourClassifier classifier = new MapColourClassifier(mapInformation, fogOfWarInformation, roomsInformation);
        this.grid = new MapColourGrid(mapInformation.getMapData().getWidth(), mapInformation.getMapData().getHeight(),
                classifier, fogOfWarInformation);
        this.neutralPlayerNumber = PlayerNumbers.playerNumber(Player.NEUTRAL_PLAYER_ID);
    }

    @Override
    public void initialize(AppStateManager stateManager, Application application) {
        super.initialize(stateManager, application);
        this.stateManager = stateManager;

        try {
            assets = MinimapAssets.load();
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, "Failed to load minimap assets, the panel minimap will not be shown", e);
            return;
        }

        rasterBgr = new byte[MinimapRasteriser.RASTER_SIZE * MinimapRasteriser.RASTER_SIZE * 3];
        view = new MinimapView(app.getAssetManager(), app.getGuiNode());
        view.attach();

        grid.recomputeRect(0, 0, grid.getWidth(), grid.getHeight());
        rebuildRaster();
    }

    @Override
    public void update(float tpf) {
        if (view == null) {
            return;
        }

        updateLayoutFromHud();
        updateYaw();

        timeSinceLastFitRebuild += tpf;
        timeSinceLastZoomedRebuild += tpf;
        boolean due = zoom == MIN_ZOOM ? timeSinceLastFitRebuild >= REBUILD_INTERVAL : timeSinceLastZoomedRebuild >= REBUILD_INTERVAL;
        if (due) {
            grid.recomputeRect(0, 0, grid.getWidth(), grid.getHeight());
            rebuildRaster();
        }
    }

    /**
     * "Resize Map" button, left click (design §5.2).
     */
    public void zoomIn() {
        setZoom(Math.min(zoom + 1, MAX_ZOOM));
    }

    /**
     * "Resize Map" button, right click (design §5.2).
     */
    public void zoomOut() {
        setZoom(Math.max(zoom - 1, MIN_ZOOM));
    }

    private void setZoom(int newZoom) {
        if (newZoom == zoom) {
            return;
        }
        zoom = newZoom;
        if (zoom == 0) {
            // design §5.2: landing exactly on 0 also calls the inert
            // world.setMapScrollX(0) - never read anywhere, in this engine
            // or the original, so there is nothing to actually call here;
            // recomputeRect below is the only observable part of this rule.
            grid.recomputeRect(0, 0, grid.getWidth(), grid.getHeight());
        }
        // Rebuild immediately rather than waiting for the next throttle
        // tick, so the zoom button feels responsive.
        rebuildRaster();
    }

    private void rebuildRaster() {
        if (zoom == MIN_ZOOM) {
            timeSinceLastFitRebuild = 0f;
            MinimapRasteriser.rebuildFitMode(grid, assets.getPalette(), assets.rockTextureBgr(), neutralPlayerNumber, rasterBgr);
        } else {
            timeSinceLastZoomedRebuild = 0f;
            PlayerCamera camera = getPlayerCamera();
            Vector2f cameraTile = camera != null ? MinimapCoordinates.worldToTile(camera.getLookAt()) : null;
            float cameraTileX = cameraTile != null ? cameraTile.x : 0f;
            float cameraTileY = cameraTile != null ? cameraTile.y : 0f;
            MinimapRasteriser.rebuildZoomedMode(grid, assets.getPalette(), assets.rockTextureBgr(),
                    neutralPlayerNumber, cameraTileX, cameraTileY, zoom, rasterBgr);
        }
        view.updateRaster(rasterBgr);
    }

    /**
     * Rewrites the octagon's UVs for the current camera yaw (design §5.7)
     * - every frame, not tied to the raster rebuild cadence, since the
     * camera can turn between rebuilds.
     */
    private void updateYaw() {
        PlayerCamera camera = getPlayerCamera();
        if (camera == null) {
            return;
        }
        view.updateYaw(MinimapCoordinates.cameraYawRadians(camera.getCamera()));
    }

    private PlayerCamera getPlayerCamera() {
        PlayerCameraState cameraState = stateManager.getState(PlayerCameraState.class);
        return cameraState != null ? cameraState.getCamera() : null;
    }

    private void updateLayoutFromHud() {
        Nifty nifty = app.getNifty();
        if (nifty == null || nifty.getCurrentScreen() == null) {
            return;
        }
        Element mapImageElement = nifty.getCurrentScreen().findElementById(MAP_IMAGE_ELEMENT_ID);
        if (mapImageElement == null) {
            return;
        }

        int screenHeight = app.getCamera().getHeight();
        view.updateLayout(mapImageElement.getX(), mapImageElement.getY(),
                mapImageElement.getWidth(), mapImageElement.getHeight(), screenHeight);
    }

    @Override
    public void cleanup() {
        if (view != null) {
            view.detach();
        }
        super.cleanup();
    }

}
