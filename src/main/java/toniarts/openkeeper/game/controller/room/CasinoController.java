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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CasinoPayOut;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.storage.RoomGamblerControl;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * The casino
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CasinoController extends NormalRoomController {

    /**
     * How the casino pays out. In DK II the keeper switches this with the pay
     * out adjuster in the casino.
     */
    public enum PayOut {

        /**
         * The gamblers win gold and cheer up, the keeper pays
         */
        SMILES,
        /**
         * The gamblers lose gold and get angrier, the keeper takes it
         */
        MONEY;

    }

    private static final short OBJECT_PAY_OUT_ADJUSTER_ID = 95;

    private final IGameTimer gameTimer;
    private EntityId payOutAdjuster;

    public CasinoController(EntityId entityId, EntityData entityData, IKwdFile kwdFile,
            RoomInstance roomInstance, IObjectsController objectsController, IGameTimer gameTimer) {
        super(entityId, entityData, kwdFile, roomInstance, objectsController, ObjectType.GAMBLER);

        this.gameTimer = gameTimer;
    }

    @Override
    public void construct() {
        super.construct();

        // A new casino pays out in smiles, like in DK II
        if (getEntityComponent(CasinoPayOut.class) == null) {
            setPayOut(PayOut.SMILES);
        }

        addObjectControl(new RoomGamblerControl(kwdFile, this, entityData, gameTimer));
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
        CasinoPayOut casinoPayOut = getEntityComponent(CasinoPayOut.class);
        return casinoPayOut != null ? casinoPayOut.payOut : PayOut.SMILES;
    }

    /**
     * Sets how the casino pays out
     *
     * @param payOut the pay out setting
     */
    public void setPayOut(PayOut payOut) {
        entityData.setComponent(entityId, new CasinoPayOut(payOut));
    }

    @Override
    public boolean switchFurniture(EntityId furniture) {
        ObjectComponent objectComponent = entityData.getComponent(furniture, ObjectComponent.class);
        if (objectComponent == null || objectComponent.objectId != OBJECT_PAY_OUT_ADJUSTER_ID) {
            return false;
        }

        // The pay out adjuster switches how the casino pays out, like in DK II
        setPayOut(getPayOut() == PayOut.SMILES ? PayOut.MONEY : PayOut.SMILES);
        return true;
    }

    @Override
    protected void constructObjects() {
        removePayOutAdjuster();
        super.constructObjects();

        // Like in DK II, the pay out adjuster takes the place of the first piece of furniture that has no
        // furniture in the row above it
        Map<Point, EntityId> furnitureByTile = new HashMap<>();
        for (EntityId piece : floorFurniture) {
            Position position = entityData.getComponent(piece, Position.class);
            if (position != null) {
                furnitureByTile.put(WorldUtils.vectorToPoint(position.position), piece);
            }
        }
        for (Point p : roomInstance.getCoordinates()) {
            EntityId piece = furnitureByTile.get(p);
            if (piece != null && !furnitureByTile.containsKey(new Point(p.x - 1, p.y - 1))
                    && !furnitureByTile.containsKey(new Point(p.x, p.y - 1))
                    && !furnitureByTile.containsKey(new Point(p.x + 1, p.y - 1))) {
                floorFurniture.remove(piece);
                entityData.removeEntity(piece);
                payOutAdjuster = objectsController.loadObject(OBJECT_PAY_OUT_ADJUSTER_ID, roomInstance.getOwnerId(), p.x, p.y);
                return;
            }
        }
    }

    @Override
    public void remove() {
        super.remove();

        removePayOutAdjuster();
    }

    private void removePayOutAdjuster() {
        if (payOutAdjuster != null) {
            entityData.removeEntity(payOutAdjuster);
            payOutAdjuster = null;
        }
    }

}
