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
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import toniarts.opendungeonkeeper.gui.Dictionary;
import toniarts.opendungeonkeeper.tools.convert.enginetextures.EngineTexturesFile;
import toniarts.opendungeonkeeper.tools.convert.kmf.KmfFile;
import toniarts.opendungeonkeeper.tools.convert.sound.SdtFile;
import toniarts.opendungeonkeeper.tools.convert.str.StrFile;
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
public abstract class AssetsConverter {

    /**
     * Processes are done in order
     *
     * @see ConvertProcess#getProcessNumber()
     */
    public enum ConvertProcess {

        TEXTURES(1),
        MODELS(2),
        MOUSE_CURSORS(3),
        MUSIC_AND_SOUNDS(4),
        INTERFACE_TEXTS(5);

        private ConvertProcess(int processNumber) {
            this.processNumber = processNumber;
        }

        public int getProcessNumber() {
            return processNumber;
        }

        @Override
        public String toString() {
            return super.toString().replace('_', ' ');
        }
        private final int processNumber;
    }
    private final String dungeonKeeperFolder;
    private final AssetManager assetManager;
    private static final String ASSETS_FOLDER = "assets";
    public static final String TEXTURES_FOLDER = "Textures";
    public static final String MODELS_FOLDER = "Models";
    public static final String MOUSE_CURSORS_FOLDER = "Interface".concat(File.separator).concat("Cursors");
    public static final String SOUNDS_FOLDER = "Sounds";
    public static final String MATERIALS_FOLDER = "Materials";
    public static final String TEXTS_FOLDER = "Interface".concat(File.separator).concat("Texts");
    private static final boolean OVERWRITE_DATA = true; // Not exhausting your SDD :) or our custom graphics
    private static final Logger logger = Logger.getLogger(AssetsConverter.class.getName());

    public AssetsConverter(String dungeonKeeperFolder, AssetManager assetManager) {
        this.dungeonKeeperFolder = dungeonKeeperFolder;
        this.assetManager = assetManager;
    }

    /**
     * Callback for updates
     *
     * @param currentProgress current progress, maybe null if not certain yet
     * @param totalProgress total progress, maybe null if not certain yet
     * @param process the process we are currently doing
     */
    protected abstract void updateStatus(Integer currentProgress, Integer totalProgress, ConvertProcess process);

