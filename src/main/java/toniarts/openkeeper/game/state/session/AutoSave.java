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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Saves the game every few minutes of game time, in turns to the auto-save
 * slots of the save folder. DK II has no auto-save (only the quick save), so
 * this works like OpenTTD's: on by default, every 10 minutes.
 */
public final class AutoSave {

    /**
     * The intervals to choose from, in minutes of game time, 0 is off
     */
    public static final List<Integer> INTERVALS = List.of(0, 5, 10, 15, 30);

    private final SaveGameFolder folder;
    private double lastSaveTime = Double.NaN;
    private CompletableFuture<Void> saving = CompletableFuture.completedFuture(null);

    /**
     * Auto-saves to the given folder
     *
     * @param folder the save folder
     */
    public AutoSave(SaveGameFolder folder) {
        this.folder = folder;
    }

    /**
     * Saves the game if the interval has passed since the last auto-save. The
     * first call starts the count, so a loaded game counts from the time it
     * was saved at. An auto-save is skipped while the previous one is still
     * being written.
     *
     * @param gameTime the game time in seconds
     * @param interval the interval in minutes of game time, 0 is off
     * @param save saves the game to the given file, completes when written
     */
    public void update(double gameTime, int interval, Function<Path, CompletableFuture<Void>> save) {
        if (Double.isNaN(lastSaveTime)) {
            lastSaveTime = gameTime;
        }
        if (interval <= 0 || !saving.isDone() || gameTime - lastSaveTime < interval * 60.0) {
            return;
        }

        lastSaveTime = gameTime;
        saving = save.apply(folder.getFile(folder.getNextAutoSaveName()));
    }

}
