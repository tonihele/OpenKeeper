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
package toniarts.openkeeper.tools.convert.map;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.Vector3f;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.IFlagEnum;
import toniarts.openkeeper.tools.convert.IValueEnum;
import toniarts.openkeeper.tools.convert.map.ArtResource.Animation;
import toniarts.openkeeper.tools.convert.map.ArtResource.Image;
import toniarts.openkeeper.tools.convert.map.ArtResource.Mesh;
import toniarts.openkeeper.tools.convert.map.ArtResource.Proc;
import toniarts.openkeeper.tools.convert.map.ArtResource.ResourceType;
import toniarts.openkeeper.tools.convert.map.ArtResource.TerrainResource;
import toniarts.openkeeper.tools.convert.map.Creature.Attraction;
import toniarts.openkeeper.tools.convert.map.Creature.JobAlternative;
import toniarts.openkeeper.tools.convert.map.Creature.Spell;
import toniarts.openkeeper.tools.convert.map.Creature.Unk7;
import toniarts.openkeeper.tools.convert.map.Creature.X1323;
import toniarts.openkeeper.tools.convert.map.Creature.Xe94;
import toniarts.openkeeper.tools.convert.map.Door.DoorFlag;
import static toniarts.openkeeper.tools.convert.map.MapDataTypeEnum.CREATURES;
import static toniarts.openkeeper.tools.convert.map.MapDataTypeEnum.CREATURE_SPELLS;
import static toniarts.openkeeper.tools.convert.map.MapDataTypeEnum.DOORS;
import static toniarts.openkeeper.tools.convert.map.MapDataTypeEnum.EFFECTS;
import static toniarts.openkeeper.tools.convert.map.MapDataTypeEnum.EFFECT_ELEMENTS;
import static toniarts.openkeeper.tools.convert.map.MapDataTypeEnum.KEEPER_SPELLS;
import static toniarts.openkeeper.tools.convert.map.MapDataTypeEnum.MAP;
import static toniarts.openkeeper.tools.convert.map.MapDataTypeEnum.OBJECTS;
import static toniarts.openkeeper.tools.convert.map.MapDataTypeEnum.PLAYERS;
import static toniarts.openkeeper.tools.convert.map.MapDataTypeEnum.ROOMS;
import static toniarts.openkeeper.tools.convert.map.MapDataTypeEnum.SHOTS;
import static toniarts.openkeeper.tools.convert.map.MapDataTypeEnum.TERRAIN;
import static toniarts.openkeeper.tools.convert.map.MapDataTypeEnum.THINGS;
import static toniarts.openkeeper.tools.convert.map.MapDataTypeEnum.TRAPS;
import static toniarts.openkeeper.tools.convert.map.MapDataTypeEnum.TRIGGERS;
import static toniarts.openkeeper.tools.convert.map.MapDataTypeEnum.VARIABLES;
import toniarts.openkeeper.tools.convert.map.Object;
import toniarts.openkeeper.tools.convert.map.Thing.ActionPoint;
import toniarts.openkeeper.tools.convert.map.Thing.ActionPoint.ActionPointFlag;
import toniarts.openkeeper.tools.convert.map.Thing.GoodCreature;
import toniarts.openkeeper.tools.convert.map.Thing.HeroParty;
import toniarts.openkeeper.tools.convert.map.Thing.KeeperCreature;
import toniarts.openkeeper.tools.convert.map.Thing.NeutralCreature;
import toniarts.openkeeper.tools.convert.map.Thing.Thing12;

