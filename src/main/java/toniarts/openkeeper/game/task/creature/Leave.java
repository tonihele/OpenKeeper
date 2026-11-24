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
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTileTask;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Leaves the dungeon. Like in DK II, the creature walks to a portal, steps
 * into the middle of it and is gone for good. It falls through the floor there
 * like a creature dropped on the middle of the portal
 */
public final class Leave extends AbstractTileTask {

    private final IRoomController portal;
    private final EntityData entityData;

    public Leave(final INavigationService navigationService, final IMapController mapController, Point p, short playerId,
            IRoomController portal, EntityData entityData) {
        super(navigationService, mapController, p, playerId);

        this.portal = portal;
        this.entityData = entityData;
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return !portal.isRemoved();
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation());
    }

    @Override
    public Vector2f getWorkPosition(ICreatureController creature) {

        // Like in DK II (0x45A000), the creature goes a quarter of a tile into the middle of the portal, on its own side
        Vector2f middle = getTarget(creature);
        boolean fromNorth = creature.getPosition().z < middle.y;
        return new Vector2f(middle.x, middle.y + (fromNorth ? -0.25f : 0.25f));
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        entityData.setComponent(creature.getEntityId(), new CreatureFall(CreatureFall.FallType.PORTAL_DROP));
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.LEAVE;
    }

}
