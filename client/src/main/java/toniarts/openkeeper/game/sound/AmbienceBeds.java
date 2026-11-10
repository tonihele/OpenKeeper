/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.game.sound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import toniarts.openkeeper.utils.Point;

/**
 * The room and terrain ambience: lava, water, the chant of the Library, the
 * hum of the Portal and so on.
 * <p>
 * The map is seen as an 11x11 grid of sound category names around the
 * listener. Tiles are weighted by how close they are to the listener (a tent,
 * so the middle counts the most), tiles are grouped by their category, and the
 * two heaviest categories get a bed each. A bed sits at the centroid of its
 * tiles, but not nearer than a two tile dead zone around the listener, so
 * standing next to a lava lake does not make the lava jump around.
 * <p>
 * A bed has two layers, event {@value #LAYER_ONE} and event {@value #LAYER_TWO},
 * of which a category defines either or both.
 */
final class AmbienceBeds {

    static final int GRID_SIZE = 11;
    static final int GRID_CENTER = GRID_SIZE / 2;
    static final int LAYER_ONE = 0x2EA;
    static final int LAYER_TWO = 0x2EB;
    static final int MAX_BEDS = 2;

    private static final int[] TENT_WEIGHTS = {1, 1, 1, 2, 2, 3, 2, 2, 1, 1, 1};
    private static final int DEAD_ZONE = 2;

    private final SoundEngine engine;
    private final Function<String, SoundCategory> loader;
    private final Map<String, SoundCategory> categories = new HashMap<>();
    private final Map<String, Bed> beds = new LinkedHashMap<>();

    AmbienceBeds(SoundEngine engine, Function<String, SoundCategory> loader) {
        this.engine = engine;
        this.loader = loader;
    }

    /**
     * @param cells category names of the tiles around the listener, row by
     * row, {@code null} or empty for a tile without a sound (or unexplored)
     * @param listener the listener tile
     */
    void update(long now, @Nullable String[] cells, Point listener) {
        Map<String, Region> regions = collectRegions(cells);
        List<Map.Entry<String, Region>> winners = selectWinners(regions);

        Iterator<Map.Entry<String, Bed>> it = beds.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Bed> entry = it.next();
            if (winners.stream().noneMatch(winner -> winner.getKey().equals(entry.getKey()))) {
                entry.getValue().stop();
                it.remove();
            }
        }

        for (Map.Entry<String, Region> winner : winners) {
            Region region = winner.getValue();
            Point position = new Point(
                    listener.x + deadZone(region.centroidX() - GRID_CENTER),
                    listener.y + deadZone(region.centroidY() - GRID_CENTER));
            Bed bed = beds.get(winner.getKey());
            if (bed == null) {
                bed = new Bed(region.category, position);
                beds.put(winner.getKey(), bed);
            }
            bed.update(now, position);
        }
    }

    void stop() {
        for (Bed bed : beds.values()) {
            bed.stop();
        }
        beds.clear();
    }

    int getBedCount() {
        return beds.size();
    }

    @Nullable
    Point getBedPosition(String category) {
        Bed bed = beds.get(category);
        return bed == null ? null : bed.position;
    }

    private Map<String, Region> collectRegions(@Nullable String[] cells) {
        Map<String, Region> regions = new LinkedHashMap<>();
        if (cells == null) {
            return regions;
        }
        for (int i = 0; i < Math.min(cells.length, GRID_SIZE * GRID_SIZE); i++) {
            String name = cells[i];
            if (name == null || name.isEmpty()) {
                continue;
            }
            SoundCategory category = getBedCategory(name);
            if (category == null) {
                continue;
            }
            int x = i % GRID_SIZE;
            int y = i / GRID_SIZE;
            regions.computeIfAbsent(name, key -> new Region(category)).add(x, y);
        }
        return regions;
    }

    /**
     * @return the category if it has an ambient bed, i.e. defines the first layer
     */
    @Nullable
    private SoundCategory getBedCategory(String name) {
        if (!categories.containsKey(name)) {
            SoundCategory category = loader.apply(name);
            categories.put(name, category != null && category.hasEvent(LAYER_ONE) ? category : null);
        }
        return categories.get(name);
    }

    private static List<Map.Entry<String, Region>> selectWinners(Map<String, Region> regions) {
        List<Map.Entry<String, Region>> sorted = new ArrayList<>(regions.entrySet());
        // the sort is stable, so on equal weights the category that was met first wins
        sorted.sort((a, b) -> Integer.compare(b.getValue().weight, a.getValue().weight));
        return sorted.subList(0, Math.min(MAX_BEDS, sorted.size()));
    }

    /**
     * Distance of the bed from the listener, a bed is not closer than the dead zone.
     */
    private static int deadZone(double offset) {
        double abs = Math.abs(offset);
        if (abs <= DEAD_ZONE) {
            return 0;
        }
        return (int) Math.round(Math.signum(offset) * (abs - DEAD_ZONE));
    }

    private static final class Region {

        private final SoundCategory category;
        private int sumX;
        private int sumY;
        private int count;
        private int weight;

        Region(SoundCategory category) {
            this.category = category;
        }

        void add(int x, int y) {
            sumX += x;
            sumY += y;
            count++;
            weight += TENT_WEIGHTS[x] + TENT_WEIGHTS[y];
        }

        double centroidX() {
            return (double) sumX / count;
        }

        double centroidY() {
            return (double) sumY / count;
        }
    }

    private final class Bed {

        private final AmbienceLayer first;
        @Nullable
        private AmbienceLayer second;
        private final SoundCategory category;
        private Point position;

        Bed(SoundCategory category, Point position) {
            this.category = category;
            this.position = position;
            this.first = new AmbienceLayer(engine, category, LAYER_ONE, position, 0);
        }

        void update(long now, Point position) {
            this.position = position;
            first.move(position);
            first.tick(now);

            // the second layer is a luxury: it needs a spare voice, and is not started without
            if (second == null && category.hasEvent(LAYER_TWO)
                    && SoundEngine.MAX_VOICES - engine.getActiveVoiceCount() > 1) {
                second = new AmbienceLayer(engine, category, LAYER_TWO, position, 0);
            }
            if (second != null) {
                second.move(position);
                second.tick(now);
            }
        }

        void stop() {
            first.stop();
            if (second != null) {
                second.stop();
            }
        }
    }
}
