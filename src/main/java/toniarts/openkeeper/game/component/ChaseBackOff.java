/*
 * Copyright (C) 2014-2019 OpenKeeper
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
 * A creature that gave up chasing an enemy running away leaves those that run
 * away alone for a while (DK II's creature +0x53B, cleared after 60 turns by
 * 0x488930)
 */
public final class ChaseBackOff implements EntityComponent {

    /**
     * The game time the creature gave up the chase
     */
    public double time;

    public ChaseBackOff() {
        // For serialization
    }

    public ChaseBackOff(double time) {
        this.time = time;
    }

}
