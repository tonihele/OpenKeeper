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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper. If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.tools.convert.map;

import java.util.HashMap;
import java.util.Map;

/** Original script records for stealing-attempt integration tests. */
public final class CreatureStealsFixtures {
    private CreatureStealsFixtures() { }

    public static Thing.Creature thing(short owner) {
        if (owner == Player.GOOD_PLAYER_ID) return CreatureAngryFixtures.thing(1);
        Thing.KeeperCreature creature = new Thing.KeeperCreature();
        creature.setTriggerId(1);
        creature.setPlayerId(owner);
        return creature;
    }

    public static Map<Integer, Trigger> script(int repeats, boolean checkGold) {
        Map<Integer, Trigger> result = new HashMap<>();
        result.put(1, CreatureAngryFixtures.condition(1, TriggerGeneric.TargetType.CREATURE_STEALS,
                checkGold ? 2 : 3, 4, repeats));
        if (checkGold) {
            TriggerGeneric gold = CreatureAngryFixtures.condition(2,
                    TriggerGeneric.TargetType.CREATURE_GOLD_HELD, 3, 0, 255);
            gold.setTargetValueComparison(TriggerGeneric.ComparisonType.EQUAL_TO);
            gold.setUserData("value", 0);
            result.put(2, gold);
        }
        result.put(3, CreatureAngryFixtures.action(3, 1));
        // Event discovery is root-only: this nested event must not fire.
        result.put(4, CreatureAngryFixtures.condition(4, TriggerGeneric.TargetType.FLAG, 5, 0, 255));
        result.put(5, CreatureAngryFixtures.condition(5, TriggerGeneric.TargetType.CREATURE_STEALS, 6, 0, 255));
        result.put(6, CreatureAngryFixtures.action(6, 2));
        return result;
    }
}
