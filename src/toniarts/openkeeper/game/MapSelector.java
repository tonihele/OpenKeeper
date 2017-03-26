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
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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

    private List<GameMapContainer> skirmishMaps;
    private List<GameMapContainer> multiplayerMaps;
    private List<GameMapContainer> mpdMaps;
    private GameMapContainer map;
    private boolean skirmish;
    private boolean mpd;

    public MapSelector() {
        reset();

        // Get the skirmish maps
        File f = new File(Main.getDkIIFolder()+ PathUtils.DKII_MAPS_FOLDER);
        File[] files = f.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".kwd");
            }
        });

        // Read them
        multiplayerMaps = new ArrayList<>(files.length);
        skirmishMaps = new ArrayList<>(files.length);
        mpdMaps = new ArrayList<>(files.length);
        for (File file : files) {
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

        // Sort them
        Comparator c = new MapComparator();
        Collections.sort(skirmishMaps, c);
        Collections.sort(multiplayerMaps, c);
        Collections.sort(mpdMaps, c);
    }

    public void random() {
        GameMapContainer current;
        List<GameMapContainer> maps = skirmish ? skirmishMaps : multiplayerMaps;

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
        if (skirmish) {
            map = skirmishMaps.get(index);
        } else if (mpd) {
            map = mpdMaps.get(index);
        } else {
            map = multiplayerMaps.get(index);
        }
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
     * Get a map by name
     *
     * @param map the map name
     * @return the map, or {@code null} if not found
     */
    public GameMapContainer getMap(String map) {
        if (skirmish) {
            return getMap(skirmishMaps, map);
        } else if (mpd) {
            return getMap(mpdMaps, map);
        } else {
            return getMap(multiplayerMaps, map);
        }
    }

    private GameMapContainer getMap(List<GameMapContainer> mapList, String map) {
        int index = Collections.binarySearch(skirmishMaps, new GameMapContainer(null, map), new MapComparator());
        if (index >= 0) {
            return mapList.get(index);
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

    }
}
