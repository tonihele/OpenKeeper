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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Party;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.PartyController;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.logic.HealthSystem;
import toniarts.openkeeper.game.trigger.party.PartyTriggerControl;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric.ComparisonType;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Synthetic fixtures exercise captured conditions through real creature lifecycle paths. */
class PartyCapturedTriggerTest {

    @ParameterizedTest
    @CsvSource({
        "LESS_THAN,0,false", "LESS_THAN,1,false", "LESS_THAN,2,true",
        "LESS_OR_EQUAL_TO,0,false", "LESS_OR_EQUAL_TO,1,true", "LESS_OR_EQUAL_TO,2,true",
        "EQUAL_TO,0,false", "EQUAL_TO,1,true", "EQUAL_TO,2,false",
        "GREATER_THAN,0,true", "GREATER_THAN,1,false", "GREATER_THAN,2,false",
        "GREATER_OR_EQUAL_TO,0,true", "GREATER_OR_EQUAL_TO,1,true", "GREATER_OR_EQUAL_TO,2,false",
        "NOT_EQUAL_TO,0,true", "NOT_EQUAL_TO,1,false", "NOT_EQUAL_TO,2,true",
        "NONE,1,false"
    })
    void comparesCapturedCountAndExecutesItsChildAction(ComparisonType comparison, int value, boolean expected) {
        try (Fixture f = new Fixture()) {
            f.convert(1, (short) 3);
            assertEquals(expected, f.fires(comparison, value));
        }
    }

    @Test
    void prisonTortureAndStartingUnconsciousDoNotByThemselvesDetachMembers() {
        try (Fixture f = new Fixture()) {
            f.data.setComponent(f.ids.get(0), new CreatureImprisoned(0, 0));
            f.data.setComponent(f.ids.get(1), new CreatureTortured(0, 0, 0));
            f.data.setComponent(f.ids.get(2), new Unconscious(0));
            assertEquals(0, f.party.getCapturedMemberCount());
            assertTrue(f.fires(ComparisonType.EQUAL_TO, 0));
            assertFalse(f.fires(ComparisonType.GREATER_THAN, 0));
        }
    }

    @Test
    void ordinaryKnockoutCountsBeforeDeathAndRecoveryDoesNotRestoreMembership() {
        try (Fixture f = new Fixture()) {
            f.knockOut(1);
            assertNotNull(f.data.getComponent(f.ids.get(1), Unconscious.class));
            assertFalse(f.controllers.get(1).isDead());
            assertEquals(1, f.party.getCapturedMemberCount());
            assertTrue(f.fires(ComparisonType.EQUAL_TO, 1));
            f.recover(1);
            assertFalse(f.controllers.get(1).isIncapacitated());
            assertEquals(1, f.party.getCapturedMemberCount());
        }
    }

    @Test
    void imprisoningAKnockedOutMemberDoesNotCountItTwice() {
        try (Fixture f = new Fixture()) {
            f.knockOut(1);
            f.data.setComponent(f.ids.get(1), new CreatureImprisoned(0, 0));
            f.data.setComponent(f.ids.get(1), new Owner(Player.GOOD_PLAYER_ID, (short) 3));
            assertEquals(1, f.party.getCapturedMemberCount());
            f.data.removeComponent(f.ids.get(1), CreatureImprisoned.class);
            f.recover(1);
            assertEquals(1, f.party.getCapturedMemberCount());
        }
    }

    @Test
    void leaderKnockoutDoesNotCountAndPromotedLeadersKeepThatRole() {
        try (Fixture f = new Fixture(2)) {
            f.knockOut(0);
            assertEquals(0, f.party.getCapturedMemberCount());
            assertSame(f.controllers.get(1), f.party.getPartyLeader());
            f.knockOut(1);
            assertEquals(0, f.party.getCapturedMemberCount());
            f.recover(0);
            f.knockOut(0);
            assertEquals(0, f.party.getCapturedMemberCount());
        }
    }

    @Test
    void successiveKnockoutsPromoteLeadersWithoutAnInterveningLeaderQuery() {
        try (Fixture f = new Fixture(2)) {
            f.knockOut(0);
            f.knockOut(1);
            assertEquals(0, f.party.getCapturedMemberCount());
        }
    }

