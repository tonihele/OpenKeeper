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
import javax.vecmath.Vector3f;
import toniarts.opendungeonkeeper.tools.convert.Utils;
import toniarts.opendungeonkeeper.tools.convert.map.ArtResource.Animation;
import toniarts.opendungeonkeeper.tools.convert.map.ArtResource.Image;
import toniarts.opendungeonkeeper.tools.convert.map.ArtResource.Mesh;
import toniarts.opendungeonkeeper.tools.convert.map.ArtResource.ResourceType;
import toniarts.opendungeonkeeper.tools.convert.map.Creature.Attraction;
import toniarts.opendungeonkeeper.tools.convert.map.Creature.Unk7;
import toniarts.opendungeonkeeper.tools.convert.map.Creature.X1323;
import toniarts.opendungeonkeeper.tools.convert.map.Creature.Xdc4;
import toniarts.opendungeonkeeper.tools.convert.map.Creature.Xe0c;
import toniarts.opendungeonkeeper.tools.convert.map.Creature.Xe14;
import toniarts.opendungeonkeeper.tools.convert.map.Creature.Xe7c;
import toniarts.opendungeonkeeper.tools.convert.map.Creature.Xe94;
import toniarts.opendungeonkeeper.tools.convert.map.Object;
import toniarts.opendungeonkeeper.tools.convert.map.Thing.ActionPoint;
import toniarts.opendungeonkeeper.tools.convert.map.Thing.Thing03;
import toniarts.opendungeonkeeper.tools.convert.map.Thing.Thing08;
import toniarts.opendungeonkeeper.tools.convert.map.Thing.Thing08.HeroPartyData;
import toniarts.opendungeonkeeper.tools.convert.map.Thing.Thing10;
import toniarts.opendungeonkeeper.tools.convert.map.Thing.Thing11;
import toniarts.opendungeonkeeper.tools.convert.map.Thing.Thing12;

