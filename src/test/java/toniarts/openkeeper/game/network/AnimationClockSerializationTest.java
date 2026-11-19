/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.network;

import com.jme3.network.serializing.Serializer;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.SimulationTime;
import toniarts.openkeeper.tools.convert.map.Creature;
import static org.junit.jupiter.api.Assertions.*;

class AnimationClockSerializationTest {
    @Test void registeredClockAndRequestSequenceRoundTripThroughNetworkSerialization() throws Exception {
        Serializer.initialize();
        var initialize = NetworkServer.class.getDeclaredMethod("initialize"); initialize.setAccessible(true); initialize.invoke(null);
        ByteBuffer bytes = ByteBuffer.allocate(4096);
        Serializer.writeClassAndObject(bytes, new SimulationTime(12.5,.05));
        Serializer.writeClassAndObject(bytes, new CreatureViewState((short)7,12,Creature.AnimationType.WALK,42));
        var position = new toniarts.openkeeper.game.component.Position(.5f, 1, 2, 3);
        Serializer.writeClassAndObject(bytes, position);
        Serializer.writeClassAndObject(bytes, new toniarts.openkeeper.game.component.CreatureMotion(12.5, .05, 44, position,
                new CreatureViewState((short)7,12,Creature.AnimationType.WALK,42), true));
        bytes.flip();
        SimulationTime clock = (SimulationTime) Serializer.readClassAndObject(bytes);
        CreatureViewState request = (CreatureViewState) Serializer.readClassAndObject(bytes);
        var decodedPosition = (toniarts.openkeeper.game.component.Position)Serializer.readClassAndObject(bytes);
        var motion = (toniarts.openkeeper.game.component.CreatureMotion)Serializer.readClassAndObject(bytes);
        assertEquals(12.5,clock.seconds); assertEquals(.05,clock.tickSeconds);
        assertEquals(42,request.sequence); assertEquals(12,request.stateStartTime);
        assertEquals(Creature.AnimationType.WALK,request.state);
        assertEquals(position.motionEpoch, decodedPosition.motionEpoch);
        assertEquals(position.motionEpoch, motion.epoch); assertEquals(44, motion.sequence);
        assertEquals(12.5, motion.time); assertEquals(3, motion.z); assertTrue(motion.immediate);
        assertEquals(42, motion.request.sequence); assertEquals(Creature.AnimationType.WALK, motion.request.state);
    }
}
