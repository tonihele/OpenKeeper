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

/** Creates KWD trigger model fixtures for possessed-creature action-point tests. */
public final class APPossessedCreatureEntersTriggerFixtures {

    private APPossessedCreatureEntersTriggerFixtures() {
    }

    public static IKwdFile possessedCreatureEnters(TriggerGeneric.ComparisonType comparison,
            Short playerId, Short targetId, Short targetType, Integer value, boolean incrementFlagChild) {
        return possessedCreatureEnters(comparison, playerId, targetId, targetType, value, (short) 255, incrementFlagChild);
    }

    public static IKwdFile possessedCreatureEnters(TriggerGeneric.ComparisonType comparison,
            Short playerId, Short targetId, Short targetType, Integer value, short repeatTimes, boolean incrementFlagChild) {
        TriggerGeneric condition = new TriggerGeneric(null);
        condition.setId(1);
        condition.setRepeatTimes(repeatTimes);
        condition.setType(TriggerGeneric.TargetType.AP_POSESSED_CREATURE_ENTERS);
        condition.setTargetValueComparison(comparison);
        condition.setUserData("playerId", playerId);
        condition.setUserData("targetId", targetId);
        condition.setUserData("targetType", targetType);
        condition.setUserData("value", value);

        Map<Integer, Trigger> triggers = new HashMap<>();
        triggers.put(condition.getId(), condition);
        if (incrementFlagChild) {
            condition.setIdChild(2);

            TriggerAction incrementFlag = new TriggerAction(null);
            incrementFlag.setId(2);
            incrementFlag.setType(TriggerAction.ActionType.FLAG);
            incrementFlag.setUserData("flagId", (short) 2);
            incrementFlag.setUserData("flag", (short) 18); // VALUE | PLUS
            incrementFlag.setUserData("value", 1);
            triggers.put(incrementFlag.getId(), incrementFlag);
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
