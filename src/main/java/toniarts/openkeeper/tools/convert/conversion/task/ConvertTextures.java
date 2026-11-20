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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.textures.enginetextures.EngineTexturesFile;
import toniarts.openkeeper.tools.convert.textures.loadingscreens.LoadingScreenFile;
import toniarts.openkeeper.tools.convert.wad.WadFile;
import toniarts.openkeeper.utils.PathUtils;
import toniarts.openkeeper.utils.Utils;

/**
 * Dungeon Keeper II textures conversion. Converts textures to PNG.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class ConvertTextures extends ConversionTask {

    private static final Logger logger = System.getLogger(ConvertTextures.class.getName());

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
            boolean interrupted = false;
            while (!executorService.isTerminated()) {
                try {
                    executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
                } catch (InterruptedException ex) {
                    logger.log(Level.ERROR, "Interrupted while waiting for texture extraction to finish!", ex);
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        copyEnhancedLiquidTextures(dungeonKeeperFolder, destination);
    }

    /**
     * Extract and copy DK II textures
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination Destination folder
     */
    private void convertTextures(String dungeonKeeperFolder, String destination) {
        logger.log(Level.INFO, "Extracting textures to: {0}", destination);
        updateStatus(null, null);
        PathUtils.deleteFolder(Paths.get(destination));
        EngineTexturesFile etFile = getEngineTexturesFile(dungeonKeeperFolder);
        WadFile frontEnd;
        WadFile engineTextures;
        try {
            frontEnd = new WadFile(Paths.get(PathUtils.getRealFileName(dungeonKeeperFolder + PathUtils.DKII_DATA_FOLDER, "FrontEnd.WAD")));
            engineTextures = new WadFile(Paths.get(PathUtils.getRealFileName(dungeonKeeperFolder + PathUtils.DKII_DATA_FOLDER, "EngineTextures.WAD")));
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
     * Some DKII releases ship the G400 liquid textures as loose files beside
     * the texture cache. Copy only this known set into the converted asset
     * directory; all other textures continue through the normal WAD/cache path.
     */
    private static void copyEnhancedLiquidTextures(String dungeonKeeperFolder, String destination) {
        final Path source;
        try {
            source = Paths.get(PathUtils.getRealFileName(PathUtils.fixFilePath(dungeonKeeperFolder),
                    PathUtils.DKII_DATA_FOLDER.concat("EngineTextures")));
        } catch (IOException ex) {
            logger.log(Level.INFO, "No loose DKII enhanced-liquid texture directory found.");
            return;
        }
        copyEnhancedLiquidTextures(source, Paths.get(destination));
    }

    static void copyEnhancedLiquidTextures(Path source, Path destination) {
        if (!Files.isDirectory(source)) {
            return;
        }

        Map<String, Path> sourceFiles = new HashMap<>();
        try (var paths = Files.list(source)) {
            paths.filter(Files::isRegularFile).forEach(path ->
                    sourceFiles.put(path.getFileName().toString().toLowerCase(Locale.ROOT), path));
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to list DKII enhanced-liquid textures from " + source + ".", ex);
            return;
        }

        List<String> names = new ArrayList<>(List.of(
                "BumpLava0.png", "BumpLavaBumpmap0.png", "BumpLavaEnvMap0.png",
                "BumpWater0.png", "BumpWaterEnvMap0.png"));
        for (int frame = 0; frame < 32; frame++) {
            names.add("BumpWaterBumpmap" + frame + ".png");
        }

        int copied = 0;
        for (String name : names) {
            Path input = sourceFiles.get(name.toLowerCase(Locale.ROOT));
            if (input == null) {
                continue;
            }
            try {
                Files.createDirectories(destination);
                Files.copy(input, destination.resolve(name), StandardCopyOption.REPLACE_EXISTING);
                copied++;
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Failed to copy enhanced-liquid texture " + input + ".", ex);
            }
        }
        if (copied > 0) {
            logger.log(Level.INFO, "Copied {0} DKII enhanced-liquid textures to {1}.", copied, destination);
        }
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
            EngineTexturesFile etFile = new EngineTexturesFile(Paths.get(PathUtils.getRealFileName(dungeonKeeperFolder, "DK2TextureCache".concat(FileSystems.getDefault().getSeparator()).concat("EngineTextures.dat"))));
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
                        logger.log(Level.INFO, "File {0} already exists, skipping!", newFile);
                        Files.delete(f);
                        updateStatus(progress.incrementAndGet(), total);

                        return;
                    }
                    Files.move(f, newFile);
                } catch (IOException ex) {
                    logger.log(Level.ERROR, "Failed to handle " + textureFile + "!", ex);
                    onError(new RuntimeException("Failed to handle " + textureFile + "!", ex));

                    return;
                }
            } else if (!found) {
                try {

                    // No mipmap levels, just extract
                    etFile.extractFileData(textureFile, destination, overwriteData);
                } catch (Exception ex) {
                    logger.log(Level.ERROR, "Failed to extract the texture file entry " + textureFile + "!", ex);
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
        ImageIO.setUseCache(false);
        for (final String entry : wad.getWadFileEntries()) {

            // Some of these archives contain .444 files, convert these to PNGs
            if (entry.endsWith(".444")) {
                LoadingScreenFile lsf = new LoadingScreenFile(wad.getFileData(entry));
                try {
                    Path destFile = Paths.get(destination, entry.substring(0, entry.length() - 3).concat("png"));
                    Files.createDirectories(destFile.getParent());
                    try (OutputStream os = Files.newOutputStream(destFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                            BufferedOutputStream bos = new BufferedOutputStream(os)) {
                        ImageIO.write(lsf.getImage(), "png", bos);
                    }
                } catch (IOException ex) {
                    logger.log(Level.ERROR, "Failed to save the wad entry " + entry + "!", ex);
                    onError(new RuntimeException("Failed to save the wad entry " + entry + "!", ex));

                    return;
                }
            } else {
                try {
                    wad.extractFileData(entry, destination);
                } catch (Exception ex) {
                    logger.log(Level.ERROR, "Failed to extract the wad entry " + entry + "!", ex);
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
