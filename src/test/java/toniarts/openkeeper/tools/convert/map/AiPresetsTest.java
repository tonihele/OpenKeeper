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

import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.AI.AIType;
import static toniarts.openkeeper.tools.convert.map.RoomFixtures.room;

class AiPresetsTest {

    private static final short TREASURY = 1;
    private static final short PORTAL = 3;
    private static final short DUNGEON_HEART = 5;
    private static final short HERO_PORTAL = 25;

    private final List<Room> rooms = List.of(
            room(HERO_PORTAL, 0),
            room(PORTAL, 0),
            room(DUNGEON_HEART, 0),
            room(TREASURY, 10, Room.RoomFlag.BUILDABLE));

    @Test
    void theMasterKeeperBuildsBigRoomsWithManyImpsAndAttacksEarly() {
        AI ai = AiPresets.create(AIType.MASTER_KEEPER, rooms, 8, 48, 40);

        assertEquals(25, ai.getDefaultSize());
        assertEquals(2, ai.getTilesLeftBetweenRooms());
        assertEquals(AI.Distance.CLOSE, ai.getDistanceBetweenRoomsThatShouldBeCloseMan());
        assertEquals(50, ai.getOpenness());
        assertEquals(20, ai.getMaximumNumberOfImps());
        assertEquals(15, ai.getImpsToTilesRatio());
        assertEquals(9000, ai.getMineGoldUntilGoldHeldIsGreaterThan());
        assertEquals(1, ai.getSpeed());
        assertEquals(20, ai.getTotalThreatGreaterThanTheEnemy());
        assertEquals(125, ai.getThreatValueOfDroppedCreaturesIsPercentageOfEnemyThreatValue());
        assertEquals(2000, ai.getPlaceCallToArmsWhereThreatValueIsGreaterThan());
        assertEquals(10, ai.getIfCreatureHealthIsPercentageAndNotInOwnRoomMoveToLairOrTemple());
        assertEquals(AI.CallToArmsUsagePolicy.AT_HEART_WITH_CREATURES, ai.getUseCallToArms());
        assertEquals(AI.DigToPolicy.CLOSEST_TO_ENEMY_HEART, ai.getFirstDigToEnemyPoint());
        assertEquals(AI.BreachRoomPolicy.WEAK_ROOM, ai.getFirstAttemptToBreachRoom());
        assertFalse(ai.isNeverAttack());
    }

    @Test
    void theParanoidBuildsSmallRoomsAnywhereAndNeverAttacks() {
        AI ai = AiPresets.create(AIType.PARANOID, rooms, 8, 48, 40);

        assertEquals(12, ai.getDefaultSize());
        assertEquals(AI.Distance.RANDOM, ai.getDistanceBetweenRoomsThatShouldBeCloseMan());
        assertEquals(15, ai.getOpenness());
        assertEquals(4, ai.getMaximumNumberOfImps());
        assertEquals(3500, ai.getPlaceCallToArmsWhereThreatValueIsGreaterThan());
        assertEquals(30, ai.getIfCreatureHealthIsPercentageAndNotInOwnRoomMoveToLairOrTemple());
        assertEquals(AI.ImprisonedCreatureFatePolicy.LEAVE_TO_DIE, ai.getFateOfImprisonedCreatures());
        assertTrue(ai.isNeverAttack());
    }

    @Test
    void theBuildOrderIsTheRoomsThatCanBeBuiltAndThePortalsById() {
        AI ai = AiPresets.create(AIType.GREYMAN, rooms, 8, 48, 40);

        assertEquals(List.of(TREASURY, PORTAL, HERO_PORTAL), ai.getBuildOrder());
    }

    @Test
    void mayBuildOnTheWholeMap() {
        AI ai = AiPresets.create(AIType.GREYMAN, rooms, 8, 48, 40);

        assertEquals(0, ai.getBuildAreaStartX());
        assertEquals(0, ai.getBuildAreaStartY());
        assertEquals(47, ai.getBuildAreaEndX());
        assertEquals(39, ai.getBuildAreaEndY());
    }

    @Test
    void theCreatureLevelAskedForIsNoHigherThanTheCombatPitTrainsTo() {
        assertEquals(8, AiPresets.create(AIType.MASTER_KEEPER, rooms, 8, 48, 40).getMinLevelForCreatures());
        assertEquals(6, AiPresets.create(AIType.MASTER_KEEPER, rooms, 6, 48, 40).getMinLevelForCreatures());
        assertEquals(5, AiPresets.create(AIType.CONQUEROR, rooms, 8, 48, 40).getMinLevelForCreatures());
    }

    @Test
    void someMapsSwapTheTypeForAnother() {
        assertEquals(AIType.CONQUEROR, AiPresets.getTypeOnMap(AIType.MASTER_KEEPER, "Spiral"));
        assertEquals(AIType.PSYCHOTIC, AiPresets.getTypeOnMap(AIType.PSYCHOTIC, "Spiral"));
        assertEquals(AIType.PSYCHOTIC, AiPresets.getTypeOnMap(AIType.THICK_SKINNED, "Caverns"));
        assertEquals(AIType.MASTER_KEEPER, AiPresets.getTypeOnMap(AIType.GUARDIAN, "Furnace"));
        assertEquals(AIType.GUARDIAN, AiPresets.getTypeOnMap(AIType.GUARDIAN, "Small"));

        // The map file is called Onslaught, so DK II's entry for it never matches
        assertEquals(AIType.PARANOID, AiPresets.getTypeOnMap(AIType.PARANOID, "Onslaught"));
    }
}
