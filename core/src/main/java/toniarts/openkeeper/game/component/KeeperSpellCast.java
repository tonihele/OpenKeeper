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
 * When a keeper last cast a keeper spell, in game time. There is one entity per
 * keeper and spell, created on the first cast. Used for the spell recharge.
 */
public final class KeeperSpellCast implements EntityComponent {

    public short keeperId;
    public short keeperSpellId;
    public double castTime;

    public KeeperSpellCast() {
        // For serialization
    }

    public KeeperSpellCast(short keeperId, short keeperSpellId, double castTime) {
        this.keeperId = keeperId;
        this.keeperSpellId = keeperSpellId;
        this.castTime = castTime;
    }

}
