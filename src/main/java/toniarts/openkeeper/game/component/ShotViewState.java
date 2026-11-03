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
 * A shot that the view should draw, i.e. a keeper spell or a creature spell
 * flying or striking. The shot data tells everything else, the mesh, the
 * effects and so on. The entity lives as long as the shot does.
 */
public final class ShotViewState implements EntityComponent {

    public short shotId;

    public ShotViewState() {
        // For serialization
    }

    public ShotViewState(short shotId) {
        this.shotId = shotId;
    }

}
