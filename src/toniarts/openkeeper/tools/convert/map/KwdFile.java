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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.IResourceReader;
import toniarts.openkeeper.tools.convert.ResourceReader;
import toniarts.openkeeper.tools.convert.map.ArtResource.ArtResourceType;
import toniarts.openkeeper.tools.convert.map.Creature.AnimationType;
import toniarts.openkeeper.tools.convert.map.Creature.Attraction;
import toniarts.openkeeper.tools.convert.map.Creature.JobAlternative;
import toniarts.openkeeper.tools.convert.map.Creature.OffsetType;
import toniarts.openkeeper.tools.convert.map.Creature.Spell;
import toniarts.openkeeper.tools.convert.map.Creature.X1323;
import toniarts.openkeeper.tools.convert.map.Door.DoorFlag;
import toniarts.openkeeper.tools.convert.map.GameLevel.LevFlag;
import toniarts.openkeeper.tools.convert.map.GameLevel.LevelReward;
import toniarts.openkeeper.tools.convert.map.GameLevel.TextTable;
import static toniarts.openkeeper.tools.convert.map.MapDataTypeEnum.MAP;
import toniarts.openkeeper.tools.convert.map.Thing.ActionPoint;
import toniarts.openkeeper.tools.convert.map.Thing.ActionPoint.ActionPointFlag;
import toniarts.openkeeper.tools.convert.map.Thing.GoodCreature;
import toniarts.openkeeper.tools.convert.map.Thing.HeroParty;
import toniarts.openkeeper.tools.convert.map.Thing.KeeperCreature;
import toniarts.openkeeper.tools.convert.map.Thing.NeutralCreature;
import toniarts.openkeeper.tools.convert.map.Variable.Availability;
import toniarts.openkeeper.tools.convert.map.Variable.CreatureFirstPerson;
import toniarts.openkeeper.tools.convert.map.Variable.CreaturePool;
import toniarts.openkeeper.tools.convert.map.Variable.CreatureStats;
import toniarts.openkeeper.tools.convert.map.Variable.CreatureStats.StatType;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.Sacrifice;
import toniarts.openkeeper.utils.PathUtils;

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

    // These are needed in various places, I don't know how to else regognize these
    private final static short ROOM_PORTAL_ID = 3;
    private final static short TRIGGER_GENERIC = 213;
    private final static short TRIGGER_ACTION = 214;

    private final static short THING_OBJECT = 194;
    private final static short THING_TRAP = 195;
    private final static short THING_DOOR = 196;
    private final static short THING_ACTION_POINT = 197;
    private final static short THING_NEUTRAL_CREATURE = 198;
    private final static short THING_GOOD_CREATURE = 199;
    private final static short THING_KEEPER_CREATURE = 200;
    private final static short THING_HERO_PARTY = 201;
    private final static short THING_DEAD_BODY = 202;
    private final static short THING_EFFECT_GENERATOR = 203;
    private final static short THING_ROOM = 204;
    private final static short THING_CAMERA = 205;

    private final static short HEADER_SIZE = 25603;
    private final static long CREATURE_SIZE = 5536L;

    private GameLevel gameLevel;
    private GameMap map;
    private Map<Short, Player> players;
    private Map<Short, Terrain> terrainTiles;
    private Map<Short, Door> doors;
    private Map<Short, Trap> traps;
    private Map<Short, Room> rooms;
    private Map<Short, Room> roomsByTerrainId; // Maps have rooms by the terrain ID
    private Map<Short, Creature> creatures;
    private Map<Short, GameObject> objects;
    private Map<Short, CreatureSpell> creatureSpells;
    private Map<Integer, EffectElement> effectElements;
    private Map<Integer, Effect> effects;
    private Map<Short, KeeperSpell> keeperSpells;
    private Map<Class<? extends Thing>, List<? extends Thing>> thingsByType;
    private Map<Short, Shot> shots;
    private Map<Integer, Trigger> triggers;
    // Variables
    private List<Availability> availabilities;
    private Map<Integer, Map<Integer, CreaturePool>> creaturePools;
    private Map<Integer, Map<StatType, CreatureStats>> creatureStatistics;
    private Map<Integer, Map<StatType, CreatureFirstPerson>> creatureFirstPersonStatistics;
    private Map<MiscVariable.MiscType, MiscVariable> variables;
    private Set<Sacrifice> sacrifices;
    private Set<Variable.Unknown> unknownVariables;
    //
    private boolean loaded = false;
    private Creature imp;
    private Creature dwarf;
    private final String basePath;
    private GameObject levelGem;

    private final Object loadingLock = new Object();
    private static final Logger LOGGER = Logger.getLogger(KwdFile.class.getName());

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
        // Read the file
        try {
            readFileContents(file);
        } catch (Exception e) {
            //Fug
            throw new RuntimeException("Failed to read the file " + file + "!", e);
        }
        this.basePath = PathUtils.fixFilePath(basePath);

        // See if we need to load the actual data
        if (load) {
            load();
        } else {

            // We need map width & height if not loaded fully, I couldn't figure out where, except the map data
            try (IResourceReader data = new ResourceReader(ConversionUtils.getRealFileName(basePath, gameLevel.getFile(MAP)))) {
                KwdHeader header = readKwdHeader(data);
                map = new GameMap(header.getWidth(), header.getHeight());
            } catch (Exception e) {

                //Fug
                throw new RuntimeException("Failed to read the file " + gameLevel.getFile(MAP) + "!", e);
            }
        }
    }

    private void readFileContents(File file) throws IOException {
        try (IResourceReader data = new ResourceReader(file)) {
            while (data.getFilePointer() < data.length()) {

                // Read header (and put the file pointer to the data start)
                KwdHeader header = readKwdHeader(data);
                readFileContents(header, data);
            }

            if (data.getFilePointer() != data.length()) {
                throw new RuntimeException("Failed to parse file");
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
            synchronized (loadingLock) {
                if (!loaded) {

                    // Read the map data first (we store some data to the map)
                    for (FilePath path : gameLevel.getPaths()) {
                        if (path.getId() == MapDataTypeEnum.MAP) {
                            readFilePath(path);
                            break;
                        }
                    }

                    // Now we have the paths, read all of those in order
                    for (FilePath path : gameLevel.getPaths()) {
                        if (path.getId() != MapDataTypeEnum.MAP) {
                            // Open the file
                            readFilePath(path);
                        }
                    }
                    loaded = true;
                }
            }
        }
    }

    private void readFilePath(FilePath path) {
        File file = null;
        try {
            file = new File(ConversionUtils.getRealFileName(basePath, path.getPath()));
            readFileContents(file);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read the file " + file + "!", e);
        }
    }

    /**
     * Reads the common KWD header
     *
     * @param data the data file
     * @return the header
     * @throws IOException may fail reading
     */
    private KwdHeader readKwdHeader(IResourceReader data) throws IOException {

        KwdHeader header = new KwdHeader();
        header.setId(data.readIntegerAsEnum(MapDataTypeEnum.class));
        int size = data.readUnsignedInteger(); // Bytes in the real size indicator, well seems to be 4 always
        if (size == 2) {
            header.setSize(data.readUnsignedShort());
        } else if (size == 4) {
            header.setSize(data.readUnsignedInteger());
        }
        header.setCheckOne(data.readUnsignedInteger());
        header.setHeaderEndOffset(data.readUnsignedInteger());
        //Mark the position
        long offset = data.getFilePointer();

        switch (header.getId()) {
            case MAP:
                header.setHeaderSize(36);
                header.setWidth(data.readUnsignedInteger());
                header.setHeight(data.readUnsignedInteger());
                break;

            case TRIGGERS:
                header.setHeaderSize(60);
                header.setItemCount(data.readUnsignedInteger() + data.readUnsignedInteger());
                header.setUnknown(data.readUnsignedInteger());

                header.setDateCreated(data.readTimestamp());
                header.setDateModified(data.readTimestamp());
                break;

            case LEVEL:
                header.setItemCount(data.readUnsignedShort());
                header.setHeight(data.readUnsignedShort());
                header.setUnknown(data.readUnsignedInteger());

                header.setDateCreated(data.readTimestamp());
                header.setDateModified(data.readTimestamp());
                break;
            default:
                header.setItemCount(data.readUnsignedInteger());
                header.setUnknown(data.readUnsignedInteger());

                header.setDateCreated(data.readTimestamp());
                header.setDateModified(data.readTimestamp());
                break;
        }

        if (data.getFilePointer() != offset + header.getHeaderEndOffset()) {
            LOGGER.warning("Incorrect parsing of file header");
        }
        //header.setHeaderSize(28 + header.getHeaderEndOffset());
        header.setCheckTwo(data.readUnsignedInteger());
        header.setDataSize(data.readUnsignedInteger());

        return header;
    }

    private void readFileContents(KwdHeader header, IResourceReader data) throws IOException {
        // Handle all the cases (we kinda skip the globals with this logic, so no need)
        // All readers must read the whole data they intend to read
        switch (header.getId()) {
            case LEVEL:
                // check header.getCheckOne() != 221 || header.getCheckTwo() != 223
                readMapInfo(header, data);
                break;

            case CREATURES:
                // check header.getCheckOne() != 171 || header.getCheckTwo() != 172
                readCreatures(header, data);
                break;

            case CREATURE_SPELLS:
                if (header.getCheckOne() != 161 || header.getCheckTwo() != 162) {
                    throw new RuntimeException("Creature spells file is corrupted");
                }
                readCreatureSpells(header, data);
                break;

            case DOORS:
                // check header.getCheckOne() != 141 || header.getCheckTwo() != 142
                readDoors(header, data);
                break;

            case EFFECTS:
                // check header.getCheckOne() != 271 || header.getCheckTwo() != 272
                readEffects(header, data);
                break;

            case EFFECT_ELEMENTS:
                // check header.getCheckOne() != 251 || header.getCheckTwo() != 252
                readEffectElements(header, data);
                break;

            case KEEPER_SPELLS:
                // check header.getCheckOne() != 151 || header.getCheckTwo() != 152
                readKeeperSpells(header, data);
                break;

            case MAP:
                // check header.getCheckOne() != 101 || header.getCheckTwo() != 102
                readMap(header, data);
                break;

            case OBJECTS:
                // check header.getCheckOne() != 241 || header.getCheckTwo() != 242
                readObjects(header, data);
                break;

            case PLAYERS:
                // check header.getCheckOne() != 181 || header.getCheckTwo() != 182
                readPlayers(header, data);
                break;

            case ROOMS:
                // check header.getCheckOne() != 121 || header.getCheckTwo() != 122
                readRooms(header, data);
                break;

            case SHOTS:
                // check header.getCheckOne() != 261 || header.getCheckTwo() != 262
                readShots(header, data);
                break;

            case TERRAIN:
                // check header.getCheckOne() != 111 || header.getCheckTwo() != 112
                readTerrain(header, data);
                break;

            case THINGS:
                // check header.getCheckOne() != 191 || header.getCheckTwo() != 192
                readThings(header, data);
                break;

            case TRAPS:
                // check header.getCheckOne() != 131 || header.getCheckTwo() != 132
                readTraps(header, data);
                break;

            case TRIGGERS:
                // check header.getCheckOne() != 211 || header.getCheckTwo() != 212
                readTriggers(header, data);
                break;

            case VARIABLES:
                // check header.getCheckOne() != 231 || header.getCheckTwo() != 232
                readVariables(header, data);
                break;

            default:
                LOGGER.log(Level.WARNING, "File type {0} have no reader", header.getId());
                break;
        }
    }

    /**
     * Reads the *Map.kld
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readMap(KwdHeader header, IResourceReader file) throws IOException {

        // Read the requested MAP file
        LOGGER.info("Reading map!");
        if (map == null) {
            map = new GameMap(header.getWidth(), header.getHeight());
        }
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                Tile tile = new Tile();
                tile.setTerrainId(file.readUnsignedByte());
                tile.setPlayerId(file.readUnsignedByte());
                tile.setFlag(file.readByteAsEnum(Tile.BridgeTerrainType.class));
                tile.setUnknown(file.readUnsignedByte());
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
    private void readPlayers(KwdHeader header, IResourceReader file) throws IOException {

        // Read the requested PLAYER file
        if (players == null) {
            LOGGER.info("Reading players!");
            players = new HashMap<>(header.getItemCount());
        } else {
            LOGGER.warning("Overrides players!");
        }

        for (int playerIndex = 0; playerIndex < header.getItemCount(); playerIndex++) {
            long offset = file.getFilePointer();
            Player player = new Player();
            player.setStartingGold(file.readInteger());
            player.setAi(file.readInteger() == 1);

            AI ai = new AI();
            ai.setAiType(file.readByteAsEnum(AI.AIType.class));
            ai.setSpeed(file.readUnsignedByte());
            ai.setOpenness(file.readUnsignedByte());
            ai.setRemoveCallToArmsIfTotalCreaturesLessThan(file.readUnsignedByte());
            ai.setBuildLostRoomAfterSeconds(file.readUnsignedByte());
            short[] unknown1 = new short[3];
            for (int i = 0; i < unknown1.length; i++) {
                unknown1[i] = file.readUnsignedByte();
            }
            ai.setUnknown1(unknown1);
            ai.setCreateEmptyAreasWhenIdle(file.readInteger() == 1);
            ai.setBuildBiggerLairAfterClaimingPortal(file.readInteger() == 1);
            ai.setSellCapturedRoomsIfLowOnGold(file.readInteger() == 1);
            ai.setMinTimeBeforePlacingResearchedRoom(file.readUnsignedByte());
            ai.setDefaultSize(file.readUnsignedByte());
            ai.setTilesLeftBetweenRooms(file.readUnsignedByte());
            ai.setDistanceBetweenRoomsThatShouldBeCloseMan(file.readByteAsEnum(AI.Distance.class));
            ai.setCorridorStyle(file.readByteAsEnum(AI.CorridorStyle.class));
            ai.setWhenMoreSpaceInRoomRequired(file.readByteAsEnum(AI.RoomExpandPolicy.class));
            ai.setDigToNeutralRoomsWithinTilesOfHeart(file.readUnsignedByte());
            List<Short> buildOrder = new ArrayList<>(15);
            for (int i = 0; i < 15; i++) {
                buildOrder.add(file.readUnsignedByte());
            }
            ai.setBuildOrder(buildOrder);
            ai.setFlexibility(file.readUnsignedByte());
            ai.setDigToNeutralRoomsWithinTilesOfClaimedArea(file.readUnsignedByte());
            ai.setRemoveCallToArmsAfterSeconds(file.readUnsignedShort());
            ai.setBoulderTrapsOnLongCorridors(file.readInteger() == 1);
            ai.setBoulderTrapsOnRouteToBreachPoints(file.readInteger() == 1);
            ai.setTrapUseStyle(file.readUnsignedByte());
            ai.setDoorTrapPreference(file.readUnsignedByte());
            ai.setDoorUsage(file.readByteAsEnum(AI.DoorUsagePolicy.class));
            ai.setChanceOfLookingToUseTrapsAndDoors(file.readUnsignedByte());
            ai.setRequireMinLevelForCreatures(file.readInteger() == 1);
            ai.setRequireTotalThreatGreaterThanTheEnemy(file.readInteger() == 1);
            ai.setRequireAllRoomTypesPlaced(file.readInteger() == 1);
            ai.setRequireAllKeeperSpellsResearched(file.readInteger() == 1);
            ai.setOnlyAttackAttackers(file.readInteger() == 1);
            ai.setNeverAttack(file.readInteger() == 1);
            ai.setMinLevelForCreatures(file.readUnsignedByte());
            ai.setTotalThreatGreaterThanTheEnemy(file.readUnsignedByte());
            ai.setFirstAttemptToBreachRoom(file.readByteAsEnum(AI.BreachRoomPolicy.class));
            ai.setFirstDigToEnemyPoint(file.readByteAsEnum(AI.DigToPolicy.class));
            ai.setBreachAtPointsSimultaneously(file.readUnsignedByte());
            ai.setUsePercentageOfTotalCreaturesInFirstFightAfterBreach(file.readUnsignedByte());
            ai.setManaValue(file.readUnsignedShort());
            ai.setPlaceCallToArmsWhereThreatValueIsGreaterThan(file.readUnsignedShort());
            ai.setRemoveCallToArmsIfLessThanEnemyCreatures(file.readUnsignedByte());
            ai.setRemoveCallToArmsIfLessThanEnemyCreaturesWithinTiles(file.readUnsignedByte());
            ai.setPullCreaturesFromFightIfOutnumberedAndUnableToDropReinforcements(file.readInteger() == 1);
            ai.setThreatValueOfDroppedCreaturesIsPercentageOfEnemyThreatValue(file.readUnsignedByte());
            ai.setSpellStyle(file.readUnsignedByte());
            ai.setAttemptToImprisonPercentageOfEnemyCreatures(file.readUnsignedByte());
            ai.setIfCreatureHealthIsPercentageAndNotInOwnRoomMoveToLairOrTemple(file.readUnsignedByte());
            ai.setGoldValue(file.readUnsignedShort());
            ai.setTryToMakeUnhappyOnesHappy(file.readInteger() == 1);
            ai.setTryToMakeAngryOnesHappy(file.readInteger() == 1);
            ai.setDisposeOfAngryCreatures(file.readInteger() == 1);
            ai.setDisposeOfRubbishCreaturesIfBetterOnesComeAlong(file.readInteger() == 1);
            ai.setDisposalMethod(file.readByteAsEnum(AI.CreatureDisposalPolicy.class));
            ai.setMaximumNumberOfImps(file.readUnsignedByte());
            ai.setWillNotSlapCreatures(file.readUnsignedByte() == 0);
            ai.setAttackWhenNumberOfCreaturesIsAtLeast(file.readUnsignedByte());
            ai.setUseLightningIfEnemyIsInWater(file.readInteger() == 1);
            ai.setUseSightOfEvil(file.readByteAsEnum(AI.SightOfEvilUsagePolicy.class));
            ai.setUseSpellsInBattle(file.readUnsignedByte());
            ai.setSpellsPowerPreference(file.readUnsignedByte());
            ai.setUseCallToArms(file.readByteAsEnum(AI.CallToArmsUsagePolicy.class));
            short[] unknown2 = new short[2];
            for (int i = 0; i < unknown2.length; i++) {
                unknown2[i] = file.readUnsignedByte();
            }
            ai.setUnknown2(unknown2);
            ai.setMineGoldUntilGoldHeldIsGreaterThan(file.readUnsignedShort());
            ai.setWaitSecondsAfterPreviousAttackBeforeAttackingAgain(file.readUnsignedShort());
            ai.setStartingMana(file.readUnsignedInteger());
            ai.setExploreUpToTilesToFindSpecials(file.readUnsignedShort());
            ai.setImpsToTilesRatio(file.readUnsignedShort());
            ai.setBuildAreaStartX(file.readUnsignedShort());
            ai.setBuildAreaStartY(file.readUnsignedShort());
            ai.setBuildAreaEndX(file.readUnsignedShort());
            ai.setBuildAreaEndY(file.readUnsignedShort());
            ai.setLikelyhoodToMovingCreaturesToLibraryForResearching(file.readByteAsEnum(AI.MoveToResearchPolicy.class));
            ai.setChanceOfExploringToFindSpecials(file.readUnsignedByte());
            ai.setChanceOfFindingSpecialsWhenExploring(file.readUnsignedByte());
            ai.setFateOfImprisonedCreatures(file.readByteAsEnum(AI.ImprisonedCreatureFatePolicy.class));
            player.setAiAttributes(ai);

            player.setTriggerId(file.readUnsignedShort());
            player.setPlayerId(file.readUnsignedByte());
            player.setStartingCameraX(file.readUnsignedShort());
            player.setStartingCameraY(file.readUnsignedShort());

            player.setName(file.readString(32).trim());

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
    private void readTerrain(KwdHeader header, IResourceReader file) throws RuntimeException, IOException {

        // Read the terrain catalog
        if (terrainTiles == null) {
            LOGGER.info("Reading terrain!");
            terrainTiles = new HashMap<>(header.getItemCount());
        } else {
            LOGGER.warning("Overrides terrain!");
        }

        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();
            Terrain terrain = new Terrain();

            terrain.setName(file.readString(32).trim());
            terrain.setCompleteResource(readArtResource(file));
            terrain.setSideResource(readArtResource(file));
            terrain.setTopResource(readArtResource(file));
            terrain.setTaggedTopResource(readArtResource(file));
            terrain.setStringIds(readStringId(file));
            terrain.setDepth(file.readIntegerAsFloat());
            terrain.setLightHeight(file.readIntegerAsFloat());
            terrain.setFlags(file.readIntegerAsFlag(Terrain.TerrainFlag.class));
            terrain.setDamage(file.readUnsignedShort());
            terrain.setEditorTextureId(file.readUnsignedShort());
            terrain.setUnk198(file.readUnsignedShort());
            terrain.setGoldValue(file.readUnsignedShort());
            terrain.setManaGain(file.readUnsignedShort());
            terrain.setMaxManaGain(file.readUnsignedShort());
            terrain.setTooltipStringId(file.readUnsignedShort());
            terrain.setNameStringId(file.readUnsignedShort());
            terrain.setMaxHealthEffectId(file.readUnsignedShort());
            terrain.setDestroyedEffectId(file.readUnsignedShort());
            terrain.setGeneralDescriptionStringId(file.readUnsignedShort());
            terrain.setStrengthStringId(file.readUnsignedShort());
            terrain.setWeaknessStringId(file.readUnsignedShort());
            int[] unk1ae = new int[16];
            for (int x = 0; x < unk1ae.length; x++) {
                unk1ae[x] = file.readUnsignedShort();
            }
            terrain.setUnk1ae(unk1ae);
            terrain.setWibbleH(file.readUnsignedByte());
            short[] leanH = new short[3];
            for (int x = 0; x < leanH.length; x++) {
                leanH[x] = file.readUnsignedByte();
            }
            terrain.setLeanH(leanH);
            terrain.setWibbleV(file.readUnsignedByte());
            short[] leanV = new short[3];
            for (int x = 0; x < leanV.length; x++) {
                leanV[x] = file.readUnsignedByte();
            }
            terrain.setLeanV(leanV);
            terrain.setTerrainId(file.readUnsignedByte());
            terrain.setStartingHealth(file.readUnsignedShort());
            terrain.setMaxHealthTypeTerrainId(file.readUnsignedByte());
            terrain.setDestroyedTypeTerrainId(file.readUnsignedByte());
            terrain.setTerrainLight(new Color(file.readUnsignedByte(),
                    file.readUnsignedByte(),
                    file.readUnsignedByte()));
            terrain.setTextureFrames(file.readUnsignedByte());

            terrain.setSoundCategory(file.readString(32).trim());
            terrain.setMaxHealth(file.readUnsignedShort());
            terrain.setAmbientLight(new Color(file.readUnsignedByte(),
                    file.readUnsignedByte(),
                    file.readUnsignedByte()));

            terrain.setSoundCategoryFirstPerson(file.readString(32).trim());
            terrain.setUnk224(file.readUnsignedInteger());

            // Add to the hash by the terrain ID
            terrainTiles.put(terrain.getTerrainId(), terrain);

            // See that we have water & lava set
            if (map.getWater() == null && terrain.getFlags().contains(Terrain.TerrainFlag.WATER)) {
                map.setWater(terrain);
            }
            if (map.getLava() == null && terrain.getFlags().contains(Terrain.TerrainFlag.LAVA)) {
                map.setLava(terrain);
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
    private ArtResource readArtResource(IResourceReader file) throws IOException {
        ArtResource artResource = new ArtResource();

        // Read the data
        artResource.setName(file.readString(64).trim());
        artResource.setFlags(file.readIntegerAsFlag(ArtResource.ArtResourceFlag.class));

        long pointer = file.getFilePointer();
        file.seek(pointer + 12);
        artResource.setType(file.readByteAsEnum(ArtResource.ArtResourceType.class));
        if (artResource.getType() == ArtResourceType.ANIMATING_MESH) {
            artResource.setData("startAf", file.readUnsignedByte()); // if HAS_START_ANIMATION
            artResource.setData("endAf", file.readUnsignedByte()); // if HAS_END_ANIMATION
        } else {
            artResource.setData("unknown_n", file.readUnsignedShort());
        }
        artResource.setSometimesOne(file.readUnsignedByte());

        file.seek(pointer);
        switch (artResource.getType()) {
            case NONE: // skip empty type
                file.readAndCheckNull(12);
                break;

            case SPRITE: // And alphas and images probably share the same attributes
            case ALPHA:
            case ADDITIVE_ALPHA:  // Images of different type
                artResource.setData("width", file.readIntegerAsFloat());
                artResource.setData("height", file.readIntegerAsFloat());
                artResource.setData("frames", file.readUnsignedInteger()); // if (ANIMATING_TEXTURE)
                break;

            case TERRAIN_MESH:
                artResource.setData("unknown_1", file.readUnsignedInteger());
                artResource.setData("unknown_2", file.readUnsignedInteger());
                artResource.setData("unknown_3", file.readUnsignedInteger());
                break;

            case MESH:
                artResource.setData("scale", file.readIntegerAsFloat());
                artResource.setData("frames", file.readUnsignedInteger()); // if (ANIMATING_TEXTURE)
                artResource.setData("unknown_1", file.readUnsignedInteger());
                break;

            case ANIMATING_MESH:
                artResource.setData("frames", file.readUnsignedInteger());
                artResource.setData("fps", file.readUnsignedInteger());
                artResource.setData("startDist", file.readUnsignedShort());
                artResource.setData("endDist", file.readUnsignedShort());
                break;

            case PROCEDURAL_MESH:
                artResource.setData("id", file.readUnsignedInteger());
                artResource.setData("unknown_1", file.readUnsignedInteger());
                artResource.setData("unknown_2", file.readUnsignedInteger());
                break;

            case MESH_COLLECTION: // FIXME nothing todo ?! has just the name, reference to GROP meshes probably
            case UNKNOWN:
                artResource.setData("unknown_1", file.readUnsignedInteger());
                artResource.setData("unknown_2", file.readUnsignedInteger());
                artResource.setData("unknown_3", file.readUnsignedInteger());
                break;

            default:
                file.readAndCheckNull(12);
                LOGGER.log(Level.WARNING, "Unknown artResource type {0}", artResource.getType());
                break;
        }

        file.skipBytes(4);
        // If it has no name or the type is not known, return null
        if (artResource.getName().isEmpty() || artResource.getType() == null) {
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
    private StringId readStringId(IResourceReader file) throws IOException {

        // Read the IDs
        int[] ids = new int[5];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = file.readUnsignedInteger();
        }

        // And the unknowns
        short[] x14 = new short[4];
        for (int i = 0; i < x14.length; i++) {
            x14[i] = file.readUnsignedByte();
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
    private void readDoors(KwdHeader header, IResourceReader file) throws IOException {

        // Read the doors catalog
        if (doors == null) {
            LOGGER.info("Reading doors!");
            doors = new HashMap<>(header.getItemCount());
        } else {
            LOGGER.warning("Overrides doors!");
        }

        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();
            Door door = new Door();

            door.setName(file.readString(32).trim());
            door.setMesh(readArtResource(file));
            door.setGuiIcon(readArtResource(file));
            door.setEditorIcon(readArtResource(file));
            door.setFlowerIcon(readArtResource(file));
            door.setOpenResource(readArtResource(file));
            door.setCloseResource(readArtResource(file));
            door.setHeight(file.readIntegerAsFloat());
            door.setHealthGain(file.readUnsignedShort());
            door.setUnknown1(file.readUnsignedShort());
            door.setUnknown2(file.readUnsignedInteger());
            door.setResearchTime(file.readUnsignedShort());
            door.setMaterial(file.readByteAsEnum(Material.class));
            door.setTrapTypeId(file.readUnsignedByte());
            door.setFlags(file.readIntegerAsFlag(DoorFlag.class));
            door.setHealth(file.readUnsignedShort());
            door.setGoldCost(file.readUnsignedShort());
            short[] unknown3 = new short[2];
            for (int x = 0; x < unknown3.length; x++) {
                unknown3[x] = file.readUnsignedByte();
            }
            door.setUnknown3(unknown3);
            door.setDeathEffectId(file.readUnsignedShort());
            door.setManufToBuild(file.readUnsignedInteger());
            door.setManaCost(file.readUnsignedShort());
            door.setTooltipStringId(file.readUnsignedShort());
            door.setNameStringId(file.readUnsignedShort());
            door.setGeneralDescriptionStringId(file.readUnsignedShort());
            door.setStrengthStringId(file.readUnsignedShort());
            door.setWeaknessStringId(file.readUnsignedShort());
            door.setDoorId(file.readUnsignedByte());
            door.setOrderInEditor(file.readUnsignedByte());
            door.setManufCrateObjectId(file.readUnsignedByte());
            door.setKeyObjectId(file.readUnsignedByte());

            door.setSoundCategory(file.readString(32).trim());

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
    private void readTraps(KwdHeader header, IResourceReader file) throws IOException {

        // Read the traps catalog
        if (traps == null) {
            LOGGER.info("Reading traps!");
            traps = new HashMap<>(header.getItemCount());
        } else {
            LOGGER.warning("Overrides traps!");
        }

        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();
            Trap trap = new Trap();

            trap.setName(file.readString(32).trim());
            trap.setMeshResource(readArtResource(file));
            trap.setGuiIcon(readArtResource(file));
            trap.setEditorIcon(readArtResource(file));
            trap.setFlowerIcon(readArtResource(file));
            trap.setFireResource(readArtResource(file));
            trap.setHeight(file.readIntegerAsFloat());
            trap.setRechargeTime(file.readIntegerAsFloat());
            trap.setChargeTime(file.readIntegerAsFloat());
            trap.setThreatDuration(file.readIntegerAsFloat());
            trap.setManaCostToFire(file.readUnsignedInteger());
            trap.setIdleEffectDelay(file.readIntegerAsFloat());
            trap.setTriggerData(file.readUnsignedInteger());
            trap.setShotData1(file.readUnsignedInteger());
            trap.setShotData2(file.readUnsignedInteger());
            trap.setResearchTime(file.readUnsignedShort());
            trap.setThreat(file.readUnsignedShort());
            trap.setFlags(file.readIntegerAsFlag(Trap.TrapFlag.class));
            trap.setHealth(file.readUnsignedShort());
            trap.setManaCost(file.readUnsignedShort());
            trap.setPowerlessEffectId(file.readUnsignedShort());
            trap.setIdleEffectId(file.readUnsignedShort());
            trap.setDeathEffectId(file.readUnsignedShort());
            trap.setManufToBuild(file.readUnsignedShort());
            trap.setGeneralDescriptionStringId(file.readUnsignedShort());
            trap.setStrengthStringId(file.readUnsignedShort());
            trap.setWeaknessStringId(file.readUnsignedShort());
            trap.setManaUsage(file.readUnsignedShort());
            short[] unknown4 = new short[2];
            for (int x = 0; x < unknown4.length; x++) {
                unknown4[x] = file.readUnsignedByte();
            }
            trap.setUnknown4(unknown4);
            trap.setTooltipStringId(file.readUnsignedShort());
            trap.setNameStringId(file.readUnsignedShort());
            trap.setShotsWhenArmed(file.readUnsignedByte());
            trap.setTriggerType(file.readByteAsEnum(Trap.TriggerType.class));
            trap.setTrapId(file.readUnsignedByte());
            trap.setShotTypeId(file.readUnsignedByte());
            trap.setManufCrateObjectId(file.readUnsignedByte());

            trap.setSoundCategory(file.readString(32).trim());
            trap.setMaterial(file.readByteAsEnum(Material.class));
            trap.setOrderInEditor(file.readUnsignedByte());
            trap.setShotOffset(file.readIntegerAsFloat(),
                    file.readIntegerAsFloat(),
                    file.readIntegerAsFloat());
            trap.setShotDelay(file.readIntegerAsFloat());
            trap.setHealthGain(file.readUnsignedShort());

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
    private void readRooms(KwdHeader header, IResourceReader file) throws RuntimeException, IOException {

        // Read the rooms catalog
        if (rooms == null) {
            LOGGER.info("Reading rooms!");
            rooms = new HashMap<>(header.getItemCount());
            roomsByTerrainId = new HashMap<>(header.getItemCount());
        } else {
            LOGGER.warning("Overrides rooms!");
        }

        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();
            Room room = new Room();

            room.setName(file.readString(32).trim());
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
            room.setCeilingHeight(file.readIntegerAsFloat());
            room.setResearchTime(file.readUnsignedShort());
            room.setTorchIntensity(file.readUnsignedShort());
            room.setFlags(file.readIntegerAsFlag(Room.RoomFlag.class));
            room.setTooltipStringId(file.readUnsignedShort());
            room.setNameStringId(file.readUnsignedShort());
            room.setCost(file.readUnsignedShort());
            room.setFightEffectId(file.readUnsignedShort());
            room.setGeneralDescriptionStringId(file.readUnsignedShort());
            room.setStrengthStringId(file.readUnsignedShort());
            room.setTorchHeight(file.readShortAsFloat());
            List<Integer> roomEffects = new ArrayList<>(8);
            for (int x = 0; x < 8; x++) {
                int effectId = file.readUnsignedShort();
                roomEffects.add(effectId);
            }
            room.setEffects(roomEffects);
            room.setRoomId(file.readUnsignedByte());
            room.setReturnPercentage(file.readUnsignedByte());
            room.setTerrainId(file.readUnsignedByte());
            room.setTileConstruction(file.readByteAsEnum(Room.TileConstruction.class));
            room.setCreatedCreatureId(file.readUnsignedByte());
            room.setTorchColor(new Color(file.readUnsignedByte(), file.readUnsignedByte(), file.readUnsignedByte())); // This is the editor is rather weird
            List<Short> roomObjects = new ArrayList<>(8);
            for (int x = 0; x < 8; x++) {
                short objectId = file.readUnsignedByte();
                roomObjects.add(objectId);
            }
            room.setObjects(roomObjects);

            room.setSoundCategory(file.readString(32).trim());
            room.setOrderInEditor(file.readUnsignedByte());
            room.setTorchRadius(file.readIntegerAsFloat());
            room.setTorch(readArtResource(file));
            room.setRecommendedSizeX(file.readUnsignedByte());
            room.setRecommendedSizeY(file.readUnsignedByte());
            room.setHealthGain(file.readShort());

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
    private void readMapInfo(KwdHeader header, IResourceReader data) throws IOException {

        //Additional header data
        if (gameLevel == null) {
            LOGGER.info("Reading level info!");
            gameLevel = new GameLevel();
        } else {
            LOGGER.warning("Overrides level!");
        }

        //Property data
        String name = data.readStringUtf16(64).trim();
        if (name != null && !name.isEmpty() && name.toLowerCase().endsWith(".kwd")) {
            name = name.substring(0, name.length() - 4);
        }
        gameLevel.setName(name);
        gameLevel.setDescription(data.readStringUtf16(1024).trim());
        gameLevel.setAuthor(data.readStringUtf16(64).trim());
        gameLevel.setEmail(data.readStringUtf16(64).trim());
        gameLevel.setInformation(data.readStringUtf16(1024).trim());

        gameLevel.setTriggerId(data.readUnsignedShort());
        gameLevel.setTicksPerSec(data.readUnsignedShort());
        short[] x01184 = new short[520];
        for (int x = 0; x < x01184.length; x++) {
            x01184[x] = data.readUnsignedByte();
        }
        gameLevel.setX01184(x01184);
        // I don't know if we need the index, level 19 & 3 has messages, but they are rare
        List<String> messages = new ArrayList<>();
        for (int x = 0; x < 512; x++) {
            String message = data.readStringUtf16(20).trim();
            if (!message.isEmpty()) {
                messages.add(message);
            }
        }
        gameLevel.setMessages(messages);

        gameLevel.setLvlFlags(data.readShortAsFlag(LevFlag.class));
        gameLevel.setSoundCategory(data.readString(32).trim());
        gameLevel.setTalismanPieces(data.readUnsignedByte());

        for (int x = 0; x < 4; x++) {
            LevelReward reward = data.readByteAsEnum(LevelReward.class);
            gameLevel.addRewardPrev(reward);
        }

        for (int x = 0; x < 4; x++) {
            LevelReward reward = data.readByteAsEnum(LevelReward.class);
            gameLevel.addRewardNext(reward);
        }

        gameLevel.setSoundTrack(data.readUnsignedByte());
        gameLevel.setTextTableId(data.readByteAsEnum(TextTable.class));
        gameLevel.setTextTitleId(data.readUnsignedShort());
        gameLevel.setTextPlotId(data.readUnsignedShort());
        gameLevel.setTextDebriefId(data.readUnsignedShort());
        gameLevel.setTextObjectvId(data.readUnsignedShort());
        gameLevel.setX063c3(data.readUnsignedShort());
        gameLevel.setTextSubobjctvId1(data.readUnsignedShort());
        gameLevel.setTextSubobjctvId2(data.readUnsignedShort());
        gameLevel.setTextSubobjctvId3(data.readUnsignedShort());
        gameLevel.setSpeclvlIdx(data.readUnsignedShort());

        // Swap the arrays for more convenient data format
        short[] textIntrdcOverrdObj = new short[8];
        for (int x = 0; x < textIntrdcOverrdObj.length; x++) {
            textIntrdcOverrdObj[x] = data.readUnsignedByte();
        }
        int[] textIntrdcOverrdId = new int[8];
        for (int x = 0; x < textIntrdcOverrdId.length; x++) {
            textIntrdcOverrdId[x] = data.readUnsignedShort();
        }
        Map<Short, Integer> introductionOverrideTextIds = new HashMap<>(8);
        for (int x = 0; x < textIntrdcOverrdObj.length; x++) {
            if (textIntrdcOverrdObj[x] > 0) {
                // Over 0 is a valid creature ID
                introductionOverrideTextIds.put(textIntrdcOverrdObj[x], textIntrdcOverrdId[x]);
            }
        }
        gameLevel.setIntroductionOverrideTextIds(introductionOverrideTextIds);

        gameLevel.setTerrainPath(data.readString(32).trim());
        // Some very old files are smaller, namely the FrontEnd3DLevel map in some version
        if (header.dataSize > HEADER_SIZE) {
            gameLevel.setOneShotHornyLev(data.readUnsignedByte());
            gameLevel.setPlayerCount(data.readUnsignedByte());
            gameLevel.addRewardPrev(data.readByteAsEnum(LevelReward.class));
            gameLevel.addRewardNext(data.readByteAsEnum(LevelReward.class));
            gameLevel.setSpeechHornyId(data.readUnsignedShort());
            gameLevel.setSpeechPrelvlId(data.readUnsignedShort());
            gameLevel.setSpeechPostlvlWin(data.readUnsignedShort());
            gameLevel.setSpeechPostlvlLost(data.readUnsignedShort());
            gameLevel.setSpeechPostlvlNews(data.readUnsignedShort());
            gameLevel.setSpeechPrelvlGenr(data.readUnsignedShort());
            gameLevel.setHeroName(data.readStringUtf16(32).trim());
        }

        // Paths and the unknown array
        int checkThree = data.readUnsignedInteger();
        if (checkThree != 222) {
            throw new RuntimeException("Level file is corrupted");
        }
        // the last part of file have size contentSize
        int contentSize = data.readUnsignedInteger();
        boolean customOverrides = false;
        List<FilePath> paths = new ArrayList<>(header.getItemCount());
        for (int x = 0; x < header.getItemCount(); x++) {
            FilePath filePath = new FilePath();
            filePath.setId(data.readIntegerAsEnum(MapDataTypeEnum.class));
            filePath.setUnknown2(data.readInteger());
            String path = data.readString(64).trim();

            // Tweak the paths
            // Paths are relative to the base path, may or may not have an extension (assume kwd if none found)
            path = ConversionUtils.convertFileSeparators(path);
            if (!".".equals(path.substring(path.length() - 4, path.length() - 3))) {
                path = path.concat(".kwd");
            }

            // See if the globals are present
            if (filePath.getId() == MapDataTypeEnum.GLOBALS) {
                customOverrides = true;
                LOGGER.info("The map uses custom overrides!");
            }

            filePath.setPath(path);

            paths.add(filePath);
        }
        gameLevel.setPaths(paths);

        // Hmm, seems that normal maps don't refer the effects nor effect elements
        if (!customOverrides) {
            FilePath file = new FilePath(MapDataTypeEnum.EFFECTS, PathUtils.DKII_EDITOR_FOLDER + "Effects.kwd");
            if (!gameLevel.getPaths().contains(file)) {
                gameLevel.getPaths().add(file);
            }

            file = new FilePath(MapDataTypeEnum.EFFECT_ELEMENTS, PathUtils.DKII_EDITOR_FOLDER + "EffectElements.kwd");
            if (!gameLevel.getPaths().contains(file)) {
                gameLevel.getPaths().add(file);
            }
        }

        int[] unknown = new int[header.getHeight()];
        for (int x = 0; x < unknown.length; x++) {
            unknown[x] = data.readUnsignedInteger();
        }
        gameLevel.setUnknown(unknown);
    }

    /**
     * Reads the Creatures.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readCreatures(KwdHeader header, IResourceReader file) throws IOException {

        // Read the creatures catalog
        if (creatures == null) {
            LOGGER.info("Reading creatures!");
            creatures = new HashMap<>(header.getItemCount());
        } else {
            LOGGER.warning("Overrides creatures!");
        }

        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();
            Creature creature = new Creature();

            creature.setName(file.readString(32).trim());
            // 39 ArtResources (with IMPs these are not 100% same)
            creature.setUnknown1Resource(file.read(84));  // all 0: Maiden Of The Nest, Prince Balder, Horny. Other the same
            creature.setAnimation(AnimationType.WALK, readArtResource(file));
            creature.setAnimation(AnimationType.RUN, readArtResource(file));
            creature.setAnimation(AnimationType.DRAGGED, readArtResource(file));
            creature.setAnimation(AnimationType.RECOIL_FORWARDS, readArtResource(file));
            creature.setAnimation(AnimationType.MELEE_ATTACK, readArtResource(file));
            creature.setAnimation(AnimationType.CAST_SPELL, readArtResource(file));
            creature.setAnimation(AnimationType.DIE, readArtResource(file));
            creature.setAnimation(AnimationType.HAPPY, readArtResource(file));
            creature.setAnimation(AnimationType.ANGRY, readArtResource(file));
            creature.setAnimation(AnimationType.STUNNED, readArtResource(file));
            creature.setAnimation(AnimationType.IN_HAND, readArtResource(file));
            creature.setAnimation(AnimationType.SLEEPING, readArtResource(file));
            creature.setAnimation(AnimationType.EATING, readArtResource(file));
            creature.setAnimation(AnimationType.RESEARCHING, readArtResource(file));
            creature.setAnimation(AnimationType.NULL_2, readArtResource(file));
            creature.setAnimation(AnimationType.NULL_1, readArtResource(file));
            creature.setAnimation(AnimationType.TORTURED_WHEEL, readArtResource(file));
            creature.setAnimation(AnimationType.NULL_3, readArtResource(file));
            creature.setAnimation(AnimationType.DRINKING, readArtResource(file));
            creature.setAnimation(AnimationType.IDLE_1, readArtResource(file));
            creature.setAnimation(AnimationType.RECOIL_BACKWARDS, readArtResource(file));
            creature.setAnimation(AnimationType.MANUFACTURING, readArtResource(file));
            creature.setAnimation(AnimationType.PRAYING, readArtResource(file));
            creature.setAnimation(AnimationType.FALLBACK, readArtResource(file));
            creature.setAnimation(AnimationType.TORTURED_CHAIR, readArtResource(file));
            creature.setAnimation(AnimationType.TORTURED_CHAIR_SKELETON, readArtResource(file));
            creature.setAnimation(AnimationType.GET_UP, readArtResource(file));
            creature.setAnimation(AnimationType.DANCE, readArtResource(file));
            creature.setAnimation(AnimationType.DRUNK, readArtResource(file));
            creature.setAnimation(AnimationType.ENTRANCE, readArtResource(file));
            creature.setAnimation(AnimationType.IDLE_2, readArtResource(file));
            creature.setAnimation(AnimationType.SPECIAL_1, readArtResource(file));
            creature.setAnimation(AnimationType.SPECIAL_2, readArtResource(file));
            creature.setAnimation(AnimationType.DRUNKED_WALK, readArtResource(file));
            creature.setAnimation(AnimationType.ROAR, readArtResource(file)); // FIXME
            creature.setAnimation(AnimationType.NULL_4, readArtResource(file));

            creature.setIcon1Resource(readArtResource(file));
            creature.setIcon2Resource(readArtResource(file));
            //
            creature.setUnkcec(file.readUnsignedShort());
            creature.setUnkcee(file.readUnsignedInteger());
            creature.setUnkcf2(file.readUnsignedInteger());
            creature.setOrderInEditor(file.readUnsignedByte());
            creature.setAngerStringIdGeneral(file.readUnsignedShort());
            creature.setShotDelay(file.readIntegerAsFloat());
            creature.setOlhiEffectId(file.readUnsignedShort());
            creature.setIntroductionStringId(file.readUnsignedShort());
            creature.getAttributes().setPerceptionRange(file.readIntegerAsFloat());
            creature.setAngerStringIdLair(file.readUnsignedShort());
            creature.setAngerStringIdFood(file.readUnsignedShort());
            creature.setAngerStringIdPay(file.readUnsignedShort());
            creature.setAngerStringIdWork(file.readUnsignedShort());
            creature.setAngerStringIdSlap(file.readUnsignedShort());
            creature.setAngerStringIdHeld(file.readUnsignedShort());
            creature.setAngerStringIdLonely(file.readUnsignedShort());
            creature.setAngerStringIdHatred(file.readUnsignedShort());
            creature.setAngerStringIdTorture(file.readUnsignedShort());

            creature.setTranslationSoundGategory(file.readString(32).trim());
            creature.getAttributes().setShuffleSpeed(file.readIntegerAsFloat());
            creature.setCloneCreatureId(file.readUnsignedByte());
            creature.setFirstPersonGammaEffect(file.readByteAsEnum(Creature.GammaEffect.class));
            creature.setFirstPersonWalkCycleScale(file.readUnsignedByte());
            creature.setIntroCameraPathIndex(file.readUnsignedByte());
            creature.setUnk2e2(file.readUnsignedByte());
            creature.setPortraitResource(readArtResource(file));
            creature.setLight(readLight(file));
            Attraction[] attractions = new Attraction[2];
            for (int x = 0; x < attractions.length; x++) {
                Attraction attraction = creature.new Attraction();
                attraction.setPresent(file.readUnsignedInteger());
                attraction.setRoomId(file.readUnsignedShort());
                attraction.setRoomSize(file.readUnsignedShort());
                attractions[x] = attraction;
            }
            creature.setAttractions(attractions);
            creature.setFirstPersonWaddleScale(file.readIntegerAsFloat());
            creature.setFirstPersonOscillateScale(file.readIntegerAsFloat());
            List<Spell> spells = new ArrayList<>(3);
            for (int x = 0; x < 3; x++) {
                Spell spell = creature.new Spell();
                spell.setShotOffset(file.readIntegerAsFloat(),
                        file.readIntegerAsFloat(),
                        file.readIntegerAsFloat());
                spell.setX0c(file.readUnsignedByte());
                spell.setPlayAnimation(file.readUnsignedByte() == 1);
                spell.setX0e(file.readUnsignedByte()); // This value can changed when you not change anything on map, only save it
                spell.setX0f(file.readUnsignedByte());
                spell.setShotDelay(file.readIntegerAsFloat());
                spell.setX14(file.readUnsignedByte());
                spell.setX15(file.readUnsignedByte());
                spell.setCreatureSpellId(file.readUnsignedByte());
                spell.setLevelAvailable(file.readUnsignedByte());
                if (spell.getCreatureSpellId() != 0) {
                    spells.add(spell);
                }
            }
            creature.setSpells(spells);
            Creature.Resistance[] resistances = new Creature.Resistance[4];
            for (int x = 0; x < resistances.length; x++) {
                Creature.Resistance resistance = creature.new Resistance();
                resistance.setAttackType(file.readByteAsEnum(Creature.AttackType.class));
                resistance.setValue(file.readUnsignedByte());
                resistances[x] = resistance;
            }
            creature.setResistances(resistances);
            creature.setHappyJobs(readJobPreferences(3, creature, file));
            creature.setUnhappyJobs(readJobPreferences(2, creature, file));
            creature.setAngryJobs(readJobPreferences(3, creature, file));
            Creature.JobType[] hateJobs = new Creature.JobType[2];
            for (int x = 0; x < hateJobs.length; x++) {
                hateJobs[x] = file.readIntegerAsEnum(Creature.JobType.class);
            }
            creature.setHateJobs(hateJobs);
            JobAlternative[] alternatives = new JobAlternative[3];
            for (int x = 0; x < alternatives.length; x++) {
                JobAlternative alternative = creature.new JobAlternative();
                alternative.setJobType(file.readIntegerAsEnum(Creature.JobType.class));
                alternative.setMoodChange(file.readUnsignedShort());
                alternative.setManaChange(file.readUnsignedShort());
                alternatives[x] = alternative;
            }
            creature.setAlternativeJobs(alternatives);
            creature.setAnimationOffsets(OffsetType.PORTAL_ENTRANCE,
                    file.readIntegerAsFloat(),
                    file.readIntegerAsFloat(),
                    file.readIntegerAsFloat()
            );
            creature.setUnkea0(file.readInteger());
            creature.getAttributes().setHeight(file.readIntegerAsFloat());
            creature.setUnkea8(file.readIntegerAsFloat());
            creature.setUnk3ab(file.readUnsignedInteger());
            creature.getAttributes().setEyeHeight(file.readIntegerAsFloat());
            creature.getAttributes().setSpeed(file.readIntegerAsFloat());
            creature.getAttributes().setRunSpeed(file.readIntegerAsFloat());
            creature.getAttributes().setHungerRate(file.readIntegerAsFloat());
            creature.getAttributes().setTimeAwake(file.readUnsignedInteger());
            creature.getAttributes().setTimeSleep(file.readUnsignedInteger());
            creature.getAttributes().setDistanceCanSee(file.readIntegerAsFloat());
            creature.getAttributes().setDistanceCanHear(file.readIntegerAsFloat());
            creature.getAttributes().setStunDuration(file.readIntegerAsFloat());
            creature.getAttributes().setGuardDuration(file.readIntegerAsFloat());
            creature.getAttributes().setIdleDuration(file.readIntegerAsFloat());
            creature.getAttributes().setSlapFearlessDuration(file.readIntegerAsFloat());
            creature.setUnkee0(file.readInteger());
            creature.setUnkee4(file.readInteger());
            creature.getAttributes().setPossessionManaCost(file.readShort());
            creature.getAttributes().setOwnLandHealthIncrease(file.readShort());
            creature.setMeleeRange(file.readIntegerAsFloat());
            creature.setUnkef0(file.readUnsignedInteger());
            creature.getAttributes().setTortureTimeToConvert(file.readIntegerAsFloat());
            creature.setMeleeRecharge(file.readIntegerAsFloat());
            // The flags is actually very big, pushing the boundaries, a true uint32, need to -> long
            creature.setFlags(file.readIntegerAsFlag(Creature.CreatureFlag.class));
            creature.getAttributes().setExpForNextLevel(file.readUnsignedShort());
            creature.setJobClass(file.readByteAsEnum(Creature.JobClass.class));
            creature.setFightStyle(file.readByteAsEnum(Creature.FightStyle.class));
            creature.getAttributes().setExpPerSecond(file.readUnsignedShort());
            creature.getAttributes().setExpPerSecondTraining(file.readUnsignedShort());
            creature.getAttributes().setResearchPerSecond(file.readUnsignedShort());
            creature.getAttributes().setManufacturePerSecond(file.readUnsignedShort());
            creature.getAttributes().setHp(file.readUnsignedShort());
            creature.getAttributes().setHpFromChicken(file.readUnsignedShort());
            creature.getAttributes().setFear(file.readUnsignedShort());
            creature.getAttributes().setThreat(file.readUnsignedShort());
            creature.setMeleeDamage(file.readUnsignedShort());
            creature.getAttributes().setSlapDamage(file.readUnsignedShort());
            creature.getAttributes().setManaGenPrayer(file.readUnsignedShort());
            creature.setUnk3cb(file.readUnsignedShort());
            creature.getAttributes().setPay(file.readUnsignedShort());
            creature.getAttributes().setMaxGoldHeld(file.readUnsignedShort());
            creature.setUnk3cc(file.readShortAsFloat());
            creature.getAttributes().setDecomposeValue(file.readUnsignedShort());
            creature.setNameStringId(file.readUnsignedShort());
            creature.setTooltipStringId(file.readUnsignedShort());
            creature.getAttributes().setAngerNoLair(file.readShort());
            creature.getAttributes().setAngerNoFood(file.readShort());
            creature.getAttributes().setAngerNoPay(file.readShort());
            creature.getAttributes().setAngerNoWork(file.readShort());
            creature.getAttributes().setAngerSlap(file.readShort());
            creature.getAttributes().setAngerInHand(file.readShort());
            creature.getAttributes().setInitialGoldHeld(file.readShort());
            creature.setEntranceEffectId(file.readUnsignedShort());
            creature.setGeneralDescriptionStringId(file.readUnsignedShort());
            creature.setStrengthStringId(file.readUnsignedShort());
            creature.setWeaknessStringId(file.readUnsignedShort());
            creature.setSlapEffectId(file.readUnsignedShort());
            creature.setDeathEffectId(file.readUnsignedShort());
            creature.setMelee1Swipe(file.readByteAsEnum(Creature.Swipe.class));
            creature.setMelee2Swipe(file.readByteAsEnum(Creature.Swipe.class));
            creature.setMelee3Swipe(file.readByteAsEnum(Creature.Swipe.class));
            creature.setSpellSwipe(file.readByteAsEnum(Creature.Swipe.class));
            creature.setFirstPersonSpecialAbility1(file.readByteAsEnum(Creature.SpecialAbility.class));
            creature.setFirstPersonSpecialAbility2(file.readByteAsEnum(Creature.SpecialAbility.class));
            short[] unkf48 = new short[3];
            for (int x = 0; x < unkf48.length; x++) {
                unkf48[x] = file.readUnsignedByte();
            }
            creature.setUnkf48(unkf48);
            creature.setCreatureId(file.readUnsignedByte());
            short[] unk3ea = new short[2];
            for (int x = 0; x < unk3ea.length; x++) {
                unk3ea[x] = file.readUnsignedByte();
            }
            creature.setUnk3ea(unk3ea);
            creature.getAttributes().setHungerFill(file.readUnsignedByte());
            creature.getAttributes().setUnhappyThreshold(file.readUnsignedByte());
            creature.setMeleeAttackType(file.readByteAsEnum(Creature.AttackType.class));
            creature.setUnk3eb2(file.readUnsignedByte());
            creature.setLairObjectId(file.readUnsignedByte());
            creature.setUnk3f1(file.readUnsignedByte());
            creature.setDeathFallDirection(file.readByteAsEnum(Creature.DeathFallDirection.class));
            creature.setUnk3f2(file.readUnsignedByte());

            creature.setSoundCategory(file.readString(32).trim());
            creature.setMaterial(file.readByteAsEnum(Material.class));
            creature.setFirstPersonFilterResource(readArtResource(file));
            creature.setUnkfcb(file.readUnsignedShort());
            creature.setUnk4(file.readIntegerAsFloat());
            creature.setAnimation(AnimationType.DRUNKED_IDLE, readArtResource(file));
            creature.setSpecial1Swipe(file.readByteAsEnum(Creature.Swipe.class));
            creature.setSpecial2Swipe(file.readByteAsEnum(Creature.Swipe.class));
            creature.setFirstPersonMeleeResource(readArtResource(file));
            creature.setUnk6(file.readUnsignedInteger());
            creature.getAttributes().setTortureHpChange(file.readShort());
            creature.getAttributes().setTortureMoodChange(file.readShort());
            creature.setAnimation(AnimationType.SWIPE, readArtResource(file));
            creature.setAnimation(AnimationType.IDLE_3, readArtResource(file));
            creature.setAnimation(AnimationType.IDLE_4, readArtResource(file));
            creature.setAnimation(AnimationType.IDLE_3_1, readArtResource(file));
            creature.setAnimation(AnimationType.IDLE_4_1, readArtResource(file));
            creature.setAnimation(AnimationType.DIG, readArtResource(file));

            OffsetType[] offsetTypes = new OffsetType[]{OffsetType.FALL_BACK_GET_UP,
                OffsetType.PRAYING, OffsetType.CORPSE, OffsetType.OFFSET_5,
                OffsetType.OFFSET_6, OffsetType.OFFSET_7, OffsetType.OFFSET_8};
            for (OffsetType type : offsetTypes) {
                creature.setAnimationOffsets(type,
                        file.readIntegerAsFloat(),
                        file.readIntegerAsFloat(),
                        file.readIntegerAsFloat()
                );
            }
            creature.setAnimation(AnimationType.BACK_OFF, readArtResource(file));
            X1323[] x1323s = new X1323[48];
            for (int x = 0; x < x1323s.length; x++) {
                X1323 x1323 = creature.new X1323();
                x1323.setX00(file.readUnsignedShort());
                x1323.setX02(file.readUnsignedShort());
                x1323s[x] = x1323;
            }
            creature.setX1323(x1323s);
            creature.setAnimation(AnimationType.STAND_STILL, readArtResource(file));
            creature.setAnimation(AnimationType.STEALTH_WALK, readArtResource(file));
            creature.setAnimation(AnimationType.DEATH_POSE, readArtResource(file));
            creature.setUniqueNameTextId(file.readUnsignedShort());
            int[] x14e1 = new int[2];
            for (int x = 0; x < x14e1.length; x++) {
                x14e1[x] = file.readUnsignedInteger();
            }
            creature.setX14e1(x14e1);
            creature.setFirstPersonSpecialAbility1Count(file.readUnsignedInteger());
            creature.setFirstPersonSpecialAbility2Count(file.readUnsignedInteger());
            creature.setUniqueResource(readArtResource(file));
            creature.setFlags3(file.readIntegerAsFlag(Creature.CreatureFlag3.class));

            // The normal file stops here, but if it is the bigger one, continue
            if (header.getItemSize() > CREATURE_SIZE) {
                short[] unknownExtraBytes = new short[80];
                for (int x = 0; x < unknownExtraBytes.length; x++) {
                    unknownExtraBytes[x] = file.readUnsignedByte();
                }
                creature.setUnknownExtraBytes(unknownExtraBytes);
                creature.setFlags2(file.readIntegerAsFlag(Creature.CreatureFlag2.class));
                creature.setUnknown(file.readUnsignedShort());
                creature.setUnknown_1(file.readShortAsFloat());
            }

            // Add to the hash by the creature ID
            creatures.put(creature.getCreatureId(), creature);

            // Set the imp
            if (imp == null && creature.getFlags().contains(Creature.CreatureFlag.IS_WORKER)
                    && creature.getFlags().contains(Creature.CreatureFlag.IS_EVIL)) {
                imp = creature;
            }

            // Set the dwarf
            if (dwarf == null && creature.getFlags().contains(Creature.CreatureFlag.IS_WORKER)
                    && !creature.getFlags().contains(Creature.CreatureFlag.IS_EVIL)) {
                dwarf = creature;
            }

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
    private Creature.JobPreference[] readJobPreferences(int count, Creature creature, IResourceReader file) throws IOException {
        Creature.JobPreference[] preferences = new Creature.JobPreference[count];
        for (int x = 0; x < preferences.length; x++) {
            Creature.JobPreference jobPreference = creature.new JobPreference();
            jobPreference.setJobType(file.readIntegerAsEnum(Creature.JobType.class));
            jobPreference.setMoodChange(file.readUnsignedShort());
            jobPreference.setManaChange(file.readUnsignedShort());
            jobPreference.setChance(file.readUnsignedByte());
            jobPreference.setX09(file.readUnsignedByte());
            jobPreference.setX0a(file.readUnsignedByte());
            jobPreference.setX0b(file.readUnsignedByte());
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
    private Light readLight(IResourceReader file) throws IOException {
        Light light = new Light();

        // Read the data
        light.setmKPos(file.readIntegerAsFloat(),
                file.readIntegerAsFloat(),
                file.readIntegerAsFloat());
        light.setRadius(file.readIntegerAsFloat());
        light.setFlags(file.readIntegerAsFlag(Light.LightFlag.class));
        light.setColor(file.readUnsignedByte(), file.readUnsignedByte(),
                file.readUnsignedByte(), file.readUnsignedByte());

        return light;
    }

    /**
     * Reads the Objects.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readObjects(KwdHeader header, IResourceReader file) throws IOException {

        // Read the objects catalog
        if (objects == null) {
            LOGGER.info("Reading objects!");
            objects = new HashMap<>(header.getItemCount());
        } else {
            LOGGER.warning("Overrides objects!");
        }

        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();
            GameObject object = new GameObject();

            object.setName(file.readString(32).trim());
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
            object.setWidth(file.readIntegerAsFloat());
            object.setHeight(file.readIntegerAsFloat());
            object.setMass(file.readIntegerAsFloat());
            object.setSpeed(file.readIntegerAsFloat());
            object.setAirFriction(file.readIntegerAsDouble());
            object.setMaterial(file.readByteAsEnum(Material.class));
            short[] unknown3 = new short[3];
            for (int x = 0; x < unknown3.length; x++) {
                unknown3[x] = file.readUnsignedByte();
            }
            object.setUnknown3(unknown3);
            object.setFlags(file.readIntegerAsFlag(GameObject.ObjectFlag.class));
            object.setHp(file.readUnsignedShort());
            object.setMaxAngle(file.readUnsignedShort());
            object.setX34c(file.readUnsignedShort());
            object.setManaValue(file.readUnsignedShort());
            object.setTooltipStringId(file.readUnsignedShort());
            object.setNameStringId(file.readUnsignedShort());
            object.setSlapEffectId(file.readUnsignedShort());
            object.setDeathEffectId(file.readUnsignedShort());
            object.setMiscEffectId(file.readUnsignedShort());
            object.setObjectId(file.readUnsignedByte());
            object.setStartState(file.readByteAsEnum(GameObject.State.class));
            object.setRoomCapacity(file.readUnsignedByte());
            object.setPickUpPriority(file.readUnsignedByte());

            object.setSoundCategory(file.readString(32).trim());

            // Add to the hash by the object ID
            objects.put(object.getObjectId(), object);

            // See special objects
            if (levelGem == null && object.getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_LEVEL_GEM)) {
                levelGem = object;
            }

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
    private void readCreatureSpells(KwdHeader header, IResourceReader file) throws IOException {

        // Read the creature spells catalog
        if (creatureSpells == null) {
            LOGGER.info("Reading creature spells!");
            creatureSpells = new HashMap<>(header.getItemCount());
        } else {
            LOGGER.warning("Overrides creature spells!");
        }

        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();
            CreatureSpell creatureSpell = new CreatureSpell();

            creatureSpell.setName(file.readString(32).trim());
            creatureSpell.setEditorIcon(readArtResource(file));
            creatureSpell.setGuiIcon(readArtResource(file));
            creatureSpell.setShotData1(file.readUnsignedInteger());
            creatureSpell.setShotData2(file.readUnsignedInteger());
            creatureSpell.setRange(file.readIntegerAsFloat());
            creatureSpell.setFlags(file.readIntegerAsFlag(CreatureSpell.CreatureSpellFlag.class));
            creatureSpell.setCombatPoints(file.readUnsignedShort());
            creatureSpell.setSoundEvent(file.readUnsignedShort());
            creatureSpell.setNameStringId(file.readUnsignedShort());
            creatureSpell.setTooltipStringId(file.readUnsignedShort());
            creatureSpell.setGeneralDescriptionStringId(file.readUnsignedShort());
            creatureSpell.setStrengthStringId(file.readUnsignedShort());
            creatureSpell.setWeaknessStringId(file.readUnsignedShort());
            creatureSpell.setCreatureSpellId(file.readUnsignedByte());
            creatureSpell.setShotTypeId(file.readUnsignedByte());
            creatureSpell.setAlternativeShotId(file.readUnsignedByte());
            creatureSpell.setAlternativeRoomId(file.readUnsignedByte());
            creatureSpell.setRechargeTime(file.readIntegerAsFloat());
            creatureSpell.setAlternativeShot(file.readByteAsEnum(CreatureSpell.AlternativeShot.class));
            short[] data3 = new short[27];
            for (int x = 0; x < data3.length; x++) {
                data3[x] = file.readUnsignedByte();
            }
            creatureSpell.setUnused(data3);

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
    private void readEffectElements(KwdHeader header, IResourceReader file) throws IOException {

        // Read the effect elements catalog
        if (effectElements == null) {
            LOGGER.info("Reading effect elements!");
            effectElements = new HashMap<>(header.getItemCount());
        } else {
            LOGGER.warning("Overrides effect elements!");
        }

        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();
            EffectElement effectElement = new EffectElement();

            effectElement.setName(file.readString(32).trim());
            effectElement.setArtResource(readArtResource(file));
            effectElement.setMass(file.readIntegerAsFloat());
            effectElement.setAirFriction(file.readIntegerAsDouble());
            effectElement.setElasticity(file.readIntegerAsDouble());
            effectElement.setMinSpeedXy(file.readIntegerAsFloat());
            effectElement.setMaxSpeedXy(file.readIntegerAsFloat());
            effectElement.setMinSpeedYz(file.readIntegerAsFloat());
            effectElement.setMaxSpeedYz(file.readIntegerAsFloat());
            effectElement.setMinScale(file.readIntegerAsFloat());
            effectElement.setMaxScale(file.readIntegerAsFloat());
            effectElement.setScaleRatio(file.readIntegerAsFloat());
            effectElement.setFlags(file.readIntegerAsFlag(EffectElement.EffectElementFlag.class));
            effectElement.setEffectElementId(file.readUnsignedShort());
            effectElement.setMinHp(file.readUnsignedShort());
            effectElement.setMaxHp(file.readUnsignedShort());
            effectElement.setDeathElementId(file.readUnsignedShort());
            effectElement.setHitSolidElementId(file.readUnsignedShort());
            effectElement.setHitWaterElementId(file.readUnsignedShort());
            effectElement.setHitLavaElementId(file.readUnsignedShort());
            effectElement.setColor(new Color(file.readUnsignedByte(), file.readUnsignedByte(), file.readUnsignedByte()));
            effectElement.setRandomColorIndex(file.readUnsignedByte());
            effectElement.setTableColorIndex(file.readUnsignedByte());
            effectElement.setFadePercentage(file.readUnsignedByte());
            effectElement.setNextEffectId(file.readUnsignedShort());

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
    private void readEffects(KwdHeader header, IResourceReader file) throws IOException {

        // Read the effects catalog
        if (effects == null) {
            LOGGER.info("Reading effects!");
            effects = new HashMap<>(header.getItemCount());
        } else {
            LOGGER.warning("Overrides effects!");
        }

        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();
            Effect effect = new Effect();

            effect.setName(file.readString(32).trim());
            effect.setArtResource(readArtResource(file));
            effect.setLight(readLight(file));
            effect.setMass(file.readIntegerAsFloat());
            effect.setAirFriction(file.readIntegerAsDouble());
            effect.setElasticity(file.readIntegerAsDouble());
            effect.setRadius(file.readIntegerAsFloat());
            effect.setMinSpeedXy(file.readIntegerAsFloat());
            effect.setMaxSpeedXy(file.readIntegerAsFloat());
            effect.setMinSpeedYz(file.readIntegerAsFloat());
            effect.setMaxSpeedYz(file.readIntegerAsFloat());
            effect.setMinScale(file.readIntegerAsFloat());
            effect.setMaxScale(file.readIntegerAsFloat());
            effect.setFlags(file.readIntegerAsFlag(Effect.EffectFlag.class));
            effect.setEffectId(file.readUnsignedShort());
            effect.setMinHp(file.readUnsignedShort());
            effect.setMaxHp(file.readUnsignedShort());
            effect.setFadeDuration(file.readUnsignedShort());
            effect.setNextEffectId(file.readUnsignedShort());
            effect.setDeathEffectId(file.readUnsignedShort());
            effect.setHitSolidEffectId(file.readUnsignedShort());
            effect.setHitWaterEffectId(file.readUnsignedShort());
            effect.setHitLavaEffectId(file.readUnsignedShort());
            List<Integer> generateIds = new ArrayList<>(8);
            for (int x = 0; x < 8; x++) {
                int id = file.readUnsignedShort();
                if (id > 0) {
                    generateIds.add(id);
                }
            }
            effect.setGenerateIds(generateIds);
            effect.setOuterOriginRange(file.readUnsignedShort());
            effect.setLowerHeightLimit(file.readUnsignedShort());
            effect.setUpperHeightLimit(file.readUnsignedShort());
            effect.setOrientationRange(file.readUnsignedShort());
            effect.setSpriteSpinRateRange(file.readUnsignedShort());
            effect.setWhirlpoolRate(file.readUnsignedShort());
            effect.setDirectionalSpread(file.readUnsignedShort());
            effect.setCircularPathRate(file.readUnsignedShort());
            effect.setInnerOriginRange(file.readUnsignedShort());
            effect.setGenerateRandomness(file.readUnsignedShort());
            effect.setMisc2(file.readUnsignedShort());
            effect.setMisc3(file.readUnsignedShort());
            effect.setGenerationType(file.readByteAsEnum(Effect.GenerationType.class));
            effect.setElementsPerTurn(file.readUnsignedByte());
            effect.setUnknown3(file.readUnsignedShort());

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
    private void readKeeperSpells(KwdHeader header, IResourceReader file) throws IOException {

        // Read the keeper spells catalog
        if (keeperSpells == null) {
            LOGGER.info("Reading keeper spells!");
            keeperSpells = new HashMap<>(header.getItemCount());
        } else {
            LOGGER.warning("Overrides keeper spells!");
        }

        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();
            KeeperSpell keeperSpell = new KeeperSpell();

            keeperSpell.setName(file.readString(32).trim());
            keeperSpell.setGuiIcon(readArtResource(file));
            keeperSpell.setEditorIcon(readArtResource(file));
            keeperSpell.setXc8(file.readInteger());
            keeperSpell.setRechargeTime(file.readIntegerAsFloat());
            keeperSpell.setShotData1(file.readInteger());
            keeperSpell.setShotData2(file.readInteger());
            keeperSpell.setResearchTime(file.readUnsignedShort());
            keeperSpell.setTargetRule(file.readByteAsEnum(KeeperSpell.TargetRule.class));
            keeperSpell.setOrderInEditor(file.readUnsignedByte());
            keeperSpell.setFlags(file.readIntegerAsFlag(KeeperSpell.KeeperSpellFlag.class));
            keeperSpell.setXe0Unreferenced(file.readUnsignedShort());
            keeperSpell.setManaDrain(file.readUnsignedShort());
            keeperSpell.setTooltipStringId(file.readUnsignedShort());
            keeperSpell.setNameStringId(file.readUnsignedShort());
            keeperSpell.setGeneralDescriptionStringId(file.readUnsignedShort());
            keeperSpell.setStrengthStringId(file.readUnsignedShort());
            keeperSpell.setWeaknessStringId(file.readUnsignedShort());
            keeperSpell.setKeeperSpellId(file.readUnsignedByte());
            keeperSpell.setCastRule(file.readByteAsEnum(KeeperSpell.CastRule.class));
            keeperSpell.setShotTypeId(file.readUnsignedByte());

            keeperSpell.setSoundCategory(file.readString(32).trim());
            keeperSpell.setBonusRTime(file.readUnsignedShort());
            keeperSpell.setBonusShotTypeId(file.readUnsignedByte());
            keeperSpell.setBonusShotData1(file.readInteger());
            keeperSpell.setBonusShotData2(file.readInteger());
            keeperSpell.setManaCost(file.readInteger());
            keeperSpell.setBonusIcon(readArtResource(file));

            keeperSpell.setSoundCategoryGui(file.readString(32).trim());
            keeperSpell.setHandAnimId(file.readByteAsEnum(KeeperSpell.HandAnimId.class));
            keeperSpell.setNoGoHandAnimId(file.readByteAsEnum(KeeperSpell.HandAnimId.class));

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
    private void readThings(KwdHeader header, IResourceReader file) throws IOException {

        // Read the requested Things file
        if (thingsByType == null) {
            LOGGER.info("Reading things!");
            thingsByType = new HashMap<>(12);
        } else {
            LOGGER.warning("Overrides things!");
        }

        for (int i = 0; i < header.getItemCount(); i++) {
            Thing thing = null;
            int[] thingTag = new int[2];
            for (int x = 0; x < thingTag.length; x++) {
                thingTag[x] = file.readUnsignedInteger();
            }
            long offset = file.getFilePointer();

            // Figure out the type
            switch (thingTag[0]) {
                case THING_OBJECT: {

                    // Object (door & trap crates, objects...)
                    thing = new Thing.Object();
                    ((Thing.Object) thing).setPosX(file.readInteger());
                    ((Thing.Object) thing).setPosY(file.readInteger());
                    short unknown1[] = new short[4];
                    for (int x = 0; x < unknown1.length; x++) {
                        unknown1[x] = file.readUnsignedByte();
                    }
                    ((Thing.Object) thing).setUnknown1(unknown1);
                    ((Thing.Object) thing).setKeeperSpellId(file.readInteger());
                    ((Thing.Object) thing).setMoneyAmount(file.readInteger());
                    ((Thing.Object) thing).setTriggerId(file.readUnsignedShort());
                    ((Thing.Object) thing).setObjectId(file.readUnsignedByte());
                    ((Thing.Object) thing).setPlayerId(file.readUnsignedByte());

                    addThing((Thing.Object) thing);
                    break;
                }
                case THING_TRAP: {

                    // Trap
                    thing = new Thing.Trap();
                    ((Thing.Trap) thing).setPosX(file.readInteger());
                    ((Thing.Trap) thing).setPosY(file.readInteger());
                    ((Thing.Trap) thing).setUnknown1(file.readInteger());
                    ((Thing.Trap) thing).setNumberOfShots(file.readUnsignedByte());
                    ((Thing.Trap) thing).setTrapId(file.readUnsignedByte());
                    ((Thing.Trap) thing).setPlayerId(file.readUnsignedByte());
                    ((Thing.Trap) thing).setUnknown2(file.readUnsignedByte());

                    addThing((Thing.Trap) thing);
                    break;
                }
                case THING_DOOR: {

                    // Door
                    thing = new Thing.Door();
                    ((Thing.Door) thing).setPosX(file.readInteger());
                    ((Thing.Door) thing).setPosY(file.readInteger());
                    ((Thing.Door) thing).setUnknown1(file.readInteger());
                    ((Thing.Door) thing).setTriggerId(file.readUnsignedShort());
                    ((Thing.Door) thing).setDoorId(file.readUnsignedByte());
                    ((Thing.Door) thing).setPlayerId(file.readUnsignedByte());
                    ((Thing.Door) thing).setFlag(file.readByteAsEnum(Thing.Door.DoorFlag.class));
                    short unknown2[] = new short[3];
                    for (int x = 0; x < unknown2.length; x++) {
                        unknown2[x] = file.readUnsignedByte();
                    }
                    ((Thing.Door) thing).setUnknown2(unknown2);

                    addThing((Thing.Door) thing);
                    break;
                }
                case THING_ACTION_POINT: {

                    // ActionPoint
                    thing = new ActionPoint();
                    ((ActionPoint) thing).setStartX(file.readInteger());
                    ((ActionPoint) thing).setStartY(file.readInteger());
                    ((ActionPoint) thing).setEndX(file.readInteger());
                    ((ActionPoint) thing).setEndY(file.readInteger());
                    ((ActionPoint) thing).setWaitDelay(file.readUnsignedShort());
                    ((ActionPoint) thing).setFlags(file.readShortAsFlag(ActionPointFlag.class));
                    ((ActionPoint) thing).setTriggerId(file.readUnsignedShort());
                    ((ActionPoint) thing).setId(file.readUnsignedByte());
                    ((ActionPoint) thing).setNextWaypointId(file.readUnsignedByte());

                    ((ActionPoint) thing).setName(file.readString(32).trim());

                    addThing((Thing.ActionPoint) thing);
                    break;
                }
                case THING_NEUTRAL_CREATURE: {

                    // Neutral creature
                    thing = new Thing.NeutralCreature();
                    ((NeutralCreature) thing).setPosX(file.readInteger());
                    ((NeutralCreature) thing).setPosY(file.readInteger());
                    ((NeutralCreature) thing).setPosZ(file.readInteger());
                    ((NeutralCreature) thing).setGoldHeld(file.readUnsignedShort());
                    ((NeutralCreature) thing).setLevel(file.readUnsignedByte());
                    ((NeutralCreature) thing).setFlags(file.readByteAsFlag(Thing.Creature.CreatureFlag.class));
                    ((NeutralCreature) thing).setInitialHealth(file.readInteger());
                    ((NeutralCreature) thing).setTriggerId(file.readUnsignedShort());
                    ((NeutralCreature) thing).setCreatureId(file.readUnsignedByte());
                    ((NeutralCreature) thing).setUnknown1(file.readUnsignedByte());

                    addThing((Thing.NeutralCreature) thing);
                    break;
                }
                case THING_GOOD_CREATURE: {

                    // Good creature
                    thing = new Thing.GoodCreature();
                    ((GoodCreature) thing).setPosX(file.readInteger());
                    ((GoodCreature) thing).setPosY(file.readInteger());
                    ((GoodCreature) thing).setPosZ(file.readInteger());
                    ((GoodCreature) thing).setGoldHeld(file.readUnsignedShort());
                    ((GoodCreature) thing).setLevel(file.readUnsignedByte());
                    ((GoodCreature) thing).setFlags(file.readByteAsFlag(Thing.Creature.CreatureFlag.class));
                    ((GoodCreature) thing).setObjectiveTargetActionPointId(file.readInteger());
                    ((GoodCreature) thing).setInitialHealth(file.readInteger());
                    ((GoodCreature) thing).setTriggerId(file.readUnsignedShort());
                    ((GoodCreature) thing).setObjectiveTargetPlayerId(file.readUnsignedByte());
                    ((GoodCreature) thing).setObjective(file.readByteAsEnum(Thing.HeroParty.Objective.class));
                    ((GoodCreature) thing).setCreatureId(file.readUnsignedByte());
                    short unknown1[] = new short[2];
                    for (int x = 0; x < unknown1.length; x++) {
                        unknown1[x] = file.readUnsignedByte();
                    }
                    ((GoodCreature) thing).setUnknown1(unknown1);
                    ((GoodCreature) thing).setFlags2(file.readByteAsFlag(Thing.Creature.CreatureFlag2.class));

                    addThing((Thing.GoodCreature) thing);
                    break;
                }
                case THING_KEEPER_CREATURE: {

                    // Creature
                    thing = new Thing.KeeperCreature();
                    ((KeeperCreature) thing).setPosX(file.readInteger());
                    ((KeeperCreature) thing).setPosY(file.readInteger());
                    ((KeeperCreature) thing).setPosZ(file.readInteger());
                    ((KeeperCreature) thing).setGoldHeld(file.readUnsignedShort());
                    ((KeeperCreature) thing).setLevel(file.readUnsignedByte());
                    ((KeeperCreature) thing).setFlags(file.readByteAsFlag(KeeperCreature.CreatureFlag.class));
                    ((KeeperCreature) thing).setInitialHealth(file.readInteger());
                    ((KeeperCreature) thing).setObjectiveTargetActionPointId(file.readInteger());
                    ((KeeperCreature) thing).setTriggerId(file.readUnsignedShort());
                    ((KeeperCreature) thing).setCreatureId(file.readUnsignedByte());
                    ((KeeperCreature) thing).setPlayerId(file.readUnsignedByte());

                    addThing((Thing.KeeperCreature) thing);
                    break;
                }
                case THING_HERO_PARTY: {

                    // HeroParty
                    thing = new HeroParty();

                    ((HeroParty) thing).setName(file.readString(32).trim());
                    ((HeroParty) thing).setTriggerId(file.readUnsignedShort());
                    ((HeroParty) thing).setId(file.readUnsignedByte());
                    ((HeroParty) thing).setX23(file.readInteger());
                    ((HeroParty) thing).setX27(file.readInteger());
                    List<GoodCreature> heroPartyMembers = new ArrayList<>(16);
                    for (int x = 0; x < 16; x++) {
                        GoodCreature creature = new GoodCreature();
                        creature.setPosX(file.readInteger());
                        creature.setPosY(file.readInteger());
                        creature.setPosZ(file.readInteger());
                        creature.setGoldHeld(file.readUnsignedShort());
                        creature.setLevel(file.readUnsignedByte());
                        creature.setFlags(file.readByteAsFlag(KeeperCreature.CreatureFlag.class));
                        creature.setObjectiveTargetActionPointId(file.readInteger());
                        creature.setInitialHealth(file.readInteger());
                        creature.setTriggerId(file.readUnsignedShort());
                        creature.setObjectiveTargetPlayerId(file.readUnsignedByte());
                        creature.setObjective(file.readByteAsEnum(Thing.HeroParty.Objective.class));
                        creature.setCreatureId(file.readUnsignedByte());
                        short unknown1[] = new short[2];
                        for (int index = 0; index < unknown1.length; index++) {
                            unknown1[index] = file.readUnsignedByte();
                        }
                        creature.setUnknown1(unknown1);
                        creature.setFlags2(file.readByteAsFlag(Thing.Creature.CreatureFlag2.class));

                        // If creature id is 0, it is safe to say this is not a valid entry
                        if (creature.getCreatureId() > 0) {
                            heroPartyMembers.add(creature);
                        }
                    }
                    ((HeroParty) thing).setHeroPartyMembers(heroPartyMembers);

                    addThing((Thing.HeroParty) thing);
                    break;
                }
                case THING_DEAD_BODY: {

                    // Dead body
                    thing = new Thing.DeadBody();
                    ((Thing.DeadBody) thing).setPosX(file.readInteger());
                    ((Thing.DeadBody) thing).setPosY(file.readInteger());
                    ((Thing.DeadBody) thing).setPosZ(file.readInteger());
                    ((Thing.DeadBody) thing).setGoldHeld(file.readUnsignedShort());
                    ((Thing.DeadBody) thing).setCreatureId(file.readUnsignedByte());
                    ((Thing.DeadBody) thing).setPlayerId(file.readUnsignedByte());

                    addThing((Thing.DeadBody) thing);
                    break;
                }
                case THING_EFFECT_GENERATOR: {

                    // Effect generator
                    thing = new Thing.EffectGenerator();
                    ((Thing.EffectGenerator) thing).setPosX(file.readInteger());
                    ((Thing.EffectGenerator) thing).setPosY(file.readInteger());
                    ((Thing.EffectGenerator) thing).setX08(file.readInteger());
                    ((Thing.EffectGenerator) thing).setX0c(file.readInteger());
                    ((Thing.EffectGenerator) thing).setX10(file.readUnsignedShort());
                    ((Thing.EffectGenerator) thing).setX12(file.readUnsignedShort());
                    List<Integer> effectIds = new ArrayList<>(4);
                    for (int x = 0; x < 4; x++) {
                        int effectId = file.readUnsignedShort();
                        if (effectId > 0) {
                            effectIds.add(effectId);
                        }
                    }
                    ((Thing.EffectGenerator) thing).setEffectIds(effectIds);
                    ((Thing.EffectGenerator) thing).setFrequency(file.readUnsignedByte());
                    ((Thing.EffectGenerator) thing).setId(file.readUnsignedByte());
                    short[] pad = new short[6];
                    for (int x = 0; x < pad.length; x++) {
                        pad[x] = file.readUnsignedByte();
                    }
                    ((Thing.EffectGenerator) thing).setPad(pad);

                    addThing((Thing.EffectGenerator) thing);
                    break;
                }
                case THING_ROOM: {

                    // Room
                    thing = new Thing.Room();
                    ((Thing.Room) thing).setPosX(file.readInteger());
                    ((Thing.Room) thing).setPosY(file.readInteger());
                    ((Thing.Room) thing).setX08(file.readInteger());
                    ((Thing.Room) thing).setX0c(file.readUnsignedShort());
                    ((Thing.Room) thing).setDirection(file.readByteAsEnum(Thing.Room.Direction.class));
                    ((Thing.Room) thing).setX0f(file.readUnsignedByte());
                    ((Thing.Room) thing).setInitialHealth(file.readUnsignedShort());
                    ((Thing.Room) thing).setRoomType(file.readByteAsEnum(Thing.Room.RoomType.class));
                    ((Thing.Room) thing).setPlayerId(file.readUnsignedByte());

                    addThing((Thing.Room) thing);
                    break;
                }
                case THING_CAMERA: {

                    // TODO: decode values
                    thing = new Thing.Camera();
                    ((Thing.Camera) thing).setPosition(file.readIntegerAsFloat(),
                            file.readIntegerAsFloat(),
                            file.readIntegerAsFloat());
                    ((Thing.Camera) thing).setPositionMinClipExtent(file.readIntegerAsFloat(),
                            file.readIntegerAsFloat(),
                            file.readIntegerAsFloat());
                    ((Thing.Camera) thing).setPositionMaxClipExtent(file.readIntegerAsFloat(),
                            file.readIntegerAsFloat(),
                            file.readIntegerAsFloat());
                    ((Thing.Camera) thing).setViewDistanceValue(file.readIntegerAsFloat());
                    ((Thing.Camera) thing).setViewDistanceMin(file.readIntegerAsFloat());
                    ((Thing.Camera) thing).setViewDistanceMax(file.readIntegerAsFloat());
                    ((Thing.Camera) thing).setZoomValue(file.readIntegerAsFloat());
                    ((Thing.Camera) thing).setZoomValueMin(file.readIntegerAsFloat());
                    ((Thing.Camera) thing).setZoomValueMax(file.readIntegerAsFloat());
                    ((Thing.Camera) thing).setLensValue(file.readIntegerAsFloat());
                    ((Thing.Camera) thing).setLensValueMin(file.readIntegerAsFloat());
                    ((Thing.Camera) thing).setLensValueMax(file.readIntegerAsFloat());
                    ((Thing.Camera) thing).setFlags(file.readIntegerAsFlag(Thing.Camera.CameraFlag.class));
                    ((Thing.Camera) thing).setAngleYaw(file.readUnsignedShort());
                    ((Thing.Camera) thing).setAngleRoll(file.readUnsignedShort());
                    ((Thing.Camera) thing).setAnglePitch(file.readUnsignedShort());
                    ((Thing.Camera) thing).setId((short) file.readUnsignedShort());

                    addThing((Thing.Camera) thing);
                    break;
                }
                default: {

                    // Just skip the bytes
                    file.skipBytes(thingTag[1]);
                    LOGGER.log(Level.WARNING, "Unsupported thing type {0}!", thingTag[0]);
                }
            }

            // Check file offset
            file.checkOffset(thingTag[1], offset);
        }
    }

    private <T extends Thing> void addThing(T thing) {
        List<T> thingList = (List<T>) thingsByType.get(thing.getClass());
        if (thingList == null) {
            thingList = new ArrayList<>();
            thingsByType.put(thing.getClass(), thingList);
        }
        thingList.add(thing);
    }

    /**
     * Reads the Shots.kwd
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readShots(KwdHeader header, IResourceReader file) throws IOException {

        // Read the shots catalog
        if (shots == null) {
            LOGGER.info("Reading shots!");
            shots = new HashMap<>(header.getItemCount());
        } else {
            LOGGER.warning("Overrides shots!");
        }

        for (int i = 0; i < header.getItemCount(); i++) {
            long offset = file.getFilePointer();

            // One shot is 239 bytes
            Shot shot = new Shot();

            shot.setName(file.readString(32).trim());
            shot.setMeshResource(readArtResource(file));
            shot.setLight(readLight(file));
            shot.setAirFriction(file.readIntegerAsDouble());
            shot.setMass(file.readIntegerAsFloat());
            shot.setSpeed(file.readIntegerAsFloat());
            shot.setData1(file.readUnsignedInteger());
            shot.setData2(file.readUnsignedInteger());
            shot.setShotProcessFlags(file.readIntegerAsFlag(Shot.ShotProcessFlag.class));
            shot.setRadius(file.readIntegerAsFloat());
            shot.setFlags(file.readIntegerAsFlag(Shot.ShotFlag.class));
            shot.setGeneralEffectId(file.readUnsignedShort());
            shot.setCreationEffectId(file.readUnsignedShort());
            shot.setDeathEffectId(file.readUnsignedShort());
            shot.setTimedEffectId(file.readUnsignedShort());
            shot.setHitSolidEffectId(file.readUnsignedShort());
            shot.setHitLavaEffectId(file.readUnsignedShort());
            shot.setHitWaterEffect(file.readUnsignedShort());
            shot.setHitThingEffectId(file.readUnsignedShort());
            shot.setHealth(file.readUnsignedShort());
            shot.setShotId(file.readUnsignedByte());
            shot.setDeathShotId(file.readUnsignedByte());
            shot.setTimedDelay(file.readUnsignedByte());
            shot.setHitSolidShotId(file.readUnsignedByte());
            shot.setHitLavaShotId(file.readUnsignedByte());
            shot.setHitWaterShotId(file.readUnsignedByte());
            shot.setHitThingShotId(file.readUnsignedByte());
            shot.setDamageType(file.readByteAsEnum(Shot.DamageType.class));
            shot.setCollideType(file.readByteAsEnum(Shot.CollideType.class));
            shot.setProcessType(file.readByteAsEnum(Shot.ProcessType.class));
            shot.setAttackCategory(file.readByteAsEnum(Shot.AttackCategory.class));

            shot.setSoundCategory(file.readString(32).trim());
            shot.setThreat(file.readUnsignedShort());
            shot.setBurnDuration(file.readIntegerAsFloat());

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
    private void readTriggers(KwdHeader header, IResourceReader file) throws IOException {

        // Read the requested Triggers file
        if (triggers == null) {
            LOGGER.info("Reading triggers!");
            triggers = new HashMap<>(header.getItemCount());
        } else {
            LOGGER.warning("Overrides triggers!");
        }

        for (int i = 0; i < header.getItemCount(); i++) {
            Trigger trigger = null;
            int[] triggerTag = new int[2];
            for (int x = 0; x < triggerTag.length; x++) {
                triggerTag[x] = file.readUnsignedInteger();
            }
            long offset = file.getFilePointer();

            // Figure out the type
            switch (triggerTag[0]) {
                case TRIGGER_GENERIC: {
                    long start = file.getFilePointer();
                    file.seek(start + triggerTag[1] - 2);

                    trigger = new TriggerGeneric(this);
                    ((TriggerGeneric) trigger).setType(file.readByteAsEnum(TriggerGeneric.TargetType.class));
                    trigger.setRepeatTimes(file.readUnsignedByte());

                    file.seek(start);
                    switch (((TriggerGeneric) trigger).getType()) {
                        case AP_CONGREGATE_IN:
                        case AP_POSESSED_CREATURE_ENTERS:
                            ((TriggerGeneric) trigger).setTargetValueComparison(file.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            trigger.setUserData("playerId", file.readUnsignedByte());
                            trigger.setUserData("targetId", file.readUnsignedByte()); // creatureId, objectId
                            trigger.setUserData("targetType", file.readUnsignedByte()); // 3 = Creature, 6 = Object
                            trigger.setUserData("value", file.readUnsignedInteger());
                            break;

                        case AP_SLAB_TYPES:
                            ((TriggerGeneric) trigger).setTargetValueComparison(file.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            trigger.setUserData("playerId", file.readUnsignedByte());
                            trigger.setUserData("terrainId", file.readUnsignedByte());
                            file.readAndCheckNull(1); // file.skipBytes(1); // 0 = None
                            trigger.setUserData("value", file.readUnsignedInteger());
                            break;

                        case AP_TAG_PART_OF:
                        case AP_TAG_ALL_OF:
                        case AP_CLAIM_PART_OF:
                        case AP_CLAIM_ALL_OF:
                            ((TriggerGeneric) trigger).setTargetValueComparison(file.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            trigger.setUserData("playerId", file.readUnsignedByte());
                            file.readAndCheckNull(2); // file.skipBytes(2);
                            // trigger.setUserData("targetId", file.readUnsignedByte()); // 0 = None
                            // trigger.setUserData("targetType", file.readUnsignedByte()); // 0 = None
                            trigger.setUserData("value", file.readUnsignedInteger());
                            break;

                        case PLAYER_DUNGEON_BREACHED:
                        case PLAYER_ENEMY_BREACHED:
                            trigger.setUserData("playerId", file.readUnsignedByte()); // 0 = Any
                            file.readAndCheckNull(7); // file.skipBytes(7);
                            break;

                        case PLAYER_KILLED:
                            trigger.setUserData("playerId", file.readUnsignedByte()); // 0 = Any
                            file.readAndCheckNull(3); // file.skipBytes(7);
                            trigger.setUserData("value", file.readUnsignedInteger()); // FIXME unknown value
                            break;

                        case PLAYER_CREATURE_PICKED_UP:
                        case PLAYER_CREATURE_SLAPPED:
                        case PLAYER_CREATURE_SACKED:
                            trigger.setUserData("creatureId", file.readUnsignedByte()); // 0 = Any
                            file.readAndCheckNull(7); // file.skipBytes(7);
                            break;

                        case PLAYER_CREATURE_DROPPED:
                            trigger.setUserData("creatureId", file.readUnsignedByte()); // 0 = Any
                            trigger.setUserData("roomId", file.readUnsignedByte()); // 0 = Any
                            file.readAndCheckNull(6); // file.skipBytes(6);
                            break;

                        case PLAYER_CREATURES:
                        case PLAYER_HAPPY_CREATURES:
                        case PLAYER_ANGRY_CREATURES:
                            ((TriggerGeneric) trigger).setTargetValueComparison(file.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            trigger.setUserData("creatureId", file.readUnsignedByte());
                            trigger.setUserData("flag", file.readUnsignedByte()); // 0x1 = Value, !0x1 = Player
                            trigger.setUserData("playerId", file.readUnsignedByte());
                            trigger.setUserData("value", file.readUnsignedInteger());
                            break;

                        case PLAYER_CREATURES_KILLED:
                        case PLAYER_KILLS_CREATURES:
                            ((TriggerGeneric) trigger).setTargetValueComparison(file.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            trigger.setUserData("targetId", file.readUnsignedByte()); // playerId
                            trigger.setUserData("flag", file.readUnsignedByte()); // 0x1 = Value, !0x1 = Player
                            trigger.setUserData("playerId", file.readUnsignedByte());
                            trigger.setUserData("value", file.readUnsignedInteger());
                            break;

                        case PLAYER_ROOMS:
                        case PLAYER_ROOM_SLABS:
                        case PLAYER_ROOM_SIZE:
                        case PLAYER_ROOM_FURNITURE:
                            ((TriggerGeneric) trigger).setTargetValueComparison(file.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            trigger.setUserData("roomId", file.readUnsignedByte());
                            trigger.setUserData("flag", file.readUnsignedByte()); // 0x1 = Value, !0x1 = Player
                            trigger.setUserData("playerId", file.readUnsignedByte());
                            trigger.setUserData("value", file.readUnsignedInteger());
                            break;

                        case PLAYER_DOORS:
                        case PLAYER_TRAPS:
                        case PLAYER_KEEPER_SPELL:
                        case PLAYER_DESTROYS:
                            ((TriggerGeneric) trigger).setTargetValueComparison(file.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            trigger.setUserData("targetId", file.readUnsignedByte()); // doorId, trapId, keeperSpellId,
                            trigger.setUserData("flag", file.readUnsignedByte()); // 0x1 = Value, !0x1 = Player
                            trigger.setUserData("playerId", file.readUnsignedByte());
                            trigger.setUserData("value", file.readUnsignedInteger());
                            break;

                        case PLAYER_SLAPS:
                        case PLAYER_GOLD:
                        case PLAYER_GOLD_MINED:
                        case PLAYER_MANA:
                        case PLAYER_CREATURES_GROUPED:
                        case PLAYER_CREATURES_DYING:
                            ((TriggerGeneric) trigger).setTargetValueComparison(file.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            file.readAndCheckNull(1); // file.skipBytes(1);
                            // trigger.setUserData("targetId", file.readUnsignedByte()); // = 0
                            trigger.setUserData("flag", file.readUnsignedByte()); // 0x1 = Value, !0x1 = Player
                            trigger.setUserData("playerId", file.readUnsignedByte());
                            trigger.setUserData("value", file.readUnsignedInteger());
                            break;

                        case PLAYER_CREATURES_AT_LEVEL:
                            ((TriggerGeneric) trigger).setTargetValueComparison(file.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            // FIXME some bug in editor
                            trigger.setUserData("targetId", file.readUnsignedByte()); // = 0, must be a level
                            trigger.setUserData("flag", file.readUnsignedByte()); // 0x1 = Value, !0x1 = Player
                            trigger.setUserData("playerId", file.readUnsignedByte()); // level also
                            trigger.setUserData("value", file.readUnsignedInteger());
                            break;

                        case LEVEL_PAY_DAY:
                        case CREATURE_KILLED:
                        case CREATURE_SLAPPED:
                        case CREATURE_ATTACKED:
                        case CREATURE_IMPRISONED:
                        case CREATURE_TORTURED:
                        case CREATURE_CONVERTED:
                        case CREATURE_CLAIMED:
                        case CREATURE_ANGRY:
                        case CREATURE_AFRAID:
                        case CREATURE_STEALS:
                        case CREATURE_LEAVES:
                        case CREATURE_STUNNED:
                        case CREATURE_DYING:
                        case GUI_TRANSITION_ENDS:
                        case CREATURE_PICKED_UP:
                        case CREATURE_SACKED:
                        case CREATURE_PICKS_UP_PORTAL_GEM:
                        case CREATURE_HUNGER_SATED:
                        case PARTY_CREATED:
                            file.readAndCheckNull(8); // file.skipBytes(8);
                            break;

                        case CREATURE_CREATED:
                            file.readAndCheckNull(4); // file.skipBytes(4);
                            trigger.setUserData("value", file.readUnsignedInteger()); // FIXME unknown value
                            break;

                        case LEVEL_PLAYED:
                        case PARTY_MEMBERS_CAPTURED:
                        case CREATURE_EXPERIENCE_LEVEL:
                        case CREATURE_GOLD_HELD:
                        case CREATURE_HEALTH:
                        case LEVEL_TIME:
                        case LEVEL_CREATURES:
                            ((TriggerGeneric) trigger).setTargetValueComparison(file.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            file.readAndCheckNull(3); // file.skipBytes(3);
                            trigger.setUserData("value", file.readUnsignedInteger());
                            break;

                        case PARTY_MEMBERS_KILLED:
                        case PARTY_MEMBERS_INCAPACITATED:
                            ((TriggerGeneric) trigger).setTargetValueComparison(file.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            trigger.setUserData("unknown", file.readUnsignedByte()); // FIXME unknown value
                            file.readAndCheckNull(2); // file.skipBytes(2);
                            trigger.setUserData("value", file.readUnsignedInteger());
                            break;

                        case GUI_BUTTON_PRESSED:
                            // Misc Button = 0, Room = 1, Creature = 2, Door = 3, Trap = 4, Keeper Spell = 5
                            trigger.setUserData("targetType", file.readUnsignedByte());
                            trigger.setUserData("targetId", file.readUnsignedByte()); // buttonId, roomId, creatureId ...
                            file.readAndCheckNull(2); // file.skipBytes(2);
                            trigger.setUserData("value", file.readUnsignedInteger()); // FIXME unknown value
                            break;

                        case FLAG:
                            ((TriggerGeneric) trigger).setTargetValueComparison(file.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            trigger.setUserData("targetId", file.readUnsignedByte()); // flagId
                            trigger.setUserData("flag", file.readUnsignedByte()); // 0x1 = Value, !0x1 = Flag
                            trigger.setUserData("flagId", file.readUnsignedByte());
                            trigger.setUserData("value", file.readUnsignedInteger());
                            break;

                        case TIMER:
                            ((TriggerGeneric) trigger).setTargetValueComparison(file.readByteAsEnum(TriggerGeneric.ComparisonType.class));
                            trigger.setUserData("targetId", file.readUnsignedByte()); // timerId
                            trigger.setUserData("flag", file.readUnsignedByte()); // 0x1 = Value, !0x1 = Flag
                            trigger.setUserData("timerId", file.readUnsignedByte());
                            trigger.setUserData("value", file.readUnsignedInteger());
                            break;

                        default:
                            file.readAndCheckNull(8); // file.skipBytes(8);
                            LOGGER.warning("Unsupported Type of TriggerGeneric");
                            break;

                    }

                    trigger.setId(file.readUnsignedShort());
                    trigger.setIdNext(file.readUnsignedShort()); // SiblingID
                    trigger.setIdChild(file.readUnsignedShort()); // ChildID

                    file.skipBytes(2);
                    break;
                }
                case TRIGGER_ACTION: {

                    long start = file.getFilePointer();
                    file.seek(start + triggerTag[1] - 2);

                    trigger = new TriggerAction(this);
                    ((TriggerAction) trigger).setType(file.readByteAsEnum(TriggerAction.ActionType.class));
                    trigger.setRepeatTimes(file.readUnsignedByte());

                    file.seek(start);
                    switch (((TriggerAction) trigger).getType()) {
                        // in levels triggers
                        case ALTER_TERRAIN_TYPE:
                            trigger.setUserData("terrainId", file.readUnsignedByte());
                            trigger.setUserData("playerId", file.readUnsignedByte());
                            file.readAndCheckNull(2); // file.skipBytes(2);
                            trigger.setUserData("posX", file.readUnsignedShort());
                            trigger.setUserData("posY", file.readUnsignedShort());
                            break;

                        case COLLAPSE_HERO_GATE:
                            file.readAndCheckNull(4); // file.skipBytes(4);
                            trigger.setUserData("posX", file.readUnsignedShort());
                            trigger.setUserData("posY", file.readUnsignedShort());
                            break;

                        case CHANGE_ROOM_OWNER:
                            file.readAndCheckNull(1); // file.skipBytes(1);
                            trigger.setUserData("playerId", file.readUnsignedByte());
                            file.readAndCheckNull(2); // file.skipBytes(2);
                            trigger.setUserData("posX", file.readUnsignedShort());
                            trigger.setUserData("posY", file.readUnsignedShort());
                            break;

                        case SET_ALLIANCE:
                            trigger.setUserData("playerOneId", file.readUnsignedByte());
                            trigger.setUserData("playerTwoId", file.readUnsignedByte());
                            trigger.setUserData("available", file.readUnsignedByte()); // 0 = Create, !0 = Break
                            file.readAndCheckNull(5); // file.skipBytes(5);
                            break;

                        case SET_CREATURE_MOODS:
                        case SET_SYSTEM_MESSAGES:
                        case SET_TIMER_SPEECH:
                        case SET_WIDESCREEN_MODE:
                        case ALTER_SPEED:  // 0 = Walk, !0 = Run
                        case SET_FIGHT_FLAG: // 0 = Don`t Fight, !0 = Fight
                        case SET_PORTAL_STATUS: // 0 = Closed, !0 = Open
                            trigger.setUserData("available", file.readUnsignedByte());  // 0 = Off, !0 = On
                            file.readAndCheckNull(7); // file.skipBytes(7);
                            break;

                        case SET_SLAPS_LIMIT:
                            file.readAndCheckNull(4); // file.skipBytes(4);
                            trigger.setUserData("value", file.readUnsignedInteger()); // limit 4 bytes, 0 = Off
                            break;

                        case INITIALIZE_TIMER:
                            trigger.setUserData("timerId", file.readUnsignedByte()); // timerId + 1, 16 - Time Limit
                            file.readAndCheckNull(3); // file.skipBytes(3);
                            trigger.setUserData("value", file.readUnsignedInteger()); // limit 4 bytes, only for Time limit (max 100 s)
                            break;

                        case FLAG:
                            trigger.setUserData("flagId", file.readUnsignedByte()); // flagId + 1, 128 - level score
                            trigger.setUserData("flag", file.readUnsignedByte()); // flag = Equal = 12 | Plus = 20 | Minus = 36
                            file.readAndCheckNull(2); // file.skipBytes(2);
                            trigger.setUserData("value", file.readUnsignedInteger()); // limit 4 bytes
                            break;

                        case MAKE:
                            trigger.setUserData("playerId", file.readUnsignedByte());
                            trigger.setUserData("type", file.readUnsignedByte()); // type = TriggerAction.MakeType.
                            trigger.setUserData("targetId", file.readUnsignedByte());
                            trigger.setUserData("available", file.readUnsignedByte()); // 0 = Unavailable, !0 = Available
                            file.readAndCheckNull(4); // file.skipBytes(4);
                            break;
                        // in player triggers
                        case DISPLAY_SLAB_OWNER:
                            // FIXME Show wrong values in editor
                            trigger.setUserData("available", file.readUnsignedByte());  // 0 = Off, !0 = On
                            //((TriggerAction) trigger).setActionTargetValue1(ConversionUtils.toUnsignedInteger(file)); // limit 4 bytes
                            // 1635984
                            file.readAndCheckNull(7); // file.skipBytes(7);
                            break;

                        case DISPLAY_NEXT_ROOM_TYPE: // 0 = Off or roomId
                        case MAKE_OBJECTIVE: // 0 = Off, 1 = Kill, 2 = Imprison, 3 = Convert
                        case ZOOM_TO_ACTION_POINT: // actionPointId
                            trigger.setUserData("targetId", file.readUnsignedByte());
                            file.readAndCheckNull(7); // file.skipBytes(7);
                            break;

                        case DISPLAY_OBJECTIVE:
                            trigger.setUserData("objectiveId", file.readUnsignedInteger()); // objectiveId, limit 32767
                            trigger.setUserData("actionPointId", file.readUnsignedByte()); // if != 0 => Zoom To AP = this
                            file.readAndCheckNull(3); // file.skipBytes(3);
                            break;

                        case PLAY_SPEECH:
                            trigger.setUserData("speechId", file.readUnsignedInteger()); // speechId, limit 32767
                            trigger.setUserData("text", file.readUnsignedByte()); // 0 = Show Text, !0 = Without text
                            trigger.setUserData("introduction", file.readUnsignedByte()); // 0 = No Introduction, !0 = Introduction
                            trigger.setUserData("pathId", file.readUnsignedShort()); // pathId
                            break;

                        case DISPLAY_TEXT_STRING:
                            trigger.setUserData("textId", file.readUnsignedInteger()); // textId, limit 32767
                            // FIXME Maybe Zoom to AP X
                            file.readAndCheckNull(4); // file.skipBytes(4);
                            break;
                        // creature triggers
                        case ATTACH_PORTAL_GEM:
                        case MAKE_HUNGRY:
                        case REMOVE_FROM_MAP:
                        case ZOOM_TO:
                        case WIN_GAME:
                        case LOSE_GAME:
                        case FORCE_FIRST_PERSON:
                        case LOSE_SUBOBJECTIVE:
                        case WIN_SUBOBJECTIVE:
                            file.readAndCheckNull(8); // file.skipBytes(8); // no other parameters
                            break;

                        case SET_MUSIC_LEVEL: // level
                        case SHOW_HEALTH_FLOWER: // limit Seconds
                            trigger.setUserData("value", file.readUnsignedInteger());
                            file.readAndCheckNull(4); // file.skipBytes(4);
                            break;

                        case SET_TIME_LIMIT:
                            trigger.setUserData("timerId", file.readUnsignedByte()); // timerId + 1, 16 - Time Limit
                            file.readAndCheckNull(3); // file.skipBytes(3);
                            trigger.setUserData("value", file.readUnsignedInteger()); // Seconds
                            break;

                        case FOLLOW_CAMERA_PATH:
                            trigger.setUserData("pathId", file.readUnsignedByte());
                            trigger.setUserData("actionPointId", file.readUnsignedByte());
                            trigger.setUserData("available", file.readUnsignedByte()); // 0 = Show Ceiling, !0 = Hide Ceiling
                            file.readAndCheckNull(5); // file.skipBytes(5);
                            break;

                        case FLASH_BUTTON:
                            trigger.setUserData("type", file.readUnsignedByte()); // TriggerAction.MakeType.
                            trigger.setUserData("targetId", file.readUnsignedByte());
                            trigger.setUserData("available", file.readUnsignedByte()); // 0 = Off, !0 & !time = Until selected
                            file.readAndCheckNull(1); // file.skipBytes(1);
                            trigger.setUserData("value", file.readUnsignedInteger()); // Seconds
                            break;

                        case FLASH_ACTION_POINT:
                            trigger.setUserData("actionPointId", file.readUnsignedByte());
                            trigger.setUserData("available", file.readUnsignedByte()); // 0 = Off, !0 & !time = Until switched off
                            file.readAndCheckNull(2); // file.skipBytes(2);
                            trigger.setUserData("value", file.readUnsignedInteger()); // Seconds
                            break;

                        case REVEAL_ACTION_POINT:
                            trigger.setUserData("actionPointId", file.readUnsignedByte());
                            trigger.setUserData("available", file.readUnsignedByte()); // 0 = Reveal, !0 = Conceal
                            file.readAndCheckNull(6); // file.skipBytes(6);
                            break;

                        case ROTATE_AROUND_ACTION_POINT:
                            trigger.setUserData("actionPointId", file.readUnsignedByte());
                            trigger.setUserData("available", file.readUnsignedByte()); // 0 = Relative, !0 = Absolute
                            trigger.setUserData("angle", file.readUnsignedShort()); // degrees
                            trigger.setUserData("time", file.readUnsignedInteger()); // seconds
                            break;

                        case CREATE_CREATURE:
                            trigger.setUserData("creatureId", file.readUnsignedByte());
                            trigger.setUserData("playerId", file.readUnsignedByte());
                            trigger.setUserData("level", file.readUnsignedByte());
                            trigger.setUserData("flag", file.readUnsignedByte()); // TriggerAction.CreatureFlag.
                            trigger.setUserData("posX", file.readUnsignedShort());
                            trigger.setUserData("posY", file.readUnsignedShort());
                            break;

                        case SET_OBJECTIVE:
                            trigger.setUserData("playerId", file.readUnsignedByte());
                            trigger.setUserData("type", file.readUnsignedByte()); // Creature.JobType
                            file.readAndCheckNull(2); // file.skipBytes(2);
                            trigger.setUserData("actionPointId", file.readUnsignedInteger()); // for type = SEND_TO_ACTION_POINT
                            break;

                        case CREATE_HERO_PARTY:
                            trigger.setUserData("partyId", file.readUnsignedByte()); // partyId + 1
                            trigger.setUserData("type", file.readUnsignedByte()); // 0 = None, 1 = IP, 2 = IP Random
                            file.readAndCheckNull(2); // file.skipBytes(2);
                            trigger.setUserData("actionPointId", file.readUnsignedByte()); //
                            file.readAndCheckNull(3); // file.skipBytes(3);
                            break;

                        case TOGGLE_EFFECT_GENERATOR:
                            trigger.setUserData("generatorId", file.readUnsignedByte()); // generatorId + 1
                            trigger.setUserData("available", file.readUnsignedByte()); // 0 = Off, !0 = On
                            file.readAndCheckNull(6); // file.skipBytes(6);
                            break;

                        case GENERATE_CREATURE:
                            trigger.setUserData("creatureId", file.readUnsignedByte()); // creatureId + 1
                            trigger.setUserData("level", file.readUnsignedByte());
                            file.readAndCheckNull(6); // file.skipBytes(6);
                            break;

                        case INFORMATION:
                            trigger.setUserData("informationId", file.readUnsignedInteger());
                            trigger.setUserData("actionPointId", file.readUnsignedByte());
                            file.readAndCheckNull(3); // file.skipBytes(3);
                            break;

                        case SEND_TO_AP:
                            file.readAndCheckNull(4); // file.skipBytes(4);
                            trigger.setUserData("actionPointId", file.readUnsignedByte());
                            file.readAndCheckNull(3); // file.skipBytes(3);
                            break;

                        case CREATE_PORTAL_GEM:
                            trigger.setUserData("objectId", file.readUnsignedByte());
                            trigger.setUserData("playerId", file.readUnsignedByte());
                            file.readAndCheckNull(2); // file.skipBytes(2);
                            trigger.setUserData("posX", file.readUnsignedShort()); // posX + 1
                            trigger.setUserData("posY", file.readUnsignedShort()); // posY + 1
                            break;

                        default:
                            file.readAndCheckNull(8); // file.skipBytes(8);
                            LOGGER.warning("Unsupported Type of TriggerAction");
                            break;
                    }

                    trigger.setId(file.readUnsignedShort()); // ID
                    trigger.setIdNext(file.readUnsignedShort()); // SiblingID
                    trigger.setIdChild(file.readUnsignedShort()); // ChildID

                    file.skipBytes(2);
                    break;
                }
                default: {

                    // Just skip the bytes
                    file.skipBytes(triggerTag[1]);
                    LOGGER.log(Level.WARNING, "Unsupported trigger type {0}!", triggerTag[0]);
                }
            }

            // Add to the list
            if (trigger != null) {
                triggers.put(trigger.getId(), trigger);
            }

            // Check file offset
            file.checkOffset(triggerTag[1], offset);
        }
    }

    /**
     * Reads the *Variables.kld
     *
     * @param header Kwd header data
     * @param file the file data, rewind to data position
     * @throws IOException the reading may fail
     */
    private void readVariables(KwdHeader header, IResourceReader file) throws IOException {

        // Read the requested VARIABLES file
        // Should be the GlobalVariables first, then the level's own
        if (variables == null) {
            LOGGER.info("Reading variables!");
            availabilities = new ArrayList<>();
            creaturePools = new HashMap<>(4);
            creatureStatistics = new HashMap<>(10);
            creatureFirstPersonStatistics = new HashMap<>(10);
            variables = new HashMap<>();
            sacrifices = new HashSet<>();
            unknownVariables = new HashSet<>();
        } else {
            LOGGER.info("Overrides variables!");
        }

        for (int i = 0; i < header.getItemCount(); i++) {
            int id = file.readInteger();

            switch (id) {
                case Variable.CREATURE_POOL:
                    Variable.CreaturePool creaturePool = new Variable.CreaturePool();
                    creaturePool.setCreatureId(file.readInteger());
                    creaturePool.setValue(file.readInteger());
                    creaturePool.setPlayerId(file.readInteger());

                    // Add
                    Map<Integer, CreaturePool> playerCreaturePool = creaturePools.get(creaturePool.getPlayerId());
                    if (playerCreaturePool == null) {
                        playerCreaturePool = new HashMap<>(12);
                        creaturePools.put(creaturePool.getPlayerId(), playerCreaturePool);
                    }
                    playerCreaturePool.put(creaturePool.getCreatureId(), creaturePool);
                    break;

                case Variable.AVAILABILITY:
                    Variable.Availability availability = new Variable.Availability();
                    availability.setType(file.readShortAsEnum(Variable.Availability.AvailabilityType.class));
                    availability.setPlayerId(file.readUnsignedShort());
                    availability.setTypeId(file.readInteger());
                    availability.setValue(file.readIntegerAsEnum(Variable.Availability.AvailabilityValue.class));

                    // Add
                    availabilities.add(availability);
                    break;

                case Variable.SACRIFICES_ID: // not changeable (in editor you can, but changes will not save)
                    Variable.Sacrifice sacrifice = new Variable.Sacrifice();
                    sacrifice.setType1(file.readByteAsEnum(Variable.SacrificeType.class));
                    sacrifice.setId1(file.readUnsignedByte());
                    sacrifice.setType2(file.readByteAsEnum(Variable.SacrificeType.class));
                    sacrifice.setId2(file.readUnsignedByte());
                    sacrifice.setType3(file.readByteAsEnum(Variable.SacrificeType.class));
                    sacrifice.setId3(file.readUnsignedByte());

                    sacrifice.setRewardType(file.readByteAsEnum(Variable.SacrificeRewardType.class));
                    sacrifice.setSpeechId(file.readUnsignedByte());
                    sacrifice.setRewardValue(file.readInteger());

                    // Add
                    sacrifices.add(sacrifice);
                    break;

                case Variable.CREATURE_STATS_ID:
                    Variable.CreatureStats creatureStats = new Variable.CreatureStats();
                    creatureStats.setStatId(file.readIntegerAsEnum(Variable.CreatureStats.StatType.class));
                    creatureStats.setValue(file.readInteger());
                    creatureStats.setLevel(file.readInteger());

                    // Add
                    Map<StatType, CreatureStats> stats = creatureStatistics.get(creatureStats.getLevel());
                    if (stats == null) {
                        stats = new HashMap<>(CreatureStats.StatType.values().length);
                        creatureStatistics.put(creatureStats.getLevel(), stats);
                    }
                    stats.put(creatureStats.getStatId(), creatureStats);
                    break;

                case Variable.CREATURE_FIRST_PERSON_ID:
                    Variable.CreatureFirstPerson creatureFirstPerson = new Variable.CreatureFirstPerson();
                    creatureFirstPerson.setStatId(file.readIntegerAsEnum(Variable.CreatureStats.StatType.class));
                    creatureFirstPerson.setValue(file.readInteger());
                    creatureFirstPerson.setLevel(file.readInteger());

                    // Add
                    Map<StatType, CreatureFirstPerson> firstPersonStats = creatureFirstPersonStatistics.get(creatureFirstPerson.getLevel());
                    if (firstPersonStats == null) {
                        firstPersonStats = new HashMap<>(CreatureStats.StatType.values().length);
                        creatureFirstPersonStatistics.put(creatureFirstPerson.getLevel(), firstPersonStats);
                    }
                    firstPersonStats.put(creatureFirstPerson.getStatId(), creatureFirstPerson);
                    break;

                case Variable.UNKNOWN_17: // FIXME unknown value
                case Variable.UNKNOWN_66: // FIXME unknown value
                case Variable.UNKNOWN_0: // FIXME unknownn value
                case Variable.UNKNOWN_77: // FIXME unknownn value
                    Variable.Unknown unknown = new Variable.Unknown();
                    unknown.setVariableId(id);
                    unknown.setValue(file.readInteger());
                    unknown.setUnknown1(file.readInteger());
                    unknown.setUnknown2(file.readInteger());

                    // Add
                    unknownVariables.add(unknown);
                    break;

                default:
                    Variable.MiscVariable miscVariable = new Variable.MiscVariable();
                    miscVariable.setVariableId(ConversionUtils.parseEnum(id,
                            Variable.MiscVariable.MiscType.class));
                    miscVariable.setValue(file.readInteger());
                    miscVariable.setUnknown1(file.readInteger());
                    miscVariable.setUnknown2(file.readInteger());

                    // Add
                    variables.put(miscVariable.getVariableId(), miscVariable);
                    break;
            }
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
    public Collection<GameObject> getObjectList() {
        return objects.values();
    }

    /**
     * Get list of different creatures
     *
     * @return list of creatures
     */
    public Collection<Creature> getCreatureList() {
        return creatures.values();
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

    public Map<Short, Player> getPlayers() {
        return players;
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
     * Bridges are a bit special, identifies one and returns the terrain that
     * should be under it
     *
     * @param type tile BridgeTerrainType
     * @param terrain the terrain tile
     * @return returns null if this is not a bridge, otherwise returns pretty
     * much either water or lava
     */
    public Terrain getTerrainBridge(Tile.BridgeTerrainType type, Terrain terrain) {
        if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
            Room room = getRoomByTerrain(terrain.getTerrainId());
            return getTerrainBridge(type, room);
        }

        return null;
    }

    public Terrain getTerrainBridge(Tile.BridgeTerrainType type, Room room) {
        // Swap the terrain if this is a bridge
        if (room != null && !room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAND)) {
            // It is a bridge
            switch (type) {
                case WATER:
                    return getMap().getWater();
                case LAVA:
                    return getMap().getLava();
            }
        }

        return null;
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
     * Get list of things by certain type
     *
     * @param <T> the instance type of the things you want
     * @param thingClass the class of things you want
     * @return things list of things you want
     */
    public <T extends Thing> List<T> getThings(Class<T> thingClass) {
        List<T> result = (List<T>) thingsByType.get(thingClass);
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
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

    public Map<Integer, Trigger> getTriggers() {
        return triggers;
    }

    /**
     * Get the object with the specified ID
     *
     * @param id the id of object
     * @return the object
     */
    public GameObject getObject(int id) {
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
     * Get the list of all doors
     *
     * @return list of all doors
     */
    public List<Door> getDoors() {
        List<Door> c = new ArrayList(doors.values());
        Collections.sort(c);
        return c;
    }

    /**
     * Get the list of all shots
     *
     * @return list of all shots
     */
    public List<Shot> getShots() {
        List<Shot> c = new ArrayList(shots.values());
        Collections.sort(c);
        return c;
    }

    public GameMap getMap() {
        return map;
    }

    /**
     * Get the list of all traps
     *
     * @return list of all traps
     */
    public List<Trap> getTraps() {
        List<Trap> c = new ArrayList(traps.values());
        Collections.sort(c);
        return c;
    }

    public GameLevel getGameLevel() {
        return gameLevel;
    }

    public CreatureSpell getCreatureSpellById(short spellId) {
        return creatureSpells.get(spellId);
    }

    public Effect getEffect(int effectId) {
        return effects.get(effectId);
    }

    public Map<Integer, Effect> getEffects() {
        return effects;
    }

    public EffectElement getEffectElement(int effectElementId) {
        return effectElements.get(effectElementId);
    }

    public Map<Integer, EffectElement> getEffectElements() {
        return effectElements;
    }

    public Map<MiscVariable.MiscType, MiscVariable> getVariables() {
        return variables;
    }

    public List<Availability> getAvailabilities() {
        return availabilities;
    }

    /**
     * Get player specific creature pool
     *
     * @param playerId the player id
     * @return the creature pool
     */
    public Map<Integer, CreaturePool> getCreaturePool(short playerId) {
        return creaturePools.get(Short.valueOf(playerId).intValue());
    }

    public Creature getImp() {
        return imp;
    }

    public Creature getDwarf() {
        return dwarf;
    }

    public Room getPortal() {
        return getRoomById(ROOM_PORTAL_ID);
    }

    public GameObject getLevelGem() {
        return levelGem;
    }

    /**
     * Get the creature stats by level. There might not be a record for every
     * level. Then should just default to 100% stat.
     *
     * @param level the creature level
     * @return the creature stats on given level
     */
    public Map<CreatureStats.StatType, CreatureStats> getCreatureStats(int level) {
        return creatureStatistics.get(level);
    }

    /**
     * Not all the data types are of the length that suits us, do our best to
     * ignore it<br>
     * Skips the file to the correct position after an item is read<br>
     * <b>Use this with the common types!</b>
     *
     * @see toniarts.openkeeper.tools.convert.ResourceReader#checkOffset(long, long)
     * @param header the header
     * @param file the file
     * @param offset the file offset before the last item was read
     */
    private void checkOffset(KwdHeader header, IResourceReader file, long offset) throws IOException {
        file.checkOffset(header.getItemSize(), offset);
    }

    /**
     * Kwd header, few different kinds, handles all
     */
    private class KwdHeader {
        // struct kwdHeader {
        //     unsigned int id;
        //     unsigned int size;
        //     union {
        //         struct {
        //             uint16_t w08; <- width
        //             uint16_t w0a; <- height
        //         } level;
        //         unsigned int dw08; <- version?
        //     };
        //     unsigned int x0c[7];
        // };

        private MapDataTypeEnum id;
        private int headerSize = 56; // Well, header and the id data
        private long size;
        private int checkOne;
        private int itemCount;
        private int width;
        private int height;
        private int checkThree; // in level = 223
        private int dataSizeLevel; // in Level size of data exclude paths
        private int unknown; // only in Triggers and Level
        private int headerEndOffset; // 28, *Map - 8, *Triggers - 32,
        private Date dateCreated;
        private Date dateModified;
        private int checkTwo;
        private int dataSize;

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

        public int getUnknown() {
            return unknown;
        }

        protected void setUnknown(int unknown) {
            this.unknown = unknown;
        }

        protected void setCheckOne(int check) {
            this.checkOne = check;
        }

        public int getCheckOne() {
            return checkOne;
        }

        protected void setCheckTwo(int check) {
            this.checkTwo = check;
        }

        public int getCheckTwo() {
            return checkTwo;
        }

        public Date getDateCreated() {
            return dateCreated;
        }

        protected void setDateCreated(Date date) {
            this.dateCreated = date;
        }

        public Date getDateModified() {
            return dateModified;
        }

        protected void setDateModified(Date date) {
            this.dateModified = date;
        }

        protected void setHeaderEndOffset(int offset) {
            this.headerEndOffset = offset;
        }

        public int getHeaderEndOffset() {
            return headerEndOffset;
        }

        protected void setDataSize(int size) {
            this.dataSize = size;
        }

        public int getDataSize(int unknown) {
            return dataSize;
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
