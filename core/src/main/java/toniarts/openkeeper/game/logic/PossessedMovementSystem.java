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
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Moves possessed creatures according to the movement intent sent by their
 * keeper. Replaces the steering of {@link MovementSystem}, which does not
 * handle possessed creatures since they have no navigation.
 */
public final class PossessedMovementSystem implements IGameLogicUpdatable {

    /**
     * How close a possessed creature is allowed to approach a wall or door
     * it cannot pass through. Creatures stop at the centre of their own
     * tile, i.e. half a tile short of the obstacle, instead of walking
     * right up against it.
     */
    private static final float WALL_APPROACH_GAP = WorldUtils.TILE_WIDTH / 6f;

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
                INavigable navigable = creaturesController.createController(entity.getId());
                float x = newPosition.x + movement.direction.x * movement.speed * tpf;
                float z = newPosition.z + movement.direction.y * movement.speed * tpf;

                // Slide along walls by trying the axes separately, this way we
                // only ever cross into an orthogonally adjacent tile
                newPosition.x = limitApproach(navigable, newPosition.x, newPosition.z, x, true);
                newPosition.z = limitApproach(navigable, newPosition.x, newPosition.z, z, false);
            }

            if (!newPosition.equals(position.position) || movement.rotation != position.rotation) {
                entityData.setComponent(entity.getId(), new Position(movement.rotation, newPosition));
            }
        }
    }

    /**
     * Uses the same rules as path finding (solid terrain, doors, room
     * obstacles, water & lava abilities) to limit movement along one axis.
     * If the tile ahead, in the direction of travel, is not passable the
     * returned value is clamped so that the entity stops
     * {@link #WALL_APPROACH_GAP} short of it, i.e. at the centre of its own
     * tile, rather than walking right up against the wall or door.
     */
    private float limitApproach(INavigable navigable, float fromX, float fromZ, float toValue, boolean isX) {
        float fromValue = isX ? fromX : fromZ;
        if (toValue == fromValue) {
            return toValue;
        }

        Point fromPoint = WorldUtils.vectorToPoint(fromX, fromZ);
        int direction = toValue > fromValue ? 1 : -1;
        Point aheadPoint = isX ? new Point(fromPoint.x + direction, fromPoint.y)
                : new Point(fromPoint.x, fromPoint.y + direction);

        IMapTileInformation from = mapController.getMapData().getTile(fromPoint);
        IMapTileInformation ahead = mapController.getMapData().getTile(aheadPoint);
        boolean blocked = ahead == null || navigable.getCost(from, ahead, mapController, entityPositionLookup) == null;
        if (!blocked) {
            return toValue;
        }

        float wallFace = (isX ? fromPoint.x : fromPoint.y) * WorldUtils.TILE_WIDTH + direction * (WorldUtils.TILE_WIDTH / 2f);
        float limit = wallFace - direction * WALL_APPROACH_GAP;
        return direction > 0 ? Math.min(toValue, limit) : Math.max(toValue, limit);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        possessedEntities.release();
    }

}
