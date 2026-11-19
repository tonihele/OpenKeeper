/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.control;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.view.animation.AnimationLoader;
import static org.junit.jupiter.api.Assertions.*;

class CreatureAnimationRequestTest {
    @Test void delayedWorkRequestUsesTheLatestAiPolicyBeforeItsInitialSeek() {
        var assets = new DesktopAssetManager(true) {
            @Override public Spatial loadModel(ModelKey key) {
                Node wrapper = new Node(), model = new Node(); wrapper.attachChild(model);
                var composer = new com.jme3.anim.AnimComposer();
                var clip = new com.jme3.anim.AnimClip(toniarts.openkeeper.tools.convert.KmfModelLoader.DUMMY_ANIM_CLIP_NAME);
                clip.setTracks(new com.jme3.anim.AnimTrack<?>[0]); composer.addAnimClip(clip); model.addControl(composer);
                model.setUserData(toniarts.openkeeper.tools.convert.KmfModelLoader.FRAME_FACTOR_FUNCTION,"WRAP");
                return wrapper;
            }
        };
        var data = new DefaultEntityData(); var id = data.createEntity(); var creature = new Creature();
        creature.getAnimations().put(Creature.AnimationType.RESEARCHING,
                toniarts.openkeeper.tools.convert.map.AnimationFixtures.resource("late-work-policy",3,30));
        data.setComponent(id,new toniarts.openkeeper.game.component.CreatureAi(0,toniarts.openkeeper.game.controller.creature.CreatureState.IDLE,(short)1));
        Node root = new Node(); root.setCullHint(Spatial.CullHint.Always);
        var control = new CreatureViewControl(id,data,creature,null,assets,null); root.addControl(control);
        double[] time = {2.5}; control.setGameTime(()->time[0]);
        data.setComponent(id,new toniarts.openkeeper.game.component.CreatureAi(0,toniarts.openkeeper.game.controller.creature.CreatureState.WORK,(short)1));
        try {
            assertTrue(control.setViewState(creature,new CreatureViewState((short)1,0,Creature.AnimationType.RESEARCHING,1)));
            time[0] = 2.55; root.updateLogicalState(.1f);
            assertEquals(2.55,AnimationLoader.getPlaybackAge(root),1e-9);
            assertEquals(.05,root.getChild("late-work-policy").getControl(com.jme3.anim.AnimComposer.class).getTime(),1e-6);
        } finally { control.cleanup(); data.close(); }
    }
    @Test void positionOnlyUpdatesDoNotRestartButSameTickSequenceAndCreatureChangesDo() {
        var assets = new DesktopAssetManager(true) {
            @Override public Spatial loadModel(ModelKey key) {
                Node root = new Node(); root.attachChild(new Node("static")); return root;
            }
        };
        var data = new DefaultEntityData(); var id = data.createEntity();
        var creature = new Creature();
        creature.getAnimations().put(Creature.AnimationType.WALK,
                new ArtResource("request-identity-fixture", ArtResource.ArtResourceType.MESH));
        Node root = new Node(); root.setCullHint(Spatial.CullHint.Always);
        var control = new CreatureViewControl(id, data, creature, null, assets, null); root.addControl(control);
        var state = new CreatureViewState((short)1, 2, Creature.AnimationType.WALK);
        assertTrue(control.setViewState(creature, state)); assertEquals(1, AnimationLoader.getModelRevision(root));
        assertFalse(control.isStopAnimation());
        var unavailable = state.next(2,Creature.AnimationType.DEATH_POSE);
        assertFalse(control.setViewState(creature,unavailable));
        assertFalse(control.isStopAnimation());
        assertEquals(Creature.AnimationType.WALK,control.getCurrentState());
        control.setTargetState(Creature.AnimationType.DEATH_POSE);
        assertFalse(control.isStopAnimation());
        assertFalse(control.setViewState(creature, state)); assertEquals(1, AnimationLoader.getModelRevision(root));
        assertTrue(control.setViewState(creature, state.next(2, state.state))); assertEquals(2, AnimationLoader.getModelRevision(root));
        var changed = new CreatureViewState((short)2, 2, state.state, 1);
        assertTrue(control.setViewState(creature, changed)); assertEquals(3, AnimationLoader.getModelRevision(root));
        control.cleanup();
    }
}
