/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VertexClipBakeTest {
    @Test void velocityErrorIncludesEveryIntervalAndDoesNotInventWrapVelocity() {
        var reference=VertexClip.of(1,new float[]{.1f,.2f,.4f},new float[]{0,0,0,1,0,0,3,0,0});
        var changed=VertexClip.of(1,new float[]{.1f,.2f,.4f},new float[]{0,0,0,1.01f,0,0,3,0,0});
        assertEquals(0,VertexClipBake.velocityError(reference,reference));
        assertEquals(.1,VertexClipBake.velocityError(reference,changed),1e-6);
        var changedTime=VertexClip.of(1,new float[]{.1f,.3f,.4f},new float[]{0,0,0,1,0,0,3,0,0});
        assertThrows(IllegalArgumentException.class,()->VertexClipBake.velocityError(reference,changedTime));
        var changedOrigin=VertexClip.of(1,new float[]{0,.2f,.4f},new float[]{0,0,0,1,0,0,3,0,0});
        assertThrows(IllegalArgumentException.class,()->VertexClipBake.velocityError(reference,changedOrigin));
    }
}
