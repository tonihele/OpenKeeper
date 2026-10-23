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
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.Creature.CreatureFlag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatRulesTest {

    private static final Vector3f TARGET = new Vector3f(5, 0, 5);

    @Test
    void hitFromTheFrontGetsNoBonus() {
        assertEquals(100, CombatRules.addRearAttackBonus(100, new Vector3f(5, 0, 6), TARGET, 0));
    }

    @Test
    void hitFromBehindGetsTheBiggestBonus() {
        assertEquals(151, CombatRules.addRearAttackBonus(100, new Vector3f(5, 0, 4), TARGET, 0));
    }

    @Test
    void hitFromTheSideGetsAQuarterMore() {
        assertEquals(125, CombatRules.addRearAttackBonus(100, new Vector3f(6, 0, 5), TARGET, 0));
    }

    @Test
    void bonusFollowsTheTargetsFacing() {

        // Target turned to face +x, so the attacker at -x is behind it
        assertEquals(151, CombatRules.addRearAttackBonus(100, new Vector3f(4, 0, 5), TARGET, FastMath.HALF_PI));
        assertEquals(100, CombatRules.addRearAttackBonus(100, new Vector3f(6, 0, 5), TARGET, FastMath.HALF_PI));
    }

    @Test
    void smallAngleOffTheFrontGetsNoBonus() {
        float angle = 50 * FastMath.DEG_TO_RAD;
        Vector3f attacker = new Vector3f(5 + FastMath.sin(angle), 0, 5 + FastMath.cos(angle));

        assertEquals(100, CombatRules.addRearAttackBonus(100, attacker, TARGET, 0));
    }

    @Test
    void possessedCreatureTakesHalfTheDamage() {
        assertEquals(240, CombatRules.getDamageTaken(240, false));
        assertEquals(60, CombatRules.getDamageTaken(120, true));
    }

    @Test
    void hornedReaperCannotBeHurt() {
        assertFalse(CombatRules.canBeHurtBy(EnumSet.noneOf(CreatureFlag.class), EnumSet.of(CreatureFlag.IS_HORNY)));
    }

    @Test
    void stoneKnightIsOnlyHurtByTheHornedReaper() {
        EnumSet<CreatureFlag> stoneKnight = EnumSet.of(CreatureFlag.ONLY_ATTACKABLE_BY_HORNY);

        assertFalse(CombatRules.canBeHurtBy(EnumSet.noneOf(CreatureFlag.class), stoneKnight));
        assertTrue(CombatRules.canBeHurtBy(EnumSet.of(CreatureFlag.IS_HORNY), stoneKnight));
        assertTrue(CombatRules.canBeHurtBy(EnumSet.noneOf(CreatureFlag.class), EnumSet.of(CreatureFlag.IS_EVIL)));
    }

}
