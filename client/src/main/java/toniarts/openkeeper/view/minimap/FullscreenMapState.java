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
import com.jme3.asset.AssetNotFoundException;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import com.jme3.math.Vector3f;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Console;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.elements.Element;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.cinematics.Cinematic;
import toniarts.openkeeper.game.data.Settings.Setting;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.map.IRoomInformation;
import toniarts.openkeeper.game.map.IRoomsInformation;
import toniarts.openkeeper.game.map.MapColourClassifier;
import toniarts.openkeeper.game.map.MapColourGrid;
import toniarts.openkeeper.game.map.PlayerNumbers;
import toniarts.openkeeper.game.state.IPlayerScreenController;
import toniarts.openkeeper.game.state.PlayerState;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.PlayerCameraState;
import toniarts.openkeeper.view.PlayerInteractionState;
import toniarts.openkeeper.view.PossessionCameraState;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/**
 * The full-screen map. The game keeps running while it is open. A left click
 * on a tile moves the main camera there and closes the map; the map key,
 * Escape and a right click close it without moving the camera. Closing
 * always turns the camera back to north.
 *
 * <p>
 * While open the HUD is replaced by the empty Nifty screen, the player's
 * camera and interaction states are disabled and the 3D world is not drawn.
 */
public final class FullscreenMapState extends AbstractAppState {

    private static final Logger LOGGER = System.getLogger(FullscreenMapState.class.getName());

    /**
     * How often the raster is rebuilt while open
     */
    private static final float REBUILD_INTERVAL = 0.15f;

    private final Main app;
    private final MapColourGrid grid;
    private final IMapDataInformation<? extends IMapTileInformation> mapData;
    private final IFogOfWarInformation fogOfWar;
    private final EntityData entityData;
    private final IRoomsInformation<? extends IRoomInformation> roomsInformation;
    private final IKwdFile kwdFile;
    private final Collection<Keeper> keepers;
    private final Function<Keeper, String> playerName;
    private final short neutralPlayerNumber;

    private AppStateManager stateManager;
    private MinimapAssets assets;
    private FullscreenMapView view;
    private FullscreenMapPreview preview;
    private FullscreenMapOverlay overlay;
    private RawInputListener inputListener;
    private final byte[] rockKeyBgr = FullscreenMapView.rockKeyBgr();
    private final byte[] rasterBgr =new byte[MinimapRasteriser.RASTER_SIZE * MinimapRasteriser.RASTER_SIZE * 3];

    private MapLayout layout;
    private boolean open;
    // Opened or closed during this frame's input. Several listeners see the
    // same click (e.g. the minimap's right click opens, ours would close).
    private boolean changedThisFrame;
    private float timeSinceLastRebuild;
    private int rotationPhase;

    public FullscreenMapState(Main app, IMapInformation<? extends IMapTileInformation> mapInformation,
            IFogOfWarInformation fogOfWarInformation, IRoomsInformation<? extends IRoomInformation> roomsInformation,
            EntityData entityData, IKwdFile kwdFile, Collection<Keeper> keepers, Function<Keeper, String> playerName) {
        this.app = app;
        this.mapData = mapInformation.getMapData();
        this.fogOfWar = fogOfWarInformation;
        this.entityData = entityData;
        this.roomsInformation = roomsInformation;
        this.kwdFile = kwdFile;
        this.keepers = keepers;
        this.playerName = playerName;
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
            LOGGER.log(Level.ERROR, "Failed to load minimap assets, the full-screen map will not be available", e);
            return;
        }

