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
package toniarts.openkeeper.game.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.creature.IPartyController;
import toniarts.openkeeper.game.trigger.party.PartyTriggerControl;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.PartyMemberTriggerFixtures;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Trigger;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric.ComparisonType;

/**
 * The "party members incapacitated" condition. In the original the party is
 * the one the condition names, whatever tree the condition is in. A tree that
 * belongs to a member of a party (not the party itself) has to count too.
 */
class PartyMembersIncapacitatedTest {

    private static final short PARTY_ID = 4;
    private static final short OTHER_PARTY_ID = 5;
    private static final int ROOT_TRIGGER_ID = 171;
    private static final int ACTION_TRIGGER_ID = 172;
    private static final int FLAG_ID = 7;

    private final int[] flags = new int[16];
    private final Map<Short, IPartyController> parties = new HashMap<>();

    @Test
    void aMemberTreeNoticesTheIncapacitatedMembersOfTheParty() {
        addParty(PARTY_ID, true, 4, member(true, false), member(false, true), member(false, false), member(false, false));
        TriggerControl control = memberTree(ComparisonType.GREATER_OR_EQUAL_TO, PARTY_ID, 2);

        control.update(0.25f);

        assertEquals(1, flags[FLAG_ID]);
    }

    @Test
    void aMemberTreeWaitsForEnoughIncapacitatedMembers() {
        addParty(PARTY_ID, true, 4, member(true, false), member(false, false), member(false, false), member(false, false));
        TriggerControl control = memberTree(ComparisonType.GREATER_OR_EQUAL_TO, PARTY_ID, 2);

        control.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    @Test
    void theMembersThatWereNeverSpawnedAreIncapacitatedToo() {
        addParty(PARTY_ID, true, 3, member(false, false));
        TriggerControl control = memberTree(ComparisonType.GREATER_OR_EQUAL_TO, PARTY_ID, 2);

        control.update(0.25f);

        assertEquals(1, flags[FLAG_ID]);
    }

    @Test
    void thePartyIsTheOneInTheConditionNotAnotherOne() {
        addParty(PARTY_ID, true, 2, member(false, false), member(false, false));
        addParty(OTHER_PARTY_ID, true, 2, member(true, false), member(true, false));
        TriggerControl control = memberTree(ComparisonType.GREATER_OR_EQUAL_TO, PARTY_ID, 1);

        control.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    @Test
    void aPartyThatDoesNotExistHasNoIncapacitatedMembers() {
        TriggerControl control = memberTree(ComparisonType.GREATER_OR_EQUAL_TO, PARTY_ID, 1);

        control.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    @Test
    void aPartyTreeCountsThePartyOfTheCondition() {
        IPartyController own = addParty(OTHER_PARTY_ID, true, 1, member(false, false));
        addParty(PARTY_ID, true, 2, member(true, false), member(true, false));
        TriggerControl control = partyTree(own, ComparisonType.GREATER_OR_EQUAL_TO, PARTY_ID, 2);

        control.update(0.25f);

        assertEquals(1, flags[FLAG_ID]);
    }

    private ICreatureController member(boolean dead, boolean incapacitated) {
        return stub(ICreatureController.class, (method, args) -> switch (method.getName()) {
            case "isDead" ->
                dead;
            case "isIncapacitated" ->
                incapacitated;
            default ->
                null;
        });
    }

    private IPartyController addParty(short partyId, boolean created, int definedMembers, ICreatureController... members) {
        Set<Thing.GoodCreature> defined = new AbstractSet<>() {
            @Override
            public Iterator<Thing.GoodCreature> iterator() {
                return new ArrayList<Thing.GoodCreature>().iterator();
            }

            @Override
            public int size() {
                return definedMembers;
            }
        };
        IPartyController party = stub(IPartyController.class, (method, args) -> switch (method.getName()) {
            case "isCreated" ->
                created;
            case "getMembers" ->
                defined;
            case "getActualMembers" ->
                List.of(members);
            case "getPartyId" ->
                partyId;
            default ->
                null;
        });
        parties.put(partyId, party);
        return party;
    }

    /**
     * The tree of a member of a party, no party scope at all
     */
    private TriggerControl memberTree(ComparisonType comparison, short partyId, int value) {
        return new TriggerControl(null, levelInfo(comparison, partyId, value), stub(IGameTimer.class, (m, a) -> null),
                null, creaturesController(), null, ROOT_TRIGGER_ID);
    }

    private TriggerControl partyTree(IPartyController own, ComparisonType comparison, short partyId, int value) {
        return new PartyTriggerControl(null, levelInfo(comparison, partyId, value), stub(IGameTimer.class, (m, a) -> null),
                null, creaturesController(), ROOT_TRIGGER_ID, own, null);
    }

    private ICreaturesController creaturesController() {
        return stub(ICreaturesController.class,
                (method, args) -> method.getName().equals("getParty") ? parties.get((Short) args[0]) : null);
    }

    private ILevelInfo levelInfo(ComparisonType comparison, short partyId, int value) {
        Map<Integer, Trigger> triggers = new HashMap<>();
        triggers.put(ROOT_TRIGGER_ID, PartyMemberTriggerFixtures.membersIncapacitated(ROOT_TRIGGER_ID, ACTION_TRIGGER_ID,
                comparison, partyId, value));
        triggers.put(ACTION_TRIGGER_ID, PartyMemberTriggerFixtures.setFlag(ACTION_TRIGGER_ID, FLAG_ID, 1));
        IKwdFile kwdFile = stub(IKwdFile.class, (method, args) -> method.getName().equals("getTriggers") ? triggers : null);
        return stub(ILevelInfo.class, (method, args) -> switch (method.getName()) {
            case "getLevelData" ->
                kwdFile;
            case "getFlag" ->
                flags[(Integer) args[0]];
            case "setFlag" -> {
                flags[(Integer) args[0]] = (Integer) args[1];
                yield null;
            }
            default ->
                null;
        });
    }

    private interface Answer {

        Object answer(Method method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Object result = answer.answer(method, args);
            if (result == null && method.getReturnType().isPrimitive()) {
                return method.getReturnType() == boolean.class ? (Object) false : (Object) 0;
            }
            return result;
        });
    }
}
