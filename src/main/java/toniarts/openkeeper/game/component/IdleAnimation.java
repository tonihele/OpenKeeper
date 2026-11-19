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
import toniarts.openkeeper.tools.convert.map.Creature;

/**
 * What a creature with nothing to do did last: the idle animation it stands
 * through, or an idle walk. DK II only lets an idle animation follow an idle
 * walk (0x4776C0)
 */
public final class IdleAnimation implements EntityComponent {

    /**
     * The idle animation, {@code null} after an idle walk
     */
    public Creature.AnimationType animation;
    public double endTime;

    public IdleAnimation() {
        // For serialization
    }

    public IdleAnimation(Creature.AnimationType animation, double endTime) {
        this.animation = animation;
        this.endTime = endTime;
    }

}
