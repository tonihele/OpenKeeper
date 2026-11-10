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
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureComponent;

/**
 * Which enemies scare a creature. In DKII.EXE a creature's fear is made of the
 * threat of the enemy creatures that are fighting it (the fight update at
 * 0x45E4F0 adds each fighter's threat, shared between its opponents, to the
 * fear of the creatures it fights, and only where it can see them), not of every
 * enemy that is near. A trap's threat has no fight, it counts when sensed.
 */
public final class ThreatSource {

    private ThreatSource() {
        // Nope
    }

    /**
     * Does the enemy scare us right now
     *
     * @param entityData the entity data
     * @param enemy the enemy entity
     * @param us the creature that might be scared
     * @return true if the enemy is a creature that has chosen us as its target,
     * or anything else with a threat, like a trap
     */
    public static boolean isThreateningUs(EntityData entityData, EntityId enemy, EntityId us) {
        if (entityData.getComponent(enemy, CreatureComponent.class) == null) {
            return true;
        }

        AttackTarget target = entityData.getComponent(enemy, AttackTarget.class);
        return target != null && us.equals(target.entityId);
    }

}
