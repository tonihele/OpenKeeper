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
package toniarts.openkeeper.game.task.creature;

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import toniarts.openkeeper.game.component.CreaturePay;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractRoomTask;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Collect the pay the creature is owed from a gold room of its keeper. The
 * creature takes what it is owed, or what the room has, and keeps half of it
 */
public final class CollectPay extends AbstractRoomTask {

    private final IGameWorldController gameWorldController;
    private final EntityData entityData;
    private boolean executed = false;

    public CollectPay(final INavigationService navigationService, final IMapController mapController, Point p, short playerId,
            final IRoomController room, final IGameWorldController gameWorldController, final EntityData entityData) {
        super(navigationService, mapController, p, playerId, room);
        this.gameWorldController = gameWorldController;
        this.entityData = entityData;
    }

    @Override
    public boolean isValid(ICreatureController creature) {

        // A full gold room is as good as any, unlike for the other room tasks
        return !executed && !room.isRemoved() && room.getOwnerId() == playerId && !room.isDestroyed();
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation());
    }

    @Override
    protected ObjectType getRoomObjectType() {
        return ObjectType.GOLD;
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        CreaturePay pay = entityData.getComponent(creature.getEntityId(), CreaturePay.class);
        if (pay != null) {
            int unpaid = gameWorldController.substractGold(pay.unpaid, playerId, getTaskLocation());
            int collected = pay.unpaid - unpaid;
            if (unpaid > 0) {

                // Coming away with nothing counts as a failed try
                entityData.setComponent(creature.getEntityId(), new CreaturePay(unpaid,
                        collected > 0 ? pay.attempts : pay.attempts + 1, pay.nextAttemptTime, false));
            } else {
                entityData.removeComponent(creature.getEntityId(), CreaturePay.class);
            }

            // The creature keeps half of its pay, up to what it can carry
            Gold gold = entityData.getComponent(creature.getEntityId(), Gold.class);
            if (gold != null && collected > 1) {
                entityData.setComponent(creature.getEntityId(), new Gold(Math.min(gold.gold + collected / 2, gold.maxGold), gold.maxGold));
            }
        }

        // This is a one timer
        executed = true;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.COLLECT_PAY;
    }

}
