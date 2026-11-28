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
import com.simsilica.es.EntityId;

/**
 * The enemy door a creature is breaking. DK II gives the door to the creature
 * as its attack target (0x48F390), and once the creature goes for it in melee
 * it keeps hitting it, the fight state doesn't pick its attack again in melee
 * (0x45F030)
 */
public final class DoorTarget implements EntityComponent {

    public EntityId door;

    /**
     * The creature hits the door in melee
     */
    public boolean melee;

    public DoorTarget() {
        // For serialization
    }

    public DoorTarget(EntityId door, boolean melee) {
        this.door = door;
        this.melee = melee;
    }

}
