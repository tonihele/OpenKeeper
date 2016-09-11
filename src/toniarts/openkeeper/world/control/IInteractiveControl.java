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
package toniarts.openkeeper.world.control;

import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.world.TileData;

/**
 * A small interface telling that the control (creature, object...) is
 * interactive
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IInteractiveControl extends Control {

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
     * Get control tooltip
     *
     * @param playerId the player who is asking
     * @return the tooltip string
     */
    public String getTooltip(short playerId);

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
     * Get the in hand cursor
     *
     * @return cursor type when the control is held by the keeper
     */
    public CursorFactory.CursorType getInHandCursor();

    /**
     * Get in hand mesh, when the keeper dangles the control in hand
     *
     * @return the in hand mesh/animation
     */
    public ArtResource getInHandMesh();
    
    /**
     * Get in hand icon, when the keeper dangles the control in hand
     *
     * @return the in hand icon
     */
    public ArtResource getInHandIcon();

    /**
     * Get the control spatial
     *
     * @return the spatial
     */
    public Spatial getSpatial();

    /**
     * Get the result of dropping the control on a tile. A result what would
     * happen if...
     *
     * @param tile the tile to be tested against
     * @return the result if the control were to be dropped here
     */
    public DroppableStatus getDroppableStatus(TileData tile);

    /**
     * Drop the control on the tile
     *
     * @param tile tile to drop to
     */
    public void drop(TileData tile);

    /**
     * Interact with the control. Typically alternative mouse button pressed
     *
     * @param playerId the player who wants to interact
     * @return the interaction result, {@code true} if the interaction was
     * successful
     */
    public boolean interact(short playerId);

    /**
     * When the control is hovered upon
     */
    public void onHover();

}
