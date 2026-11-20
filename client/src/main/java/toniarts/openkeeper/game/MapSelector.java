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
import toniarts.openkeeper.Main;
import toniarts.openkeeper.tools.convert.map.GameLevel.LevFlag;
import toniarts.openkeeper.tools.convert.map.IKwdMap;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.utils.PathUtils;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Class isolate map selection
 *
 * @author ArchDemon
 */
public final class MapSelector {

    private static final Logger logger = System.getLogger(MapSelector.class.getName());

    private final List<IKwdMap> skirmishMaps = new ArrayList<>();
    private final List<IKwdMap> multiplayerMaps = new ArrayList<>();
    private final List<IKwdMap> mpdMaps = new ArrayList<>();
    private IKwdMap map;
    private boolean skirmish;
    private boolean mpd;

    public MapSelector() {
        // Get the maps
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(Main.getDkIIFolder(),
                PathUtils.DKII_MAPS_FOLDER), PathUtils.getFilterForFilesEndingWith(".kwd"))) {
            for (Path file : stream) {
                // Read the map
                IKwdMap kwd = KwdFile.loadInfo(file);

                if (kwd.getGameLevel().getLvlFlags().contains(LevFlag.IS_SKIRMISH_LEVEL)) {
                    skirmishMaps.add(kwd);
                }
                if (kwd.getGameLevel().getLvlFlags().contains(LevFlag.IS_MULTIPLAYER_LEVEL)) {
                    multiplayerMaps.add(kwd);
                }
                if (kwd.getGameLevel().getLvlFlags().contains(LevFlag.IS_MY_PET_DUNGEON_LEVEL)) {
                    mpdMaps.add(kwd);
                }
            }
        } catch (IOException ex) {
            logger.log(Level.ERROR, "Failed to load the maps!", ex);
        }

        // Sort them
        Collections.sort(skirmishMaps);
        Collections.sort(multiplayerMaps);
        Collections.sort(mpdMaps);
    }

    public void random() {
        IKwdMap current;
        List<IKwdMap> maps = getMaps();

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

    public IKwdMap getMap() {
        if (map == null) {
            random();
        }
        return map;
    }

    public void selectMap(int index) {
        map = getMaps().get(index);
    }

    public List<IKwdMap> getMaps() {
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
     * @param name the map name
     * @return the map, or {@code null} if not found
     */
    public IKwdMap getMap(String name) {
        int index = Collections.binarySearch(getMaps(), name, new MapComparator());
        if (index >= 0) {
            map = getMaps().get(index);
            return map;
        }

        return null;
    }

    /**
     * Compares the maps by their name
     */
    private static final class MapComparator implements Comparator<Comparable<?>> {

        @Override
        public int compare(Comparable<?> o1, Comparable<?> o2) {
            String mapName1 = o1 instanceof IKwdMap kwdMap ? kwdMap.getGameLevel().getName() : o1.toString();
            String mapName2 = o2 instanceof IKwdMap kwdMap ? kwdMap.getGameLevel().getName() : o2.toString();

            return Collator.getInstance().compare(mapName1, mapName2);
        }
    }

}
