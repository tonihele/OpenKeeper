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

class SlapSystemTest {

    @Test
    void slapWhileSleepingRemovesOneAuthoredUnitAndPreservesBed() {
        EntityId bed = new EntityId(7);
        CreatureSleep sleep = new CreatureSleep(bed, 10, 12, 36);

        CreatureSleep updated = SlapSystem.applySleepingSlap(
                sleep, CreatureState.SLEEPING, 9);

        assertSame(bed, updated.lairObjectId);
        assertEquals(10, updated.lastSleepTime);
        assertEquals(12, updated.sleepStartTime);
        assertEquals(27, updated.sleepNeed);
    }

    @Test
    void slapClampsRemainingDebtAtZero() {
        CreatureSleep sleep = new CreatureSleep(null, 0, 0, 4);

        CreatureSleep updated = SlapSystem.applySleepingSlap(
                sleep, CreatureState.SLEEPING, 9);

        assertEquals(0, updated.sleepNeed);
    }

    @Test
    void slapOutsideSleepLeavesDebtUntouched() {
        CreatureSleep sleep = new CreatureSleep(null, 0, 0, 36);

        assertSame(sleep, SlapSystem.applySleepingSlap(
                sleep, CreatureState.IDLE, 9));
    }
}
