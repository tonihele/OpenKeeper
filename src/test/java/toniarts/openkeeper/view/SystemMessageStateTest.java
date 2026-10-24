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
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The message state is gone once a level ends, messages sent after that must
 * not crash the game
 */
public class SystemMessageStateTest {

    @Test
    public void addMessageIfAttachedDoesNothingWithoutTheState() {
        AppStateManager stateManager = new AppStateManager(null);

        assertDoesNotThrow(() -> {
            assertFalse(SystemMessageState.addMessageIfAttached(stateManager,
                    SystemMessageState.MessageType.INFO, "${level.1}"));
        });
    }
}
