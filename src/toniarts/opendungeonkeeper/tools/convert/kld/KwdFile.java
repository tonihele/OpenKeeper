/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.kld;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a DK II map file, the KWD is the file name of the main map identifier,
 * reads the KLDs actually<br>
 * The file is LITTLE ENDIAN I might say<br>
 * Many parts adapted from C code by:
 * <li>George Gensure (werkt)</li>
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class KwdFile {

    private List<Map> tiles;

    /**
     * Constructs a new KWD file reader<br>
     * Reads the KWD catalogs files from the DKII dir
     *
     * @param dkIIPath path to DK II data files (for filling up the catalogs)
     */
    public KwdFile(String dkIIPath) {
        // First we should read the catalogs which are the same to all maps
        // However it seems possible for a map to have its own custom catalog
        // The format is probably the same, it is just stored with the map (in the main KWD if you don't want to guess from the file names)
        // It should be easy to support such, but I don't see the why
        // These are in /Data/editor/*.kwd
        // Creatures
        // CreatureSpells
        // Doors
        // EffectElemets
        // Effects
        // GlobalVariables
        // KeeperSpells
        // Objects
        // Rooms
        // Shots
        // Terrain
        // Traps
    }

    /**
     * Reads the actual map, assumes that default catalogs are in use<br>
     * KWD has information about catalogs used, but we don't use it<br>
     * We just assume the KLDs are with standard names
     *
     * @param file the KWD file to read
     */
    public void loadMap(File file) {

        // Read the requested MAP file
        File mapFile = new File(file.toString().substring(0, file.toString().length() - 4).concat("Map.kld"));
        try (RandomAccessFile rawMap = new RandomAccessFile(mapFile, "r")) {

            // The map file is just simple blocks until EOF
            tiles = new ArrayList<>((int) (rawMap.length() / 4));
            while (rawMap.getFilePointer() != rawMap.length()) {
                Map map = new Map();
                map.setTerrainId((short) rawMap.readUnsignedByte());
                map.setPlayerId((short) rawMap.readUnsignedByte());
                map.setFlag((short) rawMap.readUnsignedByte());
                map.setUnknown((short) rawMap.readUnsignedByte());
                tiles.add(map);
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to open the file " + mapFile + "!", e);
        }
    }
}
