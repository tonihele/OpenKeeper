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
import toniarts.openkeeper.game.component.CasinoBigWinner;
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
 * The big winner of the Casino collects its jackpot from a Treasury or the
 * Dungeon Heart. The keeper pays what gold he has, and the creature keeps what
 * it can carry. Whatever the keeper couldn't pay, the creature comes back for
 * later.
 */
public final class CollectWinnings extends AbstractRoomTask {

    private final EntityData entityData;
    private final IGameWorldController gameWorldController;
    private boolean executed = false;

    public CollectWinnings(final INavigationService navigationService, final IMapController mapController, Point p,
            short playerId, IRoomController room, EntityData entityData, IGameWorldController gameWorldController) {
        super(navigationService, mapController, p, playerId, room);

        this.entityData = entityData;
        this.gameWorldController = gameWorldController;
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return !executed && !room.isRemoved() && room.getOwnerId() == playerId && !room.isDestroyed()
                && entityData.getComponent(creature.getEntityId(), CasinoBigWinner.class) != null;
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
        CasinoBigWinner bigWinner = entityData.getComponent(creature.getEntityId(), CasinoBigWinner.class);
        int paid = bigWinner.winnings - gameWorldController.substractGold(bigWinner.winnings, playerId);
        int kept = Math.min(paid, creature.getMaxGold() - creature.getGold());
        if (kept > 0) {
            creature.addGold(kept);
        }

        // Once it has all of its winnings, it is no longer the big winner
        if (paid == bigWinner.winnings) {
            entityData.removeComponent(creature.getEntityId(), CasinoBigWinner.class);
        } else {
            entityData.setComponent(creature.getEntityId(), new CasinoBigWinner(bigWinner.timeLeft, bigWinner.winnings - paid, bigWinner.slapCount));
        }

        // This is a one timer
        executed = true;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.COLLECT_WINNINGS;
    }

}
