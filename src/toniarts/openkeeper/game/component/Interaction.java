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
 * This entity tells what actions if any are possible for an entity (hmm, not
 * sure if this is a good ES design...)
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Interaction implements EntityComponent {

    public boolean interactable;
    public boolean slappable;
    public boolean pickUppable;
    public boolean canBeDroppedOnAnyLand;

    public Interaction() {
        // For serialization
    }

    public Interaction(boolean interactable, boolean slappable, boolean pickUppable, boolean canBeDroppedOnAnyLand) {
        this.interactable = interactable;
        this.slappable = slappable;
        this.pickUppable = pickUppable;
        this.canBeDroppedOnAnyLand = canBeDroppedOnAnyLand;
    }

}
