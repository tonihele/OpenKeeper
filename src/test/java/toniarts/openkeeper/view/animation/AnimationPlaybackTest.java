/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.animation;

import com.jme3.anim.AnimComposer;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import com.jme3.renderer.*;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.*;
import static org.junit.jupiter.api.Assertions.*;

class AnimationPlaybackTest {
    private static com.jme3.scene.Spatial poseModel(String name, ArtResource resource) {
        var wrapper = (Node)AnimationLoaderTest.model(name,resource); var model = (Node)wrapper.getChild(0);
        var mesh = new com.jme3.scene.Mesh();
        mesh.setBuffer(com.jme3.scene.VertexBuffer.Type.Position,3,new float[]{0,0,0});
        mesh.setBuffer(com.jme3.scene.VertexBuffer.Type.BindPosePosition,3,new float[]{0,0,0});
        var geometry = new com.jme3.scene.Geometry("vertex",mesh); model.attachChild(geometry);
        var a = new toniarts.openkeeper.animation.Pose("a",new com.jme3.math.Vector3f[]{new com.jme3.math.Vector3f()},new int[]{0});
        var b = new toniarts.openkeeper.animation.Pose("b",new com.jme3.math.Vector3f[]{new com.jme3.math.Vector3f(2,0,0)},new int[]{0});
        var frames = new toniarts.openkeeper.animation.PoseTrack.PoseFrame[3];
        for (int i=0;i<3;i++) frames[i] = new toniarts.openkeeper.animation.PoseTrack.PoseFrame(new toniarts.openkeeper.animation.Pose[]{a,b},new float[]{i/2f});
        model.getControl(AnimComposer.class).getAnimClip(toniarts.openkeeper.tools.convert.KmfModelLoader.DUMMY_ANIM_CLIP_NAME)
                .setTracks(new toniarts.openkeeper.animation.PoseTrack[]{new toniarts.openkeeper.animation.PoseTrack(geometry,new float[]{0,1f/30,2f/30},frames)});
        return wrapper;
    }
    private static float x(Node root) {
        var geometry = (com.jme3.scene.Geometry)((Node)root.getChild("pose")).getChild("vertex");
        return ((java.nio.FloatBuffer)geometry.getMesh().getBuffer(com.jme3.scene.VertexBuffer.Type.Position).getData()).get(0);
    }
    @Test void explicitTerminalFrameIsFiniteAndHeldRegardlessOfLoopPolicy() {
        for (var policy : AnimationControl.PlaybackPolicy.values()) {
            for (boolean segments : new boolean[]{false,true}) {
                Node root = new Node(); var listener = new Listener(); listener.policy = policy; root.addControl(listener);
                var resource = AnimationFixtures.resource("pose",3,30);
                if (segments) resource.getFlags().addAll(java.util.EnumSet.of(
                        ArtResource.ArtResourceFlag.HAS_START_ANIMATION,ArtResource.ArtResourceFlag.HAS_END_ANIMATION));
                assertEquals(AnimationLoader.PlaybackResult.STARTED,
                        AnimationLoader.playAnimation(root,resource,AnimationPlaybackTest::poseModel,true));
                assertEquals(2,x(root),1e-6,"terminal pose for "+policy);
                String visible = segments && policy != AnimationControl.PlaybackPolicy.HOLD ? "End" : "pose";
                assertNotEquals(com.jme3.scene.Spatial.CullHint.Always,root.getChild(visible).getCullHint());
                assertTrue(AnimationLoader.getPlaybackAge(root) < 1,"bounded terminal age");
                root.updateLogicalState(1);
                assertEquals(2,x(root),1e-6);
                assertEquals(0,listener.cycles); assertEquals(0,listener.stops);
            }
        }
    }
    @Test void renderedPoseAndClonedBuffersRemainIndependentUnderTheSharedClock() {
        var resource = AnimationFixtures.resource("pose",3,30); Node root = new Node(); double[] time = {10};
        AnimationLoader.playAnimation(root,resource,AnimationPlaybackTest::poseModel,false);
        AnimationLoader.bindPlaybackClock(root,()->time[0],10);
        time[0] = 20.035; root.updateLogicalState(5); assertEquals(1.05,x(root),1e-4);
        root.updateLogicalState(5); assertEquals(1.05,x(root),1e-4);
        var clone = (Node)root.deepClone(); clone.updateLogicalState(.01f);
        assertEquals(1.05,x(root),1e-4); assertEquals(1.35,x(clone),1e-4);
        AnimationLoader.pauseAnimations(root); time[0] += .02; root.updateLogicalState(5);
        assertEquals(1.05,x(root),1e-4);
        AnimationLoader.resumeAnimations(root); root.updateLogicalState(0); assertEquals(1.65,x(root),1e-4);
    }
    @Test void genericNonEntityOwnerFlushesACompletedHiddenOneShotOnReentry() {
        Node root = new Node(); var listener = new Listener(); listener.policy = AnimationControl.PlaybackPolicy.HOLD; root.addControl(listener);
        var resource = AnimationFixtures.resource("pose",3,30);
        AnimationLoader.playAnimation(root,resource,AnimationPlaybackTest::poseModel,false);
        root.setCullHint(com.jme3.scene.Spatial.CullHint.Always); root.updateLogicalState(.2f);
        assertEquals(0,x(root)); assertEquals(1,listener.cycles);
        root.setCullHint(com.jme3.scene.Spatial.CullHint.Dynamic); root.updateLogicalState(0);
        assertEquals(2,x(root)); assertEquals(1,listener.cycles);
    }
    private static final class Listener extends AbstractControl implements AnimationControl {
        int cycles, stops;
        PlaybackPolicy policy = PlaybackPolicy.LOOP;
        @Override public PlaybackPolicy getPlaybackPolicy() { return policy; }
        @Override public boolean isStopAnimation() { return policy != PlaybackPolicy.LOOP; }
        @Override public void onAnimationCycleDone() { cycles++; }
        @Override public void onAnimationStop() { stops++; }
        @Override protected void controlUpdate(float tpf) { }
        @Override protected void controlRender(RenderManager rm, ViewPort vp) { }
    }
    @Test void cadenceAndMarkerCountsAreIndependentOfRefreshRate() {
        for (int hz : new int[]{25,30,60,120,144}) {
            for (int frames : new int[]{3,12}) {
                var resource = AnimationFixtures.resource("cadence",frames,frames == 3 ? 30 : 20);
                Node root = new Node(); var listener = new Listener(); root.addControl(listener);
                AnimationLoader.playAnimation(root,resource,AnimationLoaderTest::model,false);
                double duration = frames == 3 ? .1 : .6;
                double seconds = duration * 100;
                var markers = new FootstepMarkers(new int[]{1,2},frames == 3 ? 30 : 20);
                long crossings = 0; double previous = 0, delivered = 0;
                for (int i=0;i<(int)Math.round(hz*seconds);i++) {
                    // Deliver equal elapsed time, without accumulating float representation error.
                    float delta = (float)((i+1)/(double)hz-delivered);
                    delivered += delta; root.updateLogicalState(delta);
                    double age = AnimationLoader.getPlaybackAge(root);
                    crossings += markers.crossings(previous,age,duration,LoopMode.Loop); previous = age;
                }
                assertEquals(100,listener.cycles,"hz="+hz+" frames="+frames);
                assertEquals(seconds,AnimationLoader.getPlaybackAge(root),1e-4);
                assertEquals(200,crossings);
            }
        }
    }
    @Test void largeDeltaCarriesRemainderAcrossStartMainAndEndWithoutDuplicateEvents() {
        Node root = new Node(); var listener = new Listener(); listener.policy = AnimationControl.PlaybackPolicy.ONCE; root.addControl(listener);
        var resource = AnimationFixtures.resource("segments",3,30);
        resource.getFlags().addAll(java.util.EnumSet.of(ArtResource.ArtResourceFlag.HAS_START_ANIMATION,ArtResource.ArtResourceFlag.HAS_END_ANIMATION));
        AnimationLoader.playAnimation(root,resource,AnimationLoaderTest::model,false);
        root.updateLogicalState(.15f);
        assertEquals(1,listener.cycles); assertEquals(0,listener.stops);
        assertEquals(.15-1.0/30-.1,root.getChild("End").getControl(AnimComposer.class).getTime(),1e-6);
        root.updateLogicalState(.1f); root.updateLogicalState(1);
        assertEquals(1,listener.cycles); assertEquals(1,listener.stops);
    }
    @Test void authoritativeClockFreezesOnStalledDeliveryAndInitialSeekDoesNotReplayHistory() {
        Node root = new Node(); var listener = new Listener(); root.addControl(listener);
        var resource = AnimationFixtures.resource("clock",3,30);
        AnimationLoader.playAnimation(root,resource,AnimationLoaderTest::model,false);
        double[] time = {Double.NaN}; AnimationLoader.bindPlaybackClock(root,()->time[0],20);
        root.updateLogicalState(5); assertEquals(0,AnimationLoader.getPlaybackAge(root));
        time[0] = 21.25; root.updateLogicalState(.1f);
        assertEquals(1.25,AnimationLoader.getPlaybackAge(root)); assertEquals(0,listener.cycles);
        root.updateLogicalState(5); assertEquals(1.25,AnimationLoader.getPlaybackAge(root));
        time[0] = 21.75; root.updateLogicalState(.1f); assertEquals(5,listener.cycles);
        AnimationLoader.pauseAnimations(root); time[0] = 22; root.updateLogicalState(1);
        assertEquals(1.75,AnimationLoader.getPlaybackAge(root));
        AnimationLoader.resumeAnimations(root); root.updateLogicalState(0);
        assertEquals(2,AnimationLoader.getPlaybackAge(root));
    }
    @Test void irregularUpdatesPreserveAllLoopCompletionsAndSilentSeekingResetsTheCursor() {
        Node root = new Node(); var listener = new Listener(); root.addControl(listener);
        var resource = AnimationFixtures.resource("irregular",3,30);
        AnimationLoader.playAnimation(root,resource,AnimationLoaderTest::model,false);
        root.updateLogicalState(.27f); root.updateLogicalState(.23f); root.updateLogicalState(.51f);
        assertEquals(10,listener.cycles);
        AnimationLoader.seekAnimation(root,resource,10.05); assertEquals(10,listener.cycles);
        root.updateLogicalState(.06f); assertEquals(11,listener.cycles);
    }
}
