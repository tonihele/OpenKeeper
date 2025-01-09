/*
 * Copyright (C) 2014-2021 OpenKeeper
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
import java.awt.Point;

/**
 * A base room component. This entity is a room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class RoomComponent implements EntityComponent {

    public short roomId;
    public boolean destroyed = false;

    /**
     * Room center, for convenience
     */
    public Point location;

    public RoomComponent() {
        // For serialization
    }

    public RoomComponent(RoomComponent roomComponent) {
        roomId = roomComponent.roomId;
        destroyed = roomComponent.destroyed;
        location = roomComponent.location;
    }

    public RoomComponent(short roomId, boolean destroyed, Point location) {
        this.roomId = roomId;
        this.destroyed = destroyed;
    }

}
