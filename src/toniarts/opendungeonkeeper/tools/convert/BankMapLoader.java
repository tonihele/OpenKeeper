/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import toniarts.opendungeonkeeper.tools.convert.sound.BankMapFile;

/**
 * Simple class to open up and browse through the *Bank.map files
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class BankMapLoader {

    public static void main(String[] args) throws IOException {

        //Take Dungeon Keeper 2 root folder as parameter
        if (args.length != 1 && !new File(args[0]).exists()) {
            throw new RuntimeException("Please provide Dungeon Keeper II main folder as a first parameter!");
        }

        //Form the data path
        String dataDirectory = args[0];
        if (!dataDirectory.endsWith(File.separator)) {
            dataDirectory = dataDirectory.concat(File.separator);
        }
        dataDirectory = dataDirectory.concat("data").concat(File.separator).concat("sound").concat(File.separator).concat("sfx").concat(File.separator);

        //Find all the bank.map files
        final List<File> bankMapFiles = new ArrayList<>();
        File dataDir = new File(dataDirectory);
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
