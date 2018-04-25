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

import java.util.List;
import toniarts.openkeeper.tools.convert.IValueEnum;

/**
 *
 * @author ArchDemon
 */
public class AI {

    public enum AIType implements IValueEnum {

        MASTER_KEEPER(0, "1604"),
        CONQUEROR(1, "1605"),
        PSYCHOTIC(2, "1606"),
        STALWART(3, "1607"),
        GREYMAN(4, "1608"),
        IDIOT(5, "1609"),
        GUARDIAN(6, "1610"),
        THICK_SKINNED(7, "1611"),
        PARANOID(8, "1612");

        private AIType(int id, String translationKey) {
            this.id = id;
            this.translationKey = translationKey;
        }

        @Override
        public int getValue() {
            return id;
        }

        public String getTranslationKey() {
            return translationKey;
        }

        @Override
        public String toString() {
            String[] names = name().toLowerCase().split("_");
            StringBuilder sb = new StringBuilder(name().length());
            for (String item : names) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(Character.toUpperCase(item.charAt(0)));
                sb.append(item.substring(1));
            }
            return sb.toString();
        }

        private final int id;
        private final String translationKey;
    }

    public enum Distance implements IValueEnum {

        CLOSE(0),
        RANDOM(1);

        private Distance(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    public enum CorridorStyle implements IValueEnum {

        ANY_ANGLE(0),
        DEGREES_45_AND_90(1),
        DEGREES_90(2);

        private CorridorStyle(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    public enum RoomExpandPolicy implements IValueEnum {

        EXPAND_EXISTING(0),
        CREATE_NEW(1),
        IGNORE(2);

        private RoomExpandPolicy(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    public enum DoorUsagePolicy implements IValueEnum {

        BEST_DOOR(0),
        WORST_DOOR(1),
        RANDOM(2);

        private DoorUsagePolicy(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    public enum BreachRoomPolicy implements IValueEnum {

        ENEMY_DUNGEON_HEART(0),
        RANDOM(1),
        WEAK_ROOM(2),
        STRONG_ROOM(3);

        private BreachRoomPolicy(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    public enum DigToPolicy implements IValueEnum {

        FURTHEST_FROM_HEART(0),
        RANDOM(1),
        HEART(2),
        CLOSEST_TO_ENEMY_HEART(3),
        UNKNOWN_255(255); // FIXME Unknown value

        private DigToPolicy(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    public enum CreatureDisposalPolicy implements IValueEnum {

        SACK(0),
        SACRIFICE(1);

        private CreatureDisposalPolicy(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    public enum SightOfEvilUsagePolicy implements IValueEnum {

        NONE(0),
        RANDOM(1),
        ENEMY_LAND(2);

        private SightOfEvilUsagePolicy(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    public enum CallToArmsUsagePolicy implements IValueEnum {

        TOWARDS_HEART(0),
        AT_HEART_WITH_CREATURES(1),
        AT_HEART(2);

        private CallToArmsUsagePolicy(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    public enum MoveToResearchPolicy implements IValueEnum {

        VERY_LIKELY(0),
        PROBABLE(1),
        UNLIKELY(2);

        private MoveToResearchPolicy(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    public enum ImprisonedCreatureFatePolicy implements IValueEnum {

        TORTURE(0),
        LEAVE_TO_DIE(1),
        PLACE_IN_PIT(2),
        SACRIFICE(3);

        private ImprisonedCreatureFatePolicy(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }

    // All AI related parameters
    private AIType aiType; // x08, byte
    private short speed; // Misc, byte
    private short openness; // Misc, byte, percentage
    private short removeCallToArmsIfTotalCreaturesLessThan; // Battles, byte
    private short buildLostRoomAfterSeconds; // Rooms, byte
    private short unknown1[]; // 3 bytes // values: 0, 0, 0
    private boolean createEmptyAreasWhenIdle; // Rooms, hmm int
    private boolean buildBiggerLairAfterClaimingPortal; // Rooms, hmm int
    private boolean sellCapturedRoomsIfLowOnGold; // Rooms, hmm int
    private short minTimeBeforePlacingResearchedRoom; // Rooms
    private short defaultSize; // Rooms, byte, in square tiles
    private short tilesLeftBetweenRooms; // Rooms, byte
    private Distance distanceBetweenRoomsThatShouldBeCloseMan; // Rooms, byte
    private CorridorStyle corridorStyle; // Rooms, byte
    private RoomExpandPolicy whenMoreSpaceInRoomRequired; // Rooms, byte
    private short digToNeutralRoomsWithinTilesOfHeart; // Rooms, byte
    private List<Short> buildOrder; // General, roomIds, 15 bytes
    private short flexibility; // Rooms, byte, what?
    private short digToNeutralRoomsWithinTilesOfClaimedArea; // Rooms, byte
    private int removeCallToArmsAfterSeconds; // Battles, short
    private boolean boulderTrapsOnLongCorridors; // Doors / Traps, hmm int
    private boolean boulderTrapsOnRouteToBreachPoints; // Doors / Traps, hmm int
    private short trapUseStyle; // Doors / Traps, byte, 0-4, 0 being aggressive, 4 being defensive
    private short doorTrapPreference; // Doors / Traps, byte, 0-4, 0 being traps, 4 being doors
    private DoorUsagePolicy doorUsage; // Doors / Traps, byte
    private short chanceOfLookingToUseTrapsAndDoors; // Doors / Traps, byte, percentage
    private boolean requireMinLevelForCreatures; // Initial attack, hmm int, attack when
    private boolean requireTotalThreatGreaterThanTheEnemy; // Initial attack, hmm int, attack when
    private boolean requireAllRoomTypesPlaced; // Initial attack, hmm int, attack when
    private boolean requireAllKeeperSpellsResearched; // Initial attack, hmm int, attack when
    private boolean onlyAttackAttackers; // Initial attack, hmm int
    private boolean neverAttack; // Initial attack, hmm int
    private short minLevelForCreatures; // Initial attack, byte, attack when
    private short totalThreatGreaterThanTheEnemy; // Initial attack, byte, attack when, percentage
    private BreachRoomPolicy firstAttemptToBreachRoom; // Initial attack, byte
    private DigToPolicy firstDigToEnemyPoint; // Initial attack, byte
    private short breachAtPointsSimultaneously; // Initial attack, byte, number of points
    private short usePercentageOfTotalCreaturesInFirstFightAfterBreach; // Initial attack, byte
    private int manaValue; // Misc, short, percentage (of normal income?)
    private int placeCallToArmsWhereThreatValueIsGreaterThan; // Battles, short
    private short removeCallToArmsIfLessThanEnemyCreatures; // Battles, byte, number of enemy creatures in area defined in the next variable
    private short removeCallToArmsIfLessThanEnemyCreaturesWithinTiles; // Battles, byte
    private boolean pullCreaturesFromFightIfOutnumberedAndUnableToDropReinforcements; // Battles, hmm int
    private short threatValueOfDroppedCreaturesIsPercentageOfEnemyThreatValue; // Battles, byte, need to drop amount of own forces
    private short spellStyle; // Battles, byte, 0-4, 0 being aggressive, 4 being defensive
    private short attemptToImprisonPercentageOfEnemyCreatures; // Battles, byte
    private short ifCreatureHealthIsPercentageAndNotInOwnRoomMoveToLairOrTemple; // Battles, byte
    private int goldValue; // Misc, short, percentage (of normal income?)
    private boolean tryToMakeUnhappyOnesHappy; // Creatures, hmm int
    private boolean tryToMakeAngryOnesHappy; // Creatures, hmm int
    private boolean disposeOfAngryCreatures; // Creatures, hmm int
    private boolean disposeOfRubbishCreaturesIfBetterOnesComeAlong; // Creatures, hmm int
    private CreatureDisposalPolicy disposalMethod; // Creatures, byte
    private short maximumNumberOfImps; // Creatures, byte
    private boolean willNotSlapCreatures; // Creatures, byte, 0 = true, 32 = false
    private short attackWhenNumberOfCreaturesIsAtLeast; // Initial attack, byte, attack when
    private boolean useLightningIfEnemyIsInWater; // Spells, hmm int
    private SightOfEvilUsagePolicy useSightOfEvil; // Spells, byte
    private short useSpellsInBattle; // Spells, byte, 0-4, 0 being often, 4 being rarely
    private short spellsPowerPreference; // Spells, byte, 0-4, 0 being high power, 4 being low power
    private CallToArmsUsagePolicy useCallToArms; // Spells, byte
    private short unknown2[]; // 2 bytes // values: 200, 42
    private int mineGoldUntilGoldHeldIsGreaterThan; // Misc, short
    private int waitSecondsAfterPreviousAttackBeforeAttackingAgain; // Battles, short
    private int startingMana; // Misc, int (?) // @ArchDemon: value 0 - 100 (percentage ?)
    private int exploreUpToTilesToFindSpecials; // Misc, short, number of tiles
    private int impsToTilesRatio; // Misc, short, 1:x
    private int buildAreaStartX; // Short, 0 based coordinate
    private int buildAreaStartY; // Short, 0 based coordinate
    private int buildAreaEndX; // Short, 0 based coordinate
    private int buildAreaEndY; // Short, 0 based coordinate
    private MoveToResearchPolicy likelyhoodToMovingCreaturesToLibraryForResearching; // Misc, byte
    private short chanceOfExploringToFindSpecials; // Misc, byte, percentage
    private short chanceOfFindingSpecialsWhenExploring; // Misc, byte, percentage
    private ImprisonedCreatureFatePolicy fateOfImprisonedCreatures; // Misc, byte

    public AI.AIType getAiType() {
        return aiType;
    }

    protected void setAiType(AI.AIType aiType) {
        this.aiType = aiType;
    }

    public short getSpeed() {
        return speed;
    }

    protected void setSpeed(short speed) {
        this.speed = speed;
    }

    public short getOpenness() {
        return openness;
    }

    protected void setOpenness(short openness) {
        this.openness = openness;
    }

    public short getRemoveCallToArmsIfTotalCreaturesLessThan() {
        return removeCallToArmsIfTotalCreaturesLessThan;
    }

    protected void setRemoveCallToArmsIfTotalCreaturesLessThan(short removeCallToArmsIfTotalCreaturesLessThan) {
        this.removeCallToArmsIfTotalCreaturesLessThan = removeCallToArmsIfTotalCreaturesLessThan;
    }

    public short getBuildLostRoomAfterSeconds() {
        return buildLostRoomAfterSeconds;
    }

    protected void setBuildLostRoomAfterSeconds(short buildLostRoomAfterSeconds) {
        this.buildLostRoomAfterSeconds = buildLostRoomAfterSeconds;
    }

    public short[] getUnknown1() {
        return unknown1;
    }

    protected void setUnknown1(short[] unknown1) {
        this.unknown1 = unknown1;
    }

    public boolean isCreateEmptyAreasWhenIdle() {
        return createEmptyAreasWhenIdle;
    }

    protected void setCreateEmptyAreasWhenIdle(boolean createEmptyAreasWhenIdle) {
        this.createEmptyAreasWhenIdle = createEmptyAreasWhenIdle;
    }

    public boolean isBuildBiggerLairAfterClaimingPortal() {
        return buildBiggerLairAfterClaimingPortal;
    }

    protected void setBuildBiggerLairAfterClaimingPortal(boolean buildBiggerLairAfterClaimingPortal) {
        this.buildBiggerLairAfterClaimingPortal = buildBiggerLairAfterClaimingPortal;
    }

    public boolean isSellCapturedRoomsIfLowOnGold() {
        return sellCapturedRoomsIfLowOnGold;
    }

    protected void setSellCapturedRoomsIfLowOnGold(boolean sellCapturedRoomsIfLowOnGold) {
        this.sellCapturedRoomsIfLowOnGold = sellCapturedRoomsIfLowOnGold;
    }

    public short getMinTimeBeforePlacingResearchedRoom() {
        return minTimeBeforePlacingResearchedRoom;
    }

    protected void setMinTimeBeforePlacingResearchedRoom(short minTimeBeforePlacingResearchedRoom) {
        this.minTimeBeforePlacingResearchedRoom = minTimeBeforePlacingResearchedRoom;
    }

    public short getDefaultSize() {
        return defaultSize;
    }

    protected void setDefaultSize(short defaultSize) {
        this.defaultSize = defaultSize;
    }

    public short getTilesLeftBetweenRooms() {
        return tilesLeftBetweenRooms;
    }

    protected void setTilesLeftBetweenRooms(short tilesLeftBetweenRooms) {
        this.tilesLeftBetweenRooms = tilesLeftBetweenRooms;
    }

    public Distance getDistanceBetweenRoomsThatShouldBeCloseMan() {
        return distanceBetweenRoomsThatShouldBeCloseMan;
    }

    protected void setDistanceBetweenRoomsThatShouldBeCloseMan(Distance distanceBetweenRoomsThatShouldBeCloseMan) {
        this.distanceBetweenRoomsThatShouldBeCloseMan = distanceBetweenRoomsThatShouldBeCloseMan;
    }

    public CorridorStyle getCorridorStyle() {
        return corridorStyle;
    }

    protected void setCorridorStyle(CorridorStyle corridorStyle) {
        this.corridorStyle = corridorStyle;
    }

    public RoomExpandPolicy getWhenMoreSpaceInRoomRequired() {
        return whenMoreSpaceInRoomRequired;
    }

    protected void setWhenMoreSpaceInRoomRequired(RoomExpandPolicy whenMoreSpaceInRoomRequired) {
        this.whenMoreSpaceInRoomRequired = whenMoreSpaceInRoomRequired;
    }

    public short getDigToNeutralRoomsWithinTilesOfHeart() {
        return digToNeutralRoomsWithinTilesOfHeart;
    }

    protected void setDigToNeutralRoomsWithinTilesOfHeart(short digToNeutralRoomsWithinTilesOfHeart) {
        this.digToNeutralRoomsWithinTilesOfHeart = digToNeutralRoomsWithinTilesOfHeart;
    }

    public List<Short> getBuildOrder() {
        return buildOrder;
    }

    protected void setBuildOrder(List<Short> buildOrder) {
        this.buildOrder = buildOrder;
    }

    public short getFlexibility() {
        return flexibility;
    }

    protected void setFlexibility(short flexibility) {
        this.flexibility = flexibility;
    }

    public short getDigToNeutralRoomsWithinTilesOfClaimedArea() {
        return digToNeutralRoomsWithinTilesOfClaimedArea;
    }

    protected void setDigToNeutralRoomsWithinTilesOfClaimedArea(short digToNeutralRoomsWithinTilesOfClaimedArea) {
        this.digToNeutralRoomsWithinTilesOfClaimedArea = digToNeutralRoomsWithinTilesOfClaimedArea;
    }

    public int getRemoveCallToArmsAfterSeconds() {
        return removeCallToArmsAfterSeconds;
    }

    protected void setRemoveCallToArmsAfterSeconds(int removeCallToArmsAfterSeconds) {
        this.removeCallToArmsAfterSeconds = removeCallToArmsAfterSeconds;
    }

    public boolean isBoulderTrapsOnLongCorridors() {
        return boulderTrapsOnLongCorridors;
    }

    protected void setBoulderTrapsOnLongCorridors(boolean boulderTrapsOnLongCorridors) {
        this.boulderTrapsOnLongCorridors = boulderTrapsOnLongCorridors;
    }

    public boolean isBoulderTrapsOnRouteToBreachPoints() {
        return boulderTrapsOnRouteToBreachPoints;
    }

    protected void setBoulderTrapsOnRouteToBreachPoints(boolean boulderTrapsOnRouteToBreachPoints) {
        this.boulderTrapsOnRouteToBreachPoints = boulderTrapsOnRouteToBreachPoints;
    }

    public short getTrapUseStyle() {
        return trapUseStyle;
    }

    protected void setTrapUseStyle(short trapUseStyle) {
        this.trapUseStyle = trapUseStyle;
    }

    public short getDoorTrapPreference() {
        return doorTrapPreference;
    }

    protected void setDoorTrapPreference(short doorTrapPreference) {
        this.doorTrapPreference = doorTrapPreference;
    }

    public DoorUsagePolicy getDoorUsage() {
        return doorUsage;
    }

    protected void setDoorUsage(DoorUsagePolicy doorUsage) {
        this.doorUsage = doorUsage;
    }

    public short getChanceOfLookingToUseTrapsAndDoors() {
        return chanceOfLookingToUseTrapsAndDoors;
    }

    protected void setChanceOfLookingToUseTrapsAndDoors(short chanceOfLookingToUseTrapsAndDoors) {
        this.chanceOfLookingToUseTrapsAndDoors = chanceOfLookingToUseTrapsAndDoors;
    }

    public boolean isRequireMinLevelForCreatures() {
        return requireMinLevelForCreatures;
    }

    protected void setRequireMinLevelForCreatures(boolean requireMinLevelForCreatures) {
        this.requireMinLevelForCreatures = requireMinLevelForCreatures;
    }

    public boolean isRequireTotalThreatGreaterThanTheEnemy() {
        return requireTotalThreatGreaterThanTheEnemy;
    }

    protected void setRequireTotalThreatGreaterThanTheEnemy(boolean requireTotalThreatGreaterThanTheEnemy) {
        this.requireTotalThreatGreaterThanTheEnemy = requireTotalThreatGreaterThanTheEnemy;
    }

    public boolean isRequireAllRoomTypesPlaced() {
        return requireAllRoomTypesPlaced;
    }

    protected void setRequireAllRoomTypesPlaced(boolean requireAllRoomTypesPlaced) {
        this.requireAllRoomTypesPlaced = requireAllRoomTypesPlaced;
    }

    public boolean isRequireAllKeeperSpellsResearched() {
        return requireAllKeeperSpellsResearched;
    }

    protected void setRequireAllKeeperSpellsResearched(boolean requireAllKeeperSpellsResearched) {
        this.requireAllKeeperSpellsResearched = requireAllKeeperSpellsResearched;
    }

    public boolean isOnlyAttackAttackers() {
        return onlyAttackAttackers;
    }

    protected void setOnlyAttackAttackers(boolean onlyAttackAttackers) {
        this.onlyAttackAttackers = onlyAttackAttackers;
    }

    public boolean isNeverAttack() {
        return neverAttack;
    }

    protected void setNeverAttack(boolean neverAttack) {
        this.neverAttack = neverAttack;
    }

    public short getMinLevelForCreatures() {
        return minLevelForCreatures;
    }

    protected void setMinLevelForCreatures(short minLevelForCreatures) {
        this.minLevelForCreatures = minLevelForCreatures;
    }

    public short getTotalThreatGreaterThanTheEnemy() {
        return totalThreatGreaterThanTheEnemy;
    }

    protected void setTotalThreatGreaterThanTheEnemy(short totalThreatGreaterThanTheEnemy) {
        this.totalThreatGreaterThanTheEnemy = totalThreatGreaterThanTheEnemy;
    }

    public BreachRoomPolicy getFirstAttemptToBreachRoom() {
        return firstAttemptToBreachRoom;
    }

    protected void setFirstAttemptToBreachRoom(BreachRoomPolicy firstAttemptToBreachRoom) {
        this.firstAttemptToBreachRoom = firstAttemptToBreachRoom;
    }

    public DigToPolicy getFirstDigToEnemyPoint() {
        return firstDigToEnemyPoint;
    }

    protected void setFirstDigToEnemyPoint(DigToPolicy firstDigToEnemyPoint) {
        this.firstDigToEnemyPoint = firstDigToEnemyPoint;
    }

    public short getBreachAtPointsSimultaneously() {
        return breachAtPointsSimultaneously;
    }

    protected void setBreachAtPointsSimultaneously(short breachAtPointsSimultaneously) {
        this.breachAtPointsSimultaneously = breachAtPointsSimultaneously;
    }

    public short getUsePercentageOfTotalCreaturesInFirstFightAfterBreach() {
        return usePercentageOfTotalCreaturesInFirstFightAfterBreach;
    }

    protected void setUsePercentageOfTotalCreaturesInFirstFightAfterBreach(short usePercentageOfTotalCreaturesInFirstFightAfterBreach) {
        this.usePercentageOfTotalCreaturesInFirstFightAfterBreach = usePercentageOfTotalCreaturesInFirstFightAfterBreach;
    }

    public int getManaValue() {
        return manaValue;
    }

    protected void setManaValue(int manaValue) {
        this.manaValue = manaValue;
    }

    public int getPlaceCallToArmsWhereThreatValueIsGreaterThan() {
        return placeCallToArmsWhereThreatValueIsGreaterThan;
    }

    protected void setPlaceCallToArmsWhereThreatValueIsGreaterThan(int placeCallToArmsWhereThreatValueIsGreaterThan) {
        this.placeCallToArmsWhereThreatValueIsGreaterThan = placeCallToArmsWhereThreatValueIsGreaterThan;
    }

    public short getRemoveCallToArmsIfLessThanEnemyCreatures() {
        return removeCallToArmsIfLessThanEnemyCreatures;
    }

    protected void setRemoveCallToArmsIfLessThanEnemyCreatures(short removeCallToArmsIfLessThanEnemyCreatures) {
        this.removeCallToArmsIfLessThanEnemyCreatures = removeCallToArmsIfLessThanEnemyCreatures;
    }

    public short getRemoveCallToArmsIfLessThanEnemyCreaturesWithinTiles() {
        return removeCallToArmsIfLessThanEnemyCreaturesWithinTiles;
    }

    protected void setRemoveCallToArmsIfLessThanEnemyCreaturesWithinTiles(short removeCallToArmsIfLessThanEnemyCreaturesWithinTiles) {
        this.removeCallToArmsIfLessThanEnemyCreaturesWithinTiles = removeCallToArmsIfLessThanEnemyCreaturesWithinTiles;
    }

    public boolean isPullCreaturesFromFightIfOutnumberedAndUnableToDropReinforcements() {
        return pullCreaturesFromFightIfOutnumberedAndUnableToDropReinforcements;
    }

    protected void setPullCreaturesFromFightIfOutnumberedAndUnableToDropReinforcements(boolean pullCreaturesFromFightIfOutnumberedAndUnableToDropReinforcements) {
        this.pullCreaturesFromFightIfOutnumberedAndUnableToDropReinforcements = pullCreaturesFromFightIfOutnumberedAndUnableToDropReinforcements;
    }

    public short getThreatValueOfDroppedCreaturesIsPercentageOfEnemyThreatValue() {
        return threatValueOfDroppedCreaturesIsPercentageOfEnemyThreatValue;
    }

    protected void setThreatValueOfDroppedCreaturesIsPercentageOfEnemyThreatValue(short threatValueOfDroppedCreaturesIsPercentageOfEnemyThreatValue) {
        this.threatValueOfDroppedCreaturesIsPercentageOfEnemyThreatValue = threatValueOfDroppedCreaturesIsPercentageOfEnemyThreatValue;
    }

    public short getSpellStyle() {
        return spellStyle;
    }

    protected void setSpellStyle(short spellStyle) {
        this.spellStyle = spellStyle;
    }

    public short getAttemptToImprisonPercentageOfEnemyCreatures() {
        return attemptToImprisonPercentageOfEnemyCreatures;
    }

    protected void setAttemptToImprisonPercentageOfEnemyCreatures(short attemptToImprisonPercentageOfEnemyCreatures) {
        this.attemptToImprisonPercentageOfEnemyCreatures = attemptToImprisonPercentageOfEnemyCreatures;
    }

    public short getIfCreatureHealthIsPercentageAndNotInOwnRoomMoveToLairOrTemple() {
        return ifCreatureHealthIsPercentageAndNotInOwnRoomMoveToLairOrTemple;
    }

    protected void setIfCreatureHealthIsPercentageAndNotInOwnRoomMoveToLairOrTemple(short ifCreatureHealthIsPercentageAndNotInOwnRoomMoveToLairOrTemple) {
        this.ifCreatureHealthIsPercentageAndNotInOwnRoomMoveToLairOrTemple = ifCreatureHealthIsPercentageAndNotInOwnRoomMoveToLairOrTemple;
    }

    public int getGoldValue() {
        return goldValue;
    }

    protected void setGoldValue(int goldValue) {
        this.goldValue = goldValue;
    }

    public boolean isTryToMakeUnhappyOnesHappy() {
        return tryToMakeUnhappyOnesHappy;
    }

    protected void setTryToMakeUnhappyOnesHappy(boolean tryToMakeUnhappyOnesHappy) {
        this.tryToMakeUnhappyOnesHappy = tryToMakeUnhappyOnesHappy;
    }

    public boolean isTryToMakeAngryOnesHappy() {
        return tryToMakeAngryOnesHappy;
    }

    protected void setTryToMakeAngryOnesHappy(boolean tryToMakeAngryOnesHappy) {
        this.tryToMakeAngryOnesHappy = tryToMakeAngryOnesHappy;
    }

    public boolean isDisposeOfAngryCreatures() {
        return disposeOfAngryCreatures;
    }

    protected void setDisposeOfAngryCreatures(boolean disposeOfAngryCreatures) {
        this.disposeOfAngryCreatures = disposeOfAngryCreatures;
    }

    public boolean isDisposeOfRubbishCreaturesIfBetterOnesComeAlong() {
        return disposeOfRubbishCreaturesIfBetterOnesComeAlong;
    }

    protected void setDisposeOfRubbishCreaturesIfBetterOnesComeAlong(boolean disposeOfRubbishCreaturesIfBetterOnesComeAlong) {
        this.disposeOfRubbishCreaturesIfBetterOnesComeAlong = disposeOfRubbishCreaturesIfBetterOnesComeAlong;
    }

    public CreatureDisposalPolicy getDisposalMethod() {
        return disposalMethod;
    }

    protected void setDisposalMethod(CreatureDisposalPolicy disposalMethod) {
        this.disposalMethod = disposalMethod;
    }

    public short getMaximumNumberOfImps() {
        return maximumNumberOfImps;
    }

    protected void setMaximumNumberOfImps(short maximumNumberOfImps) {
        this.maximumNumberOfImps = maximumNumberOfImps;
    }

    public boolean isWillNotSlapCreatures() {
        return willNotSlapCreatures;
    }

    protected void setWillNotSlapCreatures(boolean willNotSlapCreatures) {
        this.willNotSlapCreatures = willNotSlapCreatures;
    }

    public short getAttackWhenNumberOfCreaturesIsAtLeast() {
        return attackWhenNumberOfCreaturesIsAtLeast;
    }

    protected void setAttackWhenNumberOfCreaturesIsAtLeast(short attackWhenNumberOfCreaturesIsAtLeast) {
        this.attackWhenNumberOfCreaturesIsAtLeast = attackWhenNumberOfCreaturesIsAtLeast;
    }

    public boolean isUseLightningIfEnemyIsInWater() {
        return useLightningIfEnemyIsInWater;
    }

    protected void setUseLightningIfEnemyIsInWater(boolean useLightningIfEnemyIsInWater) {
        this.useLightningIfEnemyIsInWater = useLightningIfEnemyIsInWater;
    }

    public SightOfEvilUsagePolicy getUseSightOfEvil() {
        return useSightOfEvil;
    }

    protected void setUseSightOfEvil(SightOfEvilUsagePolicy useSightOfEvil) {
        this.useSightOfEvil = useSightOfEvil;
    }

    public short getUseSpellsInBattle() {
        return useSpellsInBattle;
    }

    protected void setUseSpellsInBattle(short useSpellsInBattle) {
        this.useSpellsInBattle = useSpellsInBattle;
    }

    public short getSpellsPowerPreference() {
        return spellsPowerPreference;
    }

    protected void setSpellsPowerPreference(short spellsPowerPreference) {
        this.spellsPowerPreference = spellsPowerPreference;
    }

    public CallToArmsUsagePolicy getUseCallToArms() {
        return useCallToArms;
    }

    protected void setUseCallToArms(CallToArmsUsagePolicy useCallToArms) {
        this.useCallToArms = useCallToArms;
    }

    public short[] getUnknown2() {
        return unknown2;
    }

    protected void setUnknown2(short[] unknown2) {
        this.unknown2 = unknown2;
    }

    public int getMineGoldUntilGoldHeldIsGreaterThan() {
        return mineGoldUntilGoldHeldIsGreaterThan;
    }

    protected void setMineGoldUntilGoldHeldIsGreaterThan(int mineGoldUntilGoldHeldIsGreaterThan) {
        this.mineGoldUntilGoldHeldIsGreaterThan = mineGoldUntilGoldHeldIsGreaterThan;
    }

    public int getWaitSecondsAfterPreviousAttackBeforeAttackingAgain() {
        return waitSecondsAfterPreviousAttackBeforeAttackingAgain;
    }

    protected void setWaitSecondsAfterPreviousAttackBeforeAttackingAgain(int waitSecondsAfterPreviousAttackBeforeAttackingAgain) {
        this.waitSecondsAfterPreviousAttackBeforeAttackingAgain = waitSecondsAfterPreviousAttackBeforeAttackingAgain;
    }

    public int getStartingMana() {
        return startingMana;
    }

    protected void setStartingMana(int startingMana) {
        this.startingMana = startingMana;
    }

    public int getExploreUpToTilesToFindSpecials() {
        return exploreUpToTilesToFindSpecials;
    }

    protected void setExploreUpToTilesToFindSpecials(int exploreUpToTilesToFindSpecials) {
        this.exploreUpToTilesToFindSpecials = exploreUpToTilesToFindSpecials;
    }

    public int getImpsToTilesRatio() {
        return impsToTilesRatio;
    }

    protected void setImpsToTilesRatio(int impsToTilesRatio) {
        this.impsToTilesRatio = impsToTilesRatio;
    }

    public int getBuildAreaStartX() {
        return buildAreaStartX;
    }

    protected void setBuildAreaStartX(int buildAreaStartX) {
        this.buildAreaStartX = buildAreaStartX;
    }

    public int getBuildAreaStartY() {
        return buildAreaStartY;
    }

    protected void setBuildAreaStartY(int buildAreaStartY) {
        this.buildAreaStartY = buildAreaStartY;
    }

    public int getBuildAreaEndX() {
        return buildAreaEndX;
    }

    protected void setBuildAreaEndX(int buildAreaEndX) {
        this.buildAreaEndX = buildAreaEndX;
    }

    public int getBuildAreaEndY() {
        return buildAreaEndY;
    }

    protected void setBuildAreaEndY(int buildAreaEndY) {
        this.buildAreaEndY = buildAreaEndY;
    }

    public MoveToResearchPolicy getLikelyhoodToMovingCreaturesToLibraryForResearching() {
        return likelyhoodToMovingCreaturesToLibraryForResearching;
    }

    protected void setLikelyhoodToMovingCreaturesToLibraryForResearching(MoveToResearchPolicy likelyhoodToMovingCreaturesToLibraryForResearching) {
        this.likelyhoodToMovingCreaturesToLibraryForResearching = likelyhoodToMovingCreaturesToLibraryForResearching;
    }

    public short getChanceOfExploringToFindSpecials() {
        return chanceOfExploringToFindSpecials;
    }

    protected void setChanceOfExploringToFindSpecials(short chanceOfExploringToFindSpecials) {
        this.chanceOfExploringToFindSpecials = chanceOfExploringToFindSpecials;
    }

    public short getChanceOfFindingSpecialsWhenExploring() {
        return chanceOfFindingSpecialsWhenExploring;
    }

    protected void setChanceOfFindingSpecialsWhenExploring(short chanceOfFindingSpecialsWhenExploring) {
        this.chanceOfFindingSpecialsWhenExploring = chanceOfFindingSpecialsWhenExploring;
    }

    public ImprisonedCreatureFatePolicy getFateOfImprisonedCreatures() {
        return fateOfImprisonedCreatures;
    }

    protected void setFateOfImprisonedCreatures(ImprisonedCreatureFatePolicy fateOfImprisonedCreatures) {
        this.fateOfImprisonedCreatures = fateOfImprisonedCreatures;
    }
}
