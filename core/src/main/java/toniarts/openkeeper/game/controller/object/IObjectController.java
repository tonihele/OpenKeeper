/*
 * Copyright (C) 2014-2018 OpenKeeper
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
package toniarts.openkeeper.game.controller.object;

import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;

/**
 * Controls game object entities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IObjectController extends IEntityController {

    public int getPickUpPriority();

    public AbstractRoomController.ObjectType getType();

    public boolean isStoredInRoom();

    public boolean isPickableByPlayerCreature(short playerId);

    public boolean isHaulable();

    /**
     * Gives the object to the creature, or the creature picks up the object
     *
     * @param creature the creature interacting
     * @return returns true if the object was consumed in the process
     */
    public boolean creaturePicksUp(ICreatureController creature);

}
