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
package toniarts.openkeeper.game.task.creature;

import com.jme3.math.Vector2f;
import toniarts.openkeeper.game.task.AbstractCapacityCriticalRoomTask;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.object.ObjectControl;
import toniarts.openkeeper.world.room.GenericRoom;
import toniarts.openkeeper.world.room.control.RoomObjectControl;

/**
 * Claim a lair for a creature
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ClaimLair extends AbstractCapacityCriticalRoomTask {

    private boolean executed = false;

    public ClaimLair(WorldState worldState, int x, int y, short playerId, GenericRoom room, TaskManager taskManager) {
        super(worldState, x, y, playerId, room, taskManager);
    }

    @Override
    public boolean isValid(CreatureControl creature) {
        if (!executed) {
            return super.isValid(creature);
        }
        return false;
    }

    @Override
    public Vector2f getTarget(CreatureControl creature) {
        return new Vector2f(getTaskLocation().x + 0.5f, getTaskLocation().y + 0.5f);
    }

    @Override
    protected String getStringId() {
        return "2627";
    }

    @Override
    protected GenericRoom.ObjectType getRoomObjectType() {
        return GenericRoom.ObjectType.LAIR;
    }

    @Override
    public void executeTask(CreatureControl creature) {

        // Create a lair
        RoomObjectControl control = getRoomObjectControl();
        if ((int) control.addItem(1, getTaskLocation(), worldState.getThingLoader(), creature) == 0) {
            creature.setCreatureLair((ObjectControl) control.getItems(getTaskLocation()).iterator().next());
        }

        // This is a one timer
        executed = true;
    }

    @Override
    public ArtResource getTaskAnimation(CreatureControl creature) {
        return null;
    }

    @Override
    public String getTaskIcon() {
        return "Textures/GUI/moods/SJ-Rest.png";
    }
}
