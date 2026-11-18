/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.view.texture;

import static org.junit.jupiter.api.Assertions.*;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureCubeMap;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;

import org.junit.jupiter.api.Test;

import toniarts.openkeeper.game.data.TextureFiltering;

import java.util.concurrent.atomic.AtomicInteger;

class WorldTextureFilteringTest {
    static Texture2D texture(String name, int width, int height) {
        var texture =
                new Texture2D(
                        new Image(
                                Image.Format.RGBA8,
                                width,
                                height,
                                BufferUtils.createByteBuffer(width * height * 4),
                                ColorSpace.sRGB));
        if (name != null) texture.setKey(new TextureKey(name, false));
        return texture;
    }

    @Test
    void roleNotSizeDeterminesMipEligibilityAndPreservesPixelsAndWrapping() {
        var policy = new WorldTextureFiltering(TextureFiltering.BILINEAR, () -> 16);
        for (int[] size :
                new int[][] {{1, 1}, {16, 16}, {32, 128}, {64, 64}, {128, 128}, {127, 63}}) {
            var texture = texture("Textures/world.png", size[0], size[1]);
            texture.setWrap(Texture.WrapMode.Repeat);
            var image = texture.getImage();
            var pixels = image.getData(0);
            assertSame(texture, policy.prepare(texture));
            assertSame(image, texture.getImage());
            assertSame(pixels, image.getData(0));
            assertTrue(image.isGeneratedMipmapsRequired());
            assertEquals(Texture.MinFilter.BilinearNearestMipMap, texture.getMinFilter());
            assertEquals(Texture.WrapMode.Repeat, texture.getWrap(Texture.WrapAxis.S));
            assertEquals(ColorSpace.sRGB, image.getColorSpace());
        }
    }

    @Test
    void equalIndependentSamplersAndDetachedModelsBothRefreshWithCurrentHardwareLimit() {
        var limit = new AtomicInteger(4);
        var policy = new WorldTextureFiltering(TextureFiltering.TRILINEAR, limit::get);
        var one = texture("Textures/world.png", 64, 64);
        policy.prepare(one);
        var two = one.clone();
        policy.prepare(two);
        assertEquals(one, two);
        assertNotSame(one, two);
        for (int i = 0; i < 4; i++) {
            policy.refresh(TextureFiltering.BILINEAR);
            assertEquals(Texture.MinFilter.BilinearNearestMipMap, one.getMinFilter());
            assertEquals(Texture.MinFilter.BilinearNearestMipMap, two.getMinFilter());
            policy.refresh(TextureFiltering.ANISOTROPIC_16);
            assertEquals(4, one.getAnisotropicFilter());
            assertEquals(4, two.getAnisotropicFilter());
        }
        limit.set(1);
        policy.refresh(TextureFiltering.ANISOTROPIC_16);
        assertEquals(1, policy.getEffectiveAnisotropy());
        assertEquals(1, two.getAnisotropicFilter());
        assertSame(one.getImage(), two.getImage());
    }

