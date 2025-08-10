/*
 * Copyright (C) 2014-2024 OpenKeeper
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
package toniarts.openkeeper.view.map;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityContainer;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.game.component.Storage;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.controller.room.AbstractRoomInformation;
import toniarts.openkeeper.game.map.IRoomInformation;
import toniarts.openkeeper.game.map.IRoomsInformation;
import toniarts.openkeeper.tools.convert.map.KwdFile;

import java.lang.System.Logger.Level;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Contains the map rooms
 */
public class MapRoomContainer extends EntityContainer<IRoomInformation> implements IRoomsInformation<IRoomInformation> {

    private static final System.Logger logger = System.getLogger(MapRoomContainer.class.getName());

    private final Map<EntityId, IRoomInformation> roomMap = new HashMap<>();
    private final Map<Short, Set<EntityId>> roomsByOwners = new HashMap<>();
    private final Map<EntityId, Short> ownersByRoom = new HashMap<>();
    private final Map<EntityId, Short> roomTypesByRoom = new HashMap<>();
    private final Map<Short, Map<Short, Set<EntityId>>> roomTypesByOwners = new HashMap<>();
    private final Map<EntityId, Map<AbstractRoomController.ObjectType, Entity>> roomCatalogsByRoom = new HashMap<>();
    private final Map<EntityId, AbstractRoomController.ObjectType> storageTypeByRoomCatalog = new HashMap<>();
    private final Map<EntityId, EntityId> roomByRoomCatalog = new HashMap<>();

    private final EntitySet roomCatalogs;

    public MapRoomContainer(EntityData entityData, KwdFile kwdFile) {
        super(entityData, RoomComponent.class, Owner.class, Health.class);

        roomCatalogs = entityData.getEntities(Storage.class);
    }

    @Override
    protected IRoomInformation addObject(Entity e) {
        logger.log(Level.TRACE, "MapRoomContainer.addObject({0})", e);
        IRoomInformation result = new RoomInformation(e);
        roomMap.put(e.getId(), result);
        ownersByRoom.put(e.getId(), result.getOwnerId());
        roomTypesByRoom.put(e.getId(), result.getRoomId());
        roomsByOwners
                .computeIfAbsent(result.getOwnerId(), (t) -> new HashSet<>())
                .add(e.getId());
        roomTypesByOwners
                .computeIfAbsent(result.getOwnerId(), (t) -> new HashMap<>())
                .computeIfAbsent(result.getRoomId(), (t) -> new HashSet<>())
                .add(e.getId());

        return result;
    }

    @Override
    protected void updateObject(IRoomInformation object, Entity e) {
        logger.log(System.Logger.Level.TRACE, "MapRoomContainer.updateObject({0}, {1})", object, e);

        // See if the room has changed owners
        Short oldOwner = ownersByRoom.get(e.getId());
        Short newOwner = object.getOwnerId();
        if (oldOwner != newOwner) {
            ownersByRoom.put(e.getId(), newOwner);
            roomsByOwners.get(oldOwner).remove(e.getId());
            roomsByOwners
                    .computeIfAbsent(newOwner, (t) -> new HashSet<>())
                    .add(e.getId());
            roomTypesByOwners.get(oldOwner).get(object.getRoomId()).remove(e.getId());
            roomTypesByOwners
                    .computeIfAbsent(newOwner, (t) -> new HashMap<>())
                    .computeIfAbsent(object.getRoomId(), (t) -> new HashSet<>())
                    .add(e.getId());
        }
    }

    @Override
    protected void removeObject(IRoomInformation object, Entity e) {
        logger.log(Level.TRACE, "MapTileContainer.removeObject({0})", e);
        roomMap.remove(e.getId());
        Short ownerId = ownersByRoom.remove(e.getId());
        roomsByOwners.get(ownerId).remove(e.getId());
        roomTypesByOwners.get(ownerId).get(roomTypesByRoom.remove(e.getId())).remove(e.getId());

        roomCatalogsByRoom.remove(e.getId());
    }

    @Override
    public boolean update() {
        boolean changes = super.update();

        // Also update the room capacity catalogs
        if (roomCatalogs.applyChanges()) {
            removeCatalogs(roomCatalogs.getRemovedEntities());
            addCatalogs(roomCatalogs.getAddedEntities());
        }

        return changes;
    }

    @Override
    public void start() {
        super.start();

        roomCatalogs.applyChanges();
        addCatalogs(roomCatalogs);
    }

    @Override
    public void stop() {

        // Also free the room capacity catalog updates
        roomCatalogs.release();
        roomCatalogsByRoom.clear();
        storageTypeByRoomCatalog.clear();
        roomByRoomCatalog.clear();

        super.stop();
    }

    @Override
    public IRoomInformation getRoomInformation(EntityId entityId) {
        return roomMap.get(entityId);
    }

    private void addCatalogs(Set<Entity> entities) {
        for (Entity entity : entities) {
            storageTypeByRoomCatalog.put(entity.getId(), entity.get(Storage.class).objectType);
            roomByRoomCatalog.put(entity.getId(), entity.get(Storage.class).room);
            Map<AbstractRoomController.ObjectType, Entity> roomEntities = roomCatalogsByRoom.computeIfAbsent(entity.get(Storage.class).room, (t) -> new HashMap<>());
            roomEntities.put(entity.get(Storage.class).objectType, entity);
        }
    }

    private void removeCatalogs(Set<Entity> entities) {
        for (Entity entity : entities) {
            Map<AbstractRoomController.ObjectType, Entity> roomEntities = roomCatalogsByRoom.getOrDefault(roomByRoomCatalog.remove(entity.getId()), Collections.emptyMap());
            roomEntities.remove(storageTypeByRoomCatalog.remove(entity.getId()));
        }
    }

    @Override
    public int getRoomCount(short ownerId, short roomId) {
        return roomTypesByOwners
                .getOrDefault(ownerId, Collections.emptyMap())
                .getOrDefault(roomId, Collections.emptySet())
                .size();
    }

    /**
     * Single room that taps into the entity information
     */
    private class RoomInformation extends AbstractRoomInformation {

        private final Entity entity;

        public RoomInformation(Entity entity) {
            super(entity.getId());

            this.entity = entity;
        }

        @Override
        protected <T extends EntityComponent> T getEntityComponent(Class<T> type) {
            return entity.get(type);
        }

        @Override
        public int getMaxCapacity(AbstractRoomController.ObjectType objectType) {
            return getStorageValueIfExists(objectType, (storage) -> storage.maxCapacity);
        }

        @Override
        public int getUsedCapacity(AbstractRoomController.ObjectType objectType) {
            return getStorageValueIfExists(objectType, (storage) -> storage.currentCapacity);
        }

        private int getStorageValueIfExists(AbstractRoomController.ObjectType objectType, Function<Storage, Integer> getValue) {
            Entity storageEntity = roomCatalogsByRoom.getOrDefault(getEntityId(), Collections.emptyMap()).get(objectType);
            if (storageEntity == null) {
                return 0;
            }

            return getValue.apply(storageEntity.get(Storage.class));
        }

    }

}
