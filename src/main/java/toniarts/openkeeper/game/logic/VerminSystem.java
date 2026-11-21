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

import com.badlogic.gdx.math.Vector2;
import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.FieldFilter;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * The vermin: bats in the Graveyards and rats in the Prisons.<br>
 * DK II rolls for every player on every game tick (DKII.EXE 0x4BA0D0): while
 * the player has fewer bats than a ninth of its Graveyard tiles, 16 at most,
 * there is a 5 in 100 chance of a new bat on a random tile of its first
 * Graveyard. Rats go the same way by the Prison and Torture Chamber tiles
 * together, and appear in the first Prison.<br>
 * A rat or a bat then wanders one tile at a time, to a random spot of a
 * neighbouring tile of its owner, and one whose tile is not its owner's
 * anymore is gone (object state WANDER_WITHIN_PLAYER, 0x49FE60).
 */
public final class VerminSystem implements IGameLogicUpdatable {

    /**
     * The Rat object
     */
    public static final short OBJECT_RAT_ID = 11;
    /**
     * The Bat object
     */
    public static final short OBJECT_BAT_ID = 12;
    private static final int MAX_VERMIN = 16;
    private static final int TILES_PER_VERMIN = 9;
    private static final int SPAWN_CHANCE_PERCENTAGE = 5;
    /**
     * DK II puts a bat at height 0x1C00 and a rat on the floor at 0x1000
     */
    private static final float BAT_FLYING_HEIGHT = 0.75f;
    private static final int[][] NEIGHBOURS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    private final EntityData entityData;
    private final IMapController mapController;
    private final IObjectsController objectsController;
    private final Collection<IPlayerController> players;
    private final Random random;
    private final EntitySet rats;
    private final EntitySet bats;

    /**
     * Creates the vermin system
     *
     * @param entityData the entity data
     * @param mapController the map controller
     * @param objectsController the objects controller
     * @param players the players
     */
    public VerminSystem(EntityData entityData, IMapController mapController, IObjectsController objectsController,
            Collection<IPlayerController> players) {
        this(entityData, mapController, objectsController, players, new Random());
    }

    VerminSystem(EntityData entityData, IMapController mapController, IObjectsController objectsController,
            Collection<IPlayerController> players, Random random) {
        this.entityData = entityData;
        this.mapController = mapController;
        this.objectsController = objectsController;
        this.players = players;
        this.random = random;
        rats = entityData.getEntities(new FieldFilter<>(ObjectComponent.class, "objectId", OBJECT_RAT_ID),
                ObjectComponent.class, Owner.class, Position.class);
        bats = entityData.getEntities(new FieldFilter<>(ObjectComponent.class, "objectId", OBJECT_BAT_ID),
                ObjectComponent.class, Owner.class, Position.class);
    }

    @Override
    public void processTick(float tpf) {
        rats.applyChanges();
        bats.applyChanges();
        for (IPlayerController player : players) {
            short playerId = player.getKeeper().getId();
            spawn(playerId, bats, OBJECT_BAT_ID, ObjectType.CORPSE, null);
            spawn(playerId, rats, OBJECT_RAT_ID, ObjectType.PRISONER, ObjectType.TORTUREE);
        }
        wander(rats);
        wander(bats);
    }

    private void spawn(short playerId, EntitySet vermin, short objectId, ObjectType homeRoom, ObjectType otherRoom) {
        if (random.nextInt(100) >= SPAWN_CHANCE_PERCENTAGE) {
            return;
        }
        List<IRoomController> homes = mapController.getRoomsByFunction(homeRoom, playerId);
        if (homes.isEmpty()) {
            return;
        }
        int tiles = countTiles(homes);
        if (otherRoom != null) {
            tiles += countTiles(mapController.getRoomsByFunction(otherRoom, playerId));
        }
        int count = 0;
        for (Entity entity : vermin) {
            if (entity.get(Owner.class).ownerId == playerId) {
                count++;
            }
        }
        if (count >= Math.min(MAX_VERMIN, tiles / TILES_PER_VERMIN)) {
            return;
        }

        List<Point> coordinates = homes.getFirst().getRoomInstance().getCoordinates();
        Vector3f position = WorldUtils.pointToVector3f(coordinates.get(random.nextInt(coordinates.size())));
        EntityId entityId = objectsController.loadObject(objectId, playerId, position, 0);
        if (objectId == OBJECT_BAT_ID) {
            Position floorPosition = entityData.getComponent(entityId, Position.class);
            entityData.setComponent(entityId, new Position(floorPosition.rotation,
                    floorPosition.position.add(0, BAT_FLYING_HEIGHT, 0)));
        }
    }

    private static int countTiles(List<IRoomController> rooms) {
        int tiles = 0;
        for (IRoomController room : rooms) {
            tiles += room.getRoomInstance().getCoordinates().size();
        }
        return tiles;
    }

    private void wander(EntitySet vermin) {
        for (Entity entity : vermin) {
            if (entityData.getComponent(entity.getId(), Navigation.class) != null) {
                continue;
            }
            short ownerId = entity.get(Owner.class).ownerId;
            Vector3f position = entity.get(Position.class).position;
            Point tile = WorldUtils.vectorToPoint(position);
            IMapTileInformation mapTile = mapController.getMapData().getTile(tile);
            if (mapTile == null || mapTile.getOwnerId() != ownerId) {
                entityData.removeEntity(entity.getId());
                continue;
            }

            // The four neighbours from a random one on, the first that will do
            int start = random.nextInt(NEIGHBOURS.length);
            for (int i = 0; i < NEIGHBOURS.length; i++) {
                int[] offset = NEIGHBOURS[(start + i) % NEIGHBOURS.length];
                Point neighbour = new Point(tile.x + offset[0], tile.y + offset[1]);
                if (isWanderable(neighbour, ownerId)) {
                    Vector2 spot = new Vector2(neighbour.x + random.nextFloat() - 0.5f, neighbour.y + random.nextFloat() - 0.5f);
                    entityData.setComponent(entity.getId(), new Navigation(neighbour, null, List.of(new Vector2(position.x, position.z), spot)));
                    break;
                }
            }
        }
    }

    /**
     * The vermin keep to the open tiles of their owner, and out of the middle
     * of a Prison, a Combat Pit or a Temple
     */
    private boolean isWanderable(Point p, short ownerId) {
        IMapTileInformation mapTile = mapController.getMapData().getTile(p);
        if (mapTile == null || mapTile.getOwnerId() != ownerId || mapController.isSolid(p)) {
            return false;
        }
        IRoomController room = mapController.getRoomControllerByCoordinates(p);
        if (room == null || room.getRoom().getTileConstruction() != Room.TileConstruction.DOUBLE_QUAD) {
            return true;
        }
        for (int x = p.x - 1; x <= p.x + 1; x++) {
            for (int y = p.y - 1; y <= p.y + 1; y++) {
                if (!room.getRoomInstance().hasCoordinate(new Point(x, y))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        rats.release();
        bats.release();
    }

}
