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
package toniarts.openkeeper.game.controller.room.storage;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * The crates stored in a workshop, stacked three on each free floor tile,
 * until they are carried to their blueprints
 */
public abstract class RoomCrateControl extends AbstractRoomObjectControl<EntityId> {

    /**
     * Crates stacked on one tile
     */
    static final int CRATES_PER_STACK = 3;

    /**
     * The size of a crate in case its object doesn't tell
     */
    static final float DEFAULT_CRATE_SIZE = 0.21582031f;

    /**
     * The gap between the two crates at the bottom of a stack
     */
    static final float CRATE_GAP = 0.02f;

    protected RoomCrateControl(IKwdFile kwdFile, IRoomController parent, EntityData entityData, IGameTimer gameTimer) {
        super(kwdFile, parent, entityData, gameTimer, ObjectType.CRATE);
    }

    @Override
    protected int getObjectsPerTile() {
        return CRATES_PER_STACK;
    }

    @Override
    public EntityId addItem(EntityId value, Point p) {
        Collection<Point> availableCoordinates = getAvailableCoordinates();
        if (p == null || !availableCoordinates.contains(p)) {
            p = findStack(availableCoordinates);
            if (p == null) {
                return value; // No room for it
            }
        }

        objectsByCoordinate.computeIfAbsent(p, (point) -> new ArrayList<>(getObjectsPerTile())).add(value);
        stack(p);
        setRoomStorageToItem(value, true);
        addCurrentCapacity(1);
        return null;
    }

    /**
     * Fills up the started stacks before starting a new one
     */
    private Point findStack(Collection<Point> availableCoordinates) {
        Point emptyTile = null;
        for (Point coordinate : availableCoordinates) {
            Collection<EntityId> crates = objectsByCoordinate.get(coordinate);
            if (crates != null && !crates.isEmpty()) {
                return coordinate;
            }
            if (emptyTile == null) {
                emptyTile = coordinate;
            }
        }
        return emptyTile;
    }

    @Override
    public void removeItem(EntityId object) {
        Point tile = getTile(object);
        release(object);

        // The crate on top comes down
        if (tile != null) {
            stack(tile);
        }
    }

    @Override
    public void destroy() {

        // The crates stay where they are, stacks and all, no longer stored in a workshop. Before the storage
        // itself is gone, releasing a crate updates its capacity
        for (Collection<EntityId> crates : new ArrayList<>(objectsByCoordinate.values())) {
            for (EntityId crate : new ArrayList<>(crates)) {
                release(crate);
            }
        }

        super.destroy();
    }

    private void release(EntityId crate) {
        super.removeItem(crate);
        addCurrentCapacity(-1);
    }

    private Point getTile(EntityId crate) {
        for (Map.Entry<Point, Collection<EntityId>> entry : objectsByCoordinate.entrySet()) {
            if (entry.getValue().contains(crate)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Stacks the crates of a tile like the original: two side by side with a
     * small gap, the third on top of them. A single crate stands in the middle
     * of the tile.
     */
    private void stack(Point tile) {
        List<EntityId> crates = new ArrayList<>(objectsByCoordinate.get(tile));
        if (crates.isEmpty()) {
            return;
        }
        float size = getCrateSize(crates.getFirst());
        for (int i = 0; i < crates.size(); i++) {
            EntityId crate = crates.get(i);
            Position position = entityData.getComponent(crate, Position.class);
            entityData.setComponent(crate, new Position(position.rotation, getStackPosition(tile, i, crates.size(), size)));
        }
    }

    /**
     * Where a crate goes in its stack
     *
     * @param tile the tile of the stack
     * @param index the crate's place in the stack, 0 and 1 at the bottom, 2 on
     * top
     * @param count the crates in the stack
     * @param size the size of a crate
     * @return the position
     */
    static Vector3f getStackPosition(Point tile, int index, int count, float size) {
        Vector3f position = WorldUtils.pointToVector3f(tile);
        position.y = WorldUtils.FLOOR_HEIGHT;
        if (count == 1) {
            return position;
        }

        float offset = (size + CRATE_GAP) / 2;
        switch (index) {
            case 0 ->
                position.x -= offset;
            case 1 ->
                position.x += offset;
            default ->
                position.y += size;
        }
        return position;
    }

    /**
     * A crate is a cube, its width is also its height
     */
    private float getCrateSize(EntityId crate) {
        ObjectComponent objectComponent = entityData.getComponent(crate, ObjectComponent.class);
        if (objectComponent == null) {
            return DEFAULT_CRATE_SIZE;
        }
        return kwdFile.getObject(objectComponent.objectId).getWidth();
    }

    @Override
    protected Collection<Point> getCoordinates() {

        // The floor, the furniture takes up its tiles
        Set<Point> furniture = new HashSet<>(parent.getFloorFurnitureCount());
        for (EntityId oc : parent.getFloorFurniture()) {
            furniture.add(WorldUtils.vectorToPoint(entityData.getComponent(oc, Position.class).position));
        }
        List<Point> coordinates = new ArrayList<>(super.getCoordinates());
        coordinates.removeAll(furniture);
        return coordinates;
    }

}
