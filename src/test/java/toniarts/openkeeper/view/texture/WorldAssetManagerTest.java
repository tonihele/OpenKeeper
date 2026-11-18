/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.view.texture;

import static org.junit.jupiter.api.Assertions.*;

import com.jme3.asset.AssetEventListener;
import com.jme3.asset.AssetKey;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.asset.TextureKey;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import toniarts.openkeeper.game.data.TextureFiltering;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

class WorldAssetManagerTest {
    @TempDir Path directory;

    private DesktopAssetManager assets() {
        var assets = new DesktopAssetManager(true);
        assets.registerLocator(directory.toString(), FileLocator.class);
        return assets;
    }

    private void image(String name) throws Exception {
        Path p = directory.resolve(name);
        Files.createDirectories(p.getParent());
        ImageIO.write(new BufferedImage(32, 64, BufferedImage.TYPE_INT_ARGB), "png", p.toFile());
    }

    private static Texture texture(Node model) {
        return ((Geometry) model.getChild(0))
                .getMaterial()
                .getTextureParam("ColorMap")
                .getTextureValue();
    }

    @Test
    void missesHitsAndDetachedSamplerClonesRespectPreferenceWithoutChangingCacheKeys()
            throws Exception {
        image("Textures/shared.png");
        var raw = assets();
        var policy = new WorldTextureFiltering(TextureFiltering.ANISOTROPIC_16, () -> 4);
        var world = new WorldAssetManager(raw, policy);
        var key = new TextureKey("Textures/shared.png", false);
        var unchanged = key.clone();
        var loads = new AtomicInteger();
        raw.addAssetEventListener(
                new AssetEventListener() {
                    @Override
                    public void assetRequested(AssetKey key) {}

                    @Override
                    public void assetDependencyNotFound(AssetKey parent, AssetKey child) {}

                    @Override
                    public void assetLoaded(AssetKey key) {
                        loads.incrementAndGet();
                    }
                });
        var ui = raw.loadTexture(key);
        var one = world.loadTexture(new TextureKey("Textures/shared.png", false));
        var two = world.loadTexture(new TextureKey("Textures/shared.png", false));
        assertEquals(1, loads.get());
        assertNotSame(one, two);
        assertSame(ui.getImage(), one.getImage());
        assertSame(one.getImage(), two.getImage());
        assertEquals(4, one.getAnisotropicFilter());
        assertEquals(4, two.getAnisotropicFilter());
        assertEquals(Texture.MinFilter.BilinearNoMipMaps, ui.getMinFilter());
        assertNull(((Texture) raw.getFromCache(key)).getKey());
        assertEquals(unchanged, key);
        policy.refresh(TextureFiltering.BILINEAR);
        assertEquals(Texture.MinFilter.BilinearNearestMipMap, one.getMinFilter());
        assertEquals(Texture.MinFilter.BilinearNearestMipMap, two.getMinFilter());
        assertEquals(1, one.getAnisotropicFilter());
        var future = world.loadTexture(new TextureKey("Textures/shared.png", false));
        assertEquals(Texture.MinFilter.BilinearNearestMipMap, future.getMinFilter());
        assertSame(one.getImage(), future.getImage());
        assertEquals(1, loads.get());
        assertEquals(Texture.MinFilter.BilinearNoMipMaps, raw.loadTexture(key).getMinFilter());
        assertNull(((Texture) raw.getFromCache(key)).getKey());
        world.clearCache();
        var reloaded = world.loadTexture(new TextureKey("Textures/shared.png", false));
        assertNotSame(one.getImage(), reloaded.getImage());
        policy.refresh(TextureFiltering.ANISOTROPIC_8);
        assertEquals(4, one.getAnisotropicFilter());
        assertEquals(4, reloaded.getAnisotropicFilter());
        assertEquals(2, loads.get());
    }

