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
import java.util.HashMap;
import java.util.Map;

/**
 * Creates KWD trigger model fixtures for action-point trigger tests.
 */
public final class APCongregateInTriggerFixtures {

    private APCongregateInTriggerFixtures() {
    }

    public static Creature creatureDefinition(short id) {
        Creature creature = new Creature();
        creature.setCreatureId(id);
        return creature;
    }

    public static IKwdFile congregateIn(TriggerGeneric.ComparisonType comparison,
            short playerId, short targetId, short targetType, Integer value) {
        return congregateIn(comparison, playerId, targetId, targetType, value, false);
    }

    public static IKwdFile congregateIn(TriggerGeneric.ComparisonType comparison,
            short playerId, short targetId, short targetType, Integer value, boolean setFlagChild) {
        TriggerGeneric condition = new TriggerGeneric(null);
        condition.setId(1);
        condition.setRepeatTimes((short) 255);
        condition.setType(TriggerGeneric.TargetType.AP_CONGREGATE_IN);
        condition.setTargetValueComparison(comparison);
        condition.setUserData("playerId", playerId);
        condition.setUserData("targetId", targetId);
        condition.setUserData("targetType", targetType);
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

        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getTriggers")) {
                        return triggers;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
