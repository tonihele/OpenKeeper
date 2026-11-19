/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.animation;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.*;
import static org.junit.jupiter.api.Assertions.*;

class LocomotionVariationTest {
    private final ArtResource art = new ArtResource("walk",ArtResource.ArtResourceType.ANIMATING_MESH);
    @Test void seededPhasesAreStableAndSpreadAcrossTheCrowd() {
        int[] bins = new int[16];
        for (int i=1;i<=512;i++) {
            double phase=phase(i,5,Creature.AnimationType.WALK,true,AnimationControl.PlaybackPolicy.LOOP,1);
            assertEquals(phase,phase(i,5,Creature.AnimationType.WALK,true,AnimationControl.PlaybackPolicy.LOOP,1));
            assertTrue(phase>=0 && phase<1); bins[(int)(phase*16)]++;
        }
        for (int count : bins) assertTrue(count>=12 && count<=55,"Unexpected bin population "+count);
        assertNotEquals(phase(1,5,Creature.AnimationType.WALK,true,AnimationControl.PlaybackPolicy.LOOP,1),
                phase(1,6,Creature.AnimationType.WALK,true,AnimationControl.PlaybackPolicy.LOOP,1));
    }
    @Test void classicGameplayTimedAndSegmentedActionsRetainOriginalOrigin() {
        for (var action : Creature.AnimationType.values()) if (action!=Creature.AnimationType.WALK && action!=Creature.AnimationType.RUN && action!=Creature.AnimationType.STEALTH_WALK)
            assertEquals(0,phase(1,5,action,true,AnimationControl.PlaybackPolicy.LOOP,1));
        assertEquals(0,phase(1,5,Creature.AnimationType.WALK,false,AnimationControl.PlaybackPolicy.LOOP,1));
        assertEquals(0,phase(1,5,Creature.AnimationType.WALK,true,AnimationControl.PlaybackPolicy.ONCE,1));
        assertEquals(0,phase(1,5,Creature.AnimationType.WALK,true,AnimationControl.PlaybackPolicy.LOOP,0));
        art.getFlags().add(ArtResource.ArtResourceFlag.HAS_START_ANIMATION);
        assertEquals(0,phase(1,5,Creature.AnimationType.WALK,true,AnimationControl.PlaybackPolicy.LOOP,1));
        art.getFlags().clear(); art.getFlags().add(ArtResource.ArtResourceFlag.ANIMATING_TEXTURE);
        assertEquals(0,phase(1,5,Creature.AnimationType.WALK,true,AnimationControl.PlaybackPolicy.LOOP,1));
    }
    private double phase(long id,long sequence,Creature.AnimationType action,boolean enabled,AnimationControl.PlaybackPolicy policy,double period) {
        return LocomotionVariation.offset(enabled,id,sequence,(short)1,action,art,policy,period);
    }
}
