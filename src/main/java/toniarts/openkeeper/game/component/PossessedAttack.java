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
 * Attack intent of a possessed creature, as given by its keeper. The swing
 * itself happens on the game logic thread.
 */
public final class PossessedAttack implements EntityComponent {

    /**
     * Whether the attack button is still held down. A released attack that
     * hasn't swung yet still swings once.
     */
    public boolean held;
    public double pressTime;

    public PossessedAttack() {
        // For serialization
    }

    public PossessedAttack(boolean held, double pressTime) {
        this.held = held;
        this.pressTime = pressTime;
    }

}
