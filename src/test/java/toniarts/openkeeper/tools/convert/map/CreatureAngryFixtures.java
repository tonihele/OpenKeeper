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

import java.util.EnumSet;

/** Original synthetic creature definitions and script records for anger-event tests. */
public final class CreatureAngryFixtures {
    private CreatureAngryFixtures() {
    }

    public static Creature creature(boolean emotionless) {
        Creature creature = CreatureSleepFixtures.sleeper(1);
        creature.getAttributes().setAngerNoLair((short) 4);
        creature.getAttributes().setAngerSlap((short) 1);
        creature.getAttributes().setSlapDamage(0);
        creature.setFlags2(emotionless ? EnumSet.of(Creature.CreatureFlag2.IS_EMOTIONLESS)
                : EnumSet.noneOf(Creature.CreatureFlag2.class));
        return creature;
    }

    public static Thing.GoodCreature thing(int triggerId) {
        Thing.GoodCreature creature = new Thing.GoodCreature();
        creature.setTriggerId(triggerId);
        return creature;
    }

    public static TriggerGeneric condition(int id, TriggerGeneric.TargetType type,
            int child, int next, int repeats) {
        TriggerGeneric trigger = new TriggerGeneric(null);
        trigger.setId(id);
        trigger.setIdChild(child);
        trigger.setIdNext(next);
        trigger.setType(type);
        trigger.setRepeatTimes((short) repeats);
        if (type == TriggerGeneric.TargetType.FLAG) {
            trigger.setTargetValueComparison(TriggerGeneric.ComparisonType.EQUAL_TO);
            trigger.setUserData("targetId", (short) 1);
            trigger.setUserData("flag", (short) 1);
            trigger.setUserData("value", 1);
        }
        return trigger;
    }

    public static TriggerGeneric healthCondition(int id, int child, int percentage) {
        TriggerGeneric condition = condition(id, TriggerGeneric.TargetType.CREATURE_HEALTH, child, 0, 255);
        condition.setTargetValueComparison(TriggerGeneric.ComparisonType.EQUAL_TO);
        condition.setUserData("value", percentage);
        return condition;
    }

    public static TriggerAction action(int id, int flagId) {
        TriggerAction action = new TriggerAction(null);
        action.setId(id);
        action.setType(TriggerAction.ActionType.FLAG);
        action.setUserData("flagId", (short) flagId);
        action.setUserData("flag", (short) 18); // Add the fixed value.
        action.setUserData("value", 1);
        return action;
    }
}
