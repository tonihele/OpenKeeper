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
 * tiles around them. Like in the original, the attackers go for the three Arch
 * Pillars of the heart and the hits land on the heart.
 */
public final class DungeonHeartTarget {

    /**
     * How far from the centre of the heart the ring of tiles is
     */
    private static final int RING_DISTANCE = 2;

    /**
     * Where the Arch Pillars stand from the centre of the heart, the original
     * puts them on the room tiles 7, 16 and 18 (0x4DB700)
     */
    private static final Vector2f[] PILLARS = {new Vector2f(0, -1.2f), new Vector2f(-1.1f, 0.6f), new Vector2f(1.1f, 0.6f)};

    /**
     * How much further than its melee range a creature reaches an object, like
     * the pillars (0x45CB00)
     */
    private static final float OBJECT_REACH = 0.5f;

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
     * Can a creature standing here hit the heart. It has to be within its melee
     * range and half a tile of a pillar (0x45CB00, 0x45E8F0).
     *
     * @param position where the creature stands
     * @param heartTile the tile at the centre of the heart
     * @param meleeRange the melee range of the creature
     * @return true if the heart is in reach
     */
    public static boolean isInReach(Vector2f position, Point heartTile, float meleeRange) {
        float reach = meleeRange + OBJECT_REACH;
        for (Vector2f pillar : PILLARS) {
            if (position.distanceSquared(heartTile.x * WorldUtils.TILE_WIDTH + pillar.x,
                    heartTile.y * WorldUtils.TILE_WIDTH + pillar.y) <= reach * reach) {
                return true;
            }
        }

        return false;
    }

    /**
     * Finds the tile next to the heart nearest to the given tile that a
     * creature can get to and hit the heart from its centre
     *
     * @param from where the creature is
     * @param heartTile the tile at the centre of the heart
     * @param meleeRange the melee range of the creature
     * @param isReachable tells whether the creature can get to the tile
     * @return the tile to attack the heart from, or {@code null} if there is
     * none
     */
    public static Point getAttackTile(Point from, Point heartTile, float meleeRange, Predicate<Point> isReachable) {
        List<Point> ring = new ArrayList<>();
        for (int x = -RING_DISTANCE; x <= RING_DISTANCE; x++) {
            for (int y = -RING_DISTANCE; y <= RING_DISTANCE; y++) {
                Point tile = new Point(heartTile.x + x, heartTile.y + y);
                if (Math.max(Math.abs(x), Math.abs(y)) == RING_DISTANCE
                        && isInReach(WorldUtils.pointToVector2f(tile), heartTile, meleeRange)) {
                    ring.add(tile);
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

}