    @Test
    void successorsFollowReverseSpawnOrderAndNeverReelectFormerLeaders() {
        try (Fixture f = new Fixture(4)) {
            f.knockOut(0);
            assertSame(f.controllers.get(3), f.party.getPartyLeader());
            f.recover(0);
            f.knockOut(3);
            assertSame(f.controllers.get(2), f.party.getPartyLeader());
            f.recover(3);
            f.knockOut(2);
            assertSame(f.controllers.get(1), f.party.getPartyLeader());
            assertEquals(0, f.party.getCapturedMemberCount());
        }
    }

    @Test
    void convertedAndRecoveredOrdinaryMembersCannotBecomeSuccessors() {
        try (Fixture f = new Fixture(4)) {
            f.convert(3, (short) 3);
            f.convert(3, Player.GOOD_PLAYER_ID);
            f.knockOut(2);
            f.recover(2);
            f.knockOut(0);
            assertSame(f.controllers.get(1), f.party.getPartyLeader());
            assertEquals(2, f.party.getCapturedMemberCount());
        }
    }

    @Test
    void anUnflaggedMemberIsNotAFallbackSuccessor() {
        try (Fixture f = new Fixture(2, false)) {
            f.knockOut(0);
            assertSame(f.controllers.get(0), f.party.getPartyLeader());
            f.knockOut(1);
            assertEquals(1, f.party.getCapturedMemberCount());
        }
    }

    @Test
    void aMissingFollowerIsSkippedDuringSuccession() {
        try (Fixture f = new Fixture(3)) {
            f.data.removeEntity(f.ids.get(2));
            f.knockOut(0);
            assertSame(f.controllers.get(1), f.party.getPartyLeader());
        }
    }

    @Test
    void conversionAndReturningToHeroesFollowTheNativeCountSequence() {
        try (Fixture f = new Fixture()) {
            assertEquals(0, f.party.getCapturedMemberCount());
            f.convert(1, (short) 3);
            assertEquals(1, f.party.getCapturedMemberCount());
            f.convert(1, Player.GOOD_PLAYER_ID);
            assertEquals(Player.GOOD_PLAYER_ID, f.controllers.get(1).getOwnerId());
            assertEquals(1, f.party.getCapturedMemberCount());
            f.knockOut(2);
            assertEquals(2, f.party.getCapturedMemberCount());
            f.knockOut(0);
            assertEquals(2, f.party.getCapturedMemberCount());
            assertTrue(f.fires(ComparisonType.EQUAL_TO, 2));
        }
    }

    @ParameterizedTest
    @CsvSource({"0", "1"})
    void conversionCountsBothLeadersAndOrdinaryMembers(int member) {
        try (Fixture f = new Fixture()) {
            f.convert(member, (short) 3);
            assertTrue(f.fires(ComparisonType.EQUAL_TO, 1));
        }
    }

    @Test
    void assigningTheSameHeroOwnerDoesNotDetachAMember() {
        try (Fixture f = new Fixture()) {
            f.convert(1, Player.GOOD_PLAYER_ID);
            assertEquals(0, f.party.getCapturedMemberCount());
        }
    }

    @Test
    void missingAndDeadMembersCountAgainstTheOriginalRoster() {
        try (Fixture f = new Fixture()) {
            f.data.removeComponent(f.ids.get(1), Health.class);
            f.data.removeEntity(f.ids.get(2));
            assertEquals(2, f.party.getCapturedMemberCount());
            assertTrue(f.fires(ComparisonType.EQUAL_TO, 2));
        }
    }

    @Test
    void uncreatedPartyDoesNotActivateEvenForZeroCount() {
        try (Fixture f = new Fixture()) {
            PartyController uncreated = new PartyController(f.roster);
            assertFalse(f.fires(uncreated, ComparisonType.EQUAL_TO, 0));
        }
    }

    private static final class Fixture implements AutoCloseable {
        final EntityData data = new DefaultEntityData();
        final Thing.HeroParty roster = new Thing.HeroParty();
        final PartyController party;
        final List<EntityId> ids = new ArrayList<>();
        final List<CreatureController> controllers = new ArrayList<>();
        final Creature definition = new Creature();
        final AtomicInteger flag = new AtomicInteger();
        final ICreaturesController creatures;
        final ILevelInfo level;
        final HealthSystem health;
        final Map<MiscType, Variable.MiscVariable> settings = Map.of(
                MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, variable(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, 100),
                MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, variable(MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, 0),
                MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, variable(MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, 0));
        TriggerGeneric condition;
        TriggerAction action;

        Fixture() { this(3); }

