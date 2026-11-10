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
package toniarts.openkeeper.game.component;

import com.simsilica.es.EntityComponent;
import toniarts.openkeeper.utils.Point;

/**
 * Just a tagging component for imp generators
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class ImpGenerator implements EntityComponent {

    public Point entrance;

    /**
     * Last entity spawn time, in game time
     */
    public double lastSpawnTime;

    public ImpGenerator() {
        // For serialization
    }

    public ImpGenerator(Point entrance, double lastSpawnTime) {
        this.entrance = entrance;
        this.lastSpawnTime = lastSpawnTime;
    }
}
