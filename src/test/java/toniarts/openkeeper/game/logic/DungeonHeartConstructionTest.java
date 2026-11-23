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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.logic;

import com.simsilica.es.*;
import com.simsilica.es.base.DefaultEntityData;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.*;

class DungeonHeartConstructionTest {
    private DefaultEntityData data;
    private AtomicLong now;
    private EntityId room, heart;
    private DungeonHeartConstruction construction;

    @BeforeEach void setup() {
        data = new DefaultEntityData();
        now = new AtomicLong(1_000_000_000L);
        room = assembly();
    }
    @AfterEach void cleanup() {
        if (construction != null) construction.stop();
        data.close();
    }
    private EntityId assembly() {
        EntityId id = data.createEntity();
        heart = data.createEntity();
        data.setComponent(heart, new Health(100, 100));
        data.setComponents(id, new RoomComponent((short) 5, false, new Point(10, 20)),
                new DungeonHeartIntroduction(null, heart, data.createEntity(),
                        data.createEntity(), data.createEntity(), data.createEntity()));
        return id;
    }
    private DungeonHeartIntroduction intro() { return data.getComponent(room, DungeonHeartIntroduction.class); }
    private void start(float delay) {
        construction = new DungeonHeartConstruction(data, delay, 4, now::get);
        construction.start();
    }
    private void advance(double seconds) {
        now.addAndGet(Math.round(seconds * 1e9));
        construction.processTick(0); // elapsed render tpf must not control the timeline
    }

    @Test void startsAtNativeDelayPlusOneGameTick() {
        start(15);
        assertEquals(16_250_000_000L, intro().startTime);
        assertEquals(now.get(), intro().waitingStartTime);
        assertEquals(4, intro().ticksPerSecond);
        assertEquals(-15.25, intro().age(now.get()));
        advance(22.5);
        assertFalse(intro().complete);
        assertEquals(32 / 255f, intro().opacity(now.get()), 1e-6);
        advance(1.75);
        assertTrue(intro().complete);
        assertEquals(1, intro().opacity(now.get()));
    }
    @Test void skippedUpdatesCompleteOnceWithoutResettingEpoch() {
        start(0);
        long epoch = intro().startTime;
        advance(30);
        DungeonHeartIntroduction completed = intro();
        advance(1);
        assertSame(completed, intro());
        assertEquals(epoch, intro().startTime);
        assertTrue(intro().complete);
    }
    @Test void pausePreservesAgeAndShiftsTheEpoch() {
        start(0);
        advance(3);
        construction.pause();
        long waitingStart = intro().waitingStartTime;
        double age = intro().age(now.get());
        advance(40);
        assertEquals(age, intro().age(now.get()));
        assertFalse(intro().complete);
        construction.resume();
        assertEquals(waitingStart + 40_000_000_000L, intro().waitingStartTime);
        assertEquals(age, intro().age(now.get()));
        advance(7);
        assertTrue(intro().complete);
    }
    @Test void reconstructionPreservesCompletedLifecycle() {
        start(0);
        advance(10);
        var old = intro();
        EntityId newHeart = data.createEntity();
        data.setComponent(newHeart, new Health(100, 100));
        data.setComponent(room, new DungeonHeartIntroduction(old, newHeart,
                data.createEntity(), data.createEntity(), data.createEntity(), data.createEntity()));
        advance(1);
        assertEquals(old.startTime, intro().startTime);
        assertTrue(intro().complete);
        assertEquals(newHeart, intro().heart);
    }
    @Test void destructionCancelsOnlyThatRoom() {
        start(0);
        EntityId other = assembly();
        EntityId otherHeart = heart;
        advance(1);
        data.setComponent(intro().heart, new Health(0, 100));
        advance(1);
        assertNull(intro());
        assertNotNull(data.getComponent(other, DungeonHeartIntroduction.class));
        assertNotNull(data.getComponent(otherHeart, Health.class));
    }
    @Test void temporarilyMissingFurnitureCannotEraseTheEpoch() {
        start(0);
        long epoch = intro().startTime;
        data.removeComponent(heart, Health.class);
        advance(1);
        assertEquals(epoch, intro().startTime);
        assertNotNull(intro());
    }
    @Test void roomDestructionCancelsPresentation() {
        start(0);
        data.setComponent(room, new RoomComponent((short) 5, true, new Point(10, 20)));
        advance(1);
        assertNull(intro());
    }
    @Test void unscheduledPartsStayHidden() {
        assertEquals(0, intro().opacity(now.get()));
        assertEquals(Double.NEGATIVE_INFINITY, intro().age(now.get()));
    }
    @Test void invalidClockConfigurationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DungeonHeartConstruction(data, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new DungeonHeartConstruction(data, Float.NaN, 4));
        assertThrows(IllegalArgumentException.class, () -> new DungeonHeartConstruction(data, -1, 4));
    }
}