        Fixture(int count) { this(count, true); }

        Fixture(int count, boolean followerFlags) {
            definition.setCreatureId((short) 7);
            definition.setFlags(EnumSet.of(Creature.CreatureFlag.GENERATE_DEAD_BODY));
            var members = new ArrayList<Thing.GoodCreature>();
            for (int i = 0; i < count; i++) {
                var member = new Thing.GoodCreature();
                member.setCreatureId((short) 7);
                member.setFlags(i == 0 ? EnumSet.of(Thing.Creature.CreatureFlag.LEADER)
                        : (followerFlags ? EnumSet.of(Thing.Creature.CreatureFlag.FOLLOWER)
                                : EnumSet.noneOf(Thing.Creature.CreatureFlag.class)));
                members.add(member);
            }
            roster.setHeroPartyMembers(members);
            party = new PartyController(roster);
            party.create();
            creatures = proxy(ICreaturesController.class, (p, m, a) -> switch (m.getName()) {
                case "getPartyById" -> { assertEquals(party.getId(), a[0]); yield party; }
                case "createController" -> controllers.get(ids.indexOf(a[0]));
                default -> throw new UnsupportedOperationException(m.getName());
            });
            IKwdFile kwd = proxy(IKwdFile.class, (p, m, a) -> switch (m.getName()) {
                case "getCreature" -> definition;
                case "getTriggers" -> Map.of(1, condition, 2, action);
                default -> throw new UnsupportedOperationException(m.getName());
            });
            level = proxy(ILevelInfo.class, (p, m, a) -> switch (m.getName()) {
                case "getLevelData" -> kwd;
                case "getFlag" -> flag.get();
                case "setFlag" -> { flag.set((int) a[1]); yield null; }
                default -> throw new UnsupportedOperationException(m.getName());
            });
            GameEventService events = proxy(GameEventService.class, (p, m, a) -> null);
            for (Thing.GoodCreature member : members) {
                EntityId id = data.createEntity();
                ids.add(id);
                CreatureComponent component = new CreatureComponent();
                component.creatureId = 7;
                data.setComponents(id, component, new Health(100, 100), new Owner((short) 1, (short) 1),
                        new Party(party.getId()), new CreatureAi(0, CreatureState.WANDER, (short) 7));
                var controller = new CreatureController(id, data, definition, null, null, () -> 0,
                        settings, creatures, null, null, level, null, null, events);
                controllers.add(controller);
                party.addMemberInstance(member, controller);
            }
            health = new HealthSystem(data, null, settings, creatures, level, List.of(), null, () -> 0, events);
        }

        void knockOut(int index) {
            data.setComponent(ids.get(index), new Damage(100));
            health.processTick(.25f);
        }

        void recover(int index) {
            data.removeComponent(ids.get(index), Unconscious.class);
            data.setComponent(ids.get(index), new Health(100, 100));
        }

        void convert(int index, short owner) {
            // Retain a harmless requested state so AI task selection is independent of this conversion test.
            data.setComponent(ids.get(index), new CreatureAi(0, CreatureState.IDLE, (short) 7, CreatureState.WANDER));
            controllers.get(index).convertCreature(owner);
        }

        boolean fires(ComparisonType comparison, int threshold) {
            return fires(party, comparison, threshold);
        }

        boolean fires(PartyController testedParty, ComparisonType comparison, int threshold) {
            flag.set(0);
            condition = new TriggerGeneric(null);
            condition.setId(1);
            condition.setIdChild(2);
            condition.setRepeatTimes((short) 255);
            condition.setType(TriggerGeneric.TargetType.PARTY_MEMBERS_CAPTURED);
            condition.setTargetValueComparison(comparison);
            condition.setUserData("value", threshold);
            action = new TriggerAction(null);
            action.setId(2);
            action.setType(TriggerAction.ActionType.FLAG);
            action.setUserData("flagId", (short) 0);
            action.setUserData("flag", (short) 10);
            action.setUserData("value", 1);
            var control = new PartyTriggerControl(null, level, null, null, null, 1, testedParty);
            control.update(.25f);
            control.update(.25f);
            return flag.get() == 1;
        }

        @Override
        public void close() {
            health.stop();
            data.close();
        }
    }

    private static Variable.MiscVariable variable(MiscType type, int value) {
        var variable = new Variable.MiscVariable();
        variable.setVariableId(type);
        variable.setValue(value);
        return variable;
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
