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

import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.DoorPlacementValidator;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Keeps the doors and door blueprints standing only where they can stand. A
 * door falls when its tile is no longer its keeper's, and a door that is not a
 * barricade also falls when one of the two walls it stands between goes.<br>
 * Also lets the enemies discover the secret doors: an open secret door is
 * discovered by an enemy creature that is near and can see it.
 */
public final class DoorSystem implements IGameLogicUpdatable {

    /**
     * How near, in tiles, an enemy creature has to be to discover an open
     * secret door
     */
    private static final float SECRET_DOOR_DISCOVERY_DISTANCE = 4;

    /**
     * The step, in tiles, of the line of sight check
     */
    private static final float LINE_OF_SIGHT_STEP = 0.1f;

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final IMapInformation<?> mapInformation;
    private final ILevelInfo levelInfo;
    private final EntitySet doorEntities;
    private final EntitySet creatureEntities;

    public DoorSystem(EntityData entityData, IKwdFile kwdFile, IMapInformation<?> mapInformation, ILevelInfo levelInfo) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.mapInformation = mapInformation;
        this.levelInfo = levelInfo;

        doorEntities = entityData.getEntities(DoorComponent.class, Position.class, Owner.class);
        creatureEntities = entityData.getEntities(CreatureComponent.class, Position.class, Owner.class);
    }

    @Override
    public void processTick(float tpf) {
        doorEntities.applyChanges();
        creatureEntities.applyChanges();

        List<EntityId> fallenDoors = new ArrayList<>();
        for (Entity entity : doorEntities) {
            if (!isStanding(entity)) {
                fallenDoors.add(entity.getId());
                continue;
            }

            // Secret doors
            DoorComponent doorComponent = entity.get(DoorComponent.class);
            if (doorComponent.secret && isOpen(entity.getId()) && isDiscovered(entity)) {
                entityData.setComponent(entity.getId(), new DoorComponent(doorComponent.doorId, doorComponent.locked,
                        doorComponent.blueprint, false));
            }
        }

        // Nothing is left of a fallen door, no crate and no gold
        for (EntityId entityId : fallenDoors) {
            entityData.removeEntity(entityId);
        }
    }

    private boolean isOpen(EntityId entityId) {
        DoorViewState doorViewState = entityData.getComponent(entityId, DoorViewState.class);
        return doorViewState != null && doorViewState.open;
    }

    private boolean isDiscovered(Entity door) {
        Keeper doorOwner = levelInfo.getPlayer(door.get(Owner.class).ownerId);
        if (doorOwner == null) {
            return false;
        }

        Vector3f doorPosition = door.get(Position.class).position;
        for (Entity creature : creatureEntities) {
            if (!doorOwner.isEnemy(creature.get(Owner.class).ownerId) || !canSee(creature.getId())) {
                continue;
            }

            Vector3f creaturePosition = creature.get(Position.class).position;
            if (distance(creaturePosition, doorPosition) < SECRET_DOOR_DISCOVERY_DISTANCE * WorldUtils.TILE_WIDTH
                    && hasLineOfSight(creaturePosition, doorPosition)) {
                return true;
            }
        }
        return false;
    }

    private boolean canSee(EntityId creature) {
        return entityData.getComponent(creature, Unconscious.class) == null
                && entityData.getComponent(creature, Death.class) == null
                && entityData.getComponent(creature, CreatureImprisoned.class) == null
                && entityData.getComponent(creature, CreatureTortured.class) == null;
    }

    private static float distance(Vector3f from, Vector3f to) {
        float dx = to.x - from.x;
        float dz = to.z - from.z;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * No solid tile between the two points
     */
    private boolean hasLineOfSight(Vector3f from, Vector3f to) {
        int steps = (int) Math.ceil(distance(from, to) / (LINE_OF_SIGHT_STEP * WorldUtils.TILE_WIDTH));
        Point toTile = WorldUtils.vectorToPoint(to);
        for (int i = 1; i < steps; i++) {
            float t = (float) i / steps;
            Point tile = WorldUtils.vectorToPoint(from.x + (to.x - from.x) * t, from.z + (to.z - from.z) * t);
            if (tile.equals(toTile)) {
                break;
            }
            if (isSolid(tile)) {
                return false;
            }
        }
        return true;
    }

    private boolean isSolid(Point tile) {
        IMapTileInformation mapTile = mapInformation.getMapData().getTile(tile);
        if (mapTile == null) {
            return true;
        }
        Terrain terrain = mapInformation.getTerrain(mapTile);
        return terrain != null && terrain.getFlags().contains(Terrain.TerrainFlag.SOLID);
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
        creatureEntities.release();
    }

}
