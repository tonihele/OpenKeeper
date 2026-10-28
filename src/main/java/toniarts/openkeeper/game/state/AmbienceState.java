/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.game.state;

import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.sound.AmbienceDriver;
import toniarts.openkeeper.game.sound.SoundEngine;
import toniarts.openkeeper.game.sound.SoundManager;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.PlayerMapViewState;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;
import toniarts.openkeeper.view.map.MapViewController;
import toniarts.openkeeper.view.map.TorchPlacement;

/**
 * The background sounds of a level: the ambience bed, the distant noises, the
 * beds of the rooms and the terrain around the camera and the crackle of the
 * nearest torch. This state only looks at the map, what is played is up to the
 * {@link AmbienceDriver}.
 */
public final class AmbienceState extends AbstractPauseAwareState {

    private static final int GRID_RADIUS = 5;
    private static final int GRID_SIZE = GRID_RADIUS * 2 + 1;
    private static final int TORCH_RADIUS = 1;

    /**
     * The map is not scanned every frame, the camera does not move that fast.
     */
    private static final long SCAN_INTERVAL_MS = 250;

    private final IKwdFile kwdFile;
    private final PlayerMapViewState mapViewState;
    private final String[] cells = new String[GRID_SIZE * GRID_SIZE];

    @Nullable
    private AmbienceDriver driver;
    @Nullable
    private SoundEngine driverEngine;
    @Nullable
    private Point scannedAround;
    @Nullable
    private Point nearestTorch;
    private long lastScan;
    private volatile boolean possessing;
    private final IntSupplier musicIntensity;

    public AmbienceState(IKwdFile kwdFile, PlayerMapViewState mapViewState) {
        this(kwdFile, mapViewState, () -> 0);
    }

    /**
     * @param musicIntensity the current music intensity, the ambience follows it
     */
    public AmbienceState(IKwdFile kwdFile, PlayerMapViewState mapViewState, IntSupplier musicIntensity) {
        this.kwdFile = kwdFile;
        this.mapViewState = mapViewState;
        this.musicIntensity = musicIntensity;
    }

    @Override
    public boolean isPauseable() {
        return true;
    }

    /**
     * While the player possesses a creature, the terrain has a first person
     * sound, i.e. the lava is heard from nearby instead of from afar.
     */
    public void setPossessing(boolean possessing) {
        this.possessing = possessing;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            stopDriver();
        }
    }

    @Override
    public void cleanup() {
        stopDriver();
        super.cleanup();
    }

    @Override
    public void update(float tpf) {
        SoundEngine engine = SoundManager.getEngine();
        if (!isEnabled() || engine == null || !mapViewState.isMapLoaded()) {
            return;
        }

        // The engine is replaced if the sound system is restarted
        if (driver == null || driverEngine != engine) {
            stopDriver();
            driverEngine = engine;
            driver = new AmbienceDriver(engine, SoundManager::loadCategory, musicIntensity);
        }

        long now = System.nanoTime() / 1_000_000L;
        Point listener = engine.getListener();
        if (scannedAround == null || !scannedAround.equals(listener) || now - lastScan >= SCAN_INTERVAL_MS) {
            scan(listener);
            scannedAround = listener;
            lastScan = now;
        }
        driver.update(now, cells, listener, nearestTorch);
    }

    private void stopDriver() {
        if (driver != null) {
            driver.stop();
        }
        driver = null;
        driverEngine = null;
        scannedAround = null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void scan(Point listener) {
        IMapDataInformation<IMapTileInformation> data = mapViewState.getMapInformation().getMapData();
        IFogOfWarInformation fogOfWar = mapViewState.getFogOfWarInformation();
        boolean firstPerson = possessing;

        for (int dy = -GRID_RADIUS; dy <= GRID_RADIUS; dy++) {
            for (int dx = -GRID_RADIUS; dx <= GRID_RADIUS; dx++) {
                Terrain terrain = getExploredTerrain(data, fogOfWar, listener.x + dx, listener.y + dy);
                cells[(dy + GRID_RADIUS) * GRID_SIZE + dx + GRID_RADIUS] = terrain == null ? null
                        : getSoundCategory(terrain, firstPerson);
            }
        }

        nearestTorch = findNearestTorch(data, fogOfWar, listener);
    }

    private static String getSoundCategory(Terrain terrain, boolean firstPerson) {
        String category = firstPerson ? terrain.getSoundCategoryFirstPerson() : null;
        return category == null || category.isEmpty() ? terrain.getSoundCategory() : category;
    }

    /**
     * @return the terrain of the tile, {@code null} if it is off the map or the
     * player has not seen it. What is hidden by the fog does not make a sound
     */
    @Nullable
    private Terrain getExploredTerrain(IMapDataInformation<IMapTileInformation> data, IFogOfWarInformation fogOfWar,
            int x, int y) {
        if (x < 0 || y < 0 || x >= data.getWidth() || y >= data.getHeight()) {
            return null;
        }
        if (!fogOfWar.isExplored(new Point(x, y))) {
            return null;
        }
        IMapTileInformation tile = data.getTile(x, y);
        return tile == null ? null : kwdFile.getTerrain(tile.getTerrainId());
    }

    /**
     * Torches are in the walls, but are looked for through the floor tile that
     * lights them, like the map does when it puts them up.
     */
    @Nullable
    private Point findNearestTorch(IMapDataInformation<IMapTileInformation> data, IFogOfWarInformation fogOfWar,
            Point listener) {
        Point nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int dy = -TORCH_RADIUS; dy <= TORCH_RADIUS; dy++) {
            for (int dx = -TORCH_RADIUS; dx <= TORCH_RADIUS; dx++) {
                Point p = new Point(listener.x + dx, listener.y + dy);
                double distance = listener.distance(p);
                if (distance < nearestDistance && hasTorch(data, fogOfWar, p)) {
                    nearest = p;
                    nearestDistance = distance;
                }
            }
        }
        return nearest;
    }

    private boolean hasTorch(IMapDataInformation<IMapTileInformation> data, IFogOfWarInformation fogOfWar, Point p) {
        Terrain floor = getExploredTerrain(data, fogOfWar, p.x, p.y);
        if (floor == null || floor.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            return false;
        }
        for (Point wall : TorchPlacement.getTorchWallCandidates(p.x, p.y)) {
            Terrain wallTerrain = getExploredTerrain(data, fogOfWar, wall.x, wall.y);
            if (wallTerrain == null || !wallTerrain.getFlags().contains(Terrain.TerrainFlag.TORCH)) {
                continue;
            }
            // a room can do without its torches
            if (floor.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
                Room room = kwdFile.getRoomByTerrain(floor.getTerrainId());
                return room != null && room.getTorch() != null;
            }
            return true;
        }
        return false;
    }
}
