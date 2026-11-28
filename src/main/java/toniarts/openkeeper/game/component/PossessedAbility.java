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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.tools.convert.map.Creature;

/**
 * A first person ability the possessed creature has turned on, such as the
 * Vampire's bat form or the Monk's prayer, or a worker the Bile Demon
 * carries. It stays on until the keeper turns it off or the possession ends.
 */
public final class PossessedAbility implements EntityComponent {

    public Creature.SpecialAbility ability;

    /**
     * When the ability last did its work, a praying creature heals once a
     * second
     */
    public double checkTime;

    /**
     * The worker the creature carries, {@code null} if it carries nothing
     */
    public EntityId entity;

    /**
     * The velocity the carried worker is let go with, {@code null} while it is
     * held. Like DK II's flag 0x200 (0x516EC0), the keeper's throw only marks
     * it and the game logic tick lets the worker go (0x4831D0)
     */
    public Vector3f releaseVelocity;

    public PossessedAbility() {
        // For serialization
    }

    public PossessedAbility(Creature.SpecialAbility ability, double checkTime) {
        this.ability = ability;
        this.checkTime = checkTime;
    }

    public PossessedAbility(Creature.SpecialAbility ability, double checkTime, EntityId entity) {
        this(ability, checkTime);
        this.entity = entity;
    }

    /**
     * The creature lets go of the worker it carries on the next game logic
     * tick
     *
     * @param ability the ability
     * @param checkTime when the ability last did its work
     * @param entity the worker the creature carries
     * @param releaseVelocity the velocity the worker is let go with, in tiles
     * per turn
     */
    public PossessedAbility(Creature.SpecialAbility ability, double checkTime, EntityId entity, Vector3f releaseVelocity) {
        this(ability, checkTime, entity);
        this.releaseVelocity = releaseVelocity;
    }

}