    /**
     * Convert all the original DK II assets to our formats and copy to our
     * working folder
     */
    public void convertAssets() {
        Date start = new Date();
        String currentFolder = getCurrentFolder();
        logger.log(Level.INFO, "Starting asset convertion from DK II folder: {0}", dungeonKeeperFolder);
        logger.log(Level.INFO, "Current folder set to: {0}", currentFolder);

        //Create an assets folder
        currentFolder = currentFolder.concat(ASSETS_FOLDER).concat(File.separator);

        //TODO: We need to search the normal assets before extracting do we actually already
        //have a user made asset there

        //First and foremost, we need the textures
        convertTextures(dungeonKeeperFolder, currentFolder.concat(TEXTURES_FOLDER).concat(File.separator));

        //And the models, note that these already need to find the textures (our custom resource locator)
        //In development this works without such
        convertModels(dungeonKeeperFolder, currentFolder.concat(MODELS_FOLDER).concat(File.separator), assetManager);

        //The mouse cursors
        convertMouseCursors(dungeonKeeperFolder, currentFolder.concat(MOUSE_CURSORS_FOLDER).concat(File.separator));

        //The sound and music
        convertSounds(dungeonKeeperFolder, currentFolder.concat(SOUNDS_FOLDER).concat(File.separator));

        //The texts
        convertTexts(dungeonKeeperFolder, currentFolder.concat(TEXTS_FOLDER).concat(File.separator));

        // Log the time taken
        long duration = new Date().getTime() - start.getTime();
        logger.log(Level.INFO, "Conversion took {0} seconds!", TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS));
    }

    /**
     * Extract and copy DK II textures
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination Destination folder
     */
    private void convertTextures(String dungeonKeeperFolder, String destination) {
        logger.log(Level.INFO, "Extracting textures to: {0}", destination);
        updateStatus(null, null, ConvertProcess.TEXTURES);
        EngineTexturesFile etFile = getEngineTexturesFile(dungeonKeeperFolder);
        Pattern pattern = Pattern.compile("(?<name>\\w+)MM(?<mipmaplevel>\\d{1})");
        WadFile frontEnd = new WadFile(new File(dungeonKeeperFolder.concat("data").concat(File.separator).concat("FrontEnd.WAD")));
        WadFile engineTextures = new WadFile(new File(dungeonKeeperFolder.concat("data").concat(File.separator).concat("EngineTextures.WAD")));
        int i = 0;
        int total = etFile.getFileCount() + frontEnd.getWadFileEntries().size() + engineTextures.getWadFileEntries().size();

        for (String textureFile : etFile) {
            updateStatus(i, total, ConvertProcess.TEXTURES);
            i++;

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
                    logger.log(Level.INFO, "File {0} already exists, skipping!", newFile);
                    f.delete();
                    continue;
                }
                f.renameTo(newFile);
            } else if (!found) {

                // No mipmap levels, just extract
                etFile.extractFileData(textureFile, destination, OVERWRITE_DATA);
            }
        }

        extractTextureContainer(i, total, frontEnd, destination);
        extractTextureContainer(i, total, engineTextures, destination);
    }

    /**
     * Extract and copy DK II models
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination Destination folder
     */
    private void convertModels(String dungeonKeeperFolder, String destination, AssetManager assetManager) {
        logger.log(Level.INFO, "Extracting models to: {0}", destination);
        updateStatus(null, null, ConvertProcess.MODELS);

        // Get the engine textures catalog
        EngineTexturesFile engineTexturesFile = getEngineTexturesFile(dungeonKeeperFolder);

        //Meshes are in the data folder, access the packed file
        WadFile wad = new WadFile(new File(dungeonKeeperFolder.concat("data").concat(File.separator).concat("Meshes.WAD")));
        HashMap<String, KmfFile> kmfs = new HashMap<>();
        File tmpdir = new File(System.getProperty("java.io.tmpdir"));
        int i = 0;
        int total = wad.getWadFileEntryCount();
        for (final String entry : wad.getWadFileEntries()) {
            try {
                updateStatus(i, total, ConvertProcess.MODELS);

                // See if we already have this model
                if (!OVERWRITE_DATA && new File(destination.concat(entry.substring(0, entry.length() - 4)).concat(".j3o")).exists()) {
                    logger.log(Level.INFO, "File {0} already exists, skipping!", entry);
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
                    convertModel(assetManager, new Entry<String, KmfFile>() {
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
                    }, destination, engineTexturesFile);

                    // We can delete the file straight
                    f.delete();
                    i++;
                } else {

                    // For later processing
                    kmfs.put(entry, kmfFile);
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Failed to create a file for WAD entry " + entry + "!", ex);
                throw ex;
            }
        }

        // And the groups (now they can be linked)
        for (Entry<String, KmfFile> entry : kmfs.entrySet()) {
            updateStatus(i, total, ConvertProcess.MODELS);
            convertModel(assetManager, entry, destination, engineTexturesFile);
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
    private void convertModel(AssetManager assetManager, Entry<String, KmfFile> entry, String destination, EngineTexturesFile engineTexturesFile) throws RuntimeException {

        //Remove the file extension from the file
        KmfAssetInfo ai = new KmfAssetInfo(assetManager, new AssetKey(entry.getKey()), entry.getValue(), engineTexturesFile, true);
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

    /**
     * Extract and copy DK II mouse cursors
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination Destination folder
     */
    private void convertMouseCursors(String dungeonKeeperFolder, String destination) {
        logger.log(Level.INFO, "Extracting mouse cursors to: {0}", destination);
        updateStatus(null, null, ConvertProcess.MOUSE_CURSORS);

        //Mouse cursors are PNG files in the Sprite.WAD
        WadFile wadFile = new WadFile(new File(dungeonKeeperFolder.concat("data").concat(File.separator).concat("Sprite.WAD")));
        int i = 0;
        int total = wadFile.getWadFileEntryCount();
        for (String fileName : wadFile.getWadFileEntries()) {
            updateStatus(i, total, ConvertProcess.MOUSE_CURSORS);
            i++;
            if (fileName.toLowerCase().endsWith(".png")) {

                //Extract the file
                wadFile.extractFileData(fileName, destination);
            }
        }
    }

    /**
     * Extract and copy DK II sounds & music
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination Destination folder
     */
    private void convertSounds(String dungeonKeeperFolder, String destination) {
        logger.log(Level.INFO, "Extracting sounds to: {0}", destination);
        updateStatus(null, null, ConvertProcess.MUSIC_AND_SOUNDS);
        String dataDirectory = dungeonKeeperFolder.concat("data").concat(File.separator).concat("sound").concat(File.separator).concat("sfx").concat(File.separator);

        //Find all the sound files
        final List<File> sdtFiles = new ArrayList<>();
        File dataDir = new File(dataDirectory);
        try {
            Files.walkFileTree(dataDir.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                    //Get all the SDT files
                    if (attrs.isRegularFile() && file.getFileName().toString().toLowerCase().endsWith(".sdt")) {
                        sdtFiles.add(file.toFile());
                    }

                    //Always continue
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            String msg = "Failed to scan sounds folder " + dataDirectory + "!";
            logger.log(Level.SEVERE, msg, ex);
            throw new RuntimeException(msg, ex);
        }

        //Extract the sounds
        // FIXME: We should try to figure out the map files, but at least merge the sound track files
        int i = 0;
        int total = sdtFiles.size();
        for (File file : sdtFiles) {
            updateStatus(i, total, ConvertProcess.MUSIC_AND_SOUNDS);
            i++;

            SdtFile sdt = new SdtFile(file);

            //Get a relative path
            Path relative = dataDir.toPath().relativize(file.toPath());
            String dest = destination;
            dest += relative.toString();

            //Remove the actual file name
            dest = dest.substring(0, dest.length() - file.toPath().getFileName().toString().length());

            //Extract
            sdt.extractFileData(dest);
        }
    }

    /**
     * Get the current folder
     *
     * @return the current folder
     */
    public static String getCurrentFolder() {
        String currentFolder = Paths.get("").toAbsolutePath().toString();
        if (!currentFolder.endsWith(File.separator)) {
            currentFolder = currentFolder.concat(File.separator);
        }
        return currentFolder;
    }

    /**
     * Get the assets root folder
     *
     * @return the assets folder
     */
    public static String getAssetsFolder() {
        return getCurrentFolder().concat(ASSETS_FOLDER).concat(File.separator);
    }

    /**
     * Loads up an instance of the engine textures catalog
     *
     * @param dungeonKeeperFolder DK II folder
     * @return EngineTextures catalog
     */
    public static EngineTexturesFile getEngineTexturesFile(String dungeonKeeperFolder) {

        //Form the data path
        String dataDirectory = dungeonKeeperFolder.concat("DK2TextureCache").concat(File.separator);

        //Extract the textures
        EngineTexturesFile etFile = new EngineTexturesFile(new File(dataDirectory.concat("EngineTextures.dat")));
        return etFile;
    }

    /**
     * Extract and copy DK II interface texts
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination Destination folder
     */
    private void convertTexts(String dungeonKeeperFolder, String destination) {
        logger.log(Level.INFO, "Extracting texts to: {0}", destination);
        updateStatus(null, null, ConvertProcess.INTERFACE_TEXTS);
        String dataDirectory = dungeonKeeperFolder.concat("data").concat(File.separator).concat("text").concat(File.separator).concat("default").concat(File.separator);

        //Find all the STR files
        final List<File> srtFiles = new ArrayList<>();
        File dataDir = new File(dataDirectory);
        try {
            Files.walkFileTree(dataDir.toPath(), EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                    //Get all the STR files
                    if (attrs.isRegularFile() && file.getFileName().toString().toLowerCase().endsWith(".str")) {
                        srtFiles.add(file.toFile());
                    }

                    //Always continue
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            String msg = "Failed to scan texts folder " + dataDirectory + "!";
            logger.log(Level.SEVERE, msg, ex);
            throw new RuntimeException(msg, ex);
        }

        //Convert the STR files to our simple dictionaries
        new File(destination).mkdirs(); // Ensure that the folder exists
        int i = 0;
        int total = srtFiles.size();
        CharBuffer codePage = null;
        for (File file : srtFiles) {
            updateStatus(i, total, ConvertProcess.INTERFACE_TEXTS);
            i++;

            // The code page cache makes processing faster
            StrFile strFile;
            if (codePage == null) {
                strFile = new StrFile(file);
                codePage = strFile.getCodePage();
            } else {
                strFile = new StrFile(codePage, file);
            }

            // Create dictionary and save
            Dictionary dict = new Dictionary(strFile.getEntries());
            String fileName = file.getName();
            fileName = fileName.substring(0, fileName.length() - 3);
            File dictFile = new File(destination.concat(fileName).concat("dict"));
            try {
                dict.save(dictFile);
            } catch (IOException ex) {
                String msg = "Failed to save the dictionary file to " + dictFile + "!";
                logger.log(Level.SEVERE, msg, ex);
                throw new RuntimeException(msg, ex);
            }
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
    private void extractTextureContainer(int i, int total, WadFile wad, String destination) {
        for (final String entry : wad.getWadFileEntries()) {
            updateStatus(i, total, ConvertProcess.TEXTURES);
            i++;

            wad.extractFileData(entry, destination);
        }
    }
}
