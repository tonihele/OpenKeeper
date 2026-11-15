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
            f.tick();
            assertEquals(1, f.stats.getCreaturesKilled((short) 0));
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
            f.tick();
            f.tick();
            assertNotNull(f.data.getComponent(victim, Death.class));
            assertEquals(1, f.stats.getCreaturesKilled((short) 0));
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
            EntityController.setDamage(f.data, victim, 100);
            f.tick(); f.tick(); f.tick();
            assertEquals(0, f.stats.getCreaturesKilled((short) 0));
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
        }
    }

    @Test
    void lastDamagingPlayerWinsAndHealingDoesNotReplaceTheSource() {
        try (Fixture f = new Fixture(true)) {
            EntityId victim = f.creature((short) 4, true);
            EntityController.setDamage(f.data, victim, 20, (short) 5);
            EntityController.setDamage(f.data, victim, 100, (short) 3);
            EntityController.setDamage(f.data, victim, -5, (short) 4);
            f.tick();
            assertEquals(1, f.stats.getCreaturesKilled((short) 0));
        }
    }

    private static final class Fixture implements AutoCloseable {
        final EntityData data = new DefaultEntityData();
        final PlayerStatsControl stats = new PlayerStatsControl();
        final HealthSystem health;

        Fixture(boolean corpse) {
            Creature definition = new Creature();
            definition.setFlags(corpse ? EnumSet.of(Creature.CreatureFlag.GENERATE_DEAD_BODY)
                    : EnumSet.noneOf(Creature.CreatureFlag.class));
            IKwdFile kwd = proxy(IKwdFile.class, (p, m, a) -> definition);
            ILevelInfo level = proxy(ILevelInfo.class, (p, m, a) -> kwd);
            IPlayerController player = proxy(IPlayerController.class, (p, m, a) -> switch (m.getName()) {
                case "getKeeper" -> new Keeper(false, (short) 3);
                case "getStatsControl" -> stats;
                default -> throw new AssertionError(m.getName());
            });
            StateMachine state = proxy(StateMachine.class, (p, m, a) -> null);
            ICreatureController creature = proxy(ICreatureController.class, (p, m, a) -> state);
            ICreaturesController creatures = proxy(ICreaturesController.class, (p, m, a) -> creature);
            IEntityPositionLookup lookup = proxy(IEntityPositionLookup.class, (p, m, a) ->
                    proxy(IEntityController.class, (p2, m2, a2) -> { data.removeEntity((EntityId) a[0]); return null; }));
            Map<MiscType, MiscVariable> settings = new EnumMap<>(MiscType.class);
            for (MiscType type : List.of(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS,
                    MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND)) {
                settings.put(type, CreatureSleepFixtures.variable(type, type == MiscType.CREATURE_DYING_STATE_DURATION_SECONDS ? 2 : 0));
            }
            health = new HealthSystem(data, lookup, settings, creatures, level, List.of(player), null, () -> 0,
                    proxy(GameEventService.class, (p, m, a) -> null));
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
