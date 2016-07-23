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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import toniarts.openkeeper.tools.convert.sound.BankMapFile;
import toniarts.openkeeper.utils.PathUtils;

/**
 * Simple class to open up and browse through the *Bank.map files
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class BankMapLoader {

    private static String dkIIFolder;

    public static void main(String[] args) throws IOException {

        //Take Dungeon Keeper 2 root folder as parameter
        if (args.length != 1 || !new File(args[0]).exists()) {
            dkIIFolder = PathUtils.getDKIIFolder();
            if (dkIIFolder == null)
            {
                throw new RuntimeException("Please provide Dungeon Keeper II main folder as a first parameter!");
            }
        } else {
            dkIIFolder = PathUtils.fixFilePath(args[0]);
        }

        final String soundFolder = dkIIFolder.concat(PathUtils.DKII_DATA_FOLDER).concat(File.separator).concat(PathUtils.DKII_SOUND_FOLDER).concat(File.separator).concat(PathUtils.DKII_SFX_FOLDER).concat(File.separator);

        //Find all the bank.map files
        final List<File> bankMapFiles = new ArrayList<>();
        File dataDir = new File(soundFolder);
        Files.walkFileTree(dataDir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                //Get all the SDT files
                if (attrs.isRegularFile() && file.getFileName().toString().toLowerCase().endsWith("bank.map")) {
                    bankMapFiles.add(file.toFile());
                }

                //Always continue
                return FileVisitResult.CONTINUE;
            }
        });

        //Just open up the bank map files for fun
        for (File file : bankMapFiles) {
            BankMapFile bankMap = new BankMapFile(file);
            if (bankMap != null) {
                continue; // For debugging
            }
        }
    }
}
