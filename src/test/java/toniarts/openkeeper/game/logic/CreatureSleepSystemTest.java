/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.logic;

import com.simsilica.es.EntityId;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.controller.creature.CreatureState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CreatureSleepSystemTest {

    @Test
    void awakeIntervalLatchesFourRuntimeAuthoredSleepTime() {
        EntityId bed = new EntityId(7);
        CreatureSleep sleep = new CreatureSleep(bed, 10, 0, 0);

        CreatureSleep updated = CreatureSleepSystem.updateSleep(
                sleep, CreatureState.IDLE, 30, 20, 9);

        assertEquals(bed, updated.lairObjectId);
        assertEquals(30, updated.lastSleepTime);
        assertEquals(36, updated.sleepNeed);
    }

    @Test
    void sleepingConsumesExactlyOneDebtUnitPerUpdate() {
        CreatureSleep sleep = new CreatureSleep(null, 10, 12, 36);

        CreatureSleep updated = CreatureSleepSystem.updateSleep(
                sleep, CreatureState.SLEEPING, 100, 20, 9);

        assertEquals(10, updated.lastSleepTime);
        assertEquals(12, updated.sleepStartTime);
        assertEquals(35, updated.sleepNeed);
    }

    @Test
    void sleepingDebtDoesNotUnderflow() {
        CreatureSleep sleep = new CreatureSleep(null, 10, 12, 0);

        CreatureSleep updated = CreatureSleepSystem.updateSleep(
                sleep, CreatureState.SLEEPING, 100, 20, 9);

        assertSame(sleep, updated);
    }

    @Test
    void awakeIntervalDoesNotRelatchWhileSleeping() {
        CreatureSleep sleep = new CreatureSleep(null, 10, 12, 5);

        CreatureSleep updated = CreatureSleepSystem.updateSleep(
                sleep, CreatureState.SLEEPING, 100, 20, 9);

        assertEquals(10, updated.lastSleepTime);
        assertEquals(4, updated.sleepNeed);
    }
}
