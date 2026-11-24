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
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.tools.convert.map.GameObject;
import static org.junit.jupiter.api.Assertions.*;

class SpecialIdleEffectSystemTest {
    @Test
    void usesSixteenSimulationTicksAndStaggersBoxesWithoutHover() {
        try (var f = new Fixture()) {
            EntityId first = f.box(), second = f.box();
            List<Integer> ticks = new ArrayList<>();
            for (int tick = 0; tick < 32; tick++) {
                int before = f.positions.size();
                f.tick(tick);
                if (f.positions.size() > before) ticks.add(tick);
                f.tick(tick); // Rendering/repeated calls during the same game tick add nothing.
            }
            assertEquals(4, f.positions.size());
            assertEquals(16, ticks.get(2) - ticks.get(0));
            assertEquals(16, ticks.get(3) - ticks.get(1));
            assertNotEquals(first.getId() & 15, second.getId() & 15);
        }
    }

    @Test
    void hiddenHandConsumedAndOtherStatesDoNotEmitAndDropResumesOnWorldPhase() {
        try (var f = new Fixture()) {
            EntityId id = f.box();
            int phase = (int) (-id.getId() & 15);
            f.data.setComponent(id, new InHand());
            f.tick(phase);
            f.data.removeComponent(id, InHand.class);
            f.data.setComponent(id, state(GameObject.State.SPECIAL, false));
            f.tick(phase + 16);
            f.data.setComponent(id, state(GameObject.State.BEING_DROPPED, true));
            f.tick(phase + 32);
            f.data.setComponent(id, state(GameObject.State.SPECIAL, true));
            f.data.removeComponent(id, Position.class);
            f.tick(phase + 48);
            assertTrue(f.positions.isEmpty());
            f.data.setComponent(id, new Position(0, new Vector3f(5, 1, 7)));
            f.tick(phase + 64);
            assertEquals(List.of(new Vector3f(5, 1, 7)), f.positions);
            f.data.removeEntity(id);
            f.tick(phase + 80);
            assertEquals(1, f.positions.size());
        }
    }

    @Test
    void storedAndHauledSpecialsEmitAtTheirCurrentPositionButLavaSuppressesThem() {
        try (var f = new Fixture()) {
            EntityId id = f.box();
            int phase = (int) (-id.getId() & 15);
            f.data.setComponent(id, new Stored(new EntityId(100), AbstractRoomController.ObjectType.SPECIAL));
            f.tick(phase);
            f.data.removeComponent(id, Stored.class);
            f.data.setComponent(id, new HauledBy(new EntityId(101)));
            f.data.setComponent(id, new Position(0, new Vector3f(8, 1, 9)));
            f.tick(phase + 16);
            assertEquals(List.of(new Vector3f(2, 1, 3), new Vector3f(8, 1, 9)), f.positions);
            f.lava = true;
            f.tick(phase + 32);
            assertEquals(2, f.positions.size());
        }
    }

    private static ObjectViewState state(GameObject.State state, boolean visible) {
        return new ObjectViewState((short) 117, state, ObjectViewState.GameObjectAnimState.MESH_RESOURCE, visible);
    }

    private static final class Fixture implements AutoCloseable {
        final DefaultEntityData data = new DefaultEntityData();
        final List<Vector3f> positions = new ArrayList<>();
        double time;
        boolean lava;
        final SpecialIdleEffectSystem system;

        Fixture() {
            var map = (IMapInformation<?>) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{IMapInformation.class}, (p, m, a) -> {
                        assertEquals("isLava", m.getName());
                        return lava;
                    });
            var events = (GameEventService) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{GameEventService.class}, (p, m, a) -> {
                        assertEquals("emitEffect", m.getName());
                        assertEquals(204, a[0]);
                        assertEquals(0f, a[2]);
                        positions.add((Vector3f) a[1]);
                        return null;
                    });
            system = new SpecialIdleEffectSystem(data, () -> time, 4, map, events);
        }

        EntityId box() {
            EntityId id = data.createEntity();
            data.setComponents(id, state(GameObject.State.SPECIAL, true), new Position(0, new Vector3f(2, 1, 3)));
            return id;
        }

        void tick(int tick) { time = tick / 4.0; system.processTick(.25f); }
        @Override public void close() { system.stop(); data.close(); }
    }
}
