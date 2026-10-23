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
package toniarts.openkeeper.game.logic;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.DoorPlacementValidator;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Keeps the doors and door blueprints standing only where they can stand. A
 * door falls when its tile is no longer its keeper's, and a door that is not a
 * barricade also falls when one of the two walls it stands between goes.
 */
public final class DoorSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final IMapInformation<?> mapInformation;
    private final EntitySet doorEntities;

    public DoorSystem(EntityData entityData, IKwdFile kwdFile, IMapInformation<?> mapInformation) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.mapInformation = mapInformation;

        doorEntities = entityData.getEntities(DoorComponent.class, Position.class, Owner.class);
    }

    @Override
    public void processTick(float tpf) {
        doorEntities.applyChanges();

        List<EntityId> fallenDoors = new ArrayList<>();
        for (Entity entity : doorEntities) {
            if (!isStanding(entity)) {
                fallenDoors.add(entity.getId());
            }
        }

        // Nothing is left of a fallen door, no crate and no gold
        for (EntityId entityId : fallenDoors) {
            entityData.removeEntity(entityId);
        }
    }

    private boolean isStanding(Entity entity) {
        Point tile = WorldUtils.vectorToPoint(entity.get(Position.class).position);
        IMapTileInformation mapTile = mapInformation.getMapData().getTile(tile);
        if (mapTile == null || mapTile.getOwnerId() != entity.get(Owner.class).ownerId) {
            return false;
        }

        Door door = kwdFile.getDoorById(entity.get(DoorComponent.class).doorId);
        if (door == null || door.getFlags().contains(Door.DoorFlag.IS_BARRICADE)) {
            return true;
        }

        // The door is turned to stand between walls north and south, otherwise it stands between east and west
        if (entity.get(Position.class).rotation != 0) {
            return DoorPlacementValidator.isBetweenWallsNorthAndSouth(mapInformation, tile.x, tile.y);
        }
        return DoorPlacementValidator.isBetweenWallsEastAndWest(mapInformation, tile.x, tile.y);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        doorEntities.release();
    }

}
