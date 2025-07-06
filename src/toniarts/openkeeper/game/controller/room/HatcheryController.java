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
import java.awt.Point;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.ChickenGenerator;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.storage.RoomFoodControl;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.utils.Utils;

/**
 * The hatchery
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class HatcheryController extends NormalRoomController implements IChickenGenerator {

    private final IGameTimer gameTimer;
    private RoomFoodControl roomFoodControl;

    public HatcheryController(EntityId entityId, EntityData entityData, KwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController, IGameTimer gameTimer) {
        super(entityId, entityData, kwdFile, roomInstance, objectsController, ObjectType.FOOD);

        this.gameTimer = gameTimer;
        entityData.setComponent(entityId, new ChickenGenerator(gameTimer.getGameTime()));
    }

    @Override
    public void construct() {
        super.construct();

        roomFoodControl = new RoomFoodControl(kwdFile, this, entityData, gameTimer) {

            @Override
            protected int getNumberOfAccessibleTiles() {
                return roomInstance.getCoordinates().size();
            }
        };
        addObjectControl(roomFoodControl);
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
    public void onSpawn(double time, EntityId entityId) {
        entityData.setComponent(entityId, new ChickenGenerator(time));
        if (entityId != null) {
            this.roomFoodControl.addItem(entityId, start);
        }
    }

    @Override
    public void captured(short playerId) {
        super.captured(playerId);
        entityData.setComponent(entityId, new ChickenGenerator(gameTimer.getGameTime()));
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
