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
import toniarts.openkeeper.utils.Point;

/**
 * A single hardcoded visual effect tied to a room at a specific tile - e.g.
 * the hero gate's gem holder swirl. Not a generic per-room effect list,
 * matching how the original game hardcodes a couple of specific effect ids
 * directly rather than driving them off room data.
 */
public final class RoomEffect implements EntityComponent {

    public int effectId;
    public Point location;

    public RoomEffect() {
        // For serialization
    }

    public RoomEffect(int effectId, Point location) {
        this.effectId = effectId;
        this.location = location;
    }
}
