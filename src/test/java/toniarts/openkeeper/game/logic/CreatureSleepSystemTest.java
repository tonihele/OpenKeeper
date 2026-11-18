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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreatureSleepSystemTest {

    @Test
    void noFatigueLatchesWhileGamblingButItDoesOnceItStops() {

        // DK II's Casino states have flag 0x10, so 0x477C20 doesn't latch the fatigue while gambling
        EntityData data = new DefaultEntityData();
        EntityId elf = data.createEntity();
        CreatureComponent creature = new CreatureComponent();
        creature.creatureId = 6;
        data.setComponents(elf, creature, new CreatureSleep(null, 0, 0, 0), new CreatureAi(0, CreatureState.WORK, (short) 6),
                new TaskComponent(1, null, new Point(1, 1), TaskType.GAMBLE, 0, true));
        IKwdFile kwd = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getGameLevel" -> CreatureSleepFixtures.level(4);
                    case "getCreature" -> CreatureSleepFixtures.sleeper(9);
                    default -> null;
                });
        double[] time = {30};
        CreatureSleepSystem system = new CreatureSleepSystem(data, kwd, () -> time[0]);

        system.processTick(0.25f);
        assertEquals(0, data.getComponent(elf, CreatureSleep.class).sleepNeed);

        data.removeComponent(elf, TaskComponent.class);
        time[0] = 30.25;
        system.processTick(0.25f);
        assertEquals(36, data.getComponent(elf, CreatureSleep.class).sleepNeed);
        assertEquals(30.25, data.getComponent(elf, CreatureSleep.class).lastSleepTime);
        data.close();
    }

    @Test
    void awakeIntervalLatchesFourRuntimeAuthoredSleepTime() {
        EntityId bed = new EntityId(7);
        CreatureSleep sleep = new CreatureSleep(bed, 10, 0, 0);

        CreatureSleep updated = CreatureSleepSystem.updateSleep(
                sleep, CreatureState.IDLE, 30, 20, 9, 4);

        assertEquals(bed, updated.lairObjectId);
        assertEquals(30, updated.lastSleepTime);
        assertEquals(36, updated.sleepNeed);
    }

    @Test
    void configuredTickRateKeepsAuthoredSleepDuration() {
        for (int rate : new int[]{2, 4, 8}) {
            CreatureSleep sleep = new CreatureSleep(null, 0, 0, 0);
            sleep.lastSleepAttemptTime = 19;
            sleep = CreatureSleepSystem.updateSleep(sleep, CreatureState.IDLE, 20, 20, 9, rate);
            assertEquals(9 * rate, sleep.sleepNeed);
            for (int tick = 1; tick <= 9 * rate; tick++) {
                sleep = CreatureSleepSystem.updateSleep(sleep, CreatureState.SLEEPING,
                        20 + (double) tick / rate, 20, 9, rate);
                assertEquals(9 * rate - tick, sleep.sleepNeed);
                assertEquals(19, sleep.lastSleepAttemptTime);
            }
        }
    }

    @Test
    void sleepingConsumesExactlyOneDebtUnitPerUpdate() {
        CreatureSleep sleep = new CreatureSleep(null, 10, 12, 36);

        CreatureSleep updated = CreatureSleepSystem.updateSleep(
                sleep, CreatureState.SLEEPING, 100, 20, 9, 4);

        assertEquals(100, updated.lastSleepTime);
        assertEquals(12, updated.sleepStartTime);
        assertEquals(35, updated.sleepNeed);
    }

    @Test
    void sleepingDebtDoesNotUnderflow() {
        CreatureSleep sleep = new CreatureSleep(null, 10, 12, 0);

        CreatureSleep updated = CreatureSleepSystem.updateSleep(
                sleep, CreatureState.SLEEPING, 100, 20, 9, 4);

        assertEquals(0, updated.sleepNeed);
        assertEquals(100, updated.lastSleepTime);
    }

    @Test
    void awakeIntervalDoesNotRelatchWhileSleeping() {
        CreatureSleep sleep = new CreatureSleep(null, 10, 12, 5);

        CreatureSleep updated = CreatureSleepSystem.updateSleep(
                sleep, CreatureState.SLEEPING, 100, 20, 9, 4);

        assertEquals(100, updated.lastSleepTime);
        assertEquals(4, updated.sleepNeed);
    }
}