    @Test
    void excludedUiAtlasesTargetsAndSpecialSamplerTypesRemainUnchanged() {
        var policy = new WorldTextureFiltering(TextureFiltering.ANISOTROPIC_16, () -> 16);
        for (String name :
                new String[] {
                    "Textures/GUI/icon.png",
                    "Textures\\GUI\\icon.png",
                    "Interface/Fonts/font.png",
                    "Textures/InGameDigit0.png",
                    "Textures/MapBack.PNG",
                    "Textures/MapRock.png",
                    "Textures/MapOver.png",
                    "Textures/LoadingScreen/x.png",
                    "Textures/TitleScreen/x.png",
                    "Sprites/creature.png"
                }) {
            var t = texture(name, 128, 128);
            t.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
            t.setMagFilter(Texture.MagFilter.Nearest);
            policy.prepare(t);
            assertEquals(Texture.MinFilter.NearestNoMipMaps, t.getMinFilter());
            assertEquals(Texture.MagFilter.Nearest, t.getMagFilter());
            assertFalse(t.getImage().isGeneratedMipmapsRequired());
        }
        var target = new Texture2D(128, 128, Image.Format.RGBA8);
        target.setKey(new TextureKey("target.png"));
        policy.prepare(target);
        assertFalse(target.getImage().isGeneratedMipmapsRequired());
        var depth = new Texture2D(128, 128, Image.Format.Depth);
        depth.setKey(new TextureKey("depth.png"));
        policy.prepare(depth);
        assertFalse(depth.getImage().isGeneratedMipmapsRequired());
        var cube = new TextureCubeMap();
        cube.setImage(texture(null, 4, 4).getImage());
        cube.setKey(new TextureKey("cube.dds"));
        policy.prepare(cube);
        assertFalse(cube.getImage().isGeneratedMipmapsRequired());
    }

    @Test
    void onlySurfaceSlotsArePreparedAndAnimationStripsKeepTheirSampler() {
        var assets = new DesktopAssetManager(true);
        var material = new Material(assets, "MatDefs/DkLighting/Vertex.j3md");
        var surface = texture("Textures/surface.png", 32, 32);
        var data = texture("Textures/data.png", 128, 128);
        material.setTexture("DiffuseMap", surface);
        material.setTexture("LightData", data);
        var policy = new WorldTextureFiltering(TextureFiltering.TRILINEAR, () -> 16);
        policy.prepare(material);
        assertTrue(surface.getImage().isGeneratedMipmapsRequired());
        assertFalse(data.getImage().isGeneratedMipmapsRequired());
        var strip = texture("Textures/strip.png", 128, 32);
        material.setTexture("DiffuseMap", strip);
        material.setInt("NumberOfTiles", 4);
        var geometry = new Geometry("strip", new Quad(1, 1));
        geometry.setMaterial(material);
        policy.prepare(geometry);
        assertFalse(strip.getImage().isGeneratedMipmapsRequired());
        material.setInt("NumberOfTiles", 1);
        var generated = texture(null, 128, 128);
        material.setTexture("DiffuseMap", generated);
        policy.prepare(material);
        assertFalse(generated.getImage().isGeneratedMipmapsRequired());
    }

    @Test
    void safeGeneratedDerivativesInheritTheRoleButAtlasDerivativesDoNot() {
        var policy = new WorldTextureFiltering(TextureFiltering.ANISOTROPIC_8, () -> 16);
        var source = texture("Textures/body.png", 4, 4);
        policy.prepare(source);
        var derived = texture(null, 4, 4);
        policy.prepareDerived(derived, source);
        assertEquals(8, derived.getAnisotropicFilter());
        policy.refresh(TextureFiltering.BILINEAR);
        assertEquals(Texture.MinFilter.BilinearNearestMipMap, derived.getMinFilter());
        assertEquals(1, derived.getAnisotropicFilter());
        var atlas = texture(null, 128, 32);
        var atlasDerived = texture(null, 128, 32);
        policy.prepareDerived(atlasDerived, atlas);
        assertFalse(atlasDerived.getImage().isGeneratedMipmapsRequired());
        policy.close();
        policy.refresh(TextureFiltering.ANISOTROPIC_16);
        assertEquals(1, derived.getAnisotropicFilter());
    }