    @Test
    void importedJ3oOverridesSerializedFiltersAndCachedModelsShareRegisteredSamplers()
            throws Exception {
        image("Textures/embedded.png");
        var raw = assets();
        var original = raw.loadTexture(new TextureKey("Textures/embedded.png", false));
        original.setMinFilter(Texture.MinFilter.Trilinear);
        original.setAnisotropicFilter(16);
        var material = new Material(raw, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap", original);
        var geometry = new Geometry("mesh", new Quad(1, 1));
        geometry.setMaterial(material);
        var model = new Node();
        model.attachChild(geometry);
        BinaryExporter.getInstance().save(model, directory.resolve("model.j3o").toFile());
        var policy = new WorldTextureFiltering(TextureFiltering.BILINEAR, () -> 4);
        var world = new WorldAssetManager(raw, policy);
        var loaded = (Node) world.loadModel("model.j3o");
        assertEquals(Texture.MinFilter.BilinearNearestMipMap, texture(loaded).getMinFilter());
        assertEquals(1, texture(loaded).getAnisotropicFilter());
        var detached = loaded.clone(true);
        assertSame(texture(loaded), texture(detached));
        policy.refresh(TextureFiltering.ANISOTROPIC_8);
        assertEquals(4, texture(detached).getAnisotropicFilter());
        var cached = (Node) world.loadModel(new ModelKey("model.j3o"));
        assertSame(texture(loaded), texture(cached));
        assertEquals(4, texture(cached).getAnisotropicFilter());
        assertNull(((Node) raw.getFromCache(new ModelKey("model.j3o"))).getKey());
    }

    @Test
    void materialOptionsAreProcessedBeforePolicyAndStreamsArePreparedWithoutInventingAKey()
            throws Exception {
        image("Textures/surface.png");
        var raw = assets();
        Files.writeString(
                directory.resolve("old.j3m"),
                """
                Material old : Common/MatDefs/Misc/Unshaded.j3md {
                    MaterialParameters {
                        ColorMap : MinBilinearNoMipMaps Textures/surface.png
                    }
                }
                """);
        var policy = new WorldTextureFiltering(TextureFiltering.ANISOTROPIC_8, () -> 16);
        var world = new WorldAssetManager(raw, policy);
        var material = world.loadMaterial("old.j3m");
        assertEquals(
                8, material.getTextureParam("ColorMap").getTextureValue().getAnisotropicFilter());
        try (var input =
                new ByteArrayInputStream(
                        Files.readAllBytes(directory.resolve("Textures/surface.png")))) {
            var streamed = world.loadAssetFromStream(new TextureKey("stream.png", false), input);
            assertNull(streamed.getKey());
            assertEquals(8, streamed.getAnisotropicFilter());
            assertTrue(streamed.getImage().isGeneratedMipmapsRequired());
        }
    }

    @Test
    void concurrentlyLoadedSamplersJoinTheCurrentSnapshotBeforePublication() throws Exception {
        image("Textures/threaded.png");
        var raw = assets();
        var policy = new WorldTextureFiltering(TextureFiltering.TRILINEAR, () -> 16);
        var world = new WorldAssetManager(raw, policy);
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        raw.addAssetEventListener(
                new AssetEventListener() {
                    @Override
                    public void assetRequested(AssetKey key) {
                        started.countDown();
                        try {
                            assertTrue(release.await(5, TimeUnit.SECONDS));
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }
                    }

                    @Override
                    public void assetLoaded(AssetKey key) {}

                    @Override
                    public void assetDependencyNotFound(AssetKey parent, AssetKey child) {}
                });
        try (var executor = Executors.newSingleThreadExecutor()) {
            var pending =
                    executor.submit(() ->
                            world.loadTexture(new TextureKey("Textures/threaded.png", false)));
            assertTrue(started.await(5, TimeUnit.SECONDS));
            policy.refresh(TextureFiltering.BILINEAR);
            release.countDown();
            var loaded = pending.get(5, TimeUnit.SECONDS);
            assertEquals(Texture.MinFilter.BilinearNearestMipMap, loaded.getMinFilter());
            policy.refresh(TextureFiltering.ANISOTROPIC_8);
            assertEquals(8, loaded.getAnisotropicFilter());
        } finally {
            release.countDown();
        }
    }
}
