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
import toniarts.opendungeonkeeper.tools.convert.sound.SdtFile;

/**
 * Simple class to extract all the files from given SDT to given location
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class SdtExtractor {

    public static void main(String[] args) throws IOException {

        //Take Dungeon Keeper 2 root folder as parameter
        if (args.length != 2 || !new File(args[0]).exists()) {
            throw new RuntimeException("Please provide Dungeon Keeper II main folder as a first parameter! Second parameter is the extraction target folder!");
        }

        //Form the data path
        String dataDirectory = args[0];
        if (!dataDirectory.endsWith(File.separator)) {
            dataDirectory = dataDirectory.concat(File.separator);
        }
        dataDirectory = dataDirectory.concat("data").concat(File.separator).concat("sound").concat(File.separator).concat("sfx").concat(File.separator);

        //And the destination
        String destination = args[1];
        if (!destination.endsWith(File.separator)) {
            destination = destination.concat(File.separator);
        }

        //Find all the sound files
        final List<File> sdtFiles = new ArrayList<>();
        File dataDir = new File(dataDirectory);
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
