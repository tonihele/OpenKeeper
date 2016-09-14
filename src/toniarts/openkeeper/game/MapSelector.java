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
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.GameLevel;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 * Class isolate map selection
 *
 * @author ArchDemon
 */
public class MapSelector {

    private List<KwdFile> skirmishMaps;
    private List<KwdFile> multiplayerMaps;
    private List<KwdFile> mpdMaps;
    private KwdFile map;
    private boolean skirmish;
    private boolean mpd;

    public MapSelector() {
        reset();
        // Get the skirmish maps
        File f = new File(Main.getDkIIFolder().concat(AssetsConverter.MAPS_FOLDER));
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
            if (kwd.getGameLevel().getLvlFlags().contains(GameLevel.LevFlag.IS_SKIRMISH_LEVEL)) {
                skirmishMaps.add(kwd);
            }
            if (kwd.getGameLevel().getLvlFlags().contains(GameLevel.LevFlag.IS_MULTIPLAYER_LEVEL)) {
                multiplayerMaps.add(kwd);
            }
            if (kwd.getGameLevel().getLvlFlags().contains(GameLevel.LevFlag.IS_MY_PET_DUNGEON_LEVEL)) {
                mpdMaps.add(kwd);
            }
        }

        // Sort them
        Comparator c = new Comparator<KwdFile>() {
            @Override
            public int compare(KwdFile o1, KwdFile o2) {
                return o1.getGameLevel().getName().compareToIgnoreCase(o2.getGameLevel().getName());
            }
        };
        Collections.sort(skirmishMaps, c);
        Collections.sort(multiplayerMaps, c);
        Collections.sort(mpdMaps, c);
    }

    public void random() {
        KwdFile current;
        List<KwdFile> maps = skirmish ? skirmishMaps : multiplayerMaps;

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

    public KwdFile getMap() {
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

    public List<KwdFile> getMaps() {
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
}
