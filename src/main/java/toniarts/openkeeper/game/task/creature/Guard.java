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
package toniarts.openkeeper.game.task.creature;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import toniarts.openkeeper.game.component.GuardedRoom;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractCapacityCriticalRoomTask;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Stands guard in the Guard Room. As in DK II, the guard goes to the edge
 * tile of the room nearest to it and walks round the edge (DKII.EXE 0x462930,
 * 0x462D00). Every so often it walks to a piece of the room's furniture and
 * then to a random tile of the room, and takes up its round again (0x463190,
 * 0x4632A0). DK II sends it to the furniture's own position, and the solid
 * furniture stops it where the two touch. The guard remembers the room, DK II
 * sends it back there for its next guard duty (0x462850).
 */
public final class Guard extends AbstractCapacityCriticalRoomTask {

    /**
     * The least time between the guard's trips to the furniture, in logic
     * ticks
     */
    public static final int ROUND_TICKS = 75;
    /**
     * The least time a guard takes over the trip from the furniture to a room
     * tile, in logic ticks
     */
    public static final int TRIP_TICKS = 50;

    /**
     * The four directions a guard walks the edge in: north, east, south, west
     */
    private static final int[][] DIRECTIONS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    private enum Step {
        ROUND, FURNITURE, ROOM_TILE
    }

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final int ticksPerSecond;
    private final int maxExtraTicks;
    private final int noFurnitureExtraTicks;
    private final RandomGenerator random;
    private Point target;
    private Vector2f spotAtFurniture;
    private Step step = Step.ROUND;
    private int direction;
    private float tripTime = -1;

    /**
     * A guard duty in the Guard Room
     *
     * @param navigationService the navigation service
     * @param mapController the map controller
     * @param p the tile reserved for the guard in the room
     * @param playerId the owner of the room
     * @param room the Guard Room
     * @param taskManager the task manager
     * @param entityData the entity data
     * @param kwdFile the level data, for the widths of the furniture and the
     * guard
     * @param ticksPerSecond the logic ticks per second of the level
     * @param maxExtraTicks at most this many random ticks are added to the
     * time before a trip to the furniture and to the trip itself
     * @param noFurnitureExtraTicks at most this many random ticks are added
     * to the time before the next look for furniture when there was none
     * @param random the random number generator
     */
    public Guard(final INavigationService navigationService, final IMapController mapController, Point p, short playerId,
            IRoomController room, TaskManager taskManager, EntityData entityData, IKwdFile kwdFile, int ticksPerSecond,
            int maxExtraTicks, int noFurnitureExtraTicks, RandomGenerator random) {
        super(navigationService, mapController, p, playerId, room, taskManager);

        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.ticksPerSecond = ticksPerSecond;
        this.maxExtraTicks = maxExtraTicks;
        this.noFurnitureExtraTicks = noFurnitureExtraTicks;
        this.random = random;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        if (target == null) {
            target = getNearestEdgeTile(creature.getCreatureCoordinates());
        }
        if (step == Step.FURNITURE) {

            // Halfway from the middle of the furniture to the guard's spot: still on the furniture's tile, and near
            // enough to the spot to count as there once the guard stands on it
            return WorldUtils.pointToVector2f(target).interpolateLocal(spotAtFurniture, 0.5f);
        }
        return WorldUtils.pointToVector2f(target);
    }

    @Override
    public Vector2f getWorkPosition(ICreatureController creature) {
        return step == Step.FURNITURE ? spotAtFurniture : getTarget(creature);
    }

    @Override
    protected ObjectType getRoomObjectType() {
        return ObjectType.GUARD;
    }

