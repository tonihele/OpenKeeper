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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

public class PathUtils {

    private static final Map<String, String> FILENAME_CACHE = new HashMap<>();
    private static final PathTree PATH_CACHE = new PathTree();
    private static final Object FILENAME_LOCK = new Object();
    protected static final String QUOTED_FILE_SEPARATOR = Matcher.quoteReplacement(File.separator);

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

    private static final Logger LOGGER = Logger.getLogger(PathUtils.class.getName());

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
            return fixFilePath(getCanonicalRelativePath(rootFolder, folder));
        }
        return fixFilePath(folder);
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
     * Converts all the file separators to current system separators
     *
     * @param fileName the file name to convert
     * @return the file name with native file separators
     */
    public static String convertFileSeparators(String fileName) {
        return fileName.replaceAll("[/\\\\]", QUOTED_FILE_SEPARATOR);
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
            LOGGER.log(Level.WARNING, "Can not locate path " + path + " from " + rootPath + "!", e);
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

        // Make sure that the uncertain path's separators are system separators
        uncertainPath = convertFileSeparators(uncertainPath);

        String fileName = realPath.concat(uncertainPath);
        String fileKey = fileName.toLowerCase();

        // See cache
        String cachedName = FILENAME_CACHE.get(fileKey);
        if (cachedName == null) {
            synchronized (FILENAME_LOCK) {

                // If it exists as such, that is super!
                Path testFile = Paths.get(fileName);
                if (Files.exists(testFile)) {
                    cachedName = testFile.toRealPath().toString();
                    FILENAME_CACHE.put(fileKey, cachedName);

                    return cachedName;
                }

                // Otherwise we need to do a recursive search
                String certainPath = PATH_CACHE.getCertainPath(fileName, realPath);
                final String[] path = fileName.substring(certainPath.length()).split(QUOTED_FILE_SEPARATOR);

                // If the path length is 1, lets try, maybe it was just the file name
                if (path.length == 1 && !certainPath.equalsIgnoreCase(realPath)) {
                    Path p = Paths.get(certainPath, path[0]);
                    if (Files.exists(p)) {
                        cachedName = p.toRealPath().toString();
                        FILENAME_CACHE.put(fileKey, cachedName);
                        return cachedName;
                    }
                }

                // Find the file
                final Path realPathAsPath = Paths.get(certainPath);
                FileFinder fileFinder = new FileFinder(realPathAsPath, path);
                Files.walkFileTree(realPathAsPath, fileFinder);
                FILENAME_CACHE.put(fileKey, fileFinder.file);
                cachedName = fileFinder.file;
                if (fileFinder.file == null) {
                    throw new IOException("File not found " + testFile + "!");
                }

                // Cache the known path
                PATH_CACHE.setPathToCache(fileFinder.file);
            }
        }

        return cachedName;
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
            LOGGER.log(Level.SEVERE, ex, () -> {
                return "Failed to delete file/folder " + file + "!";
            });
            return false;
        }
        return true;
    }

    /**
     * File finder, recursively tries to find a file ignoring case
     */
    private static class FileFinder extends SimpleFileVisitor<Path> {

        private int level = 0;
        private String file;
        private final Path startingPath;
        private final String[] path;

        private FileFinder(Path startingPath, String[] path) {
            this.startingPath = startingPath;
            this.path = path;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (startingPath.equals(dir)) {
                return FileVisitResult.CONTINUE; // Just the root
            } else if (startingPath.relativize(dir).getName(level).toString().equalsIgnoreCase(path[level])) {
                if (level < path.length - 1) {
                    level++;
                    return FileVisitResult.CONTINUE; // Go to dir
                } else {

                    // We are looking for a directory and we found it
                    this.file = dir.toRealPath().toString().concat(File.separator);
                    return FileVisitResult.TERMINATE;
                }
            }
            return FileVisitResult.SKIP_SUBTREE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

            // See if this is the file we are looking for
            if (level == path.length - 1 && file.getName(file.getNameCount() - 1).toString().equalsIgnoreCase(path[level])) {
                this.file = file.toRealPath().toString();
                return FileVisitResult.TERMINATE;
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return FileVisitResult.TERMINATE; // We already missed our window here
        }
    }

    /**
     * Represents a simple path tree cache, with unlimited number of roots.
     * Offers some methods to manage the tree.
     */
    private static class PathTree extends HashMap<String, PathNode> {

        /**
         * Add the path to cache from KNOWN file
         *
         * @param file the known and existing file
         */
        public void setPathToCache(String file) {
            List<String> paths = new ArrayList<>(Arrays.asList(file.split(QUOTED_FILE_SEPARATOR)));
            if (!paths.isEmpty()) {
                if (!file.endsWith(File.separator)) {
                    paths.remove(paths.size() - 1);
                }
                PathNode node = null;
                for (String folder : paths) {
                    node = getPath(folder, node, true);
                }
            }
        }

        private PathNode getPath(String folder, PathNode node, boolean add) {
            String key = folder.toLowerCase();
            Map<String, PathNode> leaf;
            if (node != null) {
                leaf = node.children;
            } else {
                leaf = this;
            }
            PathNode result = leaf.get(key);
            if (result == null && add) {
                result = new PathNode(folder, (node != null ? node.level + 1 : 0), node);
                leaf.put(key, result);
            }
            return result;
        }

        /**
         * Get certain path from cache
         *
         * @param fileName the file name we aim to find, if folder, we expect
         * path separator at the end
         * @param defaultPath the default path we know that exists, we'll return
         * it if no cached path found
         * @return the cached known path, quaranteed to be exactly the default
         * path or deeper
         */
        public String getCertainPath(String fileName, String defaultPath) {
            List<String> paths = new ArrayList<>(Arrays.asList(fileName.split(QUOTED_FILE_SEPARATOR)));
            if (!paths.isEmpty()) {
                if (!fileName.endsWith(File.separator)) {
                    paths.remove(paths.size() - 1);
                }
                PathNode node = null;
                for (String folder : paths) {
                    PathNode nextNode = getPath(folder, node, false);
                    if (nextNode != null) {
                        node = nextNode;
                    } else {
                        break;
                    }
                }

                // Return if we have longer path
                if (node != null && node.path.length() > defaultPath.length()) {
                    return node.path;
                }
            }
            return defaultPath;
        }

    }

    /**
     * Path node that represents a single folder
     */
    private static class PathNode {

        private final String path;
        private final String name;
        private final int level;
        private final PathNode parent;
        private final Map<String, PathNode> children = new HashMap<>();

        public PathNode(String name, int level, PathNode parent) {
            this.name = name;
            this.level = level;
            this.parent = parent;

            StringBuilder sb = new StringBuilder();
            if (parent != null) {
                sb.append(parent.path);
            }
            sb.append(name);
            sb.append(File.separator);
            path = sb.toString();
        }

        public String getName() {
            return name;
        }

        public int getLevel() {
            return level;
        }

        public PathNode getParent() {
            return parent;
        }

        public Map<String, PathNode> getChildren() {
            return children;
        }

        public String getPath() {
            return path;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 67 * hash + Objects.hashCode(this.name);
            hash = 67 * hash + this.level;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PathNode other = (PathNode) obj;
            if (this.level != other.level) {
                return false;
            }
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            return true;
        }

    }

}
