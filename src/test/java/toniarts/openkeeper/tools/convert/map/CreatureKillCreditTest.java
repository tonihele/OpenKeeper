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

import com.badlogic.gdx.ai.fsm.StateMachine;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.game.controller.*;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.logic.HealthSystem;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.*;

class CreatureKillCreditTest {

    @Test
    void lethalDamageCreditsItsPlayerOnceAndUsesOwnerRatherThanCaptor() {
        try (Fixture f = new Fixture(true)) {
            EntityId victim = f.creature((short) 4, true);
            f.data.setComponent(victim, new Owner((short) 4, (short) 3));
            EntityController.setDamage(f.data, victim, 100, (short) 3);
            f.tick();
            assertNotNull(f.data.getComponent(victim, Death.class));
            assertEquals(1, f.stats.getCreaturesKilled((short) 4));
            assertEquals(1, f.victimStats.getCreaturesLost());
            assertEquals(0, f.stats.getCreaturesLost());
            f.tick();
            assertEquals(1, f.stats.getCreaturesKilled((short) 0));
            assertEquals(1, f.victimStats.getCreaturesLost());
        }
    }

    @Test
    void knockoutCountsOnlyWhenTheCreatureEventuallyDies() {
        try (Fixture f = new Fixture(true)) {
            EntityId victim = f.creature((short) 4, false);
            EntityController.setDamage(f.data, victim, 100, (short) 3);
            f.tick();
            assertNotNull(f.data.getComponent(victim, Unconscious.class));
            assertEquals(0, f.stats.getCreaturesKilled((short) 0));
            assertEquals(0, f.victimStats.getCreaturesLost());
            f.tick();
            f.tick();
            assertNotNull(f.data.getComponent(victim, Death.class));
            assertEquals(1, f.stats.getCreaturesKilled((short) 0));
            assertEquals(1, f.victimStats.getCreaturesLost());
        }
    }

    @Test
    void rescuedCreatureDoesNotGiveStaleCreditToAnEnvironmentalDeath() {
        try (Fixture f = new Fixture(true)) {
            EntityId victim = f.creature((short) 4, false);
            EntityController.setDamage(f.data, victim, 100, (short) 3);
            f.tick();
            f.data.removeComponent(victim, Unconscious.class);
            f.data.setComponent(victim, new Health(100, 100));
            f.tick();
            assertNull(f.data.getComponent(victim, CreatureDeathCredit.class));
            assertEquals(0, f.victimStats.getCreaturesLost());
            EntityController.setDamage(f.data, victim, 100);
            f.tick(); f.tick(); f.tick();
            assertEquals(0, f.stats.getCreaturesKilled((short) 0));
            assertEquals(1, f.victimStats.getCreaturesLost());
        }
    }

    @Test
    void creaturesWithoutCorpsesAlsoCountButObjectsDoNot() {
        try (Fixture f = new Fixture(false)) {
            EntityId victim = f.creature((short) 4, true);
            EntityId object = f.data.createEntity();
            f.data.setComponents(object, new Health(10, 10), new Owner((short) 4, (short) 4));
            EntityController.setDamage(f.data, victim, 100, (short) 3);
            EntityController.setDamage(f.data, object, 100, (short) 3);
            f.tick();
            assertNull(f.data.getComponent(victim, Health.class));
            assertEquals(1, f.stats.getCreaturesKilled((short) 0));
            assertEquals(1, f.victimStats.getCreaturesLost());
        }
    }

    @Test
    void latestHealthAdjustmentSelectsTheSourceIncludingHealing() {
        try (Fixture f = new Fixture(true)) {
            EntityId victim = f.creature((short) 4, true);
            EntityController.setDamage(f.data, victim, 20, (short) 5);
            EntityController.setDamage(f.data, victim, 100, (short) 3);
            EntityController.setDamage(f.data, victim, -5, (short) 4);
            f.tick();
            assertEquals(0, f.stats.getCreaturesKilled((short) 0));
            assertEquals(1, f.victimStats.getCreaturesKilled((short) 4));
        }
    }

    @Test
    void workerAndOwnCreatureDeathsCountAsLosses() {
        try (Fixture f = new Fixture(true)) {
            EntityId worker = f.creature((short) 4, true);
            CreatureComponent type = new CreatureComponent(f.data.getComponent(worker, CreatureComponent.class));
            type.worker = true;
            f.data.setComponent(worker, type);
            EntityId own = f.creature((short) 3, true);
            EntityController.setDamage(f.data, worker, 100, (short) 3);
            EntityController.setDamage(f.data, own, 100, (short) 3);
            f.tick();
            assertEquals(1, f.victimStats.getCreaturesLost());
            assertEquals(1, f.stats.getCreaturesLost());
            assertEquals(0, f.stats.getCreaturesKilled((short) 0));
            assertEquals(1, f.stats.getCreaturesKilled((short) 4));
            assertEquals(1, f.stats.getCreaturesKilled((short) 3));
        }
    }

