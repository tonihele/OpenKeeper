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
package toniarts.openkeeper.view.control;

import com.jme3.math.Vector2f;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Terrain;

/**
 * A small interface telling that the control (creature, object...) is
 * interactive
 *
 * @param <T> The type of entity
 * @param <S> The animation type of the entity
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IEntityViewControl<T, S> extends Control {

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
     * The control has been picked up
     *
     * @param playerId the player who picks up the control
     */
    public void pickUp(short playerId);

    /**
     * Is the control interactable by the keeper
     *
     * @param playerId the player who tries to interact with the control
     * @return {@code true} if the player can interact with the control
     */
    public boolean isInteractable(short playerId);

    /**
     * Interact with the control
     *
     * @param playerId the player who wants to interact
     */
    public void interact(short playerId);

    /**
     * Is the control slappable by the keeper
     *
     * @param playerId the player who tries to slap the control
     * @return {@code true} if the player can slap the control
     */
    public boolean isSlappable(short playerId);

    /**
     * Slap the control
     *
     * @param playerId the player who wants to slap
     */
    public void slap(short playerId);

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
     * @param terrain the terrain
     * @param playerId the player who wants to drop
     * @return the result if the control were to be dropped here
     */
    public DroppableStatus getDroppableStatus(MapTile tile, Terrain terrain, short playerId);

    /**
     * Drop the control on the tile
     *
     * @param tile tile to drop to
     * @param coordinates real world coordinates inside
     * @param control if there is already an
     * {@link toniarts.openkeeper.view.control.IEntityViewControl} at the
     * position
     */
    public void drop(MapTile tile, Vector2f coordinates, IEntityViewControl control);

    /**
     * When the control is hovered upon
     *
     * @param playerId the player
     */
    public void onHover(short playerId);

    /**
     * When the control is hovered upon. The hovering starts
     *
     * @param playerId the player
     */
    public void onHoverStart(short playerId);

    /**
     * When the control is no longer hovered on
     *
     * @param playerId the player
     */
    public void onHoverEnd(short playerId);

    /**
     * Get the owner ID of the entity
     *
     * @return the player ID of the owner
     */
    public short getOwnerId();

    /**
     * Get the entity ID
     *
     * @return the entity ID
     */
    public EntityId getEntityId();

    public void setTargetState(S state);

    public T getDataObject();

}
