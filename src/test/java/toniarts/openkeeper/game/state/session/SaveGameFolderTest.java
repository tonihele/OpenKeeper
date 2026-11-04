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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lists the saves and checks the save names
 */
class SaveGameFolderTest {

    @TempDir
    Path folder;

    @Test
    void testListNewestFirst() throws IOException {
        SaveGameFolder saves = new SaveGameFolder(folder);
        assertEquals(List.of(), saves.list());
        assertEquals(List.of(), new SaveGameFolder(folder.resolve("missing")).list(), "No folder before the first save");

        write(saves, "old", "Level1", 1000);
        write(saves, "new", "Level2", 3000);
        write(saves, "middle", "Level3", 2000);
        Files.writeString(saves.getFile("broken"), "not a save");
        Files.writeString(folder.resolve("notes.txt"), "not a save either");

        List<SaveGameFolder.Entry> list = saves.list();
        assertEquals(List.of("new", "middle", "old"), list.stream().map(SaveGameFolder.Entry::name).toList(), "Newest first, unreadable left out");
        assertEquals(List.of("Level2", "Level3", "Level1"), list.stream().map(SaveGameFolder.Entry::level).toList());
        assertEquals(3000, list.get(0).time().toEpochMilli());
    }

    @Test
    void testValidNames() {
        SaveGameFolder saves = new SaveGameFolder(folder);
        assertTrue(saves.isValidName("My dungeon 2"));
        assertTrue(saves.isValidName("a".repeat(64)));
        assertFalse(saves.isValidName(""));
        assertFalse(saves.isValidName("   "));
        assertFalse(saves.isValidName(" padded"));
        assertFalse(saves.isValidName("a".repeat(65)));
        assertFalse(saves.isValidName(".hidden"));
        assertFalse(saves.isValidName("folder/name"));
        assertFalse(saves.isValidName("drive:name"));
        assertFalse(saves.isValidName("what?"));
    }

    private static void write(SaveGameFolder saves, String name, String level, long time) throws IOException {
        Path file = saves.getFile(name);
        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(file))) {
            out.write(("{\"version\":1,\"level\":\"" + level + "\",\"gameTime\":0}").getBytes(StandardCharsets.UTF_8));
        }
        Files.setLastModifiedTime(file, FileTime.fromMillis(time));
    }

}
