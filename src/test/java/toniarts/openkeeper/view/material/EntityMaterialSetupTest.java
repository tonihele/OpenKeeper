/*
 * Copyright (C) 2026 OpenKeeper
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package toniarts.openkeeper.view.material;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.asset.TextureKey;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.shader.VarType;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Effect;
import toniarts.openkeeper.tools.convert.map.MaterialLifecycleFixtures;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.view.effect.EffectEmitter;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.lighting.LightRules;

import static org.junit.jupiter.api.Assertions.*;

class EntityMaterialSetupTest {
    private final DesktopAssetManager assets = new DesktopAssetManager(true);
    private final AtomicInteger preparations = new AtomicInteger();

    EntityMaterialSetupTest() {
        assets.registerLocator("assets", FileLocator.class);
    }

    private EntityMaterialSetup setup(boolean cached) {
        var provider = new JmeMaterialProvider(assets);
        return new EntityMaterialSetup((root, policy, previous) -> {
            preparations.incrementAndGet();
            return provider.prepare(root, policy, previous);
        }, cached);
    }

    private Geometry geometry(String name) {
        var geometry = new Geometry(name, new Quad(1, 1));
        var material = new Material(assets, "MatDefs/DkLighting/Flat.j3md");
        material.setTexture("DiffuseMap", new Texture2D(new Image(Image.Format.RGBA8,
                1, 1, ByteBuffer.allocateDirect(4))));
        geometry.setMaterial(material);
        return geometry;
    }

    private static LightRules rules(int width, int height) {
        var rules = new LightRules();
        rules.initAmbientMap(width, height);
        return rules;
    }

    @Test
    void stableFramesRetainMaterialsWithoutPreparingOrAllocatingVectors() {
        var model = geometry("model");
        var source = model.getMaterial();
        try (var setup = setup(true)) {
            var rules = rules(2, 3);
            setup.register(model, MaterialSetupPolicy.WORLD);
            setup.update(rules);
            var converted = model.getMaterial();
            var center = converted.getParam("MeshCenter").getValue();
            for (int i = 0; i < 100; i++) {
                setup.update(rules);
                setup.flush(rules);
            }
            assertEquals(1, preparations.get());
            assertSame(converted, model.getMaterial());
            assertSame(center, converted.getParam("MeshCenter").getValue());
            assertNull(source.getTextureParam("LightData"));
            assertSame(source.getTextureParam("DiffuseMap").getTextureValue(),
                    converted.getTextureParam("DiffuseMap").getTextureValue());
        }
    }

    @Test
    void deferredAmbientInitializationNeedsNoEntityEventOrRepeatedPreparation() {
        var model = geometry("deferred");
        var rules = new LightRules();
        try (var setup = setup(true)) {
            setup.register(model, MaterialSetupPolicy.CREATURE);
            setup.update(null);
            assertEquals(1, preparations.get());
            assertNull(model.getMaterial().getTextureParam("LightData"));
            setup.update(rules);
            EntityMaterialSetup.materialsChanged(model);
            setup.update(rules);
            assertEquals(2, preparations.get());
            rules.initAmbientMap(4, 5);
            setup.flush(rules);
            assertEquals(2, preparations.get());
            assertNotNull(model.getMaterial().getTextureParam("AmbientMap"));
            assertEquals(0f, model.getMaterial().getParam("AmbientScale").getValue());
        }
    }

    @Test
    void resourceReplacementRebindsIdentityAndDimensionsWithoutRecollecting() {
        var model = geometry("rebound");
        var first = rules(2, 3);
        var second = rules(7, 9);
        try (var setup = setup(true)) {
            setup.register(model, MaterialSetupPolicy.WORLD);
            setup.update(first);
            var material = model.getMaterial();
            var oldLights = material.getTextureParam("LightData").getTextureValue();
            var oldAmbient = material.getTextureParam("AmbientMap").getTextureValue();
            setup.flush(second);
            assertNotSame(oldLights, material.getTextureParam("LightData").getTextureValue());
            assertNotSame(oldAmbient, material.getTextureParam("AmbientMap").getTextureValue());
            assertEquals(new Vector2f(7, 9), material.getParam("AmbientMapSize").getValue());
            var ambient = material.getTextureParam("AmbientMap").getTextureValue();
            second.initAmbientMap(11, 13);
            setup.flush(second);
            assertNotSame(ambient, material.getTextureParam("AmbientMap").getTextureValue());
            assertEquals(new Vector2f(11, 13), material.getParam("AmbientMapSize").getValue());
            second.setTileAmbient(0, 0, ColorRGBA.Red, true);
            second.update(0.016f, null);
            setup.update(second);
            assertEquals(1, preparations.get());
        }
    }

    @Test
    void binderClearsObsoleteAmbientFromAnExplicitUninitializedContext() {
        var model = geometry("bound");
        AssetUtils.applyVertexLight(assets, model, rules(2, 3), false);
        var material = model.getMaterial();
        new LightRules().bindMaterials(java.util.List.of(material));
        assertNull(material.getTextureParam("AmbientMap"));
        assertNull(material.getParam("AmbientMapSize"));
    }

    @Test
    void retainedMaterialsReleaseOldContextWhileTheReplacementIsLoading() {
        var model = geometry("context transition");
        try (var setup = setup(true)) {
            setup.register(model, MaterialSetupPolicy.WORLD);
            setup.update(rules(2, 3));
            var material = model.getMaterial();
            var next = new LightRules();
            setup.flush(next);
            assertNull(material.getTextureParam("AmbientMap"));
            assertNull(material.getParam("AmbientMapSize"));
            assertNotNull(material.getTextureParam("LightData"));
            setup.flush(null);
            assertNull(material.getTextureParam("LightData"));
            next.initAmbientMap(4, 5);
            setup.flush(next);
            assertNotNull(material.getTextureParam("AmbientMap"));
            assertEquals(new Vector2f(4, 5), material.getParam("AmbientMapSize").getValue());
            assertEquals(1, preparations.get());
        }
    }

    @Test
    void childMutationsCoalesceAndLateReplacementIsReadyBeforeSubmission() {
        var root = new Node("root");
        root.attachChild(geometry("initial"));
        var rules = rules(3, 3);
        try (var setup = setup(true)) {
            setup.register(root, MaterialSetupPolicy.WORLD);
            setup.update(rules);
            root.detachAllChildren();
            var replacement = geometry("replacement");
            root.attachChild(replacement);
            EntityMaterialSetup.materialsChanged(replacement);
            EntityMaterialSetup.materialsChanged(root);
            setup.flush(rules);
            assertEquals(2, preparations.get());
            assertNotNull(replacement.getMaterial().getTextureParam("LightData"));
            assertNotNull(replacement.getMaterial().getTextureParam("AmbientMap"));
        }
    }

    @Test
    void blueprintRestorationReportsRealMaterialReplacementEvenWithoutAnimation() {
        var root = new Node("blueprint");
        var geometry = geometry("mesh");
        root.attachChild(geometry);
        var rules = rules(3, 3);
        try (var setup = setup(true)) {
            setup.register(root, MaterialSetupPolicy.WORLD);
            AssetUtils.setBlueprint(assets, root);
            setup.update(rules);
            assertFalse(AssetUtils.isVertexMaterial(geometry.getMaterial()));
            AssetUtils.removeBlueprint(root);
            setup.flush(rules);
            assertTrue(AssetUtils.isVertexMaterial(geometry.getMaterial()));
            assertNotNull(geometry.getMaterial().getTextureParam("AmbientMap"));
            AssetUtils.setBlueprint(assets, root);
            setup.flush(rules);
            AssetUtils.removeBlueprint(root);
            setup.flush(rules);
            assertEquals(4, preparations.get());
        }
    }

    @Test
    void movingAndRotatingModelsRefreshCenterWithoutRepreparing() {
        var root = new Node("moving");
        var mesh = geometry("offset mesh");
        mesh.move(2, 0, 0);
        root.attachChild(mesh);
        var rules = rules(3, 3);
        try (var setup = setup(true)) {
            setup.register(root, MaterialSetupPolicy.WORLD);
            setup.update(rules);
            root.move(4, 2, 3);
            root.rotate(0, 1, 0);
            setup.update(rules);
            assertEquals(root.getWorldBound().getCenter(), mesh.getMaterial().getParam("MeshCenter").getValue());
            assertEquals(1, preparations.get());
        }
    }

    @Test
    void sharedSourcesAreIsolatedAcrossInstancesAndPolicies() {
        var first = geometry("first");
        var second = geometry("second");
        second.setMaterial(first.getMaterial());
        var source = first.getMaterial();
        var rules = rules(3, 3);
        try (var setup = setup(true)) {
            setup.register(first, MaterialSetupPolicy.WORLD);
            setup.register(second, MaterialSetupPolicy.CREATURE);
            setup.update(rules);
            assertNotSame(first.getMaterial(), second.getMaterial());
            assertNotSame(source, first.getMaterial());
            assertEquals(1f, first.getMaterial().getParam("AmbientScale").getValue());
            assertEquals(0f, second.getMaterial().getParam("AmbientScale").getValue());
            assertFalse(AssetUtils.isVertexMaterial(source));
        }
    }

    @Test
    void nestedRegistrationsKeepOneAuthorityAndTransferOwnershipOnClose() {
        var parent = new Node("entity");
        var child = new Node("nested effect");
        var mesh = geometry("nested mesh");
        child.attachChild(mesh);
        parent.attachChild(child);
        var rules = rules(2, 2);
        try (var parentSetup = setup(true); var childSetup = setup(true)) {
            parentSetup.register(parent, MaterialSetupPolicy.WORLD);
            parentSetup.update(rules);
            childSetup.register(child, MaterialSetupPolicy.CREATURE);
            parentSetup.flush(rules);
            childSetup.update(rules);
            var ownedByChild = mesh.getMaterial();
            parentSetup.register(parent, MaterialSetupPolicy.FULL_BRIGHT);
            parentSetup.flush(rules);
            assertSame(ownedByChild, mesh.getMaterial());
            assertEquals(0f, ownedByChild.getParam("MinLight").getValue());
            assertEquals(0f, ownedByChild.getParam("AmbientScale").getValue());
            childSetup.close();
            parentSetup.flush(rules);
            assertNotSame(ownedByChild, mesh.getMaterial());
            assertEquals(1f, mesh.getMaterial().getParam("MinLight").getValue());
            assertEquals(0f, ownedByChild.getParam("MinLight").getValue());
        }
    }

    @Test
    void fullBrightPolicyIsReversibleWithoutCloningOnEverySwitch() {
        var mesh = geometry("held");
        var rules = rules(2, 2);
        try (var setup = setup(true)) {
            setup.register(mesh, MaterialSetupPolicy.WORLD);
            setup.update(rules);
            var material = mesh.getMaterial();
            setup.register(mesh, MaterialSetupPolicy.FULL_BRIGHT);
            setup.update(null);
            assertEquals(1f, material.getParam("RockFloor").getValue());
            assertNull(material.getTextureParam("LightData"));
            assertNull(material.getTextureParam("AmbientMap"));
            assertNull(material.getParam("AmbientMapSize"));
            assertNull(material.getParam("MeshCenter"));
            setup.register(mesh, MaterialSetupPolicy.CREATURE);
            setup.update(null);
            assertEquals(0f, material.getParam("RockFloor").getValue());
            assertEquals(0f, material.getParam("MinLight").getValue());
            assertNull(material.getTextureParam("AmbientMap"));
            setup.update(rules);
            assertSame(material, mesh.getMaterial());
            assertEquals(0f, material.getParam("RockFloor").getValue());
            assertEquals(0f, material.getParam("MinLight").getValue());
            assertNotNull(material.getTextureParam("AmbientMap"));
            assertNotNull(material.getParam("MeshCenter"));
        }
    }

    @Test
    void clonedAndExportedModelsNeverInheritRegistrationOrCachedMaterialLists() throws Exception {
        var root = new Node("template");
        root.attachChild(geometry("mesh"));
        var rules = rules(2, 2);
        try (var first = setup(true); var second = setup(true)) {
            first.register(root, MaterialSetupPolicy.WORLD);
            first.update(rules);
            Node clone = (Node) root.clone();
            second.register(clone, MaterialSetupPolicy.CREATURE);
            second.update(rules);
            assertEquals(2, preparations.get());
            assertNotSame(((Geometry) root.getChild(0)).getMaterial(), ((Geometry) clone.getChild(0)).getMaterial());
            var bytes = new ByteArrayOutputStream();
            BinaryExporter.getInstance().save(root, bytes);
            var importer = BinaryImporter.getInstance();
            importer.setAssetManager(assets);
            Spatial loaded = (Spatial) importer.load(new ByteArrayInputStream(bytes.toByteArray()));
            second.register(loaded, MaterialSetupPolicy.WORLD);
            second.update(rules);
            assertEquals(3, preparations.get());
        }
    }

    @Test
    void removalAndCloseCancelDeferredWorkAndDetachMutationRouting() {
        var mesh = geometry("removed");
        var setup = setup(true);
        setup.register(mesh, MaterialSetupPolicy.WORLD);
        setup.unregister(mesh);
        setup.update(null);
        setup.update(rules(3, 3));
        assertEquals(0, preparations.get());
        assertEquals(0, setup.registeredModels());
        setup.register(mesh, MaterialSetupPolicy.WORLD);
        setup.close();
        setup.close();
        EntityMaterialSetup.materialsChanged(mesh);
        assertEquals(0, setup.registeredModels());
        assertNull(mesh.getControl(EntityMaterialSetup.InvalidationControl.class));
        assertThrows(IllegalStateException.class, () -> setup.register(mesh, MaterialSetupPolicy.WORLD));
    }

    @Test
    void referenceConfigurationActuallyRepreparesRegisteredModels() {
        var mesh = geometry("reference");
        var rules = rules(2, 2);
        try (var setup = setup(false)) {
            setup.register(mesh, MaterialSetupPolicy.WORLD);
            setup.update(rules);
            var material = mesh.getMaterial();
            setup.update(rules);
            assertSame(material, mesh.getMaterial());
            assertEquals(2, preparations.get());
        }
    }

    @Test
    void liveRegistrationRejectsMutationFromAnotherThread() throws Exception {
        var mesh = geometry("owned");
        try (var setup = setup(true)) {
            setup.register(mesh, MaterialSetupPolicy.WORLD);
            setup.update(rules(2, 2));
            var failure = new AtomicReference<Throwable>();
            var thread = new Thread(() -> {
                try {
                    EntityMaterialSetup.materialsChanged(mesh);
                } catch (Throwable thrown) {
                    failure.set(thrown);
                }
            });
            thread.start();
            thread.join();
            assertInstanceOf(IllegalStateException.class, failure.get());
        }
    }

    @Test
    void conversionPreservesAlphaDiffuseAndDepthStateAndSkipsUnsupportedMaterials() {
        var root = new Node("model");
        var mesh = geometry("alpha");
        var source = mesh.getMaterial();
        source.setTransparent(true);
        source.setFloat("AlphaDiscardThreshold", 0.4f);
        source.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        source.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        source.getAdditionalRenderState().setDepthWrite(false);
        root.attachChild(mesh);
        var unshaded = new Geometry("unsupported", new Quad(1, 1));
        var untouched = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        unshaded.setMaterial(untouched);
        root.attachChild(unshaded);
        try (var setup = setup(true)) {
            setup.register(root, MaterialSetupPolicy.WORLD);
            setup.update(rules(2, 2));
            var material = mesh.getMaterial();
            assertTrue(material.isTransparent());
            assertEquals(0.4f, material.getParam("AlphaDiscardThreshold").getValue());
            assertEquals(RenderState.BlendMode.Alpha, material.getAdditionalRenderState().getBlendMode());
            assertEquals(RenderState.FaceCullMode.Off, material.getAdditionalRenderState().getFaceCullMode());
            assertFalse(material.getAdditionalRenderState().isDepthWrite());
            assertSame(untouched, unshaded.getMaterial());
        }
    }

    @Test
    void effectCreatedBeforeMapReadinessRestartsWithAFreshTargetAndReleasesOldContext() {
        var effect = MaterialLifecycleFixtures.effect();
        IKwdFile kwd = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getEffect")) return effect;
                    throw new UnsupportedOperationException(method.getName());
                });
        var context = new AtomicReference<LightRules>();
        var setup = setup(true);
        var manager = new EffectManagerState(kwd, assets, setup);
        manager.setLightRulesSupplier(context::get);
        var world = new Node("world");
        var visual = manager.load(world, Vector3f.ZERO, 1, true);
        manager.update(0.001f);
        assertEquals(1, preparations.get());
        var first = rules(2, 2);
        context.set(first);
        manager.update(0.001f);
        assertNotNull(first.getThingLight(visual));
        assertEquals(1, preparations.get());
        Spatial oldTarget = visual.getEffectNode().getChild(0);
        world.updateLogicalState(0.1f);
        assertEquals(0, setup.registeredModels());
        manager.update(0.001f);
        assertNotSame(oldTarget, visual.getEffectNode().getChild(0));
        assertEquals(2, preparations.get());
        assertEquals(1, setup.registeredModels());
        var second = rules(3, 4);
        context.set(second);
        manager.update(0.001f);
        assertNull(first.getThingLight(visual));
        assertNotNull(second.getThingLight(visual));
        manager.clearActiveEffects();
        assertNull(second.getThingLight(visual));
        assertEquals(0, setup.registeredModels());
        manager.cleanup();
    }

    @Test
    void removedEffectCannotRecreateLightingWhenTheContextBecomesReady() {
        var effect = MaterialLifecycleFixtures.effect();
        IKwdFile kwd = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getEffect")) return effect;
                    throw new UnsupportedOperationException(method.getName());
                });
        var context = new AtomicReference<LightRules>();
        var setup = setup(true);
        var manager = new EffectManagerState(kwd, assets, setup);
        manager.setLightRulesSupplier(context::get);
        var world = new Node("world");
        var visual = manager.load(world, Vector3f.ZERO, 1, true);
        visual.removeEffect();
        var ready = rules(2, 2);
        context.set(ready);
        assertFalse(visual.update(0.001f));
        manager.update(0.001f);
        assertNull(ready.getThingLight(visual));
        assertEquals(0, setup.registeredModels());
        assertEquals(0, world.getQuantity());
        manager.clearActiveEffects();
        manager.cleanup();
    }

    @Test
    void meshElementSelfRespawnUnregistersTheDyingInstanceAndPreparesTheReplacement() {
        var effect = MaterialLifecycleFixtures.effect();
        var element = MaterialLifecycleFixtures.element();
        var rules = rules(2, 2);
        try (var setup = setup(true)) {
            var emitter = new EffectEmitter(element, effect, setup) {
                @Override public void onDeath(Vector3f location) { }
                @Override public void onHit(Vector3f location) { }
            };
            emitter.setSpatial(geometry("template"));
            emitter.emitOne();
            setup.update(rules);
            Spatial old = emitter.getChild(0);
            emitter.updateLogicalState(0.1f);
            setup.flush(rules);
            assertEquals(1, emitter.getQuantity());
            assertEquals(1, setup.registeredModels());
            assertNotSame(old, emitter.getChild(0));
            assertNull(old.getControl(EntityMaterialSetup.InvalidationControl.class));
            assertNotNull(((Geometry) emitter.getChild(0)).getMaterial().getTextureParam("AmbientMap"));
            assertEquals(2, preparations.get());
            emitter.killAllParticles();
            assertEquals(0, setup.registeredModels());
            assertEquals(0, emitter.getQuantity());
        }
    }

    @Test
    void groupedGeometryIsNeverRematerialized() {
        var mesh = geometry("batched");
        var original = mesh.getMaterial();
        var batch = new BatchNode("batch");
        batch.attachChild(mesh);
        batch.updateGeometricState();
        batch.batch();
        try (var setup = setup(true)) {
            setup.register(mesh, MaterialSetupPolicy.WORLD);
            assertDoesNotThrow(() -> setup.update(rules(2, 2)));
            assertSame(original, mesh.getMaterial());
        }
    }

    @Test
    void pageLightingExtensionRemainsOnTheSharedPageMaterialButNeverOnAnEntityCopy() {
        var page = new Material(assets, "MatDefs/DkLighting/Vertex.j3md");
        // PR #1012's additive parameter contract, on the current MeshCenter-aware definition.
        var definition = page.getMaterialDef();
        definition.addMaterialParamTexture(VarType.Texture2D, "LightIndexData", ColorSpace.Linear, null);
        definition.addMaterialParam(VarType.Int, "LightIndexRow", null);
        definition.addMaterialParam(VarType.Int, "LightIndexCount", null);
        var indices = new Texture2D(new Image(Image.Format.RGBA8, 1, 1, ByteBuffer.allocateDirect(4)));
        page.setTexture("LightIndexData", indices);
        page.setInt("LightIndexRow", 3);
        page.setInt("LightIndexCount", 7);
        var mesh = geometry("entity sharing page source");
        mesh.setMaterial(page);
        try (var setup = setup(true)) {
            var rules = rules(2, 2);
            setup.register(mesh, MaterialSetupPolicy.WORLD);
            setup.update(rules);
            assertNotSame(page, mesh.getMaterial());
            assertNull(mesh.getMaterial().getTextureParam("LightIndexData"));
            assertNull(mesh.getMaterial().getParam("LightIndexRow"));
            assertNull(mesh.getMaterial().getParam("LightIndexCount"));
            rules.bindMaterials(List.of(page));
            assertSame(indices, page.getTextureParam("LightIndexData").getTextureValue());
            assertEquals(3, page.getParam("LightIndexRow").getValue());
            assertEquals(7, page.getParam("LightIndexCount").getValue());
        }
    }

    @Test
    void attachedLightPositionUpdatesReuseStorageAndPreserveOwnerAndOffsetVectors() {
        var rules = rules(2, 2);
        Object owner = new Object();
        var position = new Vector3f(2, 3, 4);
        var offset = new Vector3f(0.5f, 1, 2);
        var light = rules.addThingLight(owner, Vector3f.ZERO, MaterialLifecycleFixtures.effect().getLight(), null, 1, true);
        Vector3f stored = light.getPosition();
        rules.updateThingLightPosition(owner, position, offset);
        assertSame(stored, light.getPosition());
        assertEquals(new Vector3f(2.5f, 4, 6), stored);
        assertEquals(new Vector3f(2, 3, 4), position);
        assertEquals(new Vector3f(0.5f, 1, 2), offset);
    }

    private static IKwdFile effects(Effect... definitions) {
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getEffect")) {
                        for (var effect : definitions) {
                            if (effect.getEffectId() == (int) arguments[0]) return effect;
                        }
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    @Test
    void artlessEffectsReleaseRegistrationsAndStayTerminalAfterNaturalCompletion() {
        var setup = setup(true);
        var manager = new EffectManagerState(effects(MaterialLifecycleFixtures.artlessEffect(1, List.of(), 0)), assets, setup);
        var context = new AtomicReference<>(rules(2, 2));
        manager.setLightRulesSupplier(context::get);
        var world = new Node("world");
        for (int i = 0; i < 100; i++) {
            var visual = manager.load(world, null, 1, false);
            manager.update(0.1f);
            assertEquals(0, world.getQuantity());
            assertNull(context.get().getThingLight(visual));
            assertEquals(0, setup.registeredModels());
            context.set(rules(3, 3));
            assertFalse(visual.update(0.1f));
            assertNull(context.get().getThingLight(visual));
            visual.removeEffect();
        }
        manager.clearActiveEffects();
        assertEquals(0, setup.registeredModels());
        manager.cleanup();
    }

    @Test
    void artlessParentAndGeneratedChildFinishAfterDelayedContextWithoutOrphanRegistrations() {
        var setup = setup(true);
        var parent = MaterialLifecycleFixtures.artlessEffect(2, List.of(1), 0);
        var manager = new EffectManagerState(effects(parent, MaterialLifecycleFixtures.effect()), assets, setup);
        var context = new AtomicReference<LightRules>();
        manager.setLightRulesSupplier(context::get);
        var world = new Node("world");
        var visual = manager.load(world, null, 2, false);
        manager.update(0.001f);
        context.set(rules(2, 2));
        manager.update(0.001f);
        assertEquals(1, setup.registeredModels()); // The model-bearing child only.
        world.updateLogicalState(0.1f);
        manager.update(0.1f);
        assertEquals(0, world.getQuantity());
        assertEquals(0, setup.registeredModels());
        assertNull(context.get().getThingLight(visual));
        manager.cleanup();
    }

    @Test
    void artlessSelfRestartIsBoundedAcrossContextReadinessAndExplicitRemoval() {
        var setup = setup(true);
        var manager = new EffectManagerState(effects(MaterialLifecycleFixtures.artlessEffect(1, List.of(), 1)), assets, setup);
        var context = new AtomicReference<LightRules>();
        manager.setLightRulesSupplier(context::get);
        var world = new Node("world");
        var visual = manager.load(world, null, 1, false);
        for (int i = 0; i < 100; i++) {
            if (i == 5) context.set(rules(2, 2));
            manager.update(0.1f);
            assertEquals(1, world.getQuantity());
            assertEquals(0, setup.registeredModels());
        }
        assertNotNull(context.get().getThingLight(visual));
        manager.clearActiveEffects();
        assertNull(context.get().getThingLight(visual));
        assertEquals(0, world.getQuantity());
        assertFalse(visual.update(0.1f));
        manager.cleanup();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void blueprintRestorationClearsInheritedBindingsWhileTheContextIsMissingOrLoading(boolean loadingContext) {
        var root = new Node("restored");
        var mesh = geometry("mesh");
        root.attachChild(mesh);
        var originalContext = rules(2, 2);
        var loading = new LightRules();
        LightRules pendingContext = loadingContext ? loading : null;
        try (var setup = setup(true)) {
            setup.register(root, MaterialSetupPolicy.WORLD);
            setup.update(originalContext);
            var oldMaterial = mesh.getMaterial();
            var oldLight = oldMaterial.getTextureParam("LightData").getTextureValue();
            var oldAmbient = oldMaterial.getTextureParam("AmbientMap").getTextureValue();
            AssetUtils.setBlueprint(assets, root);
            setup.flush(originalContext);
            setup.flush(pendingContext);
            AssetUtils.removeBlueprint(root);
            setup.flush(pendingContext);
            if (loadingContext) {
                assertNotSame(oldLight, mesh.getMaterial().getTextureParam("LightData").getTextureValue());
            } else {
                assertNull(mesh.getMaterial().getTextureParam("LightData"));
            }
            assertNull(mesh.getMaterial().getTextureParam("AmbientMap"));
            assertNull(mesh.getMaterial().getParam("AmbientMapSize"));
            setup.flush(loading);
            assertNotSame(oldLight, mesh.getMaterial().getTextureParam("LightData").getTextureValue());
            assertNull(mesh.getMaterial().getTextureParam("AmbientMap"));
            int count = preparations.get();
            for (int i = 0; i < 100; i++) setup.update(loading);
            assertEquals(count, preparations.get());
            loading.initAmbientMap(3, 4);
            setup.flush(loading);
            assertNotSame(oldAmbient, mesh.getMaterial().getTextureParam("AmbientMap").getTextureValue());
            assertEquals(new Vector2f(3, 4), mesh.getMaterial().getParam("AmbientMapSize").getValue());
            assertEquals(count, preparations.get());
        }
    }

    @Test
    void preparedClonesAndImportedModelsAreSanitizedBeforeAmbientReadiness() throws Exception {
        var root = new Node("template");
        var mesh = geometry("mesh");
        root.attachChild(mesh);
        try (var first = setup(true); var second = setup(true)) {
            first.register(root, MaterialSetupPolicy.WORLD);
            first.update(rules(2, 2));
            var original = mesh.getMaterial();
            var oldAmbient = original.getTextureParam("AmbientMap").getTextureValue();
            Node clone = (Node) root.clone();
            var bytes = new ByteArrayOutputStream();
            BinaryExporter.getInstance().save(root, bytes);
            var importer = BinaryImporter.getInstance();
            importer.setAssetManager(assets);
            var loaded = (Node) importer.load(new ByteArrayInputStream(bytes.toByteArray()));
            var loading = new LightRules();
            for (var reused : List.of(clone, loaded)) {
                var reusedMesh = (Geometry) reused.getChild(0);
                var inherited = reusedMesh.getMaterial();
                second.register(reused, MaterialSetupPolicy.CREATURE);
                second.flush(null);
                assertNotSame(inherited, reusedMesh.getMaterial());
                assertNull(reusedMesh.getMaterial().getTextureParam("LightData"));
                assertNull(reusedMesh.getMaterial().getTextureParam("AmbientMap"));
                assertNull(reusedMesh.getMaterial().getParam("AmbientMapSize"));
                second.flush(loading);
                assertNotNull(reusedMesh.getMaterial().getTextureParam("LightData"));
                assertNull(reusedMesh.getMaterial().getTextureParam("AmbientMap"));
            }
            assertSame(oldAmbient, original.getTextureParam("AmbientMap").getTextureValue());
            int count = preparations.get();
            loading.initAmbientMap(4, 5);
            second.flush(loading);
            assertEquals(count, preparations.get());
            for (var reused : List.of(clone, loaded)) {
                assertEquals(new Vector2f(4, 5), ((Geometry) reused.getChild(0)).getMaterial().getParam("AmbientMapSize").getValue());
            }
        }
    }

    @Test
    void noMapEffectPreviewKeepsInitialNewAndRespawnedMeshesFullBright() {
        String modelName = "O1-full-bright-preview-fixture";
        var source = geometry("preview mesh");
        source.setMaterial(new Material(assets, "MatDefs/DkLighting/Vertex.j3md"));
        var texture = new Texture2D(new Image(Image.Format.RGBA8, 1, 1, ByteBuffer.allocateDirect(4)));
        texture.setKey(new TextureKey("preview-fixture.png", false));
        source.getMaterial().setTexture("DiffuseMap", texture);
        var template = new Node("model");
        template.attachChild(source);
        var previewAssets = new DesktopAssetManager(true) {
            @Override public Spatial loadModel(ModelKey key) {
                assertTrue(key.getName().endsWith(modelName + ".j3o"));
                return template.clone();
            }
        };
        var effect = MaterialLifecycleFixtures.meshEffect(modelName);
        var element = MaterialLifecycleFixtures.meshElement(modelName);
        var kwd = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getEffect" -> effect;
                    case "getEffectElement" -> element;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        var setup = new EntityMaterialSetup(previewAssets);
        var manager = new EffectManagerState(kwd, previewAssets, setup, MaterialSetupPolicy.FULL_BRIGHT);
        var preview = new Node("preview");
        var visual = manager.load(preview, null, 1, false);
        // The viewer's compatibility walk must respect the nested owners.
        AssetUtils.applyFullLight(preview);
        manager.render(null);
        assertEquals(2, setup.registeredModels());
        assertFullBright(preview);
        var emitter = (EffectEmitter) visual.getEffectNode().getChildren().stream()
                .filter(EffectEmitter.class::isInstance).findFirst().orElseThrow();
        Spatial dying = emitter.getChild(0);
        preview.updateLogicalState(0.1f);
        manager.render(null);
        assertEquals(1, setup.registeredModels());
        assertNotSame(dying, emitter.getChild(0));
        emitter.emitOne();
        manager.render(null);
        assertEquals(2, setup.registeredModels());
        assertFullBright(preview);

        var mesh = (Geometry) ((Node) emitter.getChild(0)).getChild(0);
        var restored = mesh.getMaterial();
        mesh.setMaterial(new Material(assets, "Common/MatDefs/Misc/ShowNormals.j3md"));
        EntityMaterialSetup.materialsChanged(mesh);
        manager.render(null);
        mesh.setMaterial(restored);
        EntityMaterialSetup.materialsChanged(mesh);
        manager.render(null);
        assertFullBright(preview);
        manager.clearActiveEffects();
        assertEquals(0, setup.registeredModels());
        assertEquals(0, preview.getQuantity());
        manager.loadSingleEffect(preview, null, 1, false);
        manager.render(null);
        assertFullBright(preview);
        assertEquals(0f, source.getMaterial().getParam("RockFloor").getValue());
        manager.cleanup();
        assertEquals(0, setup.registeredModels());
    }

    private static void assertFullBright(Spatial root) {
        var count = new AtomicInteger();
        root.depthFirstTraversal(spatial -> {
            if (spatial instanceof Geometry geometry && AssetUtils.isVertexMaterial(geometry.getMaterial())) {
                var material = geometry.getMaterial();
                assertEquals(1f, material.getParam("RockFloor").getValue());
                assertEquals(1f, material.getParam("MinLight").getValue());
                assertNull(material.getTextureParam("LightData"));
                assertNull(material.getTextureParam("AmbientMap"));
                count.incrementAndGet();
            }
        });
        assertTrue(count.get() > 0);
    }

}