/**
 * Reads a DK II map file, the KWD is the file name of the main map identifier,
 * reads the KLDs actually<br>
 * The files are LITTLE ENDIAN I might say<br>
 * Some values are 3D coordinates or scale values presented in fixed point
 * integers. They are automatically converted to floats (divided by 2^12 = 4096
 * or 2^16 = 65536)<br>
 * Many parts adapted from C code by:
 * <li>George Gensure (werkt)</li>
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class KwdFile {

    private static final float FIXED_POINT_DIVISION = 4096f;
    private static final float FIXED_POINT5_DIVISION = 65536f;
    private Map[][] tiles;
    private int width;
    private int height;
    private HashMap<Short, Player> players;
    private HashMap<Short, Terrain> terrainTiles;
    private List<Door> doors;
    private List<Trap> traps;
    private HashMap<Short, Room> rooms;
    private String name;
    private String description;
    private String author;
    private String email;
    private String information;
    private HashMap<Short, Creature> creatures;
    private HashMap<Short, Object> objects;
    private List<CreatureSpell> creatureSpells;
    private HashMap<Integer, EffectElement> effectElements;
    private HashMap<Integer, Effect> effects;
    private HashMap<Short, KeeperSpell> keeperSpells;
    private List<Thing> things;
    private HashMap<Short, Shot> shots;

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
        readCreatures(dkIIPath);

        // CreatureSpells
        readCreatureSpells(dkIIPath);

        // Doors
        readDoors(dkIIPath);

        // EffectElemets
        readEffectElements(dkIIPath);

        // Effects
        readEffects(dkIIPath);

        // GlobalVariables
        // KeeperSpells
        readKeeperSpells(dkIIPath);

        // Objects
        readObjects(dkIIPath);

        // Rooms
        readRooms(dkIIPath);

        // Shots
        readShots(dkIIPath);

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

        // Load some map info, nice to show
        readMapInfo(file);

        // Read the requested MAP file
        readMapFile(file);

        // Read the requested PLAYERS file
        readPlayersFile(file);

        // Read the requested THINGS
        readThingsFile(file);
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
    private void readPlayersFile(File file) throws RuntimeException {

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
        // 0 = nothing, should be just the basic class?
        // 1 = image?
        // 2 = terrain resource?
        // 4 = ?
        // 5 = mesh?
        // 6 = animation?
        // Debug
        System.out.println("Type: " + type);
        int param1 = Utils.readUnsignedInteger(bytes);
        System.out.println("Param1: " + param1);

        ResourceType resourceType = artResource.new ResourceType();
        switch (type) {
            case 1: { // Image
                resourceType = artResource.new Image();
                ((Image) resourceType).setWidth(Utils.readUnsignedInteger(Arrays.copyOfRange(bytes, 0, 4)) / FIXED_POINT_DIVISION);
                ((Image) resourceType).setHeight(Utils.readUnsignedInteger(Arrays.copyOfRange(bytes, 4, 8)) / FIXED_POINT_DIVISION);
                ((Image) resourceType).setFrames(Utils.readUnsignedShort(Arrays.copyOfRange(bytes, 8, 10)));
                break;
            }
            case 2: {
                resourceType = artResource.new Mesh();
                break;
            }
            case 4: {
                resourceType = artResource.new Mesh();
                break;
            }
            case 5: {
                resourceType = artResource.new Mesh();
                ((Mesh) resourceType).setScale(Utils.readUnsignedInteger(Arrays.copyOfRange(bytes, 0, 4)) / FIXED_POINT_DIVISION);
                ((Mesh) resourceType).setFrames(Utils.readUnsignedShort(Arrays.copyOfRange(bytes, 4, 6)));
                break;
            }
            case 6: {
                resourceType = artResource.new Animation();
                ((Animation) resourceType).setFrames(Utils.readUnsignedInteger(Arrays.copyOfRange(bytes, 0, 4)));
                ((Animation) resourceType).setFps(Utils.readUnsignedInteger(Arrays.copyOfRange(bytes, 4, 8)));
                ((Animation) resourceType).setStartDist(Utils.readUnsignedShort(Arrays.copyOfRange(bytes, 8, 10)));
                ((Animation) resourceType).setEndDist(Utils.readUnsignedShort(Arrays.copyOfRange(bytes, 10, 12)));
                break;
            }
        }

        // Add the common values
        resourceType.setFlags(flags);
        resourceType.setType(type);
        resourceType.setStartAf(startAf);
        resourceType.setEndAf(endAf);
        resourceType.setSometimesOne(sometimesOne);
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

        // Read the traps catalog
        File trapsFile = new File(dkIIPath.concat("Data").concat(File.separator).concat("editor").concat(File.separator).concat("Traps.kwd"));
        try (RandomAccessFile rawTraps = new RandomAccessFile(trapsFile, "r")) {

            // Traps file has a 36 header
            rawTraps.seek(20);
            int trapCount = Utils.readUnsignedInteger(rawTraps);

            // The traps file is just simple blocks until EOF
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

    /**
     * Reads the Rooms.kwd
     *
     * @param dkIIPath path to DK II data files (for filling up the catalogs)
     * @throws RuntimeException reading may fail
     */
    private void readRooms(String dkIIPath) throws RuntimeException {

        // Read the rooms catalog
        File roomsFile = new File(dkIIPath.concat("Data").concat(File.separator).concat("editor").concat(File.separator).concat("Rooms.kwd"));
        try (RandomAccessFile rawRooms = new RandomAccessFile(roomsFile, "r")) {

            // Room file has a 36 header
            rawRooms.seek(20);
            int roomsCount = Utils.readUnsignedInteger(rawRooms);

            // The rooms file is just simple blocks until EOF
            rawRooms.seek(36); // End of header
            rawRooms.skipBytes(20); // I don't know what is in here

            rooms = new HashMap<>(roomsCount);
            for (int i = 0; i < roomsCount; i++) {
                Room room = new Room();
                byte[] bytes = new byte[32];
                rawRooms.read(bytes);
                room.setName(Utils.bytesToString(bytes).trim());
                room.setGuiIcon(readArtResource(rawRooms));
                room.setRoomIcon(readArtResource(rawRooms));
                room.setComplete(readArtResource(rawRooms));
                ArtResource[] ref = new ArtResource[7];
                for (int x = 0; x < ref.length; x++) {
                    ref[x] = readArtResource(rawRooms);
                }
                room.setRef(ref);
                room.setUnknown1(Utils.readUnsignedInteger(rawRooms));
                room.setUnknown2(Utils.readUnsignedShort(rawRooms));
                room.setTorchIntensity(Utils.readUnsignedShort(rawRooms));
                room.setUnknown3(Utils.readUnsignedInteger(rawRooms));
                room.setX374(Utils.readUnsignedShort(rawRooms));
                room.setX376(Utils.readUnsignedShort(rawRooms));
                room.setX378(Utils.readUnsignedShort(rawRooms));
                room.setX37a(Utils.readUnsignedShort(rawRooms));
                room.setX37c(Utils.readUnsignedShort(rawRooms));
                room.setX37e(Utils.readUnsignedShort(rawRooms));
                room.setTorchRadius(Utils.readUnsignedShort(rawRooms) / FIXED_POINT_DIVISION);
                int[] effects = new int[8];
                for (int x = 0; x < effects.length; x++) {
                    effects[x] = Utils.readUnsignedShort(rawRooms);
                }
                room.setEffects(effects);
                room.setRoomId((short) rawRooms.readUnsignedByte());
                room.setUnknown7((short) rawRooms.readUnsignedByte());
                room.setTerrainId((short) rawRooms.readUnsignedByte());
                room.setTileConstruction((short) rawRooms.readUnsignedByte());
                room.setUnknown8((short) rawRooms.readUnsignedByte());
                room.setTorchColor(new Color(rawRooms.readUnsignedByte(), rawRooms.readUnsignedByte(), rawRooms.readUnsignedByte()));
                short[] objects = new short[8];
                for (int x = 0; x < objects.length; x++) {
                    objects[x] = (short) rawRooms.readUnsignedByte();
                }
                room.setObjects(objects);
                bytes = new byte[32];
                rawRooms.read(bytes);
                room.setSoundCategory(Utils.bytesToString(bytes).trim());
                room.setX3c2((short) rawRooms.readUnsignedByte());
                room.setX3c3((short) rawRooms.readUnsignedByte());
                room.setUnknown10(Utils.readUnsignedShort(rawRooms));
                room.setUnknown11((short) rawRooms.readUnsignedByte());
                room.setTorch(readArtResource(rawRooms));
                room.setX41b((short) rawRooms.readUnsignedByte());
                room.setX41c((short) rawRooms.readUnsignedByte());
                room.setX41d(Utils.readShort(rawRooms));

                // Add to the hash by the room ID
                rooms.put(room.getRoomId(), room);
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to read the file " + roomsFile + "!", e);
        }
    }

    /**
     * Reads the *.kwd
     *
     * @param file the original map KWD file
     * @throws RuntimeException reading may fail
     */
    private void readMapInfo(File file) throws RuntimeException {

        // Read the file
        try (RandomAccessFile rawMapInfo = new RandomAccessFile(file, "r")) {

            rawMapInfo.seek(36); // End of header
            rawMapInfo.skipBytes(20); // I don't know what is in here

            byte[] bytes = new byte[64 * 2];
            rawMapInfo.read(bytes);
            name = (Utils.bytesToStringUtf16(bytes).trim());

            bytes = new byte[1024 * 2];
            rawMapInfo.read(bytes);
            description = (Utils.bytesToStringUtf16(bytes).trim());

            bytes = new byte[64 * 2];
            rawMapInfo.read(bytes);
            author = (Utils.bytesToStringUtf16(bytes).trim());

            bytes = new byte[64 * 2];
            rawMapInfo.read(bytes);
            email = (Utils.bytesToStringUtf16(bytes).trim());

            bytes = new byte[1024 * 2];
            rawMapInfo.read(bytes);
            information = (Utils.bytesToStringUtf16(bytes).trim());
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to read the file " + file + "!", e);
        }
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthor() {
        return author;
    }

    public String getEmail() {
        return email;
    }

    public String getInformation() {
        return information;
    }

    /**
     * Reads the Creatures.kwd
     *
     * @param dkIIPath path to DK II data files (for filling up the catalogs)
     * @throws RuntimeException reading may fail
     */
    private void readCreatures(String dkIIPath) throws RuntimeException {

        // Read the creatures catalog
        File creaturesFile = new File(dkIIPath.concat("Data").concat(File.separator).concat("editor").concat(File.separator).concat("Creatures.kwd"));
        try (RandomAccessFile rawCreatures = new RandomAccessFile(creaturesFile, "r")) {

            // Creatures file has a 36 header
            rawCreatures.seek(20);
            int creaturesCount = Utils.readUnsignedInteger(rawCreatures);

            // The creatures file is just simple blocks until EOF
            rawCreatures.seek(36); // End of header
            rawCreatures.skipBytes(20); // I don't know what is in here

            creatures = new HashMap<>(creaturesCount);
            for (int i = 0; i < creaturesCount; i++) {
                Creature creature = new Creature();
                byte[] bytes = new byte[32];
                rawCreatures.read(bytes);
                creature.setName(Utils.bytesToString(bytes).trim());
                ArtResource[] ref1 = new ArtResource[39];
                for (int x = 0; x < ref1.length; x++) {
                    ref1[x] = readArtResource(rawCreatures);
                }
                creature.setRef1(ref1);
                creature.setUnkcec(Utils.readUnsignedShort(rawCreatures));
                creature.setUnkcee(Utils.readUnsignedInteger(rawCreatures));
                creature.setUnkcf2(Utils.readUnsignedInteger(rawCreatures));
                creature.setEditorOrder((short) rawCreatures.readUnsignedByte());
                creature.setUnk2c(Utils.readUnsignedShort(rawCreatures));
                creature.setShotDelay(Utils.readUnsignedInteger(rawCreatures));
                creature.setUnkcfd(Utils.readUnsignedShort(rawCreatures));
                creature.setUnkcff(Utils.readUnsignedShort(rawCreatures));
                creature.setUnkd01(Utils.readUnsignedInteger(rawCreatures));
                int[] unk2d = new int[9];
                for (int x = 0; x < unk2d.length; x++) {
                    unk2d[x] = Utils.readUnsignedShort(rawCreatures);
                }
                creature.setUnk2d(unk2d);
                bytes = new byte[32];
                rawCreatures.read(bytes);
                creature.setUnkd17(Utils.bytesToString(bytes).trim());
                creature.setShuffleSpeed(Utils.readUnsignedInteger(rawCreatures));
                short[] unk2e = new short[5];
                for (int x = 0; x < unk2e.length; x++) {
                    unk2e[x] = (short) rawCreatures.readUnsignedByte();
                }
                creature.setUnk2e(unk2e);
                creature.setRef2(readArtResource(rawCreatures));
                creature.setLight(readLight(rawCreatures));
                Attraction[] attractions = new Attraction[2];
                for (int x = 0; x < attractions.length; x++) {
                    Attraction attraction = creature.new Attraction();
                    attraction.setPresent(Utils.readUnsignedInteger(rawCreatures));
                    attraction.setRoomIdAndSize(Utils.readUnsignedInteger(rawCreatures));
                    attractions[x] = attraction;
                }
                creature.setAttraction(attractions);
                creature.setUnkdbc(Utils.readUnsignedInteger(rawCreatures));
                creature.setUnkdc0(Utils.readUnsignedInteger(rawCreatures));
                Xdc4[] xdc4s = new Xdc4[3];
                for (int x = 0; x < xdc4s.length; x++) {
                    Xdc4 xdc4 = creature.new Xdc4();
                    xdc4.setX00(Utils.readUnsignedInteger(rawCreatures));
                    xdc4.setX04(Utils.readUnsignedInteger(rawCreatures));
                    xdc4.setX08(Utils.readUnsignedInteger(rawCreatures));
                    xdc4.setX0c((short) rawCreatures.readUnsignedByte());
                    xdc4.setX0d((short) rawCreatures.readUnsignedByte());
                    xdc4.setX0e((short) rawCreatures.readUnsignedByte());
                    xdc4.setX0f((short) rawCreatures.readUnsignedByte());
                    xdc4.setX10(Utils.readUnsignedInteger(rawCreatures));
                    xdc4.setX14((short) rawCreatures.readUnsignedByte());
                    xdc4.setX15((short) rawCreatures.readUnsignedByte());
                    xdc4.setX16((short) rawCreatures.readUnsignedByte());
                    xdc4.setX17((short) rawCreatures.readUnsignedByte());
                    xdc4s[x] = xdc4;
                }
                creature.setXdc4(xdc4s);
                Xe0c[] xe0cs = new Xe0c[4];
                for (int x = 0; x < xe0cs.length; x++) {
                    Xe0c xe0c = creature.new Xe0c();
                    xe0c.setX00((short) rawCreatures.readUnsignedByte());
                    xe0c.setX01((short) rawCreatures.readUnsignedByte());
                    xe0cs[x] = xe0c;
                }
                creature.setXe0c(xe0cs);
                Xe14[] xe14s = new Xe14[3];
                for (int x = 0; x < xe14s.length; x++) {
                    Xe14 xe14 = creature.new Xe14();
                    xe14.setX00(Utils.readUnsignedInteger(rawCreatures));
                    xe14.setX04(Utils.readUnsignedShort(rawCreatures));
                    xe14.setX06(Utils.readUnsignedShort(rawCreatures));
                    xe14.setX08((short) rawCreatures.readUnsignedByte());
                    xe14.setX09((short) rawCreatures.readUnsignedByte());
                    xe14.setX0a((short) rawCreatures.readUnsignedByte());
                    xe14.setX0b((short) rawCreatures.readUnsignedByte());
                    xe14s[x] = xe14;
                }
                creature.setXe14(xe14s);
                Xe14[] xe38s = new Xe14[2];
                for (int x = 0; x < xe38s.length; x++) {
                    Xe14 xe14 = creature.new Xe14();
                    xe14.setX00(Utils.readUnsignedInteger(rawCreatures));
                    xe14.setX04(Utils.readUnsignedShort(rawCreatures));
                    xe14.setX06(Utils.readUnsignedShort(rawCreatures));
                    xe14.setX08((short) rawCreatures.readUnsignedByte());
                    xe14.setX09((short) rawCreatures.readUnsignedByte());
                    xe14.setX0a((short) rawCreatures.readUnsignedByte());
                    xe14.setX0b((short) rawCreatures.readUnsignedByte());
                    xe38s[x] = xe14;
                }
                creature.setXe38(xe38s);
                Xe14[] xe50s = new Xe14[3];
                for (int x = 0; x < xe50s.length; x++) {
                    Xe14 xe14 = creature.new Xe14();
                    xe14.setX00(Utils.readUnsignedInteger(rawCreatures));
                    xe14.setX04(Utils.readUnsignedShort(rawCreatures));
                    xe14.setX06(Utils.readUnsignedShort(rawCreatures));
                    xe14.setX08((short) rawCreatures.readUnsignedByte());
                    xe14.setX09((short) rawCreatures.readUnsignedByte());
                    xe14.setX0a((short) rawCreatures.readUnsignedByte());
                    xe14.setX0b((short) rawCreatures.readUnsignedByte());
                    xe50s[x] = xe14;
                }
                creature.setXe50(xe50s);
                int[] xe74 = new int[2];
                for (int x = 0; x < xe74.length; x++) {
                    xe74[x] = Utils.readUnsignedInteger(rawCreatures);
                }
                creature.setXe74(xe74);
                Xe7c[] xe7cs = new Xe7c[3];
                for (int x = 0; x < xe7cs.length; x++) {
                    Xe7c xe7c = creature.new Xe7c();
                    xe7c.setX00(Utils.readUnsignedInteger(rawCreatures));
                    xe7c.setX04(Utils.readUnsignedShort(rawCreatures));
                    xe7c.setX06(Utils.readUnsignedShort(rawCreatures));
                    xe7cs[x] = xe7c;
                }
                creature.setXe7c(xe7cs);
                Xe94 xe94 = creature.new Xe94();
                xe94.setX00(Utils.readUnsignedInteger(rawCreatures));
                xe94.setX04(Utils.readUnsignedInteger(rawCreatures));
                xe94.setX08(Utils.readUnsignedInteger(rawCreatures));
                creature.setXe94(xe94);
                creature.setUnkea0(Utils.readInteger(rawCreatures));
                creature.setHeight(Utils.readInteger(rawCreatures) / FIXED_POINT_DIVISION);
                creature.setUnkea8(Utils.readUnsignedInteger(rawCreatures));
                creature.setUnk3ab(Utils.readUnsignedInteger(rawCreatures));
                creature.setEyeHeight(Utils.readInteger(rawCreatures) / FIXED_POINT_DIVISION);
                creature.setSpeed(Utils.readInteger(rawCreatures));
                creature.setRunSpeed(Utils.readInteger(rawCreatures));
                creature.setUnk3ac(Utils.readUnsignedInteger(rawCreatures));
                creature.setTimeAwake(Utils.readUnsignedInteger(rawCreatures));
                creature.setTimeSleep(Utils.readUnsignedInteger(rawCreatures));
                creature.setUnkec8(Utils.readUnsignedInteger(rawCreatures));
                creature.setUnkecc(Utils.readUnsignedInteger(rawCreatures));
                creature.setUnked0(Utils.readUnsignedInteger(rawCreatures));
                creature.setUnked4(Utils.readUnsignedInteger(rawCreatures));
                creature.setUnked8(Utils.readUnsignedInteger(rawCreatures));
                creature.setSlapFearlessDuration(Utils.readInteger(rawCreatures));
                creature.setUnkee0(Utils.readInteger(rawCreatures));
                creature.setUnkee4(Utils.readInteger(rawCreatures));
                creature.setPossessionManaCost(Utils.readShort(rawCreatures));
                creature.setOwnLandHealthIncrease(Utils.readShort(rawCreatures));
                creature.setRange(Utils.readInteger(rawCreatures));
                creature.setUnkef0(Utils.readUnsignedInteger(rawCreatures));
                creature.setUnk3af(Utils.readUnsignedInteger(rawCreatures));
                creature.setMeleeRecharge(Utils.readInteger(rawCreatures));
                creature.setUnkefc(Utils.readUnsignedInteger(rawCreatures));
                creature.setExpForNextLevel(Utils.readUnsignedShort(rawCreatures));
                short[] unk3b = new short[2];
                for (int x = 0; x < unk3b.length; x++) {
                    unk3b[x] = (short) rawCreatures.readUnsignedByte();
                }
                creature.setUnk3b(unk3b);
                creature.setExpPerSecond(Utils.readUnsignedShort(rawCreatures));
                creature.setExpPerSecondTraining(Utils.readUnsignedShort(rawCreatures));
                creature.setResearchPerSecond(Utils.readUnsignedShort(rawCreatures));
                creature.setManufacturePerSecond(Utils.readUnsignedShort(rawCreatures));
                creature.setHp(Utils.readUnsignedShort(rawCreatures));
                creature.setHpFromChicken(Utils.readUnsignedShort(rawCreatures));
                creature.setFear(Utils.readUnsignedShort(rawCreatures));
                creature.setThreat(Utils.readUnsignedShort(rawCreatures));
                creature.setMeleeDamage(Utils.readUnsignedShort(rawCreatures));
                creature.setSlapDamage(Utils.readUnsignedShort(rawCreatures));
                creature.setManaGenPrayer(Utils.readUnsignedShort(rawCreatures));
                creature.setUnk3cb(Utils.readUnsignedShort(rawCreatures));
                creature.setPay(Utils.readUnsignedShort(rawCreatures));
                creature.setMaxGoldHeld(Utils.readUnsignedShort(rawCreatures));
                creature.setUnk3cc(Utils.readUnsignedShort(rawCreatures));
                creature.setDecomposeValue(Utils.readUnsignedShort(rawCreatures));
                int[] unk3cd = new int[2];
                for (int x = 0; x < unk3cd.length; x++) {
                    unk3cd[x] = Utils.readUnsignedShort(rawCreatures);
                }
                creature.setUnk3cd(unk3cd);
                creature.setAngerNoLair(Utils.readShort(rawCreatures));
                creature.setAngerNoFood(Utils.readShort(rawCreatures));
                creature.setAngerNoPay(Utils.readShort(rawCreatures));
                creature.setAngerNoWork(Utils.readShort(rawCreatures));
                creature.setAngerSlap(Utils.readShort(rawCreatures));
                creature.setAngerInHand(Utils.readShort(rawCreatures));
                creature.setInitialGoldHeld(Utils.readShort(rawCreatures));
                int[] unk3ce = new int[3];
                for (int x = 0; x < unk3ce.length; x++) {
                    unk3ce[x] = Utils.readUnsignedShort(rawCreatures);
                }
                creature.setUnk3ce(unk3ce);
                creature.setSlapEffectId(Utils.readUnsignedShort(rawCreatures));
                creature.setDeathEffectId(Utils.readUnsignedShort(rawCreatures));
                creature.setUnkf40(Utils.readUnsignedShort(rawCreatures));
                short[] unk3d = new short[3];
                for (int x = 0; x < unk3d.length; x++) {
                    unk3d[x] = (short) rawCreatures.readUnsignedByte();
                }
                creature.setUnk3d(unk3d);
                creature.setUnkf45((short) rawCreatures.readUnsignedByte());
                short[] unk40 = new short[2];
                for (int x = 0; x < unk40.length; x++) {
                    unk40[x] = (short) rawCreatures.readUnsignedByte();
                }
                creature.setUnk40(unk40);
                short[] unkf48 = new short[3];
                for (int x = 0; x < unkf48.length; x++) {
                    unkf48[x] = (short) rawCreatures.readUnsignedByte();
                }
                creature.setUnkf48(unkf48);
                creature.setCreatureId((short) rawCreatures.readUnsignedByte());
                short[] unk3ea = new short[3];
                for (int x = 0; x < unk3ea.length; x++) {
                    unk3ea[x] = (short) rawCreatures.readUnsignedByte();
                }
                creature.setUnk3ea(unk3ea);
                creature.setUnhappyThreshold((short) rawCreatures.readUnsignedByte());
                short[] unk3eb = new short[2];
                for (int x = 0; x < unk3eb.length; x++) {
                    unk3eb[x] = (short) rawCreatures.readUnsignedByte();
                }
                creature.setUnk3eb(unk3eb);
                creature.setLairObjectId((short) rawCreatures.readUnsignedByte());
                short[] unk3f = new short[3];
                for (int x = 0; x < unk3f.length; x++) {
                    unk3f[x] = (short) rawCreatures.readUnsignedByte();
                }
                creature.setUnk3f(unk3f);
                bytes = new byte[32];
                rawCreatures.read(bytes);
                creature.setXname(Utils.bytesToString(bytes).trim());
                creature.setMaterial((short) rawCreatures.readUnsignedByte());
                creature.setReff77(readArtResource(rawCreatures));
                creature.setUnkfcb(Utils.readUnsignedShort(rawCreatures));
                creature.setUnk4(Utils.readUnsignedInteger(rawCreatures));
                creature.setRef3(readArtResource(rawCreatures));
                short[] unk5 = new short[2];
                for (int x = 0; x < unk5.length; x++) {
                    unk5[x] = (short) rawCreatures.readUnsignedByte();
                }
                creature.setUnk5(unk5);
                creature.setRef4(readArtResource(rawCreatures));
                creature.setUnk6(Utils.readUnsignedInteger(rawCreatures));
                creature.setTortureHpChange(Utils.readShort(rawCreatures));
                creature.setTortureMoodChange(Utils.readShort(rawCreatures));
                ArtResource[] ref5 = new ArtResource[6];
                for (int x = 0; x < ref5.length; x++) {
                    ref5[x] = readArtResource(rawCreatures);
                }
                creature.setRef5(ref5);
                Unk7[] unk7s = new Unk7[7];
                for (int x = 0; x < unk7s.length; x++) {
                    Unk7 unk7 = creature.new Unk7();
                    unk7.setX00(Utils.readUnsignedInteger(rawCreatures));
                    unk7.setX04(Utils.readUnsignedInteger(rawCreatures));
                    unk7.setX08(Utils.readUnsignedInteger(rawCreatures));
                    unk7s[x] = unk7;
                }
                creature.setUnk7(unk7s);
                creature.setRef6(readArtResource(rawCreatures));
                X1323[] x1323s = new X1323[48];
                for (int x = 0; x < x1323s.length; x++) {
                    X1323 x1323 = creature.new X1323();
                    x1323.setX00(Utils.readUnsignedShort(rawCreatures));
                    x1323.setX02(Utils.readUnsignedShort(rawCreatures));
                    x1323s[x] = x1323;
                }
                creature.setX1323(x1323s);
                ArtResource[] ref7 = new ArtResource[3];
                for (int x = 0; x < ref7.length; x++) {
                    ref7[x] = readArtResource(rawCreatures);
                }
                creature.setRef7(ref7);
                creature.setUnk14df(Utils.readUnsignedShort(rawCreatures));
                int[] x14e1 = new int[2];
                for (int x = 0; x < x14e1.length; x++) {
                    x14e1[x] = Utils.readUnsignedInteger(rawCreatures);
                }
                creature.setX14e1(x14e1);
                int[] x14e9 = new int[2];
                for (int x = 0; x < x14e9.length; x++) {
                    x14e9[x] = Utils.readUnsignedInteger(rawCreatures);
                }
                creature.setX14e9(x14e9);
                creature.setRef8(readArtResource(rawCreatures));
                creature.setUnk1545(Utils.readUnsignedInteger(rawCreatures));

                // Add to the hash by the creature ID
                creatures.put(creature.getCreatureId(), creature);
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to read the file " + creaturesFile + "!", e);
        }
    }

    /**
     * Reads and parses an Light object from the current file location
     *
     * @param file the file stream to parse from
     * @return a Light
     */
    private Light readLight(RandomAccessFile file) throws IOException {
        Light light = new Light();

        // Read the data
        light.setmKPos(new Vector3f(Utils.readInteger(file) / FIXED_POINT_DIVISION, Utils.readInteger(file) / FIXED_POINT_DIVISION, Utils.readInteger(file) / FIXED_POINT_DIVISION));
        light.setRadius(Utils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
        light.setFlags(Utils.readUnsignedInteger(file));
        light.setColor(new Color(file.readUnsignedByte(), file.readUnsignedByte(), file.readUnsignedByte(), file.readUnsignedByte()));

        return light;
    }

    /**
     * Reads the Objects.kwd
     *
     * @param dkIIPath path to DK II data files (for filling up the catalogs)
     * @throws RuntimeException reading may fail
     */
    private void readObjects(String dkIIPath) throws RuntimeException {

        // Read the objects catalog
        File objectsFile = new File(dkIIPath.concat("Data").concat(File.separator).concat("editor").concat(File.separator).concat("Objects.kwd"));
        try (RandomAccessFile rawObjects = new RandomAccessFile(objectsFile, "r")) {

            // Objects file has a 36 header
            rawObjects.seek(20);
            int objectsCount = Utils.readUnsignedInteger(rawObjects);

            // The objects file is just simple blocks until EOF
            rawObjects.seek(36); // End of header
            rawObjects.skipBytes(20); // I don't know what is in here

            objects = new HashMap<>(objectsCount);
            for (int i = 0; i < objectsCount; i++) {
                Object object = new Object();
                byte[] bytes = new byte[32];
                rawObjects.read(bytes);
                object.setName(Utils.bytesToString(bytes).trim());
                object.setMeshResource(readArtResource(rawObjects));
                object.setGuiIconResource(readArtResource(rawObjects));
                object.setInHandIconResource(readArtResource(rawObjects));
                object.setInHandMeshResource(readArtResource(rawObjects));
                object.setkUnknownResource(readArtResource(rawObjects));
                ArtResource[] additionalResources = new ArtResource[4];
                for (int x = 0; x < additionalResources.length; x++) {
                    additionalResources[x] = readArtResource(rawObjects);
                }
                object.setAdditionalResources(additionalResources);
                object.setLight(readLight(rawObjects));
                object.setWidth(Utils.readUnsignedInteger(rawObjects) / FIXED_POINT_DIVISION);
                object.setHeight(Utils.readUnsignedInteger(rawObjects) / FIXED_POINT_DIVISION);
                object.setMass(Utils.readUnsignedInteger(rawObjects) / FIXED_POINT_DIVISION);
                object.setUnknown1(Utils.readUnsignedInteger(rawObjects));
                object.setUnknown2(Utils.readUnsignedInteger(rawObjects));
                object.setMaterial((short) rawObjects.readUnsignedByte());
                short[] unknown3 = new short[3];
                for (int x = 0; x < unknown3.length; x++) {
                    unknown3[x] = (short) rawObjects.readUnsignedByte();
                }
                object.setUnknown3(unknown3);
                object.setFlags(Utils.readUnsignedInteger(rawObjects));
                object.setHp(Utils.readUnsignedShort(rawObjects));
                object.setUnknown4(Utils.readUnsignedShort(rawObjects));
                object.setX34c(Utils.readUnsignedShort(rawObjects));
                object.setX34e(Utils.readUnsignedShort(rawObjects));
                object.setX350(Utils.readUnsignedShort(rawObjects));
                object.setX352(Utils.readUnsignedShort(rawObjects));
                object.setSlapEffect(Utils.readUnsignedShort(rawObjects));
                object.setDeathEffect(Utils.readUnsignedShort(rawObjects));
                object.setUnknown5(Utils.readUnsignedShort(rawObjects));
                object.setObjectId((short) rawObjects.readUnsignedByte());
                object.setUnknown6((short) rawObjects.readUnsignedByte());
                object.setRoomCapacity((short) rawObjects.readUnsignedByte());
                object.setUnknown7((short) rawObjects.readUnsignedByte());
                bytes = new byte[32];
                rawObjects.read(bytes);
                object.setSoundCategory(Utils.bytesToString(bytes).trim());

                // Add to the hash by the object ID
                objects.put(object.getObjectId(), object);
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to read the file " + objectsFile + "!", e);
        }
    }

    /**
     * Reads the CreatureSpells.kwd
     *
     * @param dkIIPath path to DK II data files (for filling up the catalogs)
     * @throws RuntimeException reading may fail
     */
    private void readCreatureSpells(String dkIIPath) throws RuntimeException {

        // Read the creature spells catalog
        File creatureSpellsFile = new File(dkIIPath.concat("Data").concat(File.separator).concat("editor").concat(File.separator).concat("CreatureSpells.kwd"));
        try (RandomAccessFile rawCreatureSpells = new RandomAccessFile(creatureSpellsFile, "r")) {

            // Creature spells file has a 36 header
            rawCreatureSpells.seek(20);
            int creatureSpellsCount = Utils.readUnsignedInteger(rawCreatureSpells);

            // The creature spells file is just simple blocks until EOF
            rawCreatureSpells.seek(36); // End of header
            rawCreatureSpells.skipBytes(20); // I don't know what is in here

            creatureSpells = new ArrayList<>(creatureSpellsCount);
            for (int i = 0; i < creatureSpellsCount; i++) {
                CreatureSpell creatureSpell = new CreatureSpell();
                byte[] bytes = new byte[32];
                rawCreatureSpells.read(bytes);
                creatureSpell.setName(Utils.bytesToString(bytes).trim());
                short[] data = new short[234];
                for (int x = 0; x < data.length; x++) {
                    data[x] = (short) rawCreatureSpells.readUnsignedByte();
                }
                creatureSpell.setData(data);

                // Add to the list
                creatureSpells.add(creatureSpell);
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to read the file " + creatureSpellsFile + "!", e);
        }
    }

    /**
     * Reads the EffectElements.kwd
     *
     * @param dkIIPath path to DK II data files (for filling up the catalogs)
     * @throws RuntimeException reading may fail
     */
    private void readEffectElements(String dkIIPath) throws RuntimeException {

        // Read the effect elements catalog
        File effectElementsFile = new File(dkIIPath.concat("Data").concat(File.separator).concat("editor").concat(File.separator).concat("EffectElements.kwd"));
        try (RandomAccessFile raweffectElements = new RandomAccessFile(effectElementsFile, "r")) {

            // Effect elements file has a 36 header
            raweffectElements.seek(20);
            int effectElementsCount = Utils.readUnsignedInteger(raweffectElements);

            // The effect elements file is just simple blocks until EOF
            raweffectElements.seek(36); // End of header
            raweffectElements.skipBytes(20); // I don't know what is in here

            effectElements = new HashMap<>(effectElementsCount);
            for (int i = 0; i < effectElementsCount; i++) {
                EffectElement effectElement = new EffectElement();
                byte[] bytes = new byte[32];
                raweffectElements.read(bytes);
                effectElement.setName(Utils.bytesToString(bytes).trim());
                effectElement.setArtResource(readArtResource(raweffectElements));
                effectElement.setMass(Utils.readInteger(raweffectElements) / FIXED_POINT_DIVISION);
                effectElement.setAirFriction(Utils.readUnsignedInteger(raweffectElements) / FIXED_POINT5_DIVISION);
                effectElement.setElasticity(Utils.readUnsignedInteger(raweffectElements) / FIXED_POINT5_DIVISION);
                effectElement.setMinSpeedXy(Utils.readInteger(raweffectElements) / FIXED_POINT_DIVISION);
                effectElement.setMaxSpeedXy(Utils.readInteger(raweffectElements) / FIXED_POINT_DIVISION);
                effectElement.setMinSpeedYz(Utils.readInteger(raweffectElements) / FIXED_POINT_DIVISION);
                effectElement.setMaxSpeedYz(Utils.readInteger(raweffectElements) / FIXED_POINT_DIVISION);
                effectElement.setMinScale(Utils.readUnsignedInteger(raweffectElements) / FIXED_POINT_DIVISION);
                effectElement.setMaxScale(Utils.readUnsignedInteger(raweffectElements) / FIXED_POINT_DIVISION);
                effectElement.setScaleRatio(Utils.readUnsignedInteger(raweffectElements) / FIXED_POINT_DIVISION);
                effectElement.setFlags(Utils.readUnsignedInteger(raweffectElements));
                effectElement.setEffectElementId(Utils.readUnsignedShort(raweffectElements));
                effectElement.setMinHp(Utils.readUnsignedShort(raweffectElements));
                effectElement.setMaxHp(Utils.readUnsignedShort(raweffectElements));
                effectElement.setDeathElement(Utils.readUnsignedShort(raweffectElements));
                effectElement.setHitSolidElement(Utils.readUnsignedShort(raweffectElements));
                effectElement.setHitWaterElement(Utils.readUnsignedShort(raweffectElements));
                effectElement.setHitLavaElement(Utils.readUnsignedShort(raweffectElements));
                effectElement.setColor(new Color(raweffectElements.readUnsignedByte(), raweffectElements.readUnsignedByte(), raweffectElements.readUnsignedByte()));
                effectElement.setRandomColorIndex((short) raweffectElements.readUnsignedByte());
                effectElement.setTableColorIndex((short) raweffectElements.readUnsignedByte());
                effectElement.setFadePercentage((short) raweffectElements.readUnsignedByte());
                effectElement.setNextEffect(Utils.readUnsignedShort(raweffectElements));

                // Add to the hash by the effect element ID
                effectElements.put(effectElement.getEffectElementId(), effectElement);
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to read the file " + effectElementsFile + "!", e);
        }
    }

    /**
     * Reads the Effects.kwd
     *
     * @param dkIIPath path to DK II data files (for filling up the catalogs)
     * @throws RuntimeException reading may fail
     */
    private void readEffects(String dkIIPath) throws RuntimeException {

        // Read the effects catalog
        File effectsFile = new File(dkIIPath.concat("Data").concat(File.separator).concat("editor").concat(File.separator).concat("Effects.kwd"));
        try (RandomAccessFile rawEffects = new RandomAccessFile(effectsFile, "r")) {

            // Effects file has a 36 header
            rawEffects.seek(20);
            int effectsCount = Utils.readUnsignedInteger(rawEffects);

            // The effects file is just simple blocks until EOF
            rawEffects.seek(36); // End of header
            rawEffects.skipBytes(20); // I don't know what is in here

            effects = new HashMap<>(effectsCount);
            for (int i = 0; i < effectsCount; i++) {
                Effect effect = new Effect();
                byte[] bytes = new byte[32];
                rawEffects.read(bytes);
                effect.setName(Utils.bytesToString(bytes).trim());
                effect.setArtResource(readArtResource(rawEffects));
                effect.setLight(readLight(rawEffects));
                effect.setMass(Utils.readInteger(rawEffects) / FIXED_POINT_DIVISION);
                effect.setAirFriction(Utils.readUnsignedInteger(rawEffects) / FIXED_POINT5_DIVISION);
                effect.setElasticity(Utils.readUnsignedInteger(rawEffects) / FIXED_POINT5_DIVISION);
                effect.setRadius(Utils.readUnsignedInteger(rawEffects) / FIXED_POINT_DIVISION);
                effect.setMinSpeedXy(Utils.readInteger(rawEffects) / FIXED_POINT_DIVISION);
                effect.setMaxSpeedXy(Utils.readInteger(rawEffects) / FIXED_POINT_DIVISION);
                effect.setMinSpeedYz(Utils.readInteger(rawEffects) / FIXED_POINT_DIVISION);
                effect.setMaxSpeedYz(Utils.readInteger(rawEffects) / FIXED_POINT_DIVISION);
                effect.setMinScale(Utils.readUnsignedInteger(rawEffects) / FIXED_POINT_DIVISION);
                effect.setMaxScale(Utils.readUnsignedInteger(rawEffects) / FIXED_POINT_DIVISION);
                effect.setFlags(Utils.readUnsignedInteger(rawEffects));
                effect.setEffectId(Utils.readUnsignedShort(rawEffects));
                effect.setMinHp(Utils.readUnsignedShort(rawEffects));
                effect.setMaxHp(Utils.readUnsignedShort(rawEffects));
                effect.setFadeDuration(Utils.readUnsignedShort(rawEffects));
                effect.setNextEffect(Utils.readUnsignedShort(rawEffects));
                effect.setDeathEffect(Utils.readUnsignedShort(rawEffects));
                effect.setHitSolidEffect(Utils.readUnsignedShort(rawEffects));
                effect.setHitWaterEffect(Utils.readUnsignedShort(rawEffects));
                effect.setHitLavaEffect(Utils.readUnsignedShort(rawEffects));
                int[] generateIds = new int[8];
                for (int x = 0; x < generateIds.length; x++) {
                    generateIds[x] = Utils.readUnsignedShort(rawEffects);
                }
                effect.setGenerateIds(generateIds);
                effect.setOuterOriginRange(Utils.readUnsignedShort(rawEffects));
                effect.setLowerHeightLimit(Utils.readUnsignedShort(rawEffects));
                effect.setUpperHeightLimit(Utils.readUnsignedShort(rawEffects));
                effect.setOrientationRange(Utils.readUnsignedShort(rawEffects));
                effect.setSpriteSpinRateRange(Utils.readUnsignedShort(rawEffects));
                effect.setWhirlpoolRate(Utils.readUnsignedShort(rawEffects));
                effect.setDirectionalSpread(Utils.readUnsignedShort(rawEffects));
                effect.setCircularPathRate(Utils.readUnsignedShort(rawEffects));
                effect.setInnerOriginRange(Utils.readUnsignedShort(rawEffects));
                effect.setGenerateRandomness(Utils.readUnsignedShort(rawEffects));
                effect.setMisc2(Utils.readUnsignedShort(rawEffects));
                effect.setMisc3(Utils.readUnsignedShort(rawEffects));
                effect.setUnknown1((short) rawEffects.readUnsignedByte());
                effect.setElementsPerTurn((short) rawEffects.readUnsignedByte());
                effect.setUnknown3(Utils.readUnsignedShort(rawEffects));

                // Add to the hash by the effect ID
                effects.put(effect.getEffectId(), effect);
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to read the file " + effectsFile + "!", e);
        }
    }

    /**
     * Reads the KeeperSpells.kwd
     *
     * @param dkIIPath path to DK II data files (for filling up the catalogs)
     * @throws RuntimeException reading may fail
     */
    private void readKeeperSpells(String dkIIPath) throws RuntimeException {

        // Read the keeper spells catalog
        File keeperSpellsFile = new File(dkIIPath.concat("Data").concat(File.separator).concat("editor").concat(File.separator).concat("KeeperSpells.kwd"));
        try (RandomAccessFile rawKeeperSpells = new RandomAccessFile(keeperSpellsFile, "r")) {

            // Keeper spells file has a 36 header
            rawKeeperSpells.seek(20);
            int keeperSpellsCount = Utils.readUnsignedInteger(rawKeeperSpells);

            // The keeper spells file is just simple blocks until EOF
            rawKeeperSpells.seek(36); // End of header
            rawKeeperSpells.skipBytes(20); // I don't know what is in here

            keeperSpells = new HashMap<>(keeperSpellsCount);
            for (int i = 0; i < keeperSpellsCount; i++) {
                KeeperSpell keeperSpell = new KeeperSpell();
                byte[] bytes = new byte[32];
                rawKeeperSpells.read(bytes);
                keeperSpell.setName(Utils.bytesToString(bytes).trim());
                keeperSpell.setRef1(readArtResource(rawKeeperSpells));
                keeperSpell.setRef3(readArtResource(rawKeeperSpells));
                keeperSpell.setXc8(Utils.readInteger(rawKeeperSpells));
                keeperSpell.setXcc(Utils.readInteger(rawKeeperSpells));
                keeperSpell.setShotData1(Utils.readInteger(rawKeeperSpells));
                keeperSpell.setShotData2(Utils.readInteger(rawKeeperSpells));
                keeperSpell.setXd8(Utils.readUnsignedShort(rawKeeperSpells));
                keeperSpell.setXda((short) rawKeeperSpells.readUnsignedByte());
                keeperSpell.setXdb((short) rawKeeperSpells.readUnsignedByte());
                keeperSpell.setXdc(Utils.readInteger(rawKeeperSpells));
                keeperSpell.setXe0Unreferenced(Utils.readUnsignedShort(rawKeeperSpells));
                keeperSpell.setManaDrain(Utils.readUnsignedShort(rawKeeperSpells));
                keeperSpell.setXe4(Utils.readUnsignedShort(rawKeeperSpells));
                keeperSpell.setXe6(Utils.readUnsignedShort(rawKeeperSpells));
                keeperSpell.setXe8(Utils.readUnsignedShort(rawKeeperSpells));
                keeperSpell.setXea(Utils.readUnsignedShort(rawKeeperSpells));
                keeperSpell.setXec(Utils.readUnsignedShort(rawKeeperSpells));
                keeperSpell.setKeeperSpellId((short) rawKeeperSpells.readUnsignedByte());
                keeperSpell.setXef((short) rawKeeperSpells.readUnsignedByte());
                keeperSpell.setXf0((short) rawKeeperSpells.readUnsignedByte());
                bytes = new byte[32];
                rawKeeperSpells.read(bytes);
                keeperSpell.setYName(Utils.bytesToString(bytes).trim());
                keeperSpell.setBonusRTime(Utils.readUnsignedShort(rawKeeperSpells));
                keeperSpell.setBonusShotType((short) rawKeeperSpells.readUnsignedByte());
                keeperSpell.setBonusShotData1(Utils.readInteger(rawKeeperSpells));
                keeperSpell.setBonusShotData2(Utils.readInteger(rawKeeperSpells));
                keeperSpell.setManaCost(Utils.readInteger(rawKeeperSpells));
                keeperSpell.setRef2(readArtResource(rawKeeperSpells));
                bytes = new byte[32];
                rawKeeperSpells.read(bytes);
                keeperSpell.setXName(Utils.bytesToString(bytes).trim());
                keeperSpell.setX194((short) rawKeeperSpells.readUnsignedByte());
                keeperSpell.setX195((short) rawKeeperSpells.readUnsignedByte());

                // Add to the hash by the keeper spell ID
                keeperSpells.put(keeperSpell.getKeeperSpellId(), keeperSpell);
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to read the file " + keeperSpellsFile + "!", e);
        }
    }

    /**
     * Reads the *Things.kld
     *
     * @param file the original map KWD file
     * @throws RuntimeException reading may fail
     */
    private void readThingsFile(File file) throws RuntimeException {

        // Read the requested Things file
        File thingsFile = new File(file.toString().substring(0, file.toString().length() - 4).concat("Things.kld"));
        try (RandomAccessFile rawThings = new RandomAccessFile(thingsFile, "r")) {

            // Things file has a 36 header
            rawThings.seek(20);
            int thingsCount = Utils.readUnsignedInteger(rawThings);

            // The things file is just simple blocks until EOF
            rawThings.seek(36); // End of header
            rawThings.skipBytes(20); // I don't know what is in here

            things = new ArrayList<>(thingsCount);
            for (int i = 0; i < thingsCount; i++) {
                Thing thing = new Thing() {
                };
                int[] thingTag = new int[2];
                for (int x = 0; x < thingTag.length; x++) {
                    thingTag[x] = Utils.readUnsignedInteger(rawThings);
                }

                // Figure out the type
                switch (thingTag[0]) {
                    case 194: {

                        // Thing06
                        rawThings.skipBytes(thingTag[1]);
                        break;
                    }
                    case 195: {

                        // Thing05
                        rawThings.skipBytes(thingTag[1]);
                        break;
                    }
                    case 196: {

                        // Thing04
                        rawThings.skipBytes(thingTag[1]);
                        break;
                    }
                    case 197: {

                        // ActionPoint
                        thing = thing.new ActionPoint();
                        ((ActionPoint) thing).setX00(Utils.readInteger(rawThings));
                        ((ActionPoint) thing).setX04(Utils.readInteger(rawThings));
                        ((ActionPoint) thing).setX08(Utils.readInteger(rawThings));
                        ((ActionPoint) thing).setX0c(Utils.readInteger(rawThings));
                        ((ActionPoint) thing).setX10(Utils.readInteger(rawThings));
                        ((ActionPoint) thing).setX14(Utils.readUnsignedShort(rawThings));
                        ((ActionPoint) thing).setId((short) rawThings.readUnsignedByte());
                        ((ActionPoint) thing).setX17((short) rawThings.readUnsignedByte());
                        byte[] bytes = new byte[32];
                        rawThings.read(bytes);
                        ((ActionPoint) thing).setName(Utils.bytesToString(bytes).trim());
                        break;
                    }
                    case 198: {

                        // Thing01
                        rawThings.skipBytes(thingTag[1]);
                        break;
                    }
                    case 199: {

                        // Thing02
                        rawThings.skipBytes(thingTag[1]);
                        break;
                    }
                    case 200: {

                        // Thing03
                        thing = thing.new Thing03();
                        ((Thing03) thing).setPos(new Vector3f(Utils.readInteger(rawThings) / FIXED_POINT_DIVISION, Utils.readInteger(rawThings) / FIXED_POINT_DIVISION, Utils.readInteger(rawThings) / FIXED_POINT_DIVISION));
                        ((Thing03) thing).setX0c(Utils.readUnsignedShort(rawThings));
                        ((Thing03) thing).setX0e((short) rawThings.readUnsignedByte());
                        ((Thing03) thing).setX0f((short) rawThings.readUnsignedByte());
                        ((Thing03) thing).setX10(Utils.readInteger(rawThings));
                        ((Thing03) thing).setX14(Utils.readInteger(rawThings));
                        ((Thing03) thing).setX18(Utils.readUnsignedShort(rawThings));
                        ((Thing03) thing).setId((short) rawThings.readUnsignedByte());
                        ((Thing03) thing).setX1b((short) rawThings.readUnsignedByte());
                        break;
                    }
                    case 201: {

                        // Thing08 -- not tested
                        thing = thing.new Thing08();
                        byte[] bytes = new byte[32];
                        rawThings.read(bytes);
                        ((Thing08) thing).setName(Utils.bytesToString(bytes).trim());
                        ((Thing08) thing).setX20(Utils.readUnsignedShort(rawThings));
                        ((Thing08) thing).setX22((short) rawThings.readUnsignedByte());
                        ((Thing08) thing).setX23(Utils.readInteger(rawThings));
                        ((Thing08) thing).setX27(Utils.readInteger(rawThings));
                        HeroPartyData[] x2b = new HeroPartyData[16];
                        for (int x = 0; x < x2b.length; x++) {
                            HeroPartyData heroPartyData = ((Thing08) thing).new HeroPartyData();
                            heroPartyData.setX00(Utils.readInteger(rawThings));
                            heroPartyData.setX04(Utils.readInteger(rawThings));
                            heroPartyData.setX08(Utils.readInteger(rawThings));
                            heroPartyData.setGoldHeld(Utils.readUnsignedShort(rawThings));
                            heroPartyData.setX0e((short) rawThings.readUnsignedByte());
                            heroPartyData.setX0f((short) rawThings.readUnsignedByte());
                            heroPartyData.setX10(Utils.readInteger(rawThings));
                            heroPartyData.setInitialHealth(Utils.readInteger(rawThings));
                            heroPartyData.setX18(Utils.readUnsignedShort(rawThings));
                            heroPartyData.setX1a((short) rawThings.readUnsignedByte());
                            heroPartyData.setX1b((short) rawThings.readUnsignedByte());
                            heroPartyData.setX1c((short) rawThings.readUnsignedByte());
                            heroPartyData.setX1d((short) rawThings.readUnsignedByte());
                            heroPartyData.setX1e((short) rawThings.readUnsignedByte());
                            heroPartyData.setX1f((short) rawThings.readUnsignedByte());
                            x2b[x] = heroPartyData;
                        }
                        ((Thing08) thing).setX2b(x2b);
                        break;
                    }
                    case 203: {

                        // Thing10 -- not tested
                        thing = thing.new Thing10();
                        ((Thing10) thing).setX00(Utils.readInteger(rawThings));
                        ((Thing10) thing).setX04(Utils.readInteger(rawThings));
                        ((Thing10) thing).setX08(Utils.readInteger(rawThings));
                        ((Thing10) thing).setX0c(Utils.readInteger(rawThings));
                        ((Thing10) thing).setX10(Utils.readUnsignedShort(rawThings));
                        ((Thing10) thing).setX12(Utils.readUnsignedShort(rawThings));
                        int[] x14 = new int[4];
                        for (int x = 0; x < x14.length; x++) {
                            x14[x] = Utils.readUnsignedShort(rawThings);
                        }
                        ((Thing10) thing).setX14(x14);
                        ((Thing10) thing).setX1c((short) rawThings.readUnsignedByte());
                        ((Thing10) thing).setX1d((short) rawThings.readUnsignedByte());
                        short[] pad = new short[6];
                        for (int x = 0; x < pad.length; x++) {
                            pad[x] = (short) rawThings.readUnsignedByte();
                        }
                        ((Thing10) thing).setPad(pad);
                        break;
                    }
                    case 204: {

                        // Thing11
                        thing = thing.new Thing11();
                        ((Thing11) thing).setX00(Utils.readInteger(rawThings));
                        ((Thing11) thing).setX04(Utils.readInteger(rawThings));
                        ((Thing11) thing).setX08(Utils.readInteger(rawThings));
                        ((Thing11) thing).setX0c(Utils.readUnsignedShort(rawThings));
                        ((Thing11) thing).setX0e((short) rawThings.readUnsignedByte());
                        ((Thing11) thing).setX0f((short) rawThings.readUnsignedByte());
                        ((Thing11) thing).setX10(Utils.readUnsignedShort(rawThings));
                        ((Thing11) thing).setX12((short) rawThings.readUnsignedByte());
                        ((Thing11) thing).setX13((short) rawThings.readUnsignedByte());
                        break;
                    }
                    case 205: {

                        // Thing12 -- not tested
                        thing = thing.new Thing12();
                        ((Thing12) thing).setX00(new Vector3f(Utils.readInteger(rawThings) / FIXED_POINT_DIVISION, Utils.readInteger(rawThings) / FIXED_POINT_DIVISION, Utils.readInteger(rawThings) / FIXED_POINT_DIVISION));
                        ((Thing12) thing).setX0c(new Vector3f(Utils.readInteger(rawThings) / FIXED_POINT_DIVISION, Utils.readInteger(rawThings) / FIXED_POINT_DIVISION, Utils.readInteger(rawThings) / FIXED_POINT_DIVISION));
                        ((Thing12) thing).setX18(new Vector3f(Utils.readInteger(rawThings) / FIXED_POINT_DIVISION, Utils.readInteger(rawThings) / FIXED_POINT_DIVISION, Utils.readInteger(rawThings) / FIXED_POINT_DIVISION));
                        ((Thing12) thing).setX24(Utils.readInteger(rawThings));
                        ((Thing12) thing).setX28(Utils.readInteger(rawThings));
                        ((Thing12) thing).setX2c(Utils.readInteger(rawThings));
                        ((Thing12) thing).setX30(Utils.readInteger(rawThings));
                        ((Thing12) thing).setX34(Utils.readInteger(rawThings));
                        ((Thing12) thing).setX38(Utils.readInteger(rawThings));
                        ((Thing12) thing).setX3c(Utils.readInteger(rawThings));
                        ((Thing12) thing).setX40(Utils.readInteger(rawThings));
                        ((Thing12) thing).setX44(Utils.readInteger(rawThings));
                        ((Thing12) thing).setX48(Utils.readInteger(rawThings));
                        ((Thing12) thing).setX4c(Utils.readUnsignedShort(rawThings));
                        ((Thing12) thing).setX4e(Utils.readUnsignedShort(rawThings));
                        ((Thing12) thing).setX50(Utils.readUnsignedShort(rawThings));
                        ((Thing12) thing).setX52((short) rawThings.readUnsignedByte());
                        break;
                    }
                    default: {

                        // Just skip the bytes
                        rawThings.skipBytes(thingTag[1]);
                    }
                }

                System.out.println(thingTag[0] + " type");

                // Add to the list
                things.add(thing);
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to read the file " + thingsFile + "!", e);
        }
    }

    /**
     * Reads the Shots.kwd
     *
     * @param dkIIPath path to DK II data files (for filling up the catalogs)
     * @throws RuntimeException reading may fail
     */
    private void readShots(String dkIIPath) throws RuntimeException {

        // Read the shots catalog
        File shotsFile = new File(dkIIPath.concat("Data").concat(File.separator).concat("editor").concat(File.separator).concat("Shots.kwd"));
        try (RandomAccessFile rawShots = new RandomAccessFile(shotsFile, "r")) {

            // Shots file has a 36 header
            rawShots.seek(20);
            int shotsCount = Utils.readUnsignedInteger(rawShots);

            // The shots file is just simple blocks until EOF
            rawShots.seek(36); // End of header
            rawShots.skipBytes(20); // I don't know what is in here

            shots = new HashMap<>(shotsCount);
            for (int i = 0; i < shotsCount; i++) {

                // One shot is 239 bytes
                Shot shot = new Shot();
                byte[] bytes = new byte[32];
                rawShots.read(bytes);
                shot.setName(Utils.bytesToString(bytes).trim());

                // The ID is probably a uint8 @ 190
                rawShots.skipBytes(158);
                shot.setShotId((short) rawShots.readUnsignedByte());
                rawShots.skipBytes(48);

                // Add to the hash by the shot ID
                shots.put(shot.getShotId(), shot);
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to read the file " + shotsFile + "!", e);
        }
    }
}
