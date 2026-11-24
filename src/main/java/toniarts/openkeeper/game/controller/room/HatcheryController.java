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
package toniarts.openkeeper.game.controller.room;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.filter.FieldFilter;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.ChickenGenerator;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.ObjectsController;
import toniarts.openkeeper.game.controller.room.storage.RoomFoodControl;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * The hatchery
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class HatcheryController extends NormalRoomController implements IChickenGenerator {

    private final IGameTimer gameTimer;
    private RoomFoodControl roomFoodControl;

    public HatcheryController(EntityId entityId, EntityData entityData, IKwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController, IGameTimer gameTimer) {
        super(entityId, entityData, kwdFile, roomInstance, objectsController, ObjectType.FOOD);

        this.gameTimer = gameTimer;
        entityData.setComponent(entityId, new ChickenGenerator(gameTimer.getGameTime()));
    }

    @Override
    public void construct() {
        super.construct();

        roomFoodControl = addObjectControl(new RoomFoodControl(kwdFile, this, entityData, gameTimer) {

            @Override
            protected int getNumberOfAccessibleTiles() {
                return roomInstance.getCoordinates().size();
            }
        });
        takeInChickens();
    }

    /**
     * Every chicken standing on a Hatchery belongs to it: when the room's
     * objects are set up again, on being built, changed or taken over, DK II
     * gives the chickens on its tiles to the room's owner and puts those that
     * are in no room in the room, the free-range chickens and another
     * keeper's alike (DKII.EXE 0x4DEB70, called from 0x4DE5D0)
     */
    private void takeInChickens() {
        for (EntityId chicken : entityData.findEntities(
                new FieldFilter<>(ObjectComponent.class, "objectId", ObjectsController.OBJECT_CHICKEN_ID),
                ObjectComponent.class, Position.class)) {
            Position position = entityData.getComponent(chicken, Position.class);
            if (position == null || entityData.getComponent(chicken, Stored.class) != null) {
                continue;
            }
            Point tile = WorldUtils.vectorToPoint(position.position);
            if (roomInstance.hasCoordinate(tile)) {
                roomFoodControl.addItem(chicken, tile);
            }
        }
    }

    @Override
    protected boolean isFloorFurnitureEnabled() {
        // DKII creates coops through the Hatchery's own routine, without SPECIAL_TILES.
        return true;
    }

    @Override
    protected RoomObjectLayout getRoomObjectLayout() {
        return RoomObjectLayout.ISOLATED;
    }

    @Override
    public Point getEntranceCoordinate() {
        // TODO: Should be maybe random available point, where there are no coops
        return Utils.getRandomItem(roomInstance.getCoordinates());
    }

    @Override
    public double getLastSpawnTime() {
        return getEntityComponent(ChickenGenerator.class).lastSpawnTime;
    }

    @Override
    public boolean hasSpawned() {
        return getEntityComponent(ChickenGenerator.class).spawned;
    }

    @Override
    public void onSpawn(double time, EntityId spawnedEntityId) {
        entityData.setComponent(this.entityId, new ChickenGenerator(time, true));
        if (spawnedEntityId != null) {
            this.roomFoodControl.addItem(spawnedEntityId, start);
        }
    }

    @Override
    public void captured(short playerId) {
        super.captured(playerId);
        entityData.setComponent(entityId, new ChickenGenerator(gameTimer.getGameTime()));
        takeInChickens();
    }

    @Override
    protected void constructObjects() {
        super.constructObjects();

        // The only objects we have are coops, they don't seem to have any indicator etc. so to avoid hard coding an ID and to allow maximum editability, all objects in the room generate chickens
        for (EntityId obj : floorFurniture) {
            entityData.setComponent(obj, new ChickenGenerator());
        }
    }


}
