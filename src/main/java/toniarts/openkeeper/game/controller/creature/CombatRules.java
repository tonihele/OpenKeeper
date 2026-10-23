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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.Set;
import toniarts.openkeeper.tools.convert.map.Creature;

/**
 * The rules of a melee fight between creatures, as the original game has them
 */
public final class CombatRules {

    /**
     * How many creatures can attack the same creature at once in melee
     */
    public static final int MAX_MELEE_ATTACKERS = 8;

    /**
     * The original measures angles in 2048 steps per full circle
     */
    private static final int HALF_CIRCLE_STEPS = 1024;
    private static final int STEPS_PER_BONUS_PERCENT = 20;
    private static final int MIN_REAR_ATTACK_BONUS_PERCENT = 16;
    private static final int POSSESSED_DAMAGE_TAKEN_PERCENT = 50;

    private CombatRules() {
        // Nope
    }

    /**
     * Adds the bonus for hitting a creature from its side or back. The bonus
     * grows with the angle between the target's facing and the direction of
     * the attacker, from 16 % at about 56 degrees off the front up to 51 % at
     * the back. Attacks from the front get no bonus.
     *
     * @param damage the damage of the hit
     * @param attackerPosition where the attacker stands
     * @param targetPosition where the target stands
     * @param targetRotation the target's rotation around the y-axis
     * @return the damage with the bonus added
     */
    public static int addRearAttackBonus(int damage, Vector3f attackerPosition, Vector3f targetPosition, float targetRotation) {
        float dx = attackerPosition.x - targetPosition.x;
        float dz = attackerPosition.z - targetPosition.z;
        float length = FastMath.sqrt(dx * dx + dz * dz);
        if (length == 0) {
            return damage;
        }

        // Facing of the target, the same way the view turns the model
        float facingX = FastMath.sin(targetRotation);
        float facingZ = FastMath.cos(targetRotation);
        float cos = FastMath.clamp((facingX * dx + facingZ * dz) / length, -1f, 1f);
        int steps = Math.min(HALF_CIRCLE_STEPS - 1, (int) (FastMath.acos(cos) / FastMath.PI * HALF_CIRCLE_STEPS));
        int bonusPercent = steps / STEPS_PER_BONUS_PERCENT;
        if (bonusPercent < MIN_REAR_ATTACK_BONUS_PERCENT) {
            return damage;
        }

        return damage + damage * bonusPercent / 100;
    }

    /**
     * Scales the damage by how much of it the target takes. A possessed
     * creature takes only half of the damage.
     *
     * @param damage the damage of the hit
     * @param targetPossessed whether the target is possessed by its keeper
     * @return the damage the target takes
     */
    public static int getDamageTaken(int damage, boolean targetPossessed) {
        if (targetPossessed) {
            return damage * POSSESSED_DAMAGE_TAKEN_PERCENT / 100;
        }

        return damage;
    }

    /**
     * Can the attacker hurt the target at all. Horned Reapers take no damage,
     * and creatures only attackable by the Horned Reaper take damage only from
     * it.
     *
     * @param attackerFlags the attacker's creature flags
     * @param targetFlags the target's creature flags
     * @return true if the target takes damage from the attacker
     */
    public static boolean canBeHurtBy(Set<Creature.CreatureFlag> attackerFlags, Set<Creature.CreatureFlag> targetFlags) {
        if (targetFlags.contains(Creature.CreatureFlag.IS_HORNY)) {
            return false;
        }

        return !targetFlags.contains(Creature.CreatureFlag.ONLY_ATTACKABLE_BY_HORNY)
                || attackerFlags.contains(Creature.CreatureFlag.IS_HORNY);
    }

}
