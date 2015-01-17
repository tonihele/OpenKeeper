/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert;

import java.io.File;
import toniarts.opendungeonkeeper.tools.convert.map.KwdFile;

/**
 * A simple class to load up a map file
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MapLoader {

    public static void main(String[] args) {

        //Take Dungeon Keeper 2 root folder as parameter
        if (args.length != 2 || !new File(args[0]).exists()) {
            throw new RuntimeException("Please provide Dungeon Keeper II root folder as a first parameter! Second parameter is the actual map file!");
        }

        //Form the data path
        String dataDirectory = args[0];
        if (!dataDirectory.endsWith(File.separator)) {
            dataDirectory = dataDirectory.concat(File.separator);
        }

        //Load the map
        KwdFile kwd = new KwdFile(dataDirectory, new File(args[1]));
    }
}
