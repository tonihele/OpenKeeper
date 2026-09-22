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
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
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
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.PlayerCamera;
import toniarts.openkeeper.view.PlayerCameraState;
import toniarts.openkeeper.view.PlayerInteractionState;
import toniarts.openkeeper.view.PlayerInteractionState.InteractionState.Type;
import toniarts.openkeeper.view.PossessionCameraState;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/**
 * Owns the panel minimap's live raster (minimap_jmonkey.md Steps 4-8):
 * builds the colour-class grid, periodically rasterises fit-mode or zoomed
 * geometry depending on the current {@link #zoom} level, rotates the
 * disc's UVs every frame to track the camera's yaw, positions the
 * fit-mode frustum overlay, paints the marker overlay
 */
public final class MinimapPanelState extends AbstractAppState {

    private static final Logger LOGGER = System.getLogger(MinimapPanelState.class.getName());

    /**
     * How often the raster is rebuilt
     */
    private static final float REBUILD_INTERVAL = 0.15f;

    /**
     * How often {@link #blinkParity} flips
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
    private int neutralRotationPhase = 0;

    // The panel's current on-screen rectangle in jME's own bottom-left
    // origin space, refreshed every frame by updateLayoutFromHud() -
    // MinimapView.updateLayout is given the same numbers, so the overlay's
    // 0..1 local space and this rectangle always agree.
    private float panelJmeX;
    private float panelJmeY;
    private float panelWidth;
    private float panelHeight;
    private boolean panelLayoutKnown;
    private boolean leftButtonDown;

    private RawInputListener inputListener;

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

        inputListener = new MinimapInputListener();
        app.getInputManager().addRawInputListener(inputListener);

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
     * "Resize Map" button, left click.
     */
    public void zoomIn() {
        setZoom(Math.min(zoom + 1, MAX_ZOOM));
    }

    /**
     * "Resize Map" button, right click.
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

        neutralRotationPhase++;
        if (zoom == MIN_ZOOM) {
            timeSinceLastFitRebuild = 0f;
            MinimapRasteriser.rebuildFitMode(grid, assets.getPalette(), assets.rockTextureBgr(),
                    neutralPlayerNumber, neutralRotationPhase, rasterBgr);
        } else {
            timeSinceLastZoomedRebuild = 0f;
            MinimapRasteriser.rebuildZoomedMode(grid, assets.getPalette(), assets.rockTextureBgr(),
                    neutralPlayerNumber, cameraTileX, cameraTileY, zoom, neutralRotationPhase, rasterBgr);
        }

        markerPainter.update();
        markerPainter.paint(rasterBgr, grid.getWidth(), grid.getHeight(), zoom, cameraTileX, cameraTileY,
                fogOfWarInformation, blinkParity, dashPhase, neutralRotationPhase,
                localKeeper.getDungeonHeartLocation(), dungeonHeartReportingDistanceTiles);

        view.updateRaster(rasterBgr);
    }

    /**
     * Rewrites the disc's UVs, and the north indicator's orbit position,
     * for the current camera yaw - every frame, not tied to the raster
     * rebuild cadence, since the camera can turn between rebuilds.
     */
    private void updateYaw() {
        PlayerCamera camera = getPlayerCamera();
        if (camera == null) {
            return;
        }
        float yaw = MinimapCoordinates.cameraYawRadians(camera.getCamera());
        view.updateYaw(yaw);
        view.updateNorthIndicator(yaw);
    }

    private PlayerCamera getPlayerCamera() {
        PlayerCameraState cameraState = stateManager.getState(PlayerCameraState.class);
        return cameraState != null ? cameraState.getCamera() : null;
    }

