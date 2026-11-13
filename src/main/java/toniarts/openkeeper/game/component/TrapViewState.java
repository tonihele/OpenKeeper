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
 * Determines that the entity should be viewed as an trap. Visual presentation
 * only.<br>
 * A hidden trap is shown only to its owner, until it is revealed. Blueprints
 * too are shown only to their owner.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class TrapViewState implements EntityComponent {

    public short trapId;
    public boolean blueprint;
    public boolean hidden;

    public TrapViewState() {
        // For serialization
    }

    public TrapViewState(short trapId, boolean blueprint) {
        this(trapId, blueprint, false);
    }

    public TrapViewState(short trapId, boolean blueprint, boolean hidden) {
        this.trapId = trapId;
        this.blueprint = blueprint;
        this.hidden = hidden;
    }

    /**
     * Whether a player gets to see the trap
     *
     * @param ownerId the trap's owner
     * @param playerId the player looking
     * @return is the trap shown to the player
     */
    public boolean isShownTo(short ownerId, short playerId) {
        return ownerId == playerId || (!blueprint && !hidden);
    }
}
