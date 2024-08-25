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
import com.simsilica.es.EntityId;
import java.util.List;

/**
 * Creature spells. Minor infraction of ECS design having a list here. The idea
 * is to link the actual spell entities to a creature. Give this component to a
 * creature and you have yourself a search key to the actual spells.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureSpells implements EntityComponent {

    public List<EntityId> creatureSpells;

    public CreatureSpells() {
        // For serialization
    }

    public CreatureSpells(List<EntityId> creatureSpells) {
        this.creatureSpells = creatureSpells;
    }

}
