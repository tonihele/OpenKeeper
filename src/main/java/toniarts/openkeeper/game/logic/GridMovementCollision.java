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

import com.jme3.math.Vector3f;
import java.util.Objects;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Sweeps a creature's movement across the map grid and slides it along blocked
 * axes. The same resolver is used for AI route following and possessed input.
 */
public final class GridMovementCollision {

    private static final int MAX_SWEEP_STEPS = 4096;

    private final IMapController mapController;
    private final IEntityPositionLookup entityPositionLookup;
    private final MovementSettings settings;

    public GridMovementCollision(IMapController mapController, IEntityPositionLookup entityPositionLookup,
            MovementSettings settings) {
        this.mapController = mapController;
        this.entityPositionLookup = entityPositionLookup;
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    /**
     * Applies a displacement into {@code result}, preserving its vertical
     * coordinate. Large steps are subdivided so a fast creature cannot skip a
     * blocked tile between checks.
     */
    public void move(INavigable navigable, float startX, float startZ, float deltaX, float deltaZ,
            Vector3f result) {
        if (navigable == null || mapController == null || mapController.getMapData() == null) {
            result.x = startX + deltaX;
            result.z = startZ + deltaZ;
            return;
        }

        float largestAxisDelta = Math.max(Math.abs(deltaX), Math.abs(deltaZ));
        int steps = Math.max(1, (int) Math.ceil(largestAxisDelta / settings.collisionStep()));
        if (steps > MAX_SWEEP_STEPS) {
            result.x = startX;
            result.z = startZ;
            return;
        }

        float stepX = deltaX / steps;
        float stepZ = deltaZ / steps;
        float x = startX;
        float z = startZ;
        for (int i = 0; i < steps; i++) {
            float proposedX = x + stepX;
            x = limitAxis(navigable, x, z, proposedX, true);

            float proposedZ = z + stepZ;
            z = limitAxis(navigable, x, z, proposedZ, false);
        }

        result.x = x;
        result.z = z;
    }

    private float limitAxis(INavigable navigable, float fromX, float fromZ, float toValue, boolean xAxis) {
        float fromValue = xAxis ? fromX : fromZ;
        if (toValue == fromValue) {
            return toValue;
        }

        Point fromPoint = WorldUtils.vectorToPoint(fromX, fromZ);
        int direction = toValue > fromValue ? 1 : -1;
        Point aheadPoint = xAxis ? new Point(fromPoint.x + direction, fromPoint.y)
                : new Point(fromPoint.x, fromPoint.y + direction);
        Point proposedPoint = xAxis ? WorldUtils.vectorToPoint(toValue, fromZ)
                : WorldUtils.vectorToPoint(fromX, toValue);
        if (xAxis ? proposedPoint.x == fromPoint.x : proposedPoint.y == fromPoint.y) {
            return toValue;
        }

        IMapTileInformation from = mapController.getMapData().getTile(fromPoint);
        IMapTileInformation ahead = mapController.getMapData().getTile(aheadPoint);
        boolean blocked = from == null || ahead == null
                || navigable.getCost(from, ahead, mapController, entityPositionLookup) == null;
        if (!blocked) {
            return toValue;
        }

        float wallFace = (xAxis ? fromPoint.x : fromPoint.y) * WorldUtils.TILE_WIDTH
                + direction * (WorldUtils.TILE_WIDTH / 2f);
        return wallFace - direction * settings.wallApproachGap();
    }
}
