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
package toniarts.openkeeper.game.state.session;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Writes save games and reads them back
 */
class SaveGameTest {

    @TempDir
    Path folder;

    @Test
    void testHeaderRoundTrip() throws Exception {
        Path file = folder.resolve("saves").resolve("Level6a.json.gz");
        new SaveGame("Level6a", 1234.5).write(file);

        SaveGame saveGame = SaveGame.read(file);

        assertEquals("Level6a", saveGame.getLevel());
        assertEquals(1234.5, saveGame.getGameTime());
    }

}
