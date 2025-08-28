/*
 * Copyright (C) 2014-2025 OpenKeeper
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

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityComponent;

/**
 *
 * @author ArchDemon
 */
public final class TileBuildOrSell implements EntityComponent {

    public Vector2f start;

    public Vector2f end;

    public short playerId;

    public short roomId;

    public TileBuildOrSell() {
        // For serialization
    }

    public TileBuildOrSell(Vector2f start, Vector2f end, short playerId) {
        this(start, end, playerId, (short) 0);
    }

    public TileBuildOrSell(Vector2f start, Vector2f end, short playerId, short roomId) {
        this.start = start;
        this.end = end;
        this.playerId = playerId;
        this.roomId = roomId;
    }
}
