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
package toniarts.openkeeper.tools.convert;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import java.io.InputStream;
import toniarts.openkeeper.tools.convert.textures.enginetextures.EngineTexturesFile;
import toniarts.openkeeper.tools.convert.kmf.KmfFile;

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
