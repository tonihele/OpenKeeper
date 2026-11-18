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
