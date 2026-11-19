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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.PossessedMovement;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;
import toniarts.openkeeper.game.navigation.pathfinding.MapIndexedGraph;
import toniarts.openkeeper.game.navigation.steering.CreatureSizing;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Moves possessed creatures according to the movement intent sent by their
 * keeper. Replaces the steering of {@link MovementSystem}, which does not
 * handle possessed creatures since they have no navigation.
 */
public final class PossessedMovementSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final IMapController mapController;
    private final IEntityPositionLookup entityPositionLookup;
    private final ICreaturesController creaturesController;
    private final EntitySet possessedEntities;

    public PossessedMovementSystem(EntityData entityData, IMapController mapController,
            IEntityPositionLookup entityPositionLookup, ICreaturesController creaturesController) {
        this.entityData = entityData;
        this.mapController = mapController;
        this.entityPositionLookup = entityPositionLookup;
        this.creaturesController = creaturesController;
        possessedEntities = entityData.getEntities(Possessed.class, PossessedMovement.class, Position.class);
    }

    @Override
    public void processTick(float tpf) {
        possessedEntities.applyChanges();

        for (Entity entity : possessedEntities) {
            PossessedMovement movement = entity.get(PossessedMovement.class);
            Position position = entity.get(Position.class);
            Vector3f newPosition = position.position.clone();
            if (!movement.direction.equals(Vector2f.ZERO)) {
                var navigable = creaturesController.createController(entity.getId());
                float width = CreatureSizing.getBodyWidth(navigable.getCreature());
                float x = newPosition.x + movement.direction.x * movement.speed * tpf;
                float z = newPosition.z + movement.direction.y * movement.speed * tpf;

                // Slide along walls by trying the axes separately, this way we
                // only ever cross into an orthogonally adjacent tile
                newPosition.x = limitApproach(navigable, newPosition.x, newPosition.z, x, true, width);
                newPosition.z = limitApproach(navigable, newPosition.x, newPosition.z, z, false, width);
            }

            if (!newPosition.equals(position.position) || movement.rotation != position.rotation) {
                entityData.setComponent(entity.getId(), new Position(movement.rotation, newPosition));
            }
        }
    }

    /** Sweep the full-width leading point against tile access, preserving manual sliding. */
    private float limitApproach(INavigable navigable, float fromX, float fromZ, float toValue, boolean isX, float width) {
        float fromValue = isX ? fromX : fromZ;
        if (toValue == fromValue) {
            return toValue;
        }
        Point tile = WorldUtils.vectorToPoint(fromX, fromZ);
        int direction = toValue > fromValue ? 1 : -1;
        while (true) {
            Point aheadPoint = isX ? new Point(tile.x + direction, tile.y)
                    : new Point(tile.x, tile.y + direction);
            IMapTileInformation from = mapController.getMapData().getTile(tile);
            IMapTileInformation ahead = mapController.getMapData().getTile(aheadPoint);
            float face = (isX ? tile.x : tile.y) * WorldUtils.TILE_WIDTH
                    + direction * WorldUtils.TILE_WIDTH / 2f;
            if (ahead == null || MapIndexedGraph.getMovementCost(navigable, from, ahead, mapController, entityPositionLookup) == null) {
                float limit = face - direction * width;
                // Only the approached face is clamped. Moving away retains
                // direct player control, including escape from a closing tile.
                toValue = direction > 0 ? Math.min(toValue, limit) : Math.max(toValue, limit);
                break;
            }
            if (direction > 0 ? toValue + width < face : toValue - width >= face) {
                break;
            }
            tile = aheadPoint;
        }
        return toValue;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        possessedEntities.release();
    }

}
