/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.view.control;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.TransformTrack;
import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.TrapFiringFixtures;

import static org.junit.jupiter.api.Assertions.*;

class TrapViewControlTest {

    private static final short TRAP_ID = 42;
    private static final String IDLE = "synthetic-trap-idle";
    private final Trap trap = TrapFiringFixtures.withIdleAnimation(
            TrapFiringFixtures.trap(TRAP_ID, Trap.TriggerType.PRESSURE, 1, 6, (short) 1),
            new ArtResource(IDLE, ArtResource.ArtResourceType.ANIMATING_MESH));
    private final Node root = new Node("trap");
    private TrapViewControl control;

    private void start(boolean blueprint) {
        var data = new DefaultEntityData();
        var id = data.createEntity();
        AssetManager assets = (AssetManager) Proxy.newProxyInstance(AssetManager.class.getClassLoader(),
                new Class<?>[]{AssetManager.class}, (proxy, method, args) -> {
                    if (method.getName().equals("loadModel")) {
                        Node container = new Node("model-root");
                        Node model = new Node("model");
                        model.setUserData(KmfModelLoader.FRAME_FACTOR_FUNCTION, "WRAP");
                        AnimClip clip = new AnimClip(KmfModelLoader.DUMMY_ANIM_CLIP_NAME);
                        clip.setTracks(new TransformTrack[]{new TransformTrack(model, new float[]{0, 1},
                            new Vector3f[]{new Vector3f(), new Vector3f(0.25f, 0, 0)}, null, null)});
                        AnimComposer composer = new AnimComposer();
                        composer.addAnimClip(clip);
                        model.addControl(composer);
                        container.attachChild(model);
                        return container;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        control = new TrapViewControl(id, data, trap, new TrapViewState(TRAP_ID, blueprint), assets, null);
        root.addControl(control);
    }

    private AnimComposer composer(String resource) {
        return root.getChild(resource).getControl(AnimComposer.class);
    }

    private void advance(float seconds) {
        for (int i = 0; i < Math.round(seconds * 20); i++) {
            root.updateLogicalState(0.05f);
        }
    }

    @Test
    void idleUsesItsAuthoredFrameRate() {
        TrapFiringFixtures.withIdleAnimation(trap, TrapFiringFixtures.animation(IDLE, 48, 12));
        start(false);
        // The synthetic clip lasts one second: rate .25 makes the authored 4 seconds.
        assertEquals(0.25, composer(IDLE).getGlobalSpeed(), 0.0001);
        advance(1);
        assertEquals(0.25, composer(IDLE).getTime(), 0.06);
        advance(4);
        assertTrue(composer(IDLE).isEnabled());
        assertEquals(0.25, composer(IDLE).getTime(), 0.06);
    }

    @Test
    void armedTrapKeepsPlayingItsIdleAnimation() {
        start(false);
        advance(3);
        assertNotNull(root.getChild(IDLE));
        assertTrue(composer(IDLE).isEnabled());
        assertFalse(control.isStopAnimation());
    }

    @Test
    void visibilityUpdateDoesNotRestartIdle() {
        start(false);
        advance(0.4f);
        var idle = root.getChild(IDLE);
        double phase = composer(IDLE).getTime();
        control.setTargetState(new TrapViewState(TRAP_ID, false, true));
        assertSame(idle, root.getChild(IDLE));
        assertEquals(phase, composer(IDLE).getTime(), 0.001);
    }

    @Test
    void invalidTimingMetadataKeepsDefaultSpeed() {
        TrapFiringFixtures.withIdleAnimation(trap, TrapFiringFixtures.animation(IDLE, 48, 0));
        start(false);
        assertEquals(1, composer(IDLE).getGlobalSpeed(), 0.0001);
    }

    @Test
    void blueprintIsStillUntilArmed() {
        start(true);
        assertFalse(composer(IDLE).isEnabled());
        advance(2);
        assertFalse(composer(IDLE).isEnabled());
        control.setTargetState(new TrapViewState(TRAP_ID, false));
        assertTrue(composer(IDLE).isEnabled());
        assertFalse(control.isStopAnimation());
    }

}
