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
 * The low mana warnings of a keeper, on the player's entity while his mana is
 * below the low mana warning threshold. The counters are in seconds and may go
 * negative, a warning or an imp choice that is due waits for its turn.
 */
public final class LowMana implements EntityComponent {

    /**
     * The next warning to give, 1 to 4. 0 = the warnings have not started yet,
     * 5 = the warnings are over and the keeper's imps are popped
     */
    public int stage;

    /**
     * Seconds to the next warning
     */
    public int secondsToWarning;

    /**
     * Seconds to choosing the next imp to pop
     */
    public int secondsToImpChoice;

    public LowMana() {
        // For serialization
    }

    public LowMana(int stage, int secondsToWarning, int secondsToImpChoice) {
        this.stage = stage;
        this.secondsToWarning = secondsToWarning;
        this.secondsToImpChoice = secondsToImpChoice;
    }

}
