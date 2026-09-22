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
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.elements.Element;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.data.Keeper;
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
import toniarts.openkeeper.view.PossessionCameraState;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/**
 * Owns the panel minimap's live raster (minimap_jmonkey.md Steps 4/5/6):
 * builds the colour-class grid, periodically rasterises fit-mode or zoomed
 * geometry depending on the current {@link #zoom} level, rotates the
 * octagon's UVs every frame to track the camera's yaw, positions the
 * fit-mode frustum overlay, paints the marker overlay ({@link
 * MinimapMarkerPainter} - see its own javadoc for which rows are and
 * aren't implemented), and keeps a {@link MinimapView} overlay positioned
 * over the GameHUD's map panel element in place of the static placeholder
 * image that used to sit there.
 *
 * <p>
 * No click input yet - see
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

    /**
     * How often {@link #blinkParity} flips. No authoritative source value
     * exists for this either (see {@link #REBUILD_INTERVAL}'s own note) -
     * a plain guess at a readable blink rate.
     */
    private static final float BLINK_INTERVAL = 0.5f;

    private static final int MIN_ZOOM = -1; // fit
    private static final int MAX_ZOOM = 4; // 16px/tile

    private static final String MAP_IMAGE_ELEMENT_ID = "minimapImage";

    private final Main app;
    private final MapColourGrid grid;
    private final IFogOfWarInformation fogOfWarInformation;
    private final EntityData entityData;
    private final Keeper localKeeper;
    private final short neutralPlayerNumber;
    private final float dungeonHeartReportingDistanceTiles;

    private AppStateManager stateManager;
    private MinimapAssets assets;
    private MinimapView view;
    private MinimapMarkerPainter markerPainter;
    private byte[] rasterBgr;

    private int zoom = MIN_ZOOM;
    // the original keeps one throttle per mode, so
    // switching zoom modes doesn't skip a legitimately due rebuild of
    // whichever mode you switch back to.
    private float timeSinceLastFitRebuild = Float.MAX_VALUE;
    private float timeSinceLastZoomedRebuild = Float.MAX_VALUE;
    private float timeSinceLastBlink = 0f;
    private boolean blinkParity = true;
    private int dashPhase = 0;

    public MinimapPanelState(Main app, IMapInformation<? extends IMapTileInformation> mapInformation,
            IFogOfWarInformation fogOfWarInformation, IRoomsInformation<? extends IRoomInformation> roomsInformation,
            EntityData entityData, Keeper localKeeper, float dungeonHeartReportingDistanceTiles) {
        this.app = app;
        this.fogOfWarInformation = fogOfWarInformation;
        this.entityData = entityData;
        this.localKeeper = localKeeper;
        this.dungeonHeartReportingDistanceTiles = dungeonHeartReportingDistanceTiles;
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
        markerPainter = new MinimapMarkerPainter(entityData, assets.getPalette(), localKeeper.getId());

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
        updateFrustum();

        timeSinceLastBlink += tpf;
        if (timeSinceLastBlink >= BLINK_INTERVAL) {
            timeSinceLastBlink = 0f;
            blinkParity = !blinkParity;
            dashPhase++; // advances the heart-direction line's marching-dash animation
        }

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
            // landing exactly on 0 also calls the inert world.setMapScrollX(0)
            // recomputeRect below is the only observable part of this rule.
            grid.recomputeRect(0, 0, grid.getWidth(), grid.getHeight());
        }
        // Rebuild immediately rather than waiting for the next throttle
        // tick, so the zoom button feels responsive.
        rebuildRaster();
    }

    private void rebuildRaster() {
        PlayerCamera camera = getPlayerCamera();
        Vector2f cameraTile = camera != null ? MinimapCoordinates.worldToTile(camera.getLookAt()) : null;
        float cameraTileX = cameraTile != null ? cameraTile.x : 0f;
        float cameraTileY = cameraTile != null ? cameraTile.y : 0f;

        if (zoom == MIN_ZOOM) {
            timeSinceLastFitRebuild = 0f;
            MinimapRasteriser.rebuildFitMode(grid, assets.getPalette(), assets.rockTextureBgr(), neutralPlayerNumber, rasterBgr);
        } else {
            timeSinceLastZoomedRebuild = 0f;
            MinimapRasteriser.rebuildZoomedMode(grid, assets.getPalette(), assets.rockTextureBgr(),
                    neutralPlayerNumber, cameraTileX, cameraTileY, zoom, rasterBgr);
        }

        markerPainter.update();
        markerPainter.paint(rasterBgr, grid.getWidth(), grid.getHeight(), zoom, cameraTileX, cameraTileY,
                fogOfWarInformation, blinkParity, dashPhase, localKeeper.getDungeonHeartLocation(), dungeonHeartReportingDistanceTiles);

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

    /**
     * Fit mode only, and only when the camera isn't in the possession/
     * first-person mode design §2.6/§5.7 says hides the overlay -
     * {@code PossessionCameraState} is the closest match this codebase has
     * to that "camera mode" concept (minimap_jmonkey.md §0).
     */
    private void updateFrustum() {
        if (zoom != MIN_ZOOM) {
            view.hideFrustum();
            return;
        }
        PossessionCameraState possessionCameraState = stateManager.getState(PossessionCameraState.class);
        if (possessionCameraState != null && possessionCameraState.isEnabled()) {
            view.hideFrustum();
            return;
        }
        PlayerCamera camera = getPlayerCamera();
        if (camera == null) {
            view.hideFrustum();
            return;
        }
        Vector3f[] groundCorners = MinimapFrustum.groundCorners(camera.getCamera());
        view.updateFrustum(groundCorners, grid.getWidth(), grid.getHeight(),
                MinimapCoordinates.cameraYawRadians(camera.getCamera()));
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
        if (markerPainter != null) {
            markerPainter.dispose();
        }
        super.cleanup();
    }

}
