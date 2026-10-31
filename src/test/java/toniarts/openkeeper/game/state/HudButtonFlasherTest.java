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
package toniarts.openkeeper.game.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.TriggerAction.ButtonType;
import toniarts.openkeeper.tools.convert.map.TriggerAction.MakeType;

/**
 * The HUD button flashing that the FLASH_BUTTON level action asks for
 */
class HudButtonFlasherTest {

    private final Map<String, Boolean> flashing = new HashMap<>();
    private final HudButtonFlasher flasher = new HudButtonFlasher(flashing::put);

    @Test
    void roomSpellDoorAndTrapButtonsAreFoundByTheirIconIds() {
        assertEquals("room_2", HudButtonFlasher.buttonKey(MakeType.ROOM, (short) 2, null));
        assertEquals("spell_7", HudButtonFlasher.buttonKey(MakeType.KEEPER_SPELL, (short) 7, null));
        assertEquals("door_1", HudButtonFlasher.buttonKey(MakeType.DOOR, (short) 1, null));
        assertEquals("trap_4", HudButtonFlasher.buttonKey(MakeType.TRAP, (short) 4, null));
    }

    @Test
    void miscButtonsAreFoundByTheirButtonType() {
        assertEquals("sell_0", HudButtonFlasher.buttonKey(MakeType.MISC_BUTTON, (short) 255, ButtonType.SELL_ROOM));
        assertEquals(HudButtonFlasher.CREATURE_TAB,
                HudButtonFlasher.buttonKey(MakeType.MISC_BUTTON, (short) 248, ButtonType.CREATURE_TAB));
    }

    @Test
    void buttonsThatTheHudDoesNotHaveHaveNoKey() {
        assertNull(HudButtonFlasher.buttonKey(MakeType.MISC_BUTTON, (short) 244, ButtonType.MPD_SINGLE_INVASION));
        assertNull(HudButtonFlasher.buttonKey(MakeType.CREATURE, (short) 3, null));
    }

    @Test
    void startsFlashingOnTheNextUpdate() {
        flasher.flash(MakeType.ROOM, (short) 2, null, true, 0);
        assertTrue(flashing.isEmpty());

        flasher.update(0.016f);

        assertTrue(flashing.get("room_2"));
    }

    @Test
    void withoutATimeItFlashesUntilStopped() {
        flasher.flash(MakeType.ROOM, (short) 2, null, true, 0);

        flasher.update(1000f);
        assertTrue(flashing.get("room_2"));

        flasher.flash(MakeType.ROOM, (short) 2, null, false, 0);
        flasher.update(0.016f);
        assertFalse(flashing.get("room_2"));
    }

    @Test
    void stopsAfterTheGivenSeconds() {
        flasher.flash(MakeType.TRAP, (short) 4, null, true, 5);

        flasher.update(4.9f);
        assertTrue(flashing.get("trap_4"));

        flasher.update(0.2f);
        assertFalse(flashing.get("trap_4"));
    }

    @Test
    void pressingTheButtonStopsTheFlash() {
        flasher.flash(MakeType.MISC_BUTTON, (short) 255, ButtonType.SELL_ROOM, true, 0);
        flasher.update(0.016f);

        flasher.stop("sell_0");
        flasher.update(0.016f);

        assertFalse(flashing.get("sell_0"));
    }

    @Test
    void keepsApplyingTheFlashSoThatIconsCreatedLaterGetIt() {
        flasher.flash(MakeType.ROOM, (short) 2, null, true, 0);
        flasher.update(0.016f);
        flashing.clear();

        flasher.update(HudButtonFlasher.REAPPLY_INTERVAL);

        assertTrue(flashing.get("room_2"));
    }
}
