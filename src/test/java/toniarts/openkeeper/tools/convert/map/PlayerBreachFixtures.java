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

/** Original synthetic player script records for breach-event scenarios. */
public final class PlayerBreachFixtures {
    private PlayerBreachFixtures() {
    }

    public static Player player(short id, int triggerId) {
        Player player = new Player();
        player.setPlayerId(id);
        player.setTriggerId(triggerId);
        return player;
    }

    public static TriggerGeneric condition(int id, TriggerGeneric.TargetType type, int child, int next, int repeats) {
        TriggerGeneric condition = new TriggerGeneric(null);
        condition.setId(id);
        condition.setIdChild(child);
        condition.setIdNext(next);
        condition.setRepeatTimes((short) repeats);
        condition.setType(type);
        if (type == TriggerGeneric.TargetType.FLAG) {
            condition.setTargetValueComparison(TriggerGeneric.ComparisonType.EQUAL_TO);
            condition.setUserData("targetId", (short) 1);
            condition.setUserData("flag", (short) 1);
            condition.setUserData("value", 1);
        } else if (type == TriggerGeneric.TargetType.PLAYER_ENEMY_BREACHED) {
            condition.setUserData("playerId", (short) 255);
        }
        return condition;
    }

    public static TriggerAction action(int id, int flagId, int next) {
        TriggerAction action = new TriggerAction(null);
        action.setId(id);
        action.setIdNext(next);
        action.setType(TriggerAction.ActionType.FLAG);
        action.setUserData("flagId", (short) flagId);
        action.setUserData("flag", (short) 18);
        action.setUserData("value", 1);
        return action;
    }
}
