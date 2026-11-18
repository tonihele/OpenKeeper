/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.animation;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.math.Vector3f;
import com.jme3.scene.*;
import java.nio.FloatBuffer;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.animation.*;
import toniarts.openkeeper.game.sound.FootstepSounds;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.map.AnimationFixtures;
import static org.junit.jupiter.api.Assertions.*;

class TimedClipTest {
    @Test void authoredRatesMapToKeysAndHoldTheLastFrameForOneInterval() {
        for (int fps : new int[]{15,18,20,30,38,40,50}) {
            var resource = AnimationFixtures.resource("fixture", 3, fps);
            Geometry geometry = geometry();
            Pose a = new Pose("a", new Vector3f[]{new Vector3f()}, new int[]{0});
            Pose b = new Pose("b", new Vector3f[]{new Vector3f(2,0,0)}, new int[]{0});
            var track = new PoseTrack(geometry, new float[]{1f/30,2f/30,3f/30}, new PoseTrack.PoseFrame[]{
                new PoseTrack.PoseFrame(new Pose[]{a,b},new float[]{0}),
                new PoseTrack.PoseFrame(new Pose[]{a,b},new float[]{.5f}),
                new PoseTrack.PoseFrame(new Pose[]{a,b},new float[]{1})});
            var composer = new AnimComposer(); var clip = new AnimClip(KmfModelLoader.DUMMY_ANIM_CLIP_NAME);
            clip.setTracks(new PoseTrack[]{track}); composer.addAnimClip(clip);
            var timed = new TimedClip(composer, resource, true);
            assertEquals(3.0/fps, timed.getLength(), 1e-9);
            timed.interpolate(1.0/fps); assertEquals(1, x(geometry), 1e-5);
            timed.interpolate(2.5/fps); assertEquals(2, x(geometry), 1e-5);
            assertEquals(timed.getLength(), AnimationTiming.duration(resource, 0));
            assertEquals(FootstepSounds.STEP_A, FootstepSounds.getStep(new int[]{1}, .9f/fps, 1.1f/fps, 3f/fps, fps));
        }
    }
    @Test void invalidOrAbsentMetadataNeverProducesInfiniteDurations() {
        assertEquals(.1, AnimationTiming.duration(AnimationFixtures.resource("bad",3,0), 1), 1e-9);
        assertEquals(1, AnimationTiming.duration(AnimationFixtures.resource("empty",0,30), 1));
        assertEquals(0, AnimationTiming.duration(null, 0));
    }
    private static Geometry geometry() {
        Mesh mesh = new Mesh(); mesh.setBuffer(VertexBuffer.Type.Position,3,new float[]{0,0,0});
        mesh.setBuffer(VertexBuffer.Type.BindPosePosition,3,new float[]{0,0,0}); return new Geometry("fixture",mesh);
    }
    private static float x(Geometry geometry) { return ((FloatBuffer)geometry.getMesh().getBuffer(VertexBuffer.Type.Position).getData()).get(0); }
}
