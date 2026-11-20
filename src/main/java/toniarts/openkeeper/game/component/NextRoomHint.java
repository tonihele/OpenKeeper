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
 * The room the level script tells a keeper to build next (the "Display Next
 * Room Type" action). Lives on a per-player entity together with {@link Owner};
 * the HUD pulses that room's button.
 */
public final class NextRoomHint implements EntityComponent {

    /**
     * Room id to hint, 0 = no hint
     */
    public short roomId;

    public NextRoomHint() {
        // For serialization
    }

    public NextRoomHint(short roomId) {
        this.roomId = roomId;
    }

}
