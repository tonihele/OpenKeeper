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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import toniarts.openkeeper.Main;

/**
 * The saved games folder. Like in DK II, a save is known by its file name, and
 * the list shows the newest first. The auto-saves are in the list too.
 */
public final class SaveGameFolder {

    /**
     * The quick save's name, DK II's quick save also writes over one save
     */
    public static final String QUICK_SAVE_NAME = "quicksave";

    private static final Logger logger = System.getLogger(SaveGameFolder.class.getName());

    private static final String EXTENSION = ".json.gz";

    /**
     * DK II takes save names of up to 64 characters
     */
    private static final int MAX_NAME_LENGTH = 64;
    private static final String INVALID_CHARACTERS = "\\/:*?\"<>|";
    private static final String AUTO_SAVE_NAME = "autosave";
    private static final int AUTO_SAVE_SLOTS = 3;

    private final Path folder;

    /**
     * The saves in the user folder
     */
    public SaveGameFolder() {
        this(Paths.get(System.getProperty("user.home"), "." + Main.TITLE, "saves"));
    }

    /**
     * The saves in the given folder
     *
     * @param folder the folder
     */
    public SaveGameFolder(Path folder) {
        this.folder = folder;
    }

    /**
     * Gets the file of a save
     *
     * @param name the save name
     * @return the save file
     */
    public Path getFile(String name) {
        return folder.resolve(name + EXTENSION);
    }

    /**
     * Deletes a save. DK II can't delete saves, this is an addition.
     *
     * @param name the save name
     * @return {@code true} if the save was deleted, {@code false} if there
     * was none
     * @throws IOException if the file can't be deleted
     */
    public boolean delete(String name) throws IOException {
        return Files.deleteIfExists(getFile(name));
    }

    /**
     * Whether the name can be used as a file name for a save
     *
     * @param name the save name
     * @return {@code true} if the name is valid
     */
    public boolean isValidName(String name) {
        return !name.isBlank() && name.length() <= MAX_NAME_LENGTH && name.equals(name.strip()) && !name.startsWith(".")
                && name.chars().noneMatch(c -> c < ' ' || INVALID_CHARACTERS.indexOf(c) >= 0);
    }

    /**
     * Lists the saves, the newest first. A file that can't be read is left
     * out.
     *
     * @return the saves
     */
    public List<Entry> list() {
        List<Entry> saves = new ArrayList<>();
        if (!Files.isDirectory(folder)) {
            return saves;
        }

        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*" + EXTENSION)) {
            for (Path file : files) {
                String fileName = file.getFileName().toString();
                try {
                    saves.add(new Entry(fileName.substring(0, fileName.length() - EXTENSION.length()), SaveGame.readLevel(file),
                            Files.getLastModifiedTime(file).toInstant()));
                } catch (IOException | RuntimeException ex) {
                    logger.log(Level.WARNING, "Failed to read the save " + file + "!", ex);
                }
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to list the saves in " + folder + "!", ex);
        }
        saves.sort(Comparator.comparing(Entry::time).reversed());
        return saves;
    }

    /**
     * Gets the name for the next auto-save. The auto-saves take turns in
     * three slots: the first one not used yet, otherwise the oldest.
     *
     * @return the save name
     */
    public String getNextAutoSaveName() {
        String oldest = null;
        FileTime oldestTime = null;
        for (int slot = 1; slot <= AUTO_SAVE_SLOTS; slot++) {
            String name = AUTO_SAVE_NAME + slot;
            try {
                FileTime time = Files.getLastModifiedTime(getFile(name));
                if (oldestTime == null || time.compareTo(oldestTime) < 0) {
                    oldest = name;
                    oldestTime = time;
                }
            } catch (IOException ex) {
                return name;
            }
        }
        return oldest;
    }

    /**
     * A save in the list
     *
     * @param name the save name
     * @param level the level name
     * @param time when it was saved
     */
    public record Entry(String name, String level, Instant time) {

    }

}
