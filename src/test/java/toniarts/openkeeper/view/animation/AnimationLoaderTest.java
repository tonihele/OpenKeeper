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
    static Spatial model(String name, ArtResource resource) {
        Node wrapper = new Node(); Node model = new Node(name); wrapper.attachChild(model);
        if (resource.getType() == ArtResource.ArtResourceType.ANIMATING_MESH) {
            var composer = new AnimComposer();
            composer.addAnimClip(new AnimClip(KmfModelLoader.DUMMY_ANIM_CLIP_NAME));
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
