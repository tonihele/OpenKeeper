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
package toniarts.openkeeper.view;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * The tiles a room can't be built on, because a door, a trap or an obstacle
 * stands there. It follows the changes of the client's entity sets, so asking
 * about a tile is a map lookup instead of going through every entity.
 */
final class ConstructionBlockingTiles implements Predicate<Point> {

    private final Map<EntityId, Point> tilesByEntity = new HashMap<>();
    private final Map<Point, Integer> blockersByTile = new HashMap<>();

    /**
     * Takes in all the entities of a set, for a set that has just been created
     *
     * @param entities the entities
     * @param isBlocking whether an entity of the set blocks building
     */
    void addAll(EntitySet entities, Predicate<Entity> isBlocking) {
        for (Entity entity : entities) {
            add(entity, isBlocking);
        }
    }

    /**
     * Takes in what changed in a set with its last {@code applyChanges()}
     *
     * @param entities the entities
     * @param isBlocking whether an entity of the set blocks building
     */
    void applyChanges(EntitySet entities, Predicate<Entity> isBlocking) {
        for (Entity entity : entities.getRemovedEntities()) {
            remove(entity.getId());
        }
        for (Entity entity : entities.getAddedEntities()) {
            add(entity, isBlocking);
        }
        for (Entity entity : entities.getChangedEntities()) {
            add(entity, isBlocking);
        }
    }

    @Override
    public boolean test(Point tile) {
        return blockersByTile.containsKey(tile);
    }

    private void add(Entity entity, Predicate<Entity> isBlocking) {

        // An entity that moved leaves its old tile
        remove(entity.getId());

        Position position = entity.get(Position.class);
        if (position == null || !isBlocking.test(entity)) {
            return;
        }
        Point tile = WorldUtils.vectorToPoint(position.position);
        tilesByEntity.put(entity.getId(), tile);
        blockersByTile.merge(tile, 1, Integer::sum);
    }

    private void remove(EntityId entityId) {
        Point tile = tilesByEntity.remove(entityId);
        if (tile != null) {
            blockersByTile.computeIfPresent(tile, (point, count) -> count > 1 ? count - 1 : null);
        }
    }

}