    /**
     * Fit mode only, and only when the camera isn't in the possession/
     * first-person mode design
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
        Element mapImageElement = (nifty != null && nifty.getCurrentScreen() != null)
                ? nifty.getCurrentScreen().findElementById(MAP_IMAGE_ELEMENT_ID) : null;
        if (mapImageElement == null) {
            // Not on the HUD screen right now (e.g. widescreen/cinematic
            // mode switches to a different Nifty screen entirely) - hide
            // rather than leaving the overlay floating at its last known
            // position over whatever's showing instead.
            if (panelLayoutKnown) {
                view.hide();
                panelLayoutKnown = false;
            }
            return;
        }

        int niftyX = mapImageElement.getX();
        int niftyY = mapImageElement.getY();
        int niftyWidth = mapImageElement.getWidth();
        int niftyHeight = mapImageElement.getHeight();
        int screenHeight = app.getCamera().getHeight();
        view.updateLayout(niftyX, niftyY, niftyWidth, niftyHeight, screenHeight);

        // Same conversion as MinimapView.updateLayout, kept in sync here so
        // click handling agrees with what's actually on screen.
        panelJmeX = niftyX;
        panelJmeY = screenHeight - niftyY - niftyHeight;
        panelWidth = niftyWidth;
        panelHeight = niftyHeight;
        panelLayoutKnown = true;
    }

    /**
     * Click -&gt; tile resolution and camera jump. Returns {@code true}
     * if the click landed on the panel and resolved to a tile
     * (so drag-scroll can keep following the cursor), {@code false}
     * otherwise.
     */
    private boolean jumpCameraToClickedTile(float mouseXJme, float mouseYJme) {
        if (!panelLayoutKnown || panelWidth <= 0 || panelHeight <= 0) {
            return false;
        }
        float localX = (mouseXJme - panelJmeX) / panelWidth;
        float localY = (mouseYJme - panelJmeY) / panelHeight;
        if (localX < 0f || localX > 1f || localY < 0f || localY > 1f) {
            return false; // outside the panel - not ours to handle
        }

        PlayerCamera camera = getPlayerCamera();
        float yaw = camera != null ? MinimapCoordinates.cameraYawRadians(camera.getCamera()) : 0f;
        float[] pixel = MinimapClickResolver.panelLocalToRasterPixel(localX, localY, yaw);

        boolean fit = zoom == MIN_ZOOM;
        MinimapRasteriser.FitGeometry fitGeometry = fit ? MinimapRasteriser.FitGeometry.of(grid.getWidth(), grid.getHeight()) : null;
        int pixelsPerTile = fit ? 0 : (1 << zoom);
        Vector2f cameraTile = camera != null ? MinimapCoordinates.worldToTile(camera.getLookAt()) : null;
        float cameraTileX = cameraTile != null ? cameraTile.x : 0f;
        float cameraTileY = cameraTile != null ? cameraTile.y : 0f;

        Point tile = MinimapClickResolver.resolveTile(pixel[0], pixel[1], fit, fitGeometry,
                cameraTileX, cameraTileY, pixelsPerTile, grid.getWidth(), grid.getHeight());
        if (tile == null) {
            return true; // on the panel, just not a resolvable map tile (e.g. the rock border)
        }

        PlayerCameraState cameraState = stateManager.getState(PlayerCameraState.class);
        if (cameraState != null) {
            cameraState.setCameraLookAt(tile);
        }
        return true;
    }

    /**
     * Right click: the panel's generic "cancel current tool" action, plus a full recompute.
     * {@code world.setMapScrollY(1)}
     */
    private void cancelToolAndRecompute() {
        PlayerInteractionState interactionState = stateManager.getState(PlayerInteractionState.class);
        if (interactionState != null) {
            interactionState.setInteractionState(Type.NONE, 0);
        }
        grid.recomputeRect(0, 0, grid.getWidth(), grid.getHeight());
        rebuildRaster();
    }

    @Override
    public void cleanup() {
        if (inputListener != null) {
            app.getInputManager().removeRawInputListener(inputListener);
        }
        if (view != null) {
            view.detach();
        }
        if (markerPainter != null) {
            markerPainter.dispose();
        }
        super.cleanup();
    }

    /**
     * Raw jME mouse input, the same pattern
     * {@code PlayerInteractionState.MapInteractionInputListener} already
     * uses (Nifty's own {@code <interact>} click events don't hand back
     * per-pixel mouse position, which this needs for the click-to-tile
     * math) - bypassing Nifty's own input path the same way this feature
     * already bypasses its render path (see {@link MinimapView}'s own
     * javadoc).
     */
    private final class MinimapInputListener implements RawInputListener {

        @Override
        public void beginInput() {
        }

        @Override
        public void endInput() {
        }

        @Override
        public void onJoyAxisEvent(JoyAxisEvent evt) {
        }

        @Override
        public void onJoyButtonEvent(JoyButtonEvent evt) {
        }

        @Override
        public void onMouseMotionEvent(MouseMotionEvent evt) {
            if (leftButtonDown) {
                // drag-to-scroll - keep resolving on every move while held.
                jumpCameraToClickedTile(evt.getX(), evt.getY());
            }
        }

        @Override
        public void onMouseButtonEvent(MouseButtonEvent evt) {
            if (evt.getButtonIndex() == MouseInput.BUTTON_LEFT) {
                if (evt.isPressed()) {
                    leftButtonDown = jumpCameraToClickedTile(evt.getX(), evt.getY());
                } else if (evt.isReleased()) {
                    leftButtonDown = false;
                }
            } else if (evt.getButtonIndex() == MouseInput.BUTTON_RIGHT && evt.isReleased()) {
                if (panelLayoutKnown) {
                    float localX = (evt.getX() - panelJmeX) / panelWidth;
                    float localY = (evt.getY() - panelJmeY) / panelHeight;
                    if (localX >= 0f && localX <= 1f && localY >= 0f && localY <= 1f) {
                        cancelToolAndRecompute();
                    }
                }
            }
        }

        @Override
        public void onKeyEvent(KeyInputEvent evt) {
        }

        @Override
        public void onTouchEvent(TouchEvent evt) {
        }

    }

}
