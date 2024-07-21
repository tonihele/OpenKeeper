/*
 * Copyright (C) 2014-2024 OpenKeeper
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
import com.simsilica.es.EntityId;

/**
 * Handles shots. Shots are spells and weapons cast by traps, creatures and
 * keepers
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IShotsController {

    /**
     * Creates a shot
     *
     * @param shotTypeId shot type to create
     * @param shotData1 arbitrary value, interpreted per shot
     * @param shotData2 arbitrary value, interpreted per shot
     * @param playerId owher of the shot
     * @param position 2D coordinate of the shot origin
     * @param target shot target, can be null
     */
    public void createShot(short shotTypeId, int shotData1, int shotData2, short playerId, Vector2f position, EntityId target);

}
