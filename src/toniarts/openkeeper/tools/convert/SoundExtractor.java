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
import toniarts.openkeeper.tools.convert.sound.SFFile;

import toniarts.openkeeper.tools.convert.sound.SdtFile;
import toniarts.openkeeper.tools.convert.sound.sfx.SfxMapFile;
import toniarts.openkeeper.utils.PathUtils;

/**
 * Simple class to extract all the files from given SDT to given location
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class SoundExtractor {

    private static String dkIIFolder;

    public static void main(String[] args) throws IOException {

        //Take Dungeon Keeper 2 root folder as parameter
        if (args.length != 2 || !new File(args[1]).exists()) {
            dkIIFolder = PathUtils.getDKIIFolder();
            if (dkIIFolder == null || args.length == 0)
            {
                throw new RuntimeException("Please provide extraction target folder as a first parameter! Second parameter is the Dungeon Keeper II main folder (optional)!");
            }
        } else {
            dkIIFolder = PathUtils.fixFilePath(args[1]);
        }

        final String soundFolder = dkIIFolder + PathUtils.DKII_SFX_FOLDER;

        //And the destination
        String destination = PathUtils.fixFilePath(args[0]);

        //Find all the sound files
        final List<File> sdtFiles = new ArrayList<>();
        File dataDir = new File(soundFolder);
        //Find all the bank.map files
        final List<File> bankMapFiles = new ArrayList<>();
        //Find all the bank.map files
        final List<File> sfxMapFiles = new ArrayList<>();
        //Find all the bank.map files
        final List<File> sf2Files = new ArrayList<>();
        Files.walkFileTree(dataDir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                //Get all the SDT files
                if (attrs.isRegularFile()) {
                    String filename = file.getFileName().toString().toLowerCase();
                    if (filename.endsWith(".sdt")){
                        sdtFiles.add(file.toFile());
                    } else if (filename.endsWith(".sf2")) {
                        sf2Files.add(file.toFile());
                    } else if (filename.endsWith("bank.map")) {
                        bankMapFiles.add(file.toFile());
                    } else if (filename.endsWith("sfx.map")) {
                        sfxMapFiles.add(file.toFile());
                    }
                }

                //Always continue
                return FileVisitResult.CONTINUE;
            }
        });

        // TODO unpack files
        //Just open up the bank map files for fun
        for (File file : bankMapFiles) {
            BankMapFile bankMap = new BankMapFile(file);
            //System.out.println(bankMap);
        }
        //Just open up the sfx map files for fun
        for (File file : sfxMapFiles) {
            SfxMapFile sfxMap = new SfxMapFile(file);
            //System.out.println(sfxMap);
        }
        //Just open up the sf2 map files for fun
        for (File file : sf2Files) {
            SFFile sf2File = new SFFile(file);
            //System.out.println(sf2File);
        }
        //Extract the sounds
        for (File file : sdtFiles) {
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
}
