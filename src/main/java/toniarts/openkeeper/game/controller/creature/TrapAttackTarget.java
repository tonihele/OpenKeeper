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
package toniarts.openkeeper.game.controller.creature;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Player;

/**
 * An enemy trap as a target of an attack. In DKII.EXE a creature that looks
 * for something to fight (0x45CB00) and finds no enemy creature goes for the
 * nearest enemy trap it can see, and hits it like any other target until the
 * trap breaks.
 */
public final class TrapAttackTarget {

    /**
     * The Jack In The Box is never picked as a target
     */
    public static final short JACK_IN_THE_BOX_ID = 16;

    /**
     * A worker only goes for a trap it is practically standing on, closer than
     * the square root of half a tile
     */
    public static final float WORKER_REACH = (float) Math.sqrt(0.5);

    private TrapAttackTarget() {
        // Nope
    }

    /**
     * Can the trap be attacked at all. Blueprints are not traps yet, a hidden
     * trap is not seen, and a trap with no health left is gone already.
     *
     * @param entityData the entity data
     * @param trap the entity, any entity
     * @return true if the entity is a trap that stands and shows
     */
    public static boolean isAttackable(EntityData entityData, EntityId trap) {
        TrapViewState viewState = entityData.getComponent(trap, TrapViewState.class);
        if (viewState == null || viewState.blueprint || viewState.hidden || viewState.trapId == JACK_IN_THE_BOX_ID) {
            return false;
        }

        Health health = entityData.getComponent(trap, Health.class);
        return health != null && health.health > 0;
    }

    /**
     * Does the creature look for traps to attack. The neutral creatures don't
     * fight, and the creatures that don't fight at all leave the traps alone
     * too.
     *
     * @param fightStyle the fight style of the creature
     * @param ownerId the owner of the creature
     * @return true if the creature goes for the enemy traps
     */
    public static boolean attacksTraps(Creature.FightStyle fightStyle, short ownerId) {
        return fightStyle != Creature.FightStyle.NON_FIGHTER && ownerId != Player.NEUTRAL_PLAYER_ID;
    }

    /**
     * Is a trap at the given distance close enough to go for. The creature
     * has to see it, and a worker has to be right on top of it.
     *
     * @param distance the distance to the trap, in tiles
     * @param distanceCanSee how far the creature sees, in tiles
     * @param worker is the creature a worker
     * @return true if the trap is in reach
     */
    public static boolean isInReach(double distance, float distanceCanSee, boolean worker) {
        return distance <= distanceCanSee && (!worker || distance < WORKER_REACH);
    }

    /**
     * Finds the nearest trap to attack
     *
     * @param sensed the entities the creature senses
     * @param isTarget tells if the entity is a trap to attack
     * @param distance gives the distance to the entity, in tiles
     * @param distanceCanSee how far the creature sees, in tiles
     * @param worker is the creature a worker
     * @param isReachable tells if the creature can get to the trap
     * @return the nearest trap, or null if there is none
     */
    public static EntityId findNearest(Collection<EntityId> sensed, Predicate<EntityId> isTarget,
            ToDoubleFunction<EntityId> distance, float distanceCanSee, boolean worker,
            Predicate<EntityId> isReachable) {
        List<EntityId> traps = sensed.stream()
                .filter(isTarget)
                .filter(trap -> isInReach(distance.applyAsDouble(trap), distanceCanSee, worker))
                .sorted(Comparator.comparingDouble(distance))
                .toList();
        for (EntityId trap : traps) {
            if (isReachable.test(trap)) {
                return trap;
            }
        }
        return null;
    }

}
