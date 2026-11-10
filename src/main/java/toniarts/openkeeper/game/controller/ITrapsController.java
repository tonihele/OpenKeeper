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
package toniarts.openkeeper.game.controller;

import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.controller.trap.ITrapController;
import toniarts.openkeeper.utils.Point;

/**
 * Handles traps
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface ITrapsController extends IEntityWrapper<ITrapController> {

    /**
     * Places a trap blueprint. The trap isn't armed, it waits for its crate
     * from the workshop
     *
     * @param trapId the trap
     * @param tile the tile to place it on
     * @param ownerId the owning keeper
     * @return the trap entity
     */
    public EntityId addTrapBlueprint(short trapId, Point tile, short ownerId);

}
