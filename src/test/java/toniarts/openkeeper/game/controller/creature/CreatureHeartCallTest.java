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

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Peaceful;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A creature called by its Dungeon Heart goes to the enemy when it is idle or
 * working, will fight, has more than a quarter of its health and can walk
 * there. The Horned Reaper never goes (DKII.EXE 0x4D9690)
 */
class CreatureHeartCallTest {

    private static final Point ENEMY_TILE = new Point(8, 5);

    private final EntityData entityData = new DefaultEntityData();
    private final EntityId entity = entityData.createEntity();
    private boolean reachable = true;

    @Test
    void idleCreatureSetsOffForTheEnemy() {
        CreatureController controller = controller(CreatureState.IDLE, new Health(26, 100));

        assertTrue(controller.answerHeartCall(ENEMY_TILE));

        assertEquals(CreatureState.HEART_CALL, controller.getStateMachine().getCurrentState());
        assertEquals(ENEMY_TILE, entityData.getComponent(entity, Navigation.class).target);
    }

    @Test
    void workingCreatureGoesToo() {
        assertTrue(controller(CreatureState.WORK, new Health(100, 100)).answerHeartCall(ENEMY_TILE));
    }

    @Test
    void creatureThatCanNotGetThereStaysAtItsWork() {
        reachable = false;
        CreatureController controller = controller(CreatureState.WORK, new Health(100, 100));

        assertFalse(controller.answerHeartCall(ENEMY_TILE));

        assertEquals(CreatureState.WORK, controller.getStateMachine().getCurrentState());
        assertNull(entityData.getComponent(entity, Navigation.class));
    }

    @Test
    void creatureWithAQuarterOfItsHealthOrLessStays() {
        assertFalse(controller(CreatureState.IDLE, new Health(25, 100)).answerHeartCall(ENEMY_TILE));
    }

    @Test
    void creatureThatIsFightingFleeingOrAsleepStays() {
        assertFalse(controller(CreatureState.FIGHT, new Health(100, 100)).answerHeartCall(ENEMY_TILE));
        assertFalse(controller(CreatureState.FLEE, new Health(100, 100)).answerHeartCall(ENEMY_TILE));
        assertFalse(controller(CreatureState.SLEEPING, new Health(100, 100)).answerHeartCall(ENEMY_TILE));
        assertFalse(controller(CreatureState.HEART_CALL, new Health(100, 100)).answerHeartCall(ENEMY_TILE));
    }

    @Test
    void creatureThatWillNotFightStays() {
        CreatureController controller = controller(CreatureState.IDLE, new Health(100, 100));
        entityData.setComponent(entity, new Peaceful());

        assertFalse(controller.answerHeartCall(ENEMY_TILE));
    }

    @Test
    void hornedReaperNeverAnswers() {
        CreatureController reaper = controller(CreatureState.IDLE, new Health(100, 100), Creature.CreatureFlag.IS_HORNY);

        assertFalse(reaper.answerHeartCall(ENEMY_TILE));
    }

    private CreatureController controller(CreatureState state, Health health, Creature.CreatureFlag... flags) {
        EnumSet<Creature.CreatureFlag> flagSet = EnumSet.noneOf(Creature.CreatureFlag.class);
        flagSet.addAll(List.of(flags));
        Creature creature = ShotFixtures.creature((short) 1, flagSet, EnumSet.noneOf(Creature.CreatureFlag3.class));
        entityData.setComponents(entity, new Owner((short) 3, (short) 3), health, new Position(0, new Vector3f(5, 1, 5)),
                new CreatureAi(0, state, (short) 1));
        ILevelInfo levelInfo = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                new Class<?>[]{ILevelInfo.class}, (proxy, method, args) -> null);
        INavigationService navigationService = (INavigationService) Proxy.newProxyInstance(INavigationService.class.getClassLoader(),
                new Class<?>[]{INavigationService.class}, (proxy, method, args) -> reachable ? new DefaultGraphPath<>() : null);
        CreatureController controller = new CreatureController(entity, entityData, creature, navigationService, null, () -> 0,
                Map.of(), null, null, null, levelInfo, null, null, null);
        controller.getStateMachine().setInitialState(state);
        return controller;
    }

}