        preview = new FullscreenMapPreview(app.getRenderManager(), app.getRootNode());
        try {
            view = new FullscreenMapView(app.getAssetManager(), app.getGuiNode(), preview.getTexture());
        } catch (AssetNotFoundException e) {
            LOGGER.log(Level.ERROR, "Full-screen map images are missing, the full-screen map will not be available", e);
            preview.dispose();
            preview = null;
            return;
        }
        overlay = new FullscreenMapOverlay(app.getAssetManager(), view.getOverlayNode(), entityData, kwdFile, mapData,
                fogOfWar, roomsInformation, assets.getPalette(), keepers, playerName);
        inputListener = new MapInputListener();
        app.getInputManager().addRawInputListener(inputListener);
    }

    /**
     * Opens the map, if the game is in plain HUD play
     */
    public void open() {
        if (open || changedThisFrame || view == null || !canOpen()) {
            return;
        }
        open = true;
        changedThisFrame = true;

        stateManager.getState(PlayerState.class).getScreen().goToScreen(IPlayerScreenController.SCREEN_EMPTY_ID);
        setPlayerStatesEnabled(false);
        app.getViewPort().setEnabled(false);

        rebuildRaster();
        updateLayout();
        view.attach();
    }

    /**
     * Closes the map and turns the main camera back to north.
     *
     * @param jumpTo the tile to centre the camera on, or {@code null} to
     * leave the camera where it was
     */
    public void close(Point jumpTo) {
        if (!open) {
            return;
        }
        open = false;
        changedThisFrame = true;

        view.detach();
        preview.stop();
        app.getViewPort().setEnabled(true);
        setPlayerStatesEnabled(true);

        PlayerCameraState cameraState = stateManager.getState(PlayerCameraState.class);
        if (jumpTo != null) {
            cameraState.setCameraLookAt(new Vector3f(jumpTo.x + 0.5f, 0, jumpTo.y + 0.5f)); // keeps the height
        }
        cameraState.getCamera().resetYawToNorth();

        stateManager.getState(PlayerState.class).getScreen().goToScreen(IPlayerScreenController.SCREEN_HUD_ID);
    }

    private boolean canOpen() {
        PlayerState playerState = stateManager.getState(PlayerState.class);
        PossessionCameraState possession = stateManager.getState(PossessionCameraState.class);
        Nifty nifty = app.getNifty();
        return playerState != null && !playerState.isPaused()
                && stateManager.getState(Cinematic.class) == null
                && (possession == null || !possession.isEnabled())
                && nifty.getCurrentScreen() != null
                && IPlayerScreenController.SCREEN_HUD_ID.equals(nifty.getCurrentScreen().getScreenId());
    }

    private void setPlayerStatesEnabled(boolean enabled) {
        stateManager.getState(PlayerCameraState.class).setEnabled(enabled);
        stateManager.getState(PlayerInteractionState.class).setEnabled(enabled);
    }

    @Override
    public void update(float tpf) {
        changedThisFrame = false;
        if (!open) {
            return;
        }

        updateLayout();
        Vector2f cursor = app.getInputManager().getCursorPosition();
        Vector2f hoverTile = layout.screenToTile(cursor.x, layout.getScreenHeight() - cursor.y, grid.getWidth(), grid.getHeight());
        // Unlike the tile picture, the 3D preview shows any tile, explored or not (the world draws unexplored ground as plain rock)
        preview.lookAt(hoverTile, grid.getWidth(), grid.getHeight());
        view.setPreviewVisible(hoverTile != null);
        overlay.update(layout, hoverTile, rotationPhase);
        timeSinceLastRebuild += tpf;
        if (timeSinceLastRebuild >= REBUILD_INTERVAL) {
            rebuildRaster();
        }
    }

    private void updateLayout() {
        layout = new MapLayout(app.getCamera().getWidth(), app.getCamera().getHeight());
        view.updateLayout(layout);
        preview.setAspect(layout.getPreview().w() / layout.getPreview().h());
    }

    private void rebuildRaster() {
        timeSinceLastRebuild = 0f;
        rotationPhase++;
        grid.recomputeRect(0, 0, grid.getWidth(), grid.getHeight());
        MinimapRasteriser.rebuildFitMode(grid, assets.getPalette(), rockKeyBgr, neutralPlayerNumber,
                rotationPhase, rasterBgr);
        view.updateRaster(rasterBgr);
    }

    /**
     * @return {@code true} if a text field has the keyboard focus, so typing
     * (e.g. in the console) doesn't trigger the map key
     */
    private boolean isTyping() {
        Nifty nifty = app.getNifty();
        Element focus = nifty.getCurrentScreen() != null
                ? nifty.getCurrentScreen().getFocusHandler().getKeyboardFocusElement() : null;
        return focus != null && (focus.getNiftyControl(TextField.class) != null
                || focus.getNiftyControl(Console.class) != null);
    }

    @Override
    public void cleanup() {
        if (open) {
            // The other states may already be gone, so only undo what we changed ourselves
            open = false;
            view.detach();
            app.getViewPort().setEnabled(true);
        }
        if (preview != null) {
            preview.dispose();
        }
        if (overlay != null) {
            overlay.dispose();
        }
        if (inputListener != null) {
            app.getInputManager().removeRawInputListener(inputListener);
        }
        super.cleanup();
    }

    /**
     * Raw jME input, like {@link MinimapPanelState} uses: bottom-left origin,
     * where {@link MapLayout} is top-left.
     */
    private final class MapInputListener implements RawInputListener {

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
        }

        @Override
        public void onMouseButtonEvent(MouseButtonEvent evt) {
            if (!open || changedThisFrame || !evt.isReleased()) {
                return;
            }
            if (evt.getButtonIndex() == MouseInput.BUTTON_RIGHT) {
                close(null);
            } else if (evt.getButtonIndex() == MouseInput.BUTTON_LEFT) {
                Point tile = layout.pickTile(evt.getX(), layout.getScreenHeight() - evt.getY(), grid.getWidth(), grid.getHeight());
                if (tile != null) {
                    close(tile);
                }
            }
        }

        @Override
        public void onKeyEvent(KeyInputEvent evt) {
            if (!evt.isPressed() || evt.isRepeating() || changedThisFrame) {
                return;
            }
            boolean mapKey = evt.getKeyCode() == Main.getUserSettings().getInteger(Setting.MAP);
            if (open && (mapKey || evt.getKeyCode() == KeyInput.KEY_ESCAPE)) {
                close(null);
            } else if (!open && mapKey && !isTyping()) {
                open();
            }
        }

        @Override
        public void onTouchEvent(TouchEvent evt) {
        }

    }

}
