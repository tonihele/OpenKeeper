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
package toniarts.openkeeper.game.controller;

import com.jme3.math.Vector2f;
import toniarts.openkeeper.game.map.IMapTileInformation;

/**
 * A small interface telling that the control (creature, object...) is
 * interactive
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IInteractiveControl {

    /**
     * Signifies a state or result for dropping the item
     */
    public enum DroppableStatus {

        DROPPABLE,
        NOT_DROPPABLE,
        /**
         * i.e. evict a creature
         */
        DESTRUCTIBLE_DROP
    }

    /**
     * Is the control pickuppable by the keeper
     *
     * @param playerId the player who tries to pick the control up
     * @return {@code true} if the player can pick the control up
     */
    public boolean isPickable(short playerId);

    /**
     * Is the control interactable by the keeper
     *
     * @param playerId the player who tries to interact with the control
     * @return {@code true} if the player can interact with the control
     */
    public boolean isInteractable(short playerId);

    /**
     * The control has been picked up
     *
     * @param playerId the player who picks up the control
     * @return self, for chaining
     */
    public IInteractiveControl pickUp(short playerId);

    /**
     * Get the result of dropping the control on a tile. A result what would
     * happen if...
     *
     * @param tile the tile to be tested against
     * @param playerId the player who wants to drop
     * @return the result if the control were to be dropped here
     */
    public DroppableStatus getDroppableStatus(IMapTileInformation tile, short playerId);

    /**
     * Drop the control on the tile
     *
     * @param tile tile to drop to
     * @param coordinates real world coordinates inside
     * @param control if there is already an interactive control at the position
     */
    public void drop(IMapTileInformation tile, Vector2f coordinates, IInteractiveControl control);

    /**
     * Interact with the control. Typically alternative mouse button pressed
     *
     * @param playerId the player who wants to interact
     * @return the interaction result, {@code true} if the interaction was
     * successful
     */
    public boolean interact(short playerId);

    /**
     * Get the owner ID of the interactable thing
     *
     * @return the player ID who owns this
     */
    public short getOwnerId();
}
