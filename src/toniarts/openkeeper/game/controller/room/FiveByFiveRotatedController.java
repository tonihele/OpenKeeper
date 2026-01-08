/*
 * Copyright (C) 2014-2015 OpenKeeper
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

import com.jme3.math.FastMath;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import java.util.Map;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.DungeonHeart;
import toniarts.openkeeper.game.component.ImpGenerator;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.storage.RoomGoldControl;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Variable;

/**
 * Constructs 5 by 5 "rotated" buildings. As far as I know, only Dungeon Heart
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class FiveByFiveRotatedController extends AbstractRoomController implements ICreatureEntrance {

    public static final short OBJECT_HEART_ID = 13;
    public static final short OBJECT_ARCHES_ID = 86;
    public static final short OBJECT_BIG_STEPS_ID = 88;
    public static final short OBJECT_PLUG_ID = 96;

    private final IGameTimer gameTimer;
    private final int maxGold;

    public FiveByFiveRotatedController(EntityId entityId, EntityData entityData, IKwdFile kwdFile,
            RoomInstance roomInstance, IObjectsController objectsController,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings, IGameTimer gameTimer) {
        super(entityId, entityData, kwdFile, roomInstance, objectsController, ObjectType.GOLD);

        this.gameTimer = gameTimer;
        maxGold = (int) gameSettings.get(Variable.MiscVariable.MiscType.MAX_GOLD_PER_DUNGEON_HEART_TILE).getValue();

        entityData.setComponent(entityId, new DungeonHeart());
    }

    @Override
    public void destroy() {
        super.destroy();

        entityData.removeComponent(entityId, ImpGenerator.class);
    }

    @Override
    public void construct() {
        super.construct();

        addObjectControl(new RoomGoldControl(kwdFile, this, entityData, gameTimer, objectsController) {

            @Override
            protected int getGoldPerObject() {
                return maxGold;
            }

            @Override
            protected int getNumberOfAccessibleTiles() {
                return 16;
            }
        });

        // Init the spawn point
        if (!isDestroyed()) {
            entityData.setComponent(entityId, new ImpGenerator(start, Double.MIN_VALUE));
        }
    }

    @Override
    protected void constructObjects() {

        // We contruct the Dungeon Heart here
        // Because of physics and whatnot, the object are on server, so what about the creation animation?
        // The creation animation should be on the client perhaps... We don't care about it...
        Point center = roomInstance.getCenter();
        if (isDestroyed()) {
            constructDestroyed(center);
        } else {
            constructNonDestroyed(center);
        }
    }

    private void constructNonDestroyed(Point center) {
        floorFurniture.add(objectsController.loadObject(OBJECT_HEART_ID, roomInstance.getOwnerId(), center.x, center.y));

        // Construct the plug
        floorFurniture.add(objectsController.loadObject(OBJECT_PLUG_ID, roomInstance.getOwnerId(), center.x, center.y));

        // The arches
        floorFurniture.add(objectsController.loadObject(OBJECT_ARCHES_ID, roomInstance.getOwnerId(), center.x, center.y));

        // The steps between the arches
        floorFurniture.add(objectsController.loadObject(OBJECT_BIG_STEPS_ID, roomInstance.getOwnerId(), center.x, center.y));
        floorFurniture.add(objectsController.loadObject(OBJECT_BIG_STEPS_ID, roomInstance.getOwnerId(), center.x, center.y, -FastMath.TWO_PI / 3));
        floorFurniture.add(objectsController.loadObject(OBJECT_BIG_STEPS_ID, roomInstance.getOwnerId(), center.x, center.y, FastMath.TWO_PI / 3));
    }

    private void constructDestroyed(Point center) {
        // TODO:
    }

    @Override
    public boolean isTileAccessible(Integer fromX, Integer fromY, int toX, int toY) {

        // The center 3x3 is not accessible
        Point roomPoint = roomInstance.worldCoordinateToLocalCoordinate(toX, toY);
        return ((roomPoint.x == 0 || roomPoint.x == 4) || (roomPoint.y == 0 || roomPoint.y == 4));
    }

    @Override
    public Point getEntranceCoordinate() {
        return getEntityComponent(ImpGenerator.class).entrance;
    }

    @Override
    public double getLastSpawnTime() {
        return getEntityComponent(ImpGenerator.class).lastSpawnTime;
    }

    @Override
    public void onSpawn(double time, EntityId entityId) {
        entityData.setComponent(entityId, new ImpGenerator(getEntityComponent(ImpGenerator.class).entrance, time));
    }

    @Override
    public void captured(short playerId) {
        super.captured(playerId);

        entityData.setComponent(entityId, new ImpGenerator(getEntityComponent(ImpGenerator.class).entrance, Double.MIN_VALUE));
    }

}
