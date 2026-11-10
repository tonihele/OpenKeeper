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
package toniarts.openkeeper.game.controller;

import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerSlapCounterTest {
    @Test
    void creditsTheTargetOwnerForCreatureAndObjectSlaps() {
        try (EntityDataResource resource = new EntityDataResource()) {
            DefaultEntityData data = resource.data;
            PlayerStatsControl owner = new PlayerStatsControl(), other = new PlayerStatsControl();
            Map<Short,IPlayerController> players = Map.of((short) 3, player(owner), (short) 4, player(other));
            EntityId creature = data.createEntity();
            data.setComponents(creature, new CreatureComponent(), new Owner((short) 3, (short) 3));
            EntityId object = data.createEntity();
            data.setComponents(object, new ObjectComponent(), new Owner((short) 4, (short) 4));
            GameWorldController.recordSlap(data, players, creature);
            GameWorldController.recordSlap(data, players, object);
            assertEquals(1, owner.getSlapCount());
            assertEquals(1, other.getSlapCount());
        }
    }

    @Test
    void prisonerCreditBelongsToItsOwnerRatherThanItsJailer() {
        try (EntityDataResource resource = new EntityDataResource()) {
            DefaultEntityData data = resource.data;
            PlayerStatsControl owner = new PlayerStatsControl(), jailer = new PlayerStatsControl();
            Map<Short,IPlayerController> players = Map.of((short) 3, player(owner), (short) 4, player(jailer));
            EntityId captive = data.createEntity();
            data.setComponents(captive, new CreatureComponent(), new Owner((short) 3, (short) 4),
                    new CreatureImprisoned(0,0));
            GameWorldController.recordSlap(data, players, captive);
            assertEquals(1, owner.getSlapCount());
            assertEquals(0, jailer.getSlapCount());
        }
    }

    @Test
    void repeatedAcceptedSlapsCountEvenWithTheSameEffectTimestamp() {
        try (EntityDataResource resource = new EntityDataResource()) {
            DefaultEntityData data = resource.data;
            PlayerStatsControl stats = new PlayerStatsControl();
            EntityId id = data.createEntity();
            data.setComponents(id, new CreatureComponent(), new Owner((short) 3, (short) 3), new Slapped(1));
            Map<Short,IPlayerController> players = Map.of((short) 3, player(stats));
            GameWorldController.recordSlap(data, players, id);
            data.setComponent(id, new Slapped(1));
            GameWorldController.recordSlap(data, players, id);
            stats.creatureSlapped(null); // Existing SlapSystem per-type bookkeeping is separate.
            assertEquals(2, stats.getSlapCount());
            data.removeComponent(id, Slapped.class);
            assertEquals(2, stats.getSlapCount());
        }
    }

    @Test
    void missingOwnersAndNonparticipatingOwnersDoNotCreditAnotherPlayer() {
        try (EntityDataResource resource = new EntityDataResource()) {
            DefaultEntityData data = resource.data;
            PlayerStatsControl stats = new PlayerStatsControl();
            EntityId id = data.createEntity();
            Map<Short,IPlayerController> players = Map.of((short) 3, player(stats));
            GameWorldController.recordSlap(data, players, id);
            data.setComponent(id, new Owner((short) 4, (short) 4));
            GameWorldController.recordSlap(data, players, id);
            assertEquals(0, stats.getSlapCount());
        }
    }

    @Test
    void concurrentAcceptedInteractionsDoNotLoseCounterUpdates() throws Exception {
        PlayerStatsControl stats = new PlayerStatsControl();
        try (var executor = Executors.newFixedThreadPool(4)) {
            var results = new ArrayList<Future<?>>();
            for (int worker = 0; worker < 4; worker++) {
                results.add(executor.submit(() -> {
                    for (int slap = 0; slap < 1000; slap++) stats.entitySlapped();
                }));
            }
            for (Future<?> result : results) result.get();
        }
        assertEquals(4000, stats.getSlapCount());
        assertEquals(0, new PlayerStatsControl().getSlapCount());
    }

    private static final class EntityDataResource implements AutoCloseable {
        final DefaultEntityData data = new DefaultEntityData();
        @Override public void close() { data.close(); }
    }

    private static IPlayerController player(PlayerStatsControl stats) {
        return (IPlayerController) Proxy.newProxyInstance(IPlayerController.class.getClassLoader(),
                new Class<?>[]{IPlayerController.class}, (p,m,a) -> {
                    assertEquals("getStatsControl", m.getName());
                    return stats;
                });
    }
}
