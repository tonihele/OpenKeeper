/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert;

import com.jme3.asset.AssetManager;
import java.io.File;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import toniarts.opendungeonkeeper.tools.convert.enginetextures.EngineTexturesFile;

/**
 *
 * Converts all the assets from the original game to our game directory<br>
 * In formats supported by our engine
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class AssetsConverter {

    private static final String ASSETS_FOLDER = "assets";
    private static final String TEXTURES_FOLDER = "Textures";
    private static final Logger logger = Logger.getLogger(AssetsConverter.class.getName());

    private AssetsConverter() {
    }

    /**
     * Convert all the original DK II assets to our formats and copy to our
     * working folder
     *
     * @param dungeonKeeperFolder
     * @param assetManager
     */
    public static void convertAssets(String dungeonKeeperFolder, AssetManager assetManager) {
        String currentFolder = Paths.get("").toAbsolutePath().toString();
        logger.log(Level.INFO, "Starting asset convertion from DK II folder: " + dungeonKeeperFolder);
        logger.log(Level.INFO, "Current folder set to: " + currentFolder);

        //Create an assets folder
        if (!currentFolder.endsWith(File.separator)) {
            currentFolder = currentFolder.concat(File.separator);
        }
        currentFolder = currentFolder.concat(ASSETS_FOLDER).concat(File.separator);

        //First and foremost, we need the textures
        convertTextures(dungeonKeeperFolder, currentFolder.concat(TEXTURES_FOLDER).concat(File.separator));
    }

    /**
     * Extract and copy DK II textures
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination
     */
    private static void convertTextures(String dungeonKeeperFolder, String destination) {
        logger.log(Level.INFO, "Extracting textures to: " + destination);

        //Form the data path
        String dataDirectory = dungeonKeeperFolder.concat("DK2TextureCache").concat(File.separator);

        //Extract the textures
        EngineTexturesFile etFile = new EngineTexturesFile(new File(dataDirectory.concat("EngineTextures.dat")));
        Pattern pattern = Pattern.compile("(?<name>\\w+)MM(?<mipmaplevel>\\d{1})");
        for (String textureFile : etFile) {

            //All are PNG files, and MipMap levels are present, we need only the
            //highest quality one, so don't bother extracting the other mipmap levels
            Matcher matcher = pattern.matcher(textureFile);
            boolean found = matcher.find();
            if (found && Integer.parseInt(matcher.group("mipmaplevel")) == 0) {

                //Highest resolution, extract and rename
                File f = etFile.extractFileData(textureFile, destination);
                f.renameTo(new File(f.toString().replaceFirst("MM" + matcher.group("mipmaplevel"), "")));
            } else if (!found) {

                // No mipmap levels, just extract
                etFile.extractFileData(textureFile, destination);
            }
        }
    }
}
