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
package toniarts.openkeeper.game.controller;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.tools.convert.map.Player;
import static org.junit.jupiter.api.Assertions.*;

class TimeLimitWarningTest {
    @Test
    void disabledSpeechIsSilentAndOnlyHumanKeepersReceiveWarnings() {
        var players = List.of(new Keeper(false, Player.KEEPER1_ID),
                new Keeper(false, Player.KEEPER2_ID), new Keeper(true, Player.KEEPER3_ID),
                new Keeper(false, Player.NEUTRAL_PLAYER_ID), new Keeper(false, (short) 0));
        var sent = new ArrayList<Object[]>();
        var events = (GameEventService) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{GameEventService.class}, (proxy, method, args) -> {
                    assertEquals("emitToPlayer", method.getName());
                    sent.add(args);
                    return null;
                });
        GameController.announceTimeLimitWarning(false, 60.1f, 60f, players, events);
        assertTrue(sent.isEmpty());
        GameController.announceTimeLimitWarning(true, 60f, 59.9f, players, events);
        assertTrue(sent.isEmpty(), "Enabling speech must not replay a past warning");
        GameController.announceTimeLimitWarning(true, 10.1f, 10f, players, events);
        assertEquals(2, sent.size());
        assertArrayEquals(new Object[]{GameEventType.TIME_LIMIT_WARNING, null, 10, Player.KEEPER1_ID}, sent.get(0));
        assertArrayEquals(new Object[]{GameEventType.TIME_LIMIT_WARNING, null, 10, Player.KEEPER2_ID}, sent.get(1));
    }

    @Test
    void announcesMinuteMilestonesOnceEvenAcrossFractionalTicks() {
        for (int seconds : new int[]{1800, 1200, 600, 300, 240, 180, 120, 60}) {
            assertEquals(seconds, GameController.crossedTimeLimitWarning(seconds + .01f, seconds - .01f));
            assertEquals(0, GameController.crossedTimeLimitWarning(seconds - .01f, seconds - .02f));
        }
        assertEquals(0, GameController.crossedTimeLimitWarning(420.1f, 419.9f));
        assertEquals(0, GameController.crossedTimeLimitWarning(30.1f, 29.9f));
    }

    @Test
    void countsTheLastTenSecondsWithoutRepeatingOrAnnouncingZero() {
        for (int seconds = 1; seconds <= 10; seconds++) {
            assertEquals(seconds, GameController.crossedTimeLimitWarning(seconds + .1f, seconds));
            assertEquals(0, GameController.crossedTimeLimitWarning(seconds, seconds - .1f));
        }
        assertEquals(0, GameController.crossedTimeLimitWarning(.1f, 0f));
        assertEquals(0, GameController.crossedTimeLimitWarning(0f, 0f));
        assertEquals(0, GameController.crossedTimeLimitWarning(1f, 120f));
    }

    @Test
    void aDelayedTickProducesOnlyTheLatestWarning() {
        assertEquals(3, GameController.crossedTimeLimitWarning(10.1f, 2.9f));
        assertEquals(60, GameController.crossedTimeLimitWarning(301f, 59f));
    }
}
