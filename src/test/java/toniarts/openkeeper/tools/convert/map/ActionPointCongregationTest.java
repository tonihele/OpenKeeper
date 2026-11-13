/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper. If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.tools.convert.map;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.trigger.actionpoint.ActionPointTriggerControl;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric.ComparisonType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises a synthetic map condition and its child flag action through the trigger loop. */
class ActionPointCongregationTest {

    @ParameterizedTest
    @CsvSource({
        "LESS_THAN,1,false", "LESS_THAN,2,false", "LESS_THAN,3,true",
        "LESS_OR_EQUAL_TO,1,false", "LESS_OR_EQUAL_TO,2,true", "LESS_OR_EQUAL_TO,3,true",
        "EQUAL_TO,1,false", "EQUAL_TO,2,true", "EQUAL_TO,3,false",
        "GREATER_THAN,1,true", "GREATER_THAN,2,false", "GREATER_THAN,3,false",
        "GREATER_OR_EQUAL_TO,1,true", "GREATER_OR_EQUAL_TO,2,true", "GREATER_OR_EQUAL_TO,3,false",
        "NOT_EQUAL_TO,1,true", "NOT_EQUAL_TO,2,false", "NOT_EQUAL_TO,3,true",
        "NONE,2,false"
    })
    void comparesCreatureCount(ComparisonType comparison, int threshold, boolean expected) {
        assertEquals(expected, fires(3, 0, 0, comparison, threshold,
                Map.of(new Point(2, 3), List.of(creature(1, 4), creature(2, 5)))));
    }

    @ParameterizedTest
    @CsvSource({"0,0,4", "1,0,2", "0,4,2", "1,4,1", "3,0,0", "0,9,0"})
    void filtersOwnerAndCreatureTypeWithZeroAsWildcard(int playerId, int creatureId, int expectedCount) {
        assertTrue(fires(3, playerId, creatureId, ComparisonType.EQUAL_TO, expectedCount,
                Map.of(new Point(2, 3), List.of(
                        creature(1, 4), creature(1, 5), creature(2, 4), creature(2, 5)))));
    }

    @ParameterizedTest
    @CsvSource({"0", "3"})
    void includesEveryBoundaryTileAndExcludesCreaturesOutside(int targetType) {
        assertTrue(fires(targetType, 0, 0, ComparisonType.EQUAL_TO, 4, Map.of(
                new Point(2, 3), List.of(creature(1, 4)),
                new Point(2, 4), List.of(creature(1, 4)),
                new Point(3, 3), List.of(creature(1, 4)),
                new Point(3, 4), List.of(creature(1, 4)),
                new Point(1, 3), List.of(creature(1, 4)),
                new Point(4, 3), List.of(creature(1, 4)),
                new Point(2, 2), List.of(creature(1, 4)),
                new Point(2, 5), List.of(creature(1, 4)))));
    }

    @Test
    void emptyAreaCanSatisfyZeroButNotPositiveCount() {
        assertTrue(fires(3, 0, 0, ComparisonType.EQUAL_TO, 0, Map.of()));
        assertFalse(fires(3, 0, 0, ComparisonType.GREATER_THAN, 0, Map.of()));
    }

    @ParameterizedTest
    @CsvSource({"6", "99"})
    void unsupportedTargetsDoNotSatisfyZeroCount(int targetType) {
        assertFalse(fires(targetType, 0, 0, ComparisonType.EQUAL_TO, 0, Map.of()));
    }

    private static boolean fires(int targetType, int playerId, int creatureId,
            ComparisonType comparison, int threshold, Map<Point, List<ICreatureController>> occupants) {
        TriggerGeneric condition = new TriggerGeneric(null);
        condition.setId(1);
        condition.setType(TriggerGeneric.TargetType.AP_CONGREGATE_IN);
        condition.setTargetValueComparison(comparison);
        condition.setRepeatTimes((short) 255);
        condition.setIdChild(2);
        condition.setUserData("targetType", (short) targetType);
        condition.setUserData("playerId", (short) playerId);
        condition.setUserData("targetId", (short) creatureId);
        condition.setUserData("value", threshold);

        TriggerAction action = new TriggerAction(null);
        action.setId(2);
        action.setType(TriggerAction.ActionType.FLAG);
        action.setUserData("flagId", (short) 0);
        action.setUserData("flag", (short) (TriggerAction.FlagTargetValueActionType.VALUE.getFlagValue()
                | TriggerAction.FlagTargetValueActionType.EQUAL.getFlagValue()));
        action.setUserData("value", 1);

        IKwdFile level = proxy(IKwdFile.class, (p, m, a) -> switch (m.getName()) {
            case "getTriggers" -> Map.of(1, condition, 2, action);
            default -> throw new UnsupportedOperationException(m.getName());
        });
        AtomicInteger flag = new AtomicInteger();
        ILevelInfo levelInfo = proxy(ILevelInfo.class, (p, m, a) -> switch (m.getName()) {
            case "getLevelData" -> level;
            case "getFlag" -> flag.get();
            case "setFlag" -> { flag.set((int) a[1]); yield null; }
            default -> throw new UnsupportedOperationException(m.getName());
        });
        IEntityPositionLookup lookup = proxy(IEntityPositionLookup.class, (p, m, a) -> switch (m.getName()) {
            case "getEntityTypesInLocation" -> {
                assertEquals(ICreatureController.class, a[2]);
                yield occupants.getOrDefault(new Point((int) a[0], (int) a[1]), List.of());
            }
            default -> throw new UnsupportedOperationException(m.getName());
        });
        Thing.ActionPoint area = new Thing.ActionPoint();
        area.setStartX(2);
        area.setStartY(3);
        area.setEndX(3);
        area.setEndY(4);
        var control = new ActionPointTriggerControl(null, levelInfo, null, null, null,
                1, new ActionPoint(area), lookup);
        control.update(0.1f); // Evaluate the condition.
        control.update(0.1f); // Execute its child action if active.
        return flag.get() == 1;
    }

    private static ICreatureController creature(int ownerId, int creatureId) {
        Creature creature = new Creature();
        creature.setCreatureId((short) creatureId);
        return proxy(ICreatureController.class, (p, m, a) -> switch (m.getName()) {
            case "getOwnerId" -> (short) ownerId;
            case "getCreature" -> creature;
            default -> throw new UnsupportedOperationException(m.getName());
        });
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
