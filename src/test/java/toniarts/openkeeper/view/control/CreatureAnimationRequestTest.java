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
