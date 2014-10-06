/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import toniarts.opendungeonkeeper.tools.convert.Utils;
import toniarts.opendungeonkeeper.tools.convert.map.ArtResource.Image;
import toniarts.opendungeonkeeper.tools.convert.map.ArtResource.ResourceType;

/**
 * Reads a DK II map file, the KWD is the file name of the main map identifier,
 * reads the KLDs actually<br>
 * The files are LITTLE ENDIAN I might say<br>
 * Some values are 3D coordinates or scale values presented in fixed point
 * integers. They are automatically converted to floats (divided by 2^12 =
 * 4096)<br>
 * Many parts adapted from C code by:
 * <li>George Gensure (werkt)</li>
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class KwdFile {

    private static final float FIXED_POINT_DIVISION = 4096f;
    private Map[][] tiles;
    private int width;
    private int height;
    private HashMap<Short, Player> players;
    private HashMap<Short, Terrain> terrainTiles;
    private List<Door> doors;
    private List<Trap> traps;

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
        readDoors(dkIIPath);

        // EffectElemets
        // Effects
        // GlobalVariables
        // KeeperSpells
        // Objects
        // Rooms
        // Shots
        // Terrain catalog
        readTerrain(dkIIPath);

        // Traps
        readTraps(dkIIPath);
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
        readMapFile(file);

        // Read the requested PLAYER file
        readPlayerFile(file);
    }

    /**
     * Get the map width
     *
     * @return map width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Get the map height
     *
     * @return map height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Reads the *Map.kld
     *
     * @param file the original map KWD file
     * @throws RuntimeException reading may fail
     */
    private void readMapFile(File file) throws RuntimeException {

        // Read the requested MAP file
        File mapFile = new File(file.toString().substring(0, file.toString().length() - 4).concat("Map.kld"));
        try (RandomAccessFile rawMap = new RandomAccessFile(mapFile, "r")) {

            // Map file has a 36 header (height & width got confused in the keeper klan forums I think)
            // 8 - 11: Size of file w*h*4+36
            // 20 : width
            // 24 : height
            // 32 - 33: Size of map w*h*4
            // Lets just take the height & width
            rawMap.seek(20);
            width = Utils.readUnsignedInteger(rawMap);
            height = Utils.readUnsignedInteger(rawMap);

            // The map file is just simple blocks until EOF
            rawMap.seek(36);
            tiles = new Map[width][height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Map map = new Map();
                    map.setTerrainId((short) rawMap.readUnsignedByte());
                    map.setPlayerId((short) rawMap.readUnsignedByte());
                    map.setFlag((short) rawMap.readUnsignedByte());
                    map.setUnknown((short) rawMap.readUnsignedByte());
                    tiles[x][y] = map;
                }
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to read the file " + mapFile + "!", e);
        }
    }

    /**
     * Reads the *Players.kld
     *
     * @param file the original map KWD file
     * @throws RuntimeException reading may fail
     */
    private void readPlayerFile(File file) throws RuntimeException {

        // Read the requested PLAYER file
        File playerFile = new File(file.toString().substring(0, file.toString().length() - 4).concat("Players.kld"));
        try (RandomAccessFile rawPlayer = new RandomAccessFile(playerFile, "r")) {

            // Player file has a 36 header
            rawPlayer.seek(20);
            int playerCount = Utils.readUnsignedInteger(rawPlayer);

            // The map file is just simple blocks until EOF
            rawPlayer.seek(36); // End of header
            rawPlayer.skipBytes(20); // I don't know what is in here

            players = new HashMap<>(playerCount);
            for (int playerIndex = 0; playerIndex < playerCount; playerIndex++) {
                Player player = new Player();
                player.setStartingGold(Utils.readInteger(rawPlayer));
                player.setUnknown2(Utils.readInteger(rawPlayer));
                short[] unknown3 = new short[158];
                for (int i = 0; i < unknown3.length; i++) {
                    unknown3[i] = (short) rawPlayer.readUnsignedByte();
                }
                player.setUnknown3(unknown3);
                player.setUnknown4(Utils.readUnsignedShort(rawPlayer));
                player.setPlayerId((short) rawPlayer.readUnsignedByte());
                player.setUnknown5(Utils.readUnsignedShort(rawPlayer));
                player.setUnknown6(Utils.readUnsignedShort(rawPlayer));
                byte[] bytes = new byte[32];
                rawPlayer.read(bytes);
                player.setName(Utils.bytesToString(bytes).trim());

                // Add to the hash by the player ID
                players.put(player.getPlayerId(), player);
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to read the file " + playerFile + "!", e);
        }
    }

    /**
     * Reads the Terrain.kwd
     *
     * @param dkIIPath path to DK II data files (for filling up the catalogs)
     * @throws RuntimeException reading may fail
     */
    private void readTerrain(String dkIIPath) throws RuntimeException {

        // Read the terrain catalog
        File terrainFile = new File(dkIIPath.concat("Data").concat(File.separator).concat("editor").concat(File.separator).concat("Terrain.kwd"));
        try (RandomAccessFile rawTerrain = new RandomAccessFile(terrainFile, "r")) {

            // Terrain file has a 36 header
            rawTerrain.seek(20);
            int terrainCount = Utils.readUnsignedInteger(rawTerrain);

            // The terrain file is just simple blocks until EOF
            rawTerrain.seek(36); // End of header
            rawTerrain.skipBytes(20); // I don't know what is in here

            terrainTiles = new HashMap<>(terrainCount);
            for (int i = 0; i < terrainCount; i++) {
                Terrain terrain = new Terrain();
                byte[] bytes = new byte[32];
                rawTerrain.read(bytes);
                terrain.setName(Utils.bytesToString(bytes).trim());
                terrain.setComplete(readArtResource(rawTerrain));
                terrain.setSide(readArtResource(rawTerrain));
                terrain.setTop(readArtResource(rawTerrain));
                terrain.setTagged(readArtResource(rawTerrain));
                terrain.setStringIds(readStringId(rawTerrain));
                terrain.setUnk188(Utils.readUnsignedInteger(rawTerrain));
                terrain.setLightHeight(Utils.readUnsignedInteger(rawTerrain) / FIXED_POINT_DIVISION);
                terrain.setFlags(Utils.readUnsignedInteger(rawTerrain));
                terrain.setDamage(Utils.readUnsignedShort(rawTerrain));
                terrain.setUnk196(Utils.readUnsignedShort(rawTerrain));
                terrain.setUnk198(Utils.readUnsignedShort(rawTerrain));
                terrain.setGoldValue(Utils.readUnsignedShort(rawTerrain));
                terrain.setManaGain(Utils.readUnsignedShort(rawTerrain));
                terrain.setMaxManaGain(Utils.readUnsignedShort(rawTerrain));
                terrain.setUnk1a0(Utils.readUnsignedShort(rawTerrain));
                terrain.setUnk1a2(Utils.readUnsignedShort(rawTerrain));
                terrain.setUnk1a4(Utils.readUnsignedShort(rawTerrain));
                terrain.setUnk1a6(Utils.readUnsignedShort(rawTerrain));
                terrain.setUnk1a8(Utils.readUnsignedShort(rawTerrain));
                terrain.setUnk1aa(Utils.readUnsignedShort(rawTerrain));
                terrain.setUnk1ac(Utils.readUnsignedShort(rawTerrain));
                int[] unk1ae = new int[16];
                for (int x = 0; x < unk1ae.length; x++) {
                    unk1ae[x] = Utils.readUnsignedShort(rawTerrain);
                }
                terrain.setUnk1ae(unk1ae);
                terrain.setWibbleH((short) rawTerrain.readUnsignedByte());
                short[] leanH = new short[3];
                for (int x = 0; x < leanH.length; x++) {
                    leanH[x] = (short) rawTerrain.readUnsignedByte();
                }
                terrain.setLeanH(leanH);
                terrain.setWibbleV((short) rawTerrain.readUnsignedByte());
                short[] leanV = new short[3];
                for (int x = 0; x < leanV.length; x++) {
                    leanV[x] = (short) rawTerrain.readUnsignedByte();
                }
                terrain.setLeanV(leanV);
                terrain.setTerrainId((short) rawTerrain.readUnsignedByte());
                terrain.setStartingHealth(Utils.readUnsignedShort(rawTerrain));
                terrain.setMaxHealthType((short) rawTerrain.readUnsignedByte());
                terrain.setDestroyedType((short) rawTerrain.readUnsignedByte());
                terrain.setTerrainLight(new Color(rawTerrain.readUnsignedByte(), rawTerrain.readUnsignedByte(), rawTerrain.readUnsignedByte()));
                terrain.setTextureFrames((short) rawTerrain.readUnsignedByte());
                bytes = new byte[32];
                rawTerrain.read(bytes);
                terrain.setStr1(Utils.bytesToString(bytes).trim());
                terrain.setMaxHealth(Utils.readUnsignedShort(rawTerrain));
                terrain.setAmbientLight(new Color(rawTerrain.readUnsignedByte(), rawTerrain.readUnsignedByte(), rawTerrain.readUnsignedByte()));
                bytes = new byte[32];
                rawTerrain.read(bytes);
                terrain.setStr2(Utils.bytesToString(bytes).trim());
                terrain.setUnk224(Utils.readUnsignedInteger(rawTerrain));

                // Add to the hash by the terrain ID
                terrainTiles.put(terrain.getTerrainId(), terrain);
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to read the file " + terrainFile + "!", e);
        }
    }

    /**
     * Reads and parses an ArtResource object from the current file location
     *
     * @param file the file stream to parse from
     * @return an ArtResource
     */
    private ArtResource readArtResource(RandomAccessFile file) throws IOException {
        ArtResource artResource = new ArtResource();

        // Read the data
        byte[] bytes = new byte[64];
        file.read(bytes);
        artResource.setName(Utils.bytesToString(bytes).trim());
        int flags = Utils.readUnsignedInteger(file);
        bytes = new byte[12];
        file.read(bytes); // Depends on the type how these are interpreted?
        short type = (short) file.readUnsignedByte();
        short startAf = (short) file.readUnsignedByte();
        short endAf = (short) file.readUnsignedByte();
        short sometimesOne = (short) file.readUnsignedByte();

        // TODO: Map the types to the classes
        // 0 = ?
        // 1 = image?
        // 2 = ?
        // 4 = ?
        // 5 = ?
        // 6 = ?
        // Debug
        System.out.println("Type: " + type);
        int param1 = Utils.readUnsignedInteger(bytes);
        System.out.println("Param1: " + param1);

        ResourceType resourceType = null;
        switch (type) {
            case 1: { // Image
                resourceType = artResource.new Image();
                ((Image) resourceType).setWidth(Utils.readUnsignedInteger(Arrays.copyOfRange(bytes, 0, 4)) / FIXED_POINT_DIVISION);
                ((Image) resourceType).setHeight(Utils.readUnsignedInteger(Arrays.copyOfRange(bytes, 4, 8)) / FIXED_POINT_DIVISION);
                ((Image) resourceType).setFrames(Utils.readUnsignedShort(Arrays.copyOfRange(bytes, 8, 10)));
                break;
            }
        }

        // Add the common values
        if (resourceType != null) {
            resourceType.setFlags(flags);
            resourceType.setType(type);
            resourceType.setStartAf(startAf);
            resourceType.setEndAf(endAf);
            resourceType.setSometimesOne(sometimesOne);
        }
        artResource.setSettings(resourceType);

        return artResource;
    }

    /**
     * Reads and parses an StringId object from the current file location
     *
     * @param file the file stream to parse from
     * @return an StringId
     */
    private StringId readStringId(RandomAccessFile file) throws IOException {

        // Read the IDs
        int[] ids = new int[5];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = Utils.readUnsignedInteger(file);
        }

        // And the unknowns
        short[] x14 = new short[4];
        for (int i = 0; i < x14.length; i++) {
            x14[i] = (short) file.readUnsignedByte();
        }

        return new StringId(ids, x14);
    }

    /**
     * Reads the Doors.kwd
     *
     * @param dkIIPath path to DK II data files (for filling up the catalogs)
     * @throws RuntimeException reading may fail
     */
    private void readDoors(String dkIIPath) throws RuntimeException {

        // Read the doors catalog
        File doorsFile = new File(dkIIPath.concat("Data").concat(File.separator).concat("editor").concat(File.separator).concat("Doors.kwd"));
        try (RandomAccessFile rawDoors = new RandomAccessFile(doorsFile, "r")) {

            // Doors file has a 36 header
            rawDoors.seek(20);
            int doorCount = Utils.readUnsignedInteger(rawDoors);

            // The doors file is just simple blocks until EOF
            rawDoors.seek(36); // End of header
            rawDoors.skipBytes(20); // I don't know what is in here

            doors = new ArrayList<>(doorCount);
            for (int i = 0; i < doorCount; i++) {
                Door door = new Door();
                byte[] bytes = new byte[32];
                rawDoors.read(bytes);
                door.setName(Utils.bytesToString(bytes).trim());
                ArtResource[] ref = new ArtResource[5];
                for (int x = 0; x < ref.length; x++) {
                    ref[x] = readArtResource(rawDoors);
                }
                door.setRef(ref);
                short[] unknown = new short[164];
                for (int x = 0; x < unknown.length; x++) {
                    unknown[x] = (short) rawDoors.readUnsignedByte();
                }
                door.setUnknown(unknown);

                doors.add(door);
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to read the file " + doorsFile + "!", e);
        }
    }

    /**
     * Reads the Traps.kwd
     *
     * @param dkIIPath path to DK II data files (for filling up the catalogs)
     * @throws RuntimeException reading may fail
     */
    private void readTraps(String dkIIPath) throws RuntimeException {

        // Read the doors catalog
        File trapsFile = new File(dkIIPath.concat("Data").concat(File.separator).concat("editor").concat(File.separator).concat("Traps.kwd"));
        try (RandomAccessFile rawTraps = new RandomAccessFile(trapsFile, "r")) {

            // Traps file has a 36 header
            rawTraps.seek(20);
            int trapCount = Utils.readUnsignedInteger(rawTraps);

            // The doors file is just simple blocks until EOF
            rawTraps.seek(36); // End of header
            rawTraps.skipBytes(20); // I don't know what is in here

            traps = new ArrayList<>(trapCount);
            for (int i = 0; i < trapCount; i++) {
                Trap trap = new Trap();
                byte[] bytes = new byte[32];
                rawTraps.read(bytes);
                trap.setName(Utils.bytesToString(bytes).trim());
                ArtResource[] ref = new ArtResource[5];
                for (int x = 0; x < ref.length; x++) {
                    ref[x] = readArtResource(rawTraps);
                }
                trap.setRef(ref);
                short[] data = new short[127];
                for (int x = 0; x < data.length; x++) {
                    data[x] = (short) rawTraps.readUnsignedByte();
                }
                trap.setData(data);

                traps.add(trap);
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to read the file " + trapsFile + "!", e);
        }
    }
}
