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
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import java.util.List;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.ManufactureOrders;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerManufactureControl;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.game.event.GameEvent.ThingFlag;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractCapacityCriticalRoomTask;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Manufacture traps and doors for the keeper's blueprints in the workshop. The
 * finished trap or door is a crate in the workshop.
 */
public final class Manufacture extends AbstractCapacityCriticalRoomTask {

    private final PlayerManufactureControl manufactureControl;
    private final IObjectsController objectsController;
    private final GameEventService gameEventService;
    private Vector2f workPlace;

    public Manufacture(final INavigationService navigationService, final IMapController mapController, Point p, short playerId, IRoomController room,
            TaskManager taskManager, PlayerManufactureControl manufactureControl, IObjectsController objectsController,
            GameEventService gameEventService) {
        super(navigationService, mapController, p, playerId, room, taskManager);

        this.manufactureControl = manufactureControl;
        this.objectsController = objectsController;
        this.gameEventService = gameEventService;
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

        // Next to the workbench, it takes up the middle of its tile. Looked up once, the target is asked every tick
        if (workPlace == null) {
            workPlace = getAccessibleTargetNextToLocation(creature);
        }
        return workPlace;
    }

    @Override
    public boolean isFaceTarget() {
        return true;
    }

    @Override
    public Vector2f getWorkPosition(ICreatureController creature) {

        // Halfway from the middle of the tile next to the workbench to the workbench
        Vector2f target = getTarget(creature);
        if (target == null) {
            return null;
        }
        return getWorkPosition(target, getTaskLocation());
    }

    static Vector2f getWorkPosition(Vector2f nextToWorkbench, Point workbench) {
        return WorldUtils.pointToVector2f(workbench).addLocal(nextToWorkbench).multLocal(0.5f);
    }

    @Override
    protected ObjectType getRoomObjectType() {
        return ObjectType.CRATE;
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

        // Also like DK II, the crate plays the made sound from its trap's or door's bank and the workshop's keeper is
        // told what was made. The room's first effect goes off over the workshop once for every 9 tiles, at least
        // once, a tile and a half above the floor, and its second effect at the crate
        Vector3f cratePosition = WorldUtils.pointToVector3f(getTaskLocation()).setY(WorldUtils.FLOOR_HEIGHT);
        gameEventService.emit(GameEventType.WORKSHOP_ITEM_MADE, order.type() == ResearchableType.DOOR ? ThingFlag.DOOR : ThingFlag.TRAP,
                order.typeId(), cratePosition);
        gameEventService.emitToPlayer(GameEventType.WORKSHOP_ITEM_MADE_MESSAGE, order.crateObjectId(), playerId);
        List<Integer> effects = getRoom().getRoom().getEffects();
        List<Point> tiles = getRoom().getRoomInstance().getCoordinates();
        if (effects.get(0) != 0) {
            for (int i = 0; i < Math.max(1, tiles.size() / 9); i++) {
                Point tile = tiles.get(FastMath.nextRandomInt(0, tiles.size() - 1));
                gameEventService.emitEffect(effects.get(0), WorldUtils.pointToVector3f(tile).setY(WorldUtils.FLOOR_HEIGHT + 1.5f), 0);
            }
        }
        if (effects.get(1) != 0) {
            gameEventService.emitEffect(effects.get(1), cratePosition, 0);
        }
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.MANUFACTURE;
    }

}
