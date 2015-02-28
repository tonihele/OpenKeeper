/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.gui.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import java.io.IOException;
import java.io.InputStream;
import toniarts.opendungeonkeeper.gui.Dictionary;

/**
 * Asset loader for dictionaries (our own dictionary format)
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class DictLoader implements AssetLoader {

    public static final String DICTIONARY_FILE_EXTENSION = "dict";

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        try (InputStream inputStream = assetInfo.openStream()) {
            return new Dictionary(inputStream);
        }
    }
}
