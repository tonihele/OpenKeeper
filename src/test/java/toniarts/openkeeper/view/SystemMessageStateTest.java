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
package toniarts.openkeeper.view;

import com.jme3.app.state.AppStateManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The message state is gone once a level ends, messages sent after that must
 * not crash the game. While it is there it shows DK II's row of message tabs:
 * the row (0x423BD0) looks at the keeper's 8 oldest held messages only, so a
 * newer message gets its tab once an older one is no longer held. The icon is
 * the message's graphic id in the table at 0x6A0B00
 */
public class SystemMessageStateTest {

    private final SystemMessageState state = new SystemMessageState(null);

    public SystemMessageStateTest() {
        state.initialize(new AppStateManager(null), null);
    }

    @Test
    public void addMessageIfAttachedDoesNothingWithoutTheState() {
        AppStateManager stateManager = new AppStateManager(null);

        assertDoesNotThrow(() -> {
            assertFalse(SystemMessageState.addMessageIfAttached(stateManager,
                    SystemMessageState.MessageType.INFO, "${level.1}"));
        });
    }

    @Test
    void theNinthMessageGetsItsTabWhenAnOlderOneIsNoLongerHeld() {
        add("1", 10, 10);
        addSeven(60, 50);
        add("9", 60, 50);

        state.update(0.25f);
        assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8"), state.getTabTexts());

        state.update(10);
        assertEquals(List.of("2", "3", "4", "5", "6", "7", "8", "9"), state.getTabTexts());
    }

    @Test
    void aMessageWhoseTabHasGoneStillTakesItsPlaceInTheRow() {

        // Like the heart attack, held for 120 seconds with a tab for 50
        add("1", 120, 50);
        addSeven(200, 200);
        add("9", 200, 200);

        state.update(1);
        state.update(50);
        assertEquals(List.of("2", "3", "4", "5", "6", "7", "8"), state.getTabTexts());

        state.update(70);
        assertEquals(List.of("2", "3", "4", "5", "6", "7", "8", "9"), state.getTabTexts());
    }

    @Test
    void aWaitingMessageWhoseTabTimeRunsOutNeverShowsItsTab() {
        add("1", 60, 60);
        addSeven(60, 60);
        add("9", 100, 20);

        state.update(1);
        state.update(60);
        assertEquals(List.of(), state.getTabTexts());
    }

    @Test
    void theTabIconIsTheMessagesGraphicId() {
        assertEquals(List.of(SystemMessageState.MessageType.INFO, SystemMessageState.MessageType.INFO,
                SystemMessageState.MessageType.CREATURE, SystemMessageState.MessageType.FIGHT,
                SystemMessageState.MessageType.OBJECTIVE, SystemMessageState.MessageType.ALLY_MSG,
                SystemMessageState.MessageType.PLAYER_EXIT),
                List.of(0, 1, 2, 3, 4, 5, 6).stream().map(SystemMessageState.MessageType::ofGraphic).toList());
    }

    private void add(String text, float seconds, float tabSeconds) {
        state.addMessage(SystemMessageState.MessageType.INFO, text, null, seconds, tabSeconds);
    }

    private void addSeven(float seconds, float tabSeconds) {
        for (int i = 2; i <= 8; i++) {
            add(Integer.toString(i), seconds, tabSeconds);
        }
    }
}
