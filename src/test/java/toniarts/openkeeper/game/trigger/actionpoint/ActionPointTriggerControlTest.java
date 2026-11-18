/*
 * Copyright (C) 2026 OpenKeeper
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
package toniarts.openkeeper.game.trigger.actionpoint;

import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.logic.PositionSystem;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.trigger.TriggerGenericData;
import toniarts.openkeeper.game.trigger.TriggerLoader;
import toniarts.openkeeper.tools.convert.map.APPossessedCreatureEntersTriggerFixtures;
import toniarts.openkeeper.tools.convert.map.APCongregateInTriggerFixtures;
import toniarts.openkeeper.tools.convert.map.APTagAllOfTriggerFixtures;
import toniarts.openkeeper.tools.convert.map.ActionPointFixtures;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ActionPointTriggerControlTest {

    private static final ActionPoint ACTION_POINT = actionPoint(1, 1, 2, 2);

    @Test
    void tagAllOfCountsPlayerTasksAgainstTaggableTerrainAndIgnoresAuthoredComparison() {
        IKwdFile kwd = APTagAllOfTriggerFixtures.tagAllOf((short) 1,
                TriggerGeneric.ComparisonType.EQUAL_TO, 99);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        fixture.setTerrain(1, 1, APTagAllOfTriggerFixtures.TAGGABLE_TERRAIN_ID);
        fixture.setTerrain(2, 1, APTagAllOfTriggerFixtures.TAGGABLE_TERRAIN_ID);
        fixture.setTerrain(1, 2, APTagAllOfTriggerFixtures.TAGGABLE_TERRAIN_ID);
        fixture.setTerrain(2, 2, APTagAllOfTriggerFixtures.UNTAGGABLE_TERRAIN_ID);
        fixture.setTerrain(0, 0, APTagAllOfTriggerFixtures.TAGGABLE_TERRAIN_ID);
        fixture.setTagged(1, 1, (short) 1, true);
        fixture.setTagged(2, 1, (short) 1, true);
        fixture.setTagged(1, 2, (short) 2, true);
        fixture.setTagged(0, 0, (short) 1, true);

        TriggerGenericData condition = fixture.condition();
        ActionPointTriggerControl control = fixture.control();
        assertFalse(control.isActive(condition), "other players' tasks do not count");

        fixture.setTagged(1, 2, (short) 1, true);
        assertTrue(control.isActive(condition), "three player tasks cover the three taggable tiles");

        fixture.setTagged(1, 1, (short) 1, false);
        assertFalse(control.isActive(condition), "removing a player's task lowers the count immediately");
    }

    @Test
    void tagAllOfUsesAPlayerTaskCountComparedWithTheEligibleTileCount() {
        IKwdFile kwd = APTagAllOfTriggerFixtures.tagAllOf((short) 1,
                TriggerGeneric.ComparisonType.EQUAL_TO, 500);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        fixture.setTerrain(1, 1, APTagAllOfTriggerFixtures.TAGGABLE_TERRAIN_ID);
        fixture.setTerrain(2, 1, APTagAllOfTriggerFixtures.TAGGABLE_TERRAIN_ID);
        fixture.setTerrain(1, 2, APTagAllOfTriggerFixtures.UNTAGGABLE_TERRAIN_ID);
        fixture.setTerrain(2, 2, APTagAllOfTriggerFixtures.UNTAGGABLE_TERRAIN_ID);
        fixture.setTagged(1, 1, (short) 1, true);

        TriggerGenericData condition = fixture.condition();
        ActionPointTriggerControl control = fixture.control();
        assertFalse(control.isActive(condition));

        // The executable compares counts, not tile identities. This models a
        // task record surviving a terrain eligibility change. Normal tagging
        // input cannot create a task on a tile after it becomes untaggable.
        fixture.setTerrain(1, 1, APTagAllOfTriggerFixtures.UNTAGGABLE_TERRAIN_ID);
        assertTrue(control.isActive(condition));
    }

    @Test
    void tagAllOfReevaluatesDynamicTerrainAndTaskChanges() {
        IKwdFile kwd = APTagAllOfTriggerFixtures.tagAllOf((short) 1,
                TriggerGeneric.ComparisonType.NONE, 0);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        fixture.setTerrain(1, 1, APTagAllOfTriggerFixtures.TAGGABLE_TERRAIN_ID);
        fixture.setTerrain(2, 1, APTagAllOfTriggerFixtures.TAGGABLE_TERRAIN_ID);
        fixture.setTerrain(1, 2, APTagAllOfTriggerFixtures.UNTAGGABLE_TERRAIN_ID);
        fixture.setTerrain(2, 2, APTagAllOfTriggerFixtures.UNTAGGABLE_TERRAIN_ID);
        fixture.setTagged(1, 1, (short) 1, true);
        fixture.setTagged(2, 1, (short) 1, true);

        TriggerGenericData condition = fixture.condition();
        ActionPointTriggerControl control = fixture.control();
        assertTrue(control.isActive(condition));

        fixture.setTerrain(1, 2, APTagAllOfTriggerFixtures.TAGGABLE_TERRAIN_ID);
        assertFalse(control.isActive(condition), "newly taggable terrain raises the required count");
        fixture.setTagged(1, 2, (short) 1, true);
        assertTrue(control.isActive(condition));

        fixture.setTerrain(2, 1, APTagAllOfTriggerFixtures.UNTAGGABLE_TERRAIN_ID);
        fixture.setTagged(2, 1, (short) 1, false);
        assertTrue(control.isActive(condition), "removed eligible terrain lowers the required count");

        fixture.setTagged(1, 1, (short) 1, false);
        assertFalse(control.isActive(condition), "untagging lowers the task count");
    }

    @Test
    void tagAllOfTreatsAnAreaWithoutTaggableTilesAsSatisfied() {
        IKwdFile kwd = APTagAllOfTriggerFixtures.tagAllOf((short) 1,
                TriggerGeneric.ComparisonType.NONE, 0);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        for (int x = 1; x <= 2; x++) {
            for (int y = 1; y <= 2; y++) {
                fixture.setTerrain(x, y, APTagAllOfTriggerFixtures.UNTAGGABLE_TERRAIN_ID);
            }
        }

        assertTrue(fixture.isActive(), "type 69's literal comparison is 0 >= 0");
    }

    @Test
    void tagAllOfRejectsInvalidTriggerAndMapDataWithoutThrowing() {
        IKwdFile missingPlayer = APTagAllOfTriggerFixtures.tagAllOfWithoutPlayerId(
                TriggerGeneric.ComparisonType.NONE, 0);
        Fixture missingPlayerFixture = new Fixture(missingPlayer, ACTION_POINT, 4, 4);
        assertFalse(missingPlayerFixture.isActive());

        IKwdFile kwd = APTagAllOfTriggerFixtures.tagAllOf((short) 1,
                TriggerGeneric.ComparisonType.NONE, 0);
        Fixture invalidAreaFixture = new Fixture(kwd, actionPoint(0, 0, 4, 4), 4, 4);
        assertFalse(invalidAreaFixture.isActive());

        Fixture unknownTerrainFixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        unknownTerrainFixture.setTerrain(1, 1, (short) 99);
        assertFalse(unknownTerrainFixture.isActive());
    }

    @Test
    void tagAllOfActivatesItsExistingChildActionWhenTheTerrainCountIsCovered() {
        IKwdFile kwd = APTagAllOfTriggerFixtures.tagAllOf((short) 1,
                TriggerGeneric.ComparisonType.GREATER_THAN, 500, true);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        fixture.setTerrain(1, 1, APTagAllOfTriggerFixtures.TAGGABLE_TERRAIN_ID);
        fixture.setTerrain(1, 2, APTagAllOfTriggerFixtures.UNTAGGABLE_TERRAIN_ID);
        fixture.setTerrain(2, 2, APTagAllOfTriggerFixtures.UNTAGGABLE_TERRAIN_ID);
        fixture.setTagged(1, 1, (short) 1, true);
        ActionPointTriggerControl control = fixture.control();

        control.update(0.25f);
        control.update(0.25f);
        assertFalse(fixture.flags.containsKey((short) 2));

        fixture.setTerrain(2, 1, APTagAllOfTriggerFixtures.TAGGABLE_TERRAIN_ID);
        control.update(0.25f);
        control.update(0.25f);
        assertFalse(fixture.flags.containsKey((short) 2));

        fixture.setTagged(2, 1, (short) 1, true);
        control.update(0.25f);
        control.update(0.25f);
        assertEquals(1, fixture.flags.get((short) 2));
    }

    @ParameterizedTest
    @CsvSource({
        "LESS_THAN,2,1,true", "LESS_THAN,2,2,false",
        "LESS_OR_EQUAL_TO,2,2,true", "LESS_OR_EQUAL_TO,2,3,false",
        "EQUAL_TO,2,2,true", "EQUAL_TO,2,1,false",
        "GREATER_THAN,2,3,true", "GREATER_THAN,2,2,false",
        "GREATER_OR_EQUAL_TO,2,2,true", "GREATER_OR_EQUAL_TO,2,1,false",
        "NOT_EQUAL_TO,2,1,true", "NOT_EQUAL_TO,2,2,false"
    })
    void appliesEveryComparisonOperatorToTheCurrentOccupancy(String comparisonName,
            int threshold, int count, boolean expected) {
        TriggerGeneric.ComparisonType comparison = TriggerGeneric.ComparisonType.valueOf(comparisonName);
        IKwdFile kwd = APCongregateInTriggerFixtures.congregateIn(comparison,
                (short) 0, (short) 0, (short) 3, threshold);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        for (int i = 0; i < count; i++) {
            fixture.addCreature((short) 1, (short) (10 + i), 1, 1);
        }

        assertTrue(expected == fixture.isActive(), comparisonName + " with count " + count);
    }

    @Test
    void countsOnlyMatchingCreaturesWithinTheInclusiveActionPointRectangle() {
        IKwdFile kwd = APCongregateInTriggerFixtures.congregateIn(TriggerGeneric.ComparisonType.EQUAL_TO,
                (short) 1, (short) 7, (short) 3, 2);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        fixture.addCreature((short) 1, (short) 7, 1, 1); // top-left edge
        fixture.addCreature((short) 1, (short) 7, 2, 2); // bottom-right edge
        fixture.addCreature((short) 1, (short) 7, 3, 2); // outside
        fixture.addCreature((short) 1, (short) 8, 1, 2); // wrong creature type
        fixture.addCreature((short) 2, (short) 7, 2, 1); // wrong owner

        assertTrue(fixture.isActive());
    }

    @Test
    void typeZeroPreservesTheStockMapCreatureSelectorBehavior() {
        IKwdFile kwd = APCongregateInTriggerFixtures.congregateIn(TriggerGeneric.ComparisonType.EQUAL_TO,
                (short) 0, (short) 0, (short) 0, 2);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        fixture.addCreature((short) 1, (short) 7, 1, 1);
        fixture.addCreature((short) 2, (short) 8, 2, 2);

        assertTrue(fixture.isActive());
    }

    @Test
    void possessedCreatureConditionTracksCurrentPresenceInInclusiveRectangle() {
        IKwdFile kwd = APPossessedCreatureEntersTriggerFixtures.possessedCreatureEnters(null,
                (short) 1, (short) 7, (short) 3, 0, false);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        ActionPointTriggerControl control = fixture.control();
        TriggerGenericData condition = fixture.condition();
        Occupant creature = fixture.addCreature((short) 1, (short) 7, 0, 1);

        assertFalse(control.isActive(condition)); // not possessed
        creature.moveTo(1, 1);
        assertFalse(control.isActive(condition)); // inside, but not possessed
        fixture.possess(creature);
        assertTrue(control.isActive(condition));
        assertTrue(control.isActive(condition)); // this is an existence predicate, not an edge counter

        creature.moveTo(3, 1);
        assertFalse(control.isActive(condition));
        creature.moveTo(2, 2); // inclusive bottom-right edge
        assertTrue(control.isActive(condition));
    }

    @Test
    void possessionAndOwnershipTransitionsInsideUpdateTheCondition() {
        IKwdFile kwd = APPossessedCreatureEntersTriggerFixtures.possessedCreatureEnters(null,
                (short) 1, (short) 7, (short) 0, 0, false);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        ActionPointTriggerControl control = fixture.control();
        TriggerGenericData condition = fixture.condition();
        Occupant creature = fixture.addCreature((short) 2, (short) 7, 1, 1);

        assertFalse(control.isActive(condition)); // wrong owner
        fixture.possess(creature);
        assertFalse(control.isActive(condition)); // still wrong owner

        fixture.changeOwner(creature, (short) 1);
        assertTrue(control.isActive(condition)); // owner changes while possession is active
        assertTrue(control.isActive(condition));

        fixture.endPossession(creature);
        assertFalse(control.isActive(condition));
        fixture.possess(creature);
        assertTrue(control.isActive(condition));
    }

    @Test
    void conditionRequiresPossessionAndMatchingCreatureAndPlayerFilters() {
        IKwdFile kwd = APPossessedCreatureEntersTriggerFixtures.possessedCreatureEnters(null,
                (short) 1, (short) 7, (short) 3, 0, false);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        ActionPointTriggerControl control = fixture.control();
        TriggerGenericData condition = fixture.condition();
        Occupant wrongType = fixture.addCreature((short) 1, (short) 8, 0, 1);
        fixture.possess(wrongType);
        assertFalse(control.isActive(condition));

        wrongType.moveTo(1, 1);
        assertFalse(control.isActive(condition));
        fixture.remove(wrongType);

        Occupant wrongOwner = fixture.addCreature((short) 2, (short) 7, 0, 1);
        fixture.possess(wrongOwner);
        assertFalse(control.isActive(condition));
        wrongOwner.moveTo(1, 1);
        assertFalse(control.isActive(condition));

        Occupant unpossessed = fixture.addCreature((short) 1, (short) 7, 0, 2);
        assertFalse(control.isActive(condition));
        unpossessed.moveTo(1, 2);
        assertFalse(control.isActive(condition));
        fixture.possess(unpossessed);
        assertTrue(control.isActive(condition)); // possession starts while already inside
        fixture.endPossession(unpossessed);
        assertFalse(control.isActive(condition));
    }

    @Test
    void zeroPlayerAndCreatureSelectorsMatchAnEligiblePossessedCreature() {
        IKwdFile kwd = APPossessedCreatureEntersTriggerFixtures.possessedCreatureEnters(null,
                (short) 0, (short) 23, (short) 0, 0, false);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        ActionPointTriggerControl control = fixture.control();
        TriggerGenericData condition = fixture.condition();
        Occupant creature = fixture.addCreature((short) 2, (short) 23, 0, 1);
        fixture.possess(creature);

        creature.moveTo(1, 1);
        assertTrue(control.isActive(condition));
    }

    @Test
    void possessionRemovalAndReplacementUpdateCurrentPresence() {
        IKwdFile kwd = APPossessedCreatureEntersTriggerFixtures.possessedCreatureEnters(null,
                (short) 1, (short) 0, (short) 3, 0, false);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        ActionPointTriggerControl control = fixture.control();
        TriggerGenericData condition = fixture.condition();
        Occupant creature = fixture.addCreature((short) 1, (short) 7, 0, 1);
        fixture.possess(creature);

        creature.moveTo(1, 1);
        assertTrue(control.isActive(condition));

        fixture.remove(creature);
        assertFalse(control.isActive(condition));
        Occupant replacement = fixture.addCreature((short) 1, (short) 8, 1, 1);
        fixture.possess(replacement);
        assertTrue(control.isActive(condition));

        replacement.moveTo(3, 1);
        assertFalse(control.isActive(condition));
        replacement.moveTo(2, 1);
        assertTrue(control.isActive(condition));
    }

    @Test
    void objectSelectorIsUnsupportedAndComparisonAndValueAreUnusedForType70() {
        IKwdFile unsupportedSelector = APPossessedCreatureEntersTriggerFixtures.possessedCreatureEnters(null,
                (short) 0, (short) 0, (short) 6, 0, false);
        Fixture selectorFixture = new Fixture(unsupportedSelector, ACTION_POINT, 4, 4);
        Occupant creature = selectorFixture.addCreature((short) 1, (short) 7, 0, 1);
        selectorFixture.possess(creature);
        ActionPointTriggerControl selectorControl = selectorFixture.control();
        TriggerGenericData selectorCondition = selectorFixture.condition();
        assertFalse(selectorControl.isActive(selectorCondition));
        creature.moveTo(1, 1);
        assertFalse(selectorControl.isActive(selectorCondition));

        IKwdFile unsupportedComparison = APPossessedCreatureEntersTriggerFixtures.possessedCreatureEnters(
                TriggerGeneric.ComparisonType.EQUAL_TO, (short) 0, (short) 0, (short) 3, 1, false);
        Fixture comparisonFixture = new Fixture(unsupportedComparison, ACTION_POINT, 4, 4);
        ActionPointTriggerControl comparisonControl = comparisonFixture.control();
        TriggerGenericData comparisonCondition = comparisonFixture.condition();
        Occupant comparisonCreature = comparisonFixture.addCreature((short) 1, (short) 7, 0, 1);
        comparisonFixture.possess(comparisonCreature);
        comparisonCreature.moveTo(1, 1);
        assertTrue(comparisonControl.isActive(comparisonCondition));

        IKwdFile otherCreatureSelector = APPossessedCreatureEntersTriggerFixtures.possessedCreatureEnters(null,
                (short) 1, (short) 7, (short) 9, 0, false);
        Fixture otherSelectorFixture = new Fixture(otherCreatureSelector, ACTION_POINT, 4, 4);
        Occupant selectedCreature = otherSelectorFixture.addCreature((short) 1, (short) 7, 1, 1);
        otherSelectorFixture.possess(selectedCreature);
        assertTrue(otherSelectorFixture.isActive()); // executable treats every selector except 6 as a creature

        IKwdFile missingValue = APPossessedCreatureEntersTriggerFixtures.possessedCreatureEnters(null,
                (short) 1, (short) 0, (short) 3, null, false);
        Fixture missingValueFixture = new Fixture(missingValue, ACTION_POINT, 4, 4);
        Occupant anyCreature = missingValueFixture.addCreature((short) 1, (short) 9, 1, 1);
        missingValueFixture.possess(anyCreature);
        assertTrue(missingValueFixture.control().isActive(missingValueFixture.condition()));

        IKwdFile valid = APPossessedCreatureEntersTriggerFixtures.possessedCreatureEnters(null,
                (short) 1, (short) 0, (short) 3, 0, false);
        Fixture outOfMapFixture = new Fixture(valid, actionPoint(4, 4, 4, 4), 4, 4);
        ActionPointTriggerControl outOfMapControl = outOfMapFixture.control();
        assertFalse(outOfMapControl.isActive(outOfMapFixture.condition()));
    }

    @Test
    void missingType70ParametersAndRuntimeServicesFailClosed() {
        for (int missingField = 0; missingField < 3; missingField++) {
            Short playerId = missingField == 0 ? null : (short) 1;
            Short targetId = missingField == 1 ? null : (short) 7;
            Short targetType = missingField == 2 ? null : (short) 3;
            IKwdFile kwd = APPossessedCreatureEntersTriggerFixtures.possessedCreatureEnters(null,
                    playerId, targetId, targetType, 0, false);
            Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
            assertFalse(fixture.control().isActive(fixture.condition()), "missing field " + missingField);
        }

        IKwdFile kwd = APPossessedCreatureEntersTriggerFixtures.possessedCreatureEnters(null,
                (short) 1, (short) 7, (short) 3, 0, false);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        Occupant creature = fixture.addCreature((short) 1, (short) 7, 1, 1);
        fixture.possess(creature);
        TriggerGenericData condition = fixture.condition();

        ActionPointTriggerControl missingCreaturesController = new ActionPointTriggerControl(null, fixture.levelInfo,
                null, fixture.mapController, null, 1, fixture.ap, fixture.spatialLookup.lookup);
        assertFalse(missingCreaturesController.isActive(condition));

        ActionPointTriggerControl withoutPositionLookup = new ActionPointTriggerControl(null, fixture.levelInfo,
                null, fixture.mapController, fixture.creaturesController, 1, fixture.ap, null);
        assertTrue(withoutPositionLookup.isActive(condition)); // the current creature tile is read directly
    }

    @Test
    void repeatedConditionRunsItsChildWhileThePossessedCreatureRemainsInside() {
        IKwdFile kwd = APPossessedCreatureEntersTriggerFixtures.possessedCreatureEnters(null,
                (short) 1, (short) 7, (short) 3, 0, true);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        ActionPointTriggerControl control = fixture.control();
        Occupant creature = fixture.addCreature((short) 1, (short) 7, 1, 1);

        control.update(0.25f); // inside, but not possessed
        assertFalse(control.isActive(fixture.condition()));

        fixture.possess(creature); // possession begins while inside the AP
        control.update(0.25f); // the current-presence predicate becomes true
        control.update(0.25f); // downstream action runs
        assertEquals(1, fixture.flags.get((short) 2));

        control.update(0.25f);
        control.update(0.25f);
        control.update(0.25f);
        assertEquals(2, fixture.flags.get((short) 2)); // repeat=255 keeps evaluating while true

        creature.moveTo(3, 1);
        control.update(0.25f);
        control.update(0.25f);
        assertEquals(2, fixture.flags.get((short) 2)); // outside the AP: condition is false

        creature.moveTo(1, 1);
        control.update(0.25f);
        control.update(0.25f);
        assertEquals(3, fixture.flags.get((short) 2));
    }

    @Test
    void oneShotPossessedCreatureConditionRunsItsChildOnlyOnce() {
        IKwdFile kwd = APPossessedCreatureEntersTriggerFixtures.possessedCreatureEnters(null,
                (short) 1, (short) 7, (short) 3, 0, (short) 1, true);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        ActionPointTriggerControl control = fixture.control();
        Occupant creature = fixture.addCreature((short) 1, (short) 7, 1, 1);

        control.update(0.25f); // inside, but not possessed
        fixture.possess(creature);
        control.update(0.25f);
        control.update(0.25f);
        assertEquals(1, fixture.flags.get((short) 2));

        fixture.endPossession(creature);
        control.update(0.25f);
        control.update(0.25f);
        fixture.possess(creature);
        control.update(0.25f);
        control.update(0.25f);
        assertEquals(1, fixture.flags.get((short) 2));
    }

    @Test
    void countsObjectsByOwnerAndObjectDefinitionId() {
        IKwdFile kwd = APCongregateInTriggerFixtures.congregateIn(TriggerGeneric.ComparisonType.EQUAL_TO,
                (short) 1, (short) 74, (short) 6, 1);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        fixture.addObject((short) 1, (short) 74, 1, 1);
        fixture.addObject((short) 1, (short) 74, 3, 1); // outside
        fixture.addObject((short) 1, (short) 75, 2, 1); // wrong object definition
        fixture.addObject((short) 2, (short) 74, 2, 2); // wrong owner

        assertTrue(fixture.isActive());
    }

    @Test
    void tracksCurrentOccupancyAcrossMovementOwnershipCreationAndRemoval() {
        IKwdFile kwd = APCongregateInTriggerFixtures.congregateIn(TriggerGeneric.ComparisonType.GREATER_OR_EQUAL_TO,
                (short) 1, (short) 7, (short) 3, 1);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        Occupant creature = fixture.addCreature((short) 1, (short) 7, 0, 0);

        assertFalse(fixture.isActive());
        creature.moveTo(2, 2);
        assertTrue(fixture.isActive());
        creature.ownerId = 2;
        assertFalse(fixture.isActive());
        creature.ownerId = 1;
        assertTrue(fixture.isActive());
        fixture.remove(creature);
        assertFalse(fixture.isActive());

        Occupant replacement = fixture.addCreature((short) 1, (short) 7, 1, 2);
        assertTrue(fixture.isActive());
        fixture.remove(replacement);
        assertFalse(fixture.isActive());
    }

    @Test
    void unsupportedSelectorsMissingComparisonsAndOutOfMapAreasFailClosed() {
        IKwdFile unsupported = APCongregateInTriggerFixtures.congregateIn(TriggerGeneric.ComparisonType.EQUAL_TO,
                (short) 0, (short) 0, (short) 5, 0);
        Fixture fixture = new Fixture(unsupported, ACTION_POINT, 4, 4);
        fixture.addCreature((short) 1, (short) 7, 1, 1);
        assertFalse(fixture.isActive());

        IKwdFile noComparison = APCongregateInTriggerFixtures.congregateIn(null,
                (short) 0, (short) 0, (short) 3, 0);
        assertFalse(new Fixture(noComparison, ACTION_POINT, 4, 4).isActive());

        IKwdFile missingCount = APCongregateInTriggerFixtures.congregateIn(TriggerGeneric.ComparisonType.EQUAL_TO,
                (short) 0, (short) 0, (short) 3, null);
        assertFalse(new Fixture(missingCount, ACTION_POINT, 4, 4).isActive());

        IKwdFile outsideMap = APCongregateInTriggerFixtures.congregateIn(TriggerGeneric.ComparisonType.EQUAL_TO,
                (short) 0, (short) 0, (short) 3, 0);
        Fixture outside = new Fixture(outsideMap, actionPoint(4, 4, 4, 4), 4, 4);
        assertDoesNotThrow(() -> assertFalse(outside.isActive()));
        assertTrue(outside.spatialLookup.queriedTiles.isEmpty());
    }

    @Test
    void actionPointRejectsReversedOrUnrepresentableRectangles() {
        Thing.ActionPoint reversed = ActionPointFixtures.rectangle(1, 2, 1, 1, 2, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> new ActionPoint(reversed));

        Thing.ActionPoint tooLarge = ActionPointFixtures.rectangle(1, 0, 0,
                Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> new ActionPoint(tooLarge));
    }

    @Test
    void activatesTheExistingChildActionWhenTheThresholdIsReached() {
        IKwdFile kwd = APCongregateInTriggerFixtures.congregateIn(TriggerGeneric.ComparisonType.GREATER_OR_EQUAL_TO,
                (short) 1, (short) 7, (short) 3, 1, true);
        Fixture fixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        ActionPointTriggerControl control = fixture.control();

        control.update(0.25f);
        control.update(0.25f);
        assertFalse(fixture.flags.containsKey((short) 2));

        fixture.addCreature((short) 1, (short) 7, 1, 1);
        control.update(0.25f);
        control.update(0.25f);
        assertTrue(fixture.flags.get((short) 2) == 1);
    }

    @Test
    void usesThePositionSystemForObjectPresenceMovementOwnershipAndRemoval() {
        IKwdFile kwd = APCongregateInTriggerFixtures.congregateIn(TriggerGeneric.ComparisonType.EQUAL_TO,
                (short) 1, (short) 74, (short) 6, 1);
        Fixture triggerFixture = new Fixture(kwd, ACTION_POINT, 4, 4);
        TriggerGenericData condition = (TriggerGenericData) new TriggerLoader(kwd).load(1).getChild(0);

        try (PositionSystemFixture spatial = new PositionSystemFixture(4, 4)) {
            ActionPointTriggerControl control = new ActionPointTriggerControl(null, triggerFixture.levelInfo,
                    null, spatial.mapController, null, 1, triggerFixture.ap, spatial.positionSystem);
            EntityId object = spatial.addObject((short) 1, (short) 74, 0, 0);
            spatial.tick();
            assertFalse(control.isActive(condition));

            spatial.move(object, 2, 2);
            spatial.tick();
            assertTrue(control.isActive(condition));

            spatial.setOwner(object, (short) 2);
            assertFalse(control.isActive(condition));
            spatial.setOwner(object, (short) 1);
            assertTrue(control.isActive(condition));

            spatial.move(object, 3, 2);
            spatial.tick();
            assertFalse(control.isActive(condition));

            spatial.remove(object);
            spatial.tick();
            assertFalse(control.isActive(condition));
        }
    }

    @Test
    void evaluatesParsedCongregateInRecordsFromRetailCampaignMapsWhenAvailable() throws IOException {
        String gameDirectory = System.getProperty("openkeeper.dk2.game", System.getenv("OPENKEEPER_DK2_GAME"));
        Assumptions.assumeTrue(gameDirectory != null && !gameDirectory.isBlank(),
                "Set OPENKEEPER_DK2_GAME to the retail DK2 install directory to run this integration test");

        Map<String, Integer> expectedRecords = Map.of("Level15a", 14, "Level20", 8, "Secret1", 3);
        for (Map.Entry<String, Integer> mapEntry : expectedRecords.entrySet()) {
            Path mapFile = Path.of(gameDirectory, "Data", "Editor", "Maps", mapEntry.getKey() + ".kwd");
            Assumptions.assumeTrue(Files.isRegularFile(mapFile), "Missing stock map " + mapFile);
            IKwdFile kwd = KwdFile.load(gameDirectory, mapFile);
            int width = kwd.getMap().getWidth();
            int height = kwd.getMap().getHeight();
            int recordCount = 0;

            for (Thing.ActionPoint thing : kwd.getThings(Thing.ActionPoint.class)) {
                if (thing.getTriggerId() == 0) {
                    continue;
                }
                ActionPoint ap = new ActionPoint(thing);
                TriggerGenericData tree = new TriggerLoader(kwd).load(thing.getTriggerId());
                List<TriggerGenericData> congregateTriggers = new ArrayList<>();
                collectCongregateTriggers(tree, congregateTriggers);

                for (TriggerGenericData trigger : congregateTriggers) {
                    recordCount++;
                    short targetType = trigger.getUserData("targetType", short.class);
                    assertTrue(targetType == 0 || targetType == 3 || targetType == 6,
                            mapEntry.getKey() + " trigger " + trigger.getId() + " selector " + targetType);
                    int matchingCount = countThatSatisfies(trigger);
                    assertTrue(matchingCount <= 64, "Unexpectedly large stock trigger count in " + mapEntry.getKey());

                    IKwdFile levelData = kwd;
                    Fixture fixture = new Fixture(levelData, ap, width, height);
                    Point location = ap.getStart();
                    short playerId = trigger.getUserData("playerId", short.class);
                    short targetId = trigger.getUserData("targetId", short.class);
                    for (int i = 0; i < matchingCount; i++) {
                        short owner = playerId == 0 ? 1 : playerId;
                        if (targetType == 6) {
                            fixture.addObject(owner, targetId == 0 ? (short) 74 : targetId, location.x, location.y);
                        } else {
                            fixture.addCreature(owner, targetId == 0 ? (short) 7 : targetId, location.x, location.y);
                        }
                    }

                    ActionPointTriggerControl control = fixture.control(thing.getTriggerId());
                    assertTrue(control.isActive(trigger), mapEntry.getKey() + " trigger " + trigger.getId());
                }
            }

            assertEquals(mapEntry.getValue(), recordCount, "AP_CONGREGATE_IN records in " + mapEntry.getKey());
        }
    }

    @Test
    void evaluatesEveryPossessedCreatureRecordFromRetailCampaignMapsWhenAvailable() throws IOException {
        String gameDirectory = System.getProperty("openkeeper.dk2.game", System.getenv("OPENKEEPER_DK2_GAME"));
        Assumptions.assumeTrue(gameDirectory != null && !gameDirectory.isBlank(),
                "Set OPENKEEPER_DK2_GAME to the retail DK2 install directory to run this integration test");

        Map<String, Map<Integer, Integer>> expectedRecords = Map.of(
                "Level15a", Map.of(74, 51),
                "level15b", Map.of(7, 11),
                "level4", Map.of(79, 24),
                "Secret3", Map.of(13, 12, 14, 14, 17, 15));
        int totalRecords = 0;
        for (Map.Entry<String, Map<Integer, Integer>> mapEntry : expectedRecords.entrySet()) {
            Path mapFile = Path.of(gameDirectory, "Data", "Editor", "Maps", mapEntry.getKey() + ".kwd");
            Assumptions.assumeTrue(Files.isRegularFile(mapFile), "Missing stock map " + mapFile);
            IKwdFile kwd = KwdFile.load(gameDirectory, mapFile);
            int width = kwd.getMap().getWidth();
            int height = kwd.getMap().getHeight();
            Set<String> foundRecords = new HashSet<>();

            for (Thing.ActionPoint thing : kwd.getThings(Thing.ActionPoint.class)) {
                if (thing.getTriggerId() == 0) {
                    continue;
                }
                ActionPoint ap = new ActionPoint(thing);
                TriggerGenericData tree = new TriggerLoader(kwd).load(thing.getTriggerId());
                List<TriggerGenericData> possessedCreatureTriggers = new ArrayList<>();
                collectPossessedCreatureTriggers(tree, possessedCreatureTriggers);

                for (TriggerGenericData trigger : possessedCreatureTriggers) {
                    totalRecords++;
                    foundRecords.add(trigger.getId() + ":" + thing.getId());
                    short playerId = trigger.getUserData("playerId", short.class);
                    short targetId = trigger.getUserData("targetId", short.class);
                    short targetType = trigger.getUserData("targetType", short.class);
                    assertTrue(targetType == 0 || targetType == 3,
                            mapEntry.getKey() + " trigger " + trigger.getId() + " targetType " + targetType);
                    assertEquals(3, playerId, mapEntry.getKey() + " trigger " + trigger.getId() + " player");
                    assertTrue(trigger.getComparison() == null || trigger.getComparison() == TriggerGeneric.ComparisonType.NONE,
                            mapEntry.getKey() + " trigger " + trigger.getId() + " comparison " + trigger.getComparison());
                    assertEquals(0, trigger.getUserData("value", int.class),
                            mapEntry.getKey() + " trigger " + trigger.getId() + " event value");

                    Fixture fixture = new Fixture(kwd, ap, width, height);
                    ActionPointTriggerControl control = fixture.control(thing.getTriggerId());
                    Occupant creature = fixture.addCreature(playerId, targetId == 0 ? (short) 7 : targetId,
                            ap.getStart().x, ap.getStart().y);
                    assertFalse(control.isActive(trigger), mapEntry.getKey() + " trigger " + trigger.getId() + " unpossessed");

                    fixture.possess(creature);
                    assertTrue(control.isActive(trigger), mapEntry.getKey() + " trigger " + trigger.getId() + " possession while inside");
                    assertTrue(control.isActive(trigger), mapEntry.getKey() + " trigger " + trigger.getId() + " continuous presence");

                    int outsideX = ap.getEnd().x + 1 < width ? ap.getEnd().x + 1 : ap.getStart().x - 1;
                    assertTrue(outsideX >= 0 && outsideX < width, "No outside test tile for " + mapEntry.getKey());
                    creature.moveTo(outsideX, ap.getStart().y);
                    assertFalse(control.isActive(trigger), mapEntry.getKey() + " trigger " + trigger.getId() + " outside");

                    creature.moveTo(ap.getStart().x, ap.getStart().y);
                    assertTrue(control.isActive(trigger), mapEntry.getKey() + " trigger " + trigger.getId() + " returned inside");
                    fixture.endPossession(creature);
                    assertFalse(control.isActive(trigger), mapEntry.getKey() + " trigger " + trigger.getId() + " possession ended");
                }
            }

            assertEquals(mapEntry.getValue().size(), foundRecords.size(),
                    "AP_POSESSED_CREATURE_ENTERS records in " + mapEntry.getKey());
            Set<String> expectedKeys = new HashSet<>();
            mapEntry.getValue().forEach((triggerId, actionPointId) -> expectedKeys.add(triggerId + ":" + actionPointId));
            assertEquals(expectedKeys, foundRecords, "Stock trigger/action-point references in " + mapEntry.getKey());
        }

        assertEquals(6, totalRecords, "Total retail AP_POSESSED_CREATURE_ENTERS records");
    }

    @Test
    void evaluatesAllFiveTagAllOfRecordsFromRetailCampaignMapsWhenAvailable() throws IOException {
        String gameDirectory = System.getProperty("openkeeper.dk2.game", System.getenv("OPENKEEPER_DK2_GAME"));
        Assumptions.assumeTrue(gameDirectory != null && !gameDirectory.isBlank(),
                "Set OPENKEEPER_DK2_GAME to the retail DK2 install directory to run this integration test");

        Map<String, Map<Integer, Integer>> expectedRecords = Map.of(
                "level1", Map.of(259, 20, 155, 21),
                "level2", Map.of(84, 3, 132, 5, 171, 6));
        Map<String, Map<Integer, Integer>> expectedTaggableTiles = Map.of(
                "level1", Map.of(20, 15, 21, 9),
                "level2", Map.of(3, 1, 5, 15, 6, 9));
        int totalRecords = 0;

        for (Map.Entry<String, Map<Integer, Integer>> mapEntry : expectedRecords.entrySet()) {
            Path mapFile = Path.of(gameDirectory, "Data", "Editor", "Maps", mapEntry.getKey() + ".kwd");
            Assumptions.assumeTrue(Files.isRegularFile(mapFile), "Missing stock map " + mapFile);
            IKwdFile kwd = KwdFile.load(gameDirectory, mapFile);
            int width = kwd.getMap().getWidth();
            int height = kwd.getMap().getHeight();
            Map<Integer, Integer> foundRecords = new HashMap<>();

            for (Thing.ActionPoint thing : kwd.getThings(Thing.ActionPoint.class)) {
                if (thing.getTriggerId() == 0) {
                    continue;
                }
                ActionPoint ap = new ActionPoint(thing);
                TriggerGenericData tree = new TriggerLoader(kwd).load(thing.getTriggerId());
                List<TriggerGenericData> tagAllOfTriggers = new ArrayList<>();
                collectTagAllOfTriggers(tree, tagAllOfTriggers);

                for (TriggerGenericData trigger : tagAllOfTriggers) {
                    totalRecords++;
                    foundRecords.put(trigger.getId(), (int) thing.getId());
                    assertEquals((short) 3, trigger.getUserData("playerId", short.class).shortValue(),
                            mapEntry.getKey() + " trigger " + trigger.getId() + " player");

                    Fixture fixture = new Fixture(kwd, ap, width, height);
                    fixture.copyTerrainFrom(kwd);
                    int taggableCount = 0;
                    int firstTaggableX = -1;
                    int firstTaggableY = -1;
                    for (int x = (int) ap.getStart().x; x <= (int) ap.getEnd().x; x++) {
                        for (int y = (int) ap.getStart().y; y <= (int) ap.getEnd().y; y++) {
                            var tile = kwd.getMap().getTile(x, y);
                            Terrain terrain = kwd.getTerrain(tile.getTerrainId());
                            assertTrue(terrain != null, mapEntry.getKey() + " missing terrain at " + x + "," + y);
                            if (terrain.getFlags().contains(Terrain.TerrainFlag.TAGGABLE)) {
                                taggableCount++;
                                if (firstTaggableX < 0) {
                                    firstTaggableX = x;
                                    firstTaggableY = y;
                                }
                                fixture.setTagged(x, y, (short) 3, true);
                            }
                        }
                    }
                    assertEquals(expectedTaggableTiles.get(mapEntry.getKey()).get((int) thing.getId()), taggableCount,
                            mapEntry.getKey() + " action point " + thing.getId() + " taggable terrain count");

                    ActionPointTriggerControl control = fixture.control(thing.getTriggerId());
                    assertTrue(control.isActive(trigger), mapEntry.getKey() + " trigger " + trigger.getId()
                            + " with all player tasks present");
                    fixture.setTagged(firstTaggableX, firstTaggableY, (short) 3, false);
                    assertFalse(control.isActive(trigger), mapEntry.getKey() + " trigger " + trigger.getId()
                            + " after removing one task");
                }
            }

            assertEquals(mapEntry.getValue(), foundRecords,
                    "Stock AP_TAG_ALL_OF trigger/action-point references in " + mapEntry.getKey());
        }

        assertEquals(5, totalRecords, "Total retail AP_TAG_ALL_OF records");
    }

    private static void collectPossessedCreatureTriggers(TriggerGenericData trigger, List<TriggerGenericData> result) {
        if (trigger.getType() == TriggerGeneric.TargetType.AP_POSESSED_CREATURE_ENTERS) {
            result.add(trigger);
        }
        for (var child : trigger.getChildren()) {
            if (child instanceof TriggerGenericData generic) {
                collectPossessedCreatureTriggers(generic, result);
            }
        }
    }

    private static void collectCongregateTriggers(TriggerGenericData trigger, List<TriggerGenericData> result) {
        if (trigger.getType() == TriggerGeneric.TargetType.AP_CONGREGATE_IN) {
            result.add(trigger);
        }
        for (var child : trigger.getChildren()) {
            if (child instanceof TriggerGenericData generic) {
                collectCongregateTriggers(generic, result);
            }
        }
    }

    private static void collectTagAllOfTriggers(TriggerGenericData trigger, List<TriggerGenericData> result) {
        if (trigger.getType() == TriggerGeneric.TargetType.AP_TAG_ALL_OF) {
            result.add(trigger);
        }
        for (var child : trigger.getChildren()) {
            if (child instanceof TriggerGenericData generic) {
                collectTagAllOfTriggers(generic, result);
            }
        }
    }

    private static int countThatSatisfies(TriggerGenericData trigger) {
        int threshold = trigger.getUserData("value", int.class);
        return switch (trigger.getComparison()) {
            case LESS_THAN -> threshold - 1;
            case LESS_OR_EQUAL_TO, EQUAL_TO, GREATER_OR_EQUAL_TO -> threshold;
            case GREATER_THAN -> threshold + 1;
            case NOT_EQUAL_TO -> threshold == 0 ? 1 : 0;
            case NONE -> throw new AssertionError("Stock AP_CONGREGATE_IN record has no comparison");
        };
    }

    private static ActionPoint actionPoint(int startX, int startY, int endX, int endY) {
        return new ActionPoint(ActionPointFixtures.rectangle(1, startX, startY, endX, endY, 0, 0));
    }

    private static final class Fixture {

        private final IKwdFile kwd;
        private final ActionPoint ap;
        private final int width;
        private final int height;
        private final MapTileState[][] mapTiles;
        private final SpatialLookup spatialLookup = new SpatialLookup();
        private final Map<Short, Integer> flags = new HashMap<>();
        private final Map<Short, EntityId> possessedCreatures = new HashMap<>();
        private final Map<EntityId, ICreatureController> creatureControllers = new HashMap<>();
        private final IMapController mapController;
        private final ILevelInfo levelInfo;
        private long nextEntityId = 1;
        private final ICreaturesController creaturesController = (ICreaturesController) Proxy.newProxyInstance(
                ICreaturesController.class.getClassLoader(), new Class<?>[]{ICreaturesController.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getPossessedCreature" -> possessedCreatures.get(((Number) args[0]).shortValue());
                    case "createController" -> creatureControllers.get(args[0]);
                    case "isValidEntity" -> creatureControllers.containsKey(args[0]);
                    default -> defaultValue(method.getReturnType());
                });
        private final Map<Short, Keeper> players = Map.of(
                (short) 1, new Keeper(false, (short) 1),
                (short) 2, new Keeper(false, (short) 2),
                (short) 3, new Keeper(false, (short) 3));

        private Fixture(IKwdFile kwd, ActionPoint ap, int width, int height) {
            this.kwd = kwd;
            this.ap = ap;
            this.width = width;
            this.height = height;
            mapTiles = new MapTileState[width][height];

            IMapDataInformation mapData = (IMapDataInformation) Proxy.newProxyInstance(
                    IMapDataInformation.class.getClassLoader(), new Class<?>[]{IMapDataInformation.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getWidth" -> width;
                        case "getHeight" -> height;
                        case "getTile" -> mapTile(((Number) args[0]).intValue(), ((Number) args[1]).intValue());
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                    new Class<?>[]{IMapController.class}, (proxy, method, args) -> {
                        return switch (method.getName()) {
                            case "getMapData" -> mapData;
                            case "getTerrain" -> kwd.getTerrain(
                                    ((IMapTileInformation) args[0]).getTerrainId());
                            default -> throw new UnsupportedOperationException(method.getName());
                        };
                    });
            levelInfo = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                    new Class<?>[]{ILevelInfo.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getLevelData" -> kwd;
                        case "getPlayers" -> players;
                        case "getFlag" -> flags.getOrDefault(((Number) args[0]).shortValue(), 0);
                        case "setFlag" -> {
                            flags.put(((Number) args[0]).shortValue(), ((Number) args[1]).intValue());
                            yield null;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private IMapTileInformation mapTile(int x, int y) {
            if (x < 0 || y < 0 || x >= width || y >= height) {
                return null;
            }
            MapTileState state = mapTiles[x][y];
            if (state == null) {
                state = new MapTileState(x, y);
                mapTiles[x][y] = state;
            }
            return state.tile;
        }

        private MapTileState mapTileState(int x, int y) {
            mapTile(x, y);
            return mapTiles[x][y];
        }

        private void setTerrain(int x, int y, short terrainId) {
            mapTileState(x, y).terrainId = terrainId;
        }

        private void setTagged(int x, int y, short playerId, boolean tagged) {
            mapTileState(x, y).selected.put(playerId, tagged);
        }

        private void copyTerrainFrom(IKwdFile source) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    var sourceTile = source.getMap().getTile(x, y);
                    if (sourceTile != null) {
                        setTerrain(x, y, sourceTile.getTerrainId());
                    }
                }
            }
        }

        private Occupant addCreature(short ownerId, short creatureId, int x, int y) {
            Occupant occupant = new Occupant(new EntityId(nextEntityId++), ownerId, creatureId, x, y);
            Creature definition = APCongregateInTriggerFixtures.creatureDefinition(creatureId);
            occupant.controller = Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                    new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getEntityId" -> occupant.entityId;
                        case "getOwnerId" -> occupant.ownerId;
                        case "getCreature" -> definition;
                        case "getPosition" -> WorldUtils.pointToVector3f(occupant.x, occupant.y);
                        case "getTile" -> SpatialLookup.tile(occupant);
                        default -> defaultValue(method.getReturnType());
                    });
            creatureControllers.put(occupant.entityId, (ICreatureController) occupant.controller);
            spatialLookup.occupants.add(occupant);
            return occupant;
        }

        private Occupant addObject(short ownerId, short objectId, int x, int y) {
            Occupant occupant = new Occupant(new EntityId(nextEntityId++), ownerId, objectId, x, y);
            occupant.controller = Proxy.newProxyInstance(IObjectController.class.getClassLoader(),
                    new Class<?>[]{IObjectController.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getOwnerId" -> occupant.ownerId;
                        case "getObjectId" -> occupant.targetId;
                        default -> defaultValue(method.getReturnType());
                    });
            spatialLookup.occupants.add(occupant);
            return occupant;
        }

        private void remove(Occupant occupant) {
            spatialLookup.occupants.remove(occupant);
            creatureControllers.remove(occupant.entityId);
            possessedCreatures.values().removeIf(occupant.entityId::equals);
        }

        private void possess(Occupant creature) {
            possessedCreatures.values().removeIf(creature.entityId::equals);
            possessedCreatures.put(creature.ownerId, creature.entityId);
        }

        private void endPossession(Occupant creature) {
            possessedCreatures.remove(creature.ownerId, creature.entityId);
        }

        private void changeOwner(Occupant creature, short ownerId) {
            boolean possessed = possessedCreatures.values().removeIf(creature.entityId::equals);
            creature.ownerId = ownerId;
            if (possessed) {
                possessedCreatures.put(ownerId, creature.entityId);
            }
        }

        private ActionPointTriggerControl control() {
            return control(1);
        }

        private ActionPointTriggerControl control(int triggerId) {
            return new ActionPointTriggerControl(null, levelInfo, null, mapController,
                    creaturesController, triggerId, ap, spatialLookup.lookup);
        }

        private TriggerGenericData condition() {
            return (TriggerGenericData) new TriggerLoader(kwd).load(1).getChild(0);
        }

        private boolean isActive() {
            ActionPointTriggerControl control = control();
            TriggerGenericData root = new TriggerLoader(kwd).load(1);
            return control.isActive((TriggerGenericData) root.getChild(0));
        }
    }

    private static final class MapTileState {

        private final Map<Short, Boolean> selected = new HashMap<>();
        private final IMapTileInformation tile;
        private short terrainId;

        private MapTileState(int x, int y) {
            tile = (IMapTileInformation) Proxy.newProxyInstance(
                    IMapTileInformation.class.getClassLoader(), new Class<?>[]{IMapTileInformation.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getX" -> x;
                        case "getY" -> y;
                        case "getLocation" -> new Point(x, y);
                        case "getTerrainId" -> terrainId;
                        case "isSelected" -> selected.getOrDefault(((Number) args[0]).shortValue(), false);
                        default -> defaultValue(method.getReturnType());
                    });
        }
    }

    private static final class SpatialLookup {

        private final List<Occupant> occupants = new ArrayList<>();
        private final List<String> queriedTiles = new ArrayList<>();
        private final IEntityPositionLookup lookup = (IEntityPositionLookup) Proxy.newProxyInstance(
                IEntityPositionLookup.class.getClassLoader(), new Class<?>[]{IEntityPositionLookup.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getEntityLocation")) {
                        EntityId entityId = (EntityId) args[0];
                        for (Occupant occupant : occupants) {
                            if (occupant.entityId.equals(entityId)) {
                                return tile(occupant);
                            }
                        }
                        return null;
                    }
                    if (!method.getName().equals("getEntityTypesInLocation")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    int x = (Integer) args[0];
                    int y = (Integer) args[1];
                    queriedTiles.add(x + "," + y);
                    Class<?> type = (Class<?>) args[2];
                    List<Object> result = new ArrayList<>();
                    for (Occupant occupant : occupants) {
                        if (occupant.x == x && occupant.y == y && type.isInstance(occupant.controller)) {
                            result.add(occupant.controller);
                        }
                    }
                    return result;
                });

        private static IMapTileInformation tile(Occupant occupant) {
            return (IMapTileInformation) Proxy.newProxyInstance(IMapTileInformation.class.getClassLoader(),
                    new Class<?>[]{IMapTileInformation.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getX" -> occupant.x;
                        case "getY" -> occupant.y;
                        default -> defaultValue(method.getReturnType());
                    });
        }
    }

    private static final class PositionSystemFixture implements AutoCloseable {

        private final DefaultEntityData entityData = new DefaultEntityData();
        private final IMapController mapController;
        private final PositionSystem positionSystem;
        private final Map<EntityId, IObjectController> objectControllers = new HashMap<>();
        private final IObjectsController objectsController;

        private PositionSystemFixture(int width, int height) {
            IMapTileInformation[][] tiles = new IMapTileInformation[width][height];
            IMapDataInformation<?> mapData = (IMapDataInformation<?>) Proxy.newProxyInstance(
                    IMapDataInformation.class.getClassLoader(), new Class<?>[]{IMapDataInformation.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getWidth" -> width;
                        case "getHeight" -> height;
                        case "getTile" -> args.length == 1
                                ? tile(tiles, ((Point) args[0]).x, ((Point) args[0]).y)
                                : tile(tiles, (Integer) args[0], (Integer) args[1]);
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                    new Class<?>[]{IMapController.class}, (proxy, method, args) -> {
                        if (method.getName().equals("getMapData")) {
                            return mapData;
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
            objectsController = (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(),
                    new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "isValidEntity" -> objectControllers.containsKey(args[0]);
                        case "createController" -> objectControllers.get(args[0]);
                        default -> defaultValue(method.getReturnType());
                    });
            positionSystem = new PositionSystem(mapController, entityData, null, null, objectsController);
        }

        private EntityId addObject(short ownerId, short objectId, int x, int y) {
            EntityId entityId = entityData.createEntity();
            IObjectController controller = (IObjectController) Proxy.newProxyInstance(
                    IObjectController.class.getClassLoader(), new Class<?>[]{IObjectController.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getOwnerId" -> entityData.getComponent(entityId, Owner.class).ownerId;
                        case "getObjectId" -> entityData.getComponent(entityId, ObjectComponent.class).objectId;
                        default -> defaultValue(method.getReturnType());
                    });
            objectControllers.put(entityId, controller);
            entityData.setComponents(entityId, new Position(0, WorldUtils.pointToVector3f(x, y)),
                    new Owner(ownerId, ownerId), new ObjectComponent(objectId, null));
            return entityId;
        }

        private void move(EntityId entityId, int x, int y) {
            entityData.setComponent(entityId, new Position(0, WorldUtils.pointToVector3f(x, y)));
        }

        private void setOwner(EntityId entityId, short ownerId) {
            entityData.setComponent(entityId, new Owner(ownerId, ownerId));
        }

        private void remove(EntityId entityId) {
            entityData.removeEntity(entityId);
            objectControllers.remove(entityId);
        }

        private void tick() {
            positionSystem.processTick(0.25f);
        }

        private static IMapTileInformation tile(IMapTileInformation[][] tiles, int x, int y) {
            if (x < 0 || y < 0 || x >= tiles.length || y >= tiles[x].length) {
                return null;
            }
            if (tiles[x][y] == null) {
                tiles[x][y] = (IMapTileInformation) Proxy.newProxyInstance(
                        IMapTileInformation.class.getClassLoader(), new Class<?>[]{IMapTileInformation.class},
                        (proxy, method, args) -> switch (method.getName()) {
                            case "getX" -> x;
                            case "getY" -> y;
                            default -> defaultValue(method.getReturnType());
                        });
            }
            return tiles[x][y];
        }

        @Override
        public void close() {
            positionSystem.stop();
            entityData.close();
        }
    }

    private static final class Occupant {

        private final EntityId entityId;
        private short ownerId;
        private final short targetId;
        private int x;
        private int y;
        private Object controller;

        private Occupant(EntityId entityId, short ownerId, short targetId, int x, int y) {
            this.entityId = entityId;
            this.ownerId = ownerId;
            this.targetId = targetId;
            this.x = x;
            this.y = y;
        }

        private void moveTo(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == char.class) {
            return '\0';
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0f;
        }
        if (returnType == double.class) {
            return 0d;
        }
        return null;
    }
}
