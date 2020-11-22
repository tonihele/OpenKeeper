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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.FontCreator;
import toniarts.openkeeper.tools.convert.bf4.Bf4File;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.PathUtils;

/**
 * Dungeon Keeper II font conversion. Converts all fonts to jME friendly bitmap
 * fonts.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ConvertFonts extends ConversionTask {

    private static final Logger LOGGER = Logger.getLogger(ConvertFonts.class.getName());

    public ConvertFonts(String dungeonKeeperFolder, String destination, boolean overwriteData) {
        super(dungeonKeeperFolder, destination, overwriteData);
    }

    @Override
    public void internalExecuteTask() {
        convertFonts(dungeonKeeperFolder, destination);
    }

    /**
     * Extract and convert DK II font files (BF4)
     *
     * @param dungeonKeeperFolder DK II main folder
     * @param destination Destination folder
     */
    private void convertFonts(final String dungeonKeeperFolder, final String destination) {
        LOGGER.log(Level.INFO, "Extracting fonts to: {0}", destination);
        updateStatus(null, null);
        Path destFolder = Paths.get(destination);
        AssetUtils.deleteFolder(destFolder);

        try {

            // Make sure the folder exists
            Files.createDirectories(destFolder);

            // Find all the font files
            final List<Path> bf4Files = new ArrayList<>();
            Files.walkFileTree(Paths.get(dungeonKeeperFolder, PathUtils.DKII_TEXT_DEFAULT_FOLDER), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                    // Get all the BF4 files
                    if (file.getFileName().toString().toLowerCase().endsWith(".bf4") && attrs.isRegularFile()) {
                        bf4Files.add(file);
                    }

                    // Always continue
                    return FileVisitResult.CONTINUE;
                }
            });

            // Go through the font files
            int i = 0;
            int total = bf4Files.size();
            Pattern pattern = Pattern.compile("FONT_(?<name>\\D+)(?<size>\\d+)", Pattern.CASE_INSENSITIVE);
            for (Path file : bf4Files) {
                updateStatus(i, total);

                // The file names
                final int fontSize;

                final String imageFileName;
                final String descriptionFileName;
                Matcher matcher = pattern.matcher(file.getFileName().toString());
                boolean found = matcher.find();
                if (!found) {
                    LOGGER.log(Level.SEVERE, "Font name {0} not recognized!", file.getFileName());
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

    @Override
    public AssetsConverter.ConvertProcess getConvertProcess() {
        return AssetsConverter.ConvertProcess.FONTS;
    }

}
