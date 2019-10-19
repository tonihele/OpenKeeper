/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.tools.convert;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import toniarts.openkeeper.cinematics.CameraSweepData;
import toniarts.openkeeper.cinematics.CameraSweepDataEntry;
import toniarts.openkeeper.cinematics.CameraSweepDataLoader;
import toniarts.openkeeper.game.data.HiScores;
import toniarts.openkeeper.tools.convert.bf4.Bf4File;
import toniarts.openkeeper.tools.convert.hiscores.HiScoresEntry;
import toniarts.openkeeper.tools.convert.hiscores.HiScoresFile;
import toniarts.openkeeper.tools.convert.kcs.KcsEntry;
import toniarts.openkeeper.tools.convert.kcs.KcsFile;
import toniarts.openkeeper.tools.convert.kmf.KmfFile;
import toniarts.openkeeper.tools.convert.map.GameLevel.LevFlag;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.sound.SdtFile;
import toniarts.openkeeper.tools.convert.spr.SprFile;
import toniarts.openkeeper.tools.convert.str.MbToUniFile;
import toniarts.openkeeper.tools.convert.str.StrFile;
import toniarts.openkeeper.tools.convert.textures.enginetextures.EngineTexturesFile;
import toniarts.openkeeper.tools.convert.textures.loadingscreens.LoadingScreenFile;
import toniarts.openkeeper.tools.convert.wad.WadFile;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.MapThumbnailGenerator;
import toniarts.openkeeper.utils.PathUtils;

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

        TEXTURES(6),
        MODELS(6),
        MOUSE_CURSORS(4),
        MUSIC_AND_SOUNDS(4),
        INTERFACE_TEXTS(3),
        PATHS(4),
        HI_SCORES(2),
        FONTS(3),
        MAP_THUMBNAILS(3);

        private ConvertProcess(int version) {
            this.version = version;
        }

        public int getVersion() {
            return this.version;
        }

        public String getSettingName() {
            String[] names = this.toString().toLowerCase().split(" ");
            String name = "";
            for (String item : names) {
                name += Character.toUpperCase(item.charAt(0)) + item.substring(1);
            }
            return name + "Version";
        }

        public void setOutdated(boolean outdated) {
            this.outdated = outdated;
        }

        public boolean isOutdated() {
            return this.outdated;
        }

        @Override
        public String toString() {
            return super.toString().replace('_', ' ');
        }
        private final int version;
        private boolean outdated = false;
    }
    private final String dungeonKeeperFolder;
    private final AssetManager assetManager;
    private static final boolean OVERWRITE_DATA = true; // Not exhausting your SDD :) or our custom graphics
    private static final String ASSETS_FOLDER = "assets" + File.separator + "Converted";
    private static final String ABSOLUTE_ASSETS_FOLDER = getCurrentFolder() + ASSETS_FOLDER + File.separator;

    public static final String SOUNDS_FOLDER = "Sounds";
    public static final String MATERIALS_FOLDER = "Materials";
    public static final String MODELS_FOLDER = "Models";
    public static final String TEXTURES_FOLDER = "Textures";
    public static final String SPRITES_FOLDER = "Sprites";
    public static final String MAP_THUMBNAILS_FOLDER = "Thumbnails";
    private static final String INTERFACE_FOLDER = "Interface" + File.separator;
    public static final String MOUSE_CURSORS_FOLDER = INTERFACE_FOLDER + "Cursors";
    public static final String FONTS_FOLDER = INTERFACE_FOLDER + "Fonts";
    public static final String TEXTS_FOLDER = INTERFACE_FOLDER + "Texts";
    public static final String PATHS_FOLDER = INTERFACE_FOLDER + "Paths";

    private static final Logger LOGGER = Logger.getLogger(AssetsConverter.class.getName());

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

    public static boolean conversionNeeded(AppSettings settings) {
        boolean needConversion = false;

        for (ConvertProcess item : ConvertProcess.values()) {
            String key = item.getSettingName();
            boolean isOutdated = item.getVersion() > settings.getInteger(key);
            item.setOutdated(isOutdated);
            if (isOutdated) {
                needConversion = true;
            }
        }

        return needConversion;
    }

    public static void setConversionSettings(AppSettings settings) {
        for (ConvertProcess item : ConvertProcess.values()) {
            settings.putInteger(item.getSettingName(), item.getVersion());
        }
    }

    /**
     * Convert all the original DK II assets to our formats and copy to our
     * working folder
     */
    public void convertAssets() {
        long start = System.currentTimeMillis();
        String currentFolder = getCurrentFolder();
        LOGGER.log(Level.INFO, "Starting asset convertion from DK II folder: {0}", dungeonKeeperFolder);
        LOGGER.log(Level.INFO, "Current folder set to: {0}", currentFolder);

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

        //The paths
        convertPaths(dungeonKeeperFolder, currentFolder.concat(PATHS_FOLDER).concat(File.separator));

        //HiScores
        convertHiScores(dungeonKeeperFolder);

        //The fonts
        convertFonts(dungeonKeeperFolder, currentFolder.concat(FONTS_FOLDER).concat(File.separator));

        //The map thumbnails
        generateMapThumbnails(dungeonKeeperFolder, currentFolder.concat(MAP_THUMBNAILS_FOLDER).concat(File.separator));

        // Log the time taken
        long duration = System.currentTimeMillis() - start;
        LOGGER.log(Level.INFO, "Conversion took {0} seconds!", TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS));
    }

    /**
     * Extract and copy DK II textures
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination Destination folder
     */
    private void convertTextures(String dungeonKeeperFolder, String destination) {
        if (!ConvertProcess.TEXTURES.isOutdated()) {
            return;
        }
        LOGGER.log(Level.INFO, "Extracting textures to: {0}", destination);
        updateStatus(null, null, ConvertProcess.TEXTURES);
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
                    LOGGER.log(Level.INFO, "File {0} already exists, skipping!", newFile);
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
        if (!ConvertProcess.MODELS.isOutdated()) {
            return;
        }
        LOGGER.log(Level.INFO, "Extracting models to: {0}", destination);
        updateStatus(null, null, ConvertProcess.MODELS);
        AssetUtils.deleteFolder(new File(destination));

        // Create the materials folder or else the material file saving fails
        File materialFolder = new File(getAssetsFolder().concat(AssetsConverter.MATERIALS_FOLDER));
        AssetUtils.deleteFolder(materialFolder);
        materialFolder.mkdirs();

        // Get the engine textures catalog
        EngineTexturesFile engineTexturesFile = getEngineTexturesFile(dungeonKeeperFolder);

        //Meshes are in the data folder, access the packed file
        WadFile wad = new WadFile(new File(dungeonKeeperFolder + PathUtils.DKII_DATA_FOLDER + "Meshes.WAD"));
        HashMap<String, KmfFile> kmfs = new HashMap<>();
        File tmpdir = new File(System.getProperty("java.io.tmpdir"));
        int i = 0;
        int total = wad.getWadFileEntryCount();
        for (final String entry : wad.getWadFileEntries()) {
            try {
                updateStatus(i, total, ConvertProcess.MODELS);

                // See if we already have this model
                if (!OVERWRITE_DATA && new File(destination.concat(entry.substring(0, entry.length() - 4)).concat(".j3o")).exists()) {
                    LOGGER.log(Level.INFO, "File {0} already exists, skipping!", entry);
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
                LOGGER.log(Level.SEVERE, "Failed to create a file for WAD entry " + entry + "!", ex);
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
        KmfAssetInfo ai = new KmfAssetInfo(assetManager, new AssetKey(entry.getKey()), entry.getValue(), true);
        KmfModelLoader kmfModelLoader = new KmfModelLoader();
        try {
            Node n = (Node) kmfModelLoader.load(ai);

            // Export
            BinaryExporter exporter = BinaryExporter.getInstance();
            File file = new File(destination.concat(entry.getKey().substring(0, entry.getKey().length() - 4)).concat(".j3o"));
            exporter.save(n, file);
        } catch (Exception ex) {
            String msg = "Failed to convert KMF entry " + entry.getKey() + "!";
            LOGGER.log(Level.SEVERE, msg, ex);
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
        if (!ConvertProcess.MOUSE_CURSORS.isOutdated()) {
            return;
        }
        LOGGER.log(Level.INFO, "Extracting mouse cursors to: {0}", destination);
        updateStatus(null, null, ConvertProcess.MOUSE_CURSORS);
        AssetUtils.deleteFolder(new File(destination));

        //Mouse cursors are PNG files in the Sprite.WAD
        WadFile wadFile = new WadFile(new File(dungeonKeeperFolder + PathUtils.DKII_DATA_FOLDER + "Sprite.WAD"));
        int i = 0;
        int total = wadFile.getWadFileEntryCount();
        File destinationFolder = new File(getAssetsFolder().concat(SPRITES_FOLDER).concat(File.separator));
        AssetUtils.deleteFolder(destinationFolder);
        destinationFolder.mkdirs();

        for (String fileName : wadFile.getWadFileEntries()) {
            updateStatus(i, total, ConvertProcess.MOUSE_CURSORS);
            i++;
            //Extract the file
            File extracted = wadFile.extractFileData(fileName, destination);

            if (fileName.toLowerCase().endsWith(".spr")) {
                // Extract the spr and delete it afterwards
                SprFile sprFile = new SprFile(extracted);
                try {
                    sprFile.extract(destinationFolder.getPath(), fileName.substring(0, fileName.length() - 4));
                    extracted.delete();
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error Sprite: {0}", ex);
                }
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
        if (!ConvertProcess.MUSIC_AND_SOUNDS.isOutdated()) {
            return;
        }
        LOGGER.log(Level.INFO, "Extracting sounds to: {0}", destination);
        updateStatus(null, null, ConvertProcess.MUSIC_AND_SOUNDS);
        AssetUtils.deleteFolder(new File(destination));
        String dataDirectory = PathUtils.DKII_SFX_FOLDER;

        //Find all the sound files
        final List<File> sdtFiles = new ArrayList<>();
        File dataDir = null;
        try {
            dataDir = new File(ConversionUtils.getRealFileName(dungeonKeeperFolder, dataDirectory));
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
            LOGGER.log(Level.SEVERE, msg, ex);
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
            String path = file.toString().substring(0, file.toString().length() - 4);
            Path relative = dataDir.toPath().relativize(new File(path).toPath());
            String dest = destination;
            dest += relative.toString();

            //Remove the actual file name
            //dest = dest.substring(0, dest.length() - file.toPath().getFileName().toString().length());
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
        return PathUtils.fixFilePath(currentFolder);
    }

    /**
     * Get the assets root folder
     *
     * @return the assets folder
     */
    public static String getAssetsFolder() {
        return ABSOLUTE_ASSETS_FOLDER;
    }

    /**
     * Loads up an instance of the engine textures catalog
     *
     * @param dungeonKeeperFolder DK II folder
     * @return EngineTextures catalog
     */
    public static EngineTexturesFile getEngineTexturesFile(String dungeonKeeperFolder) {

        //Get the engine textures file
        try {
            EngineTexturesFile etFile = new EngineTexturesFile(new File(ConversionUtils.getRealFileName(dungeonKeeperFolder, "DK2TextureCache".concat(File.separator).concat("EngineTextures.dat"))));
            return etFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to open the EngineTextures file!", e);
        }
    }

    /**
     * Extract and copy DK II interface texts
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination Destination folder
     */
    private void convertTexts(String dungeonKeeperFolder, String destination) {
        if (!ConvertProcess.INTERFACE_TEXTS.isOutdated()) {
            return;
        }
        LOGGER.log(Level.INFO, "Extracting texts to: {0}", destination);
        updateStatus(null, null, ConvertProcess.INTERFACE_TEXTS);
        AssetUtils.deleteFolder(new File(destination));
        String dataDirectory = dungeonKeeperFolder + PathUtils.DKII_TEXT_DEFAULT_FOLDER;

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
            LOGGER.log(Level.SEVERE, msg, ex);
            throw new RuntimeException(msg, ex);
        }

        //Convert the STR files to JAVA native resource bundles
        new File(destination).mkdirs(); // Ensure that the folder exists
        int i = 0;
        int total = srtFiles.size();
        MbToUniFile codePage = null;
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

            // Write the properties
            String fileName = file.getName();
            fileName = fileName.substring(0, fileName.length() - 3);
            File dictFile = new File(destination.concat(fileName).concat("properties"));
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(dictFile, false), "UTF-8"))) {
                for (Map.Entry<Integer, String> entry : strFile.getEntriesAsSet()) {
                    pw.println(entry.getKey() + "=" + entry.getValue());
                }
            } catch (IOException ex) {
                String msg = "Failed to save the dictionary file to " + dictFile + "!";
                LOGGER.log(Level.SEVERE, msg, ex);
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

    /**
     * Extract and copy DK II camera sweep files (paths)
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination Destination folder
     */
    private void convertPaths(String dungeonKeeperFolder, String destination) {
        if (!ConvertProcess.PATHS.isOutdated()) {
            return;
        }
        LOGGER.log(Level.INFO, "Extracting paths to: {0}", destination);
        updateStatus(null, null, ConvertProcess.PATHS);
        AssetUtils.deleteFolder(new File(destination));

        //Paths are in the data folder, access the packed file
        WadFile wad = new WadFile(new File(dungeonKeeperFolder + PathUtils.DKII_DATA_FOLDER + "Paths.WAD"));
        int i = 0;
        int total = wad.getWadFileEntryCount();
        File tmpdir = new File(System.getProperty("java.io.tmpdir"));
        BinaryExporter exporter = BinaryExporter.getInstance();
        for (final String entry : wad.getWadFileEntries()) {
            try {
                updateStatus(i, total, ConvertProcess.PATHS);
                i++;

                // Convert all the KCS entries
                if (entry.toLowerCase().endsWith(".kcs")) {

                    // Extract each file to temp
                    File f = wad.extractFileData(entry, tmpdir.toString());
                    f.deleteOnExit();

                    // Open the entry
                    KcsFile kcsFile = new KcsFile(f);

                    // Convert
                    List<CameraSweepDataEntry> entries = new ArrayList<>(kcsFile.getKcsEntries().size());
                    for (KcsEntry kcsEntry : kcsFile.getKcsEntries()) {

                        // Convert the rotation matrix to quatenion
                        Matrix3f mat = new Matrix3f();
                        Vector3f direction = ConversionUtils.convertVector(kcsEntry.getDirection());
                        Vector3f left = ConversionUtils.convertVector(kcsEntry.getLeft());
                        Vector3f up = ConversionUtils.convertVector(kcsEntry.getUp());
                        mat.setColumn(0, new Vector3f(-direction.x, direction.y, direction.z));
                        mat.setColumn(1, new Vector3f(left.x, -left.y, -left.z));
                        mat.setColumn(2, new Vector3f(-up.x, up.y, up.z));

                        entries.add(new CameraSweepDataEntry(ConversionUtils.convertVector(kcsEntry.getPosition()),
                                new Quaternion().fromRotationMatrix(mat), FastMath.RAD_TO_DEG * kcsEntry.getLens(),
                                kcsEntry.getNear()));
                    }
                    CameraSweepData cameraSweepData = new CameraSweepData(entries);

                    // Save it
                    exporter.save(cameraSweepData, new File(destination.concat(entry.substring(0, entry.length() - 3)).concat(CameraSweepDataLoader.FILE_EXTENSION)));
                } else if (entry.toLowerCase().endsWith(".txt")) {

                    // The text file is nice to have, it is an info text
                    wad.extractFileData(entry, destination);
                }

            } catch (Exception ex) {
                String msg = "Failed to save the path file to " + destination + "!";
                LOGGER.log(Level.SEVERE, msg, ex);
                throw new RuntimeException(msg, ex);
            }
        }
    }

    /**
     * Extract and copy DK II HiScores
     *
     * @param dungeonKeeperFolder DK II main folder
     */
    private void convertHiScores(String dungeonKeeperFolder) {
        if (!ConvertProcess.HI_SCORES.isOutdated()) {
            return;
        }
        LOGGER.log(Level.INFO, "Converting hiscores");
        updateStatus(0, 1, ConvertProcess.HI_SCORES);
        try {

            // Load the original
            File file = new File(dungeonKeeperFolder + "Data/Settings/HiScores.dat");
            HiScoresFile originalHiScores = new HiScoresFile(file);

            // Convert it!
            HiScores hiScores = new HiScores();
            for (HiScoresEntry entry : originalHiScores.getHiScoresEntries()) {
                hiScores.add(entry.getScore(), entry.getName(), entry.getLevel());
            }
            updateStatus(1, 1, ConvertProcess.HI_SCORES);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Can not convert HiScores!", ex);

            // By no means fatal :D
        }
    }

    /**
     * Extract and convert DK II font files (BF4)
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination Destination folder
     */
    private void convertFonts(final String dungeonKeeperFolder, final String destination) {
        if (!ConvertProcess.FONTS.isOutdated()) {
            return;
        }
        LOGGER.log(Level.INFO, "Extracting fonts to: {0}", destination);
        updateStatus(null, null, ConvertProcess.FONTS);
        AssetUtils.deleteFolder(new File(destination));

        try {

            // Make sure the folder exists
            new File(destination).mkdirs();

            // Find all the font files
            final List<File> bf4Files = new ArrayList<>();
            Files.walkFileTree(new File(dungeonKeeperFolder + PathUtils.DKII_TEXT_DEFAULT_FOLDER).toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                    //Get all the BF4 files
                    if (attrs.isRegularFile() && file.getFileName().toString().toLowerCase().endsWith(".bf4")) {
                        bf4Files.add(file.toFile());
                    }

                    //Always continue
                    return FileVisitResult.CONTINUE;
                }
            });

            // Go through the font files
            int i = 0;
            int total = bf4Files.size();
            Pattern pattern = Pattern.compile("FONT_(?<name>\\D+)(?<size>\\d+)", Pattern.CASE_INSENSITIVE);
            for (File file : bf4Files) {
                updateStatus(i, total, ConvertProcess.FONTS);

                // The file names
                final int fontSize;

                final String imageFileName;
                final String descriptionFileName;
                Matcher matcher = pattern.matcher(file.getName());
                boolean found = matcher.find();
                if (!found) {
                    LOGGER.log(Level.SEVERE, "Font name {0} not recognized!", file.getName());
                    throw new RuntimeException("Unknown font name!");
                } else {
                    fontSize = Integer.parseInt(matcher.group("size"));
                    String baseFileName = matcher.group("name");
                    baseFileName = destination.concat(Character.toUpperCase(baseFileName.charAt(0)) + baseFileName.substring(1).toLowerCase() + fontSize);
                    imageFileName = baseFileName.concat(".png");
                    descriptionFileName = baseFileName.concat(".fnt");
                }

                // Convert & save the font file
                FontCreator fc = new FontCreator(new Bf4File(file)) {
                    @Override
                    protected int getFontSize() {
                        return fontSize;
                    }

                    @Override
                    protected String getFileName() {
                        return imageFileName.substring(destination.length());
                    }
                };
                ImageIO.write(fc.getFontImage(), "png", new File(imageFileName));
                try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(descriptionFileName))) {
                    out.write(fc.getDescription());
                }

                i++;
            }

        } catch (Exception ex) {
            String msg = "Failed to save the font file to " + destination + "!";
            LOGGER.log(Level.SEVERE, msg, ex);
            throw new RuntimeException(msg, ex);
        }
    }

    /**
     * Generates thumbnails out of map files (only the skirmish/mp)
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination Destination folder
     */
    private void generateMapThumbnails(String dungeonKeeperFolder, String destination) {
        if (!ConvertProcess.MAP_THUMBNAILS.isOutdated()) {
            return;
        }
        LOGGER.log(Level.INFO, "Generating map thumbnails to: {0}", destination);
        updateStatus(null, null, ConvertProcess.MAP_THUMBNAILS);
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
                if (kwd.getGameLevel().getLvlFlags().contains(LevFlag.IS_SKIRMISH_LEVEL)
                        || kwd.getGameLevel().getLvlFlags().contains(LevFlag.IS_MULTIPLAYER_LEVEL)) {
                    maps.add(kwd);
                }
            }

            // Go through the map files
            int i = 0;
            int total = maps.size();
            for (KwdFile kwd : maps) {
                updateStatus(i, total, ConvertProcess.MAP_THUMBNAILS);
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
}
