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
package toniarts.openkeeper.game.controller.creature;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.controller.room.FiveByFiveRotatedController;
import toniarts.openkeeper.utils.Point;

/**
 * A Dungeon Heart as a target of an attack. The heart stands on the centre 3x3
 * tiles of its room, which no one can walk on, so it is hit from the ring of
 * tiles around them.
 */
public final class DungeonHeartTarget {

    /**
     * How far from the centre of the heart the ring of tiles is
     */
    private static final int RING_DISTANCE = 2;

    private DungeonHeartTarget() {
        // Nope
    }

    /**
     * Is the entity the heart object of a Dungeon Heart
     *
     * @param entityData the entity data
     * @param entity the entity
     * @return true if the entity is a Dungeon Heart
     */
    public static boolean isDungeonHeart(EntityData entityData, EntityId entity) {
        ObjectComponent objectComponent = entityData.getComponent(entity, ObjectComponent.class);
        return objectComponent != null && objectComponent.objectId == FiveByFiveRotatedController.OBJECT_HEART_ID;
    }

    /**
     * Can a creature standing on the tile hit the heart
     *
     * @param tile the tile the creature stands on
     * @param heartTile the tile at the centre of the heart
     * @return true if the heart is in reach
     */
    public static boolean isInReach(Point tile, Point heartTile) {
        return getTileDistance(tile, heartTile) <= RING_DISTANCE;
    }

    /**
     * Finds the tile next to the heart nearest to the given tile that a
     * creature can get to
     *
     * @param from where the creature is
     * @param heartTile the tile at the centre of the heart
     * @param isReachable tells whether the creature can get to the tile
     * @return the tile to attack the heart from, or {@code null} if there is
     * none
     */
    public static Point getAttackTile(Point from, Point heartTile, Predicate<Point> isReachable) {
        List<Point> ring = new ArrayList<>();
        for (int x = -RING_DISTANCE; x <= RING_DISTANCE; x++) {
            for (int y = -RING_DISTANCE; y <= RING_DISTANCE; y++) {
                if (Math.max(Math.abs(x), Math.abs(y)) == RING_DISTANCE) {
                    ring.add(new Point(heartTile.x + x, heartTile.y + y));
                }
            }
        }
        ring.sort(Comparator.comparingDouble(tile -> tile.distance(from)));
        for (Point tile : ring) {
            if (isReachable.test(tile)) {
                return tile;
            }
        }

        return null;
    }

    private static int getTileDistance(Point a, Point b) {
        return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
    }

}
