/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert;

import java.io.File;
import toniarts.opendungeonkeeper.tools.convert.wad.WadFile;

/**
 * Simple class to extract all the files from given WAD to given location
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class WadExtractor {

    public static void main(String[] args) {

        //Take Dungeon Keeper 2 root folder as parameter
        if (args.length != 2 || !new File(args[0]).exists()) {
            throw new RuntimeException("Please provide Dungeon Keeper II root folder as a first parameter! Second parameter is the extraction target folder!");
        }

        //Form the data path
        String dataDirectory = args[0];
        if (!dataDirectory.endsWith(File.separator)) {
            dataDirectory = dataDirectory.concat(File.separator);
        }
        dataDirectory = dataDirectory.concat("data").concat(File.separator);

        //And the destination
        String destination = args[1];
        if (!destination.endsWith(File.separator)) {
            destination = destination.concat(File.separator);
        }

        //Extract the meshes
        WadFile wad = new WadFile(new File(dataDirectory + "Meshes.WAD"));
        wad.extractFileData(destination.concat("meshes"));
    }
}
