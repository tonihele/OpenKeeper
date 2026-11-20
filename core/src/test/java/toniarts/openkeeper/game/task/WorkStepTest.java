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
package toniarts.openkeeper.game.task;

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityId;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Creature work like research and training happens once a second, however
 * many logic ticks the level runs per second
 */
class WorkStepTest {

    @Test
    void worksOnceASecondAtFourTicksPerSecond() {
        assertEquals(60, countWorkSteps(4, 60));
    }

    @Test
    void worksOnceASecondAtAnyTickRate() {
        assertEquals(60, countWorkSteps(10, 60));
        assertEquals(60, countWorkSteps(1, 60));
    }

    @Test
    void firstStepComesAfterOneSecond() {
        WorkTask task = new WorkTask();
        ICreatureController creature = creature();
        task.assign(creature, false);

        assertFalse(task.isWorkStepDue(creature, 0.75f));
        assertTrue(task.isWorkStepDue(creature, 1.0f));
        assertFalse(task.isWorkStepDue(creature, 1.25f));
    }

    private static int countWorkSteps(int ticksPerSecond, int seconds) {
        WorkTask task = new WorkTask();
        ICreatureController creature = creature();
        task.assign(creature, false);

        int steps = 0;
        float tpf = 1.0f / ticksPerSecond;
        for (int tick = 1; tick <= ticksPerSecond * seconds; tick++) {
            if (task.isWorkStepDue(creature, tick * tpf)) {
                steps++;
            }
        }
        return steps;
    }

    private static ICreatureController creature() {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    /**
     * The bare minimum of a task, only the work step timing is used
     */
    private static final class WorkTask extends AbstractTask {

        WorkTask() {
            super(null, null);
        }

        @Override
        public void executeTask(ICreatureController creature, float executionDuration) {
        }

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public Vector2f getTarget(ICreatureController creature) {
            return null;
        }

        @Override
        public Point getTaskLocation() {
            return null;
        }

        @Override
        public EntityId getTaskTarget() {
            return null;
        }

        @Override
        public boolean isReachable(ICreatureController creature) {
            return true;
        }

        @Override
        public boolean isValid(ICreatureController creature) {
            return true;
        }

        @Override
        public TaskType getTaskType() {
            return TaskType.RESEARCH;
        }

    }

}
