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
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import static toniarts.openkeeper.tools.convert.AssetsConverter.getAssetsFolder;
import toniarts.openkeeper.tools.convert.KmfAssetInfo;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.kmf.KmfFile;
import toniarts.openkeeper.tools.convert.wad.WadFile;
import toniarts.openkeeper.utils.PathUtils;
import toniarts.openkeeper.utils.Utils;

/**
 * Dungeon Keeper II models conversion. Converts KMF to jME internal optimized
 * format.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ConvertModels extends ConversionTask {

    private static final Logger logger = System.getLogger(ConvertModels.class.getName());

    private final AssetManager assetManager;
    private final ExecutorService executorService;

    public ConvertModels(String dungeonKeeperFolder, String destination, boolean overwriteData, AssetManager assetManager) {
        super(dungeonKeeperFolder, destination, overwriteData);

        this.assetManager = assetManager;
        this.executorService = Executors.newFixedThreadPool(Utils.MAX_THREADS, new ThreadFactory() {

            private final AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "ModelExporter_" + threadIndex.incrementAndGet());
            }

        });
    }

    @Override
    public void internalExecuteTask() {
        try {
            convertModels(dungeonKeeperFolder, destination, assetManager);
        } finally {
            executorService.shutdown();
            try {
                executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            } catch (InterruptedException ex) {
                logger.log(Level.ERROR, "Failed to wait model saving complete!", ex);
            }
        }
    }

    /**
     * Extract and copy DK II models
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination Destination folder
     */
    private void convertModels(String dungeonKeeperFolder, String destination, AssetManager assetManager) {
        logger.log(Level.INFO, "Extracting models to: {0}", destination);
        updateStatus(null, null);
        Path dest = Paths.get(destination);
        PathUtils.deleteFolder(dest);
        try {
            Files.createDirectories(dest);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to create destination folder " + dest + "!", ex);
        }

        // Create the materials folder or else the material file saving fails
        Path materialFolder = Paths.get(getAssetsFolder(), AssetsConverter.MATERIALS_FOLDER);
        PathUtils.deleteFolder(materialFolder);
        try {
            Files.createDirectories(materialFolder);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to create destination folder " + materialFolder + "!", ex);
        }

        // Meshes are in the data folder, access the packed file
        WadFile wad;
        try {
            wad = new WadFile(Paths.get(PathUtils.getRealFileName(dungeonKeeperFolder + PathUtils.DKII_DATA_FOLDER, "Meshes.WAD")));
        } catch (IOException ex) {
            throw new RuntimeException("Could not open the meshes.wad archive!", ex);
        }
        Map<String, KmfFile> kmfs = new LinkedHashMap<>(wad.getWadFileEntryCount());
        AtomicInteger progress = new AtomicInteger(0);
        int total = wad.getWadFileEntryCount();
        for (final String entry : wad.getWadFileEntries()) {
            try {

                // See if we already have this model
                if (!overwriteData && Files.exists(Paths.get(destination, entry.substring(0, entry.length() - 4).concat(".j3o")))) {
                    logger.log(Level.INFO, "File {0} already exists, skipping!", entry);
                    updateStatus(progress.incrementAndGet(), total);
                    continue;
                }

                // Parse
                final KmfFile kmfFile = new KmfFile(wad.getFileData(entry));

                // If it is a regular model or animation, process it straight away
                // Leave groups for later (since linking)
                if (kmfFile.getType() == KmfFile.Type.MESH || kmfFile.getType() == KmfFile.Type.ANIM) {
                    convertModel(assetManager, entry, kmfFile, destination, total, progress);
                } else {

                    // For later processing
                    kmfs.put(entry, kmfFile);
                }

                // See if model conversion already failed
                if (isInError()) {
                    return;
                }
            } catch (Exception ex) {
                logger.log(Level.ERROR, "Failed to create a file for WAD entry " + entry + "!", ex);
                throw ex;
            }
        }

        // And the groups (now they can be linked)
        for (Map.Entry<String, KmfFile> entry : kmfs.entrySet()) {
            convertModel(assetManager, entry.getKey(), entry.getValue(), destination, total, progress);

            // See if model conversion already failed
            if (isInError()) {
                return;
            }
        }
    }

    /**
     * Convert a single KMF to JME object
     *
     * @param assetManager assetManager, for finding stuff
     * @param name model name
     * @param model the loaded KMF model
     * @param destination destination directory
     * @param total the total amount to process
     * @param progress current progress
     * @throws RuntimeException May fail
     */
    private void convertModel(AssetManager assetManager, String name, KmfFile model, String destination, int total, AtomicInteger progress) throws RuntimeException {

        // Remove the file extension from the file
        KmfAssetInfo ai = new KmfAssetInfo(assetManager, new AssetKey(name), model, true);
        KmfModelLoader kmfModelLoader = new KmfModelLoader();
        try {
            Node n = (Node) kmfModelLoader.load(ai);

            // Handle the saving to the disk in own thread pool
            executorService.submit(() -> {
                try {
                    BinaryExporter exporter = BinaryExporter.getInstance();
                    try (OutputStream out = Files.newOutputStream(Paths.get(destination, name.substring(0, name.length() - 4).concat(".j3o")));
                            BufferedOutputStream bout = new BufferedOutputStream(out)) {
                        exporter.save(n, bout);
                    }

                    updateStatus(progress.incrementAndGet(), total);
                } catch (Exception ex) {
                    String msg = "Failed to export KMF entry " + name + "!";
                    logger.log(Level.ERROR, msg, ex);
                    onError(new RuntimeException(msg, ex));
                }
            });
        } catch (Exception ex) {
            String msg = "Failed to convert KMF entry " + name + "!";
            logger.log(Level.ERROR, msg, ex);
            throw new RuntimeException(msg, ex);
        }
    }

    @Override
    public AssetsConverter.ConvertProcess getConvertProcess() {
        return AssetsConverter.ConvertProcess.MODELS;
    }

}
