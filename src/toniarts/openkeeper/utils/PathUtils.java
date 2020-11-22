/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import toniarts.openkeeper.tools.convert.ConversionUtils;

public class PathUtils {

    public static final String DKII_DATA_FOLDER = getRealDKIIRelativeFolder("Data" + File.separator);
    public static final String DKII_SFX_FOLDER = getRealDKIIRelativeFolder(DKII_DATA_FOLDER + "Sound" + File.separator
            + "sfx" + File.separator);
    public static final String DKII_MOVIES_FOLDER = getRealDKIIRelativeFolder(DKII_DATA_FOLDER + "Movies" + File.separator);
    public static final String DKII_TEXT_DEFAULT_FOLDER = getRealDKIIRelativeFolder(DKII_DATA_FOLDER + "Text" + File.separator
            + "Default" + File.separator);
    public static final String DKII_EDITOR_FOLDER = getRealDKIIRelativeFolder(DKII_DATA_FOLDER + "editor" + File.separator);
    public static final String DKII_MAPS_FOLDER = getRealDKIIRelativeFolder(DKII_EDITOR_FOLDER + "maps" + File.separator);
    public static final String DKII_SFX_GLOBAL_FOLDER = getRealDKIIRelativeFolder(DKII_SFX_FOLDER + "Global" + File.separator);

    private final static String DKII_FOLDER_KEY = "DungeonKeeperIIFolder";
    private final static String TEST_FILE = DKII_MAPS_FOLDER + "FrontEnd3DLevel.kwd";

    /**
     * Get the folder of the original Dungeon Keeper 2 installation
     *
     * @return Dungeon Keeper 2 folder
     */
    public static String getDKIIFolder() {
        return SettingUtils.getInstance().getSettings().getString(DKII_FOLDER_KEY);
    }

    /**
     * Set the folder of the dk2 installation in the settings
     *
     * @param dkIIFolder
     */
    public static void setDKIIFolder(String dkIIFolder) {
        SettingUtils.getInstance().getSettings().putString(DKII_FOLDER_KEY, dkIIFolder);
    }

    /**
     * Checks the DK 2 folder validity
     *
     * @param folder the supposed DK II folder
     * @return true if the folder is valid
     */
    public static boolean checkDkFolder(String folder) {

        // Throw a simple test to the folder, try to find a test file
        if (folder != null && !folder.isEmpty()) {
            return Files.exists(Paths.get(PathUtils.fixFilePath(folder).concat(TEST_FILE)));
        }

        // Better luck next time
        return false;
    }

    /**
     * Adds a file separator to the folder path if it doesn't end with one
     *
     * @param folderPath path to the folder
     * @return folder with file separator at the end
     */
    public static String fixFilePath(final String folderPath) {
        if (!folderPath.endsWith(File.separator)) {
            return folderPath.concat(File.separator);
        }
        return folderPath;
    }

    /**
     * Get the relative folder that has been fixed for case sensitivity
     *
     * @param folder the path to fix
     * @return fixed path relative to the DKII folder
     */
    public static String getRealDKIIRelativeFolder(final String folder) {
        String rootFolder = getDKIIFolder();
        if (rootFolder != null && !rootFolder.isEmpty()) {
            return fixFilePath(ConversionUtils.getCanonicalRelativePath(rootFolder, folder));
        }
        return fixFilePath(folder);
    }

    /**
     * Read input stream to bytes. Can be removed in Java 9 as the InputStream
     * provides this functionality then
     *
     * @param is the input stream to read
     * @return byte array read from the input stream
     * @throws IOException may fail
     */
    public static byte[] getBytesFromInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];
        for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
            os.write(buffer, 0, len);
        }
        return os.toByteArray();
    }

    /**
     * Creates a filter for getting files that end in the wanted suffix. This is
     * case insensitive comparison.
     *
     * @param suffix the file suffix to search for
     * @return filter to use when going through file system
     */
    public static DirectoryStream.Filter<Path> getFilterForFilesEndingWith(String suffix) {
        return new DirectoryStream.Filter<Path>() {

            @Override
            public boolean accept(Path entry) throws IOException {
                return entry.getFileName().toString().toLowerCase().endsWith(suffix) && !Files.isDirectory(entry);
            }
        };
    }

}
