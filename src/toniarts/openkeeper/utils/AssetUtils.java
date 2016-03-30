/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.utils;

import com.jme3.asset.AssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.asset.cache.AssetCache;
import com.jme3.asset.cache.SimpleAssetCache;
import com.jme3.asset.cache.WeakRefAssetCache;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.tools.convert.ConversionUtils;

/**
 * Collection of asset related common functions
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class AssetUtils {

    private final static AssetCache assetCache = new SimpleAssetCache();
    private final static AssetCache weakAssetCache = new WeakRefAssetCache();

    private AssetUtils() {
        // Nope
    }

    /**
     * Loads a model. The model is cached on the first call and loaded from
     * cache.
     *
     * @param assetManager the asset manager to use
     * @param resourceName the model name, the model name is checked and fixed
     * @param useWeakCache use weak cache, if not then permanently cache the
     * models. Use weak cache to load some models that are not often needed
     * (water bed etc.)
     * @return a cloned instance from the cache
     */
    public static Spatial loadModel(AssetManager assetManager, String resourceName, boolean useWeakCache) {
        ModelKey assetKey = new ModelKey(ConversionUtils.getCanonicalAssetKey(resourceName));

        // Set the correct asset cache
        final AssetCache cache;
        if (useWeakCache) {
            cache = weakAssetCache;
        } else {
            cache = assetCache;
        }

        // Get the model from cache
        Spatial model = cache.getFromCache(assetKey);
        if (model == null) {
            model = assetManager.loadModel(assetKey);
            cache.addToCache(assetKey, model);
        }
        return model.clone();
    }

}
