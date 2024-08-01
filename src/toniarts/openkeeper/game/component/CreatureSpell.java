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

import com.simsilica.es.EntityId;

/**
 * Creature spell. Note that since a creature can have multiple, these are
 * supposed to be given to individual entities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureSpell extends Attack {

    public short creatureSpellId;
    public EntityId creatureId;

    public CreatureSpell() {
        // For serialization
    }

    public CreatureSpell(short creatureSpellId, EntityId creatureId, float rechargeTime, float range) {
        super(rechargeTime, range);
        this.creatureSpellId = creatureSpellId;
        this.creatureId = creatureId;
    }

    public CreatureSpell(CreatureSpell attack, double attackStartTime) {
        super(attack.rechargeTime, attack.range);
        this.creatureSpellId = attack.creatureSpellId;
        this.creatureId = attack.creatureId;
        this.attactStartTime = attackStartTime;
    }

}
