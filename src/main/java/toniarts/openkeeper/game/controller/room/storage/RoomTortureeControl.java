/*
 * Copyright (C) 2014-2016 OpenKeeper
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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.ObjectsController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/** Server-side device admission and storage for creatures undergoing torture. */
public abstract class RoomTortureeControl extends AbstractRoomObjectControl<EntityId> {

    // Original world positions use twelve fractional bits per tile.
    private static final int POSITION_FRACTION_BITS = 12;
    private static final long POSITION_SCALE = 1L << POSITION_FRACTION_BITS;
    private final IGameTimer gameTimer;

    public RoomTortureeControl(IKwdFile kwdFile, IRoomController parent, EntityData entityData, IGameTimer gameTimer) {
        super(kwdFile, parent, entityData, gameTimer, ObjectType.TORTUREE);
        this.gameTimer = gameTimer;
    }

    @Override
    protected int getObjectsPerTile() {
        return 1;
    }

    /** Returns the rejected creature, or null when it was admitted to a free device. */
    @Override
    public EntityId addItem(EntityId torturee, Point p) {
        return tryAdmit(torturee, new Vector2f(p.x * WorldUtils.TILE_WIDTH,
                p.y * WorldUtils.TILE_WIDTH)) ? null : torturee;
    }

    /**
     * Prefer a free device on the drop/spawn tile, otherwise use the nearest
     * free device in this room. Occupied tiles are excluded; idle bystanders and
     * the admitting creature itself do not reserve a device.
     *
     * The room lock covers validation and publication of the occupied position/AI,
     * so two session/loading producers cannot claim the same device together.
     * This method stores state only; callers queue scripting events afterwards.
     */
    public synchronized boolean tryAdmit(EntityId torturee, Vector2f position) {
        Health health = entityData.getComponent(torturee, Health.class);
        CreatureComponent creature = entityData.getComponent(torturee, CreatureComponent.class);
        Owner owner = entityData.getComponent(torturee, Owner.class);
        if (health == null || health.health <= 0 || creature == null || owner == null
                || entityData.getComponent(torturee, Death.class) != null
                || (owner.ownerId == parent.getOwnerId()
                    && creature.creatureId == kwdFile.getImp().getCreatureId())) {
            return false;
        }

        Position device = findAvailableDevice(torturee, position);
        if (device == null) {
            return false;
        }
        Point tile = WorldUtils.vectorToPoint(device.position);
        discardStaleItems();
        boolean alreadyStored = objectsByCoordinate.values().stream().anyMatch(ids -> ids.contains(torturee));
        objectsByCoordinate.values().forEach(ids -> ids.remove(torturee));
        objectsByCoordinate.computeIfAbsent(tile, p -> new HashSet<>()).add(torturee);
        if (!alreadyStored) {
            addCurrentCapacity(1);
        }
        setRoomStorageToItem(torturee, false);
        CreatureTortured previous = entityData.getComponent(torturee, CreatureTortured.class);
        double now = gameTimer.getGameTime();
        entityData.setComponents(torturee,
                new Position(device.rotation, new Vector3f(device.position.x, WorldUtils.FLOOR_HEIGHT, device.position.z)),
                new CreatureAi(now, CreatureState.TORTURED, creature.creatureId),
                new CreatureTortured(previous == null ? 0 : previous.timeTortured, now, now));
        entityData.removeComponent(torturee, CreatureImprisoned.class);
        entityData.removeComponent(torturee, CreatureFall.class);
        return true;
    }

