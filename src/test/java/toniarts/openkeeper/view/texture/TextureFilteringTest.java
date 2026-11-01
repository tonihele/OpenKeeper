/* Copyright (C) 2026 OpenKeeper. Licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.texture;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import toniarts.openkeeper.game.data.TextureFiltering;
import static org.junit.jupiter.api.Assertions.*;

class TextureFilteringTest {
    @TempDir Path directory;

    @Test void preservesLegacyChoicesAndPrefersExplicitSettings() {
        assertEquals(TextureFiltering.TRILINEAR, TextureFiltering.resolve("", 0));
        assertEquals(TextureFiltering.TRILINEAR, TextureFiltering.resolve(null, 1));
        for (int n : new int[]{2,4,8,16}) {
            assertEquals(n, TextureFiltering.resolve("", n).getAnisotropy());
        }
        assertEquals(TextureFiltering.BILINEAR, TextureFiltering.resolve("BILINEAR", 16));
        assertEquals(TextureFiltering.ANISOTROPIC_4, TextureFiltering.resolve("invalid", 4));
        assertEquals(TextureFiltering.ANISOTROPIC_16, TextureFiltering.resolve("", 32));
    }

    @Test void filtersNewAndCachedTexturesAndClampsToTheDeviceLimit() throws Exception {
        var assets = assets();
        var mode = new AtomicReference<>(TextureFiltering.ANISOTROPIC_16);
        var policy = new TextureFilteringListener(assets, mode::get, 4);
        assets.addAssetEventListener(policy);
        writeImage("Textures/stone.png");
        var texture = assets.loadTexture("Textures/stone.png");
        assertEquals(Texture.MinFilter.Trilinear, texture.getMinFilter());
        assertEquals(Texture.MagFilter.Bilinear, texture.getMagFilter());
        assertEquals(4, texture.getAnisotropicFilter());
        assertTrue(texture.getImage().isGeneratedMipmapsRequired());
        mode.set(TextureFiltering.BILINEAR);
        policy.refresh();
        // jME returns a sampler clone on cache hits. Its cached original must also be updated.
        var cached = assets.loadTexture("Textures/stone.png");
        assertEquals(Texture.MinFilter.BilinearNearestMipMap, cached.getMinFilter());
        assertEquals(1, cached.getAnisotropicFilter());
        policy.apply(texture);
        assertEquals(Texture.MinFilter.BilinearNearestMipMap, texture.getMinFilter());
        mode.set(TextureFiltering.ANISOTROPIC_8);
        policy.refresh();
        assertEquals(Texture.MinFilter.Trilinear, texture.getMinFilter());
        assertEquals(4, texture.getAnisotropicFilter());
    }

    @Test void preservesUiFontsAtlasesAndDataSamplers() throws Exception {
        var assets = assets();
        var policy = new TextureFilteringListener(assets, () -> TextureFiltering.ANISOTROPIC_16, 16);
        for (String name : new String[]{"Textures/GUI/icon.png", "Interface/Fonts/font.png", "Textures\\GUI\\icon.png"}) {
            var texture = texture();texture.setKey(new TextureKey(name));
            texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
            texture.setMagFilter(Texture.MagFilter.Nearest);
            policy.apply(texture);
            assertEquals(Texture.MinFilter.NearestNoMipMaps, texture.getMinFilter());
            assertEquals(Texture.MagFilter.Nearest, texture.getMagFilter());
            assertEquals(0, texture.getAnisotropicFilter());
            assertFalse(texture.getImage().isGeneratedMipmapsRequired());
        }
        for (Texture.MinFilter filter : new Texture.MinFilter[]{Texture.MinFilter.NearestNoMipMaps, Texture.MinFilter.BilinearNoMipMaps}) {
            var atlasOrData = texture();atlasOrData.setMinFilter(filter);
            policy.apply(atlasOrData);
            assertEquals(filter, atlasOrData.getMinFilter());
            assertFalse(atlasOrData.getImage().isGeneratedMipmapsRequired());
        }
    }

    @Test void updatesTexturesEmbeddedInConvertedModelsAndTheirVisibleClones() throws Exception {
        var assets = assets();
        var mode = new AtomicReference<>(TextureFiltering.ANISOTROPIC_8);
        var policy = new TextureFilteringListener(assets, mode::get, 16);
        assets.addAssetEventListener(policy);
        var embedded = texture();embedded.setKey(new TextureKey("Textures/embedded.png"));
        var material = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap", embedded);
        var geometry = new Geometry("Converted mesh", new Quad(1,1));geometry.setMaterial(material);
        var model = new Node();model.attachChild(geometry);
        writeImage("Textures/embedded.png");
        BinaryExporter.getInstance().save(model, directory.resolve("model.j3o").toFile());
        var loaded = (Node) assets.loadModel("model.j3o");
        var loadedTexture = ((Geometry) loaded.getChild(0)).getMaterial().getTextureParam("ColorMap").getTextureValue();
        assertEquals(8, loadedTexture.getAnisotropicFilter());
        assertTrue(loadedTexture.getImage().isGeneratedMipmapsRequired());
        var visible = loaded.clone(true);
        var visibleTexture = ((Geometry) visible.getChild(0)).getMaterial().getTextureParam("ColorMap").getTextureValue();
        mode.set(TextureFiltering.BILINEAR);
        policy.refresh();policy.apply(visible);
        assertEquals(Texture.MinFilter.BilinearNearestMipMap, visibleTexture.getMinFilter());
        assertEquals(1, visibleTexture.getAnisotropicFilter());
        var cachedModel = (Node) assets.loadModel("model.j3o");
        assertEquals(Texture.MinFilter.BilinearNearestMipMap,
                ((Geometry) cachedModel.getChild(0)).getMaterial().getTextureParam("ColorMap").getTextureValue().getMinFilter());
    }

    private DesktopAssetManager assets() {
        var manager = new DesktopAssetManager(true);
        manager.registerLocator(directory.toString(), FileLocator.class);
        return manager;
    }

    private void writeImage(String name) throws Exception {
        Path file = directory.resolve(name);Files.createDirectories(file.getParent());
        ImageIO.write(new BufferedImage(4,4,BufferedImage.TYPE_INT_ARGB), "png", file.toFile());
    }

    private static Texture2D texture() {
        return new Texture2D(new Image(Image.Format.RGBA8, 4, 4,
                BufferUtils.createByteBuffer(4*4*4), ColorSpace.sRGB));
    }
}
