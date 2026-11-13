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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.tools.convert.map;

/**
 * Builds level script trigger fixtures for tests. Lives in the trigger
 * records' own package because their setters are {@code protected}
 * (parser-only).
 */
public final class TriggerFixtures {

    private TriggerFixtures() {
    }

    /**
     * Creates a trigger that is true once the level has run the given time
     *
     * @param id the trigger ID
     * @param idNext the next trigger in the chain, 0 for none
     * @param idChild the first child, 0 for none
     * @param repeatTimes the repeats, 255 = always
     * @param seconds the level time in seconds
     * @return the trigger
     */
    public static TriggerGeneric levelTime(int id, int idNext, int idChild, short repeatTimes, int seconds) {
        TriggerGeneric trigger = new TriggerGeneric(null);
        trigger.setId(id);
        trigger.setIdNext(idNext);
        trigger.setIdChild(idChild);
        trigger.setRepeatTimes(repeatTimes);
        trigger.setType(TriggerGeneric.TargetType.LEVEL_TIME);
        trigger.setTargetValueComparison(TriggerGeneric.ComparisonType.GREATER_OR_EQUAL_TO);
        trigger.setUserData("value", seconds);
        return trigger;
    }

    /**
     * Creates an action that adds a value to a level flag
     *
     * @param id the action ID
     * @param idNext the next trigger in the chain, 0 for none
     * @param flagId the flag
     * @param value the value to add
     * @return the action
     */
    public static TriggerAction addToFlag(int id, int idNext, short flagId, int value) {
        TriggerAction action = new TriggerAction(null);
        action.setId(id);
        action.setIdNext(idNext);
        action.setType(TriggerAction.ActionType.FLAG);
        action.setUserData("flagId", flagId);
        action.setUserData("flag", (short) (TriggerAction.FlagTargetValueActionType.VALUE.getFlagValue()
                | TriggerAction.FlagTargetValueActionType.PLUS.getFlagValue()));
        action.setUserData("value", value);
        return action;
    }

}
