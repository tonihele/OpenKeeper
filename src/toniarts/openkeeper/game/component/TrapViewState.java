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
package toniarts.openkeeper.game.component;

import com.simsilica.es.EntityComponent;

/**
 * Determines that the entity should be viewed as an trap. Visual presentation
 * only.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TrapViewState implements EntityComponent {

    public short trapId;
    public boolean bluebrint;

    public TrapViewState() {
        // For serialization
    }

    public TrapViewState(short trapId, boolean bluebrint) {
        this.trapId = trapId;
        this.bluebrint = bluebrint;
    }
}
