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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.str.MbToUniFile;
import toniarts.openkeeper.tools.convert.str.StrFile;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.PathUtils;

/**
 * Dungeon Keeper II texts conversion. Converts all interface texts to plain
 * Java resource bundles.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ConvertTexts extends ConversionTask {

    private static final Logger LOGGER = Logger.getLogger(ConvertTexts.class.getName());

    public ConvertTexts(String dungeonKeeperFolder, String destination, boolean overwriteData) {
        super(dungeonKeeperFolder, destination, overwriteData);
    }

    @Override
    public void internalExecuteTask() {
        convertTexts(dungeonKeeperFolder, destination);
    }

    /**
     * Extract and copy DK II interface texts
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination Destination folder
     */
    private void convertTexts(String dungeonKeeperFolder, String destination) {
        LOGGER.log(Level.INFO, "Extracting texts to: {0}", destination);
        updateStatus(null, null);
        Path destinationFolder = Paths.get(destination);
        AssetUtils.deleteFolder(destinationFolder);
        Path dataDirectory = Paths.get(dungeonKeeperFolder, PathUtils.DKII_TEXT_DEFAULT_FOLDER);

        // Find all the STR files
        final List<Path> srtFiles = new ArrayList<>();
        try {
            Files.walkFileTree(dataDirectory, EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                    // Get all the STR files
                    if (file.getFileName().toString().toLowerCase().endsWith(".str") && attrs.isRegularFile()) {
                        srtFiles.add(file);
                    }

                    // Always continue
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            String msg = "Failed to scan texts folder " + dataDirectory + "!";
            LOGGER.log(Level.SEVERE, msg, ex);
            throw new RuntimeException(msg, ex);
        }

        // Convert the STR files to JAVA native resource bundles
        try {
            Files.createDirectories(destinationFolder);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to create destination folder " + destinationFolder + "!", ex);
        }
        int i = 0;
        int total = srtFiles.size();
        MbToUniFile codePage = null;
        for (Path file : srtFiles) {
            updateStatus(i, total);
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
            String fileName = file.getFileName().toString();
            fileName = fileName.substring(0, fileName.length() - 3);
            Path dictFile = Paths.get(destination, fileName + "properties");
            try (BufferedWriter bw = Files.newBufferedWriter(dictFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                for (Map.Entry<Integer, String> entry : strFile.getEntriesAsSet()) {
                    bw.write(entry.getKey().toString());
                    bw.write("=");
                    bw.write(entry.getValue());
                    bw.newLine();
                }
            } catch (IOException ex) {
                String msg = "Failed to save the dictionary file to " + dictFile + "!";
                LOGGER.log(Level.SEVERE, msg, ex);
                throw new RuntimeException(msg, ex);
            }
        }
    }

    @Override
    public AssetsConverter.ConvertProcess getConvertProcess() {
        return AssetsConverter.ConvertProcess.INTERFACE_TEXTS;
    }

}
