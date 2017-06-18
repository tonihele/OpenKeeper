/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.game.entity;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityComponent;

/**
 * Simple entity position class
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Position implements EntityComponent {

    public int rotation; // We are essentially 2D game
    public Vector3f position;

    public Position() {
        // For serialization
    }

    public Position(int rotation, Vector3f position) {
        this.rotation = rotation;
        this.position = position;
    }

}
