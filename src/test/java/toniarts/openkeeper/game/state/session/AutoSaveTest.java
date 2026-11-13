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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Auto-saves every few minutes of game time to the rotating slots
 */
class AutoSaveTest {

    @TempDir
    Path folder;

    private final List<Path> saves = new ArrayList<>();
    private long fileTime = 1000;

    @Test
    void testInterval() {
        SaveGameFolder saveGameFolder = new SaveGameFolder(folder);
        AutoSave autoSave = new AutoSave(saveGameFolder);

        // A loaded game counts from the time it was saved at
        autoSave.update(1000, 10, this::save);
        autoSave.update(1599, 10, this::save);
        assertEquals(List.of(), saves);
        autoSave.update(1600, 10, this::save);
        assertEquals(List.of(saveGameFolder.getFile("autosave1")), saves);

        // Off
        autoSave.update(5000, 0, this::save);
        assertEquals(1, saves.size());

        // The interval can change, and counts from the last auto-save
        autoSave.update(1900, 5, this::save);
        assertEquals(List.of(saveGameFolder.getFile("autosave1"), saveGameFolder.getFile("autosave2")), saves);
    }

    @Test
    void testSkipWhileSaving() {
        SaveGameFolder saveGameFolder = new SaveGameFolder(folder);
        AutoSave autoSave = new AutoSave(saveGameFolder);
        CompletableFuture<Void> writing = new CompletableFuture<>();
        List<Path> started = new ArrayList<>();

        autoSave.update(0, 5, file -> {
            started.add(file);
            return writing;
        });
        autoSave.update(300, 5, file -> {
            started.add(file);
            return writing;
        });
        autoSave.update(900, 5, this::save);
        assertEquals(1, started.size());
        assertEquals(List.of(), saves, "The first auto-save is still being written");

        writing.complete(null);
        autoSave.update(901, 5, this::save);
        assertEquals(1, saves.size());
    }

    @Test
    void testSlotsRotate() {
        SaveGameFolder saveGameFolder = new SaveGameFolder(folder);
        AutoSave autoSave = new AutoSave(saveGameFolder);
        autoSave.update(0, 5, this::save);
        for (int i = 1; i <= 5; i++) {
            autoSave.update(i * 300, 5, this::save);
        }

        assertEquals(List.of("autosave1", "autosave2", "autosave3", "autosave1", "autosave2"),
                saves.stream().map(file -> file.getFileName().toString().replace(".json.gz", "")).toList());
        assertEquals(List.of("autosave2", "autosave1", "autosave3"),
                saveGameFolder.list().stream().map(SaveGameFolder.Entry::name).toList(), "Auto-saves are in the list, newest first");
    }

    /**
     * Writes a small save, each one newer than the last
     */
    private CompletableFuture<Void> save(Path file) {
        try {
            try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(file))) {
                out.write("{\"version\":1,\"level\":\"Level1\"}".getBytes(StandardCharsets.UTF_8));
            }
            Files.setLastModifiedTime(file, FileTime.fromMillis(fileTime += 1000));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        saves.add(file);
        return CompletableFuture.completedFuture(null);
    }

}
