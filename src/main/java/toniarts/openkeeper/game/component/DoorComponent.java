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
 * A base door component
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class DoorComponent implements EntityComponent {

    public short doorId;
    public boolean locked;
    public boolean blueprint;

    /**
     * A built secret door that its enemies haven't discovered yet
     */
    public boolean secret;

    public DoorComponent() {
        // For serialization
    }

    public DoorComponent(short doorId, boolean locked, boolean blueprint) {
        this(doorId, locked, blueprint, false);
    }

    public DoorComponent(short doorId, boolean locked, boolean blueprint, boolean secret) {
        this.doorId = doorId;
        this.locked = locked;
        this.blueprint = blueprint;
        this.secret = secret;
    }

    @Override
    public String toString() {
        return "DoorComponent{" + "doorId=" + doorId + ", locked=" + locked + ", blueprint=" + blueprint + ", secret=" + secret + '}';
    }
}
