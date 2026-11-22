/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.game.component;

import com.jme3.network.serializing.serializers.FieldSerializer;
import com.jme3.network.serializing.Serializer;
import org.junit.jupiter.api.BeforeAll;
import java.nio.ByteBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class DoorStateSerializationTest {
    @BeforeAll
    static void initializeNetworkSerializers() {
        Serializer.initialize();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void discoverySurvivesTheRegisteredFieldSerializer(boolean hidden) throws Exception {
        FieldSerializer serializer = new FieldSerializer();
        serializer.initialize(DoorComponent.class);
        serializer.initialize(DoorViewState.class);
        ByteBuffer buffer = ByteBuffer.allocate(256);
        serializer.writeObject(buffer, new DoorComponent((short) 4, true, false, hidden));
        serializer.writeObject(buffer, new DoorViewState((short) 4, true, false, true, hidden));
        buffer.flip();
        DoorComponent logical = serializer.readObject(buffer, DoorComponent.class);
        DoorViewState view = serializer.readObject(buffer, DoorViewState.class);
        assertEquals(hidden, logical.hidden);
        assertEquals(hidden, view.hidden);
        assertEquals(4, logical.doorId);
        assertEquals(4, view.doorId);
        assertTrue(logical.locked);
        assertTrue(view.locked);
        assertTrue(view.open);
        assertFalse(logical.blueprint);
        assertFalse(view.blueprint);
        assertFalse(buffer.hasRemaining());
    }
}
