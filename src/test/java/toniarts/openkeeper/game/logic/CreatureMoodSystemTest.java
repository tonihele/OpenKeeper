/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.logic;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureMood;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreatureMoodSystemTest {

    @Test
    void lairRecoveryChangesOnlySharedOtherAnger() {
        CreatureMood mood = new CreatureMood(100, 200, 300, 400, 500, 600);

        CreatureMood updated = CreatureMoodSystem.recoverInLair(mood, -550);

        assertEquals(100, updated.general);
        assertEquals(200, updated.noFood);
        assertEquals(300, updated.noLair);
        assertEquals(400, updated.noWork);
        assertEquals(500, updated.noPay);
        assertEquals(50, updated.other);
    }

    @Test
    void lairRecoveryCannotMakeAngerNegative() {
        CreatureMood updated = CreatureMoodSystem.recoverInLair(
                new CreatureMood(0, 0, 0, 0, 0, 100), -550);

        assertEquals(0, updated.other);
    }
}
