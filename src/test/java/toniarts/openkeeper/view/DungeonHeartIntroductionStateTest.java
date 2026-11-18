/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view;

import com.jme3.anim.*;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.*;
import com.jme3.material.Material;
import com.jme3.scene.*;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture2D;
import com.simsilica.es.*;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.map.*;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.animation.AnimationLoader;
import static org.junit.jupiter.api.Assertions.*;

class DungeonHeartIntroductionStateTest {
    private static void field(Object o, String name, Object value) throws Exception {
        Field f = o.getClass().getDeclaredField(name); f.setAccessible(true); f.set(o, value);
    }
    @Test void cuesSurviveStallsPauseAndModelArrivalWithoutReplaying() throws Exception {
        var data = new DefaultEntityData();
        var now = new AtomicLong(1_000_000_000L);
        EntityId room = data.createEntity();
        EntityId heart = data.createEntity();
        EntityId arches = data.createEntity();
        var intro = new DungeonHeartIntroduction(null, heart, arches, data.createEntity(), data.createEntity(), data.createEntity());
        intro.startTime = 2_000_000_000L; intro.ticksPerSecond = 4;
        data.setComponents(room, new RoomComponent((short) 5, false, new Point(3, 4)), new Owner((short) 3, (short) 3), intro);
        List<Integer> spawned = new ArrayList<>();
        IKwdFile kwd = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{IKwdFile.class}, (p,m,a) -> {
            if (!m.getName().equals("getEffect")) return null;
            int id = (int) a[0]; spawned.add(id);
            Effect e = new Effect();
            field(e, "name", "effect-" + id); field(e, "effectId", id);
            field(e, "minHp", 1); field(e, "maxHp", 1);
            field(e, "flags", EnumSet.noneOf(Effect.EffectFlag.class)); field(e, "generateIds", List.of());
            return e;
        });
        Node root = new Node("root");
        Map<EntityId, Spatial> models = new HashMap<>();
        Node model = new Node("heart"); models.put(heart, model);
        var state = new DungeonHeartIntroductionState(data, (short) 3, root, new EffectManagerState(kwd, null),
                models::get, point -> true, now::get);
        try {
            state.update(.5f); assertTrue(spawned.isEmpty()); // loading is not game start
            now.set(0); state.start(); state.update(.01f);
            assertTrue(spawned.isEmpty(), "No effects before the remote clock is synchronized");
            now.set(1_000_000_000L); state.update(.01f);
            assertEquals(List.of(223), spawned);
            now.set(12_000_000_000L); state.update(10); // jumps across formation, reveal and completion
            assertEquals(1, Collections.frequency(spawned, 225));
            assertEquals(1, Collections.frequency(spawned, 314));
            // A late-loaded mesh joins the original phase instead of replaying the introduction.
            var clip = new AnimClip(KmfModelLoader.DUMMY_ANIM_CLIP_NAME);
            clip.setTracks(new AnimTrack[]{new TransformTrack(new Node(), new float[]{0, 2},
                    new com.jme3.math.Vector3f[]{new com.jme3.math.Vector3f(), new com.jme3.math.Vector3f()}, null, null)});
            var composer = new AnimComposer(); composer.addAnimClip(clip);
            composer.setCurrentAction(KmfModelLoader.DUMMY_ANIM_CLIP_NAME);
            model.addControl(composer); // composer arrives under the existing entity root
            state.update(.01f); assertEquals(0, composer.getTime(), 1e-6);
            now.set(12_500_000_000L); state.update(.5f); assertEquals(.5, composer.getTime(), 1e-6);
            state.setPaused(true); now.set(20_000_000_000L); state.update(7.5f);
            assertEquals(.5, composer.getTime(), 1e-6);
            state.setPaused(false);
            state.update(0); assertEquals(.5, composer.getTime(), 1e-6, "Wait for the resumed server epoch");
            var complete = new DungeonHeartIntroduction(intro); complete.complete = true;
            complete.startTime += 7_500_000_000L;
            complete.waitingStartTime += 7_500_000_000L;
            data.setComponent(room, complete); state.update(0);
            assertEquals(.5, composer.getTime(), 1e-6);
            assertEquals(1, Collections.frequency(spawned, 225));
            assertEquals(1, Collections.frequency(spawned, 314));
            data.removeComponent(room, DungeonHeartIntroduction.class); state.update(0);
            assertEquals(0, root.getQuantity());
            assertEquals(1, composer.getGlobalSpeed());
        } finally { state.cleanup(); data.close(); }
    }

    @Test void opacityIsPerInstanceAndRestoresAuthoredBlendAndBucket() {
        var assets = new DesktopAssetManager(true);
        var material = new com.jme3.material.Material(assets, "MatDefs/DkLighting/Vertex.j3md");
        Geometry a = new Geometry("a", new Box(1,1,1)); a.setMaterial(material);
        Geometry b = new Geometry("b", new Box(1,1,1)); b.setMaterial(material);
        var opacity = new toniarts.openkeeper.view.control.SpatialOpacity(a);
        opacity.set(.25f);
        assertNotSame(material, a.getMaterial()); assertSame(material, b.getMaterial());
        assertEquals(RenderState.BlendMode.Alpha, a.getMaterial().getAdditionalRenderState().getBlendMode());
        assertFalse(a.getMaterial().getAdditionalRenderState().isDepthWrite());
        opacity.set(1);
        assertSame(material, a.getMaterial());
        assertEquals(RenderState.BlendMode.Off, material.getAdditionalRenderState().getBlendMode());
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);
        var additive = new toniarts.openkeeper.view.control.SpatialOpacity(b);
        additive.set(.5f);
        assertEquals(RenderState.BlendMode.AlphaAdditive, b.getMaterial().getAdditionalRenderState().getBlendMode());
        opacity.close();
        additive.close();
    }

    @Test void furnitureReplacementUnderTheSameRootRebindsOpacityAndRestoresBothMaterials() throws Exception {
        var assets = new DesktopAssetManager(true);
        var data = new DefaultEntityData();
        var room = data.createEntity();
        var arches = data.createEntity();
        var intro = new DungeonHeartIntroduction(null, data.createEntity(), arches,
                data.createEntity(), data.createEntity(), data.createEntity());
        intro.startTime = 1_000_000_000L;
        intro.waitingStartTime = intro.startTime;
        intro.ticksPerSecond = 4;
        data.setComponents(room, intro, new Owner((short) 3, (short) 3),
                new RoomComponent((short) 5, false, new Point(1, 1)));
        var now = new AtomicLong(8_500_000_000L);
        Node model = new Node("arches");
        var original = new Material(assets, "MatDefs/DkLighting/Vertex.j3md");
        Geometry oldGeometry = new Geometry("old", new Box(1, 1, 1));
        oldGeometry.setMaterial(original);
        model.attachChild(oldGeometry);
        IKwdFile kwd = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{IKwdFile.class}, (p, m, a) -> null);
        var state = new DungeonHeartIntroductionState(data, (short) 3, new Node(),
                new EffectManagerState(kwd, assets), id -> id.equals(arches) ? model : null,
                point -> true, now::get);
        try {
            state.start();
            state.update(0);
            assertEquals(RenderState.BlendMode.Alpha, oldGeometry.getMaterial().getAdditionalRenderState().getBlendMode());
            var art = new ArtResource();
            field(art, "name", "heart-furniture-replacement-fixture");
            field(art, "type", ArtResource.ArtResourceType.MESH);
            field(art, "flags", EnumSet.noneOf(ArtResource.ArtResourceFlag.class));
            AssetManager replacementAssets = (AssetManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{AssetManager.class}, (p, m, a) -> {
                        if (!m.getName().equals("loadModel")) return null;
                        var replacement = new Material(assets, "MatDefs/DkLighting/Vertex.j3md");
                        Texture2D texture = new Texture2D();
                        texture.setKey(new TextureKey("replacement.png"));
                        replacement.setTexture("DiffuseMap", texture);
                        var geometry = new Geometry("replacement", new Box(1, 1, 1));
                        geometry.setMaterial(replacement);
                        Node wrapper = new Node(); Node contents = new Node();
                        contents.attachChild(geometry); wrapper.attachChild(contents);
                        return wrapper;
                    });
            AnimationLoader.playAnimation(model, art, replacementAssets);
            Geometry replacement = (Geometry) model.getChild("replacement");
            assertNotNull(replacement);
            Material replacementMaterial = replacement.getMaterial();
            state.update(0);
            assertSame(original, oldGeometry.getMaterial(), "Detached geometry must release its fading material");
            assertEquals(RenderState.BlendMode.Alpha, replacement.getMaterial().getAdditionalRenderState().getBlendMode());
            assertFalse(replacement.getMaterial().getAdditionalRenderState().isDepthWrite());
            now.set(10_000_000_000L);
            state.update(0);
            assertSame(replacementMaterial, replacement.getMaterial());
            assertEquals(RenderState.BlendMode.Off, replacementMaterial.getAdditionalRenderState().getBlendMode());
        } finally {
            state.cleanup();
            data.close();
        }
    }
}
