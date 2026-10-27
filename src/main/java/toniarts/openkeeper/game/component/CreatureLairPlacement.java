/*
 * Copyright (C) 2014-2018 OpenKeeper
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
import com.simsilica.es.EntityId;

/**
 * Room context for a manual Lair drop. Landing restores CreatureAi with a
 * GO_TO_SLEEP target; that state's entry consumes and validates this context.
 */
public final class CreatureLairPlacement implements EntityComponent {

    /** Room where the creature was dropped; validated against its bed after get-up. */
    public EntityId roomId;

    /** Creates an empty component for entity serialization. */
    public CreatureLairPlacement() {
        // For serialization
    }

    /**
     * Records a manual drop into a Lair without starting rest before landing.
     *
     * @param roomId the Lair to validate when the creature gets up
     */
    public CreatureLairPlacement(EntityId roomId) {
        this.roomId = roomId;
    }
}
