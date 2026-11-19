/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.animation;

import com.jme3.anim.*;
import com.jme3.renderer.*;
import com.jme3.scene.*;
import com.jme3.scene.control.AbstractControl;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.map.*;
import static org.junit.jupiter.api.Assertions.*;

class AnimationTransactionTest {
    private static Spatial failingModel(String name, ArtResource art, double failAt) {
        Node wrapper = (Node)AnimationLoaderTest.model(name,art);
        var model = wrapper.getChild(0); Mesh mesh = new Mesh(); mesh.setBuffer(VertexBuffer.Type.Position,3,new float[]{0,0,0});
        var target = new Geometry("invalid",mesh);
        model.getControl(AnimComposer.class).getAnimClip(KmfModelLoader.DUMMY_ANIM_CLIP_NAME).setTracks(new AnimTrack<?>[]{
            new MorphTrack(target,new float[]{0,1},new float[]{0,1},1) {
                @Override public void getDataAtTime(double time,float[] store) { if(time >= failAt) throw new IllegalArgumentException("evaluation failure"); }
            }
        });
        return wrapper;
    }
    @Test void firstAndTerminalSamplesFailBeforePublicationEvenUnderAHiddenPausedOwner() {
        for (double failAt : new double[]{0,.02}) {
            Node root = new Node(); var cache = new PreparedAnimationCache(3,1024); root.addControl(cache);
            var old = AnimationFixtures.resource("live",3,30); AnimationLoader.playAnimation(root,old,AnimationLoaderTest::model,false);
            var live = root.getChild(0); var playback = root.getControl(AnimationPlayback.class);
            root.updateLogicalState(.01f); int revision = AnimationLoader.getModelRevision(root);
            double age = AnimationLoader.getPlaybackAge(root);
            root.setCullHint(Spatial.CullHint.Always); AnimationLoader.pauseAnimations(root);
            var bad = AnimationFixtures.resource("bad",3,30); bad.getFlags().add(ArtResource.ArtResourceFlag.HAS_END_ANIMATION);
            assertEquals(AnimationLoader.PlaybackResult.FAILED,AnimationLoader.playAnimation(root,bad,
                    (name,art)->name.endsWith("End") ? failingModel(name,art,failAt) : AnimationLoaderTest.model(name,art),true));
            assertSame(live,root.getChild(0)); assertSame(playback,root.getControl(AnimationPlayback.class));
            assertEquals(revision,AnimationLoader.getModelRevision(root)); assertEquals(age,AnimationLoader.getPlaybackAge(root));
            assertEquals(1,cache.size()); assertTrue(AnimationLoader.isPaused(root));
        }
    }
    private static final class Listener extends AbstractControl implements AnimationControl {
        boolean fail;
        @Override public void onAnimationAttached() { if(fail) { spatial.setUserData("owner-marker","changed"); throw new IllegalStateException("attachment failure"); } }
        @Override public boolean isStopAnimation() { return false; }
        @Override public void onAnimationStop() { }
        @Override public void onAnimationCycleDone() { }
        @Override protected void controlUpdate(float tpf) { }
        @Override protected void controlRender(RenderManager rm, ViewPort vp) { }
    }
    @Test void publicationFailureRestoresChildrenClockUserDataAndValidActiveCacheEntry() {
        Node root = new Node(); var cache = new PreparedAnimationCache(3,1024); root.addControl(cache);
        var listener = new Listener(); root.addControl(listener);
        var resource = AnimationFixtures.resource("repeat",3,30);
        AnimationLoader.playAnimation(root,resource,AnimationLoaderTest::model,false);
        Spatial live = root.getChild(0), flower = new Node("flower"); flower.setUserData(toniarts.openkeeper.utils.AssetUtils.USER_DATA_KEY_REMOVABLE,false); root.attachChild(flower);
        root.setUserData("owner-marker","original"); root.updateLogicalState(.05f);
        int revision = AnimationLoader.getModelRevision(root); var playback = root.getControl(AnimationPlayback.class);
        listener.fail = true;
        assertEquals(AnimationLoader.PlaybackResult.FAILED,AnimationLoader.playAnimation(root,resource,AnimationLoaderTest::model,false));
        assertEquals(java.util.List.of(live,flower),root.getChildren()); assertSame(playback,root.getControl(AnimationPlayback.class));
        assertEquals("original",root.getUserData("owner-marker")); assertEquals(revision,AnimationLoader.getModelRevision(root));
        assertEquals(.05,AnimationLoader.getPlaybackAge(root),1e-6); assertEquals(1,cache.size());
        root.updateLogicalState(.01f); assertEquals(.06,AnimationLoader.getPlaybackAge(root),1e-6);
    }
}
