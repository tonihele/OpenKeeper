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
import toniarts.openkeeper.common.RoomInstance;
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

    private final IGameTimer gameTimer;
    private volatile PayOut payOut = PayOut.SMILES;

    public CasinoController(EntityId entityId, EntityData entityData, IKwdFile kwdFile,
            RoomInstance roomInstance, IObjectsController objectsController, IGameTimer gameTimer) {
        super(entityId, entityData, kwdFile, roomInstance, objectsController, ObjectType.GAMBLER);

        this.gameTimer = gameTimer;
    }

    @Override
    public void construct() {
        super.construct();

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
        return payOut;
    }

    /**
     * Sets how the casino pays out
     *
     * @param payOut the pay out setting
     */
    public void setPayOut(PayOut payOut) {
        this.payOut = payOut;
    }

}
