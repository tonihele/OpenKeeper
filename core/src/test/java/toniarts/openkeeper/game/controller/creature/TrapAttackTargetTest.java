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
import com.simsilica.es.base.DefaultEntityData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrapAttackTargetTest {

    private static final short SENTRY_TRAP = 3;
    private static final short GAS_TRAP = 1;
    private static final float SIGHT = 4f;

    private final EntityData entityData = new DefaultEntityData();
    private EntityId trap;

    @BeforeEach
    void setUp() {
        trap = entityData.createEntity();
        entityData.setComponents(trap, new TrapViewState(SENTRY_TRAP, false, false), new Health(3000, 3000));
    }

    @Test
    void anArmedTrapCanBeAttacked() {
        assertTrue(TrapAttackTarget.isAttackable(entityData, trap));
    }

    @Test
    void aBlueprintIsNotATrapYet() {
        entityData.setComponent(trap, new TrapViewState(SENTRY_TRAP, true, false));

        assertFalse(TrapAttackTarget.isAttackable(entityData, trap));
    }

    @Test
    void aHiddenTrapIsNotSeen() {
        entityData.setComponent(trap, new TrapViewState(GAS_TRAP, false, true));

        assertFalse(TrapAttackTarget.isAttackable(entityData, trap));
    }

    @Test
    void aTrapThatHasShownItselfCanBeAttacked() {
        entityData.setComponent(trap, new TrapViewState(GAS_TRAP, false, false));

        assertTrue(TrapAttackTarget.isAttackable(entityData, trap));
    }

    @Test
    void theJackInTheBoxIsLeftAlone() {
        entityData.setComponent(trap, new TrapViewState(TrapAttackTarget.JACK_IN_THE_BOX_ID, false, false));

        assertFalse(TrapAttackTarget.isAttackable(entityData, trap));
    }

    @Test
    void aTrapWithNoHealthLeftIsGone() {
        entityData.setComponent(trap, new Health(0, 3000));

        assertFalse(TrapAttackTarget.isAttackable(entityData, trap));
    }

    @Test
    void somethingThatIsNotATrapIsNotAttacked() {
        EntityId other = entityData.createEntity();
        entityData.setComponent(other, new Health(100, 100));

        assertFalse(TrapAttackTarget.isAttackable(entityData, other));
    }

    @Test
    void fightersOfTheKeepersAndTheHeroesGoForTraps() {
        assertTrue(TrapAttackTarget.attacksTraps(Creature.FightStyle.BLITZER, (short) 3));
        assertTrue(TrapAttackTarget.attacksTraps(Creature.FightStyle.FLANKER, Player.GOOD_PLAYER_ID));
    }

    @Test
    void neutralsAndNonFightersDont() {
        assertFalse(TrapAttackTarget.attacksTraps(Creature.FightStyle.BLITZER, Player.NEUTRAL_PLAYER_ID));
        assertFalse(TrapAttackTarget.attacksTraps(Creature.FightStyle.NON_FIGHTER, (short) 3));
    }

    @Test
    void aTrapIsInReachAsFarAsTheCreatureSees() {
        assertTrue(TrapAttackTarget.isInReach(SIGHT, SIGHT, false));
        assertFalse(TrapAttackTarget.isInReach(SIGHT + 0.1, SIGHT, false));
    }

    @Test
    void aWorkerOnlyGoesForATrapItIsOnTopOf() {
        assertTrue(TrapAttackTarget.isInReach(0.5, SIGHT, true));
        assertFalse(TrapAttackTarget.isInReach(1, SIGHT, true));
    }

    @Test
    void theNearestTrapIsTheOneToAttack() {
        EntityId far = entityData.createEntity();
        EntityId near = entityData.createEntity();
        Map<EntityId, Double> distances = Map.of(trap, 3.0, far, 3.5, near, 1.0);

        EntityId found = TrapAttackTarget.findNearest(List.of(trap, far, near), e -> true,
                distances::get, SIGHT, false, e -> true);

        assertEquals(near, found);
    }

    @Test
    void aTrapWeCantGetToIsPassedOver() {
        EntityId near = entityData.createEntity();
        Map<EntityId, Double> distances = Map.of(trap, 3.0, near, 1.0);

        EntityId found = TrapAttackTarget.findNearest(List.of(trap, near), e -> true,
                distances::get, SIGHT, false, e -> !e.equals(near));

        assertEquals(trap, found);
    }

    @Test
    void onlyTrapsToAttackAreLookedAt() {
        EntityId creature = entityData.createEntity();
        Map<EntityId, Double> distances = Map.of(trap, 3.0, creature, 1.0);

        EntityId found = TrapAttackTarget.findNearest(List.of(trap, creature), e -> e.equals(trap),
                distances::get, SIGHT, false, e -> true);

        assertEquals(trap, found);
    }

    @Test
    void aTrapOutOfSightIsNotFound() {
        Map<EntityId, Double> distances = Map.of(trap, 5.0);

        assertNull(TrapAttackTarget.findNearest(List.of(trap), e -> true, distances::get, SIGHT, false, e -> true));
    }

}
