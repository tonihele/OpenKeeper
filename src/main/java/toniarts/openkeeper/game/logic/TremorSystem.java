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

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import toniarts.openkeeper.game.component.Tremor;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;

/**
 * Shakes the ground under the active tremors (the Tremor keeper spell). Every
 * tick each tremor picks a random open tile in its area and damages the
 * reinforced walls next to it. Walls of every keeper shake, the caster's own
 * too, only neutral ones are spared.
 */
public final class TremorSystem implements IGameLogicUpdatable {

    /**
     * Largest tremor radius, in tiles
     */
    public static final int MAX_RADIUS = 5;

    /**
     * How many tremors can shake at once, a new one replaces the oldest
     */
    public static final int MAX_TREMORS = 8;

    /**
     * The tiles around a tile, clockwise from the north, and the tile itself
     * last
     */
    private static final int[][] AROUND = {
        {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, 0}
    };

    private final EntityData entityData;
    private final IGameTimer gameTimer;
    private final IMapController mapController;
    private final int damagePerTile;
    private final Random random;
    private EntitySet tremors;

    /**
     * Creates the tremor system
     *
     * @param entityData the entity data
     * @param gameTimer the game timer
     * @param mapController the map controller
     * @param damagePerTile the level variable
     * {@code TREMOR_DAMAGE_PER_TERRAIN_TILE_PER_SECOND}
     */
    public TremorSystem(EntityData entityData, IGameTimer gameTimer, IMapController mapController, int damagePerTile) {
        this(entityData, gameTimer, mapController, damagePerTile, new Random());
    }

    TremorSystem(EntityData entityData, IGameTimer gameTimer, IMapController mapController, int damagePerTile, Random random) {
        this.entityData = entityData;
        this.gameTimer = gameTimer;
        this.mapController = mapController;
        this.damagePerTile = damagePerTile;
        this.random = random;
    }

    @Override
    public void processTick(float tpf) {
        tremors.applyChanges();

        List<Entity> active = new ArrayList<>(tremors);
        active.removeIf(entity -> {
            if (entity.get(Tremor.class).endTime <= gameTimer.getGameTime()) {
                entityData.removeEntity(entity.getId());
                return true;
            }
            return false;
        });

        // Only so many tremors at once, the newest ones win
        if (active.size() > MAX_TREMORS) {
            active.sort(Comparator.comparingDouble(entity -> entity.get(Tremor.class).startTime));
            while (active.size() > MAX_TREMORS) {
                entityData.removeEntity(active.remove(0).getId());
            }
        }

        for (Entity entity : active) {
            shake(entity.get(Tremor.class));
        }
    }

    private void shake(Tremor tremor) {
        Point epicentre = findEpicentre(tremor.centre, Math.min(tremor.radius, MAX_RADIUS));
        if (epicentre == null) {
            return;
        }

        // The walls around take damage. In DK II the creatures on the open tiles
        // around also run away from the tremor, that is not done here yet
        for (int[] offset : AROUND) {
            Point p = new Point(epicentre.x + offset[0], epicentre.y + offset[1]);
            IMapTileInformation tile = mapController.getMapData().getTile(p);
            if (tile == null || !mapController.isSolid(p)) {
                continue;
            }
            Terrain terrain = mapController.getTerrain(tile);
            if (isWall(terrain) && tile.getOwnerId() != Player.NEUTRAL_PLAYER_ID) {
                mapController.applyTerrainDamage(p, getDamage());
            }
        }
    }

    /**
     * Picks a random tile in the square around the centre, and takes the
     * first open tile around it
     *
     * @param centre the tremor centre
     * @param radius the tremor radius
     * @return the epicentre, {@code null} if there are no open tiles there
     */
    private Point findEpicentre(Point centre, int radius) {
        int size = radius * 2 + 1;
        int pick = random.nextInt(size * size);
        int x = centre.x + pick % size - size / 2;
        int y = centre.y + pick / size - size / 2;
        for (int[] offset : AROUND) {
            Point p = new Point(x + offset[0], y + offset[1]);
            if (mapController.getMapData().getTile(p) != null && !mapController.isSolid(p)) {
                return p;
            }
        }

        return null;
    }

    private static boolean isWall(Terrain terrain) {
        return terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)
                && terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE);
    }

    private int getDamage() {
        int half = damagePerTile / 2;
        if (half <= 0) {
            return half;
        }

        return half + random.nextInt(half);
    }

    @Override
    public void start() {
        tremors = entityData.getEntities(Tremor.class);
    }

    @Override
    public void stop() {
        tremors.release();
        tremors = null;
    }

}
