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
package toniarts.openkeeper.view.map;

import com.jme3.math.Vector3f;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.map.WallSection.WallDirection;

/**
 * Where wall torches, and the lights that go with them, are placed. Pure
 * functions of the tile coordinates and terrain data.
 */
public final class TorchPlacement {

    private static final List<WallDirection> NO_TORCH_DIRECTIONS = List.of();
    private static final List<WallDirection> HORIZONTAL_TORCH_DIRECTIONS = List.of(WallDirection.NORTH, WallDirection.SOUTH);
    private static final List<WallDirection> VERTICAL_TORCH_DIRECTIONS = List.of(WallDirection.WEST, WallDirection.EAST);
    private static final List<WallDirection> ALL_TORCH_DIRECTIONS = List.of(WallDirection.NORTH, WallDirection.WEST,
            WallDirection.SOUTH, WallDirection.EAST);

    private TorchPlacement() {
    }

    static List<WallDirection> getTorchDirections(int x, int y) {
        boolean horizontal = x % 2 != 0;
        boolean vertical = y % 2 == 0;

        if (horizontal) {
            return vertical ? ALL_TORCH_DIRECTIONS : HORIZONTAL_TORCH_DIRECTIONS;
        }
        return vertical ? VERTICAL_TORCH_DIRECTIONS : NO_TORCH_DIRECTIONS;
    }

    /**
     * Where a torch light goes: inside the floor tile {@code floor}, next to
     * the wall face it hangs on. The offsets are given in the floor tile's
     * own space (origin in its north-west corner, y growing south): wall to
     * the north (0.5, 0.125), east
     * (0.875, 0.5), south (0.5, 0.875), west (0.125, 0.5). A tile is centred
     * on whole world coordinates here, so the corner is half a tile back.
     *
     * @param floor the floor tile the torch stands in
     * @param direction which side of {@code floor} the wall is on
     * @param height the light height
     * @return the light position in world space
     */
    static Vector3f getTorchLightPosition(Point floor, WallDirection direction, float height) {
        float u;
        float v;
        switch (direction) {
            case NORTH -> {
                u = 0.5f;
                v = 0.125f;
            }
            case EAST -> {
                u = 0.875f;
                v = 0.5f;
            }
            case SOUTH -> {
                u = 0.5f;
                v = 0.875f;
            }
            case WEST -> {
                u = 0.125f;
                v = 0.5f;
            }
            default -> throw new IllegalStateException("Unexpected torch direction: " + direction);
        }
        return new Vector3f(floor.x - 0.5f + u, height, floor.y - 0.5f + v);
    }

    /**
     * The tiles that may carry a torch for the floor tile, in the order the
     * torch is looked for. The tile has a torch if one of these is a torch wall.
     *
     * @return the neighbouring tiles, empty if the floor tile has no torch
     */
    public static List<Point> getTorchWallCandidates(int x, int y) {
        List<Point> result = new ArrayList<>(4);
        for (WallDirection direction : getTorchDirections(x, y)) {
            result.add(switch (direction) {
                case NORTH -> new Point(x, y - 1);
                case WEST -> new Point(x - 1, y);
                case SOUTH -> new Point(x, y + 1);
                case EAST -> new Point(x + 1, y);
            });
        }
        return result;
    }

    /**
     * A {@code TORCH} terrain still opts out when it declares
     * both a side and a top resource, unless it is impenetrable (no torches on
     * an ordinary diggable wall with full artwork). Invisible on stock data.
     */
    static boolean hasTorches(Terrain terrain) {
        EnumSet<Terrain.TerrainFlag> flags = terrain.getFlags();
        if (!flags.contains(Terrain.TerrainFlag.TORCH)) {
            return false;
        }
        if (flags.contains(Terrain.TerrainFlag.IMPENETRABLE)) {
            return true;
        }
        return !(hasResource(terrain.getSideResource()) && hasResource(terrain.getTopResource()));
    }

    private static boolean hasResource(ArtResource resource) {
        return resource != null && resource.getName() != null && !resource.getName().isEmpty();
    }
}
