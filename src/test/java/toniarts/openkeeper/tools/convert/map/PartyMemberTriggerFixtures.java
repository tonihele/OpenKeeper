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
 * Builds the party member conditions for the tests. Lives in the trigger
 * package because the setters are {@code protected}.
 */
public final class PartyMemberTriggerFixtures {

    private PartyMemberTriggerFixtures() {
    }

    /**
     * A "party members incapacitated" condition, like the ones in level 16.
     * The party is in the byte the editor does not name.
     *
     * @param id the trigger id
     * @param child the id of the first child
     * @param comparison how the count is compared to the value
     * @param partyId the party whose members are counted
     * @param value the value to compare to
     * @return the condition
     */
    public static TriggerGeneric membersIncapacitated(int id, int child, TriggerGeneric.ComparisonType comparison,
            short partyId, int value) {
        TriggerGeneric trigger = new TriggerGeneric(null);
        trigger.setId(id);
        trigger.setIdChild(child);
        trigger.setRepeatTimes((short) 1);
        trigger.setType(TriggerGeneric.TargetType.PARTY_MEMBERS_INCAPACITATED);
        trigger.setTargetValueComparison(comparison);
        trigger.setUserData("unknown", partyId);
        trigger.setUserData("value", value);
        return trigger;
    }

    /**
     * An action that sets a flag to a constant value
     *
     * @param id the trigger id
     * @param flagId the flag
     * @param value the value
     * @return the action
     */
    public static TriggerAction setFlag(int id, int flagId, int value) {
        TriggerAction action = new TriggerAction(null);
        action.setId(id);
        action.setType(TriggerAction.ActionType.FLAG);
        action.setUserData("flagId", (short) flagId);
        action.setUserData("flag", (short) (0x02 | 0x08)); // Constant value, "="
        action.setUserData("value", value);
        return action;
    }
}
