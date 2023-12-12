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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.FontCreator;
import toniarts.openkeeper.tools.convert.FontCreator.FontImage;
import toniarts.openkeeper.tools.convert.bf4.Bf4File;
import toniarts.openkeeper.utils.PathUtils;
import toniarts.openkeeper.utils.Utils;

/**
 * Dungeon Keeper II font conversion. Converts all fonts to jME friendly bitmap
 * fonts.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ConvertFonts extends ConversionTask {

    private static final Logger LOGGER = System.getLogger(ConvertFonts.class.getName());

    private final ExecutorService executorService;

    public ConvertFonts(String dungeonKeeperFolder, String destination, boolean overwriteData) {
        super(dungeonKeeperFolder, destination, overwriteData);

        this.executorService = Executors.newFixedThreadPool(Utils.MAX_THREADS, new ThreadFactory() {

            private final AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "FontsConverter_" + threadIndex.incrementAndGet());
            }

        });
    }

    @Override
    public void internalExecuteTask() {
        try {
            convertFonts(dungeonKeeperFolder, destination);
        } finally {
            executorService.shutdown();
            try {
                executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.ERROR, "Failed to wait font conversion complete!", ex);
            }
        }
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
        PathUtils.deleteFolder(destFolder);
        ImageIO.setUseCache(false);

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
            AtomicInteger progress = new AtomicInteger(0);
            int total = bf4Files.size();
            updateStatus(0, total);
            Pattern pattern = Pattern.compile("FONT_(?<name>\\D+)(?<size>\\d+)", Pattern.CASE_INSENSITIVE);
            for (Path file : bf4Files) {
                executorService.submit(() -> {
                    handleFontFile(pattern, file, destination, progress, total);
                });
            }

        } catch (Exception ex) {
            String msg = "Failed to save the font files to " + destination + "!";
            LOGGER.log(Level.ERROR, msg, ex);
            throw new RuntimeException(msg, ex);
        }
    }

    private void handleFontFile(Pattern pattern, Path file, final String destination, AtomicInteger progress, int total) {
        try {

            // The file names
            final int fontSize;
            final String imageFileName;
            final String descriptionFileName;
            Matcher matcher = pattern.matcher(file.getFileName().toString());
            boolean found = matcher.find();
            if (!found) {
                LOGGER.log(Level.ERROR, "Font name {0} not recognized!", file.getFileName());
                throw new RuntimeException("Unknown font name!");
            }

            // Parse font info from the file name
            fontSize = Integer.parseInt(matcher.group("size"));
            String baseFileName = matcher.group("name");
            baseFileName = destination.concat(Character.toUpperCase(baseFileName.charAt(0)) + baseFileName.substring(1).toLowerCase() + fontSize);
            imageFileName = baseFileName.substring(destination.length()).concat(".png");
            descriptionFileName = baseFileName.concat(".fnt");

            // Convert & save the font files
            FontCreator fc = new FontCreator(new Bf4File(file), fontSize, imageFileName);
            for (FontImage fontImage : fc.getFontImages()) {
                Path destPath = Paths.get(destination, fontImage.getFileName());
                try (OutputStream os = Files.newOutputStream(destPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                        BufferedOutputStream bos = new BufferedOutputStream(os)) {
                    ImageIO.write(fontImage.getFontImage(), "png", bos);
                }
            }
            try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(descriptionFileName), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                bw.write(fc.getDescription());
            }

            updateStatus(progress.incrementAndGet(), total);
        } catch (Exception ex) {
            String msg = "Failed to export font file " + file + "!";
            LOGGER.log(Level.ERROR, msg, ex);
            onError(new RuntimeException(msg, ex));
        }
    }

    @Override
    public AssetsConverter.ConvertProcess getConvertProcess() {
        return AssetsConverter.ConvertProcess.FONTS;
    }

}
