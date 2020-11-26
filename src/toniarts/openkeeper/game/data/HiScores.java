/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.game.data;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.AssetsConverter;

/**
 * Keeps track of the game hiscores, 10 entries at tops
 *
 * @author ArchDemon
 */
public class HiScores implements Savable {

    public static class HiScoresEntry implements Savable, Comparable<HiScoresEntry> {

        protected int score;
        protected String name;
        protected String level;

        public HiScoresEntry() {
        }

        public HiScoresEntry(int score, String name, String level) {
            this.score = score;
            this.name = name;
            this.level = level;
        }

        public int getScore() {
            return this.score;
        }

        public String getName() {
            return this.name;
        }

        public String getLevel() {
            return this.level;
        }

        @Override
        public void write(JmeExporter ex) throws IOException {
            OutputCapsule out = ex.getCapsule(this);
            out.write(score, "score", 0);
            out.write(name, "name", "");
            out.write(level, "level", "");
        }

        @Override
        public void read(JmeImporter im) throws IOException {
            InputCapsule in = im.getCapsule(this);
            score = in.readInt("score", 0);
            name = in.readString("name", "OpenKeeper");
            level = in.readString("level", "1");
        }

        @Override
        public int compareTo(HiScoresEntry o) {
            return Integer.compare(o.score, score);
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append(score);
            b.append(" - ");
            b.append(level);
            b.append(" - ");
            b.append(name);
            return b.toString();
        }
    }
    private List<HiScoresEntry> entries = new ArrayList<>(NUMBER_OF_ENTRIES);
    private static final int NUMBER_OF_ENTRIES = 10;
    private static final String HISCORES_FILENAME = "HiScores.okh";
    private static final Logger logger = Logger.getLogger(HiScores.class.getName());

    /**
     * Serialization-only. Do not use.
     *
     * @see #load()
     */
    public HiScores() {
    }

    /**
     * Loads up an instance of HiScores for your disposal
     *
     * @return HiScores object
     */
    public static HiScores load() {
        try (InputStream is = Files.newInputStream(getFile())) {
            HiScores hiScores = (HiScores) BinaryImporter.getInstance().load(is);

            // Ensure that the entries are in order, and remove excess data
            Collections.sort(hiScores.entries);
            hiScores.trimList();

            return hiScores;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to load HiScores data!", ex);

            return new HiScores();
        }
    }

    public void save() {
        trimList();
        try (OutputStream out = Files.newOutputStream(getFile());
                BufferedOutputStream bout = new BufferedOutputStream(out)) {
            BinaryExporter.getInstance().save(this, bout);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to save HiScores data!", ex);
        }
    }

    private static Path getFile() {
        return Paths.get(AssetsConverter.getCurrentFolder(), HISCORES_FILENAME);
    }

    /**
     * Get the hiscore entries (in order)
     *
     * @return the hiscores
     */
    public List<HiScoresEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    /**
     * Adds the HiScore entry and saves the files
     *
     * @param score score
     * @param name name
     * @param level level
     * @return true if the entry was added
     */
    public boolean add(int score, String name, String level) {
        return add(score, name, level, true);
    }

    /**
     * Adds the HiScore entry, you need to manually save after use. Bulk add
     *
     * @param score score
     * @param name name
     * @param level level
     * @return true if the entry was added
     */
    public boolean addWithoutSaving(int score, String name, String level) {
        return add(score, name, level, false);
    }

    private boolean add(int score, String name, String level, boolean save) {
        HiScoresEntry entry = new HiScoresEntry(score, name, level);

        // See do we even get to the list
        if (!isEligibleToHiScores(entry)) {
            return false;
        }

        // Add and save
        int index = Collections.binarySearch(entries, entry);
        if (index < 0) {
            index = ~index;
        }
        entries.add(index, entry);
        if (save) {
            save();
        }

        return true;
    }

    /**
     * See if this score gets you to the lists
     *
     * @param score the scored score
     * @return moment of truth
     */
    public boolean isEligibleToHiScores(int score) {
        return isEligibleToHiScores(new HiScoresEntry(score, null, null));
    }

    private boolean isEligibleToHiScores(HiScoresEntry entry) {
        int index = Collections.binarySearch(entries, entry);

        // Nope if would go over or share the final position
        return !(index == NUMBER_OF_ENTRIES - 1 || ~index >= NUMBER_OF_ENTRIES);
    }

    /**
     * Remove excess data, make sure the list is in order before calling this
     */
    private void trimList() {
        if (entries.size() > NUMBER_OF_ENTRIES) {
            entries.subList(NUMBER_OF_ENTRIES, entries.size()).clear();
        }
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.writeSavableArrayList((ArrayList) entries, "entries", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        entries = in.readSavableArrayList("entries", null);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        int i = 0;
        for (HiScoresEntry entry : entries) {
            i++;
            b.append(i);
            b.append(": ");
            b.append(entry);
            b.append("\n");
        }
        return b.toString().trim();
    }
}
