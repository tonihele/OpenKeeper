/*
 * Copyright (C) 2014-2019 OpenKeeper
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.AI.AIType;

/**
 * The nine computer player types of skirmish and multiplayer games. DK II
 * doesn't take these from the level file but has them built in, so they are
 * here. Every array has a value for each type, in the order of
 * {@link AIType}.
 */
public final class AiPresets {

    private static final short ROOM_PORTAL = 3;
    private static final short ROOM_HERO_PORTAL = 25;
    private static final int BUILD_ORDER_SIZE = 15;

    // Rooms
    private static final int[] MIN_TIME_BEFORE_PLACING_RESEARCHED_ROOM = {0, 15, 30, 0, 15, 30, 0, 15, 30};
    private static final int[] BUILD_BIGGER_LAIR_AFTER_CLAIMING_PORTAL = {1, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final int[] DEFAULT_SIZE = {25, 16, 9, 25, 16, 12, 25, 16, 12};
    private static final int[] TILES_LEFT_BETWEEN_ROOMS = {2, 1, 0, 1, 1, 1, 1, 1, 1};
    private static final int[] DISTANCE_BETWEEN_ROOMS_THAT_SHOULD_BE_CLOSE = {0, 0, 1, 0, 0, 1, 0, 1, 1};
    private static final int[] CORRIDOR_STYLE = {1, 0, 0, 1, 0, 2, 2, 2, 0};
    private static final int[] WHEN_MORE_SPACE_IN_ROOM_REQUIRED = {0, 0, 1, 0, 0, 1, 0, 0, 1};
    private static final int[] DIG_TO_NEUTRAL_ROOMS_WITHIN_TILES_OF_HEART = {25, 20, 40, 25, 20, 15, 15, 14, 13};
    private static final int[] DIG_TO_NEUTRAL_ROOMS_WITHIN_TILES_OF_CLAIMED_AREA = {4, 3, 5, 3, 3, 3, 2, 2, 1};

    // Doors and traps
    private static final int TRAP_USE_STYLE = 2;
    private static final int DOOR_TRAP_PREFERENCE = 2;
    private static final int[] DOOR_USAGE = {0, 0, 1, 0, 0, 1, 0, 2, 1};
    private static final int[] BOULDER_TRAPS_ON_LONG_CORRIDORS = {1, 1, 0, 1, 1, 0, 1, 1, 0};
    private static final int[] BOULDER_TRAPS_ON_ROUTE_TO_BREACH_POINTS = {1, 1, 0, 1, 0, 0, 1, 1, 0};
    private static final int[] CHANCE_OF_LOOKING_TO_USE_TRAPS_AND_DOORS = {99, 70, 30, 99, 50, 25, 99, 50, 25};

    // Initial attack
    private static final int[] ATTACK_WHEN_NUMBER_OF_CREATURES_IS_AT_LEAST = {15, 10, 5, 15, 15, 15, 4, 4, 4};
    private static final int[] MIN_LEVEL_FOR_CREATURES = {8, 5, 0, 8, 5, 2, 0, 0, 0};
    private static final int[] REQUIRE_MIN_LEVEL_AND_THREAT = {1, 1, 0, 1, 1, 1, 1, 1, 1};
    private static final int[] TOTAL_THREAT_GREATER_THAN_THE_ENEMY = {20, 125, 0, 100, 100, 100, 200, 200, 200};
    private static final int[] FIRST_ATTEMPT_TO_BREACH_ROOM = {2, 0, 3, 2, 1, 3, 2, 1, 3};
    private static final int[] FIRST_DIG_TO_ENEMY_POINT = {3, 3, 2, 0, 1, 2, 0, 3, 2};
    private static final int[] BREACH_AT_POINTS_SIMULTANEOUSLY = {2, 1, 1, 2, 1, 1, 1, 1, 1};
    private static final int[] USE_PERCENTAGE_OF_TOTAL_CREATURES_IN_FIRST_FIGHT = {50, 75, 100, 50, 40, 30, 40, 20, 10};
    private static final int[] ONLY_ATTACK_ATTACKERS = {0, 0, 0, 0, 0, 1, 1, 1, 1};
    private static final int[] NEVER_ATTACK = {0, 0, 0, 0, 0, 0, 0, 1, 1};

    // Battles
    private static final int[] THREAT_VALUE_OF_DROPPED_CREATURES = {125, 100, 50, 125, 110, 90, 150, 150, 150};
    private static final int[] SPELL_STYLE = {2, 2, 0, 2, 2, 2, 2, 2, 2};
    private static final int[] PLACE_CALL_TO_ARMS_WHERE_THREAT_VALUE_IS_GREATER_THAN = {2000, 1500, 1000, 2150, 2000, 2500, 2000, 2500, 3500};
    private static final int[] ATTEMPT_TO_IMPRISON_PERCENTAGE = {5, 5, 0, 15, 8, 25, 15, 20, 25};
    private static final int[] PULL_CREATURES_FROM_FIGHT_IF_OUTNUMBERED = {1, 1, 0, 1, 1, 0, 1, 1, 1};
    private static final int[] RETREAT_HEALTH_PERCENTAGE = {10, 10, 10, 20, 20, 20, 30, 30, 30};
    private static final int REMOVE_CALL_TO_ARMS_AFTER = 240;
    private static final int[] REMOVE_CALL_TO_ARMS_IF_LESS_THAN_ENEMY_CREATURES = {1, 1, 1, 2, 2, 2, 5, 5, 5};
    private static final int REMOVE_CALL_TO_ARMS_WITHIN_TILES = 5;
    private static final int[] REMOVE_CALL_TO_ARMS_IF_TOTAL_CREATURES_LESS_THAN = {10, 5, 1, 10, 10, 10, 4, 4, 4};
    private static final int WAIT_AFTER_PREVIOUS_ATTACK = 90;

    // Creatures
    private static final int[] TRY_TO_MAKE_UNHAPPY_ONES_HAPPY = {1, 0, 0, 1, 0, 0, 1, 0, 0};
    private static final int[] TRY_TO_MAKE_ANGRY_ONES_HAPPY = {1, 1, 0, 1, 1, 0, 1, 1, 0};
    private static final int[] DISPOSE_OF_ANGRY_CREATURES = {0, 0, 1, 0, 0, 1, 0, 0, 1};
    private static final int[] DISPOSAL_METHOD = {1, 1, 0, 1, 1, 0, 1, 1, 0};
    private static final int[] DISPOSE_OF_RUBBISH_CREATURES = {1, 0, 0, 1, 0, 0, 1, 0, 0};
    private static final int[] MAXIMUM_NUMBER_OF_IMPS = {20, 15, 10, 15, 10, 5, 12, 8, 4};

    // Spells
    private static final int[] USE_SIGHT_OF_EVIL = {2, 1, 2, 2, 1, 2, 2, 1, 1};
    private static final int USE_SPELLS_IN_BATTLE = 2;
    private static final int[] USE_LIGHTNING_IF_ENEMY_IS_IN_WATER = {1, 0, 0, 1, 0, 0, 1, 0, 0};
    private static final int SPELLS_POWER_PREFERENCE = 2;
    private static final int[] USE_CALL_TO_ARMS = {1, 1, 2, 0, 0, 2, 0, 1, 2};

    // Misc
    private static final int[] MOVING_CREATURES_TO_LIBRARY = {0, 1, 2, 0, 1, 2, 0, 1, 2};
    private static final int[] MINE_GOLD_UNTIL_GOLD_HELD_IS_GREATER_THAN = {9000, 6000, 6000, 9000, 6000, 6000, 9000, 6000, 6000};
    private static final int[] EXPLORE_UP_TO_TILES_TO_FIND_SPECIALS = {10, 8, 15, 8, 5, 2, 5, 4, 3};
    private static final int[] CHANCE_OF_FINDING_SPECIALS = {90, 50, 20, 90, 50, 20, 90, 50, 20};
    private static final int[] IMPS_TO_TILES_RATIO = {15, 25, 35, 15, 20, 25, 15, 15, 15};
    private static final int[] FATE_OF_IMPRISONED_CREATURES = {0, 2, 1, 0, 2, 1, 0, 3, 1};
    private static final int STARTING_MANA = 100;
    private static final int SPEED = 1;
    private static final int[] OPENNESS = {50, 50, 50, 20, 20, 20, 15, 15, 15};
    private static final int MANA_AND_GOLD_VALUE = 100;

    /**
     * The maps on which DK II swaps some computer player types for another
     */
    private record MapOverride(AIType becomes, Set<AIType> types) {

    }

    private static final Map<String, MapOverride> MAP_OVERRIDES = Map.ofEntries(
            Map.entry("Spiral", new MapOverride(AIType.CONQUEROR, Set.of(AIType.MASTER_KEEPER, AIType.STALWART, AIType.GREYMAN))),
            Map.entry("Pass", new MapOverride(AIType.CONQUEROR, Set.of(AIType.MASTER_KEEPER, AIType.STALWART, AIType.GREYMAN))),
            Map.entry("Caverns", new MapOverride(AIType.PSYCHOTIC, Set.of(AIType.MASTER_KEEPER, AIType.CONQUEROR, AIType.STALWART,
                    AIType.GREYMAN, AIType.GUARDIAN, AIType.THICK_SKINNED))),
            Map.entry("Clover", new MapOverride(AIType.PSYCHOTIC, Set.of(AIType.MASTER_KEEPER, AIType.CONQUEROR, AIType.STALWART,
                    AIType.GUARDIAN, AIType.THICK_SKINNED))),
            Map.entry("Olympia", new MapOverride(AIType.MASTER_KEEPER, Set.of(AIType.PSYCHOTIC))),
            Map.entry("OnSlaught", new MapOverride(AIType.MASTER_KEEPER, Set.of(AIType.PSYCHOTIC, AIType.STALWART, AIType.GREYMAN,
                    AIType.IDIOT, AIType.GUARDIAN, AIType.THICK_SKINNED, AIType.PARANOID))),
            Map.entry("Siege", new MapOverride(AIType.MASTER_KEEPER, Set.of(AIType.IDIOT, AIType.THICK_SKINNED, AIType.PARANOID))),
            Map.entry("Frosty's Castle", new MapOverride(AIType.MASTER_KEEPER, Set.of(AIType.GREYMAN, AIType.GUARDIAN, AIType.PARANOID))),
            Map.entry("Frosty's Lair", new MapOverride(AIType.MASTER_KEEPER, Set.of(AIType.GREYMAN, AIType.IDIOT))),
            Map.entry("Frosty's Realm", new MapOverride(AIType.MASTER_KEEPER, Set.of(AIType.GUARDIAN))),
            Map.entry("Furnace", new MapOverride(AIType.MASTER_KEEPER, Set.of(AIType.GUARDIAN))),
            Map.entry("No Mans Land", new MapOverride(AIType.MASTER_KEEPER, Set.of(AIType.GREYMAN, AIType.GUARDIAN))));

    private AiPresets() {
    }

    /**
     * On some maps DK II plays another computer player type than the one
     * picked. The map name has to match to the letter, so the entry for
     * "OnSlaught" never applies to the map "Onslaught", as in DK II.
     *
     * @param type the type picked for the computer player
     * @param mapName the name of the map, without the file extension
     * @return the type that plays on the map
     */
    public static AIType getTypeOnMap(AIType type, String mapName) {
        MapOverride mapOverride = mapName != null ? MAP_OVERRIDES.get(mapName) : null;

        return mapOverride != null && mapOverride.types.contains(type) ? mapOverride.becomes : type;
    }

    /**
     * The settings of a computer player type. It may build on the whole map.
     *
     * @param type the type
     * @param rooms all the rooms of the game, the build order is the first
     * 15 of them by id that can be built or are portals
     * @param maxCreatureLevel the highest level the creatures are asked to
     * have before an attack, the level the Combat Pit trains to
     * @param mapWidth the width of the map in tiles
     * @param mapHeight the height of the map in tiles
     * @return the settings
     */
    public static AI create(AIType type, Collection<Room> rooms, int maxCreatureLevel, int mapWidth, int mapHeight) {
        int i = type.ordinal();
        AI ai = new AI();
        ai.setAiType(type);

        ai.setMinTimeBeforePlacingResearchedRoom((short) MIN_TIME_BEFORE_PLACING_RESEARCHED_ROOM[i]);
        ai.setBuildBiggerLairAfterClaimingPortal(BUILD_BIGGER_LAIR_AFTER_CLAIMING_PORTAL[i] == 1);
        ai.setDefaultSize((short) DEFAULT_SIZE[i]);
        ai.setTilesLeftBetweenRooms((short) TILES_LEFT_BETWEEN_ROOMS[i]);
        ai.setDistanceBetweenRoomsThatShouldBeCloseMan(ConversionUtils.parseEnum(DISTANCE_BETWEEN_ROOMS_THAT_SHOULD_BE_CLOSE[i], AI.Distance.class));
        ai.setCorridorStyle(ConversionUtils.parseEnum(CORRIDOR_STYLE[i], AI.CorridorStyle.class));
        ai.setWhenMoreSpaceInRoomRequired(ConversionUtils.parseEnum(WHEN_MORE_SPACE_IN_ROOM_REQUIRED[i], AI.RoomExpandPolicy.class));
        ai.setDigToNeutralRoomsWithinTilesOfHeart((short) DIG_TO_NEUTRAL_ROOMS_WITHIN_TILES_OF_HEART[i]);
        ai.setDigToNeutralRoomsWithinTilesOfClaimedArea((short) DIG_TO_NEUTRAL_ROOMS_WITHIN_TILES_OF_CLAIMED_AREA[i]);
        List<Short> buildOrder = new ArrayList<>();
        List<Room> roomsById = new ArrayList<>(rooms);
        roomsById.sort(Comparator.comparingInt(Room::getRoomId));
        for (Room room : roomsById) {
            if (buildOrder.size() < BUILD_ORDER_SIZE && (room.getFlags().contains(Room.RoomFlag.BUILDABLE)
                    || room.getRoomId() == ROOM_PORTAL || room.getRoomId() == ROOM_HERO_PORTAL)) {
                buildOrder.add(room.getRoomId());
            }
        }
        ai.setBuildOrder(buildOrder);

        ai.setTrapUseStyle((short) TRAP_USE_STYLE);
        ai.setDoorTrapPreference((short) DOOR_TRAP_PREFERENCE);
        ai.setDoorUsage(ConversionUtils.parseEnum(DOOR_USAGE[i], AI.DoorUsagePolicy.class));
        ai.setBoulderTrapsOnLongCorridors(BOULDER_TRAPS_ON_LONG_CORRIDORS[i] == 1);
        ai.setBoulderTrapsOnRouteToBreachPoints(BOULDER_TRAPS_ON_ROUTE_TO_BREACH_POINTS[i] == 1);
        ai.setChanceOfLookingToUseTrapsAndDoors((short) CHANCE_OF_LOOKING_TO_USE_TRAPS_AND_DOORS[i]);

        ai.setAttackWhenNumberOfCreaturesIsAtLeast((short) ATTACK_WHEN_NUMBER_OF_CREATURES_IS_AT_LEAST[i]);
        ai.setMinLevelForCreatures((short) Math.min(MIN_LEVEL_FOR_CREATURES[i], maxCreatureLevel));
        ai.setRequireMinLevelForCreatures(REQUIRE_MIN_LEVEL_AND_THREAT[i] == 1);
        ai.setRequireTotalThreatGreaterThanTheEnemy(REQUIRE_MIN_LEVEL_AND_THREAT[i] == 1);
        ai.setTotalThreatGreaterThanTheEnemy((short) TOTAL_THREAT_GREATER_THAN_THE_ENEMY[i]);
        ai.setFirstAttemptToBreachRoom(ConversionUtils.parseEnum(FIRST_ATTEMPT_TO_BREACH_ROOM[i], AI.BreachRoomPolicy.class));
        ai.setFirstDigToEnemyPoint(ConversionUtils.parseEnum(FIRST_DIG_TO_ENEMY_POINT[i], AI.DigToPolicy.class));
        ai.setBreachAtPointsSimultaneously((short) BREACH_AT_POINTS_SIMULTANEOUSLY[i]);
        ai.setUsePercentageOfTotalCreaturesInFirstFightAfterBreach((short) USE_PERCENTAGE_OF_TOTAL_CREATURES_IN_FIRST_FIGHT[i]);
        ai.setOnlyAttackAttackers(ONLY_ATTACK_ATTACKERS[i] == 1);
        ai.setNeverAttack(NEVER_ATTACK[i] == 1);

        ai.setThreatValueOfDroppedCreaturesIsPercentageOfEnemyThreatValue((short) THREAT_VALUE_OF_DROPPED_CREATURES[i]);
        ai.setSpellStyle((short) SPELL_STYLE[i]);
        ai.setPlaceCallToArmsWhereThreatValueIsGreaterThan(PLACE_CALL_TO_ARMS_WHERE_THREAT_VALUE_IS_GREATER_THAN[i]);
        ai.setAttemptToImprisonPercentageOfEnemyCreatures((short) ATTEMPT_TO_IMPRISON_PERCENTAGE[i]);
        ai.setPullCreaturesFromFightIfOutnumberedAndUnableToDropReinforcements(PULL_CREATURES_FROM_FIGHT_IF_OUTNUMBERED[i] == 1);
        ai.setIfCreatureHealthIsPercentageAndNotInOwnRoomMoveToLairOrTemple((short) RETREAT_HEALTH_PERCENTAGE[i]);
        ai.setRemoveCallToArmsAfterSeconds(REMOVE_CALL_TO_ARMS_AFTER);
        ai.setRemoveCallToArmsIfLessThanEnemyCreatures((short) REMOVE_CALL_TO_ARMS_IF_LESS_THAN_ENEMY_CREATURES[i]);
        ai.setRemoveCallToArmsIfLessThanEnemyCreaturesWithinTiles((short) REMOVE_CALL_TO_ARMS_WITHIN_TILES);
        ai.setRemoveCallToArmsIfTotalCreaturesLessThan((short) REMOVE_CALL_TO_ARMS_IF_TOTAL_CREATURES_LESS_THAN[i]);
        ai.setWaitSecondsAfterPreviousAttackBeforeAttackingAgain(WAIT_AFTER_PREVIOUS_ATTACK);

        ai.setTryToMakeUnhappyOnesHappy(TRY_TO_MAKE_UNHAPPY_ONES_HAPPY[i] == 1);
        ai.setTryToMakeAngryOnesHappy(TRY_TO_MAKE_ANGRY_ONES_HAPPY[i] == 1);
        ai.setDisposeOfAngryCreatures(DISPOSE_OF_ANGRY_CREATURES[i] == 1);
        ai.setDisposalMethod(ConversionUtils.parseEnum(DISPOSAL_METHOD[i], AI.CreatureDisposalPolicy.class));
        ai.setDisposeOfRubbishCreaturesIfBetterOnesComeAlong(DISPOSE_OF_RUBBISH_CREATURES[i] == 1);
        ai.setMaximumNumberOfImps((short) MAXIMUM_NUMBER_OF_IMPS[i]);

        ai.setUseSightOfEvil(ConversionUtils.parseEnum(USE_SIGHT_OF_EVIL[i], AI.SightOfEvilUsagePolicy.class));
        ai.setUseSpellsInBattle((short) USE_SPELLS_IN_BATTLE);
        ai.setUseLightningIfEnemyIsInWater(USE_LIGHTNING_IF_ENEMY_IS_IN_WATER[i] == 1);
        ai.setSpellsPowerPreference((short) SPELLS_POWER_PREFERENCE);
        ai.setUseCallToArms(ConversionUtils.parseEnum(USE_CALL_TO_ARMS[i], AI.CallToArmsUsagePolicy.class));

        ai.setLikelyhoodToMovingCreaturesToLibraryForResearching(ConversionUtils.parseEnum(MOVING_CREATURES_TO_LIBRARY[i], AI.MoveToResearchPolicy.class));
        ai.setMineGoldUntilGoldHeldIsGreaterThan(MINE_GOLD_UNTIL_GOLD_HELD_IS_GREATER_THAN[i]);
        ai.setExploreUpToTilesToFindSpecials(EXPLORE_UP_TO_TILES_TO_FIND_SPECIALS[i]);
        ai.setChanceOfFindingSpecialsWhenExploring((short) CHANCE_OF_FINDING_SPECIALS[i]);
        ai.setImpsToTilesRatio(IMPS_TO_TILES_RATIO[i]);
        ai.setFateOfImprisonedCreatures(ConversionUtils.parseEnum(FATE_OF_IMPRISONED_CREATURES[i], AI.ImprisonedCreatureFatePolicy.class));
        ai.setStartingMana(STARTING_MANA);
        ai.setSpeed((short) SPEED);
        ai.setOpenness((short) OPENNESS[i]);
        ai.setManaValue(MANA_AND_GOLD_VALUE);
        ai.setGoldValue(MANA_AND_GOLD_VALUE);
        ai.setBuildAreaEndX(mapWidth - 1);
        ai.setBuildAreaEndY(mapHeight - 1);

        return ai;
    }
}
