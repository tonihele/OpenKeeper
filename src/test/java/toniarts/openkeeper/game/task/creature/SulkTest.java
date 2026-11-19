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
package toniarts.openkeeper.game.task.creature;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.TempleFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SulkTest {

    private static final short PLAYER_ID = 3;
    private static final int ANGER_PER_SECOND = -550;
    private static final int HEALTH_PER_SECOND = 100;

    private EntityData entityData;
    private EntityId entity;
    private ICreatureController creature;
    private boolean hasLair;
    private int sulkSounds;
    private int angryAnimations;
    private int steps;
    private Sulk sulk;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        entity = entityData.createEntity();
        entityData.setComponent(entity, new CreatureMood(1000, 2000, 0, 0, 3000, 1500));
        entityData.setComponent(entity, new Health(80, 300));
        entityData.setComponent(entity, new Position(0, new Vector3f(2.5f, 0, 3.5f)));
        hasLair = true;
        creature = (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityId" -> entity;
                    case "getOwnerId" -> PLAYER_ID;
                    case "getLairLocation" -> new Point(2, 3);
                    case "hasLair" -> hasLair;
                    case "fireEvent" -> {
                        assertEquals(GameEventType.CREATURE_SULK, args[0]);
                        sulkSounds++;
                        yield null;
                    }
                    case "playAnimation" -> {
                        assertEquals(Creature.AnimationType.ANGRY, args[0]);
                        angryAnimations++;
                        yield null;
                    }
                    case "navigateToAssignedTask" -> {
                        steps++;
                        yield null;
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "creature " + entity;
                    default -> throw new UnsupportedOperationException(method.getName());
                });

        Map<MiscType, MiscVariable> gameSettings = new EnumMap<>(MiscType.class);
        gameSettings.put(MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND,
                TempleFixtures.miscVariable(MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND, ANGER_PER_SECOND));
        gameSettings.put(MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND,
                TempleFixtures.miscVariable(MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, HEALTH_PER_SECOND));

        sulk = new Sulk(null, null, creature, entityData, gameSettings);
        sulk.assign(creature, false);
    }

    @Test
    void sulkingHappensInTheLair() {
        assertEquals(new Point(2, 3), sulk.getTaskLocation());
    }

    @Test
    void sulkingDoesNothingBeforeTheFirstSecond() {
        sulk.executeTask(creature, 0.75f);

        assertEquals(1500, entityData.getComponent(entity, CreatureMood.class).other);
        assertEquals(80, entityData.getComponent(entity, Health.class).health);
    }

    @Test
    void sulkingCalmsOnlyTheAngerTheLairCalms() {
        sulk.executeTask(creature, 1.0f);

        CreatureMood mood = entityData.getComponent(entity, CreatureMood.class);
        assertEquals(1500 + ANGER_PER_SECOND, mood.other);
        assertEquals(1000, mood.general);
        assertEquals(2000, mood.noFood);
        assertEquals(3000, mood.noPay);
    }

    @Test
    void sulkingHealsLikeSleepingInTheLair() {
        sulk.executeTask(creature, 1.0f);

        assertEquals(80 + HEALTH_PER_SECOND, entityData.getComponent(entity, Health.class).health);
    }

    @Test
    void sulkingDoesNotHealPastTheMaximum() {
        entityData.setComponent(entity, new Health(250, 300));

        sulk.executeTask(creature, 1.0f);

        assertEquals(300, entityData.getComponent(entity, Health.class).health);
    }

    @Test
    void sulkingWorksOnceASecondEvenWhenExecutedEveryTick() {
        for (float time = 0.25f; time <= 2.0f; time += 0.25f) {
            sulk.executeTask(creature, time);
        }

        assertEquals(1500 + 2 * ANGER_PER_SECOND, entityData.getComponent(entity, CreatureMood.class).other);
    }

    @Test
    void theSulkIsOverWhenTheAngerIsGone() {
        assertTrue(sulk.isValid(creature));

        for (float time = 1.0f; time <= 3.0f; time += 1.0f) {
            sulk.executeTask(creature, time);
        }

        assertEquals(0, entityData.getComponent(entity, CreatureMood.class).other);
        assertFalse(sulk.isValid(creature));
    }

    @Test
    void theCreatureStandsFiveToEightSecondsBeforeItStepsToANewSpot() {
        for (int i = 0; i < 100; i++) {
            float time = standUntilStep(i * 100.0f) - i * 100.0f;

            assertTrue(time >= 5.0f && time <= 8.0f, "stood " + time);
        }
    }

    @Test
    void theNewSpotIsFourTenthsOfATileFromTheMiddleOfTheLair() {
        Vector2f middle = sulk.getTarget(creature);
        assertEquals(middle, sulk.getWorkPosition(creature));

        for (int i = 0; i < 20; i++) {
            standUntilStep(i * 100.0f);

            assertEquals(0.4f, sulk.getWorkPosition(creature).distance(middle), 0.0001f);
        }
    }

    @Test
    void theCreatureSulksOutLoudAtTheStartFourSecondsBeforeTheStepAndAsItSteps() {
        standUntilStep(0);

        assertEquals(3, sulkSounds);
    }

    @Test
    void theCreatureIsAngryEachTimeItSulksOutLoudButAsItSteps() {
        standUntilStep(0);

        assertEquals(2, angryAnimations);
    }

    @Test
    void theCreatureTurnsOnTheSpotEveryTwoSecondsBeforeTheStepByUpToNinetyDegrees() {
        for (int i = 0; i < 50; i++) {
            float start = i * 100.0f;
            List<Float> turns = new ArrayList<>();
            float rotation = entityData.getComponent(entity, Position.class).rotation;
            int stepsBefore = steps;
            float time = start;
            while (steps == stepsBefore) {
                sulk.executeTask(creature, time);
                float now = entityData.getComponent(entity, Position.class).rotation;
                if (now != rotation) {
                    float turned = ((now - rotation) % FastMath.TWO_PI + FastMath.TWO_PI) % FastMath.TWO_PI;
                    assertTrue(turned < FastMath.HALF_PI + 0.0001f, "turned " + turned);
                    turns.add(time);
                    rotation = now;
                }
                time += 0.25f;
            }
            float step = time - 0.25f;

            // 5 to 8 seconds of standing have 2 to 4 turns, the last one 2 seconds before the step
            assertEquals((int) (step - start) / 2, turns.size(), "stood " + (step - start));
            for (int turn = 0; turn < turns.size(); turn++) {
                assertEquals(step - 2 * (turns.size() - turn), turns.get(turn), 0.0001f);
            }
        }
    }

    @Test
    void theSulkIsOverWhenTheLairIsGone() {
        hasLair = false;

        assertFalse(sulk.isValid(creature));
    }

    /**
     * Executes the sulk every tick until the creature steps to a new spot
     *
     * @param start the time the stand starts
     * @return the time of the step
     */
    private float standUntilStep(float start) {
        int stepsBefore = steps;
        float time = start;
        while (steps == stepsBefore) {
            sulk.executeTask(creature, time);
            time += 0.25f;
        }
        return time - 0.25f;
    }

}
