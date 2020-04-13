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
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
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
        File destFolder = new File(destination);
        AssetUtils.deleteFolder(destFolder);

        // Make sure it exists
        destFolder.mkdirs();
        try {

            // Get the skirmish/mp maps
            File f = new File(dungeonKeeperFolder + PathUtils.DKII_MAPS_FOLDER);
            File[] files = f.listFiles(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".kwd");
                }

            });

            // Read them
            List<KwdFile> maps = new ArrayList<>(files.length);
            for (File file : files) {
                KwdFile kwd = new KwdFile(dungeonKeeperFolder, file, false);
                if (kwd.getGameLevel().getLvlFlags().contains(GameLevel.LevFlag.IS_SKIRMISH_LEVEL)
                        || kwd.getGameLevel().getLvlFlags().contains(GameLevel.LevFlag.IS_MULTIPLAYER_LEVEL)) {
                    maps.add(kwd);
                }
            }

            // Go through the map files
            int i = 0;
            int total = maps.size();
            for (KwdFile kwd : maps) {
                updateStatus(i, total);
                genererateMapThumbnail(kwd, destination);
                i++;
            }
        } catch (Exception ex) {
            String msg = "Failed to process the map thumbnails to " + destination + "!";
            LOGGER.log(Level.WARNING, msg, ex); // Not fatal
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
        ImageIO.write(thumbnail, "png", new File(destination + ConversionUtils.stripFileName(kwd.getGameLevel().getName()) + ".png"));
    }

    @Override
    public AssetsConverter.ConvertProcess getConvertProcess() {
        return AssetsConverter.ConvertProcess.MAP_THUMBNAILS;
    }

}
