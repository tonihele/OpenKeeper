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

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.GameLevel;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.MapThumbnailGenerator;
import toniarts.openkeeper.utils.PathUtils;

/**
 * Dungeon Keeper II map thumbnail generation. The original has a few thumbnails
 * in BMP format, but they don't just cut it. Bake our own and also from custom
 * maps.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ConvertMapThumbnails extends ConversionTask {

    private static final Logger LOGGER = Logger.getLogger(ConvertMapThumbnails.class.getName());

    public ConvertMapThumbnails(String dungeonKeeperFolder, String destination, boolean overwriteData) {
        super(dungeonKeeperFolder, destination, overwriteData);
    }

    @Override
    public void internalExecuteTask() {
        generateMapThumbnails(dungeonKeeperFolder, destination);
    }

    /**
     * Generates thumbnails out of map files (only the skirmish/mp)
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination Destination folder
     */
    private void generateMapThumbnails(String dungeonKeeperFolder, String destination) {
        LOGGER.log(Level.INFO, "Generating map thumbnails to: {0}", destination);
        updateStatus(null, null);
        Path destFolder = Paths.get(destination);
        AssetUtils.deleteFolder(destFolder);

        try {

            // Make sure it exists
            Files.createDirectories(destFolder);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to create destination folder " + destFolder + "!", ex);
        }

        // Get the skirmish/mp maps
        List<KwdFile> maps = new ArrayList<>();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get(dungeonKeeperFolder, PathUtils.DKII_MAPS_FOLDER), PathUtils.getFilterForFilesEndingWith(".kwd"))) {
            for (Path path : paths) {
                try {
                    KwdFile kwd = new KwdFile(dungeonKeeperFolder, path, false);
                    if (kwd.getGameLevel().getLvlFlags().contains(GameLevel.LevFlag.IS_SKIRMISH_LEVEL)
                            || kwd.getGameLevel().getLvlFlags().contains(GameLevel.LevFlag.IS_MULTIPLAYER_LEVEL)) {
                        maps.add(kwd);
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Failed to open map file: " + path + "!", ex); // Not fatal
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to search for the map files!", ex);
        }

        // Go through the map files
        int i = 0;
        int total = maps.size();
        ImageIO.setUseCache(false);
        for (KwdFile kwd : maps) {
            updateStatus(i, total);
            try {
                genererateMapThumbnail(kwd, destination);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to create a thumbnail from map: " + kwd.getGameLevel().getName() + "!", ex); // Not fatal
            }
            i++;
        }
    }

    /**
     * Generates a map thumbnail out of the given map file
     *
     * @param kwd map file
     * @param destination the folder to save to
     * @throws IOException may fail
     */
    public static void genererateMapThumbnail(KwdFile kwd, String destination) throws IOException {

        // Create the thumbnail & save it
        // TODO maybe image size in Settings ???
        BufferedImage thumbnail = MapThumbnailGenerator.generateMap(kwd, 144, 144, false);

        Path destinationPath = Paths.get(destination, ConversionUtils.stripFileName(kwd.getGameLevel().getName()) + ".png");
        try (OutputStream os = Files.newOutputStream(destinationPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                BufferedOutputStream bos = new BufferedOutputStream(os)) {
            ImageIO.write(thumbnail, "png", bos);
        }
    }

    @Override
    public AssetsConverter.ConvertProcess getConvertProcess() {
        return AssetsConverter.ConvertProcess.MAP_THUMBNAILS;
    }

}
