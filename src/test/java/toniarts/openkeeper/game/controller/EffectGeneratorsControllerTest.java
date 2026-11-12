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
package toniarts.openkeeper.game.controller;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.WorldUtils;
import static toniarts.openkeeper.tools.convert.map.EffectGeneratorFixtures.generator;
import static org.junit.jupiter.api.Assertions.*;

class EffectGeneratorsControllerTest {
    @Test
    void startsEnabledAndEmitsOnlyAfterItsDeadline() {
        var f = new Fixture(generator(7, 2, 0, 0, 100, 100, 100, 100));
        f.tick(0);
        assertTrue(f.sent.isEmpty());
        f.tick(1);
        f.tick(3);
        assertEquals(1, f.sent.size());
        f.tick(3.01);
        assertEquals(2, f.sent.size());
    }

    @Test
    void disablingAndRepeatedEnablingPreserveThePendingEmissionTime() {
        var f = new Fixture(generator(7, 5, 0, 0, 100, 100, 100, 100));
        f.tick(1);
        f.control.setEnabled((short) 7, false);
        f.tick(2);
        f.control.setEnabled((short) 7, true);
        f.tick(3);
        f.control.setEnabled((short) 7, true);
        f.tick(6);
        assertEquals(1, f.sent.size());
        f.control.setEnabled((short) 7, false);
        f.tick(10);
        assertEquals(1, f.sent.size());
        f.control.setEnabled((short) 7, true);
        f.tick(10.1);
        assertEquals(2, f.sent.size());
    }

    @Test
    void emptySlotsRetryNextTickWithoutSchedulingADelay() {
        var f = new Fixture(generator(7, 100, 0, 0, 100, 0, 0, 200));
        f.random.values.addAll(List.of(1, 3, 0));
        f.tick(1);
        assertTrue(f.sent.isEmpty());
        f.tick(1.1);
        assertEquals(200, f.sent.getFirst()[0]);
    }

    @Test
    void togglesStoredIdsDirectlyIncludingZeroAndIgnoresMissingIds() {
        var f = new Fixture(generator(0, 0, 0, 0, 100, 100, 100, 100),
                generator(7, 0, 0, 0, 200, 200, 200, 200));
        f.control.setEnabled((short) 99, false);
        f.control.setEnabled((short) 0, false);
        f.tick(1);
        assertEquals(1, f.sent.size());
        assertEquals(200, f.sent.getFirst()[0]);
    }

    @Test
    void zeroFrequencyAndDelayedTicksEmitAtMostOncePerGeneratorPerTick() {
        var f = new Fixture(generator(7, 0, 0, 0, 100, 100, 100, 100));
        f.tick(1);
        f.tick(100);
        assertEquals(2, f.sent.size());
        f.control.stop();
        f.tick(200);
        assertEquals(2, f.sent.size());
    }

    @Test
    void choosesAnExactPositionAndYawWithinTheConfiguredRectangle() {
        var f = new Fixture(generator(7, 1, 2, 3, 100, 100, 100, 100));
        f.random.values.addAll(List.of(0, 4095, 8191, 1024));
        f.tick(1);
        var emission = f.sent.getFirst();
        assertEquals(new Vector3f(10 + 4095 / 4096f, WorldUtils.FLOOR_HEIGHT, 20 + 8191 / 4096f), emission[1]);
        assertEquals(FastMath.PI, (float) emission[2], .00001f);
    }

    @Test
    void solidAndOutOfMapPositionsRetryWithoutAdvancingTheDeadline() {
        var f = new Fixture(generator(7, 100, 0, 0, 100, 100, 100, 100));
        f.solid = true;
        f.tick(1);
        f.solid = false;
        f.outside = true;
        f.tick(1.1);
        assertTrue(f.sent.isEmpty());
        f.outside = false;
        f.tick(1.2);
        assertEquals(1, f.sent.size());
    }

    private static final class Fixture {
        double time;
        boolean solid;
        boolean outside;
        final List<Object[]> sent = new ArrayList<>();
        final ControlledRandom random = new ControlledRandom();
        final EffectGeneratorsController control;

        Fixture(Thing.EffectGenerator... things) {
            var tile = proxy(IMapTileInformation.class, (p, m, a) -> null);
            var data = proxy(IMapDataInformation.class, (p, m, a) -> outside ? null : tile);
            var map = proxy(IMapInformation.class, (p, m, a) -> switch (m.getName()) {
                case "getMapData" -> data;
                case "isSolid" -> solid;
                default -> throw new UnsupportedOperationException(m.getName());
            });
            var timer = proxy(IGameTimer.class, (p, m, a) -> time);
            var events = proxy(GameEventService.class, (p, m, a) -> {
                assertEquals("emitEffect", m.getName());
                sent.add(a);
                return null;
            });
            control = new EffectGeneratorsController(Arrays.asList(things), map, timer, events, random);
        }

        void tick(double time) {
            this.time = time;
            control.processTick(.05f);
        }
    }

    private static final class ControlledRandom extends Random {
        final ArrayDeque<Integer> values = new ArrayDeque<>();
        @Override public int nextInt(int bound) {
            int value = values.isEmpty() ? 0 : values.removeFirst();
            assertTrue(value >= 0 && value < bound);
            return value;
        }
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