    /**
     * Counts the guard in the room's used capacity from the moment it is sent.
     * Unlike the other rooms, DK II's capacity check (0x4E53B0) counts the
     * creatures on their way to a Guard Room. The guard remembers the room,
     * DK II sends it back there for its next guard duty (DKII.EXE 0x462850)
     *
     * @param creature the guard
     * @param setToCreature whether to set the task to the creature
     */
    @Override
    public void assign(ICreatureController creature, boolean setToCreature) {
        super.assign(creature, setToCreature);

        getRoomObjectControl().addItem(creature.getEntityId(), null);
        entityData.setComponent(creature.getEntityId(), new GuardedRoom(getRoom().getEntityId()));
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        switch (step) {
            case FURNITURE -> {

                // From the furniture to a random tile of the room (0x4632A0)
                step = Step.ROOM_TILE;
                tripTime = executionDuration + getTime(TRIP_TICKS, maxExtraTicks);
                List<Point> roomTiles = getRoomTiles();
                target = roomTiles.get(random.nextInt(roomTiles.size()));
            }
            case ROOM_TILE -> {

                // And back to the edge
                step = Step.ROUND;
                target = getNearestEdgeTile(target);
            }
            case ROUND -> {

                // The guard has just come to the edge
                if (tripTime < 0) {
                    tripTime = executionDuration + getTime(ROUND_TICKS, maxExtraTicks);
                }
                if (executionDuration >= tripTime) {
                    EntityId furniture = getRandomFurniture();
                    if (furniture != null) {
                        step = Step.FURNITURE;
                        Vector3f position = entityData.getComponent(furniture, Position.class).position;
                        target = WorldUtils.vectorToPoint(position);
                        spotAtFurniture = getSpotAtFurniture(creature, furniture, position);
                        return;
                    }
                    tripTime = executionDuration + getTime(ROUND_TICKS, noFurnitureExtraTicks);
                }
                target = getNextEdgeTile(target);
            }
        }
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.GUARD;
    }

    @Override
    public boolean isRemovable() {
        return true;
    }

    private List<Point> getRoomTiles() {
        return getRoom().getRoomInstance().getCoordinates();
    }

    private float getTime(int ticks, int maxExtra) {
        return (ticks + (maxExtra > 0 ? random.nextInt(maxExtra) : 0)) / (float) ticksPerSecond;
    }

    /**
     * A tile of the room is on its edge when one of the eight tiles around it
     * is not in the room
     */
    private static boolean isEdge(Set<Point> tiles, Point p) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if (!tiles.contains(new Point(p.x + x, p.y + y))) {
                    return true;
                }
            }
        }
        return false;
    }

    private Point getNearestEdgeTile(Point from) {
        Set<Point> tiles = new HashSet<>(getRoomTiles());
        Point nearest = getTaskLocation();
        int nearestDistance = Integer.MAX_VALUE;
        for (Point p : tiles) {
            int distance = WorldUtils.calculateDistance(from, p);
            if (distance < nearestDistance && isEdge(tiles, p)) {
                nearest = p;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    /**
     * Walks the edge from the given tile: keeping to the edge tiles, the guard
     * turns left if it can, else goes on, else turns right, else turns back.
     * It goes on to the end of the straight stretch, so that it doesn't stop
     * on every tile
     *
     * @param from the edge tile the guard is on
     * @return the edge tile to walk to
     */
    private Point getNextEdgeTile(Point from) {
        Set<Point> tiles = new HashSet<>(getRoomTiles());
        Point current = from;
        for (int steps = 0; steps < tiles.size(); steps++) {
            Point next = null;
            int nextDirection = direction;
            for (int turn : new int[]{3, 0, 1, 2}) {
                int d = (direction + turn) % DIRECTIONS.length;
                Point p = new Point(current.x + DIRECTIONS[d][0], current.y + DIRECTIONS[d][1]);
                if (tiles.contains(p) && isEdge(tiles, p)) {
                    next = p;
                    nextDirection = d;
                    break;
                }
            }

            // A turn ends the stretch, the guard walks on from there on its next step
            if (next == null || (steps > 0 && nextDirection != direction)) {
                break;
            }
            direction = nextDirection;
            current = next;
        }
        return current;
    }

    private EntityId getRandomFurniture() {
        List<EntityId> furniture = new ArrayList<>();
        for (EntityId entity : getRoom().getFloorFurniture()) {
            if (entityData.getComponent(entity, Position.class) != null) {
                furniture.add(entity);
            }
        }
        return furniture.isEmpty() ? null : furniture.get(random.nextInt(furniture.size()));
    }

    /**
     * Where the guard stops at the furniture: on the guard's side of it, half
     * the furniture's width and half the guard's from its middle. DK II walks
     * the guard to the furniture's own position (DKII.EXE 0x463190) and the
     * solid furniture stops it there, facing the furniture as it walked at it
     *
     * @param creature the guard
     * @param furniture the furniture
     * @param position the position of the furniture
     * @return the spot
     */
    private Vector2f getSpotAtFurniture(ICreatureController creature, EntityId furniture, Vector3f position) {
        Vector3f guard = creature.getPosition();
        float distance = (kwdFile.getObject(entityData.getComponent(furniture, ObjectComponent.class).objectId).getWidth()
                + creature.getCreature().getAttributes().getWidth()) / 2;
        return new Vector2f(guard.x - position.x, guard.z - position.z).normalizeLocal().multLocal(distance)
                .addLocal(position.x, position.z);
    }

}
