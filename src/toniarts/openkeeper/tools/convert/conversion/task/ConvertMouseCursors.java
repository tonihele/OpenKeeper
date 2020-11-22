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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import static toniarts.openkeeper.tools.convert.AssetsConverter.SPRITES_FOLDER;
import static toniarts.openkeeper.tools.convert.AssetsConverter.getAssetsFolder;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.spr.SprFile;
import toniarts.openkeeper.tools.convert.wad.WadFile;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.PathUtils;

/**
 * Dungeon Keeper II mouse cursors conversion. Extracts all mouse cursors to PNG
 * files.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ConvertMouseCursors extends ConversionTask {

    private static final Logger LOGGER = Logger.getLogger(ConvertMouseCursors.class.getName());

    public ConvertMouseCursors(String dungeonKeeperFolder, String destination, boolean overwriteData) {
        super(dungeonKeeperFolder, destination, overwriteData);
    }

    @Override
    public void internalExecuteTask() {
        convertMouseCursors(dungeonKeeperFolder, destination);
    }

    /**
     * Extract and copy DK II mouse cursors
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination Destination folder
     */
    private void convertMouseCursors(String dungeonKeeperFolder, String destination) {
        LOGGER.log(Level.INFO, "Extracting mouse cursors to: {0}", destination);
        updateStatus(null, null);
        AssetUtils.deleteFolder(new File(destination));

        // Mouse cursors are PNG files in the Sprite.WAD
        WadFile wadFile;
        try {
            wadFile = new WadFile(Paths.get(ConversionUtils.getRealFileName(dungeonKeeperFolder + PathUtils.DKII_DATA_FOLDER, "Sprite.WAD")));
        } catch (IOException ex) {
            throw new RuntimeException("Could not open the Sprite.wad archive!", ex);
        }
        int i = 0;
        int total = wadFile.getWadFileEntryCount();
        File destinationFolder = new File(getAssetsFolder().concat(SPRITES_FOLDER).concat(File.separator));
        AssetUtils.deleteFolder(destinationFolder);
        destinationFolder.mkdirs();

        for (String fileName : wadFile.getWadFileEntries()) {
            updateStatus(i, total);
            i++;

            // Extract the file
            Path extracted = wadFile.extractFileData(fileName, destination);

            if (fileName.toLowerCase().endsWith(".spr")) {

                // Extract the spr and delete it afterwards
                SprFile sprFile = new SprFile(extracted);
                try {
                    sprFile.extract(destinationFolder.getPath(), fileName.substring(0, fileName.length() - 4));
                    Files.delete(extracted);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error Sprite: {0}", ex);
                }
            }
        }
    }

    @Override
    public AssetsConverter.ConvertProcess getConvertProcess() {
        return AssetsConverter.ConvertProcess.MOUSE_CURSORS;
    }

}
