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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.textures.enginetextures.EngineTexturesFile;
import toniarts.openkeeper.tools.convert.textures.loadingscreens.LoadingScreenFile;
import toniarts.openkeeper.tools.convert.wad.WadFile;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.PathUtils;
import toniarts.openkeeper.utils.Utils;

/**
 * Dungeon Keeper II textures conversion. Converts textures to PNG.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ConvertTextures extends ConversionTask {

    private static final Logger LOGGER = Logger.getLogger(ConvertTextures.class.getName());

    private final ExecutorService executorService;

    public ConvertTextures(String dungeonKeeperFolder, String destination, boolean overwriteData) {
        super(dungeonKeeperFolder, destination, overwriteData);

        this.executorService = Executors.newFixedThreadPool(Utils.MAX_THREADS, new ThreadFactory() {

            private final AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "TexturesConverter_" + threadIndex.incrementAndGet());
            }

        });
    }

    @Override
    public void internalExecuteTask() {
        try {
            convertTextures(dungeonKeeperFolder, destination);
        } finally {
            executorService.shutdown();
            try {
                executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, "Failed to wait textures conversion complete!", ex);
            }
        }
    }

    /**
     * Extract and copy DK II textures
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination Destination folder
     */
    private void convertTextures(String dungeonKeeperFolder, String destination) {
        LOGGER.log(Level.INFO, "Extracting textures to: {0}", destination);
        updateStatus(null, null);
        AssetUtils.deleteFolder(Paths.get(destination));
        EngineTexturesFile etFile = getEngineTexturesFile(dungeonKeeperFolder);
        WadFile frontEnd;
        WadFile engineTextures;
        try {
            frontEnd = new WadFile(Paths.get(ConversionUtils.getRealFileName(dungeonKeeperFolder + PathUtils.DKII_DATA_FOLDER, "FrontEnd.WAD")));
            engineTextures = new WadFile(Paths.get(ConversionUtils.getRealFileName(dungeonKeeperFolder + PathUtils.DKII_DATA_FOLDER, "EngineTextures.WAD")));
        } catch (IOException e) {
            throw new RuntimeException("Failed to open a WAD file!", e);
        }

        AtomicInteger progress = new AtomicInteger(0);
        int total = etFile.getFileCount() + frontEnd.getWadFileEntries().size() + engineTextures.getWadFileEntries().size();

        // Process each container in its own thread
        executorService.submit(() -> {
            extractEngineTextureContainer(progress, total, etFile, destination);
        });
        executorService.submit(() -> {
            extractTextureContainer(progress, total, frontEnd, destination);
        });
        executorService.submit(() -> {
            extractTextureContainer(progress, total, engineTextures, destination);
        });
    }

    /**
     * Loads up an instance of the engine textures catalog
     *
     * @param dungeonKeeperFolder DK II folder
     * @return EngineTextures catalog
     */
    public static EngineTexturesFile getEngineTexturesFile(String dungeonKeeperFolder) {

        // Get the engine textures file
        try {
            EngineTexturesFile etFile = new EngineTexturesFile(Paths.get(ConversionUtils.getRealFileName(dungeonKeeperFolder, "DK2TextureCache".concat(FileSystems.getDefault().getSeparator()).concat("EngineTextures.dat"))));
            return etFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to open the EngineTextures file!", e);
        }
    }

    private void extractEngineTextureContainer(AtomicInteger progress, int total, EngineTexturesFile etFile, String destination) throws NumberFormatException {
        Pattern pattern = Pattern.compile("(?<name>\\w+)MM(?<mipmaplevel>\\d{1})");
        for (String textureFile : etFile) {

            // All are PNG files, and MipMap levels are present, we need only the
            // highest quality one, so don't bother extracting the other mipmap levels
            Matcher matcher = pattern.matcher(textureFile);
            boolean found = matcher.find();
            if (found && Integer.parseInt(matcher.group("mipmaplevel")) == 0) {

                // Highest resolution, extract and rename
                Path f = etFile.extractFileData(textureFile, destination, overwriteData);
                Path newFile = Paths.get(f.toString().replaceFirst("MM" + matcher.group("mipmaplevel"), ""));
                try {
                    if (overwriteData && Files.exists(newFile)) {
                        Files.delete(newFile);
                    } else if (!overwriteData && Files.exists(newFile)) {

                        // Delete the extracted file
                        LOGGER.log(Level.INFO, "File {0} already exists, skipping!", newFile);
                        Files.delete(f);
                        updateStatus(progress.incrementAndGet(), total);

                        return;
                    }
                    Files.move(f, newFile);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Failed to handle " + textureFile + "!", ex);
                    onError(new RuntimeException("Failed to handle " + textureFile + "!", ex));

                    return;
                }
            } else if (!found) {
                try {

                    // No mipmap levels, just extract
                    etFile.extractFileData(textureFile, destination, overwriteData);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Failed to extract the texture file entry " + textureFile + "!", ex);
                    onError(new RuntimeException("Failed to save the texture file entry " + textureFile + "!", ex));

                    return;
                }
            }
            updateStatus(progress.incrementAndGet(), total);
        }
    }

    /**
     * Extracts the wad files and updates the progress bar
     *
     * @param i current entry number
     * @param total total entry number
     * @param wad wad file
     * @param destination destination directory
     */
    private void extractTextureContainer(AtomicInteger progress, int total, WadFile wad, String destination) {
        for (final String entry : wad.getWadFileEntries()) {

            // Some of these archives contain .444 files, convert these to PNGs
            if (entry.endsWith(".444")) {
                LoadingScreenFile lsf = new LoadingScreenFile(wad.getFileData(entry));
                try {
                    Path destFile = Paths.get(destination, entry.substring(0, entry.length() - 3).concat("png"));
                    Files.createDirectories(destFile.getParent());
                    ImageIO.write(lsf.getImage(), "png", destFile.toFile());
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Failed to save the wad entry " + entry + "!", ex);
                    onError(new RuntimeException("Failed to save the wad entry " + entry + "!", ex));

                    return;
                }
            } else {
                try {
                    wad.extractFileData(entry, destination);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Failed to extract the wad entry " + entry + "!", ex);
                    onError(new RuntimeException("Failed to save the wad entry " + entry + "!", ex));

                    return;
                }
            }

            updateStatus(progress.incrementAndGet(), total);
        }
    }

    @Override
    public AssetsConverter.ConvertProcess getConvertProcess() {
        return AssetsConverter.ConvertProcess.TEXTURES;
    }

}
