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

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class PathUtils {

    private static final Logger logger = System.getLogger(PathUtils.class.getName());

    /**
     * Cache for fully resolved file paths: lowercase key -> exact-case real path.
     * ConcurrentHashMap eliminates the data race from the old double-checked
     * locking on a plain HashMap.
     */
    private static final ConcurrentHashMap<String, String> FILENAME_CACHE = new ConcurrentHashMap<>();

    /**
     * Cache for known directory paths: lowercase dir path -> exact-case dir path
     * (always ends with '/'). Replaces the old PathTree/PathNode custom trie.
     */
    private static final ConcurrentHashMap<String, String> PATH_CACHE = new ConcurrentHashMap<>();

    public static final String DKII_DATA_FOLDER         = "Data/";
    public static final String DKII_EDITOR_FOLDER       = "Data/Editor/";
    public static final String DKII_MAPS_FOLDER         = "Data/Editor/Maps/";
    public static final String DKII_MOVIES_FOLDER       = "Data/Movies/";
    public static final String DKII_SFX_FOLDER          = "Data/Sound/Sfx/";
    public static final String DKII_SFX_GLOBAL_FOLDER   = "Data/Sound/Sfx/Global/";
    public static final String DKII_TEXT_DEFAULT_FOLDER = "Data/Text/Default/";

    private static final String DKII_FOLDER_KEY = "DungeonKeeperIIFolder";
    private static final String TEST_FILE = DKII_MAPS_FOLDER + "FrontEnd3DLevel.kwd";

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
        if (!folderPath.endsWith("/")) {
            return folderPath + '/';
        }
        return folderPath;
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

    public static byte[] readInputStream(InputStream inputStream) throws IOException {
        try (InputStream is = inputStream) {
            return is.readAllBytes();
        }
    }

    /**
     * Converts all the file separators to forward slashes
     *
     * @param fileName the file name to convert
     * @return the file name with forward slashes
     */
    public static String convertFileSeparators(String fileName) {
        return fileName.replace('\\', '/');
    }

    /**
     * Strip file name clean from any illegal characters, replaces the illegal
     * characters with an underscore
     *
     * @param fileName the file name to be stripped
     * @return returns stripped down file name
     */
    public static String stripFileName(String fileName) {
        return fileName.replaceAll("[[^a-zA-Z0-9][\\.]]", "_");
    }

    /**
     * Returns case sensitive and valid relative path
     *
     * @param rootPath the working start path, used to relativize the path
     * @param path the unknown path to fix
     * @return fully qualified and working relative path
     */
    public static String getCanonicalRelativePath(String rootPath, String path) {
        try {
            return getRealFileName(rootPath, path).substring(rootPath.length());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Can not locate path " + path + " from " + rootPath + "!", e);
            return path;
        }
    }

    /**
     * Gets real file name for a file, this is to ignore file system case
     * sensitivity<br>
     * Does a recursive search
     *
     * @param realPath the real path that surely exists (<strong>case
     * sensitive!!</strong>), serves as a root for the searching
     * @param uncertainPath the file (and/or directory) to find from the real
     * path
     * @return the case sensitive fully working file name
     * @throws IOException if file is not found
     */
    public static String getRealFileName(final String realPath, String uncertainPath) throws IOException {

        // Make sure that the uncertain path's separators are forward slashes
        uncertainPath = convertFileSeparators(uncertainPath);

        String fileName = realPath.concat(uncertainPath);
        String fileKey = fileName.toLowerCase();

        // Fast path: ConcurrentHashMap is safe for unsynchronized reads
        String cachedName = FILENAME_CACHE.get(fileKey);
        if (cachedName != null) {
            return cachedName;
        }

        // Compute the real path (two concurrent threads may both compute, but
        // putIfAbsent ensures only the first result is stored, and the computation
        // is idempotent filesystem work).
        cachedName = resolveFileName(fileName, realPath);
        if (cachedName == null)
            throw new IOException("File not found " + Paths.get(fileName) + "!");

        // Store in caches, using the winner if another thread beat us
        String existing = FILENAME_CACHE.putIfAbsent(fileKey, cachedName);
        String resolved = (existing != null) ? existing : cachedName;

        // Cache all parent directories for future lookups
        cacheDirectoryPaths(resolved);

        return resolved;
    }

    /**
     * Resolve a file name case-insensitively. Returns null if not found.
     */
    private static String resolveFileName(String fileName, String realPath) throws IOException {
        // Try exact match first
        Path testFile = Paths.get(fileName);
        if (Files.exists(testFile))
            return testFile.toRealPath().toString();

        // Find the longest known directory path from the cache
        String dirPart = getDirectoryPart(fileName);
        String certainPath = getCertainPath(dirPart, realPath);

        // If only a single filename segment, try a direct lookup from certainPath
        String uncertainSuffix = fileName.substring(certainPath.length());
        if (!uncertainSuffix.startsWith("/"))
            uncertainSuffix = '/' + uncertainSuffix;
        String[] segments = uncertainSuffix.split("/");
        List<String> nonEmpty = new ArrayList<>();
        for (String s : segments)
            if (!s.isEmpty())
                nonEmpty.add(s);

        if (nonEmpty.isEmpty())
            return null;

        // Try one-segment shortcut: look it up directly from certainPath
        if (nonEmpty.size() == 1 && !certainPath.equalsIgnoreCase(realPath)) {
            Path p = Paths.get(certainPath, nonEmpty.get(0));
            if (Files.exists(p))
                return p.toRealPath().toString();
        }

        // Walk the path segments, resolving each case-insensitively
        return resolveCaseInsensitive(Paths.get(certainPath), nonEmpty.toArray(new String[0]));
    }

    /**
     * Given a full file path, extract the directory part (everything before the
     * last '/'). If the path ends with '/', it's already a directory path.
     */
    private static String getDirectoryPart(String fileName) {
        if (fileName.endsWith("/"))
            return fileName;

        int lastSlash = fileName.lastIndexOf('/');
        return lastSlash >= 0 ? fileName.substring(0, lastSlash + 1) : "";
    }

    /**
     * Find the longest cached known directory path. If nothing cached, returns
     * defaultPath. Walks up the directory tree checking PATH_CACHE at each level.
     */
    private static String getCertainPath(String dirPart, String defaultPath) {
        String current = dirPart;
        while (current.length() > defaultPath.length()) {
            String cached = PATH_CACHE.get(current.toLowerCase());
            if (cached != null)
                return cached;

            // Strip the last segment and trailing slash
            int lastSlash = current.lastIndexOf('/');
            if (lastSlash <= 0)
                break;

            current = current.substring(0, lastSlash); // e.g., "a/b/c/" -> "a/b"
            lastSlash = current.lastIndexOf('/');
            current = lastSlash >= 0 ? current.substring(0, lastSlash + 1) : current + '/';
        }
        return defaultPath;
    }

    /**
     * Cache all parent directory paths from a resolved file/directory path.
     * For "a/b/c/file.txt", caches "a/", "a/b/", "a/b/c/".
     * For "a/b/c/", caches "a/", "a/b/", "a/b/c/".
     */
    private static void cacheDirectoryPaths(String resolvedPath) {
        String[] parts = resolvedPath.split("/");
        int end = resolvedPath.endsWith("/") ? parts.length : parts.length - 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < end; i++) {
            if (parts[i].isEmpty())
                continue; // skip leading empty segment from absolute paths

            sb.append(parts[i]).append('/');
            String dirPath = sb.toString();
            PATH_CACHE.putIfAbsent(dirPath.toLowerCase(), dirPath);
        }
    }

    /**
     * Walk from basePath through each segment, resolving each case-insensitively
     * via directory listing. Returns the toRealPath() result, or null if any
     * segment cannot be found.
     */
    private static String resolveCaseInsensitive(Path basePath, String[] segments) throws IOException {
        Path current = basePath;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            boolean isLast = (i == segments.length - 1);

            Path found = null;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(current)) {
                for (Path entry : stream) {
                    if (!entry.getFileName().toString().equalsIgnoreCase(segment)) {
                        continue;
                    }
                    if (isLast) {
                        // Last segment: accept file or directory
                        found = entry;
                        break;
                    } else if (Files.isDirectory(entry)) {
                        // Intermediate segment: must be a directory
                        found = entry;
                        break;
                    }
                }
            }

            if (found == null)
                return null;
            current = found;
        }

        // Preserve trailing '/' for directory results (matches original FileFinder behavior)
        String result = current.toRealPath().toString();
        if (Files.isDirectory(current)) {
            result = result + '/';
        }
        return result;
    }

    /**
     * Deletes a file or a folder
     *
     * @param file
     * @return true if the file or folder was deleted
     */
    public static boolean deleteFolder(final Path file) {
        if (file == null) {
            return false;
        }
        if (!Files.exists(file)) {
            return false;
        }
        try {
            if (Files.isRegularFile(file)) {
                Files.delete(file);
                return true;
            }
            Files.walkFileTree(file, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            logger.log(Level.ERROR, () -> {
                return "Failed to delete file/folder " + file + "!";
            }, ex);
            return false;
        }
        return true;
    }

}
