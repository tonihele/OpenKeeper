/*
 * Copyright (C) 2014-2024 OpenKeeper
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
 * The settings of a casino, kept on the room's entity
 */
public final class Casino implements EntityComponent {

    /**
     * How the casino pays out. In DK II the keeper switches this with the pay
     * out adjuster in the casino.
     */
    public enum PayOut {

        /**
         * The gamblers win gold and cheer up, the keeper pays
         */
        SMILES,
        /**
         * The gamblers lose gold and get angrier, the keeper takes it
         */
        MONEY;

    }

    public PayOut payOut;

    public Casino() {
        // For serialization
    }

    public Casino(PayOut payOut) {
        this.payOut = payOut;
    }
}