/**
 * Reads a DK II map file, the KWD is the file name of the main map identifier,
 * reads the KLDs actually<br>
 * The files are LITTLE ENDIAN I might say<br>
 * Some values are 3D coordinates or scale values presented in fixed point
 * integers. They are automatically converted to floats (divided by 2^12 = 4096
 * or 2^16 = 65536)<br>
 * Many parts adapted from C code by:
 * <li>George Gensure (werkt)</li>
 * And another C code implementation by:
 * <li>Thomasz Lis</li>
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class KwdFile {

    public enum LevFlag implements IFlagEnum {

        //UNKNOWN(0x0004), // unknown; always on in maps
        ALWAYS_IMPRISON_ENEMIES(0x0008), // Always imprison enemies
        ONE_SHOT_HORNY(0x0010), // Set if one shot Horny spell is available
        IS_SECRET_LEVEL(0x0020), // The map is Secret level
        IS_SPECIAL_LEVEL(0x0040), // The map is Special level
        SHOW_HERO_KILLS(0x0080), // Display "Heroes killed" tally
        AUTO_OBJECTIVE_BOX(0x0100), // Automatic show objective box
        HEART_MAKES_GEM(0x0200), // Last heart generates Portal Gem
        IS_MULTIPLAYER_LEVEL(0x0400), // The map is Multiplayer level
        IS_SKIRMISH_LEVEL(0x0800), // The map is Skirmish level
        FREEZE_OPTIONS(0x1000), // Freeze game options
        IS_MY_PET_DUNGEON_LEVEL(0x2000); // The map is My Pet Dungeon level
        private final long flagValue;

        private LevFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    };

    public enum TextTable implements IValueEnum {

        NONE(0),
        LEVEL_1(1),
        LEVEL_2(2),
        LEVEL_3(3),
        LEVEL_4(4),
        LEVEL_5(5),
        LEVEL_6A(6),
        LEVEL_6B(7),
        LEVEL_7(8),
        LEVEL_8(9),
        LEVEL_9(10),
        LEVEL_10(11),
        LEVEL_11A(12),
        LEVEL_11B(13),
        LEVEL_11C(14),
        LEVEL_12(15),
        LEVEL_13(16),
        LEVEL_14(17),
        LEVEL_15A(18),
        LEVEL_15B(19),
        LEVEL_16(20),
        LEVEL_17(21),
        LEVEL_18(22),
        LEVEL_19(23),
        LEVEL_20(24),
        MULTI_PLAYER_1(25),
        MY_PET_DUNGEON_1(26),
        SECRET_1(27),
        SECRET_2(28),
        SECRET_3(29),
        SECRET_4(30),
        SECRET_5(31),
        DEMO_1(32),
        DEMO_2(33),
        DEMO_3(34),
        MY_PET_DUNGEON_2(35),
        MY_PET_DUNGEON_3(36),
        MY_PET_DUNGEON_4(37),
        MY_PET_DUNGEON_5(38),
        MY_PET_DUNGEON_6(39),
        MULTI_PLAYER_2(41),
        MULTI_PLAYER_3(42),
        MULTI_PLAYER_4(43),
        MULTI_PLAYER_5(44),
        MULTI_PLAYER_6(45),
        MULTI_PLAYER_7(46),
        MULTI_PLAYER_8(47),
        MULTI_PLAYER_9(48),
        MULTI_PLAYER_10(49),
        MULTI_PLAYER_11(50),
        MULTI_PLAYER_12(51),
        MULTI_PLAYER_13(52),
        MULTI_PLAYER_14(53),
        MULTI_PLAYER_15(54),
        MULTI_PLAYER_16(55),
        MULTI_PLAYER_17(56),
        MULTI_PLAYER_18(57),
        MULTI_PLAYER_19(58),
        MULTI_PLAYER_20(59);

        private TextTable(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    public enum LevelReward implements IValueEnum {

        NONE(0),
        ALARM(1),
        BARRICADE(2),
        BOULDER(3),
        BRACED(4),
        BRIDGE_STONE(5),
        BRIDGE_WOODEN(6),
        CALL_TO_ARMS(7),
        CASINO(8),
        DARK_LIBRARY(12),
        FEAR(14),
        FIREBURST(15),
        FREEZE(16),
        GAS(17),
        GRAVEYARD(18),
        GUARD_ROOM(19),
        GUARD_POST(20),
        HATCHERY(21),
        LAIR(24),
        LIGHTNING(25),
        MAGIC_DOOR(26),
        PIT(27),
        PRISON(29),
        REAPER_TALISMAN_4(30),
        SECRET_DOOR(32),
        SENTRY(33),
        SPIKE(35),
        STEEL_DOOR(36),
        TEMPLE(39),
        TORTURE(41),
        TRAINING(42),
        TREASURY(43),
        TRIGGER(44),
        WOOD_DOOR(47),
        WORK_SHOP(48),
        REAPER_TALISMAN_1(49),
        REAPER_TALISMAN_2(50),
        REAPER_TALISMAN_3(51);

        private LevelReward(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }
    private static final float FIXED_POINT_DIVISION = 4096f;
    private static final float FIXED_POINT5_DIVISION = 65536f;
    // KWD data
//    struct LevelInfoBlock {
//        ucs2le_t m_wsName[64]; /* 134 */
//        ucs2le_t m_wsDescription[1024]; /* 1b4 */
//        ucs2le_t m_wsAuthor[64]; /* 9b4 */
//        ucs2le_t m_wsEmail[64]; /* a34 */
//        ucs2le_t m_wsInformation[1024]; /* ab4 */
//        uint16_t m_wShortId0;
//        uint16_t m_wShortId1;
//        uint8_t x01184[520];
//        ucs2le_t m_wsUnknown0138c[20][512];
//        uint16_t x0638c;
//        char x0638e[32];
//        uint8_t x063ae;
//        uint8_t x063af[4];
//        uint8_t x063b3[4];
//        uint8_t x063b7;
//        uint8_t x063b8;
//        uint16_t x063b9;
//        uint16_t x063bb;
//        uint16_t x063bd;
//        uint16_t x063bf;
//        uint16_t x063c3;
//        uint16_t x063c5;
//        uint16_t x063c7;
//        uint16_t x063c9;
//        uint16_t x063ca;
//        uint8_t x063cb[8];
//        uint16_t x063d3[8];
//        char x063e3[32];
//        uint8_t x06403;
//        uint8_t x06404;
//        uint8_t x06405;
//        uint8_t x06406;
//        uint16_t x06407;
//        uint16_t x06409[5];
//        ucs2le_t x06413[32];
//        };
    private String name;
    private String description;
    private String author;
    private String email;
    private String information;
    private int triggerId; // Associated trigger
    private int ticksPerSec;
    private short x01184[];
    private List<String> messages;
    private EnumSet<LevFlag> lvlFlags;
    private String speechStr;
    private short talismanPieces;
    private List<LevelReward> rewardPrev;
    private List<LevelReward> rewardNext;
    private short soundTrack;
    private TextTable textTableId;
    private int textTitleId;
    private int textPlotId;
    private int textDebriefId;
    private int textObjectvId;
    private int x063c3; //this may be first text_subobjctv_id - not sure
    private int textSubobjctvId1;
    private int textSubobjctvId2;
    private int textSubobjctvId3;
    private int speclvlIdx;
    private java.util.Map<Short, Integer> introductionOverrideTextIds; // Creature ID, TextID
    private String terrainPath;
    private short oneShotHornyLev;
    private short playerCount;
    private short x06405; // rewardPrev[4]??
    private short x06406; // rewardNext[4]??
    private int speechHornyId;
    private int speechPrelvlId;
    private int speechPostlvlWin;
    private int speechPostlvlLost;
    private int speechPostlvlNews;
    private int speechPrelvlGenr;
    private String heroName;
    //
    private Date timestamp1; // Seem to be the same these two timeStamps, maybe checks?
    private Date timestamp2;
    private List<FilePath> paths;
    private int unknown[];
    //
    private Map map;
    private int width;
    private int height;
    private java.util.Map<Short, Player> players;
    private java.util.Map<Short, Terrain> terrainTiles;
    private java.util.Map<Short, Door> doors;
    private java.util.Map<Short, Trap> traps;
    private java.util.Map<Short, Room> rooms;
    private java.util.Map<Short, Room> roomsByTerrainId; // Maps have rooms by the terrain ID
    private java.util.Map<Short, Creature> creatures;
    private java.util.Map<Short, Object> objects;
    private java.util.Map<Short, CreatureSpell> creatureSpells;
    private java.util.Map<Integer, EffectElement> effectElements;
    private java.util.Map<Integer, Effect> effects;
    private java.util.Map<Short, KeeperSpell> keeperSpells;
    private List<Thing> things;
    private java.util.Map<Short, Shot> shots;
    private java.util.Map<Integer, Trigger> triggers;
    private List<Variable> variables;
    private Terrain water;
    private Terrain lava;
    private Terrain claimedPath;
    private boolean customOverrides = false;
    private boolean loaded = false;
    private final String basePath;
    private FilePath mapPath;
    //
    private static final Logger logger = Logger.getLogger(KwdFile.class.getName());
    /**
     * Somehow reading a global overrided file some of the items are not
     * correctly sized, but they seem to load ok<br>
     * It is not empty padding, it is data, but what kind, I don't know
     */
    private static final java.util.Map<MapDataTypeEnum, List<Long>> ITEM_SIZES = new HashMap<>(MapDataTypeEnum.values().length);

    static {
        ITEM_SIZES.put(MapDataTypeEnum.CREATURES, Arrays.asList(5449l, 5537l));
        ITEM_SIZES.put(MapDataTypeEnum.CREATURE_SPELLS, Arrays.asList(266l));
        ITEM_SIZES.put(MapDataTypeEnum.DOORS, Arrays.asList(616l));
        ITEM_SIZES.put(MapDataTypeEnum.EFFECTS, Arrays.asList(246l));
        ITEM_SIZES.put(MapDataTypeEnum.EFFECT_ELEMENTS, Arrays.asList(182l));
        ITEM_SIZES.put(MapDataTypeEnum.KEEPER_SPELLS, Arrays.asList(406l));
        ITEM_SIZES.put(MapDataTypeEnum.OBJECTS, Arrays.asList(894l));
        ITEM_SIZES.put(MapDataTypeEnum.PLAYERS, Arrays.asList(205l));
        ITEM_SIZES.put(MapDataTypeEnum.ROOMS, Arrays.asList(1055l));
        ITEM_SIZES.put(MapDataTypeEnum.SHOTS, Arrays.asList(239l));
        ITEM_SIZES.put(MapDataTypeEnum.TERRAIN, Arrays.asList(552l));
        ITEM_SIZES.put(MapDataTypeEnum.TRAPS, Arrays.asList(579l));
    }

    /**
     * Constructs a new KWD file reader<br>
     * Reads the whole map and its catalogs (either standard ones or custom
     * ones)
     *
     * @param basePath path to DK II main path (or where ever is the "root")
     * @param file the KWD file to read
     */
    public KwdFile(String basePath, File file) {
        this(basePath, file, true);
    }

    /**
     * Constructs a new KWD file reader<br>
     *
     * @param basePath path to DK II main path (or where ever is the "root")
     * @param file the KWD file to read
     * @param load whether to actually load the map data, or just get the
     * general info
     */
    public KwdFile(String basePath, File file, boolean load) {

        // Load the actual main map info (paths to catalogs most importantly)
        readMapInfo(file);
        if (!basePath.endsWith(File.separator)) {
            basePath = basePath.concat(File.separator);
        }
        this.basePath = basePath;

        // See if we need to load the actual data
        if (load) {
            load();
        } else {

            // We need map width & height, I couldn't figure out where, except the map data
            try (RandomAccessFile data = new RandomAccessFile(ConversionUtils.getRealFileName(basePath, mapPath.getPath()), "r")) {
                KwdHeader header = readKwdHeader(data);
                width = header.getWidth();
                height = header.getHeight();
            } catch (Exception e) {

                //Fug
                throw new RuntimeException("Failed to read the file " + mapPath.getPath() + "!", e);
            }
        }
    }

    /**
     * Loads the map data
     *
     * @throws RuntimeException level file fails to parse
     */
    public void load() throws RuntimeException {
        if (!loaded) {

            // Now we have the paths, read all of those in order
            for (FilePath path : paths) {

                // Open the file
                try (RandomAccessFile data = new RandomAccessFile(ConversionUtils.getRealFileName(basePath, path.getPath()), "r")) {

                    // Read the file until EOF, normally it is one data type per file, but with Globals, it is all in the same file
                    do {

                        // Read header (and put the file pointer to the data start)
                        KwdHeader header = readKwdHeader(data);
                        readFileContents(header, data);

                        // Only loop with Globals
                    } while ((data.getFilePointer() <= data.length() && path.getId() == MapDataTypeEnum.GLOBALS));

                } catch (Exception e) {

                    //Fug
                    throw new RuntimeException("Failed to read the file " + path.getPath() + "!", e);
                }
            }

            // Hmm, seems that normal maps don't refer the effects nor effect elements
            List<String> unreadFilePaths = new ArrayList<>();
            if (effects == null) {
                unreadFilePaths.add(("Data").concat(File.separator).concat("editor").concat(File.separator).concat("Effects.kwd"));
            }
            if (effectElements == null) {
                unreadFilePaths.add(("Data").concat(File.separator).concat("editor").concat(File.separator).concat("EffectElements.kwd"));
            }

            // Loop through the unprocessed files
            for (String filePath : unreadFilePaths) {
                try (RandomAccessFile data = new RandomAccessFile(ConversionUtils.getRealFileName(basePath, filePath), "r")) {

                    // Read header (and put the file pointer to the data start)
                    KwdHeader header = readKwdHeader(data);
                    readFileContents(header, data);
                } catch (Exception e) {

                    //Fug
                    throw new RuntimeException("Failed to read the file " + filePath + "!", e);
                }
            }

            loaded = true;
        }
    }

    /**
     * Reads the common KWD header
     *
     * @param data the data file
     * @return the header
     * @throws IOException may fail reading
     */
    private KwdHeader readKwdHeader(RandomAccessFile data) throws IOException {

        //Mark the position
        long offset = data.getFilePointer();

        KwdHeader header = new KwdHeader();
        header.setId(ConversionUtils.parseEnum(ConversionUtils.readUnsignedInteger(data), MapDataTypeEnum.class));
        int size = ConversionUtils.readUnsignedInteger(data); // Bytes in the real size indicator, well seems to be 4 always
        byte[] bytes = new byte[size];
        data.read(bytes);
        if (size == 2) {
            header.setSize(ConversionUtils.readUnsignedShort(bytes));
        } else if (size == 4) {
            header.setSize(ConversionUtils.readUnsignedInteger(bytes));
        }

        // Handle few special cases, always rewind the file to data start
        switch (header.getId()) {
            case MAP: {

                // Width & height
                data.seek(offset + 20);
                header.setWidth(ConversionUtils.readUnsignedInteger(data));
                header.setHeight(ConversionUtils.readUnsignedInteger(data));

                // Seek to start, starts straight after 36 byte header
                data.seek(offset + 36);
                header.setHeaderSize(36);
                break;
            }
            case TRIGGERS: {

                // A bit special, item count is dw08 + x0c[0]
                data.seek(offset + 20);
                header.setItemCount(ConversionUtils.readUnsignedInteger(data) + ConversionUtils.readUnsignedInteger(data));

                // Seek to start (40 byte header + 20 bytes of something)
                data.seek(offset + 60);
                header.setHeaderSize(60);
                break;
            }
            default: {

                // Item count
                data.seek(offset + 20);
                header.setItemCount(ConversionUtils.readUnsignedInteger(data));

                // Seek to start (36 byte header + 20 bytes of something)
                data.seek(offset + 56);
            }
        }

        return header;
    }

    private void readFileContents(KwdHeader header, RandomAccessFile data) throws IOException {

        // Check the item size (just log)
        List<Long> wantedItemSize = ITEM_SIZES.get(header.getId());
        if (wantedItemSize != null) {
            if (!wantedItemSize.contains(header.getItemSize())) {
                logger.log(Level.WARNING, "{0} item size is {1} and it should be something of the following {2}!", new java.lang.Object[]{header.getId(), header.getItemSize(), wantedItemSize});
            }
        }

        // Handle all the cases (we kinda skip the globals with this logic, so no need)
        // All readers must read the whole data they intend to read
        switch (header.getId()) {
            case CREATURES: {
                readCreatures(header, data);
                break;
            }
            case CREATURE_SPELLS: {
                readCreatureSpells(header, data);
                break;
            }
            case DOORS: {
                readDoors(header, data);
                break;
            }
            case EFFECTS: {

                // Hmm, seem not to be referenced on normal maps
                readEffects(header, data);
                break;
            }
            case EFFECT_ELEMENTS: {

                // Hmm, seem not to be referenced on normal maps
                readEffectElements(header, data);
                break;
            }
            case KEEPER_SPELLS: {
                readKeeperSpells(header, data);
                break;
            }
            case MAP: {
                readMap(header, data);
                break;
            }
            case OBJECTS: {
                readObjects(header, data);
                break;
            }
            case PLAYERS: {
                readPlayers(header, data);
                break;
            }
            case ROOMS: {
                readRooms(header, data);
                break;
            }
            case SHOTS: {
                readShots(header, data);
                break;
            }
            case TERRAIN: {
                readTerrain(header, data);
                break;
            }
            case THINGS: {
                readThings(header, data);
                break;
            }
            case TRAPS: {
                readTraps(header, data);
                break;
            }
            case TRIGGERS: {
                readTriggers(header, data);
                break;
            }
            case VARIABLES: {

                // The global variables is not read to its full extend...
                readVariables(header, data);
                break;
            }
        }
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
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readMap(KwdHeader header, RandomAccessFile file) throws IOException {

        // Read the requested MAP file
        logger.info("Reading map!");
        width = header.getWidth();
        height = header.getHeight();
        map = new Map(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = new Tile();
                tile.setTerrainId((short) file.readUnsignedByte());
                tile.setPlayerId((short) file.readUnsignedByte());
                tile.setFlag(ConversionUtils.parseEnum(file.readUnsignedByte(), Tile.BridgeTerrainType.class));
                tile.setUnknown((short) file.readUnsignedByte());
                map.setTile(x, y, tile);
            }
        }
    }

    /**
     * Reads the *Players.kld
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readPlayers(KwdHeader header, RandomAccessFile file) throws IOException {

        // Read the requested PLAYER file
        logger.info("Reading players!");
        players = new HashMap<>(header.getItemCount());
        for (int playerIndex = 0; playerIndex < header.getItemCount(); playerIndex++) {
            long offset = file.getFilePointer();
            Player player = new Player();
            player.setStartingGold(ConversionUtils.readInteger(file));
            player.setAi(ConversionUtils.readInteger(file) == 1);
            player.setAiType(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Player.AIType.class));
            player.setSpeed((short) file.readUnsignedByte());
            player.setOpenness((short) file.readUnsignedByte());
            player.setRemoveCallToArmsIfTotalCreaturesLessThan((short) file.readUnsignedByte());
            player.setBuildLostRoomAfterSeconds((short) file.readUnsignedByte());
            short[] unknown1 = new short[3];
            for (int i = 0; i < unknown1.length; i++) {
                unknown1[i] = (short) file.readUnsignedByte();
            }
            player.setUnknown1(unknown1);
            player.setCreateEmptyAreasWhenIdle(ConversionUtils.readInteger(file) == 1);
            player.setBuildBiggerLairAfterClaimingPortal(ConversionUtils.readInteger(file) == 1);
            player.setSellCapturedRoomsIfLowOnGold(ConversionUtils.readInteger(file) == 1);
            player.setMinTimeBeforePlacingResearchedRoom((short) file.readUnsignedByte());
            player.setDefaultSize((short) file.readUnsignedByte());
            player.setTilesLeftBetweenRooms((short) file.readUnsignedByte());
            player.setDistanceBetweenRoomsThatShouldBeCloseMan(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Player.Distance.class));
            player.setCorridorStyle(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Player.CorridorStyle.class));
            player.setWhenMoreSpaceInRoomRequired(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Player.RoomExpandPolicy.class));
            player.setDigToNeutralRoomsWithinTilesOfHeart((short) file.readUnsignedByte());
            List<Short> buildOrder = new ArrayList<>(15);
            for (int i = 0; i < 15; i++) {
                buildOrder.add((short) file.readUnsignedByte());
            }
            player.setBuildOrder(buildOrder);
            player.setFlexibility((short) file.readUnsignedByte());
            player.setDigToNeutralRoomsWithinTilesOfClaimedArea((short) file.readUnsignedByte());
            player.setRemoveCallToArmsAfterSeconds(ConversionUtils.readUnsignedShort(file));
            player.setBoulderTrapsOnLongCorridors(ConversionUtils.readInteger(file) == 1);
            player.setBoulderTrapsOnRouteToBreachPoints(ConversionUtils.readInteger(file) == 1);
            player.setTrapUseStyle((short) file.readUnsignedByte());
            player.setDoorTrapPreference((short) file.readUnsignedByte());
            player.setDoorUsage(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Player.DoorUsagePolicy.class));
            player.setChanceOfLookingToUseTrapsAndDoors((short) file.readUnsignedByte());
            player.setRequireMinLevelForCreatures(ConversionUtils.readInteger(file) == 1);
            player.setRequireTotalThreatGreaterThanTheEnemy(ConversionUtils.readInteger(file) == 1);
            player.setRequireAllRoomTypesPlaced(ConversionUtils.readInteger(file) == 1);
            player.setRequireAllKeeperSpellsResearched(ConversionUtils.readInteger(file) == 1);
            player.setOnlyAttackAttackers(ConversionUtils.readInteger(file) == 1);
            player.setNeverAttack(ConversionUtils.readInteger(file) == 1);
            player.setMinLevelForCreatures((short) file.readUnsignedByte());
            player.setTotalThreatGreaterThanTheEnemy((short) file.readUnsignedByte());
            player.setFirstAttemptToBreachRoom(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Player.BreachRoomPolicy.class));
            player.setFirstDigToEnemyPoint(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Player.DigToPolicy.class));
            player.setBreachAtPointsSimultaneously((short) file.readUnsignedByte());
            player.setUsePercentageOfTotalCreaturesInFirstFightAfterBreach((short) file.readUnsignedByte());
            player.setManaValue(ConversionUtils.readUnsignedShort(file));
            player.setPlaceCallToArmsWhereThreatValueIsGreaterThan(ConversionUtils.readUnsignedShort(file));
            player.setRemoveCallToArmsIfLessThanEnemyCreatures((short) file.readUnsignedByte());
            player.setRemoveCallToArmsIfLessThanEnemyCreaturesWithinTiles((short) file.readUnsignedByte());
            player.setPullCreaturesFromFightIfOutnumberedAndUnableToDropReinforcements(ConversionUtils.readInteger(file) == 1);
            player.setThreatValueOfDroppedCreaturesIsPercentageOfEnemyThreatValue((short) file.readUnsignedByte());
            player.setSpellStyle((short) file.readUnsignedByte());
            player.setAttemptToImprisonPercentageOfEnemyCreatures((short) file.readUnsignedByte());
            player.setIfCreatureHealthIsPercentageAndNotInOwnRoomMoveToLairOrTemple((short) file.readUnsignedByte());
            player.setGoldValue(ConversionUtils.readUnsignedShort(file));
            player.setTryToMakeUnhappyOnesHappy(ConversionUtils.readInteger(file) == 1);
            player.setTryToMakeAngryOnesHappy(ConversionUtils.readInteger(file) == 1);
            player.setDisposeOfAngryCreatures(ConversionUtils.readInteger(file) == 1);
            player.setDisposeOfRubbishCreaturesIfBetterOnesComeAlong(ConversionUtils.readInteger(file) == 1);
            player.setDisposalMethod(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Player.CreatureDisposalPolicy.class));
            player.setMaximumNumberOfImps((short) file.readUnsignedByte());
            player.setWillNotSlapCreatures((short) file.readUnsignedByte() == 0);
            player.setAttackWhenNumberOfCreaturesIsAtLeast((short) file.readUnsignedByte());
            player.setUseLightningIfEnemyIsInWater(ConversionUtils.readInteger(file) == 1);
            player.setUseSightOfEvil(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Player.SightOfEvilUsagePolicy.class));
            player.setUseSpellsInBattle((short) file.readUnsignedByte());
            player.setSpellsPowerPreference((short) file.readUnsignedByte());
            player.setUseCallToArms(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Player.CallToArmsUsagePolicy.class));
            short[] unknown2 = new short[2];
            for (int i = 0; i < unknown2.length; i++) {
                unknown2[i] = (short) file.readUnsignedByte();
            }
            player.setUnknown2(unknown2);
            player.setMineGoldUntilGoldHeldIsGreaterThan(ConversionUtils.readUnsignedShort(file));
            player.setWaitSecondsAfterPreviousAttackBeforeAttackingAgain(ConversionUtils.readUnsignedShort(file));
            player.setStartingMana(ConversionUtils.readUnsignedInteger(file));
            player.setExploreUpToTilesToFindSpecials(ConversionUtils.readUnsignedShort(file));
            player.setImpsToTilesRatio(ConversionUtils.readUnsignedShort(file));
            player.setBuildAreaStartX(ConversionUtils.readUnsignedShort(file));
            player.setBuildAreaStartY(ConversionUtils.readUnsignedShort(file));
            player.setBuildAreaEndX(ConversionUtils.readUnsignedShort(file));
            player.setBuildAreaEndY(ConversionUtils.readUnsignedShort(file));
            player.setLikelyhoodToMovingCreaturesToLibraryForResearching(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Player.MoveToResearchPolicy.class));
            player.setChanceOfExploringToFindSpecials((short) file.readUnsignedByte());
            player.setChanceOfFindingSpecialsWhenExploring((short) file.readUnsignedByte());
            player.setFateOfImprisonedCreatures(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Player.ImprisonedCreatureFatePolicy.class));
            player.setTriggerId(ConversionUtils.readUnsignedShort(file));
            player.setPlayerId((short) file.readUnsignedByte());
            player.setStartingCameraX(ConversionUtils.readUnsignedShort(file));
            player.setStartingCameraY(ConversionUtils.readUnsignedShort(file));
            byte[] bytes = new byte[32];
            file.read(bytes);
            player.setName(ConversionUtils.bytesToString(bytes).trim());

            // Add to the hash by the player ID
            players.put(player.getPlayerId(), player);

            // Check file offset
            checkOffset(header, file, offset);
        }
    }

    /**
     * Reads the Terrain.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readTerrain(KwdHeader header, RandomAccessFile file) throws RuntimeException, IOException {

        // Read the terrain catalog
        logger.info("Reading terrain!");
        terrainTiles = new HashMap<>(header.getItemCount());
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();
            Terrain terrain = new Terrain();
            byte[] bytes = new byte[32];
            file.read(bytes);
            terrain.setName(ConversionUtils.bytesToString(bytes).trim());
            terrain.setCompleteResource(readArtResource(file));
            terrain.setSideResource(readArtResource(file));
            terrain.setTopResource(readArtResource(file));
            terrain.setTaggedTopResource(readArtResource(file));
            terrain.setStringIds(readStringId(file));
            terrain.setDepth(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            terrain.setLightHeight(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            terrain.setFlags(ConversionUtils.parseFlagValue(ConversionUtils.readUnsignedIntegerAsLong(file), Terrain.TerrainFlag.class));
            terrain.setDamage(ConversionUtils.readUnsignedShort(file));
            terrain.setUnk196(ConversionUtils.readUnsignedShort(file));
            terrain.setUnk198(ConversionUtils.readUnsignedShort(file));
            terrain.setGoldValue(ConversionUtils.readUnsignedShort(file));
            terrain.setManaGain(ConversionUtils.readUnsignedShort(file));
            terrain.setMaxManaGain(ConversionUtils.readUnsignedShort(file));
            terrain.setTooltipStringId(ConversionUtils.readUnsignedShort(file));
            terrain.setNameStringId(ConversionUtils.readUnsignedShort(file));
            terrain.setMaxHealthEffectId(ConversionUtils.readUnsignedShort(file));
            terrain.setDestroyedEffectId(ConversionUtils.readUnsignedShort(file));
            terrain.setGeneralDescriptionStringId(ConversionUtils.readUnsignedShort(file));
            terrain.setStrengthStringId(ConversionUtils.readUnsignedShort(file));
            terrain.setWeaknessStringId(ConversionUtils.readUnsignedShort(file));
            int[] unk1ae = new int[16];
            for (int x = 0; x < unk1ae.length; x++) {
                unk1ae[x] = ConversionUtils.readUnsignedShort(file);
            }
            terrain.setUnk1ae(unk1ae);
            terrain.setWibbleH((short) file.readUnsignedByte());
            short[] leanH = new short[3];
            for (int x = 0; x < leanH.length; x++) {
                leanH[x] = (short) file.readUnsignedByte();
            }
            terrain.setLeanH(leanH);
            terrain.setWibbleV((short) file.readUnsignedByte());
            short[] leanV = new short[3];
            for (int x = 0; x < leanV.length; x++) {
                leanV[x] = (short) file.readUnsignedByte();
            }
            terrain.setLeanV(leanV);
            terrain.setTerrainId((short) file.readUnsignedByte());
            terrain.setStartingHealth(ConversionUtils.readUnsignedShort(file));
            terrain.setMaxHealthTypeTerrainId((short) file.readUnsignedByte());
            terrain.setDestroyedTypeTerrainId((short) file.readUnsignedByte());
            terrain.setTerrainLight(new Color(file.readUnsignedByte(), file.readUnsignedByte(), file.readUnsignedByte()));
            terrain.setTextureFrames((short) file.readUnsignedByte());
            bytes = new byte[32];
            file.read(bytes);
            terrain.setSoundCategory(ConversionUtils.bytesToString(bytes).trim());
            terrain.setMaxHealth(ConversionUtils.readUnsignedShort(file));
            terrain.setAmbientLight(new Color(file.readUnsignedByte(), file.readUnsignedByte(), file.readUnsignedByte()));
            bytes = new byte[32];
            file.read(bytes);
            terrain.setSoundCategoryFirstPerson(ConversionUtils.bytesToString(bytes).trim());
            terrain.setUnk224(ConversionUtils.readUnsignedInteger(file));

            // Add to the hash by the terrain ID
            terrainTiles.put(terrain.getTerrainId(), terrain);

            // See that we have water & lava set
            if (water == null && terrain.getFlags().contains(Terrain.TerrainFlag.WATER)) {
                water = terrain;
            }
            if (lava == null && terrain.getFlags().contains(Terrain.TerrainFlag.LAVA)) {
                lava = terrain;
            }
            if (claimedPath == null && terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE) && !terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
                claimedPath = terrain;
            }

            // Check file offset
            checkOffset(header, file, offset);
        }
    }

    /**
     * Reads and parses an ArtResource object from the current file location (84
     * bytes)
     *
     * @param file the file stream to parse from
     * @return an ArtResource
     */
    private ArtResource readArtResource(RandomAccessFile file) throws IOException {
        ArtResource artResource = new ArtResource();

        // Read the data
        byte[] bytes = new byte[64];
        file.read(bytes);
        artResource.setName(ConversionUtils.bytesToString(bytes).trim());
        int flags = ConversionUtils.readUnsignedInteger(file);
        bytes = new byte[12];
        file.read(bytes); // Depends on the type how these are interpreted?
        short type = (short) file.readUnsignedByte();
        short startAf = (short) file.readUnsignedByte();
        short endAf = (short) file.readUnsignedByte();
        short sometimesOne = (short) file.readUnsignedByte();

        // Debug
//        System.out.println("Name: " + artResource.getName());
//        System.out.println("Type: " + type);
//        System.out.println("Flag: " + flags);

        // Mesh collection (type 8) has just the name, reference to GROP meshes probably
        // And alphas and images probably share the same attributes
        ResourceType resourceType = artResource.new ResourceType();
        switch (type) {
            case 1:
            case 2:
            case 3: { // Images of different type
                resourceType = artResource.new Image();
                ((Image) resourceType).setWidth(ConversionUtils.readUnsignedInteger(Arrays.copyOfRange(bytes, 0, 4)) / FIXED_POINT_DIVISION);
                ((Image) resourceType).setHeight(ConversionUtils.readUnsignedInteger(Arrays.copyOfRange(bytes, 4, 8)) / FIXED_POINT_DIVISION);
                ((Image) resourceType).setFrames(ConversionUtils.readUnsignedShort(Arrays.copyOfRange(bytes, 8, 10)));
                break;
            }
            case 4: {
                resourceType = artResource.new TerrainResource();
                ((TerrainResource) resourceType).setX00(ConversionUtils.readUnsignedInteger(Arrays.copyOfRange(bytes, 0, 4)));
                ((TerrainResource) resourceType).setX04(ConversionUtils.readUnsignedInteger(Arrays.copyOfRange(bytes, 4, 8)));
                ((TerrainResource) resourceType).setFrames(ConversionUtils.toUnsignedByte(bytes[8]));
                break;
            }
            case 5: {
                resourceType = artResource.new Mesh();
                ((Mesh) resourceType).setScale(ConversionUtils.readUnsignedInteger(Arrays.copyOfRange(bytes, 0, 4)) / FIXED_POINT_DIVISION);
                ((Mesh) resourceType).setFrames(ConversionUtils.readUnsignedShort(Arrays.copyOfRange(bytes, 4, 6)));
                break;
            }
            case 6: {
                resourceType = artResource.new Animation();
                ((Animation) resourceType).setFrames(ConversionUtils.readUnsignedInteger(Arrays.copyOfRange(bytes, 0, 4)));
                ((Animation) resourceType).setFps(ConversionUtils.readUnsignedInteger(Arrays.copyOfRange(bytes, 4, 8)));
                ((Animation) resourceType).setStartDist(ConversionUtils.readUnsignedShort(Arrays.copyOfRange(bytes, 8, 10)));
                ((Animation) resourceType).setEndDist(ConversionUtils.readUnsignedShort(Arrays.copyOfRange(bytes, 10, 12)));
                break;
            }
            case 7: {
                resourceType = artResource.new Proc();
                ((Proc) resourceType).setId(ConversionUtils.readUnsignedInteger(Arrays.copyOfRange(bytes, 0, 4)));
                break;


            }
        }

        // Add the common values
        resourceType.setFlags(ConversionUtils.parseFlagValue(flags, ArtResource.ArtResourceFlag.class));
        resourceType.setType(ConversionUtils.parseEnum(type, ArtResource.Type.class));
        resourceType.setStartAf(startAf);

        resourceType.setEndAf(endAf);

        resourceType.setSometimesOne(sometimesOne);

        artResource.setSettings(resourceType);

        // If it has no name or the type is not known, return null
        if (artResource.getName().isEmpty() || resourceType.getType() == null) {
            return null;
        }

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
            ids[i] = ConversionUtils.readUnsignedInteger(file);
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
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readDoors(KwdHeader header, RandomAccessFile file) throws IOException {

        // Read the doors catalog
        logger.info("Reading doors!");
        doors = new HashMap<>(header.getItemCount());
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();
            Door door = new Door();
            byte[] bytes = new byte[32];
            file.read(bytes);
            door.setName(ConversionUtils.bytesToString(bytes).trim());
            door.setMesh(readArtResource(file));
            door.setGuiIcon(readArtResource(file));
            door.setEditorIcon(readArtResource(file));
            door.setFlowerIcon(readArtResource(file));
            door.setOpenResource(readArtResource(file));
            door.setCloseResource(readArtResource(file));
            door.setHeight(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            door.setHealthGain(ConversionUtils.readUnsignedShort(file));
            short[] unknown2 = new short[8];
            for (int x = 0; x < unknown2.length; x++) {
                unknown2[x] = (short) file.readUnsignedByte();
            }
            door.setUnknown2(unknown2);
            door.setMaterial(ConversionUtils.parseEnum(file.readUnsignedByte(), Material.class));
            door.setTrapTypeId((short) file.readUnsignedByte());
            int flag = ConversionUtils.readUnsignedInteger(file);
            door.setFlags(ConversionUtils.parseFlagValue(flag, DoorFlag.class));
            door.setHealth(ConversionUtils.readUnsignedShort(file));
            door.setGoldCost(ConversionUtils.readUnsignedShort(file));
            short[] unknown3 = new short[2];
            for (int x = 0; x < unknown3.length; x++) {
                unknown3[x] = (short) file.readUnsignedByte();
            }
            door.setUnknown3(unknown3);
            door.setDeathEffectId(ConversionUtils.readUnsignedShort(file));
            door.setManufToBuild(ConversionUtils.readUnsignedInteger(file));
            door.setManaCost(ConversionUtils.readUnsignedShort(file));
            door.setTooltipStringId(ConversionUtils.readUnsignedShort(file));
            door.setNameStringId(ConversionUtils.readUnsignedShort(file));
            door.setGeneralDescriptionStringId(ConversionUtils.readUnsignedShort(file));
            door.setStrengthStringId(ConversionUtils.readUnsignedShort(file));
            door.setWeaknessStringId(ConversionUtils.readUnsignedShort(file));
            door.setDoorId((short) file.readUnsignedByte());
            door.setOrderInEditor((short) file.readUnsignedByte());
            door.setManufCrateObjectId((short) file.readUnsignedByte());
            door.setKeyObjectId((short) file.readUnsignedByte());
            bytes = new byte[32];
            file.read(bytes);
            door.setSoundGategory(ConversionUtils.bytesToString(bytes).trim());

            doors.put(door.getDoorId(), door);

            // Check file offset
            checkOffset(header, file, offset);
        }
    }

    /**
     * Reads the Traps.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readTraps(KwdHeader header, RandomAccessFile file) throws IOException {

        // Read the traps catalog
        logger.info("Reading traps!");
        traps = new HashMap<>(header.getItemCount());
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();
            Trap trap = new Trap();
            byte[] bytes = new byte[32];
            file.read(bytes);
            trap.setName(ConversionUtils.bytesToString(bytes).trim());
            trap.setMeshResource(readArtResource(file));
            trap.setGuiIcon(readArtResource(file));
            trap.setEditorIcon(readArtResource(file));
            trap.setFlowerIcon(readArtResource(file));
            trap.setFireResource(readArtResource(file));
            trap.setHeight(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            trap.setRechargeTime(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            trap.setChargeTime(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            trap.setThreatDuration(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            trap.setManaCostToFire(ConversionUtils.readUnsignedInteger(file));
            trap.setIdleEffectDelay(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            trap.setTriggerData(ConversionUtils.readUnsignedInteger(file));
            trap.setShotData1(ConversionUtils.readUnsignedInteger(file));
            trap.setShotData2(ConversionUtils.readUnsignedInteger(file));
            short[] unknown3 = new short[2];
            for (int x = 0; x < unknown3.length; x++) {
                unknown3[x] = (short) file.readUnsignedByte();
            }
            trap.setUnknown3(unknown3);
            trap.setThreat(ConversionUtils.readUnsignedShort(file));
            trap.setFlags(ConversionUtils.parseFlagValue(ConversionUtils.readUnsignedInteger(file), Trap.TrapFlag.class));
            trap.setHealth(ConversionUtils.readUnsignedShort(file));
            trap.setManaCost(ConversionUtils.readUnsignedShort(file));
            trap.setPowerlessEffectId(ConversionUtils.readUnsignedShort(file));
            trap.setIdleEffectId(ConversionUtils.readUnsignedShort(file));
            trap.setDeathEffectId(ConversionUtils.readUnsignedShort(file));
            trap.setManufToBuild(ConversionUtils.readUnsignedShort(file));
            trap.setGeneralDescriptionStringId(ConversionUtils.readUnsignedShort(file));
            trap.setStrengthStringId(ConversionUtils.readUnsignedShort(file));
            trap.setWeaknessStringId(ConversionUtils.readUnsignedShort(file));
            trap.setManaUsage(ConversionUtils.readUnsignedShort(file));
            short[] unknown4 = new short[2];
            for (int x = 0; x < unknown4.length; x++) {
                unknown4[x] = (short) file.readUnsignedByte();
            }
            trap.setUnknown4(unknown4);
            trap.setTooltipStringId(ConversionUtils.readUnsignedShort(file));
            trap.setNameStringId(ConversionUtils.readUnsignedShort(file));
            trap.setUnknown5((short) file.readUnsignedByte());
            trap.setTriggerType(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Trap.TriggerType.class));
            trap.setTrapId((short) file.readUnsignedByte());
            trap.setShotTypeId((short) file.readUnsignedByte());
            trap.setManufCrateObjectId((short) file.readUnsignedByte());
            bytes = new byte[32];
            file.read(bytes);
            trap.setSoundCategory(ConversionUtils.bytesToString(bytes).trim());
            trap.setMaterial(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Material.class));
            trap.setOrderInEditor((short) file.readUnsignedByte());
            trap.setShotOffset(new Vector3f(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION, ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION, ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION));
            trap.setShotDelay(ConversionUtils.readUnsignedShort(file) / FIXED_POINT_DIVISION);
            trap.setUnknown2(file.readUnsignedShort());
            trap.setHealthGain(ConversionUtils.readUnsignedShort(file));

            traps.put(trap.getTrapId(), trap);

            // Check file offset
            checkOffset(header, file, offset);
        }
    }

    /**
     * Reads the Rooms.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readRooms(KwdHeader header, RandomAccessFile file) throws RuntimeException, IOException {

        // Read the rooms catalog
        logger.info("Reading rooms!");
        rooms = new HashMap<>(header.getItemCount());
        roomsByTerrainId = new HashMap<>(header.getItemCount());
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();
            Room room = new Room();
            byte[] bytes = new byte[32];
            file.read(bytes);
            room.setName(ConversionUtils.bytesToString(bytes).trim());
            room.setGuiIcon(readArtResource(file));
            room.setEditorIcon(readArtResource(file));
            room.setCompleteResource(readArtResource(file));
            room.setStraightResource(readArtResource(file));
            room.setInsideCornerResource(readArtResource(file));
            room.setUnknownResource(readArtResource(file));
            room.setOutsideCornerResource(readArtResource(file));
            room.setWallResource(readArtResource(file));
            room.setCapResource(readArtResource(file));
            room.setCeilingResource(readArtResource(file));
            room.setCeilingHeight(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            room.setUnknown2(ConversionUtils.readUnsignedShort(file));
            room.setTorchIntensity(ConversionUtils.readUnsignedShort(file));
            room.setFlags(ConversionUtils.parseFlagValue(ConversionUtils.readUnsignedInteger(file), Room.RoomFlag.class));
            room.setTooltipStringId(ConversionUtils.readUnsignedShort(file));
            room.setNameStringId(ConversionUtils.readUnsignedShort(file));
            room.setCost(ConversionUtils.readUnsignedShort(file));
            room.setFightEffectId(ConversionUtils.readUnsignedShort(file));
            room.setGeneralDescriptionStringId(ConversionUtils.readUnsignedShort(file));
            room.setStrengthStringId(ConversionUtils.readUnsignedShort(file));
            room.setTorchRadius(ConversionUtils.readUnsignedShort(file) / FIXED_POINT_DIVISION);
            List<Integer> effects = new ArrayList<>(8);
            for (int x = 0; x < 8; x++) {
                int effectId = ConversionUtils.readUnsignedShort(file);
                if (effectId > 0) {
                    effects.add(effectId);
                }
            }
            room.setEffects(effects);
            room.setRoomId((short) file.readUnsignedByte());
            room.setUnknown7((short) file.readUnsignedByte());
            room.setTerrainId((short) file.readUnsignedByte());
            room.setTileConstruction(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Room.TileConstruction.class));
            room.setCreatedCreatureId((short) file.readUnsignedByte());
            room.setTorchColor(new Color(file.readUnsignedByte(), file.readUnsignedByte(), file.readUnsignedByte())); // This is the editor is rather weird
            List<Short> objects = new ArrayList<>(8);
            for (int x = 0; x < 8; x++) {
                short objectId = (short) file.readUnsignedByte();
                if (objectId > 0) {
                    objects.add(objectId);
                }
            }
            room.setObjects(objects);
            bytes = new byte[32];
            file.read(bytes);
            room.setSoundCategory(ConversionUtils.bytesToString(bytes).trim());
            room.setOrderInEditor((short) file.readUnsignedByte());
            room.setX3c3((short) file.readUnsignedByte());
            room.setUnknown10(ConversionUtils.readUnsignedShort(file));
            room.setUnknown11((short) file.readUnsignedByte());
            room.setTorch(readArtResource(file));
            room.setRecommendedSizeX((short) file.readUnsignedByte());
            room.setRecommendedSizeY((short) file.readUnsignedByte());
            room.setHealthGain(ConversionUtils.readShort(file));

            // Add to the hash by the room ID
            rooms.put(room.getRoomId(), room);

            // And by the terrain ID
            roomsByTerrainId.put(room.getTerrainId(), room);

            // Check file offset
            checkOffset(header, file, offset);
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

            rawMapInfo.seek(20); // End of header

            //Additional header data
            int pathCount = ConversionUtils.readUnsignedShort(rawMapInfo);
            int unknownCount = ConversionUtils.readUnsignedShort(rawMapInfo);
            rawMapInfo.skipBytes(4);

            //Gather the timestamps
            timestamp1 = readTimestamp(rawMapInfo);
            timestamp2 = readTimestamp(rawMapInfo);
            rawMapInfo.skipBytes(8);

            //Property data
            byte[] bytes = new byte[64 * 2];
            rawMapInfo.read(bytes);
            name = ConversionUtils.bytesToStringUtf16(bytes).trim();
            if (name != null && !name.isEmpty() && name.toLowerCase().endsWith(".kwd")) {
                name = name.substring(0, name.length() - 4);
            }

            bytes = new byte[1024 * 2];
            rawMapInfo.read(bytes);
            description = ConversionUtils.bytesToStringUtf16(bytes).trim();

            bytes = new byte[64 * 2];
            rawMapInfo.read(bytes);
            author = ConversionUtils.bytesToStringUtf16(bytes).trim();

            bytes = new byte[64 * 2];
            rawMapInfo.read(bytes);
            email = ConversionUtils.bytesToStringUtf16(bytes).trim();

            bytes = new byte[1024 * 2];
            rawMapInfo.read(bytes);
            information = ConversionUtils.bytesToStringUtf16(bytes).trim();

            triggerId = ConversionUtils.readUnsignedShort(rawMapInfo);
            ticksPerSec = ConversionUtils.readUnsignedShort(rawMapInfo);
            x01184 = new short[520];
            for (int x = 0; x < x01184.length; x++) {
                x01184[x] = (short) rawMapInfo.readUnsignedByte();
            }
            messages = new ArrayList<>(); // I don't know if we need the index, level 19 & 3 has messages, but they are rare
            for (int x = 0; x < 512; x++) {
                bytes = new byte[20 * 2];
                rawMapInfo.read(bytes);
                String message = ConversionUtils.bytesToStringUtf16(bytes).trim();
                if (!message.isEmpty()) {
                    messages.add(message);
                }
            }
            int flag = ConversionUtils.readUnsignedShort(rawMapInfo);
            lvlFlags = ConversionUtils.parseFlagValue(flag, LevFlag.class);
            bytes = new byte[32];
            rawMapInfo.read(bytes);
            speechStr = ConversionUtils.bytesToString(bytes).trim();
            talismanPieces = (short) rawMapInfo.readUnsignedByte();
            rewardPrev = new ArrayList<>(4);
            for (int x = 0; x < 4; x++) {
                LevelReward reward = ConversionUtils.parseEnum((short) rawMapInfo.readUnsignedByte(), LevelReward.class);
                if (reward != null && !reward.equals(LevelReward.NONE)) {
                    rewardPrev.add(reward);
                }
            }
            rewardNext = new ArrayList<>(4);
            for (int x = 0; x < 4; x++) {
                LevelReward reward = ConversionUtils.parseEnum((short) rawMapInfo.readUnsignedByte(), LevelReward.class);
                if (reward != null && !reward.equals(LevelReward.NONE)) {
                    rewardNext.add(reward);
                }
            }
            soundTrack = (short) rawMapInfo.readUnsignedByte();
            textTableId = ConversionUtils.parseEnum((short) rawMapInfo.readUnsignedByte(), TextTable.class);
            textTitleId = ConversionUtils.readUnsignedShort(rawMapInfo);
            textPlotId = ConversionUtils.readUnsignedShort(rawMapInfo);
            textDebriefId = ConversionUtils.readUnsignedShort(rawMapInfo);
            textObjectvId = ConversionUtils.readUnsignedShort(rawMapInfo);
            x063c3 = ConversionUtils.readUnsignedShort(rawMapInfo);
            textSubobjctvId1 = ConversionUtils.readUnsignedShort(rawMapInfo);
            textSubobjctvId2 = ConversionUtils.readUnsignedShort(rawMapInfo);
            textSubobjctvId3 = ConversionUtils.readUnsignedShort(rawMapInfo);
            speclvlIdx = ConversionUtils.readUnsignedShort(rawMapInfo);

            // Swap the arrays for more convenient data format
            short[] textIntrdcOverrdObj = new short[8];
            for (int x = 0; x < textIntrdcOverrdObj.length; x++) {
                textIntrdcOverrdObj[x] = (short) rawMapInfo.readUnsignedByte();
            }
            int[] textIntrdcOverrdId = new int[8];
            for (int x = 0; x < textIntrdcOverrdId.length; x++) {
                textIntrdcOverrdId[x] = ConversionUtils.readUnsignedShort(rawMapInfo);
            }
            introductionOverrideTextIds = new HashMap<>(8);
            for (int x = 0; x < textIntrdcOverrdObj.length; x++) {
                if (textIntrdcOverrdObj[x] > 0) {

                    // Over 0 is a valid creature ID
                    introductionOverrideTextIds.put(textIntrdcOverrdObj[x], textIntrdcOverrdId[x]);
                }
            }
            //

            bytes = new byte[32];
            rawMapInfo.read(bytes);
            terrainPath = ConversionUtils.bytesToString(bytes).trim();
            oneShotHornyLev = (short) rawMapInfo.readUnsignedByte();
            playerCount = (short) rawMapInfo.readUnsignedByte();
            x06405 = (short) rawMapInfo.readUnsignedByte();
            x06406 = (short) rawMapInfo.readUnsignedByte();
            speechHornyId = ConversionUtils.readUnsignedShort(rawMapInfo);
            speechPrelvlId = ConversionUtils.readUnsignedShort(rawMapInfo);
            speechPostlvlWin = ConversionUtils.readUnsignedShort(rawMapInfo);
            speechPostlvlLost = ConversionUtils.readUnsignedShort(rawMapInfo);
            speechPostlvlNews = ConversionUtils.readUnsignedShort(rawMapInfo);
            speechPrelvlGenr = ConversionUtils.readUnsignedShort(rawMapInfo);
            bytes = new byte[32 * 2];
            rawMapInfo.read(bytes);
            heroName = ConversionUtils.bytesToStringUtf16(bytes).trim();

            // Paths and the unknown array
            rawMapInfo.skipBytes(8);
            paths = new ArrayList<>(pathCount);
            for (int x = 0; x < pathCount; x++) {
                FilePath filePath = new FilePath();
                filePath.setId(ConversionUtils.parseEnum(ConversionUtils.readUnsignedInteger(rawMapInfo), MapDataTypeEnum.class));
                filePath.setUnknown2(ConversionUtils.readInteger(rawMapInfo));
                bytes = new byte[64];
                rawMapInfo.read(bytes);
                String path = ConversionUtils.bytesToString(bytes).trim();

                // Tweak the paths

                // Paths are relative to the base path, may or may not have an extension (assume kwd if none found)
                path = ConversionUtils.convertFileSeparators(path);
                if (!".".equals(path.substring(path.length() - 4, path.length() - 3))) {
                    path = path.concat(".kwd");
                }

                // See if the globals are present
                if (filePath.getId() == MapDataTypeEnum.GLOBALS) {
                    customOverrides = true;
                    logger.info("The map uses custom overrides!");
                } else if (filePath.getId() == MapDataTypeEnum.MAP) {
                    mapPath = filePath;
                }
                //
                filePath.setPath(path);

                paths.add(filePath);
            }
            unknown = new int[unknownCount];
            for (int x = 0; x < unknown.length; x++) {
                unknown[x] = ConversionUtils.readUnsignedShort(rawMapInfo);
            }
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
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readCreatures(KwdHeader header, RandomAccessFile file) throws IOException {

        // Read the creatures catalog
        logger.info("Reading creatures!");
        creatures = new HashMap<>(header.getItemCount());
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();
            Creature creature = new Creature();
            byte[] bytes = new byte[32];
            file.read(bytes);
            creature.setName(ConversionUtils.bytesToString(bytes).trim());
            // 39 ArtResources (with IMPs these are not 100% same)
            creature.setUnknown1Resource(readArtResource(file));
            creature.setAnimWalkResource(readArtResource(file));
            creature.setAnimRunResource(readArtResource(file));
            creature.setAnimDraggedPoseResource(readArtResource(file));
            creature.setAnimRecoilHffResource(readArtResource(file));
            creature.setAnimMelee1Resource(readArtResource(file));
            creature.setAnimMagicResource(readArtResource(file));
            creature.setAnimDieResource(readArtResource(file));
            creature.setAnimHappyResource(readArtResource(file));
            creature.setAnimAngryResource(readArtResource(file));
            creature.setAnimStunnedPoseResource(readArtResource(file));
            creature.setAnimSwingResource(readArtResource(file));
            creature.setAnimSleepResource(readArtResource(file));
            creature.setAnimEatResource(readArtResource(file));
            creature.setAnimResearchResource(readArtResource(file));
            creature.setUnknown2Resource(readArtResource(file));
            creature.setAnimDejectedPoseResource(readArtResource(file));
            creature.setAnimTortureResource(readArtResource(file));
            creature.setUnknown3Resource(readArtResource(file));
            creature.setAnimDrinkResource(readArtResource(file));
            creature.setAnimIdle1Resource(readArtResource(file));
            creature.setAnimRecoilHfbResource(readArtResource(file));
            creature.setUnknown4Resource(readArtResource(file));
            creature.setAnimPrayResource(readArtResource(file));
            creature.setAnimFallbackResource(readArtResource(file));
            creature.setAnimElecResource(readArtResource(file));
            creature.setAnimElectrocuteResource(readArtResource(file));
            creature.setAnimGetUpResource(readArtResource(file));
            creature.setAnimDanceResource(readArtResource(file));
            creature.setAnimDrunkResource(readArtResource(file));
            creature.setAnimEntranceResource(readArtResource(file));
            creature.setAnimIdle2Resource(readArtResource(file));
            creature.setUnknown5Resource(readArtResource(file));
            creature.setUnknown6Resource(readArtResource(file));
            creature.setAnimDrunk2Resource(readArtResource(file));
            creature.setUnknown7Resource(readArtResource(file));
            creature.setUnknown8Resource(readArtResource(file));
            creature.setIcon1Resource(readArtResource(file));
            creature.setIcon2Resource(readArtResource(file));
            //
            creature.setUnkcec(ConversionUtils.readUnsignedShort(file));
            creature.setUnkcee(ConversionUtils.readUnsignedInteger(file));
            creature.setUnkcf2(ConversionUtils.readUnsignedInteger(file));
            creature.setOrderInEditor((short) file.readUnsignedByte());
            creature.setAngerStringIdGeneral(ConversionUtils.readUnsignedShort(file));
            creature.setShotDelay(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            creature.setOlhiEffectId(ConversionUtils.readUnsignedShort(file));
            creature.setIntroductionStringId(ConversionUtils.readUnsignedShort(file));
            creature.setPerceptionRange(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            creature.setAngerStringIdLair(ConversionUtils.readUnsignedShort(file));
            creature.setAngerStringIdFood(ConversionUtils.readUnsignedShort(file));
            creature.setAngerStringIdPay(ConversionUtils.readUnsignedShort(file));
            creature.setAngerStringIdWork(ConversionUtils.readUnsignedShort(file));
            creature.setAngerStringIdSlap(ConversionUtils.readUnsignedShort(file));
            creature.setAngerStringIdHeld(ConversionUtils.readUnsignedShort(file));
            creature.setAngerStringIdLonely(ConversionUtils.readUnsignedShort(file));
            creature.setAngerStringIdHatred(ConversionUtils.readUnsignedShort(file));
            creature.setAngerStringIdTorture(ConversionUtils.readUnsignedShort(file));
            bytes = new byte[32];
            file.read(bytes);
            creature.setTranslationSoundGategory(ConversionUtils.bytesToString(bytes).trim());
            creature.setShuffleSpeed(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            creature.setCreatureId((short) file.readUnsignedByte());
            creature.setFirstPersonGammaEffect(ConversionUtils.parseEnum(file.readUnsignedByte(), Creature.GammaEffect.class));
            creature.setFirstPersonWalkCycleScale((short) file.readUnsignedByte());
            creature.setIntroCameraPathIndex((short) file.readUnsignedByte());
            creature.setUnk2e2((short) file.readUnsignedByte());
            creature.setPortraitResource(readArtResource(file));
            creature.setLight(readLight(file));
            Attraction[] attractions = new Attraction[2];
            for (int x = 0; x < attractions.length; x++) {
                Attraction attraction = creature.new Attraction();
                attraction.setPresent(ConversionUtils.readUnsignedInteger(file));
                attraction.setRoomId(ConversionUtils.readUnsignedShort(file));
                attraction.setRoomSize(ConversionUtils.readUnsignedShort(file));
                attractions[x] = attraction;
            }
            creature.setAttractions(attractions);
            creature.setFirstPersonWaddleScale(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            creature.setFirstPersonOscillateScale(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            Spell[] spells = new Spell[3];
            for (int x = 0; x < spells.length; x++) {
                Spell spell = creature.new Spell();
                spell.setShotOffset(new Vector3f(ConversionUtils.readUnsignedIntegerAsLong(file) / FIXED_POINT_DIVISION, ConversionUtils.readUnsignedIntegerAsLong(file) / FIXED_POINT_DIVISION, ConversionUtils.readUnsignedIntegerAsLong(file) / FIXED_POINT_DIVISION));
                spell.setX0c((short) file.readUnsignedByte());
                spell.setPlayAnimation((short) file.readUnsignedByte() == 1 ? true : false);
                spell.setX0e((short) file.readUnsignedByte()); // This value can changed when you not change anything on map, only save it
                spell.setX0f((short) file.readUnsignedByte());
                spell.setShotDelay(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
                spell.setX14((short) file.readUnsignedByte());
                spell.setX15((short) file.readUnsignedByte());
                spell.setCreatureSpellId((short) file.readUnsignedByte());
                spell.setLevelAvailable((short) file.readUnsignedByte());
                spells[x] = spell;
            }
            creature.setSpells(spells);
            Creature.Resistance[] resistances = new Creature.Resistance[4];
            for (int x = 0; x < resistances.length; x++) {
                Creature.Resistance resistance = creature.new Resistance();
                resistance.setAttackType(ConversionUtils.parseEnum(file.readUnsignedByte(), Creature.AttackType.class));
                resistance.setValue((short) file.readUnsignedByte());
                resistances[x] = resistance;
            }
            creature.setResistances(resistances);
            creature.setHappyJobs(readJobPreferences(3, creature, file));
            creature.setUnhappyJobs(readJobPreferences(2, creature, file));
            creature.setAngryJobs(readJobPreferences(3, creature, file));
            Creature.JobType[] hateJobs = new Creature.JobType[2];
            for (int x = 0; x < hateJobs.length; x++) {
                hateJobs[x] = ConversionUtils.parseEnum(ConversionUtils.readUnsignedInteger(file), Creature.JobType.class);
            }
            creature.setHateJobs(hateJobs);
            JobAlternative[] alternatives = new JobAlternative[3];
            for (int x = 0; x < alternatives.length; x++) {
                JobAlternative alternative = creature.new JobAlternative();
                alternative.setJobType(ConversionUtils.parseEnum(ConversionUtils.readUnsignedInteger(file), Creature.JobType.class));
                alternative.setMoodChange(ConversionUtils.readUnsignedShort(file));
                alternative.setManaChange(ConversionUtils.readUnsignedShort(file));
            }
            creature.setAlternativeJobs(alternatives);
            Xe94 xe94 = creature.new Xe94();
            xe94.setX00(ConversionUtils.readUnsignedIntegerAsLong(file));
            xe94.setX04(ConversionUtils.readUnsignedInteger(file));
            xe94.setX08(ConversionUtils.readUnsignedInteger(file));
            creature.setXe94(xe94);
            creature.setUnkea0(ConversionUtils.readInteger(file));
            creature.setHeight(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION);
            creature.setUnkea8(ConversionUtils.readUnsignedInteger(file));
            creature.setUnk3ab(ConversionUtils.readUnsignedInteger(file));
            creature.setEyeHeight(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION);
            creature.setSpeed(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION);
            creature.setRunSpeed(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION);
            creature.setHungerRate(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            creature.setTimeAwake(ConversionUtils.readUnsignedInteger(file));
            creature.setTimeSleep(ConversionUtils.readUnsignedInteger(file));
            creature.setDistanceCanSee(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            creature.setDistanceCanHear(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            creature.setStunDuration(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            creature.setGuardDuration(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            creature.setIdleDuration(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            creature.setSlapFearlessDuration(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION);
            creature.setUnkee0(ConversionUtils.readInteger(file));
            creature.setUnkee4(ConversionUtils.readInteger(file));
            creature.setPossessionManaCost(ConversionUtils.readShort(file));
            creature.setOwnLandHealthIncrease(ConversionUtils.readShort(file));
            creature.setMeleeRange(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION);
            creature.setUnkef0(ConversionUtils.readUnsignedInteger(file));
            creature.setTortureTimeToConvert(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            creature.setMeleeRecharge(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION);
            // The flags is actually very big, pushing the boundaries, a true uint32, need to -> long
            creature.setFlags(ConversionUtils.parseFlagValue(ConversionUtils.readUnsignedIntegerAsLong(file), Creature.CreatureFlag.class));
            creature.setExpForNextLevel(ConversionUtils.readUnsignedShort(file));
            creature.setJobClass(ConversionUtils.parseEnum(file.readUnsignedByte(), Creature.JobClass.class));
            creature.setFightStyle(ConversionUtils.parseEnum(file.readUnsignedByte(), Creature.FightStyle.class));
            creature.setExpPerSecond(ConversionUtils.readUnsignedShort(file));
            creature.setExpPerSecondTraining(ConversionUtils.readUnsignedShort(file));
            creature.setResearchPerSecond(ConversionUtils.readUnsignedShort(file));
            creature.setManufacturePerSecond(ConversionUtils.readUnsignedShort(file));
            creature.setHp(ConversionUtils.readUnsignedShort(file));
            creature.setHpFromChicken(ConversionUtils.readUnsignedShort(file));
            creature.setFear(ConversionUtils.readUnsignedShort(file));
            creature.setThreat(ConversionUtils.readUnsignedShort(file));
            creature.setMeleeDamage(ConversionUtils.readUnsignedShort(file));
            creature.setSlapDamage(ConversionUtils.readUnsignedShort(file));
            creature.setManaGenPrayer(ConversionUtils.readUnsignedShort(file));
            creature.setUnk3cb(ConversionUtils.readUnsignedShort(file));
            creature.setPay(ConversionUtils.readUnsignedShort(file));
            creature.setMaxGoldHeld(ConversionUtils.readUnsignedShort(file));
            creature.setUnk3cc(ConversionUtils.readUnsignedShort(file));
            creature.setDecomposeValue(ConversionUtils.readUnsignedShort(file));
            creature.setNameStringId(ConversionUtils.readUnsignedShort(file));
            creature.setTooltipStringId(ConversionUtils.readUnsignedShort(file));
            creature.setAngerNoLair(ConversionUtils.readShort(file));
            creature.setAngerNoFood(ConversionUtils.readShort(file));
            creature.setAngerNoPay(ConversionUtils.readShort(file));
            creature.setAngerNoWork(ConversionUtils.readShort(file));
            creature.setAngerSlap(ConversionUtils.readShort(file));
            creature.setAngerInHand(ConversionUtils.readShort(file));
            creature.setInitialGoldHeld(ConversionUtils.readShort(file));
            creature.setEntranceEffectId(ConversionUtils.readUnsignedShort(file));
            creature.setGeneralDescriptionStringId(ConversionUtils.readUnsignedShort(file));
            creature.setStrengthStringId(ConversionUtils.readUnsignedShort(file));
            creature.setWeaknessStringId(ConversionUtils.readUnsignedShort(file));
            creature.setSlapEffectId(ConversionUtils.readUnsignedShort(file));
            creature.setDeathEffectId(ConversionUtils.readUnsignedShort(file));
            creature.setMelee1Swipe(ConversionUtils.parseEnum(file.readUnsignedByte(), Creature.Swipe.class));
            creature.setMelee2Swipe(ConversionUtils.parseEnum(file.readUnsignedByte(), Creature.Swipe.class));
            creature.setUnk3d3((short) file.readUnsignedByte());
            creature.setSpellSwipe(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Creature.Swipe.class));
            creature.setFirstPersonSpecialAbility1(ConversionUtils.parseEnum(file.readUnsignedByte(), Creature.SpecialAbility.class));
            creature.setFirstPersonSpecialAbility2(ConversionUtils.parseEnum(file.readUnsignedByte(), Creature.SpecialAbility.class));
            short[] unkf48 = new short[3];
            for (int x = 0; x < unkf48.length; x++) {
                unkf48[x] = (short) file.readUnsignedByte();
            }
            creature.setUnkf48(unkf48);
            creature.setCreatureId((short) file.readUnsignedByte());
            short[] unk3ea = new short[2];
            for (int x = 0; x < unk3ea.length; x++) {
                unk3ea[x] = (short) file.readUnsignedByte();
            }
            creature.setUnk3ea(unk3ea);
            creature.setHungerFill((short) file.readUnsignedByte());
            creature.setUnhappyThreshold((short) file.readUnsignedByte());
            creature.setMeleeAttackType(ConversionUtils.parseEnum(file.readUnsignedByte(), Creature.AttackType.class));
            creature.setUnk3eb2((short) file.readUnsignedByte());
            creature.setLairObjectId((short) file.readUnsignedByte());
            creature.setUnk3f1((short) file.readUnsignedByte());
            creature.setDeathFallDirection(ConversionUtils.parseEnum(file.readUnsignedByte(), Creature.DeathFallDirection.class));
            creature.setUnk3f2((short) file.readUnsignedByte());
            bytes = new byte[32];
            file.read(bytes);
            creature.setSoundGategory(ConversionUtils.bytesToString(bytes).trim());
            creature.setMaterial(ConversionUtils.parseEnum(file.readUnsignedByte(), Material.class));
            creature.setFirstPersonFilterResource(readArtResource(file));
            creature.setUnkfcb(ConversionUtils.readUnsignedShort(file));
            creature.setUnk4(ConversionUtils.readUnsignedInteger(file));
            creature.setRef3(readArtResource(file));
            creature.setSpecial1Swipe(ConversionUtils.parseEnum(file.readUnsignedByte(), Creature.Swipe.class));
            creature.setSpecial2Swipe(ConversionUtils.parseEnum(file.readUnsignedByte(), Creature.Swipe.class));
            creature.setFirstPersonMeleeResource(readArtResource(file));
            creature.setUnk6(ConversionUtils.readUnsignedInteger(file));
            creature.setTortureHpChange(ConversionUtils.readShort(file));
            creature.setTortureMoodChange(ConversionUtils.readShort(file));
            creature.setAnimMelee2Resource(readArtResource(file));
            creature.setUnknown9Resource(readArtResource(file));
            creature.setUnknown10Resource(readArtResource(file));
            creature.setUnknown11Resource(readArtResource(file));
            creature.setUnknown12Resource(readArtResource(file));
            creature.setUnknown13Resource(readArtResource(file));
            Unk7[] unk7s = new Unk7[7];
            for (int x = 0; x < unk7s.length; x++) {
                Unk7 unk7 = creature.new Unk7();
                unk7.setX00(ConversionUtils.readUnsignedInteger(file));
                unk7.setX04(ConversionUtils.readUnsignedIntegerAsLong(file));
                unk7.setX08(ConversionUtils.readUnsignedInteger(file));
                unk7s[x] = unk7;
            }
            creature.setUnk7(unk7s);
            creature.setAnimWalkbackResource(readArtResource(file));
            X1323[] x1323s = new X1323[48];
            for (int x = 0; x < x1323s.length; x++) {
                X1323 x1323 = creature.new X1323();
                x1323.setX00(ConversionUtils.readUnsignedShort(file));
                x1323.setX02(ConversionUtils.readUnsignedShort(file));
                x1323s[x] = x1323;
            }
            creature.setX1323(x1323s);
            creature.setAnimPoseFrameResource(readArtResource(file));
            creature.setAnimWalk2Resource(readArtResource(file));
            creature.setAnimDiePoseResource(readArtResource(file));
            creature.setUniqueNameTextId(ConversionUtils.readUnsignedShort(file));
            int[] x14e1 = new int[2];
            for (int x = 0; x < x14e1.length; x++) {
                x14e1[x] = ConversionUtils.readUnsignedInteger(file);
            }
            creature.setX14e1(x14e1);
            creature.setFirstPersonSpecialAbility1Count(ConversionUtils.readUnsignedInteger(file));
            creature.setFirstPersonSpecialAbility2Count(ConversionUtils.readUnsignedInteger(file));
            creature.setUniqueResource(readArtResource(file));
            creature.setUnk1545(ConversionUtils.readUnsignedInteger(file));

            // The normal file stops here, but if it is the bigger one, continue
            if (header.getItemSize() >= 5537l) {
                short[] unknownExtraBytes = new short[80];
                for (int x = 0; x < unknownExtraBytes.length; x++) {
                    unknownExtraBytes[x] = (short) file.readUnsignedByte();
                }
                creature.setUnknownExtraBytes(unknownExtraBytes);
                creature.setFlags2(ConversionUtils.parseFlagValue(ConversionUtils.readUnsignedIntegerAsLong(file), Creature.CreatureFlag2.class));
                creature.setUnknown(ConversionUtils.readUnsignedInteger(file));
            }

            // Add to the hash by the creature ID
            creatures.put(creature.getCreatureId(), creature);

            // Check file offset
            checkOffset(header, file, offset);
        }
    }

    /**
     * Read job preferences for a creature
     *
     * @param count amount of job preference records
     * @param creature creature instance, just for creating a job preference
     * instance
     * @param file the file to read the data from
     * @return job preferences
     * @throws IOException may fail
     */
    private Creature.JobPreference[] readJobPreferences(int count, Creature creature, RandomAccessFile file) throws IOException {
        Creature.JobPreference[] preferences = new Creature.JobPreference[count];
        for (int x = 0; x < preferences.length; x++) {
            Creature.JobPreference jobPreference = creature.new JobPreference();
            jobPreference.setJobType(ConversionUtils.parseEnum(ConversionUtils.readUnsignedInteger(file), Creature.JobType.class));
            jobPreference.setMoodChange(ConversionUtils.readUnsignedShort(file));
            jobPreference.setManaChange(ConversionUtils.readUnsignedShort(file));
            jobPreference.setChance((short) file.readUnsignedByte());
            jobPreference.setX09((short) file.readUnsignedByte());
            jobPreference.setX0a((short) file.readUnsignedByte());
            jobPreference.setX0b((short) file.readUnsignedByte());
            preferences[x] = jobPreference;
        }
        return preferences;
    }

    /**
     * Reads and parses an Light object from the current file location (24
     * bytes)
     *
     * @param file the file stream to parse from
     * @return a Light
     */
    private Light readLight(RandomAccessFile file) throws IOException {
        Light light = new Light();

        // Read the data
        light.setmKPos(new Vector3f(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION, ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION, ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION));
        light.setRadius(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
        light.setFlags(ConversionUtils.parseFlagValue(ConversionUtils.readUnsignedInteger(file), Light.LightFlag.class));
        light.setColor(new Color(file.readUnsignedByte(), file.readUnsignedByte(), file.readUnsignedByte(), file.readUnsignedByte()));

        return light;
    }

    /**
     * Reads the Objects.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readObjects(KwdHeader header, RandomAccessFile file) throws IOException {

        // Read the objects catalog
        logger.info("Reading objects!");
        objects = new HashMap<>(header.getItemCount());
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();
            Object object = new Object();
            byte[] bytes = new byte[32];
            file.read(bytes);
            object.setName(ConversionUtils.bytesToString(bytes).trim());
            object.setMeshResource(readArtResource(file));
            object.setGuiIconResource(readArtResource(file));
            object.setInHandIconResource(readArtResource(file));
            object.setInHandMeshResource(readArtResource(file));
            object.setkUnknownResource(readArtResource(file));
            List<ArtResource> additionalResources = new ArrayList<>(4);
            for (int x = 0; x < 4; x++) {
                ArtResource resource = readArtResource(file);
                if (resource != null) {
                    additionalResources.add(resource);
                }
            }
            object.setAdditionalResources(additionalResources);
            object.setLight(readLight(file));
            object.setWidth(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            object.setHeight(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            object.setMass(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            object.setSpeed(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            object.setAirFriction(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT5_DIVISION);
            object.setMaterial(ConversionUtils.parseEnum(file.readUnsignedByte(), Material.class));
            short[] unknown3 = new short[3];
            for (int x = 0; x < unknown3.length; x++) {
                unknown3[x] = (short) file.readUnsignedByte();
            }
            object.setUnknown3(unknown3);
            object.setFlags(ConversionUtils.parseFlagValue(ConversionUtils.readUnsignedIntegerAsLong(file), Object.ObjectFlag.class));
            object.setHp(ConversionUtils.readUnsignedShort(file));
            object.setMaxAngle(ConversionUtils.readUnsignedShort(file));
            object.setX34c(ConversionUtils.readUnsignedShort(file));
            object.setX34e(ConversionUtils.readUnsignedShort(file));
            object.setTooltipStringId(ConversionUtils.readUnsignedShort(file));
            object.setNameStringId(ConversionUtils.readUnsignedShort(file));
            object.setSlapEffectId(ConversionUtils.readUnsignedShort(file));
            object.setDeathEffectId(ConversionUtils.readUnsignedShort(file));
            object.setMiscEffectId(ConversionUtils.readUnsignedShort(file));
            object.setObjectId((short) file.readUnsignedByte());
            object.setStartState(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Object.State.class));
            object.setRoomCapacity((short) file.readUnsignedByte());
            object.setPickUpPriority((short) file.readUnsignedByte());
            bytes = new byte[32];
            file.read(bytes);
            object.setSoundCategory(ConversionUtils.bytesToString(bytes).trim());

            // Add to the hash by the object ID
            objects.put(object.getObjectId(), object);

            // Check file offset
            checkOffset(header, file, offset);
        }
    }

    /**
     * Reads the CreatureSpells.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readCreatureSpells(KwdHeader header, RandomAccessFile file) throws IOException {

        // Read the creature spells catalog
        logger.info("Reading creature spells!");
        creatureSpells = new HashMap<>(header.getItemCount());
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();
            CreatureSpell creatureSpell = new CreatureSpell();
            byte[] bytes = new byte[32];
            file.read(bytes);
            creatureSpell.setName(ConversionUtils.bytesToString(bytes).trim());
            creatureSpell.setEditorIcon(readArtResource(file));
            creatureSpell.setGuiIcon(readArtResource(file));
            creatureSpell.setShotData1(ConversionUtils.readUnsignedInteger(file));
            creatureSpell.setShotData2(ConversionUtils.readUnsignedInteger(file));
            creatureSpell.setRange(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            creatureSpell.setFlags(ConversionUtils.parseFlagValue(ConversionUtils.readUnsignedInteger(file), CreatureSpell.CreatureSpellFlag.class));
            short[] data2 = new short[2];
            for (int x = 0; x < data2.length; x++) {
                data2[x] = (short) file.readUnsignedByte();
            }
            creatureSpell.setData2(data2);
            creatureSpell.setSoundEvent(ConversionUtils.readUnsignedShort(file));
            creatureSpell.setNameStringId(ConversionUtils.readUnsignedShort(file));
            creatureSpell.setTooltipStringId(ConversionUtils.readUnsignedShort(file));
            creatureSpell.setGeneralDescriptionStringId(ConversionUtils.readUnsignedShort(file));
            creatureSpell.setStrengthStringId(ConversionUtils.readUnsignedShort(file));
            creatureSpell.setWeaknessStringId(ConversionUtils.readUnsignedShort(file));
            creatureSpell.setCreatureSpellId((short) file.readUnsignedByte());
            creatureSpell.setShotTypeId((short) file.readUnsignedByte());
            creatureSpell.setAlternativeShotId((short) file.readUnsignedByte());
            creatureSpell.setAlternativeRoomId((short) file.readUnsignedByte());
            creatureSpell.setRechargeTime(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            creatureSpell.setAlternativeShot(ConversionUtils.parseEnum(file.readUnsignedByte(), CreatureSpell.AlternativeShot.class));
            short[] data3 = new short[27];
            for (int x = 0; x < data3.length; x++) {
                data3[x] = (short) file.readUnsignedByte();
            }
            creatureSpell.setData3(data3);

            // Add to the list
            creatureSpells.put(creatureSpell.getCreatureSpellId(), creatureSpell);

            // Check file offset
            checkOffset(header, file, offset);
        }
    }

    /**
     * Reads the EffectElements.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readEffectElements(KwdHeader header, RandomAccessFile file) throws IOException {

        // Read the effect elements catalog
        logger.info("Reading effect elements!");
        effectElements = new HashMap<>(header.getItemCount());
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();
            EffectElement effectElement = new EffectElement();
            byte[] bytes = new byte[32];
            file.read(bytes);
            effectElement.setName(ConversionUtils.bytesToString(bytes).trim());
            effectElement.setArtResource(readArtResource(file));
            effectElement.setMass(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION);
            effectElement.setAirFriction(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT5_DIVISION);
            effectElement.setElasticity(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT5_DIVISION);
            effectElement.setMinSpeedXy(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION);
            effectElement.setMaxSpeedXy(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION);
            effectElement.setMinSpeedYz(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION);
            effectElement.setMaxSpeedYz(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION);
            effectElement.setMinScale(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            effectElement.setMaxScale(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            effectElement.setScaleRatio(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            effectElement.setFlags(ConversionUtils.parseFlagValue(ConversionUtils.readUnsignedInteger(file), EffectElement.EffectElementFlag.class));
            effectElement.setEffectElementId(ConversionUtils.readUnsignedShort(file));
            effectElement.setMinHp(ConversionUtils.readUnsignedShort(file));
            effectElement.setMaxHp(ConversionUtils.readUnsignedShort(file));
            effectElement.setDeathElementId(ConversionUtils.readUnsignedShort(file));
            effectElement.setHitSolidElementId(ConversionUtils.readUnsignedShort(file));
            effectElement.setHitWaterElementId(ConversionUtils.readUnsignedShort(file));
            effectElement.setHitLavaElementId(ConversionUtils.readUnsignedShort(file));
            effectElement.setColor(new Color(file.readUnsignedByte(), file.readUnsignedByte(), file.readUnsignedByte()));
            effectElement.setRandomColorIndex((short) file.readUnsignedByte());
            effectElement.setTableColorIndex((short) file.readUnsignedByte());
            effectElement.setFadePercentage((short) file.readUnsignedByte());
            effectElement.setNextEffectId(ConversionUtils.readUnsignedShort(file));

            // Add to the hash by the effect element ID
            effectElements.put(effectElement.getEffectElementId(), effectElement);

            // Check file offset
            checkOffset(header, file, offset);
        }
    }

    /**
     * Reads the Effects.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readEffects(KwdHeader header, RandomAccessFile file) throws IOException {

        // Read the effects catalog
        logger.info("Reading effects!");
        effects = new HashMap<>(header.getItemCount());
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();
            Effect effect = new Effect();
            byte[] bytes = new byte[32];
            file.read(bytes);
            effect.setName(ConversionUtils.bytesToString(bytes).trim());
            effect.setArtResource(readArtResource(file));
            effect.setLight(readLight(file));
            effect.setMass(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION);
            effect.setAirFriction(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT5_DIVISION);
            effect.setElasticity(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT5_DIVISION);
            effect.setRadius(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            effect.setMinSpeedXy(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION);
            effect.setMaxSpeedXy(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION);
            effect.setMinSpeedYz(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION);
            effect.setMaxSpeedYz(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION);
            effect.setMinScale(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            effect.setMaxScale(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            effect.setFlags(ConversionUtils.parseFlagValue(ConversionUtils.readUnsignedInteger(file), Effect.EffectFlag.class));
            effect.setEffectId(ConversionUtils.readUnsignedShort(file));
            effect.setMinHp(ConversionUtils.readUnsignedShort(file));
            effect.setMaxHp(ConversionUtils.readUnsignedShort(file));
            effect.setFadeDuration(ConversionUtils.readUnsignedShort(file));
            effect.setNextEffectId(ConversionUtils.readUnsignedShort(file));
            effect.setDeathEffectId(ConversionUtils.readUnsignedShort(file));
            effect.setHitSolidEffectId(ConversionUtils.readUnsignedShort(file));
            effect.setHitWaterEffectId(ConversionUtils.readUnsignedShort(file));
            effect.setHitLavaEffectId(ConversionUtils.readUnsignedShort(file));
            List<Integer> generateIds = new ArrayList<>(8);
            for (int x = 0; x < 8; x++) {
                int id = ConversionUtils.readUnsignedShort(file);
                if (id > 0) {
                    generateIds.add(id);
                }
            }
            effect.setGenerateIds(generateIds);
            effect.setOuterOriginRange(ConversionUtils.readUnsignedShort(file));
            effect.setLowerHeightLimit(ConversionUtils.readUnsignedShort(file));
            effect.setUpperHeightLimit(ConversionUtils.readUnsignedShort(file));
            effect.setOrientationRange(ConversionUtils.readUnsignedShort(file));
            effect.setSpriteSpinRateRange(ConversionUtils.readUnsignedShort(file));
            effect.setWhirlpoolRate(ConversionUtils.readUnsignedShort(file));
            effect.setDirectionalSpread(ConversionUtils.readUnsignedShort(file));
            effect.setCircularPathRate(ConversionUtils.readUnsignedShort(file));
            effect.setInnerOriginRange(ConversionUtils.readUnsignedShort(file));
            effect.setGenerateRandomness(ConversionUtils.readUnsignedShort(file));
            effect.setMisc2(ConversionUtils.readUnsignedShort(file));
            effect.setMisc3(ConversionUtils.readUnsignedShort(file));
            effect.setGenerationType(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Effect.GenerationType.class));
            effect.setElementsPerTurn((short) file.readUnsignedByte());
            effect.setUnknown3(ConversionUtils.readUnsignedShort(file));

            // Add to the hash by the effect ID
            effects.put(effect.getEffectId(), effect);

            // Check file offset
            checkOffset(header, file, offset);
        }
    }

    /**
     * Reads the KeeperSpells.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readKeeperSpells(KwdHeader header, RandomAccessFile file) throws IOException {

        // Read the keeper spells catalog
        logger.info("Reading keeper spells!");
        keeperSpells = new HashMap<>(header.getItemCount());
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();
            KeeperSpell keeperSpell = new KeeperSpell();
            byte[] bytes = new byte[32];
            file.read(bytes);
            keeperSpell.setName(ConversionUtils.bytesToString(bytes).trim());
            keeperSpell.setGuiIcon(readArtResource(file));
            keeperSpell.setEditorIcon(readArtResource(file));
            keeperSpell.setXc8(ConversionUtils.readInteger(file));
            keeperSpell.setRechargeTime(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION);
            keeperSpell.setShotData1(ConversionUtils.readInteger(file));
            keeperSpell.setShotData2(ConversionUtils.readInteger(file));
            keeperSpell.setResearchTime(ConversionUtils.readUnsignedShort(file));
            keeperSpell.setTargetRule(ConversionUtils.parseEnum((short) file.readUnsignedByte(), KeeperSpell.TargetRule.class));
            keeperSpell.setOrderInEditor((short) file.readUnsignedByte());
            keeperSpell.setFlags(ConversionUtils.parseFlagValue(ConversionUtils.readUnsignedInteger(file), KeeperSpell.KeeperSpellFlag.class));
            keeperSpell.setXe0Unreferenced(ConversionUtils.readUnsignedShort(file));
            keeperSpell.setManaDrain(ConversionUtils.readUnsignedShort(file));
            keeperSpell.setTooltipStringId(ConversionUtils.readUnsignedShort(file));
            keeperSpell.setNameStringId(ConversionUtils.readUnsignedShort(file));
            keeperSpell.setGeneralDescriptionStringId(ConversionUtils.readUnsignedShort(file));
            keeperSpell.setStrengthStringId(ConversionUtils.readUnsignedShort(file));
            keeperSpell.setWeaknessStringId(ConversionUtils.readUnsignedShort(file));
            keeperSpell.setKeeperSpellId((short) file.readUnsignedByte());
            keeperSpell.setCastRule(ConversionUtils.parseEnum((short) file.readUnsignedByte(), KeeperSpell.CastRule.class));
            keeperSpell.setShotTypeId((short) file.readUnsignedByte());
            bytes = new byte[32];
            file.read(bytes);
            keeperSpell.setSoundGategory(ConversionUtils.bytesToString(bytes).trim());
            keeperSpell.setBonusRTime(ConversionUtils.readUnsignedShort(file));
            keeperSpell.setBonusShotTypeId((short) file.readUnsignedByte());
            keeperSpell.setBonusShotData1(ConversionUtils.readInteger(file));
            keeperSpell.setBonusShotData2(ConversionUtils.readInteger(file));
            keeperSpell.setManaCost(ConversionUtils.readInteger(file));
            keeperSpell.setBonusIcon(readArtResource(file));
            bytes = new byte[32];
            file.read(bytes);
            keeperSpell.setSoundGategoryGui(ConversionUtils.bytesToString(bytes).trim());
            keeperSpell.setHandAnimId(ConversionUtils.parseEnum((short) file.readUnsignedByte(), KeeperSpell.HandAnimId.class));
            keeperSpell.setNoGoHandAnimId(ConversionUtils.parseEnum((short) file.readUnsignedByte(), KeeperSpell.HandAnimId.class));

            // Add to the hash by the keeper spell ID
            keeperSpells.put(keeperSpell.getKeeperSpellId(), keeperSpell);

            // Check file offset
            checkOffset(header, file, offset);
        }
    }

    /**
     * Reads the *Things.kld
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readThings(KwdHeader header, RandomAccessFile file) throws IOException {

        // Read the requested Things file
        logger.info("Reading things!");
        things = new ArrayList<>(header.getItemCount());
        for (int i = 0; i < header.getItemCount(); i++) {
            Thing thing = null;
            int[] thingTag = new int[2];
            for (int x = 0; x < thingTag.length; x++) {
                thingTag[x] = ConversionUtils.readUnsignedInteger(file);
            }
            long offset = file.getFilePointer();

            // Figure out the type
            switch (thingTag[0]) {
                case 194: {

                    // Object (door & trap crates, objects...)
                    thing = new Thing.Object();
                    ((Thing.Object) thing).setPosX(ConversionUtils.readInteger(file));
                    ((Thing.Object) thing).setPosY(ConversionUtils.readInteger(file));
                    short unknown1[] = new short[12];
                    for (int x = 0; x < unknown1.length; x++) {
                        unknown1[x] = (short) file.readUnsignedByte();
                    }
                    ((Thing.Object) thing).setUnknown1(unknown1);
                    ((Thing.Object) thing).setTriggerId(ConversionUtils.readUnsignedShort(file));
                    ((Thing.Object) thing).setObjectId((short) file.readUnsignedByte());
                    ((Thing.Object) thing).setPlayerId((short) file.readUnsignedByte());
                    break;
                }
                case 195: {

                    // Trap
                    thing = new Thing.Trap();
                    ((Thing.Trap) thing).setPosX(ConversionUtils.readInteger(file));
                    ((Thing.Trap) thing).setPosY(ConversionUtils.readInteger(file));
                    ((Thing.Trap) thing).setUnknown1(ConversionUtils.readInteger(file));
                    ((Thing.Trap) thing).setNumberOfShots((short) file.readUnsignedByte());
                    ((Thing.Trap) thing).setTrapId((short) file.readUnsignedByte());
                    ((Thing.Trap) thing).setPlayerId((short) file.readUnsignedByte());
                    ((Thing.Trap) thing).setUnknown2((short) file.readUnsignedByte());
                    break;
                }
                case 196: {

                    // Door
                    thing = new Thing.Door();
                    ((Thing.Door) thing).setPosX(ConversionUtils.readInteger(file));
                    ((Thing.Door) thing).setPosY(ConversionUtils.readInteger(file));
                    ((Thing.Door) thing).setUnknown1(ConversionUtils.readInteger(file));
                    ((Thing.Door) thing).setTriggerId(ConversionUtils.readUnsignedShort(file));
                    ((Thing.Door) thing).setDoorId((short) file.readUnsignedByte());
                    ((Thing.Door) thing).setPlayerId((short) file.readUnsignedByte());
                    ((Thing.Door) thing).setFlag(ConversionUtils.parseEnum(file.readUnsignedByte(), Thing.Door.DoorFlag.class));
                    short unknown2[] = new short[3];
                    for (int x = 0; x < unknown2.length; x++) {
                        unknown2[x] = (short) file.readUnsignedByte();
                    }
                    ((Thing.Door) thing).setUnknown2(unknown2);
                    break;
                }
                case 197: {

                    // ActionPoint
                    thing = new ActionPoint();
                    ((ActionPoint) thing).setStartX(ConversionUtils.readInteger(file));
                    ((ActionPoint) thing).setStartY(ConversionUtils.readInteger(file));
                    ((ActionPoint) thing).setEndX(ConversionUtils.readInteger(file));
                    ((ActionPoint) thing).setEndY(ConversionUtils.readInteger(file));
                    ((ActionPoint) thing).setWaitDelay(ConversionUtils.readUnsignedShort(file));
                    ((ActionPoint) thing).setFlags(ConversionUtils.parseFlagValue(ConversionUtils.readInteger(file), ActionPointFlag.class));
                    ((ActionPoint) thing).setId((short) file.readUnsignedByte());
                    ((ActionPoint) thing).setNextWaypointId((short) file.readUnsignedByte());
                    byte[] bytes = new byte[32];
                    file.read(bytes);
                    ((ActionPoint) thing).setName(ConversionUtils.bytesToString(bytes).trim());
                    break;
                }
                case 198: {

                    // Neutral creature
                    thing = new Thing.NeutralCreature();
                    ((NeutralCreature) thing).setPosX(ConversionUtils.readInteger(file));
                    ((NeutralCreature) thing).setPosY(ConversionUtils.readInteger(file));
                    ((NeutralCreature) thing).setPosZ(ConversionUtils.readInteger(file));
                    ((NeutralCreature) thing).setGoldHeld(ConversionUtils.readUnsignedShort(file));
                    ((NeutralCreature) thing).setLevel((short) file.readUnsignedByte());
                    ((NeutralCreature) thing).setFlags(ConversionUtils.parseFlagValue((short) file.readUnsignedByte(), Thing.Creature.CreatureFlag.class));
                    ((NeutralCreature) thing).setInitialHealth(ConversionUtils.readInteger(file));
                    ((NeutralCreature) thing).setTriggerId(ConversionUtils.readUnsignedShort(file));
                    ((NeutralCreature) thing).setCreatureId((short) file.readUnsignedByte());
                    ((NeutralCreature) thing).setUnknown1((short) file.readUnsignedByte());
                    break;
                }
                case 199: {

                    // Good creature
                    thing = new Thing.GoodCreature();
                    ((GoodCreature) thing).setPosX(ConversionUtils.readInteger(file));
                    ((GoodCreature) thing).setPosY(ConversionUtils.readInteger(file));
                    ((GoodCreature) thing).setPosZ(ConversionUtils.readInteger(file));
                    ((GoodCreature) thing).setGoldHeld(ConversionUtils.readUnsignedShort(file));
                    ((GoodCreature) thing).setLevel((short) file.readUnsignedByte());
                    ((GoodCreature) thing).setFlags(ConversionUtils.parseFlagValue((short) file.readUnsignedByte(), Thing.Creature.CreatureFlag.class));
                    ((GoodCreature) thing).setObjectiveTargetActionPointId(ConversionUtils.readInteger(file));
                    ((GoodCreature) thing).setInitialHealth(ConversionUtils.readInteger(file));
                    ((GoodCreature) thing).setTriggerId(ConversionUtils.readUnsignedShort(file));
                    ((GoodCreature) thing).setObjectiveTargetPlayerId((short) file.readUnsignedByte());
                    ((GoodCreature) thing).setObjective(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Thing.HeroParty.Objective.class));
                    ((GoodCreature) thing).setCreatureId((short) file.readUnsignedByte());
                    short unknown1[] = new short[2];
                    for (int x = 0; x < unknown1.length; x++) {
                        unknown1[x] = (short) file.readUnsignedByte();
                    }
                    ((GoodCreature) thing).setUnknown1(unknown1);
                    ((GoodCreature) thing).setFlags2(ConversionUtils.parseFlagValue((short) file.readUnsignedByte(), Thing.Creature.CreatureFlag2.class));
                    break;
                }
                case 200: {

                    // Creature
                    thing = new Thing.KeeperCreature();
                    ((KeeperCreature) thing).setPosX(ConversionUtils.readInteger(file));
                    ((KeeperCreature) thing).setPosY(ConversionUtils.readInteger(file));
                    ((KeeperCreature) thing).setPosZ(ConversionUtils.readInteger(file));
                    ((KeeperCreature) thing).setGoldHeld(ConversionUtils.readUnsignedShort(file));
                    ((KeeperCreature) thing).setLevel((short) file.readUnsignedByte());
                    ((KeeperCreature) thing).setFlags(ConversionUtils.parseFlagValue((short) file.readUnsignedByte(), KeeperCreature.CreatureFlag.class));
                    ((KeeperCreature) thing).setInitialHealth(ConversionUtils.readInteger(file));
                    ((KeeperCreature) thing).setObjectiveTargetActionPointId(ConversionUtils.readInteger(file));
                    ((KeeperCreature) thing).setTriggerId(ConversionUtils.readUnsignedShort(file));
                    ((KeeperCreature) thing).setCreatureId((short) file.readUnsignedByte());
                    ((KeeperCreature) thing).setPlayerId((short) file.readUnsignedByte());
                    break;
                }
                case 201: {

                    // HeroParty
                    thing = new HeroParty();
                    byte[] bytes = new byte[32];
                    file.read(bytes);
                    ((HeroParty) thing).setName(ConversionUtils.bytesToString(bytes).trim());
                    ((HeroParty) thing).setTriggerId(ConversionUtils.readUnsignedShort(file));
                    ((HeroParty) thing).setId((short) file.readUnsignedByte());
                    ((HeroParty) thing).setX23(ConversionUtils.readInteger(file));
                    ((HeroParty) thing).setX27(ConversionUtils.readInteger(file));
                    List<GoodCreature> heroPartyMembers = new ArrayList<>(16);
                    for (int x = 0; x < 16; x++) {
                        GoodCreature creature = new GoodCreature();
                        creature.setPosX(ConversionUtils.readInteger(file));
                        creature.setPosY(ConversionUtils.readInteger(file));
                        creature.setPosZ(ConversionUtils.readInteger(file));
                        creature.setGoldHeld(ConversionUtils.readUnsignedShort(file));
                        creature.setLevel((short) file.readUnsignedByte());
                        creature.setFlags(ConversionUtils.parseFlagValue((short) file.readUnsignedByte(), KeeperCreature.CreatureFlag.class));
                        creature.setObjectiveTargetActionPointId(ConversionUtils.readInteger(file));
                        creature.setInitialHealth(ConversionUtils.readInteger(file));
                        creature.setTriggerId(ConversionUtils.readUnsignedShort(file));
                        creature.setObjectiveTargetPlayerId((short) file.readUnsignedByte());
                        creature.setObjective(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Thing.HeroParty.Objective.class));
                        creature.setCreatureId((short) file.readUnsignedByte());
                        short unknown1[] = new short[2];
                        for (int index = 0; index < unknown1.length; index++) {
                            unknown1[index] = (short) file.readUnsignedByte();
                        }
                        creature.setUnknown1(unknown1);
                        creature.setFlags2(ConversionUtils.parseFlagValue((short) file.readUnsignedByte(), Thing.Creature.CreatureFlag2.class));

                        // If creature id is 0, it is safe to say this is not a valid entry
                        if (creature.getCreatureId() > 0) {
                            heroPartyMembers.add(creature);
                        }
                    }
                    ((HeroParty) thing).setHeroPartyMembers(heroPartyMembers);
                    break;
                }
                case 202: {

                    // Dead body
                    thing = new Thing.DeadBody();
                    ((Thing.DeadBody) thing).setPosX(ConversionUtils.readInteger(file));
                    ((Thing.DeadBody) thing).setPosY(ConversionUtils.readInteger(file));
                    ((Thing.DeadBody) thing).setPosZ(ConversionUtils.readInteger(file));
                    ((Thing.DeadBody) thing).setGoldHeld(ConversionUtils.readUnsignedShort(file));
                    ((Thing.DeadBody) thing).setCreatureId((short) file.readUnsignedByte());
                    ((Thing.DeadBody) thing).setPlayerId((short) file.readUnsignedByte());
                    break;
                }
                case 203: {

                    // Effect generator
                    thing = new Thing.EffectGenerator();
                    ((Thing.EffectGenerator) thing).setPosX(ConversionUtils.readInteger(file));
                    ((Thing.EffectGenerator) thing).setPosY(ConversionUtils.readInteger(file));
                    ((Thing.EffectGenerator) thing).setX08(ConversionUtils.readInteger(file));
                    ((Thing.EffectGenerator) thing).setX0c(ConversionUtils.readInteger(file));
                    ((Thing.EffectGenerator) thing).setX10(ConversionUtils.readUnsignedShort(file));
                    ((Thing.EffectGenerator) thing).setX12(ConversionUtils.readUnsignedShort(file));
                    List<Integer> effectIds = new ArrayList<>(4);
                    for (int x = 0; x < 4; x++) {
                        int effectId = ConversionUtils.readUnsignedShort(file);
                        if (effectId > 0) {
                            effectIds.add(effectId);
                        }
                    }
                    ((Thing.EffectGenerator) thing).setEffectIds(effectIds);
                    ((Thing.EffectGenerator) thing).setFrequency((short) file.readUnsignedByte());
                    ((Thing.EffectGenerator) thing).setId((short) file.readUnsignedByte());
                    short[] pad = new short[6];
                    for (int x = 0; x < pad.length; x++) {
                        pad[x] = (short) file.readUnsignedByte();
                    }
                    ((Thing.EffectGenerator) thing).setPad(pad);
                    break;
                }
                case 204: {

                    // Room
                    thing = new Thing.Room();
                    ((Thing.Room) thing).setPosX(ConversionUtils.readInteger(file));
                    ((Thing.Room) thing).setPosY(ConversionUtils.readInteger(file));
                    ((Thing.Room) thing).setX08(ConversionUtils.readInteger(file));
                    ((Thing.Room) thing).setX0c(ConversionUtils.readUnsignedShort(file));
                    ((Thing.Room) thing).setDirection(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Thing.Room.Direction.class));
                    ((Thing.Room) thing).setX0f((short) file.readUnsignedByte());
                    ((Thing.Room) thing).setInitialHealth(ConversionUtils.readUnsignedShort(file));
                    ((Thing.Room) thing).setRoomType(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Thing.Room.RoomType.class));
                    ((Thing.Room) thing).setPlayerId((short) file.readUnsignedByte());
                    break;
                }
                case 205: {

                    // Thing12 -- not tested
                    thing = new Thing12();
                    ((Thing12) thing).setX00(new Vector3f(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION, ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION, ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION));
                    ((Thing12) thing).setX0c(new Vector3f(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION, ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION, ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION));
                    ((Thing12) thing).setX18(new Vector3f(ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION, ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION, ConversionUtils.readInteger(file) / FIXED_POINT_DIVISION));
                    ((Thing12) thing).setX24(ConversionUtils.readInteger(file));
                    ((Thing12) thing).setX28(ConversionUtils.readInteger(file));
                    ((Thing12) thing).setX2c(ConversionUtils.readInteger(file));
                    ((Thing12) thing).setX30(ConversionUtils.readInteger(file));
                    ((Thing12) thing).setX34(ConversionUtils.readInteger(file));
                    ((Thing12) thing).setX38(ConversionUtils.readInteger(file));
                    ((Thing12) thing).setX3c(ConversionUtils.readInteger(file));
                    ((Thing12) thing).setX40(ConversionUtils.readInteger(file));
                    ((Thing12) thing).setX44(ConversionUtils.readInteger(file));
                    ((Thing12) thing).setX48(ConversionUtils.readInteger(file));
                    ((Thing12) thing).setX4c(ConversionUtils.readUnsignedShort(file));
                    ((Thing12) thing).setX4e(ConversionUtils.readUnsignedShort(file));
                    ((Thing12) thing).setX50(ConversionUtils.readUnsignedShort(file));
                    ((Thing12) thing).setId((short) file.readUnsignedByte());
                    break;
                }
                default: {

                    // Just skip the bytes
                    file.skipBytes(thingTag[1]);
                    logger.log(Level.WARNING, "Unsupported thing type {0}!", thingTag[0]);
                }
            }

            System.out.println(thingTag[0] + " type");

            // Add to the list
            things.add(thing);

            // Check file offset
            checkOffset(thingTag[1], file, offset);
        }
    }

    /**
     * Reads the Shots.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readShots(KwdHeader header, RandomAccessFile file) throws IOException {

        // Read the shots catalog
        logger.info("Reading shots!");
        shots = new HashMap<>(header.getItemCount());
        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();

            // One shot is 239 bytes
            Shot shot = new Shot();
            byte[] bytes = new byte[32];
            file.read(bytes);
            shot.setName(ConversionUtils.bytesToString(bytes).trim());
            shot.setMeshResource(readArtResource(file));
            shot.setLight(readLight(file));
            shot.setAirFriction(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT5_DIVISION);
            shot.setMass(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            shot.setSpeed(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            shot.setData1(ConversionUtils.readUnsignedInteger(file));
            shot.setData2(ConversionUtils.readUnsignedInteger(file));
            shot.setShotProcessFlags(ConversionUtils.parseFlagValue(ConversionUtils.readUnsignedInteger(file), Shot.ShotProcessFlag.class));
            shot.setRadius(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);
            shot.setFlags(ConversionUtils.parseFlagValue(ConversionUtils.readUnsignedInteger(file), Shot.ShotFlag.class));
            shot.setGeneralEffectId(ConversionUtils.readUnsignedShort(file));
            shot.setCreationEffectId(ConversionUtils.readUnsignedShort(file));
            shot.setDeathEffectId(ConversionUtils.readUnsignedShort(file));
            shot.setTimedEffectId(ConversionUtils.readUnsignedShort(file));
            shot.setHitSolidEffectId(ConversionUtils.readUnsignedShort(file));
            shot.setHitLavaEffectId(ConversionUtils.readUnsignedShort(file));
            shot.setHitWaterEffect(ConversionUtils.readUnsignedShort(file));
            shot.setHitThingEffectId(ConversionUtils.readUnsignedShort(file));
            shot.setHealth(ConversionUtils.readUnsignedShort(file));
            shot.setShotId((short) file.readUnsignedByte());
            shot.setDeathShotId((short) file.readUnsignedByte());
            shot.setTimedDelay((short) file.readUnsignedByte());
            shot.setHitSolidShotId((short) file.readUnsignedByte());
            shot.setHitLavaShotId((short) file.readUnsignedByte());
            shot.setHitWaterShotId((short) file.readUnsignedByte());
            shot.setHitThingShotId((short) file.readUnsignedByte());
            shot.setDamageType(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Shot.DamageType.class));
            shot.setCollideType(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Shot.CollideType.class));
            shot.setProcessType(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Shot.ProcessType.class));
            shot.setAttackCategory(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Shot.AttackCategory.class));
            bytes = new byte[32];
            file.read(bytes);
            shot.setSoundCategory(ConversionUtils.bytesToString(bytes).trim());
            shot.setThreat(ConversionUtils.readUnsignedShort(file));
            shot.setBurnDuration(ConversionUtils.readUnsignedInteger(file) / FIXED_POINT_DIVISION);

            // Add to the hash by the shot ID
            shots.put(shot.getShotId(), shot);

            // Check file offset
            checkOffset(header, file, offset);
        }
    }

    /**
     * Reads the *Triggers.kld
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readTriggers(KwdHeader header, RandomAccessFile file) throws IOException {

        // Read the requested Triggers file
        logger.info("Reading triggers!");
        triggers = new HashMap<>(header.getItemCount());
        for (int i = 0; i < header.getItemCount(); i++) {
            Trigger trigger = null;
            int[] triggerTag = new int[2];
            for (int x = 0; x < triggerTag.length; x++) {
                triggerTag[x] = ConversionUtils.readUnsignedInteger(file);
            }
            long offset = file.getFilePointer();

            // Figure out the type
            switch (triggerTag[0]) {
                case 213: {

                    // TriggerGeneric
                    trigger = new TriggerGeneric(this);
                    ((TriggerGeneric) trigger).setTargetValueComparison(ConversionUtils.parseEnum((short) file.readUnsignedByte(), TriggerGeneric.ComparisonType.class));
                    ((TriggerGeneric) trigger).setTargetFlagId((short) file.readUnsignedByte());
                    short targetValueType = (short) file.readUnsignedByte();
                    ((TriggerGeneric) trigger).setTargetValueFlagId((short) file.readUnsignedByte());
                    ((TriggerGeneric) trigger).setTargetValue(ConversionUtils.readInteger(file));
                    ((TriggerGeneric) trigger).setId(ConversionUtils.readUnsignedShort(file));
                    ((TriggerGeneric) trigger).setIdNext(ConversionUtils.readUnsignedShort(file)); // SiblingID
                    ((TriggerGeneric) trigger).setIdChild(ConversionUtils.readUnsignedShort(file)); // ChildID
                    ((TriggerGeneric) trigger).setTarget(ConversionUtils.parseEnum((short) file.readUnsignedByte(), TriggerGeneric.TargetType.class));
                    ((TriggerGeneric) trigger).setRepeatTimes((short) file.readUnsignedByte());
                    if (TriggerGeneric.TargetType.SLAP_TYPES.equals(((TriggerGeneric) trigger).getTarget())) {

                        // The target value type is actually a terrain ID
                        ((TriggerGeneric) trigger).setTargetValueType(TriggerGeneric.TargetValueType.TERRAIN_ID);
                        ((TriggerGeneric) trigger).setTerrainId(targetValueType);
                    } else {

                        // Assign type normally
                        ((TriggerGeneric) trigger).setTargetValueType(ConversionUtils.parseEnum(targetValueType, TriggerGeneric.TargetValueType.class));
                    }
                    break;
                }
                case 214: {

                    // TriggerAction
                    trigger = new TriggerAction(this);
                    ((TriggerAction) trigger).setActionTargetId((short) file.readUnsignedByte());
                    ((TriggerAction) trigger).setPlayerId((short) file.readUnsignedByte());
                    ((TriggerAction) trigger).setCreatureLevel((short) file.readUnsignedByte());
                    ((TriggerAction) trigger).setAvailable((short) file.readUnsignedByte());
                    ((TriggerAction) trigger).setActionTargetValue1(ConversionUtils.readUnsignedShort(file));
                    ((TriggerAction) trigger).setActionTargetValue2(ConversionUtils.readUnsignedShort(file));
                    ((TriggerAction) trigger).setId(ConversionUtils.readUnsignedShort(file)); // ID
                    ((TriggerAction) trigger).setIdNext(ConversionUtils.readUnsignedShort(file)); // SiblingID
                    short[] unknown1 = new short[2];
                    for (int x = 0; x < unknown1.length; x++) {
                        unknown1[x] = (short) file.readUnsignedByte();
                    }
                    ((TriggerAction) trigger).setUnknown1(unknown1);
                    ((TriggerAction) trigger).setActionType(ConversionUtils.parseEnum(ConversionUtils.readUnsignedShort(file), TriggerAction.ActionType.class));
                    break;
                }
                default: {

                    // Just skip the bytes
                    file.skipBytes(triggerTag[1]);
                    logger.log(Level.WARNING, "Unsupported trigger type {0}!", triggerTag[0]);
                }
            }

            // Add to the list
            if (trigger != null) {
                triggers.put(trigger.getId(), trigger);
            }

            // Check file offset
            checkOffset(triggerTag[1], file, offset);
        }
    }

    /**
     * Reads the *Variables.kld
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readVariables(KwdHeader header, RandomAccessFile file) throws IOException {

        // Read the requested VARIABLES file
        // Should be the GlobalVariables first, then the level's own
        // TODO: Overriding
        logger.info("Reading variables!");
        if (variables == null) {
            variables = new ArrayList<>(header.getItemCount());
        }

        for (int i = 0; i < header.getItemCount(); i++) {
            int id = ConversionUtils.readInteger(file);
            Variable variable;

            switch (id) {
                case Variable.CREATURE_POOL:
                    variable = new Variable.CreaturePool();
                    ((Variable.CreaturePool) variable).setCreatureId(ConversionUtils.readInteger(file));
                    ((Variable.CreaturePool) variable).setValue(ConversionUtils.readInteger(file));
                    ((Variable.CreaturePool) variable).setPlayerId(ConversionUtils.readInteger(file));
                    break;

                case Variable.AVAILABILITY:
                    variable = new Variable.Availability();
                    ((Variable.Availability) variable).setType(ConversionUtils.parseEnum(ConversionUtils.readUnsignedShort(file), Variable.Availability.AvailabilityType.class));
                    ((Variable.Availability) variable).setPlayerId(ConversionUtils.readUnsignedShort(file));
                    ((Variable.Availability) variable).setTypeId(ConversionUtils.readInteger(file));
                    ((Variable.Availability) variable).setValue(ConversionUtils.parseEnum(ConversionUtils.readInteger(file), Variable.Availability.AvailabilityValue.class));
                    break;

                case Variable.SACRIFICES_ID: // not changeable (in editor you can, but changes will not save)
                    variable = new Variable.Sacrifice();
                    ((Variable.Sacrifice) variable).setType1(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Variable.SacrificeType.class));
                    ((Variable.Sacrifice) variable).setId1((short) file.readUnsignedByte());
                    ((Variable.Sacrifice) variable).setType2(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Variable.SacrificeType.class));
                    ((Variable.Sacrifice) variable).setId2((short) file.readUnsignedByte());
                    ((Variable.Sacrifice) variable).setType3(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Variable.SacrificeType.class));
                    ((Variable.Sacrifice) variable).setId3((short) file.readUnsignedByte());

                    ((Variable.Sacrifice) variable).setRewardType(ConversionUtils.parseEnum((short) file.readUnsignedByte(), Variable.SacrificeRewardType.class));
                    ((Variable.Sacrifice) variable).setSpeechId((short) file.readUnsignedByte());
                    ((Variable.Sacrifice) variable).setRewardValue(ConversionUtils.readInteger(file));
                    break;

                case Variable.CREATURE_STATS_ID:
                    variable = new Variable.CreatureStats();
                    ((Variable.CreatureStats) variable).setStatId(ConversionUtils.readInteger(file));
                    ((Variable.CreatureStats) variable).setValue(ConversionUtils.readInteger(file));
                    ((Variable.CreatureStats) variable).setLevel(ConversionUtils.readInteger(file));
                    break;

                case Variable.CREATURE_FIRST_PERSON_ID:
                    variable = new Variable.CreatureFirstPerson();
                    ((Variable.CreatureFirstPerson) variable).setStatId(ConversionUtils.readInteger(file));
                    ((Variable.CreatureFirstPerson) variable).setValue(ConversionUtils.readInteger(file));
                    ((Variable.CreatureFirstPerson) variable).setLevel(ConversionUtils.readInteger(file));
                    break;

                case 61:
                case 62:
                case 63:
                case Variable.UNKNOWN_2:
                case 66:
                default:
                    variable = new Variable.MiscVariable();
                    ((Variable.MiscVariable) variable).setVariableId(id);
                    ((Variable.MiscVariable) variable).setValue(ConversionUtils.readInteger(file));
                    ((Variable.MiscVariable) variable).setUnknown1(ConversionUtils.readInteger(file));
                    ((Variable.MiscVariable) variable).setUnknown2(ConversionUtils.readInteger(file));
                    break;
            }

            // Add to the list
            variables.add(variable);
        }
    }

    /**
     * Get list of different terrain tiles
     *
     * @return list of terrain tiles
     */
    public Collection<Terrain> getTerrainList() {
        return terrainTiles.values();
    }

    /**
     * Get list of different objects
     *
     * @return list of objects
     */
    public Collection<Object> getObjectList() {
        return objects.values();
    }

    /**
     * Get the player with the specified ID
     *
     * @param id the id of player
     * @return the player
     */
    public Player getPlayer(short id) {
        return players.get(id);
    }

    /**
     * Get the creature with the specified ID
     *
     * @param id the id of creature
     * @return the creature
     */
    public Creature getCreature(short id) {
        return creatures.get(id);
    }

    /**
     * Get the map tiles
     *
     * @return the map tiles
     */
    public Tile[][] getTiles() {
        return map.getTiles();
    }

    /**
     * Get single map tile in specified coordinates
     *
     * @param x the x
     * @param y the y
     * @return the tile in given coordinate
     */
    public Tile getTile(int x, int y) {
        return map.getTile(x, y);
    }

    /**
     * Get the terrain with the specified ID
     *
     * @param id the id of terrain
     * @return the terrain
     */
    public Terrain getTerrain(short id) {
        return terrainTiles.get(id);
    }

    /**
     * Get the room with the specified terrain ID
     *
     * @param id the id of terrain
     * @return the room associated with the terrain ID
     */
    public Room getRoomByTerrain(short id) {
        return roomsByTerrainId.get(id);
    }

    /**
     * Get list of things
     *
     * @return things
     */
    public List<Thing> getThings() {
        return things;
    }

    /**
     * Get the trigger/action with the specified ID
     *
     * @param id the id of trigger/action
     * @return the trigger/action
     */
    public Trigger getTrigger(int id) {
        return triggers.get(id);
    }

    /**
     * Get the object with the specified ID
     *
     * @param id the id of object
     * @return the object
     */
    public Object getObject(int id) {
        return objects.get((short) id);
    }

    /**
     * Get the room with the specified ID
     *
     * @param id the id of room
     * @return the room
     */
    public Room getRoomById(int id) {
        return rooms.get((short) id);
    }

    /**
     * Get the keeper spell with the specified ID
     *
     * @param id the id of keeper spell
     * @return the keeper spell
     */
    public KeeperSpell getKeeperSpellById(int id) {
        return keeperSpells.get((short) id);
    }

    /**
     * Get the trap with the specified ID
     *
     * @param id the id of trap
     * @return the trap
     */
    public Trap getTrapById(int id) {
        return traps.get((short) id);
    }

    /**
     * Get the door with the specified ID
     *
     * @param id the id of door
     * @return the door
     */
    public Door getDoorById(int id) {
        return doors.get((short) id);
    }

    /**
     * Get the list of all rooms
     *
     * @return list of all rooms
     */
    public List<Room> getRooms() {
        List<Room> c = new ArrayList(rooms.values());
        Collections.sort(c);
        return c;
    }

    /**
     * Get the list of all keeper spells
     *
     * @return list of all keeper spells
     */
    public List<KeeperSpell> getKeeperSpells() {
        List<KeeperSpell> c = new ArrayList(keeperSpells.values());
        Collections.sort(c);
        return c;
    }

    /**
     * Get the lava terrain tile
     *
     * @return lava
     */
    public Terrain getLava() {
        return lava;
    }

    /**
     * Get the water terrain tile
     *
     * @return water
     */
    public Terrain getWater() {
        return water;
    }

    /**
     * Get the claimed path terrain tile
     *
     * @return claimed path
     */
    public Terrain getClaimedPath() {
        return claimedPath;
    }

    /**
     * Reads a DK2 style timestamp
     *
     * @param file the file to read from
     * @return the date in current locale
     * @throws IOException may fail
     */
    private Date readTimestamp(RandomAccessFile file) throws IOException {

        // Dates are in UTC
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.YEAR, ConversionUtils.readUnsignedShort(file));
        cal.set(Calendar.DAY_OF_MONTH, file.readUnsignedByte());
        cal.set(Calendar.MONTH, file.readUnsignedByte());
        file.skipBytes(2);
        cal.set(Calendar.HOUR_OF_DAY, file.readUnsignedByte());
        cal.set(Calendar.MINUTE, file.readUnsignedByte());
        cal.set(Calendar.SECOND, file.readUnsignedByte());
        file.skipBytes(1);
        return cal.getTime();
    }

    /**
     * Not all the data types are of the length that suits us, do our best to
     * ignore it<br>
     * Skips the file to the correct position after an item is read<br>
     * <b>Use this with the common types!</b>
     *
     * @see #checkOffset(long, java.io.RandomAccessFile, long)
     * @param header the header
     * @param file the file
     * @param offset the file offset before the last item was read
     */
    private void checkOffset(KwdHeader header, RandomAccessFile file, long offset) throws IOException {
        checkOffset(header.getItemSize(), file, offset);
    }

    /**
     * Not all the data types are of the length that suits us, do our best to
     * ignore it<br>
     * Skips the file to the correct position after an item is read<br>
     * <b>Use this directly with Things & Triggers!</b>
     *
     * @see
     * #checkOffset(toniarts.opendungeonkeeper.tools.convert.map.KwdFile.KwdHeader,
     * java.io.RandomAccessFile, long)
     * @param itemSize the item size
     * @param file the file
     * @param offset the file offset before the last item was read
     */
    private void checkOffset(long itemSize, RandomAccessFile file, long offset) throws IOException {
        long wantedOffset = offset + itemSize;
        if (file.getFilePointer() != wantedOffset) {
            logger.log(Level.WARNING, "Record size differs from expected! File offset is {0} and should be {1}!", new java.lang.Object[]{file.getFilePointer(), wantedOffset});
            file.seek(wantedOffset);
        }
    }

    /**
     * Get level flags
     *
     * @return level flags
     */
    public EnumSet<LevFlag> getLvlFlags() {
        return lvlFlags;
    }

    /**
     * Get number of players supported by the map
     *
     * @return player count
     */
    public short getPlayerCount() {
        return playerCount;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Kwd header, few different kinds, handles all
     */
    private class KwdHeader {

//            struct kwdHeader {
//                unsigned int id;
//                unsigned int size;
//                union {
//                struct {
//                uint16_t w08;
//                uint16_t w0a;
//                } level;
//                unsigned int dw08;
//                };
//                unsigned int x0c[7];
//                };
        private MapDataTypeEnum id;
        private long size;
        private int headerSize = 56; // Well, header and the id data
        private int width;
        private int height;
        private int itemCount;

        public KwdHeader() {
        }

        public MapDataTypeEnum getId() {
            return id;
        }

        protected void setId(MapDataTypeEnum id) {
            this.id = id;
        }

        public long getSize() {
            return size;
        }

        protected void setSize(long size) {
            this.size = size;
        }

        public int getHeaderSize() {
            return headerSize;
        }

        protected void setHeaderSize(int headerSize) {
            this.headerSize = headerSize;
        }

        public int getWidth() {
            return width;
        }

        protected void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        protected void setHeight(int height) {
            this.height = height;
        }

        public int getItemCount() {
            return itemCount;
        }

        protected void setItemCount(int itemCount) {
            this.itemCount = itemCount;
        }

        /**
         * Get the individiual item size (warning, does not apply to all!)
         *
         * @return
         */
        public long getItemSize() {
            return (getSize() - getHeaderSize()) / getItemCount();
        }
    }
}
