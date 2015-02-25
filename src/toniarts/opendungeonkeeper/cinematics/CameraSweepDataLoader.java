/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.cinematics;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.export.binary.BinaryImporter;
import java.io.IOException;

/**
 * Just a resource loader for the camera sweep data files
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CameraSweepDataLoader implements AssetLoader {

    public static final String CAMERA_SWEEP_DATA_FILE_EXTENSION = "csd";

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        BinaryImporter importer = BinaryImporter.getInstance();
        return importer.load(assetInfo);
    }
}