    /** Select and reserve under the admission lock, using current furniture and AI. */
    private Position findAvailableDevice(EntityId torturee, Vector2f position) {
        if (!Float.isFinite(position.x) || !Float.isFinite(position.y)) {
            return null;
        }
        Point dropTile = WorldUtils.vectorToPoint(position);
        if (!parent.getRoomInstance().getCoordinates().contains(dropTile)) {
            return null;
        }
        Set<Point> occupied = new HashSet<>();
        var creatures = entityData.getEntities(CreatureComponent.class, CreatureAi.class, Position.class);
        try {
            for (var other : creatures) {
                if (!other.getId().equals(torturee)
                        && other.get(CreatureAi.class).getCreatureState() == CreatureState.TORTURED) {
                    occupied.add(WorldUtils.vectorToPoint(other.get(Position.class).position));
                }
            }
        } finally {
            creatures.release();
        }
        Position nearest = null;
        boolean nearestOnDropTile = false;
        double nearestDistance = Double.POSITIVE_INFINITY;
        // Stable tie order also makes equidistant devices reproducible.
        for (EntityId id : furniture().stream().sorted().toList()) {
            Position device = devicePosition(id);
            if (device == null) {
                continue;
            }
            Point tile = WorldUtils.vectorToPoint(device.position);
            if (occupied.contains(tile)) {
                continue;
            }
            boolean onDropTile = dropTile.equals(tile);
            double dx = (double) position.x - device.position.x;
            double dy = (double) position.y - device.position.z;
            double distance = dx * dx + dy * dy;
            if (nearest == null || (onDropTile && !nearestOnDropTile)
                    || (onDropTile == nearestOnDropTile && distance < nearestDistance)) {
                nearest = device;
                nearestOnDropTile = onDropTile;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    /** Geometry-only check for an existing admission; it does not reserve another tile. */
    private boolean hasDeviceAt(Vector2f position) {
        if (!Float.isFinite(position.x) || !Float.isFinite(position.y)) {
            return false;
        }
        Point tile = WorldUtils.vectorToPoint(position);
        boolean deviceFound = false;
        for (EntityId id : furniture()) {
            Position devicePosition = devicePosition(id);
            if (devicePosition != null && tile.equals(WorldUtils.vectorToPoint(devicePosition.position))) {
                long dx = fixedPosition(position.x) - fixedPosition(devicePosition.position.x);
                long dy = fixedPosition(position.y) - fixedPosition(devicePosition.position.z);
                if ((dx * dx >> POSITION_FRACTION_BITS) + (dy * dy >> POSITION_FRACTION_BITS) < POSITION_SCALE) {
                    deviceFound = true;
                    break;
                }
            }
        }
        return deviceFound;
    }

    private static long fixedPosition(float coordinate) {
        return (long) Math.floor((double) coordinate * POSITION_SCALE / WorldUtils.TILE_WIDTH);
    }

    /** Validate before torture damage/conversion, releasing stale admissions once. */
    public static boolean validate(EntityData data, IMapController map, EntityId id, double now) {
        if (data.getComponent(id, CreatureTortured.class) == null) {
            return false;
        }
        Position position = data.getComponent(id, Position.class);
        IRoomController room = position == null ? null
                : map.getRoomControllerByCoordinates(WorldUtils.vectorToPoint(position.position));
        RoomTortureeControl control = room != null && room.hasObjectControl(ObjectType.TORTUREE)
                ? room.getObjectControl(ObjectType.TORTUREE) : null;
        Stored stored = data.getComponent(id, Stored.class);
        CreatureAi ai = data.getComponent(id, CreatureAi.class);
        if (control != null && stored != null && stored.objectType == ObjectType.TORTUREE
                && stored.room.equals(room.getEntityId()) && ai != null
                && ai.getCreatureState() == CreatureState.TORTURED
                && control.hasDeviceAt(new Vector2f(position.position.x, position.position.z))) {
            return true;
        }
        if (control != null && stored != null && stored.objectType == ObjectType.TORTUREE
                && stored.room.equals(room.getEntityId())) {
            control.removeItem(id);
        }
        release(data, id, now);
        return false;
    }

    private Set<EntityId> furniture() {
        Set<EntityId> furniture = new HashSet<>(parent.getFloorFurniture());
        furniture.addAll(parent.getWallFurniture());
        return furniture;
    }

    private Position devicePosition(EntityId id) {
        ObjectComponent object = entityData.getComponent(id, ObjectComponent.class);
        Position position = entityData.getComponent(id, Position.class);
        if (parent.getRoomInstance().isDestroyed() || object == null || position == null
                || !Float.isFinite(position.position.x) || !Float.isFinite(position.position.z)
                || (object.objectId != ObjectsController.OBJECT_TORTURE_CHAIR_ID
                    && object.objectId != ObjectsController.OBJECT_TORTURE_WHEEL_ID)
                || !parent.getRoomInstance().getCoordinates().contains(WorldUtils.vectorToPoint(position.position))) {
            return null;
        }
        return position;
    }

    private void discardStaleItems() {
        int removed = 0;
        for (Collection<EntityId> ids : objectsByCoordinate.values()) {
            int before = ids.size();
            ids.removeIf(id -> {
                Stored stored = entityData.getComponent(id, Stored.class);
                return stored == null || stored.objectType != ObjectType.TORTUREE
                        || !stored.room.equals(parent.getEntityId());
            });
            removed += before - ids.size();
        }
        if (removed != 0) {
            addCurrentCapacity(-removed);
        }
    }

    @Override
    public synchronized void removeItem(EntityId object) {
        boolean stored = objectsByCoordinate.values().stream().anyMatch(ids -> ids.contains(object));
        Stored binding = entityData.getComponent(object, Stored.class);
        if (binding == null || (binding.objectType == ObjectType.TORTUREE
                && binding.room.equals(parent.getEntityId()))) {
            super.removeItem(object);
        } else {
            objectsByCoordinate.values().forEach(ids -> ids.remove(object));
        }
        if (stored) {
            addCurrentCapacity(-1);
        }
    }

    /** Release a cancelled admission or a creature whose torture device disappeared. */
    public static void release(EntityData entityData, EntityId creature, double now) {
        entityData.removeComponent(creature, CreatureTortured.class);
        Stored stored = entityData.getComponent(creature, Stored.class);
        if (stored != null && stored.objectType == ObjectType.TORTUREE) {
            entityData.removeComponent(creature, Stored.class);
        }
        CreatureAi ai = entityData.getComponent(creature, CreatureAi.class);
        if (ai != null && ai.getCreatureState() == CreatureState.TORTURED
                && entityData.getComponent(creature, Death.class) == null) {
            entityData.setComponent(creature, new CreatureAi(now, CreatureState.IDLE, ai.creatureId));
        }
        Owner owner = entityData.getComponent(creature, Owner.class);
        if (owner != null && entityData.getComponent(creature, CreatureImprisoned.class) == null) {
            entityData.setComponent(creature, new Owner(owner.ownerId, owner.ownerId));
        }
    }

    @Override
    public synchronized void destroy() {
        for (Collection<EntityId> ids : objectsByCoordinate.values()) {
            for (EntityId id : ids) {
                Stored stored = entityData.getComponent(id, Stored.class);
                if (stored != null && stored.objectType == ObjectType.TORTUREE
                        && stored.room.equals(parent.getEntityId())) {
                    release(entityData, id, gameTimer.getGameTime());
                }
            }
        }
        objectsByCoordinate.clear();
        super.destroy();
    }

    @Override
    public void captured(short playerId) {
    }

    @Override
    protected Collection<Point> getCoordinates() {
        Set<Point> coordinates = new HashSet<>();
        for (EntityId id : furniture()) {
            Position position = devicePosition(id);
            if (position != null) {
                coordinates.add(WorldUtils.vectorToPoint(position.position));
            }
        }
        return new ArrayList<>(coordinates);
    }
}
