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
import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.game.controller.room.AbstractRoomInformation;
import toniarts.openkeeper.game.map.IRoomInformation;
import toniarts.openkeeper.game.map.IRoomsInformation;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 * Contains the map rooms
 */
public class MapRoomContainer extends EntityContainer<IRoomInformation> implements IRoomsInformation<IRoomInformation> {

    private static final System.Logger logger = System.getLogger(MapRoomContainer.class.getName());

    private final Map<EntityId, IRoomInformation> roomMap = new HashMap<>();

    public MapRoomContainer(EntityData entityData, KwdFile kwdFile) {
        super(entityData, RoomComponent.class, Owner.class, Health.class);
    }

    @Override
    protected IRoomInformation addObject(Entity e) {
        logger.log(Level.TRACE, "MapRoomContainer.addObject({0})", e);
        IRoomInformation result = new RoomInformation(e);
        roomMap.put(e.getId(), result);

        return result;
    }

    @Override
    protected void updateObject(IRoomInformation object, Entity e) {
        logger.log(System.Logger.Level.TRACE, "MapRoomContainer.updateObject({0}, {1})", object, e);
    }

    @Override
    protected void removeObject(IRoomInformation object, Entity e) {
        logger.log(Level.TRACE, "MapTileContainer.removeObject({0})", e);
        roomMap.remove(e.getId());
    }

    @Override
    public boolean update() {
        return super.update();

        // Also update the room capacity catalogs
    }

    @Override
    public void stop() {
        super.stop();

        // Also free the room capacity catalog updates
    }

    @Override
    public IRoomInformation getRoomInformation(EntityId entityId) {
        return roomMap.get(entityId);
    }

    /**
     * Single map tile that taps into the entity information
     */
    private static class RoomInformation extends AbstractRoomInformation {

        private final Entity entity;

        public RoomInformation(Entity entity) {
            super(entity.getId());

            this.entity = entity;
        }

        @Override
        protected <T extends EntityComponent> T getEntityComponent(Class<T> type) {
            return entity.get(type);
        }

    }

}