    @Test
    void lossesDoNotRequireAnAttributedOrRegisteredKiller() {
        try (Fixture f = new Fixture(true)) {
            EntityId natural = f.creature((short) 4, true);
            EntityId unknown = f.creature((short) 4, true);
            EntityController.setDamage(f.data, natural, 100);
            EntityController.setDamage(f.data, unknown, 100, (short) 255);
            f.tick();
            assertEquals(2, f.victimStats.getCreaturesLost());
            assertEquals(0, f.stats.getCreaturesKilled((short) 0));
        }
    }

    @Test
    void conversionAndRemovalAreNotLosses() {
        try (Fixture f = new Fixture(true)) {
            EntityId converted = f.creature((short) 4, true);
            EntityId removed = f.creature((short) 4, true);
            f.tick();
            f.data.setComponent(converted, new Owner((short) 3, (short) 3));
            f.data.removeEntity(removed);
            f.tick();
            assertEquals(0, f.stats.getCreaturesLost());
            assertEquals(0, f.victimStats.getCreaturesLost());
        }
    }

    @Test
    void nativeComparisonMatrixCountsOnlyActualDeathsAndKeepsBothPlayersSeparate() {
        try (Fixture f = new Fixture(true, 60)) {
            EntityId[] subjects = new EntityId[8];
            for (int index = 0; index < subjects.length; index++) {
                subjects[index] = f.creature((short) (index < 6 ? 3 : 4), false);
                if (index == 1 || index == 7) {
                    CreatureComponent worker = new CreatureComponent(f.data.getComponent(subjects[index], CreatureComponent.class));
                    worker.worker = true;
                    worker.creatureId = 1;
                    f.data.setComponent(subjects[index], worker);
                }
            }
            for (int[] hit : new int[][]{{0,4}, {1,4}, {2,3}, {3,0}, {4,4}, {6,3}, {7,3}}) {
                EntityController.setDamage(f.data, subjects[hit[0]], 100000, hit[1] == 0 ? null : (short) hit[1]);
            }
            f.data.removeEntity(subjects[5]);
            f.tick();
            assertEquals(1, f.stats.getCreaturesLost());
            assertEquals(1, f.victimStats.getCreaturesLost());
            // Completion of rescue: the creature is awake and healthy again.
            f.data.removeComponent(subjects[4], Unconscious.class);
            f.data.setComponent(subjects[4], new Health(100, 100));
            for (int second = 0; second < 59; second++) {
                f.tick();
            }
            assertEquals(1, f.stats.getCreaturesLost());
            assertEquals(1, f.victimStats.getCreaturesLost());
            f.tick();
            assertEquals(4, f.stats.getCreaturesLost());
            assertEquals(2, f.victimStats.getCreaturesLost());
            assertEquals(2, f.stats.getCreaturesKilled((short) 4));
            assertEquals(1, f.stats.getCreaturesKilled((short) 3));
            assertEquals(2, f.victimStats.getCreaturesKilled((short) 3));
            assertEquals(0, f.victimStats.getCreaturesKilled((short) 4));
            assertNull(f.data.getComponent(subjects[4], Death.class));
            f.tick();
            assertEquals(4, f.stats.getCreaturesLost());
            assertEquals(2, f.victimStats.getCreaturesLost());
        }
    }

    @Test
    void ignoredDamageToAnUnconsciousCreatureChangesWhoGetsDeathCredit() {
        try (Fixture f = new Fixture(true)) {
            EntityId victim = f.creature((short) 4, false);
            EntityController.setDamage(f.data, victim, 100, (short) 3);
            f.tick();
            EntityController.setDamage(f.data, victim, 1, (short) 4);
            f.tick();
            assertNotNull(f.data.getComponent(victim, Unconscious.class));
            assertEquals(0, f.victimStats.getCreaturesLost());
            f.tick();
            assertEquals(1, f.victimStats.getCreaturesLost());
            assertEquals(1, f.victimStats.getCreaturesKilled((short) 4));
            assertEquals(0, f.stats.getCreaturesKilled((short) 4));
        }
    }

    @Test
    void unattributedDamageToAnUnconsciousCreatureClearsThePreviousKiller() {
        try (Fixture f = new Fixture(true)) {
            EntityId victim = f.creature((short) 4, false);
            EntityController.setDamage(f.data, victim, 100, (short) 3);
            f.tick();
            EntityController.setDamage(f.data, victim, 1);
            f.tick();
            f.tick();
            assertEquals(1, f.victimStats.getCreaturesLost());
            assertEquals(0, f.stats.getCreaturesKilled((short) 4));
        }
    }

