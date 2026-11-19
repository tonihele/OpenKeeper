/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VertexRepresentationAuditTest {
    @Test void repeatedFramesRetainSourceTimeAndIncludeLookupCost() {
        var clip = VertexClip.of(1, new float[]{2, 3, 4, 5}, new float[]{
                0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0});
        var result = VertexRepresentationAudit.inspect(clip);
        assertEquals(2, result.uniqueFrames());
        assertEquals(56, result.deduplicatedPrimitiveBytes());
        assertEquals(24, result.halfPositionBytes());
        assertEquals(0, result.halfComponentError()); assertEquals(0, result.halfVelocityError());
        var store = new float[3]; clip.sample(1.5, store); assertEquals(1, store[0]);
        assertEquals(64, clip.decodedBytes(), "The audit must not change the source representation");
    }

    @Test void binary16MagnitudeRoundingCanExpandBoundsAndAmplifyVelocityError() {
        float value = 1.0006f;
        var clip = VertexClip.of(1, new float[]{0, .01f}, new float[]{0, 0, 0, value, 0, 0});
        var result = VertexRepresentationAudit.inspect(clip);
        double error = Float.float16ToFloat(Float.floatToFloat16(value)) - (double) value;
        assertTrue(error > 0);
        assertEquals(error, result.halfComponentError(), 1e-12);
        assertEquals(error / .01f, result.halfVelocityError(), 1e-9);
        assertEquals(1, result.halfOutsideSourceBounds());
        assertTrue(result.deduplicatedPrimitiveBytes() > clip.decodedBytes(), "Unique clips pay for an unnecessary lookup");
    }

    @Test void exactDeduplicationDistinguishesSignedZeroAndRejectsHalfOverflow() {
        var clip = VertexClip.of(1, new float[]{0, 1}, new float[]{-0f, 0, 0, 0f, 0, 0});
        assertEquals(2, VertexRepresentationAudit.inspect(clip).uniqueFrames());
        var overflow = VertexClip.of(1, new float[]{0}, new float[]{100_000, 0, 0});
        assertThrows(IllegalArgumentException.class, () -> VertexRepresentationAudit.inspect(overflow));
    }
}
