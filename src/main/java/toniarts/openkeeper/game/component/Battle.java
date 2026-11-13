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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityComponent;
import java.util.HashSet;
import java.util.Set;

/**
 * A battle, a group of creatures fighting near each other. Each player with
 * creatures in the battle sees it on the Battle tab.<br>
 * The centre is kept here and not in a {@link Position}, so the battle isn't
 * found as a thing on the map.
 */
public final class Battle implements EntityComponent {

    public Vector3f position;
    public Set<Short> playerIds;

    public Battle() {
        // For serialization
    }

    public Battle(Vector3f position, Set<Short> playerIds) {
        this.position = position;
        this.playerIds = new HashSet<>(playerIds);
    }

}
