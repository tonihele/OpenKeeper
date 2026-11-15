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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.HashSet;
import java.util.Set;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.Casino;
import toniarts.openkeeper.game.component.Casino.PayOut;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.storage.RoomGamblerControl;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * The casino
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CasinoController extends NormalRoomController {

    public static final short ROULETTE_TABLE_OBJECT_ID = 87;
    public static final short ROULETTE_WHEEL_OBJECT_ID = 30;
    /**
     * How far the roulette wheels are from the centre of their table, 2000 of
     * the 4096 units of a tile in the original
     */
    private static final float ROULETTE_WHEEL_OFFSET = 2000f / 4096;

    private final IGameTimer gameTimer;
    private final Set<EntityId> rouletteWheels = new HashSet<>();

    public CasinoController(EntityId entityId, EntityData entityData, IKwdFile kwdFile,
            RoomInstance roomInstance, IObjectsController objectsController, IGameTimer gameTimer) {
        super(entityId, entityData, kwdFile, roomInstance, objectsController, ObjectType.GAMBLER);

        this.gameTimer = gameTimer;

        if (getEntityComponent(Casino.class) == null) {
            setPayOut(PayOut.SMILES);
        }
    }

    @Override
    public void construct() {
        super.construct();

        addObjectControl(new RoomGamblerControl(kwdFile, this, entityData, gameTimer));
    }

    /**
     * As in the original (0x4EDFC0), every roulette table gets two roulette
     * wheels, one at each end. The gamblers spin them.
     */
    @Override
    protected void constructObjects() {
        removeRouletteWheels();
        super.constructObjects();

        for (EntityId furniture : floorFurniture) {
            ObjectComponent object = entityData.getComponent(furniture, ObjectComponent.class);
            if (object != null && object.objectId == ROULETTE_TABLE_OBJECT_ID) {
                Vector3f table = entityData.getComponent(furniture, Position.class).position;
                rouletteWheels.add(objectsController.loadObject(ROULETTE_WHEEL_OBJECT_ID, (short) 0,
                        table.add(0, 0, ROULETTE_WHEEL_OFFSET), 0));
                rouletteWheels.add(objectsController.loadObject(ROULETTE_WHEEL_OBJECT_ID, (short) 0,
                        table.add(0, 0, -ROULETTE_WHEEL_OFFSET), 0));
            }
        }
    }

    @Override
    public void remove() {
        removeRouletteWheels();
        super.remove();
    }

    private void removeRouletteWheels() {
        for (EntityId wheel : rouletteWheels) {
            entityData.removeEntity(wheel);
        }
        rouletteWheels.clear();
    }

    @Override
    protected RoomObjectLayout getRoomObjectLayout() {
        return RoomObjectLayout.ALLOW_DIAGONAL_NEIGHBOUR_ONLY;
    }

    /**
     * Gets how the casino pays out
     *
     * @return the pay out setting
     */
    public PayOut getPayOut() {
        return getEntityComponent(Casino.class).payOut;
    }

    /**
     * Sets how the casino pays out
     *
     * @param payOut the pay out setting
     */
    public void setPayOut(PayOut payOut) {
        entityData.setComponent(entityId, new Casino(payOut));
    }

}
