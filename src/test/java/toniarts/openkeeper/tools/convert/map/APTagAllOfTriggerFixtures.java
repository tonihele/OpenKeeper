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
package toniarts.openkeeper.tools.convert.map;

import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates type-69 trigger and terrain fixtures for action-point trigger tests.
 */
public final class APTagAllOfTriggerFixtures {

    public static final short TAGGABLE_TERRAIN_ID = 1;
    public static final short UNTAGGABLE_TERRAIN_ID = 2;

    private APTagAllOfTriggerFixtures() {
    }

    public static IKwdFile tagAllOf(short playerId, TriggerGeneric.ComparisonType comparison, int value) {
        return tagAllOf(playerId, comparison, value, false);
    }

    public static IKwdFile tagAllOf(short playerId, TriggerGeneric.ComparisonType comparison, int value,
            boolean setFlagChild) {
        return create(playerId, comparison, value, setFlagChild);
    }

    public static IKwdFile tagAllOfWithoutPlayerId(TriggerGeneric.ComparisonType comparison, int value) {
        return create(null, comparison, value, false);
    }

    public static Terrain terrain(boolean taggable) {
        Terrain terrain = new Terrain();
        terrain.setFlags(taggable
                ? EnumSet.of(Terrain.TerrainFlag.TAGGABLE)
                : EnumSet.noneOf(Terrain.TerrainFlag.class));
        return terrain;
    }

    private static IKwdFile create(Short playerId, TriggerGeneric.ComparisonType comparison, int value,
            boolean setFlagChild) {
        TriggerGeneric condition = new TriggerGeneric(null);
        condition.setId(1);
        condition.setRepeatTimes((short) 255);
        condition.setType(TriggerGeneric.TargetType.AP_TAG_ALL_OF);
        condition.setTargetValueComparison(comparison);
        if (playerId != null) {
            condition.setUserData("playerId", playerId);
        }
        condition.setUserData("value", value);

        Map<Integer, Trigger> triggers = new HashMap<>();
        triggers.put(condition.getId(), condition);
        if (setFlagChild) {
            condition.setIdChild(2);

            TriggerAction setFlag = new TriggerAction(null);
            setFlag.setId(2);
            setFlag.setType(TriggerAction.ActionType.FLAG);
            setFlag.setUserData("flagId", (short) 2);
            // VALUE | EQUAL; avoid resolving the right-hand side as a flag.
            setFlag.setUserData("flag", (short) 10);
            setFlag.setUserData("value", 1);
            triggers.put(setFlag.getId(), setFlag);
        }

        Map<Short, Terrain> terrains = Map.of(
                TAGGABLE_TERRAIN_ID, terrain(true),
                UNTAGGABLE_TERRAIN_ID, terrain(false));
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getTriggers" -> triggers;
                    case "getTerrain" -> terrains.get(((Number) args[0]).shortValue());
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
