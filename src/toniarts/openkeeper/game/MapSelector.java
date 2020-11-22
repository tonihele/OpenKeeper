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
package toniarts.openkeeper.game;

import com.jme3.math.FastMath;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.tools.convert.map.GameLevel;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.utils.PathUtils;

/**
 * Class isolate map selection
 *
 * @author ArchDemon
 */
public class MapSelector {

    private final List<GameMapContainer> skirmishMaps = new ArrayList<>();
    private final List<GameMapContainer> multiplayerMaps = new ArrayList<>();
    private final List<GameMapContainer> mpdMaps = new ArrayList<>();
    private GameMapContainer map;
    private boolean skirmish;
    private boolean mpd;

    public MapSelector() {

        // Get the maps
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(Main.getDkIIFolder() + PathUtils.DKII_MAPS_FOLDER), PathUtils.getFilterForFilesEndingWith(".kwd"))) {
            for (Path file : stream) {

                // Read the map
                KwdFile kwd = new KwdFile(Main.getDkIIFolder(), file, false);
                GameMapContainer gameMapContainer = new GameMapContainer(kwd, kwd.getGameLevel().getName());
                if (kwd.getGameLevel().getLvlFlags().contains(GameLevel.LevFlag.IS_SKIRMISH_LEVEL)) {
                    skirmishMaps.add(gameMapContainer);
                }
                if (kwd.getGameLevel().getLvlFlags().contains(GameLevel.LevFlag.IS_MULTIPLAYER_LEVEL)) {
                    multiplayerMaps.add(gameMapContainer);
                }
                if (kwd.getGameLevel().getLvlFlags().contains(GameLevel.LevFlag.IS_MY_PET_DUNGEON_LEVEL)) {
                    mpdMaps.add(gameMapContainer);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(MapSelector.class.getName()).log(Level.SEVERE, "Failed to load the maps!", ex);
        }

        // Sort them
        Comparator c = new MapComparator();
        Collections.sort(skirmishMaps, c);
        Collections.sort(multiplayerMaps, c);
        Collections.sort(mpdMaps, c);
    }

    public void random() {
        GameMapContainer current;
        List<GameMapContainer> maps = getMaps();

        if (maps.isEmpty()) {
            current = null;
        } else if (maps.size() == 1) {
            current = maps.get(0);
        } else {
            do {
                current = maps.get(FastMath.nextRandomInt(0, maps.size() - 1));
            } while (current.equals(map));
        }

        map = current;
    }

    public final void reset() {
        map = null;
        skirmish = false;
        mpd = false;
    }

    public GameMapContainer getMap() {
        if (map == null) {
            random();
        }
        return map;
    }

    public void selectMap(int index) {
        map = getMaps().get(index);
    }

    public List<GameMapContainer> getMaps() {
        if (skirmish) {
            return skirmishMaps;
        } else if (mpd) {
            return mpdMaps;
        } else {
            return multiplayerMaps;
        }
    }

    public boolean isSkirmish() {
        return skirmish;
    }

    public boolean isMPD() {
        return mpd;
    }

    public void setSkirmish(boolean skirmish) {
        if (this.skirmish != skirmish) {
            map = null;
        }

        this.skirmish = skirmish;
    }

    public void setMPD(boolean mpd) {
        if (this.mpd != mpd) {
            map = null;
        }

        this.mpd = mpd;
    }

    /**
     * Get a map by name, also sets it as the current map
     *
     * @param map the map name
     * @return the map, or {@code null} if not found
     */
    public GameMapContainer getMap(String map) {
        int index = Collections.binarySearch(getMaps(), new GameMapContainer(null, map), new MapComparator());
        if (index >= 0) {
            this.map = getMaps().get(index);
            return this.map;
        }
        return null;
    }

    /**
     * Compares the maps by their name
     */
    private class MapComparator implements Comparator<GameMapContainer> {

        @Override
        public int compare(GameMapContainer o1, GameMapContainer o2) {
            return o1.getMapName().compareToIgnoreCase(o2.getMapName());
        }

    }

    /**
     * Small container class that holds the actual map data and the name
     */
    public class GameMapContainer {

        private final KwdFile map;
        private final String mapName;

        public GameMapContainer(KwdFile map, String mapName) {
            this.map = map;
            this.mapName = mapName;
        }

        public KwdFile getMap() {
            return map;
        }

        public String getMapName() {
            return mapName;
        }

        @Override
        public String toString() {
            return mapName;
        }
    }
}
