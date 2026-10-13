/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.controller.creature;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureSleep;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatureControllerTest {

    private static final int TIME_SLEEP = 9;
    private static final float HEALTH_THRESHOLD = 50;

    @Test
    void sleepDebtTriggersSleepAtFullHealth() {
        assertTrue(CreatureController.isNeedForSleep(
                sleep(36), TIME_SLEEP, 100, HEALTH_THRESHOLD));
    }

    @Test
    void lowHealthTriggersSleepWithoutSleepDebt() {
        assertTrue(CreatureController.isNeedForSleep(
                sleep(0), TIME_SLEEP, 25, HEALTH_THRESHOLD));
    }

    @Test
    void healthEqualToThresholdDoesNotTriggerSleep() {
        assertFalse(CreatureController.isNeedForSleep(
                sleep(0), TIME_SLEEP, 50, HEALTH_THRESHOLD));
    }

    @Test
    void creatureWithNoSleepDurationCannotSleepForLowHealth() {
        assertFalse(CreatureController.isNeedForSleep(
                sleep(0), 0, 25, HEALTH_THRESHOLD));
    }

    private static CreatureSleep sleep(int sleepNeed) {
        return new CreatureSleep(null, 0, 0, sleepNeed);
    }
}
