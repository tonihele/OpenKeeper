/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.tools.convert.map;

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.game.controller.*;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import static org.junit.jupiter.api.Assertions.*;

class StartAsDyingTest {
    enum Kind { KEEPER, NEUTRAL, GOOD }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void placementStartsUnconsciousOnlyWhenFlagged(Kind kind) {
        try (var f = new Fixture()) {
            EntityId control = f.spawn(kind, 100, false, 1);
            EntityId dying = f.spawn(kind, 100, true, 1);
            assertEquals(1000, f.data.getComponent(control, Health.class).health);
            assertNull(f.data.getComponent(control, Unconscious.class));
            assertEquals(CreatureState.IDLE, f.data.getComponent(control, CreatureAi.class).getCreatureState());
            assertEquals(166, f.data.getComponent(dying, Health.class).health);
            assertEquals(CreatureState.UNCONSCIOUS, f.data.getComponent(dying, CreatureAi.class).getCreatureState());
            assertEquals(Creature.AnimationType.DIE, f.data.getComponent(dying, CreatureViewState.class).state);
            assertEquals(-18.5, f.data.getComponent(dying, Unconscious.class).startTime);
            assertNull(f.data.getComponent(dying, Death.class));
        }
    }

    @ParameterizedTest
    @CsvSource({"5, 1, 50, 1000", "100, 1, 166, 1000", "200, 1, 166, 1000", "100, 2, 250, 1500"})
    void healthBudgetUsesAuthoredHealthAndTheLevelAdjustedCap(int percent, int level, int expected, int maximum) {
        try (var f = new Fixture()) {
            EntityId dying = f.spawn(Kind.KEEPER, percent, true, level);
            assertEquals(expected, f.data.getComponent(dying, Health.class).health);
            assertEquals(maximum, f.data.getComponent(dying, Health.class).maxHealth);
            double remaining = 60 - (f.now - f.data.getComponent(dying, Unconscious.class).startTime);
            assertEquals(expected / 4.0, remaining);
        }
    }

    @Test
    void startDyingPreemptsActAsDroppedAndRuntimeSpawnsRemainOrdinary() {
        try (var f = new Fixture()) {
            Thing.KeeperCreature thing = (Thing.KeeperCreature) f.thing(Kind.KEEPER, 100, true, 1);
            thing.getFlags().add(Thing.Creature.CreatureFlag.ACT_AS_DROPPED);
            EntityId dying = f.factory.spawnCreature(thing, new Vector2f(2, 3));
            assertEquals(CreatureState.UNCONSCIOUS, f.data.getComponent(dying, CreatureAi.class).getCreatureState());
            assertEquals(0, f.roomQueries);
            assertNull(f.data.getComponent(dying, CreatureImprisoned.class));
            assertNull(f.data.getComponent(dying, CreatureTortured.class));
            EntityId ordinary = f.factory.spawnCreature((short) 6, (short) 3, 1, new Vector2f(2, 3), ICreaturesController.SpawnType.DROP);
            assertNull(f.data.getComponent(ordinary, Unconscious.class));
            assertEquals(1000, f.data.getComponent(ordinary, Health.class).health);
            assertEquals(1, f.roomQueries);
        }
    }

    @Test
    void instantDeathFlagDoesNotCreateAnUnconsciousCreature() {
        try (var f = new Fixture()) {
            Thing.GoodCreature thing = (Thing.GoodCreature) f.thing(Kind.GOOD, 100, true, 1);
            thing.setFlags2(EnumSet.of(Thing.Creature.CreatureFlag2.DIES_INSTANTLY));
            EntityId dying = f.factory.spawnCreature(thing, new Vector2f(2, 3));
            assertNotNull(f.data.getComponent(dying, Death.class));
            assertNull(f.data.getComponent(dying, Health.class));
            assertNull(f.data.getComponent(dying, CreatureAi.class));
            assertNull(f.data.getComponent(dying, Unconscious.class));
        }
    }

    private static final class Fixture implements AutoCloseable {
        final DefaultEntityData data = new DefaultEntityData();
        final double now = 0;
        final Creature definition = CreatureSleepFixtures.sleeper(0);
        final Creature imp = CreatureSleepFixtures.sleeper(0);
        int roomQueries;
        final CreaturesController factory;
        Fixture() {
            definition.getAttributes().setHp(1000);
            definition.setSpells(List.of());
            definition.setMeleeAttackType(Creature.AttackType.NONE);
            imp.setCreatureId((short) 1);
            Map<Variable.CreatureStats.StatType, Variable.CreatureStats> stats = new java.util.EnumMap<>(Variable.CreatureStats.StatType.class);
            for (var type : Variable.CreatureStats.StatType.values()) {
                var stat = new Variable.CreatureStats();
                stat.setStatId(type); stat.setValue(type == Variable.CreatureStats.StatType.HEALTH ? 150 : 100); stat.setLevel(2);
                stats.put(type, stat);
            }
            IKwdFile kwd = proxy(IKwdFile.class, (method, args) -> switch (method) {
                case "getThings" -> List.of();
                case "getCreature" -> definition;
                case "getImp" -> imp;
                case "getCreatureStats" -> ((Number) args[0]).intValue() == 2 ? stats : null;
                case "getGameLevel" -> CreatureSleepFixtures.level(4);
                default -> null;
            });
            IMapController map = proxy(IMapController.class, (method, args) -> { roomQueries++; return null; });
            factory = new CreaturesController(kwd, data,
                    Map.of(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, CreatureSleepFixtures.variable(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, 60)),
                    () -> now, null, map, null, null);
        }
        EntityId spawn(Kind kind, int health, boolean dying, int level) {
            return factory.spawnCreature(thing(kind, health, dying, level), new Vector2f(2, 3));
        }
        Thing.Creature thing(Kind kind, int health, boolean dying, int level) {
            var flags = EnumSet.of(Thing.Creature.CreatureFlag.WILL_FIGHT);
            if (dying) flags.add(Thing.Creature.CreatureFlag.START_AS_DYING);
            switch (kind) {
                case KEEPER -> {
                    var c = new Thing.KeeperCreature(); c.setPlayerId((short) 3); c.setCreatureId((short) 6);
                    c.setInitialHealth(health); c.setLevel((short) level); c.setFlags(flags); return c;
                }
                case NEUTRAL -> {
                    var c = new Thing.NeutralCreature(); c.setCreatureId((short) 6);
                    c.setInitialHealth(health); c.setLevel((short) level); c.setFlags(flags); return c;
                }
                default -> {
                    var c = new Thing.GoodCreature(); c.setCreatureId((short) 6);
                    c.setInitialHealth(health); c.setLevel((short) level); c.setFlags(flags); return c;
                }
            }
        }
        @Override public void close() { data.close(); }
    }
    private static <T> T proxy(Class<T> type, java.util.function.BiFunction<String, Object[], Object> fn) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, m, a) -> fn.apply(m.getName(), a)));
    }
}
