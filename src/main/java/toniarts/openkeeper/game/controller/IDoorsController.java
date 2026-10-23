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
import java.util.OptionalInt;
import toniarts.openkeeper.game.controller.door.IDoorController;
import toniarts.openkeeper.utils.Point;

/**
 * Handles doors
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IDoorsController extends IEntityWrapper<IDoorController> {

    /**
     * Places a door blueprint. The door isn't built, it waits for its crate
     * from the workshop
     *
     * @param doorId the door
     * @param tile the tile to place it on
     * @param ownerId the owning keeper
     * @return the door entity
     */
    public EntityId addDoorBlueprint(short doorId, Point tile, short ownerId);

    /**
     * Builds the door of a blueprint when its crate is delivered
     *
     * @param blueprint the blueprint, nothing happens if it is not a blueprint
     * (anymore)
     */
    public void buildDoor(EntityId blueprint);

    /**
     * Sells the keeper's door or door blueprint on the tile. The door is gone,
     * the caller returns the gold to the keeper
     *
     * @param tile the tile
     * @param playerId the selling keeper
     * @return the gold the sale returns, empty if the keeper has no door
     * there
     */
    public OptionalInt sellDoor(Point tile, short playerId);

}
