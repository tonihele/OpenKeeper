/*
 * Copyright (C) 2026 OpenKeeper
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
package toniarts.openkeeper.view.effect;

import com.jme3.scene.*;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.scene.control.AbstractControl;
import com.jme3.renderer.*;
import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.*;
import static org.junit.jupiter.api.Assertions.*;

class EffectPlaybackTest {
    static void set(Object object, String name, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }
    static Effect effect(int id, int life, int next, int death) throws Exception {
        Effect e = new Effect();
        set(e, "effectId", id); set(e, "name", "effect-" + id);
        set(e, "minHp", life); set(e, "maxHp", life);
        set(e, "nextEffectId", next); set(e, "deathEffectId", death);
        set(e, "flags", EnumSet.noneOf(Effect.EffectFlag.class));
        set(e, "generateIds", List.of());
        return e;
    }
    static EffectManagerState manager(Effect... definitions) {
        Map<Integer, Effect> effects = new HashMap<>();
        for (Effect e : definitions) effects.put(e.getEffectId(), e);
        IKwdFile kwd = (IKwdFile) Proxy.newProxyInstance(EffectPlaybackTest.class.getClassLoader(),
            new Class[]{IKwdFile.class}, (p, m, a) -> m.getName().equals("getEffect") ? effects.get(a[0]) : null);
        return new EffectManagerState(kwd, null);
    }
    private static List<String> names(Node root) {
        List<String> result = new ArrayList<>();
        root.depthFirstTraversal(s -> result.add(s.getName()));
        return result;
    }
    @Test void nextStartsImmediatelyButDeathWaitsForParentNotItsNextSibling() throws Exception {
        Node root = new Node();
        try (var playback = new EffectPlayback(manager(effect(1, 2, 2, 3),
                effect(2, 100, 0, 0), effect(3, 1, 0, 0)), root, (short) 3, 42)) {
            playback.spawn(1, null);
            assertTrue(names(root).contains("effect-2"));
            assertFalse(names(root).contains("effect-3"));
            playback.advance(.05);
            assertFalse(names(root).contains("effect-3"));
            playback.advance(.05);
            assertTrue(names(root).contains("effect-3"));
            assertTrue(names(root).contains("effect-2"));
            playback.advance(6);
            assertTrue(playback.isEmpty());
        }
        assertEquals(0, root.getQuantity());
    }
    @Test void controlsAdvanceExactlyOnceOnTheFixedClockEvenBelowAnAppRoot() throws Exception {
        Node app = new Node();
        Node sequence = new Node();
        app.attachChild(sequence);
        try (var playback = new EffectPlayback(manager(effect(1, 100, 0, 0)), sequence, (short) 3, 42)) {
            playback.spawn(1, null);
            Node model = new Node();
            // Put the test control in the playback-owned subtree.
            ((Node) sequence.getChild(0)).attachChild(model);
            int[] calls = {0};
            model.addControl(new AbstractControl() {
                protected void controlUpdate(float tpf) { assertEquals(.05f, tpf); calls[0]++; }
                protected void controlRender(RenderManager r, ViewPort v) {}
            });
            playback.manage(model);
            app.updateLogicalState(.9f);
            assertEquals(0, calls[0]);
            for (int i = 0; i < 60; i++) {
                playback.advance(1.0 / 60);
                app.updateLogicalState(1f / 60);
            }
            assertEquals(20, calls[0]);
            playback.advance(.99);
            assertEquals(39, calls[0]);
            playback.advance(.01);
            assertEquals(40, calls[0]);
        }
    }
    @Test void missingDefinitionsAndCancellationAreSafe() {
        Node root = new Node();
        var playback = new EffectPlayback(manager(), root, (short) 3, 4);
        playback.spawn(999, null);
        playback.advance(100);
        assertTrue(playback.isEmpty());
        playback.close(); playback.close();
        playback.spawn(1, null); playback.advance(1);
        assertEquals(0, root.getQuantity());
    }

    @Test void aMissingDelayedChildAssetCannotEscapeOrPreventOtherEffectsFromCompleting() throws Exception {
        Effect parent = effect(801, 2, 0, 802);
        Effect missing = effect(802, 1, 0, 0);
        var art = new ArtResource();
        set(art, "name", "missing-delayed-heart-child");
        set(art, "type", ArtResource.ArtResourceType.MESH);
        set(art, "flags", EnumSet.noneOf(ArtResource.ArtResourceFlag.class));
        set(missing, "artResource", art);
        var definitions = Map.of(801, parent, 802, missing, 803, effect(803, 5, 0, 0));
        IKwdFile kwd = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{IKwdFile.class}, (p, m, a) -> m.getName().equals("getEffect") ? definitions.get(a[0]) : null);
        int[] loads = {0};
        AssetManager assets = (AssetManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{AssetManager.class}, (p, m, a) -> {
                    if (m.getName().equals("loadModel")) {
                        loads[0]++;
                        throw new AssetNotFoundException("Missing delayed child fixture");
                    }
                    return null;
                });
        Node root = new Node();
        try (var playback = new EffectPlayback(new EffectManagerState(kwd, assets), root, (short) 3, 42)) {
            playback.spawn(801, null);
            playback.spawn(803, null);
            assertDoesNotThrow(() -> playback.advance(.1));
            assertEquals(1, loads[0]);
            assertFalse(names(root).contains("effect-802"), "Failed child's partial node must be removed");
            assertTrue(names(root).contains("effect-803"), "Unrelated roots must remain live");
            playback.advance(1);
            assertTrue(playback.isEmpty());
            assertEquals(0, ((Node) root.getChild(0)).getQuantity());
        }
        assertEquals(0, root.getQuantity());
    }

    @Test void elementScaleDoesNotOverwriteTheAuthoredMeshScale() throws Exception {
        Effect effect = effect(1, 1, 0, 0);
        set(effect, "elementsPerTurn", (short) 1);
        set(effect, "generationType", Effect.GenerationType.CUBE_GEN);
        EffectElement element = new EffectElement();
        set(element, "name", "beam"); set(element, "minHp", 100); set(element, "maxHp", 100);
        set(element, "minScale", 2f); set(element, "maxScale", 2f);
        set(element, "flags", EnumSet.noneOf(EffectElement.EffectElementFlag.class));
        Node root = new Node();
        try (var playback = new EffectPlayback(manager(), root, (short) 3, 42)) {
            var emitter = new EffectEmitter(element, effect, new Random(42), playback) {
                public void onDeath(com.jme3.math.Vector3f point) { }
                public void onHit(com.jme3.math.Vector3f point) { }
            };
            Node model = new Node("mesh"); model.setLocalScale(3);
            emitter.setSpatial(model); root.attachChild(emitter); emitter.emitOne();
            Node instance = (Node) emitter.getChild(0);
            assertEquals(2, instance.getLocalScale().x);
            assertEquals(3, instance.getChild(0).getLocalScale().x);
            assertEquals(6, instance.getChild(0).getWorldScale().x);
        }
    }

    @Test void solidCollisionRemovesOnlyTheHitElementAndCallsDeathOnce() throws Exception {
        EffectElement element = new EffectElement();
        set(element, "minHp", 100); set(element, "maxHp", 100);
        set(element, "minScale", 1f); set(element, "maxScale", 1f);
        set(element, "minSpeedYz", -1f); set(element, "maxSpeedYz", -1f);
        set(element, "flags", EnumSet.of(EffectElement.EffectElementFlag.DIE_WHEN_HIT_SOLID));
        int[] events = new int[2];
        Node root = new Node(); Node hit = new Node(); Node sibling = new Node();
        root.attachChild(hit); root.attachChild(sibling); hit.setLocalTranslation(0, 1, 0);
        hit.addControl(new EffectElementControl(element, 0, 0, new Random(1)) {
            public void onDie(com.jme3.math.Vector3f point) { events[0]++; }
            public void onHit(com.jme3.math.Vector3f point) { events[1]++; }
        });
        root.updateLogicalState(.05f); root.updateLogicalState(.05f);
        assertNull(hit.getParent()); assertSame(root, sibling.getParent());
        assertArrayEquals(new int[]{1, 1}, events);
    }
}
