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
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Casino;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Drunk;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.CasinoVariableFixtures;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Creature.AnimationType;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gamblers in the Casino and the big winner animate as in the original. The
 * test creature's animations last a second each
 */
class GamblerAnimationTest {

    private static final short CREATURE_ID = 5;
    private static final float TICK = 0.25f;

    private final EntityData entityData = new DefaultEntityData();
    private CreatureViewSystem system;
    private EntityId creatureId;
    private Casino.PayOut payOut = Casino.PayOut.SMILES;

    @Test
    void gamblerPlaysItsResearchHappyOrIdleAnimation() {
        gambler(AnimationType.IDLE_1, AnimationType.HAPPY, AnimationType.RESEARCHING, AnimationType.MELEE_ATTACK);

        assertEquals(EnumSet.of(AnimationType.RESEARCHING, AnimationType.HAPPY, AnimationType.IDLE_1), playedAnimations(400));
    }

    @Test
    void gamblerIsAngryInsteadOfHappyWhenTheCasinoTakesItsMoney() {
        payOut = Casino.PayOut.MONEY;
        gambler(AnimationType.IDLE_1, AnimationType.HAPPY, AnimationType.ANGRY, AnimationType.RESEARCHING);

        assertEquals(EnumSet.of(AnimationType.RESEARCHING, AnimationType.ANGRY, AnimationType.IDLE_1), playedAnimations(400));
    }

    @Test
    void gamblerWithoutHappyAnimationOnlyIdles() {

        // The research animation is only played by creatures that have a happy one too
        gambler(AnimationType.IDLE_1, AnimationType.RESEARCHING);

        assertEquals(EnumSet.of(AnimationType.IDLE_1), playedAnimations(100));
    }

    @Test
    void gamblerPlaysAnAnimationThreeTimesBeforeItPicksTheNext() {
        gambler(AnimationType.IDLE_1);
        system.processTick(TICK);
        double start = entityData.getComponent(creatureId, CreatureViewState.class).stateStartTime;

        tick(3 / TICK - 1);
        assertEquals(start, entityData.getComponent(creatureId, CreatureViewState.class).stateStartTime);

        // The same animation is picked again, it starts over
        tick(1);
        assertEquals(start + 3, entityData.getComponent(creatureId, CreatureViewState.class).stateStartTime, 0.001);
    }

    @Test
    void creatureAtTheAlcoveBarrelsDrinksOrSwaysDrunk() {
        drinker(AnimationType.IDLE_1, AnimationType.HAPPY, AnimationType.RESEARCHING, AnimationType.DRINKING,
                AnimationType.DRUNKED_IDLE);

        assertEquals(EnumSet.of(AnimationType.DRINKING, AnimationType.DRUNKED_IDLE), playedAnimations(400));

        // It has no drunk walk animation, so the drinks never make it drunk
        assertNull(entityData.getComponent(creatureId, Drunk.class));
    }

    @Test
    void creatureWithoutADrunkIdleAnimationOnlyDrinks() {
        drinker(AnimationType.IDLE_1, AnimationType.HAPPY, AnimationType.DRINKING);

        assertEquals(EnumSet.of(AnimationType.DRINKING), playedAnimations(100));
    }

    @Test
    void drinksMakeACreatureWithADrunkWalkDrunkForFortyFiveSeconds() {
        drinker(AnimationType.DRINKING, AnimationType.DRUNKED_WALK);

        // One drink in four does it, each drink is three seconds long
        double time = 0;
        while (entityData.getComponent(creatureId, Drunk.class) == null && time < 1200) {
            system.processTick(TICK);
            time += TICK;
        }
        Drunk drunk = entityData.getComponent(creatureId, Drunk.class);
        assertNotNull(drunk);
        assertEquals(time + 45, drunk.endTime, 0.001);
        assertEquals(AnimationType.DRINKING, entityData.getComponent(creatureId, CreatureViewState.class).state);
    }

    @Test
    void drunkCreatureWalksWithItsDrunkWalkUntilItHasSoberedUp() {
        creature(AnimationType.WALK, AnimationType.DRUNKED_WALK);
        entityData.setComponents(creatureId, new Navigation(), new Drunk(2));

        system.processTick(TICK);
        assertEquals(AnimationType.DRUNKED_WALK, entityData.getComponent(creatureId, CreatureViewState.class).state);

        tick(2 / TICK);
        assertEquals(AnimationType.WALK, entityData.getComponent(creatureId, CreatureViewState.class).state);
        assertNull(entityData.getComponent(creatureId, Drunk.class));
        assertTrue(entityData.getComponent(creatureId, Navigation.class) != null);
    }

    @Test
    void bigWinnerDancesOnceThenCheers() {
        celebrating(AnimationType.DANCE, AnimationType.HAPPY);

        system.processTick(TICK);
        assertEquals(AnimationType.DANCE, entityData.getComponent(creatureId, CreatureViewState.class).state);

        tick(1 / TICK);
        assertEquals(AnimationType.HAPPY, entityData.getComponent(creatureId, CreatureViewState.class).state);

        tick(10 / TICK);
        assertEquals(AnimationType.HAPPY, entityData.getComponent(creatureId, CreatureViewState.class).state);
    }

    @Test
    void bigWinnerWhoCanNotDanceCheers() {
        celebrating(AnimationType.HAPPY);

        system.processTick(TICK);
        assertEquals(AnimationType.HAPPY, entityData.getComponent(creatureId, CreatureViewState.class).state);
    }

    private void gambler(AnimationType... animations) {
        creature(animations);
        entityData.setComponent(creatureId, new TaskComponent(1, null, null, TaskType.GAMBLE, 0, true));
    }

    /**
     * A gambler whose place in the Casino is the Alcove Barrels, the target of
     * its task
     */
    private void drinker(AnimationType... animations) {
        creature(animations);
        entityData.setComponent(creatureId, new TaskComponent(1, entityData.createEntity(), null, TaskType.GAMBLE, 0, true));
    }

    private void celebrating(AnimationType... animations) {
        creature(animations);
        entityData.setComponent(creatureId, new CreatureAi(0, CreatureState.CELEBRATING, CREATURE_ID));
    }

    private void creature(AnimationType... animations) {
        Creature creature = CasinoVariableFixtures.creatureWithAnimations(CREATURE_ID, animations);
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> "getCreature".equals(method.getName()) ? creature : null);
        creatureId = entityData.createEntity();
        entityData.setComponents(creatureId, new CreatureViewState(CREATURE_ID, 0, AnimationType.WALK),
                new Position(0, new Vector3f()));
        IRoomController casino = (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(),
                new Class<?>[]{IRoomController.class}, (proxy, method, args) -> "getCasinoPayOut".equals(method.getName()) ? payOut : null);
        IMapController mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> casino);
        system = new CreatureViewSystem(entityData, kwdFile, mapController);
    }

    /**
     * The animations the gambler picks over the given number of picks
     */
    private Set<AnimationType> playedAnimations(int picks) {
        Set<AnimationType> played = EnumSet.noneOf(AnimationType.class);
        for (int i = 0; i < picks; i++) {
            tick(3 / TICK);
            played.add(entityData.getComponent(creatureId, CreatureViewState.class).state);
        }
        return played;
    }

    private void tick(float ticks) {
        for (int i = 0; i < ticks; i++) {
            system.processTick(TICK);
        }
    }

}
