/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.tools.modelviewer;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import toniarts.openkeeper.game.sound.SoundCategory;

/**
 *
 * @author archdemon
 */
public final class SoundsLoader {
    
    private static final Logger logger = System.getLogger(SoundsLoader.class.getName());
    
    private static final Map<String, SoundCategory> CACHE = new HashMap<>();

    private SoundsLoader() {
        // nope
    }

    @Nullable
    public static SoundCategory load(final String soundCategory) {
        return load(soundCategory, true);
    }

    @Nullable
    public static SoundCategory load(final String category, final boolean useGlobal) {

        if (category == null || category.isEmpty()) {
            return null;
        }

        // the same category name may exist in the global and in a non global folder
        String key = (useGlobal ? "global:" : "local:") + category.toUpperCase();
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }

        SoundCategory result = null;
        try {
            result = new SoundCategory(category, useGlobal);
            if (result.getEvents().isEmpty()) {
                logger.log(Level.WARNING, "Sound category {0} has no events", category);
                result = null;
            } else {
                CACHE.put(key, result);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Sound category {0} does not exist", category);
        }

        return result;
    }
}
