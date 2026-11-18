/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.animation;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.utils.AssetUtils;
import static org.junit.jupiter.api.Assertions.*;

class AnimationLoaderTest {
    @Test void preparedReuseIsBoundedResetsPlaybackAndNeverCrossesOwners() {
        var first = new Node(); var second = new Node();
        var cache = new PreparedAnimationCache(2,1024); first.addControl(cache);
        second.addControl(new PreparedAnimationCache(2,1024));
        int[] loads = {0}; AnimationLoader.ModelSource source = (name,resource) -> { loads[0]++; return model(name,resource); };
        var a = toniarts.openkeeper.tools.convert.map.AnimationFixtures.resource("a",3,30);
        var b = toniarts.openkeeper.tools.convert.map.AnimationFixtures.resource("b",3,30);
        var c = toniarts.openkeeper.tools.convert.map.AnimationFixtures.resource("c",3,30);
        AnimationLoader.playAnimation(first,a,source,false); var retained = first.getChild("a");
        first.updateLogicalState(.05f);
        AnimationLoader.playAnimation(first,b,source,false);
        AnimationLoader.playAnimation(first,a,source,false);
        assertSame(retained,first.getChild("a")); assertEquals(2,loads[0]);
        assertEquals(0,retained.getControl(AnimComposer.class).getTime());
        AnimationLoader.playAnimation(second,a,source,false); assertNotSame(retained,second.getChild("a"));
        AnimationLoader.playAnimation(first,c,source,false); assertEquals(2,cache.size());
        AnimationLoader.playAnimation(first,b,source,false); assertEquals(5,loads[0]);
        AnimationLoader.playAnimation(first,toniarts.openkeeper.tools.convert.map.AnimationFixtures.resource("b",3,15),source,false);
        assertEquals(6,loads[0]);
        var clone = (Node)first.deepClone(); assertEquals(0,clone.getControl(PreparedAnimationCache.class).size());
        cache.clear(); assertEquals(0,cache.size()); assertEquals(0,cache.getResidentPositionBytes());
    }
    @Test void preparedCacheRejectsEntriesExceedingThePositionBudget() {
        var cache = new PreparedAnimationCache(3,35);
        var mesh = new com.jme3.scene.Mesh(); mesh.setBuffer(com.jme3.scene.VertexBuffer.Type.Position,3,new float[]{0,0,0});
        cache.put(new ArtResource("large",ArtResource.ArtResourceType.MESH),java.util.List.of(new com.jme3.scene.Geometry("large",mesh)));
        assertEquals(0,cache.size());
    }
    private static final class Listener extends com.jme3.scene.control.AbstractControl implements AnimationControl {
        PlaybackPolicy policy = PlaybackPolicy.ONCE;
        int cycles, stops;
        Runnable complete = () -> {};
        @Override public PlaybackPolicy getPlaybackPolicy() { return policy; }
        @Override public boolean isStopAnimation() { return policy == PlaybackPolicy.ONCE; }
        @Override public void onAnimationStop() { stops++; }
        @Override public void onAnimationCycleDone() { cycles++; complete.run(); }
        @Override protected void controlUpdate(float tpf) {}
        @Override protected void controlRender(com.jme3.renderer.RenderManager rm, com.jme3.renderer.ViewPort vp) {}
    }
    @Test void oncePlaysStartMainEndAndNotifiesExactlyOnceWhileHoldKeepsMainVisible() {
        for (var policy : new AnimationControl.PlaybackPolicy[]{AnimationControl.PlaybackPolicy.ONCE,AnimationControl.PlaybackPolicy.HOLD}) {
            Node root = new Node(); var listener = new Listener(); listener.policy = policy; root.addControl(listener);
            var resource = toniarts.openkeeper.tools.convert.map.AnimationFixtures.resource("segments",3,30);
            resource.getFlags().add(ArtResource.ArtResourceFlag.HAS_START_ANIMATION);
            resource.getFlags().add(ArtResource.ArtResourceFlag.HAS_END_ANIMATION);
            AnimationLoader.playAnimation(root,resource,AnimationLoaderTest::model,false);
            assertNotEquals(Spatial.CullHint.Always,root.getChild("Start").getCullHint());
            for (int i=0;i<100;i++) root.updateLogicalState(.01f);
            assertEquals(1,listener.cycles);
            if (policy == AnimationControl.PlaybackPolicy.HOLD) {
                assertEquals(0,listener.stops);
                assertNotEquals(Spatial.CullHint.Always,root.getChild("segments").getCullHint());
            } else {
                assertEquals(1,listener.stops);
                assertNotEquals(Spatial.CullHint.Always,root.getChild("End").getCullHint());
            }
            assertFalse(root.getChild("segments").getControl(AnimComposer.class).isEnabled());
        }
    }
    @Test void completionThatReplacesTheModelCannotActivateTheOldEndSegment() {
        Node root = new Node(); var listener = new Listener(); root.addControl(listener);
        var old = toniarts.openkeeper.tools.convert.map.AnimationFixtures.resource("old",3,30);
        old.getFlags().add(ArtResource.ArtResourceFlag.HAS_END_ANIMATION);
        listener.complete = () -> AnimationLoader.playAnimation(root,new ArtResource("new",ArtResource.ArtResourceType.MESH),AnimationLoaderTest::model,false);
        AnimationLoader.playAnimation(root,old,AnimationLoaderTest::model,false);
        for(int i=0;i<50;i++) root.updateLogicalState(.01f);
        assertNotNull(root.getChild("new")); assertNull(root.getChild("End"));
        assertEquals(1,listener.cycles); assertEquals(0,listener.stops);
    }
    @Test void clampTraversesBothDirectionsForMultipleCyclesAndSeeksBackwards() {
        Node root = new Node();
        var resource = toniarts.openkeeper.tools.convert.map.AnimationFixtures.resource("cycle",3,30);
        AnimationLoader.playAnimation(root,resource,(name, art) -> {
            Node wrapper = (Node) model(name, art);
            Node node = (Node) wrapper.getChild(0);
            node.setUserData(KmfModelLoader.FRAME_FACTOR_FUNCTION,"CLAMP");
            var mesh = new com.jme3.scene.Mesh();
            mesh.setBuffer(com.jme3.scene.VertexBuffer.Type.Position,3,new float[]{0,0,0});
            mesh.setBuffer(com.jme3.scene.VertexBuffer.Type.BindPosePosition,3,new float[]{0,0,0});
            var geometry = new com.jme3.scene.Geometry("vertex",mesh); node.attachChild(geometry);
            var a = new toniarts.openkeeper.animation.Pose("a",new com.jme3.math.Vector3f[]{new com.jme3.math.Vector3f()},new int[]{0});
            var b = new toniarts.openkeeper.animation.Pose("b",new com.jme3.math.Vector3f[]{new com.jme3.math.Vector3f(2,0,0)},new int[]{0});
            var clip = node.getControl(AnimComposer.class).getAnimClip(KmfModelLoader.DUMMY_ANIM_CLIP_NAME);
            clip.setTracks(new toniarts.openkeeper.animation.PoseTrack[]{new toniarts.openkeeper.animation.PoseTrack(geometry,
                    new float[]{0,1f/30,2f/30},new toniarts.openkeeper.animation.PoseTrack.PoseFrame[]{
                        new toniarts.openkeeper.animation.PoseTrack.PoseFrame(new toniarts.openkeeper.animation.Pose[]{a,b},new float[]{0}),
                        new toniarts.openkeeper.animation.PoseTrack.PoseFrame(new toniarts.openkeeper.animation.Pose[]{a,b},new float[]{.5f}),
                        new toniarts.openkeeper.animation.PoseTrack.PoseFrame(new toniarts.openkeeper.animation.Pose[]{a,b},new float[]{1})})});
            return wrapper;
        },false);
        var composer = root.getChild("cycle").getControl(AnimComposer.class);
        var geometry = (com.jme3.scene.Geometry)((Node)root.getChild("cycle")).getChild(0);
        var positions = (java.nio.FloatBuffer)geometry.getMesh().getBuffer(com.jme3.scene.VertexBuffer.Type.Position).getData();
        int rising = 0, falling = 0; float previous = 0;
        for (int i=0;i<200;i++) {
            root.updateLogicalState(.01f);
            float current = positions.get(0);
            if (current > previous+.01) rising++;
            if (current < previous-.01) falling++;
            previous = current;
        }
        assertTrue(rising > 30); assertTrue(falling > 30);
        assertEquals(1,composer.getGlobalSpeed());
        AnimationLoader.seekAnimation(root,resource,.15);
        assertEquals(1.5,positions.get(0),1e-5);
        AnimationLoader.pauseAnimations(root); root.updateLogicalState(.5f);
        assertEquals(1.5,positions.get(0),1e-5);
        AnimationLoader.resumeAnimations(root); root.updateLogicalState(.01f);
        assertTrue(positions.get(0)<1.5);
    }
    @Test void nestedPauseRestoresDirectionAndInactiveComposersAndCoversReplacement() {
        Node root = new Node(); Node owner = new Node(); root.attachChild(owner);
        var resource = new ArtResource("pause", ArtResource.ArtResourceType.ANIMATING_MESH);
        AnimationLoader.playAnimation(owner, resource, AnimationLoaderTest::model, false);
        var composer = owner.getChild("pause").getControl(AnimComposer.class);
        composer.setGlobalSpeed(-2);
        AnimationLoader.pauseAnimations(root); AnimationLoader.pauseAnimations(root);
        double time = composer.getTime(); root.updateLogicalState(.01f); assertEquals(time,composer.getTime());
        AnimationLoader.resumeAnimations(root); assertTrue(composer.isEnabled()); assertEquals(-2,composer.getGlobalSpeed());
        AnimationLoader.pauseAnimations(root);
        AnimationLoader.playAnimation(owner, resource, AnimationLoaderTest::model, false);
        var replacement = owner.getChild("pause").getControl(AnimComposer.class);
        assertFalse(replacement.isEnabled()); AnimationLoader.resumeAnimations(root); assertTrue(replacement.isEnabled());
    }
    @Test void lateRequestSeeksWithinTheAuthoredDuration() {
        Node root = new Node();
        var resource = toniarts.openkeeper.tools.convert.map.AnimationFixtures.resource("seek",30,15);
        AnimationLoader.playAnimation(root,resource,AnimationLoaderTest::model,false);
        AnimationLoader.seekAnimation(root,resource,3.25);
        assertEquals(1.25, root.getChild("seek").getControl(AnimComposer.class).getTime(),1e-9);
    }
    static Spatial model(String name, ArtResource resource) {
        Node wrapper = new Node(); Node model = new Node(name); wrapper.attachChild(model);
        if (resource.getType() == ArtResource.ArtResourceType.ANIMATING_MESH) {
            var composer = new AnimComposer();
            var clip = new AnimClip(KmfModelLoader.DUMMY_ANIM_CLIP_NAME);
            clip.setTracks(new com.jme3.anim.AnimTrack<?>[0]);
            composer.addAnimClip(clip);
            model.addControl(composer);
            model.setUserData(KmfModelLoader.FRAME_FACTOR_FUNCTION, "WRAP");
        }
        return wrapper;
    }
    @Test void missingAndUnavailableResourcesPreserveTheLiveModel() {
        Node root = new Node(); Spatial live = new Node("live"); root.attachChild(live);
        assertEquals(AnimationLoader.PlaybackResult.UNAVAILABLE, AnimationLoader.playAnimation(root, null, AnimationLoaderTest::model, false));
        assertSame(live, root.getChild(0));
        var resource = new ArtResource("missing", ArtResource.ArtResourceType.ANIMATING_MESH);
        assertEquals(AnimationLoader.PlaybackResult.FAILED, AnimationLoader.playAnimation(root, resource, (n, a) -> { throw new IllegalArgumentException("missing file"); }, false));
        assertSame(live, root.getChild(0)); assertEquals(0, AnimationLoader.getModelRevision(root));
    }
    @Test void failureOfAnEndSegmentDoesNotPublishPartialChildren() {
        Node root = new Node(); Spatial live = new Node("live"); root.attachChild(live);
        var resource = new ArtResource("work", ArtResource.ArtResourceType.ANIMATING_MESH);
        resource.getFlags().add(ArtResource.ArtResourceFlag.HAS_END_ANIMATION);
        assertEquals(AnimationLoader.PlaybackResult.FAILED, AnimationLoader.playAnimation(root, resource,
            (n, a) -> { if (n.endsWith("End")) throw new IllegalArgumentException("missing end"); return model(n, a); }, false));
        assertEquals(1, root.getQuantity()); assertSame(live, root.getChild(0));
    }
    @Test void successfulReplacementRetainsNonRemovableDecorations() {
        Node root = new Node(); root.attachChild(new Node("old"));
        Spatial flower = new Node("flower"); flower.setUserData(AssetUtils.USER_DATA_KEY_REMOVABLE, false); root.attachChild(flower);
        var resource = new ArtResource("static", ArtResource.ArtResourceType.MESH);
        assertEquals(AnimationLoader.PlaybackResult.STARTED, AnimationLoader.playAnimation(root, resource, AnimationLoaderTest::model, false));
        assertEquals(2, root.getQuantity()); assertSame(flower, root.getChild("flower"));
        assertNotNull(root.getChild("static")); assertEquals(1, AnimationLoader.getModelRevision(root));
    }
    @Test void animatingResourceRequiresAComposerBeforeReplacement() {
        Node root = new Node(); Spatial live = new Node("live"); root.attachChild(live);
        var resource = new ArtResource("bad", ArtResource.ArtResourceType.ANIMATING_MESH);
        assertEquals(AnimationLoader.PlaybackResult.FAILED, AnimationLoader.playAnimation(root, resource,
            (n, a) -> { Node wrapper = new Node(); wrapper.attachChild(new Node()); return wrapper; }, false));
        assertSame(live, root.getChild(0));
    }
}
