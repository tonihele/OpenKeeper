/*
 * Copyright (C) 2014-2019 OpenKeeper
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
 * Abstract base for an attack, melee, shot, creature spell...
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class Attack implements EntityComponent {

    public Double attactStartTime;
    public float rechargeTime;
    public float range;

    public Attack() {
        // For serialization
    }

    public Attack(float rechargeTime, float range) {
        this.rechargeTime = rechargeTime;
        this.range = range;
    }

}
