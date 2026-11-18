/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.component;
import com.jme3.network.serializing.Serializer;
import com.jme3.network.serializing.serializers.FieldSerializer;
import com.simsilica.es.EntityId;
import com.simsilica.es.net.EntitySerializers;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DungeonHeartIntroductionSerializationTest {
    @Test void networkRoundTripRetainsPartIdsEpochPauseAndCompletion() throws Exception {
        Serializer.initialize();
        EntitySerializers.initialize();
        Serializer.registerClass(DungeonHeartIntroduction.class, new FieldSerializer());
        var source = new DungeonHeartIntroduction(null, new EntityId(1), new EntityId(2),
                new EntityId(3), new EntityId(4), new EntityId(5));
        source.startTime = 9_123_456_789L; source.pausedAt = 10_000_000_000L;
        source.waitingStartTime = 1_000_000_000L;
        source.ticksPerSecond = 4; source.complete = true;
        ByteBuffer bytes = ByteBuffer.allocate(1024);
        Serializer.writeClassAndObject(bytes, source); bytes.flip();
        var copy = (DungeonHeartIntroduction) Serializer.readClassAndObject(bytes);
        assertEquals(source.heart, copy.heart); assertEquals(source.arches, copy.arches);
        assertEquals(source.step0, copy.step0); assertEquals(source.step1, copy.step1);
        assertEquals(source.step2, copy.step2); assertEquals(source.startTime, copy.startTime);
        assertEquals(source.waitingStartTime, copy.waitingStartTime);
        assertEquals(source.pausedAt, copy.pausedAt); assertEquals(4, copy.ticksPerSecond);
        assertTrue(copy.complete); assertEquals(source.age(99_000_000_000L), copy.age(99_000_000_000L));
    }
}
