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

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import toniarts.openkeeper.game.sound.SoundCategory;
import toniarts.openkeeper.game.sound.SoundFile;
import toniarts.openkeeper.tools.convert.AssetsConverter;

/**
 *
 * @author archdemon
 */
public class SoundsLoader {

    private static final Map<String, SoundCategory> cache = new HashMap<>();
    private static final Logger logger = Logger.getLogger(SoundsLoader.class.getName());
    private final AssetManager assetManager;

    public SoundsLoader(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    @Nullable
    public static SoundCategory load(String soundCategory) {
        return load(soundCategory, true);
    }

    @Nullable
    public static SoundCategory load(String soundCategory, boolean useGlobal) {

        if (soundCategory != null && !soundCategory.isEmpty() && cache.containsKey(soundCategory)) {
            return cache.get(soundCategory);
        }

        try {
            SoundCategory result = new SoundCategory(soundCategory, useGlobal);
            cache.put(soundCategory, result);
            return result;
        } catch (Exception e) {
            logger.severe(e.getLocalizedMessage());
            return null;
        }
    }

    public AudioNode getAudioNode(SoundFile file) {
        return getAudioNode(assetManager, file);
    }

    public static AudioNode getAudioNode(AssetManager assetManager, SoundFile file) {
        return new AudioNode(assetManager,
                AssetsConverter.SOUNDS_FOLDER + File.separator + file.getFilename(),
                AudioData.DataType.Buffer);
    }
}
