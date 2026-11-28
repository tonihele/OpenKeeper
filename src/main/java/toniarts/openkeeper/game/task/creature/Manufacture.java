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

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.ManufactureOrders;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.controller.player.PlayerManufactureControl;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.logic.CreatureViewSystem;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractCapacityCriticalRoomTask;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Manufacture traps and doors for the keeper's blueprints in the workshop. The
 * finished trap or door is a crate in the workshop.
 */
public final class Manufacture extends AbstractCapacityCriticalRoomTask {

    private final PlayerManufactureControl manufactureControl;
    private final IObjectsController objectsController;
    private final int manufactureRounds = 6 + Utils.getRandom().nextInt(7);
    private Vector2f workPosition;

    public Manufacture(final INavigationService navigationService, final IMapController mapController, Point p, short playerId, IRoomController room,
            TaskManager taskManager, PlayerManufactureControl manufactureControl, IObjectsController objectsController) {
        super(navigationService, mapController, p, playerId, room, taskManager);

        this.manufactureControl = manufactureControl;
        this.objectsController = objectsController;
    }

    @Override
    public boolean isValid(ICreatureController creature) {

        // The workshop is there and has room for another crate, without a creature only that is asked
        return super.isValid(creature)
                && (creature == null || creature.getManufacturePerTick() > 0)
                && manufactureControl.getOrder() != null;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {

        // The tile the creature stands on
        return WorldUtils.pointToVector2f(WorldUtils.vectorToPoint(getWorkPosition(creature)));
    }

    @Override
    public boolean isFaceTarget() {
        return true;
    }

    @Override
    public Vector2f getWorkPosition(ICreatureController creature) {

        // DK II puts the creature half the furniture's width plus half its own from the furniture. On the floor the
        // direction is a random number seeded by the tile, so a piece is always worked from the same side. At a piece
        // on the wall the creature stands into the room, a sixteenth of a turn off square (0x470110). Picked once,
        // the position is asked every tick
        if (workPosition == null) {
            IObjectController furniture = getFurniture(objectsController);
            if (furniture == null) {
                workPosition = WorldUtils.pointToVector2f(getTaskLocation());
            } else {
                float angle = isOnWall(furniture) ? getAngleIntoRoom(furniture) - FastMath.PI / 8
                        : AbstractRoomController.getTileRandom(getTaskLocation(), 2048) * FastMath.TWO_PI / 2048;
                workPosition = getPositionAt(furniture.getPosition(), angle, getDistanceToFurniture(furniture, creature));
            }
        }
        return workPosition;
    }

    @Override
    protected ObjectType getRoomObjectType() {
        return ObjectType.CRATE;
    }

    @Override
    public boolean moveToOtherSpot(ICreatureController creature, float executionDuration) {

        // DK II works for 6 to 12 rounds of the work animation at a piece of furniture (0x470470), then goes to the
        // closest piece nobody is at if there is one (0x470560). Without the animation the creature stays where it is
        double roundTime = CreatureController.getAnimationTime(creature.getCreature(),
                CreatureViewSystem.getManufactureAnimation(creature.getCreature()));
        return roundTime > 0 && executionDuration >= manufactureRounds * roundTime
                && taskManager.assignFreeRoomTask(creature, getRoom(), ObjectType.MANUFACTURER);
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        // DK II adds the creature's manufacture points per tick on every game tick
        ManufactureOrders.Order order = manufactureControl.getOrder();
        if (order == null || !manufactureControl.manufacture(creature.getManufacturePerTick(), order.manufToBuild())) {
            return;
        }

        // The trap or door is done. Like DK II (0x4F5240) the crate is nobody's until the workshop takes it, and without
        // room in the workshop it stays on the floor
        EntityId crate = objectsController.addRoomCrate(order.crateObjectId(), Player.NEUTRAL_PLAYER_ID, getTaskLocation().x, getTaskLocation().y,
                order.type(), order.typeId());
        getRoomObjectControl().addItem(crate, null);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.MANUFACTURE;
    }

}
