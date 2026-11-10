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
package toniarts.openkeeper.game.component;

import com.simsilica.es.EntityComponent;

/**
 * A burning Inferno, it burns the creatures around it every game tick for as
 * long as it lasts
 */
public final class InfernoArea implements EntityComponent {

    /**
     * Damage per burn
     */
    public int damage;

    /**
     * How far the fire reaches, in tiles from where it was cast
     */
    public int radius;

    /**
     * How many times it has burnt so far
     */
    public int burns;

    public InfernoArea() {
        // For serialization
    }

    public InfernoArea(int damage, int radius, int burns) {
        this.damage = damage;
        this.radius = radius;
        this.burns = burns;
    }

}
