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
package toniarts.openkeeper.tools.convert.map;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimTrack;
import com.jme3.anim.TransformTrack;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.map.ArtResource.ArtResourceType;
import toniarts.openkeeper.view.effect.EffectEmitter;
import toniarts.openkeeper.view.effect.EffectManagerState;

import static org.junit.jupiter.api.Assertions.*;

class EffectMeshAnimationTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, 12})
    void topLevelMeshStartsNestedClipAtAuthoredOrDefaultSpeed(Integer fps) {
        ArtResource resource = animatedResource("test-top-level-effect", fps);
        Effect effect = effect(resource);
        Node sourceModel = animatedModel();
        Node effectNode = load(effect, null, sourceModel);
        Node model = (Node) effectNode.getChild(0);
        AnimComposer composer = composer(model);
        float expectedSpeed = Integer.valueOf(12).equals(fps) ? 0.4f : 1f;

        assertNull(model.getControl(AnimComposer.class));
        assertSame(composer.action(KmfModelLoader.DUMMY_ANIM_CLIP_NAME), composer.getCurrentAction());
        assertEquals(expectedSpeed, composer.getGlobalSpeed());
        effectNode.getParent().updateLogicalState(0.25f);
        assertEquals(0.25f * expectedSpeed, composer.getTime(), 1e-6);
        assertTrue(model.getChild(0).getLocalTranslation().x > 0f);
        assertEquals(Vector3f.ZERO, sourceModel.getChild(0).getLocalTranslation());
        assertNull(composer(sourceModel).getCurrentAction());
    }

    @Test
    void staticMeshStaysStaticAndItsEmitterTemplateKeepsResourceScale() {
        ArtResource resource = new ArtResource("test-static-effect", ArtResourceType.MESH);
        resource.setData(ArtResource.KEY_SCALE, 2.5f);
        Effect effect = effect(resource);
        EffectElement element = generatedElement(effect, resource);
        Node sourceModel = new Node("static-model");

        Node effectNode = load(effect, element, sourceModel);
        Spatial model = effectNode.getChild(0);
        EffectEmitter emitter = (EffectEmitter) effectNode.getChild(1);
        assertEquals(new Vector3f(2.5f, 2.5f, 2.5f), emitter.getSpatial().getLocalScale());
        effectNode.getParent().updateLogicalState(0.25f);
        assertNull(model.getControl(AnimComposer.class));
        assertEquals(Vector3f.ZERO, model.getLocalTranslation());
        assertEquals(Vector3f.UNIT_XYZ, sourceModel.getLocalScale());
    }

    @Test
    void generatedMeshStartsEachCloneAtNormalSpeedAndLeavesTemplateIdle() {
        ArtResource resource = animatedResource("test-generated-effect", 12);
        Effect effect = effect(null);
        EffectElement element = generatedElement(effect, resource);
        element.setDeathElementId(element.getEffectElementId());

        Node effectNode = load(effect, element, animatedModel());
        EffectEmitter emitter = (EffectEmitter) effectNode.getChild(0);
        AnimComposer template = composer((Node) emitter.getSpatial());
        Node first = (Node) emitter.getChild(0);
        assertEquals(1f, composer(first).getGlobalSpeed());
        assertNull(template.getCurrentAction());

        Node root = effectNode.getParent();
        root.updateLogicalState(0.25f);
        assertEquals(0.25, composer(first).getTime(), 1e-6);
        root.updateLogicalState(0.25f);

        assertNull(first.getParent());
        assertEquals(1, emitter.getQuantity());
        Node replacement = (Node) emitter.getChild(0);
        assertNotSame(first, replacement);
        assertNotSame(template, composer(replacement));
        assertSame(composer(replacement).action(KmfModelLoader.DUMMY_ANIM_CLIP_NAME),
                composer(replacement).getCurrentAction());
        assertEquals(1f, composer(replacement).getGlobalSpeed());
        assertEquals(0, composer(replacement).getTime(), 1e-6);
        root.updateLogicalState(0.125f);
        assertEquals(0.125, composer(replacement).getTime(), 1e-6);
        assertNull(template.getCurrentAction());
    }

    private static Node load(Effect effect, EffectElement element, Node model) {
        var data = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEffect" -> effect;
                    case "getEffectElement" -> element;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        var assets = new DesktopAssetManager() {
            @Override
            public Spatial loadModel(ModelKey key) {
                return model;
            }
        };
        return new EffectManagerState(data, assets).load(new Node(), null, 1, false).getEffectNode();
    }

    private static ArtResource animatedResource(String name, Integer fps) {
        var resource = new ArtResource(name, ArtResourceType.ANIMATING_MESH);
        if (fps != null) {
            resource.setData(ArtResource.KEY_FPS, fps);
        }
        return resource;
    }

    private static Effect effect(ArtResource resource) {
        var effect = new Effect();
        effect.setName("mesh effect");
        effect.setArtResource(resource);
        effect.setFlags(EnumSet.noneOf(Effect.EffectFlag.class));
        effect.setGenerateIds(List.of());
        effect.setMinHp(40);
        effect.setMaxHp(40);
        effect.setMinScale(1f);
        effect.setMaxScale(1f);
        return effect;
    }

    private static EffectElement generatedElement(Effect effect, ArtResource resource) {
        var element = new EffectElement();
        element.setEffectElementId(1);
        element.setName("mesh element");
        element.setArtResource(resource);
        element.setFlags(EnumSet.noneOf(EffectElement.EffectElementFlag.class));
        element.setMinHp(10);
        element.setMaxHp(10);
        element.setMinScale(1f);
        element.setMaxScale(1f);
        effect.setFlags(EnumSet.of(Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS));
        effect.setGenerateIds(List.of(element.getEffectElementId()));
        effect.setGenerationType(Effect.GenerationType.DEFAULT);
        effect.setElementsPerTurn((short) 1);
        return element;
    }

    private static Node animatedModel() {
        var model = new Node("model-root");
        var animated = new Node("animated-child");
        var clip = new AnimClip(KmfModelLoader.DUMMY_ANIM_CLIP_NAME);
        var track = new TransformTrack(animated, new float[]{0f, 1f},
                new Vector3f[]{new Vector3f(), new Vector3f(1f, 0f, 0f)}, null, null);
        clip.setTracks(new AnimTrack<?>[]{track});
        var composer = new AnimComposer();
        composer.addAnimClip(clip);
        animated.addControl(composer);
        model.attachChild(animated);
        return model;
    }

    private static AnimComposer composer(Node model) {
        return model.getChild(0).getControl(AnimComposer.class);
    }
}
