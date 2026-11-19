/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.logic;

import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.tools.convert.map.Creature.AnimationType;
import static org.junit.jupiter.api.Assertions.*;

class CreatureMotionPublicationTest {
    @Test void haulingPreservesIntegrationEpochAndPublishesAttachmentUntilJobEnds() {
        var data = new DefaultEntityData();
        try {
            var carrier = data.createEntity(); var victim = data.createEntity();
            var initial = new Position(.3f, 1, .25f, 2);
            data.setComponents(carrier, new Position(0, 5, 3, 6), new TaskComponent());
            data.setComponents(victim, initial, new HauledBy(carrier),
                    new CreatureViewState((short) 1, 0, AnimationType.WALK));
            var hauling = new HaulingSystem(data);
            var view = new CreatureViewSystem(data, null, null, true);
            try {
                hauling.processTick(.05f); view.processTick(.05f);
                var followed = data.getComponent(victim, Position.class);
                assertEquals(initial.motionEpoch, followed.motionEpoch);
                assertEquals(5, followed.position.x); assertEquals(6, followed.position.z);
                assertEquals(.25f, followed.position.y); assertEquals(.3f, followed.rotation);
                var attached = data.getComponent(victim, CreatureMotion.class);
                assertTrue(attached.immediate); assertEquals(carrier, attached.hauler);
                data.removeComponent(carrier, TaskComponent.class);
                hauling.processTick(.05f); view.processTick(.05f);
                assertNull(data.getComponent(victim, HauledBy.class));
                var detached = data.getComponent(victim, CreatureMotion.class);
                assertFalse(detached.immediate); assertNull(detached.hauler);
                assertEquals(carrier, attached.hauler);
                assertSame(followed, data.getComponent(victim, Position.class));
            } finally { view.stop(); hauling.stop(); }
        } finally { data.close(); }
    }

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
