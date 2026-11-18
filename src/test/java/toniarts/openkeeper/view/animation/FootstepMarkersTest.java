/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FootstepMarkersTest {
    @Test void countsLowFrameRateMultiWrapReverseAndOnceWithoutDuplicates() {
        var markers = new FootstepMarkers(new int[]{1,3},4);
        assertEquals(6,markers.crossings(0,3,1,LoopMode.Loop));
        assertEquals(1,markers.crossings(0,.25,1,LoopMode.Loop));
        assertEquals(0,markers.crossings(.25,.25,1,LoopMode.Loop));
        assertEquals(0,markers.crossings(.25,.5,1,LoopMode.Loop));
        assertEquals(2,markers.crossings(0,3,1,LoopMode.DontLoop));
        assertEquals(4,markers.crossings(0,2,1,LoopMode.Cycle));
        assertEquals(2,markers.crossings(1,2,1,LoopMode.Cycle));
        assertEquals(0,markers.crossings(1,0,1,LoopMode.Loop)); // seek resets the caller cursor
        assertEquals(3,new FootstepMarkers(new int[]{1,1},4).crossings(0,3,1,LoopMode.Loop));
    }
}
