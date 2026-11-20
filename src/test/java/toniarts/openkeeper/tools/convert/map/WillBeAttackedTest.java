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
import org.junit.jupiter.params.provider.EnumSource;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.game.controller.*;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import static org.junit.jupiter.api.Assertions.*;

class WillBeAttackedTest {
    enum Kind { KEEPER, NEUTRAL, GOOD }

    @ParameterizedTest
    @EnumSource(Kind.class)
    void placementAppliesBothFlagsIndependently(Kind kind) {
        try (var f = new Fixture()) {
            for (boolean willFight : new boolean[]{false, true}) {
                for (boolean willBeAttacked : new boolean[]{false, true}) {
                    Thing.Creature thing = f.thing(kind, willFight, willBeAttacked);
                    EntityId placed = f.factory.spawnCreature(thing, new Vector2f(2, 3));
                    assertEquals(!willFight, f.data.getComponent(placed, Peaceful.class) != null);
                    assertEquals(!willBeAttacked, f.data.getComponent(placed, Unattackable.class) != null);
                    assertEquals(1000, f.data.getComponent(placed, Health.class).health);
                }
            }
        }
    }

    @Test
    void runtimeSpawnsRemainWillingToFightAndAttackable() {
        try (var f = new Fixture()) {
            for (var spawnType : ICreaturesController.SpawnType.values()) {
                EntityId creature = f.factory.spawnCreature((short) 6, (short) 3, 1, new Vector2f(2, 3), spawnType);
                assertNull(f.data.getComponent(creature, Unattackable.class));
                assertNull(f.data.getComponent(creature, Peaceful.class));
            }
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
            IKwdFile kwd = proxy(IKwdFile.class, (method, args) -> switch (method) {
                case "getThings" -> List.of();
                case "getCreature" -> definition;
                case "getImp" -> imp;
                case "getGameLevel" -> CreatureSleepFixtures.level(4);
                default -> null;
            });
            IMapController map = proxy(IMapController.class, (method, args) -> { roomQueries++; return null; });
            factory = new CreaturesController(kwd, data,
                    Map.of(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, CreatureSleepFixtures.variable(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, 60)),
                    () -> now, null, map, null);
        }
        Thing.Creature thing(Kind kind, boolean willFight, boolean willBeAttacked) {
            var flags = EnumSet.noneOf(Thing.Creature.CreatureFlag.class);
            if (willFight) flags.add(Thing.Creature.CreatureFlag.WILL_FIGHT);
            if (willBeAttacked) flags.add(Thing.Creature.CreatureFlag.WILL_BE_ATTACKED);
            switch (kind) {
                case KEEPER -> {
                    var c = new Thing.KeeperCreature(); c.setPlayerId((short) 3); c.setCreatureId((short) 6);
                    c.setInitialHealth(100); c.setLevel((short) 1); c.setFlags(flags); return c;
                }
                case NEUTRAL -> {
                    var c = new Thing.NeutralCreature(); c.setCreatureId((short) 6);
                    c.setInitialHealth(100); c.setLevel((short) 1); c.setFlags(flags); return c;
                }
                default -> {
                    var c = new Thing.GoodCreature(); c.setCreatureId((short) 6);
                    c.setInitialHealth(100); c.setLevel((short) 1); c.setFlags(flags); return c;
                }
            }
        }
        @Override public void close() { data.close(); }
    }
    private static <T> T proxy(Class<T> type, java.util.function.BiFunction<String, Object[], Object> fn) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, m, a) -> fn.apply(m.getName(), a)));
    }
}
