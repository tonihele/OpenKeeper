/*
 * Copyright (C) 2014-2020 OpenKeeper
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
package toniarts.openkeeper.tools.convert.conversion.task;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.scene.Node;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import static toniarts.openkeeper.tools.convert.AssetsConverter.getAssetsFolder;
import toniarts.openkeeper.tools.convert.KmfAssetInfo;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.kmf.KmfFile;
import toniarts.openkeeper.tools.convert.wad.WadFile;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.PathUtils;

/**
 * Dungeon Keeper II models conversion. Converts KMF to jME internal optimized
 * format.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ConvertModels extends ConversionTask {

    private static final Logger LOGGER = Logger.getLogger(ConvertModels.class.getName());

    private final AssetManager assetManager;

    public ConvertModels(String dungeonKeeperFolder, String destination, boolean overwriteData, AssetManager assetManager) {
        super(dungeonKeeperFolder, destination, overwriteData);
        this.assetManager = assetManager;
    }

    @Override
    public void internalExecuteTask() {
        convertModels(dungeonKeeperFolder, destination, assetManager);
    }

    /**
     * Extract and copy DK II models
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination Destination folder
     */
    private void convertModels(String dungeonKeeperFolder, String destination, AssetManager assetManager) {
        LOGGER.log(Level.INFO, "Extracting models to: {0}", destination);
        updateStatus(null, null);
        AssetUtils.deleteFolder(new File(destination));

        // Create the materials folder or else the material file saving fails
        File materialFolder = new File(getAssetsFolder().concat(AssetsConverter.MATERIALS_FOLDER));
        AssetUtils.deleteFolder(materialFolder);
        materialFolder.mkdirs();

        // Meshes are in the data folder, access the packed file
        WadFile wad = new WadFile(new File(dungeonKeeperFolder + PathUtils.DKII_DATA_FOLDER + "Meshes.WAD"));
        Map<String, KmfFile> kmfs = new HashMap<>();
        File tmpdir = new File(System.getProperty("java.io.tmpdir"));
        int i = 0;
        int total = wad.getWadFileEntryCount();
        for (final String entry : wad.getWadFileEntries()) {
            try {
                updateStatus(i, total);

                // See if we already have this model
                if (!overwriteData && new File(destination.concat(entry.substring(0, entry.length() - 4)).concat(".j3o")).exists()) {
                    LOGGER.log(Level.INFO, "File {0} already exists, skipping!", entry);
                    i++;
                    continue;
                }

                // Extract each file to temp
                File f = wad.extractFileData(entry, tmpdir.toString());
                f.deleteOnExit();

                // Parse
                final KmfFile kmfFile = new KmfFile(f);

                // If it is a regular model or animation, process it straight away
                // Leave groups for later (since linking)
                if (kmfFile.getType() == KmfFile.Type.MESH || kmfFile.getType() == KmfFile.Type.ANIM) {
                    convertModel(assetManager, new Map.Entry<String, KmfFile>() {
                        @Override
                        public String getKey() {
                            return entry;
                        }

                        @Override
                        public KmfFile getValue() {
                            return kmfFile;
                        }

                        @Override
                        public KmfFile setValue(KmfFile value) {
                            throw new UnsupportedOperationException("Plz, don't do this!");
                        }
                    }, destination);

                    // We can delete the file straight
                    f.delete();
                    i++;
                } else {

                    // For later processing
                    kmfs.put(entry, kmfFile);
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to create a file for WAD entry " + entry + "!", ex);
                throw ex;
            }
        }

        // And the groups (now they can be linked)
        for (Map.Entry<String, KmfFile> entry : kmfs.entrySet()) {
            updateStatus(i, total);
            convertModel(assetManager, entry, destination);
            i++;
        }
    }

    /**
     * Convert a single KMF to JME object
     *
     * @param assetManager assetManager, for finding stuff
     * @param entry KMF / name entry
     * @param destination destination directory
     * @throws RuntimeException May fail
     */
    private void convertModel(AssetManager assetManager, Map.Entry<String, KmfFile> entry, String destination) throws RuntimeException {

        // Remove the file extension from the file
        KmfAssetInfo ai = new KmfAssetInfo(assetManager, new AssetKey(entry.getKey()), entry.getValue(), true);
        KmfModelLoader kmfModelLoader = new KmfModelLoader();
        try {
            Node n = (Node) kmfModelLoader.load(ai);

            // Export
            BinaryExporter exporter = BinaryExporter.getInstance();
            File file = new File(destination.concat(entry.getKey().substring(0, entry.getKey().length() - 4)).concat(".j3o"));
            exporter.save(n, file);
        } catch (Exception ex) {
            String msg = "Failed to convert KMF entry " + entry.getKey() + "!";
            LOGGER.log(Level.SEVERE, msg, ex);
            throw new RuntimeException(msg, ex);
        }
    }

    @Override
    public AssetsConverter.ConvertProcess getConvertProcess() {
        return AssetsConverter.ConvertProcess.MODELS;
    }

}