    @Test
    void lateAdoptionSharesOneFreshGpuImageAndLeavesTheUploadedUiImageAlone() {
        var policy = new WorldTextureFiltering(TextureFiltering.TRILINEAR, () -> 16);
        var ui = texture("Textures/shared.png", 128, 128);
        var original = ui.getImage();
        original.setId(123); // An already-uploaded base-only image; no GPU required here.
        var world = ui.clone();
        policy.prepare(world);
        assertNotSame(original, world.getImage());
        assertEquals(-1, world.getImage().getId());
        assertEquals(123, original.getId());
        assertFalse(original.isGeneratedMipmapsRequired());
        assertSame(original.getData(0), world.getImage().getData(0));
        var second = ui.clone();
        policy.prepare(second);
        assertSame(world.getImage(), second.getImage());
        world.getImage().setId(124);
        world.getImage().setMipmapsGenerated(true);
        world.getImage().setUpdateNeeded(); // A known world image may regenerate in place.
        var third = world.clone();
        policy.prepare(third);
        assertSame(world.getImage(), third.getImage());
    }

    @Test
    void replacedImagesAreAdoptedAgainWithoutRewritingUnchangedSamplers() {
        var policy = new WorldTextureFiltering(TextureFiltering.TRILINEAR, () -> 16);
        var world = texture("Textures/changing.png", 128, 128);
        policy.prepare(world);
        world.setAnisotropicFilter(
                7); // Preparation of the unchanged sampler is a no-write cache hit.
        policy.prepare(world);
        assertEquals(7, world.getAnisotropicFilter());

        var uploaded = texture("Textures/replacement.png", 128, 128);
        uploaded.getImage().setId(321);
        world.setImage(uploaded.getImage());
        policy.prepare(world);
        assertNotSame(uploaded.getImage(), world.getImage());
        assertSame(uploaded.getImage().getData(0), world.getImage().getData(0));
        assertEquals(-1, world.getImage().getId());
        assertEquals(321, uploaded.getImage().getId());
        assertTrue(world.getImage().isGeneratedMipmapsRequired());
        assertEquals(1, world.getAnisotropicFilter());
        var adopted = world.getImage();
        policy.prepare(world);
        assertSame(adopted, world.getImage());

        var derived = texture(null, 4, 4);
        policy.prepareDerived(derived, world);
        derived.setImage(uploaded.getImage());
        policy.prepareDerived(derived, world);
        assertSame(adopted, derived.getImage());
        policy.refresh(TextureFiltering.BILINEAR);
        assertEquals(Texture.MinFilter.BilinearNearestMipMap, world.getMinFilter());
        assertEquals(Texture.MinFilter.BilinearNearestMipMap, derived.getMinFilter());
    }

    @Test
    void suppliedMipChainsAndSerializedLateMipRequestsAreHandled() throws Exception {
        var policy = new WorldTextureFiltering(TextureFiltering.BILINEAR, () -> 16);
        var supplied = texture("Textures/supplied.dds", 4, 4);
        supplied.getImage().setData(BufferUtils.createByteBuffer(84));
        supplied.getImage().setMipMapSizes(new int[] {64, 16, 4});
        policy.prepare(supplied);
        assertTrue(supplied.getImage().hasMipmaps());
        assertFalse(supplied.getImage().isGeneratedMipmapsRequired());
        var serialized = texture("Textures/old-model.png", 4, 4);
        serialized.getImage().setId(5);
        serialized.setMinFilter(Texture.MinFilter.Trilinear);
        var original = serialized.getImage();
        policy.prepare(serialized);
        assertNotSame(original, serialized.getImage());
        assertEquals(Texture.MinFilter.BilinearNearestMipMap, serialized.getMinFilter());
        var matching = texture("Textures/matching-filter.png", 4, 4);
        // Simulate inconsistent external sampler/image state. Texture.read itself calls
        // setMinFilter(), so this is a defensive case rather than its serialization path.
        var minFilter = Texture.class.getDeclaredField("minificationFilter");
        minFilter.setAccessible(true);
        minFilter.set(matching, Texture.MinFilter.BilinearNearestMipMap);
        assertFalse(matching.getImage().isGeneratedMipmapsRequired());
        policy.prepare(matching);
        assertTrue(matching.getImage().isGeneratedMipmapsRequired());
        assertNull(policy.prepare((Texture) null));
    }
}
