/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnimationUpdateErrorAuditTest {
    @Test void fullRateIsExactAndHeldRateIncludesLoopSeam() {
        var clip=VertexClip.of(1,new float[]{.25f,.5f,.75f,1},new float[]{0,0,0,1,0,0,2,0,0,3,0,0});
        var full=AnimationUpdateErrorAudit.measure(clip,120,120,0);
        assertEquals(0,full.maxRelativeError()); assertEquals(full.samples(),full.writes());
        var reduced=AnimationUpdateErrorAudit.measure(clip,120,15,.375);
        assertTrue(reduced.maxRelativeError()>.9,"Loop reset must be included, not just smooth intervals");
        assertTrue(reduced.writes()<reduced.samples()/4);
    }
    @Test void staticClipHasZeroErrorAtAllStaggersAndInvalidSchedulesFail() {
        var clip=VertexClip.of(1,new float[]{.1f,.2f},new float[]{2,3,4,2,3,4});
        for (int stagger=0;stagger<8;stagger++) assertEquals(0,AnimationUpdateErrorAudit.measure(clip,144,15,stagger/8.0).maxRelativeError());
        assertThrows(IllegalArgumentException.class,()->AnimationUpdateErrorAudit.measure(clip,60,61,0));
        assertThrows(IllegalArgumentException.class,()->AnimationUpdateErrorAudit.measure(clip,60,15,1));
    }
}
