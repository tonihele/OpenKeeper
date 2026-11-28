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
package toniarts.openkeeper.game.task.worker;

import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.RoomAttackMessages;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.TaskType;

/**
 * Claim a room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class ClaimRoomTask extends ClaimTileTask {

    private final IGameWorldController gameWorldController;

    public ClaimRoomTask(final INavigationService navigationService, final IMapController mapController,
            IGameWorldController gameWorldController, Point p, short playerId) {
        super(navigationService, mapController, p, playerId);

        this.gameWorldController = gameWorldController;
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {

        // The room's owner is told as the Imp starts on the room, like DK II
        // (DKII.EXE 0x466080, message 0x22). The message queue holds the
        // message, so the Imps that come after don't repeat it
        RoomAttackMessages messages = gameWorldController.getRoomAttackMessages();
        if (messages != null && getExecutionDuration(creature) == 0.0f) {
            messages.roomTileAttacked(mapController.getMapData().getTile(getTaskLocation()).getOwnerId(),
                    mapController.getRoomControllerByCoordinates(getTaskLocation()).getRoom(), getTaskLocation());
        }
        super.executeTask(creature, executionDuration);
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return mapController.isClaimableRoom(getTaskLocation(), playerId);
    }

    @Override
    public String toString() {
        return "Claim room at " + getTaskLocation();
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.CLAIM_ROOM;
    }

}
