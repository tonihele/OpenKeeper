/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.logic;

import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.tools.convert.map.Creature.AnimationType;
import static org.junit.jupiter.api.Assertions.*;

class CreatureMotionPublicationTest {
    @Test void publicationIsOptInAndBindsObservedRootAndRequestToAuthoritativeTime() {
        var data = new DefaultEntityData();
        try {
            var id = data.createEntity(); var position = new Position(0, 1, 2, 3);
            data.setComponents(id, position, new CreatureViewState((short)1, 0, AnimationType.WALK));
            var legacy = new CreatureViewSystem(data, null); legacy.processTick(.05f);
            assertNull(data.getComponent(id, CreatureMotion.class)); legacy.stop();
            var system = new CreatureViewSystem(data, null, null, true); system.processTick(.05f);
            var first = data.getComponent(id, CreatureMotion.class);
            assertEquals(.05, first.time, 1e-6); assertEquals(3, first.z); assertEquals(position.motionEpoch, first.epoch);
            assertSame(position, data.getComponent(id, Position.class));
            data.setComponent(id, new Death()); system.processTick(.05f);
            var second = data.getComponent(id, CreatureMotion.class);
            assertEquals(AnimationType.DEATH_POSE, second.request.state); assertTrue(second.sequence > first.sequence);
            assertEquals(AnimationType.WALK, first.request.state);
            data.removeComponent(id, Position.class); system.processTick(.05f);
            assertNull(data.getComponent(id, CreatureMotion.class)); system.stop();
        } finally { data.close(); }
    }
}
