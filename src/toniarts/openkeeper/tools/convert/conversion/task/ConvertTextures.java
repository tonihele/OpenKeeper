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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import static toniarts.openkeeper.tools.convert.AssetsConverter.getEngineTexturesFile;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.textures.enginetextures.EngineTexturesFile;
import toniarts.openkeeper.tools.convert.textures.loadingscreens.LoadingScreenFile;
import toniarts.openkeeper.tools.convert.wad.WadFile;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.PathUtils;

/**
 * Dungeon Keeper II textures conversion. Converts textures to PNG.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ConvertTextures extends ConversionTask {

    private static final Logger LOGGER = Logger.getLogger(ConvertTextures.class.getName());

    public ConvertTextures(String dungeonKeeperFolder, String destination, boolean overwriteData) {
        super(dungeonKeeperFolder, destination, overwriteData);
    }

    @Override
    public void internalExecuteTask() {
        convertTextures(dungeonKeeperFolder, destination);
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
        AssetUtils.deleteFolder(new File(destination));
        EngineTexturesFile etFile = getEngineTexturesFile(dungeonKeeperFolder);
        Pattern pattern = Pattern.compile("(?<name>\\w+)MM(?<mipmaplevel>\\d{1})");
        WadFile frontEnd;
        WadFile engineTextures;
        try {
            frontEnd = new WadFile(new File(ConversionUtils.getRealFileName(dungeonKeeperFolder, PathUtils.DKII_DATA_FOLDER + "FrontEnd.WAD")));
            engineTextures = new WadFile(new File(ConversionUtils.getRealFileName(dungeonKeeperFolder, PathUtils.DKII_DATA_FOLDER + "EngineTextures.WAD")));
        } catch (IOException e) {
            throw new RuntimeException("Failed to open a WAD file!", e);
        }

        int i = 0;
        int total = etFile.getFileCount() + frontEnd.getWadFileEntries().size() + engineTextures.getWadFileEntries().size();

        for (String textureFile : etFile) {
            updateStatus(i, total);
            i++;

            // All are PNG files, and MipMap levels are present, we need only the
            // highest quality one, so don't bother extracting the other mipmap levels
            Matcher matcher = pattern.matcher(textureFile);
            boolean found = matcher.find();
            if (found && Integer.parseInt(matcher.group("mipmaplevel")) == 0) {

                // Highest resolution, extract and rename
                File f = etFile.extractFileData(textureFile, destination, overwriteData);
                File newFile = new File(f.toString().replaceFirst("MM" + matcher.group("mipmaplevel"), ""));
                if (overwriteData && newFile.exists()) {
                    newFile.delete();
                } else if (!overwriteData && newFile.exists()) {

                    // Delete the extracted file
                    LOGGER.log(Level.INFO, "File {0} already exists, skipping!", newFile);
                    f.delete();
                    continue;
                }
                f.renameTo(newFile);
            } else if (!found) {

                // No mipmap levels, just extract
                etFile.extractFileData(textureFile, destination, overwriteData);
            }
        }

        extractTextureContainer(i, total, frontEnd, destination);
        extractTextureContainer(i, total, engineTextures, destination);
    }

    /**
     * Extracts the wad files and updates the progress bar
     *
     * @param i current entry number
     * @param total total entry number
     * @param wad wad file
     * @param destination destination directory
     */
    private void extractTextureContainer(int i, int total, WadFile wad, String destination) {
        for (final String entry : wad.getWadFileEntries()) {
            updateStatus(i, total);
            i++;

            // Some of these archives contain .444 files, convert these to PNGs
            if (entry.endsWith(".444")) {
                LoadingScreenFile lsf = new LoadingScreenFile(wad.getFileData(entry));
                try {
                    File destFile = new File(destination + entry);
                    String destFilename = destFile.getCanonicalPath();
                    destFile.getParentFile().mkdirs();
                    ImageIO.write(lsf.getImage(), "png", new File(destFilename.substring(0, destFilename.length() - 3).concat("png")));
                } catch (IOException ex) {
                    throw new RuntimeException("Failed to save the wad entry " + entry + "!", ex);
                }
            } else {
                wad.extractFileData(entry, destination);
            }
        }
    }

    @Override
    public AssetsConverter.ConvertProcess getConvertProcess() {
        return AssetsConverter.ConvertProcess.TEXTURES;
    }

}
