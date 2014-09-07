/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.scene.Node;
import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import toniarts.opendungeonkeeper.tools.convert.enginetextures.EngineTexturesFile;
import toniarts.opendungeonkeeper.tools.convert.kmf.KmfFile;
import toniarts.opendungeonkeeper.tools.convert.wad.WadFile;

/**
 *
 * Converts all the assets from the original game to our game directory<br>
 * In formats supported by our engine<br>
 * Since we own no copyright to these and cannot distribute these, these wont go
 * into our JAR files. We need an custom resource locator for JME
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class AssetsConverter {

    private static final String ASSETS_FOLDER = "assets";
    public static final String TEXTURES_FOLDER = "Textures";
    private static final String MODELS_FOLDER = "Models";
    private static final boolean OVERWRITE_DATA = false; // Not exhausting your SDD :) or our custom graphics
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

        //TODO: We need to search the normal assets before extracting do we actually already
        //have a user made asset there

        //First and foremost, we need the textures
        convertTextures(dungeonKeeperFolder, currentFolder.concat(TEXTURES_FOLDER).concat(File.separator));

        //And the models, note that these already need to find the textures (our custom resource locator)
        //In development this works without such
        convertModels(dungeonKeeperFolder, currentFolder.concat(MODELS_FOLDER).concat(File.separator), assetManager);
    }

    /**
     * Extract and copy DK II textures
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination Destination folder
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
                File f = etFile.extractFileData(textureFile, destination, OVERWRITE_DATA);
                File newFile = new File(f.toString().replaceFirst("MM" + matcher.group("mipmaplevel"), ""));
                if (OVERWRITE_DATA && newFile.exists()) {
                    newFile.delete();
                } else if (!OVERWRITE_DATA && newFile.exists()) {

                    // Delete the extracted file
                    logger.log(Level.INFO, "File " + newFile + " already exists, skipping!");
                    f.delete();
                    continue;
                }
                f.renameTo(newFile);
            } else if (!found) {

                // No mipmap levels, just extract
                etFile.extractFileData(textureFile, destination, OVERWRITE_DATA);
            }
        }
    }

    /**
     * Extract and copy DK II models
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination Destination folder
     */
    private static void convertModels(String dungeonKeeperFolder, String destination, AssetManager assetManager) {
        logger.log(Level.INFO, "Extracting models to: " + destination);

        //Meshes are in the data folder, access the packed file
        WadFile wad = new WadFile(new File(dungeonKeeperFolder.concat("data").concat(File.separator).concat("Meshes.WAD")));
        HashMap<String, KmfFile> kmfs = new HashMap<>(wad.getWadFileEntryCount());
        File tmpdir = new File(System.getProperty("java.io.tmpdir"));
        for (String entry : wad.getWadFileEntries()) {
            try {

                // See if we already have this model
                if (!OVERWRITE_DATA && new File(destination.concat(entry.substring(0, entry.length() - 4)).concat(".j3o")).exists()) {
                    logger.log(Level.INFO, "File " + entry + " already exists, skipping!");
                }

                // Extract each file to temp
                File f = wad.extractFileData(entry, tmpdir.toString());
                f.deleteOnExit();

                // Parse
                kmfs.put(entry, new KmfFile(f));
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Failed to create a file for WAD entry " + entry + "!", ex);
                throw ex;
            }
        }

        // First, convert the regular models
        for (Entry<String, KmfFile> entry : kmfs.entrySet()) {
            if (entry.getValue().getType() == KmfFile.Type.MESH) {

                //Remove the file extension from the file
                KmfAssetInfo ai = new KmfAssetInfo(assetManager, new AssetKey(entry.getKey()), entry.getValue());
                KmfModelLoader kmfModelLoader = new KmfModelLoader();
                try {
                    Node n = (Node) kmfModelLoader.load(ai);

                    //Export
                    BinaryExporter exporter = BinaryExporter.getInstance();
                    File file = new File(destination.concat(entry.getKey().substring(0, entry.getKey().length() - 4)).concat(".j3o"));
                    exporter.save(n, file);
                } catch (Exception ex) {
                    String msg = "Failed to convert KMF entry " + entry.getKey() + "!";
                    logger.log(Level.SEVERE, msg, ex);
                    throw new RuntimeException(msg, ex);
                }
            }
        }
    }
}
