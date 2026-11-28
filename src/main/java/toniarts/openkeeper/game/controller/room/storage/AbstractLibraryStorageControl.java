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
import java.util.Comparator;
import java.util.List;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/** Books and specials share the two storage places on each Library bookcase. */
public abstract class AbstractLibraryStorageControl extends AbstractRoomObjectControl<EntityId> {

    // Native Library draw offset (0x1300) and diagonal spacing (0x2aa), in tiles.
    private static final float STORED_HEIGHT = 19f / 16;
    private static final float PAIR_OFFSET = 682f / 4096;
    private boolean destroyed;

    protected AbstractLibraryStorageControl(IKwdFile kwdFile, IRoomController parent,
            EntityData entityData, IGameTimer gameTimer, ObjectType objectType) {
        super(kwdFile, parent, entityData, gameTimer, objectType);
    }

    @Override
    protected int getObjectsPerTile() {
        return 2;
    }

    private IRoomObjectControl<EntityId> otherStorage() {
        return parent.getObjectControl(getObjectType() == ObjectType.SPECIAL
                ? ObjectType.SPELL_BOOK : ObjectType.SPECIAL);
    }

    private List<EntityId> storedAt(Point point) {
        List<EntityId> items = new ArrayList<>(2);
        Collection<EntityId> own = getItems(point);
        if (own != null) {
            items.addAll(own);
        }
        IRoomObjectControl<EntityId> other = otherStorage();
        if (other != null && other.getItems(point) != null) {
            items.addAll(other.getItems(point));
        }
        return items;
    }

    @Override
    public Collection<Point> getAvailableCoordinates() {
        return getCoordinates().stream().filter(p -> storedAt(p).size() < 2).toList();
    }

    @Override
    public boolean isFullCapacity() {
        return getAvailableCoordinates().isEmpty();
    }

    @Override
    public EntityId addItem(EntityId value, Point point) {
        if (getItems().contains(value)) {
            return null;
        }
        Position position = entityData.getComponent(value, Position.class);
        Collection<Point> available = getAvailableCoordinates();
        if (point == null || !available.contains(point)) {
            point = available.stream().min(Comparator.comparingDouble(p -> {
                Vector3f centre = WorldUtils.pointToVector3f(p);
                float dx = centre.x - position.position.x;
                float dz = centre.z - position.position.z;
                return dx * dx + dz * dz;
            })).orElse(null);
        }
        if (point == null) {
            return value;
        }

        List<EntityId> items = storedAt(point);
        objectsByCoordinate.computeIfAbsent(point, p -> new ArrayList<>(2)).add(value);
        items.add(value);
        setRoomStorageToItem(value, true);
        changeSharedCapacity(1);
        for (int i = 0; i < items.size(); i++) {
            EntityId item = items.get(i);
            Position old = entityData.getComponent(item, Position.class);
            Vector3f target = WorldUtils.pointToVector3f(point);
            target.y = WorldUtils.FLOOR_HEIGHT + STORED_HEIGHT * WorldUtils.TILE_HEIGHT;
            if (items.size() == 2) {
                // DK II puts the item stored last at +0x2aa and the older one at -0x2aa (0x4E12A0)
                float offset = (item.equals(value) ? PAIR_OFFSET : -PAIR_OFFSET) * WorldUtils.TILE_WIDTH;
                target.addLocal(offset, 0, offset);
            }
            entityData.setComponent(item, new Position(old.rotation, target));
        }
        return null;
    }

    private void changeSharedCapacity(int amount) {
        addCurrentCapacity(amount);
        // Both replicated Storage records describe the same shelf capacity.
        if (otherStorage() instanceof AbstractLibraryStorageControl other && !other.destroyed) {
            other.addCurrentCapacity(amount);
        }
    }

    @Override
    public void removeItem(EntityId object) {
        if (!getItems().contains(object)) {
            return;
        }
        super.removeItem(object);
        changeSharedCapacity(-1);
        Position position = entityData.getComponent(object, Position.class);
        if (position != null) {
            Vector3f target = position.position.clone();
            target.y = WorldUtils.FLOOR_HEIGHT;
            entityData.setComponent(object, new Position(position.rotation, target));
        }
    }

    /** Release items whose bookcase disappeared during a room rebuild. */
    public void furnitureChanged() {
        Collection<Point> coordinates = getCoordinates();
        for (Point point : new ArrayList<>(objectsByCoordinate.keySet())) {
            if (!coordinates.contains(point)) {
                for (EntityId item : new ArrayList<>(objectsByCoordinate.get(point))) {
                    removeItem(item);
                }
                objectsByCoordinate.remove(point);
            }
        }
    }

    @Override
    public void destroy() {
        for (EntityId item : getItems()) {
            removeItem(item);
        }
        destroyed = true;
        super.destroy();
    }

    @Override
    protected Collection<Point> getCoordinates() {
        List<Point> coordinates = new ArrayList<>(parent.getFloorFurnitureCount());
        for (EntityId furniture : parent.getFloorFurniture()) {
            coordinates.add(WorldUtils.vectorToPoint(entityData.getComponent(furniture, Position.class).position));
        }
        return coordinates;
    }
}
