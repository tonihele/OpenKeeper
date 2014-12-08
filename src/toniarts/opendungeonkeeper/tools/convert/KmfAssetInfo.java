/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import java.io.InputStream;
import toniarts.opendungeonkeeper.tools.convert.enginetextures.EngineTexturesFile;
import toniarts.opendungeonkeeper.tools.convert.kmf.KmfFile;

/**
 * Small wrapper class to avoid writing and reading KMF files all over again
 * during conversion (and textures)
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class KmfAssetInfo extends AssetInfo {

    private final KmfFile kmfFile;
    private final EngineTexturesFile engineTexturesFile;
    private final boolean generateMaterialFile;

    public KmfAssetInfo(AssetManager manager, AssetKey key, KmfFile kmfFile, EngineTexturesFile engineTexturesFile, boolean generateMaterialFile) {
        super(manager, key);

        this.kmfFile = kmfFile;
        this.engineTexturesFile = engineTexturesFile;
        this.generateMaterialFile = generateMaterialFile;
    }

    @Override
    public InputStream openStream() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public KmfFile getKmfFile() {
        return kmfFile;
    }

    public EngineTexturesFile getEngineTexturesFile() {
        return engineTexturesFile;
    }

    public boolean isGenerateMaterialFile() {
        return generateMaterialFile;
    }
}
