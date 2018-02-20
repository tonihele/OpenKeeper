/*
 * Copyright (C) 2014-2018 OpenKeeper
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
package toniarts.openkeeper.game.logic;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Maintains a tile based position map of all the entities for a quick lookup
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PositionSystem implements IGameLogicUpdatable, IEntityPositionLookup {

    private final IMapController mapController;
    private final EntitySet positionedEntities;
    private final Map<MapTile, Set<EntityId>> entitiesByMapTile = new HashMap<>();
    private final Map<EntityId, MapTile> mapTilesByEntities = new HashMap<>();

    public PositionSystem(IMapController mapController, EntityData entityData) {
        this.mapController = mapController;

        positionedEntities = entityData.getEntities(Position.class);
        processAddedEntities(positionedEntities);
    }

    @Override
    public void processTick(float tpf) {
        if (positionedEntities.applyChanges()) {

            processAddedEntities(positionedEntities.getAddedEntities());

            processDeletedEntities(positionedEntities.getRemovedEntities());

            processChangedEntities(positionedEntities.getChangedEntities());
        }
    }

    private void processChangedEntities(Set<Entity> entities) {

        // Update
        for (Entity entity : entities) {
            Point p = WorldUtils.vectorToPoint(entity.get(Position.class).position);
            MapTile currentMapTile = mapController.getMapData().getTile(p);

            MapTile previousMapTile = mapTilesByEntities.get(entity.getId());
            if (!currentMapTile.equals(previousMapTile)) {

                // Moved
                mapTilesByEntities.put(entity.getId(), currentMapTile);
                entitiesByMapTile.get(previousMapTile).remove(entity.getId());
                Set<EntityId> entitiesInTile = entitiesByMapTile.get(currentMapTile);
                if (entitiesInTile == null) {
                    entitiesInTile = new HashSet<>();
                }
                entitiesInTile.add(entity.getId());
                entitiesByMapTile.put(currentMapTile, entitiesInTile);
            }
        }
    }

    private void processDeletedEntities(Set<Entity> entities) {

        // Remove
        for (Entity entity : entities) {
            MapTile mapTile = mapTilesByEntities.remove(entity.getId());
            entitiesByMapTile.get(mapTile).remove(entity.getId());
        }
    }

    private void processAddedEntities(Set<Entity> entities) {

        // Add
        for (Entity entity : entities) {
            Point p = WorldUtils.vectorToPoint(entity.get(Position.class).position);
            MapTile mapTile = mapController.getMapData().getTile(p);
            mapTilesByEntities.put(entity.getId(), mapTile);
            Set<EntityId> entitiesInTile = entitiesByMapTile.get(mapTile);
            if (entitiesInTile == null) {
                entitiesInTile = new HashSet<>();
            }
            entitiesInTile.add(entity.getId());
            entitiesByMapTile.put(mapTile, entitiesInTile);
        }
    }

    @Override
    public List<EntityId> getEntitiesInLocation(Point p) {
        MapTile mapTile = mapController.getMapData().getTile(p);
        return getEntitiesInLocation(mapTile);
    }

    @Override
    public List<EntityId> getEntitiesInLocation(int x, int y) {
        MapTile mapTile = mapController.getMapData().getTile(x, y);
        return getEntitiesInLocation(mapTile);
    }

    @Override
    public List<EntityId> getEntitiesInLocation(MapTile mapTile) {
        Set<EntityId> entityId = entitiesByMapTile.get(mapTile);
        if (entityId != null) {
            return new ArrayList<>(entityId);
        }

        return Collections.emptyList();
    }

    @Override
    public MapTile getEntityLocation(EntityId entityId) {
        return mapTilesByEntities.get(entityId);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        positionedEntities.release();
    }

}