    @Test
    void ignoredUnconsciousDamageIsNotReplayedAfterRescue() {
        try (Fixture f = new Fixture(true)) {
            EntityId victim = f.creature((short) 4, false);
            EntityController.setDamage(f.data, victim, 100, (short) 3);
            f.tick();
            EntityController.setDamage(f.data, victim, 100, (short) 3);
            f.tick();
            assertNull(f.data.getComponent(victim, Damage.class));
            f.data.removeComponent(victim, Unconscious.class);
            f.data.setComponent(victim, new Health(100, 100));
            f.tick();
            assertNull(f.data.getComponent(victim, Unconscious.class));
            assertEquals(100, f.data.getComponent(victim, Health.class).health);
            assertEquals(0, f.victimStats.getCreaturesLost());
        }
    }

    @Test
    void ignoredHealingToAnUnconsciousCreatureAlsoUpdatesAttribution() {
        try (Fixture f = new Fixture(true)) {
            EntityId victim = f.creature((short) 4, false);
            EntityController.setDamage(f.data, victim, 100, (short) 3);
            f.tick();
            EntityController.setDamage(f.data, victim, -1, (short) 4);
            f.tick();
            assertNotNull(f.data.getComponent(victim, Unconscious.class));
            assertEquals(0, f.data.getComponent(victim, Health.class).health);
            f.tick();
            assertEquals(1, f.victimStats.getCreaturesLost());
            assertEquals(1, f.victimStats.getCreaturesKilled((short) 4));
            assertEquals(0, f.stats.getCreaturesKilled((short) 4));
        }
    }

    private static final class Fixture implements AutoCloseable {
        final EntityData data = new DefaultEntityData();
        final PlayerStatsControl stats = new PlayerStatsControl();
        final PlayerStatsControl victimStats = new PlayerStatsControl();
        final HealthSystem health;

        Fixture(boolean corpse) {
            this(corpse, 2);
        }

        Fixture(boolean corpse, int deathDelay) {
            Creature definition = new Creature();
            definition.setFlags(corpse ? EnumSet.of(Creature.CreatureFlag.GENERATE_DEAD_BODY)
                    : EnumSet.noneOf(Creature.CreatureFlag.class));
            Creature workerDefinition = new Creature();
            workerDefinition.setFlags(EnumSet.noneOf(Creature.CreatureFlag.class));
            IKwdFile kwd = proxy(IKwdFile.class, (p, m, a) -> (short) a[0] == 1 ? workerDefinition : definition);
            ILevelInfo level = proxy(ILevelInfo.class, (p, m, a) -> kwd);
            List<IPlayerController> players = List.of(player((short) 3, stats), player((short) 4, victimStats));
            StateMachine state = proxy(StateMachine.class, (p, m, a) -> null);
            ICreatureController creature = proxy(ICreatureController.class, (p, m, a) -> state);
            ICreaturesController creatures = proxy(ICreaturesController.class, (p, m, a) -> creature);
            IEntityPositionLookup lookup = proxy(IEntityPositionLookup.class, (p, m, a) ->
                    proxy(IEntityController.class, (p2, m2, a2) -> { data.removeEntity((EntityId) a[0]); return null; }));
            Map<MiscType, MiscVariable> settings = new EnumMap<>(MiscType.class);
            for (MiscType type : List.of(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS,
                    MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND)) {
                settings.put(type, CreatureSleepFixtures.variable(type, type == MiscType.CREATURE_DYING_STATE_DURATION_SECONDS ? deathDelay : 0));
            }
            health = new HealthSystem(data, lookup, settings, creatures, level, players, null, () -> 0,
                    proxy(GameEventService.class, (p, m, a) -> null));
        }

        private IPlayerController player(short owner, PlayerStatsControl playerStats) {
            return proxy(IPlayerController.class, (p, m, a) -> switch (m.getName()) {
                case "getKeeper" -> new Keeper(false, owner);
                case "getStatsControl" -> playerStats;
                default -> throw new AssertionError(m.getName());
            });
        }

        EntityId creature(short owner, boolean instant) {
            EntityId entity = data.createEntity();
            CreatureComponent creature = new CreatureComponent();
            creature.creatureId = 7;
            creature.diesInstantly = instant;
            data.setComponents(entity, creature, new Health(100, 100), new Owner(owner, owner));
            return entity;
        }

        void tick() { health.processTick(1); }

        @Override
        public void close() { health.stop(); data.close(); }
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
