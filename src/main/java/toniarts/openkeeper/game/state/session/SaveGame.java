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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A saved game, written as gzipped JSON.
 */
public final class SaveGame {

    /**
     * Version of the file layout, increase it when the layout changes
     */
    public static final int VERSION = 1;

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .serializeSpecialFloatingPointValues()
            .create();

    private final String level;
    private final double gameTime;

    /**
     * Creates a save game
     *
     * @param level the level name
     * @param gameTime the game time in seconds
     */
    public SaveGame(String level, double gameTime) {
        this.level = level;
        this.gameTime = gameTime;
    }

    /**
     * Reads a save game
     *
     * @param file the save file
     * @return the save game
     * @throws IOException if the file can't be read or is from a newer version
     */
    public static SaveGame read(Path file) throws IOException {
        try (Reader reader = new InputStreamReader(new GZIPInputStream(Files.newInputStream(file)), StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            int version = json.get("version").getAsInt();
            if (version > VERSION) {
                throw new IOException("Save game version " + version + " is newer than " + VERSION + "!");
            }

            return new SaveGame(json.get("level").getAsString(), json.get("gameTime").getAsDouble());
        }
    }

    /**
     * Writes the save game. The file is first written beside the target and
     * then moved over it, so a crash while saving doesn't break an earlier
     * save.
     *
     * @param file the save file
     * @throws IOException if the file can't be written
     */
    public void write(Path file) throws IOException {
        Path folder = file.toAbsolutePath().getParent();
        Files.createDirectories(folder);
        Path tempFile = folder.resolve(file.getFileName() + ".tmp");
        try (JsonWriter out = GSON.newJsonWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(tempFile)), StandardCharsets.UTF_8))) {
            out.beginObject();
            out.name("version").value(VERSION);
            out.name("level").value(level);
            out.name("gameTime").value(gameTime);
            out.endObject();
        }
        Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Gets the level name
     *
     * @return the level name
     */
    public String getLevel() {
        return level;
    }

    /**
     * Gets the game time
     *
     * @return the game time in seconds
     */
    public double getGameTime() {
        return gameTime;
    }

}
