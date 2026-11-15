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
 * The creature hit the jackpot in the Casino. It has the time left to collect
 * its winnings from the keeper. A keeper has only one big winner at a time.
 */
public final class CasinoBigWinner implements EntityComponent {

    public double timeLeft;
    public int winnings;
    public int slapCount;

    public CasinoBigWinner() {
        // For serialization
    }

    public CasinoBigWinner(double timeLeft, int winnings, int slapCount) {
        this.timeLeft = timeLeft;
        this.winnings = winnings;
        this.slapCount = slapCount;
    }

}
