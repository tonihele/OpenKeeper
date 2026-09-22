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
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/**
 * Owns the panel minimap's live raster (minimap_jmonkey.md Step 4): builds
 * the colour-class grid, periodically rasterises fit-mode geometry, and
 * keeps a {@link MinimapView} overlay positioned over the GameHUD's map
 * panel element in place of the static placeholder image that used to sit
 * there.
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

    private static final String MAP_IMAGE_ELEMENT_ID = "minimapImage";

    private final Main app;
    private final MapColourGrid grid;
    private final short neutralPlayerNumber;

    private MinimapAssets assets;
    private MinimapView view;
    private byte[] rasterBgr;
    private float timeSinceLastRebuild = Float.MAX_VALUE; // force a rebuild on the first update

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

        timeSinceLastRebuild += tpf;
        if (timeSinceLastRebuild >= REBUILD_INTERVAL) {
            timeSinceLastRebuild = 0f;
            grid.recomputeRect(0, 0, grid.getWidth(), grid.getHeight());
            rebuildRaster();
        }
    }

    private void rebuildRaster() {
        MinimapRasteriser.rebuildFitMode(grid, assets.getPalette(), assets.rockTextureBgr(), neutralPlayerNumber, rasterBgr);
        view.updateRaster(rasterBgr);
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
