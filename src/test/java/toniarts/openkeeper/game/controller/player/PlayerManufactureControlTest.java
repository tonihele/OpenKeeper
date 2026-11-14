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
package toniarts.openkeeper.game.controller.player;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.data.ManufactureProgress;
import toniarts.openkeeper.game.listener.PlayerManufactureListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Values from the shipped data: a Troll manufactures 60 points a second, a
 * Bile Demon 50, a Sentry Trap needs 1500
 */
class PlayerManufactureControlTest {

    private static final int TROLL = 60;
    private static final int BILE_DEMON = 50;
    private static final int SENTRY_TRAP = 1500;

    @Test
    void trollManufacturesASentryTrapIn25Seconds() {
        PlayerManufactureControl control = new PlayerManufactureControl();

        for (int second = 1; second < 25; second++) {
            assertFalse(control.manufacture(TROLL, SENTRY_TRAP), "Done after " + second + " seconds");
        }

        assertTrue(control.manufacture(TROLL, SENTRY_TRAP));
        assertEquals(0, control.getProgress());
    }

    @Test
    void allWorkersAddToTheSameItem() {
        PlayerManufactureControl control = new PlayerManufactureControl();

        // A Troll and a Bile Demon together, 110 points a second
        int seconds = 0;
        boolean done = false;
        while (!done) {
            seconds++;
            done = control.manufacture(TROLL, SENTRY_TRAP) | control.manufacture(BILE_DEMON, SENTRY_TRAP);
        }

        assertEquals(14, seconds);
    }

    @Test
    void publishesProgressForTheConfiguredKeeperAndCanRemoveListener() {
        short keeperId = 3;
        PlayerManufactureControl control = new PlayerManufactureControl(keeperId);
        List<ManufactureProgress> updates = new ArrayList<>();
        List<Short> keeperIds = new ArrayList<>();
        PlayerManufactureListener listener = (updatedKeeperId, progress) -> {
            keeperIds.add(updatedKeeperId);
            updates.add(progress);
        };
        control.addListener(listener);

        assertFalse(control.manufacture(TROLL, SENTRY_TRAP));
        assertEquals(List.of(keeperId), keeperIds);
        assertEquals(new ManufactureProgress(TROLL, SENTRY_TRAP), updates.get(0));

        assertTrue(control.manufacture(SENTRY_TRAP - TROLL, SENTRY_TRAP));
        assertEquals(new ManufactureProgress(0, SENTRY_TRAP), updates.get(1));

        control.removeListener(listener);
        assertFalse(control.manufacture(TROLL, SENTRY_TRAP));
        assertEquals(2, updates.size());
    }

}
