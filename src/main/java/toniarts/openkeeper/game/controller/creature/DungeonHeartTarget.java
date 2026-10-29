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

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.controller.room.FiveByFiveRotatedController;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A Dungeon Heart as a target of an attack. The heart stands on the centre 3x3
 * tiles of its room, which no one can walk on, so it is hit from the ring of
 * tiles around them.<br>
 * In DK II an attacker doesn't go for the heart itself but for the nearest of
 * the three Arch Pillars of the heart's arches (object type 94, 0x45CB00), so
 * the attackers end up spread around the heart.
 */
public final class DungeonHeartTarget {

    /**
     * How far from the centre of the heart the ring of tiles is
     */
    private static final int RING_DISTANCE = 2;

    /**
     * Where the Arch Pillars stand, in tiles from the centre of the heart:
     * north, south-west and south-east. Read from DK II's memory on level 5.
     */
    private static final Vector2f[] PILLARS = {new Vector2f(0, -1.2f), new Vector2f(-1.1f, 0.6f), new Vector2f(1.1f, 0.6f)};

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
     * Finds the Arch Pillar of the heart nearest to the given position
     *
     * @param from the position, in world coordinates
     * @param heartTile the tile at the centre of the heart
     * @return the pillar's position, in world coordinates
     */
    public static Vector2f getNearestPillar(Vector2f from, Point heartTile) {
        Vector2f centre = WorldUtils.pointToVector2f(heartTile);
        Vector2f nearest = null;
        for (Vector2f offset : PILLARS) {
            Vector2f pillar = centre.add(offset.mult(WorldUtils.TILE_WIDTH));
            if (nearest == null || pillar.distanceSquared(from) < nearest.distanceSquared(from)) {
                nearest = pillar;
            }
        }

        return nearest;
    }

    /**
     * Can a creature standing on the tile hit the heart. It needs to be on the
     * ring and next to the pillar nearest to it.
     *
     * @param tile the tile the creature stands on
     * @param heartTile the tile at the centre of the heart
     * @return true if the heart is in reach
     */
    public static boolean isInReach(Point tile, Point heartTile) {
        return getTileDistance(tile, heartTile) == RING_DISTANCE
                && getTileDistance(tile, getPillarTile(getNearestPillar(WorldUtils.pointToVector2f(tile), heartTile))) <= 1;
    }

    /**
     * Finds the tile to hit the heart from: the nearest tile next to the pillar
     * nearest to the creature that it can get to, or if there is none, next to
     * the pillar after that
     *
     * @param from where the creature is
     * @param heartTile the tile at the centre of the heart
     * @param isReachable tells whether the creature can get to the tile
     * @return the tile to attack the heart from, or {@code null} if there is
     * none
     */
    public static Point getAttackTile(Point from, Point heartTile, Predicate<Point> isReachable) {
        Vector2f position = WorldUtils.pointToVector2f(from);
        List<Point> ring = new ArrayList<>();
        for (int x = -RING_DISTANCE; x <= RING_DISTANCE; x++) {
            for (int y = -RING_DISTANCE; y <= RING_DISTANCE; y++) {
                Point tile = new Point(heartTile.x + x, heartTile.y + y);
                if (isInReach(tile, heartTile)) {
                    ring.add(tile);
                }
            }
        }
        ring.sort(Comparator.comparingDouble((Point tile) -> getNearestPillar(WorldUtils.pointToVector2f(tile), heartTile).distanceSquared(position))
                .thenComparingDouble(tile -> tile.distance(from)));
        for (Point tile : ring) {
            if (isReachable.test(tile)) {
                return tile;
            }
        }

        return null;
    }

    private static Point getPillarTile(Vector2f pillar) {
        return WorldUtils.vectorToPoint(pillar);
    }

    private static int getTileDistance(Point a, Point b) {
        return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
    }

}
