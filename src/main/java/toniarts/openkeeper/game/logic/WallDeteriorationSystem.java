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
package toniarts.openkeeper.game.logic;

import java.util.Collection;
import java.util.Random;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;

/**
 * Wears down the keepers' reinforced walls at random. Every tick each keeper
 * gets a number of random map tiles, and a tile that is one of its own walls
 * next to its own land takes damage, so the imps have walls to repair. The
 * original levels all have this switched off.
 */
public final class WallDeteriorationSystem implements IGameLogicUpdatable {

    /**
     * The tiles next to a tile
     */
    private static final int[][] NEIGHBOURS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    private final IMapController mapController;
    private final Collection<IPlayerController> players;
    private final int tilesPerTick;
    private final int damage;
    private final Random random;

    /**
     * Creates the wall deterioration system
     *
     * @param mapController the map controller
     * @param players the players
     * @param tilesPerTick the level variable
     * {@code NUMBER_OF_RANDOM_WALL_DETERIORATIONS_PER_SECOND}, DK II picks
     * this many tiles every tick
     * @param healthChange the level variable
     * {@code WALL_DETERIORATION_DAMAGE}, a change to the wall health so a
     * negative value damages
     */
    public WallDeteriorationSystem(IMapController mapController, Collection<IPlayerController> players,
            int tilesPerTick, int healthChange) {
        this(mapController, players, tilesPerTick, healthChange, new Random());
    }

    WallDeteriorationSystem(IMapController mapController, Collection<IPlayerController> players,
            int tilesPerTick, int healthChange, Random random) {
        this.mapController = mapController;
        this.players = players;
        this.tilesPerTick = tilesPerTick;
        this.damage = -healthChange;
        this.random = random;
    }

    @Override
    public void processTick(float tpf) {
        if (tilesPerTick <= 0 || damage <= 0) {
            return;
        }

        // The map edge is never picked
        int width = mapController.getMapData().getWidth() - 2;
        int height = mapController.getMapData().getHeight() - 2;
        for (IPlayerController player : players) {
            short playerId = player.getKeeper().getId();
            if (playerId < Player.KEEPER1_ID || player.getKeeper().isDestroyed()) {
                continue;
            }
            for (int i = 0; i < tilesPerTick; i++) {
                Point p = new Point(1 + random.nextInt(width), 1 + random.nextInt(height));
                if (isWallNextToOwnLand(p, playerId)) {
                    mapController.applyTerrainDamage(p, damage);
                }
            }
        }
    }

    private boolean isWallNextToOwnLand(Point p, short playerId) {
        IMapTileInformation tile = mapController.getMapData().getTile(p);
        Terrain terrain = mapController.getTerrain(tile);
        if (tile.getOwnerId() != playerId || terrain.getGoldValue() != 0
                || !terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)
                || terrain.getFlags().contains(Terrain.TerrainFlag.IMPENETRABLE)) {
            return false;
        }

        for (int[] offset : NEIGHBOURS) {
            IMapTileInformation neighbour = mapController.getMapData().getTile(p.x + offset[0], p.y + offset[1]);
            if (neighbour != null && neighbour.getOwnerId() == playerId) {
                Terrain neighbourTerrain = mapController.getTerrain(neighbour);
                if (!neighbourTerrain.getFlags().contains(Terrain.TerrainFlag.SOLID)
                        && neighbourTerrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

}
