/* Copyright (C) 2026 OpenKeeper. Licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.material.Material;
import com.jme3.renderer.Caps;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Image;
import com.jme3.util.BufferUtils;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import toniarts.openkeeper.game.data.Settings;

class LiquidSurfaceControlTest {

    @Test
    @SuppressWarnings("deprecation") // jME exposes update() as deprecated test support.
    void switchesWaterMaterialBetweenClassicAndEnhancedWithoutRebuilding(@TempDir Path directory) throws Exception {
        Path textures = Files.createDirectories(directory.resolve("Textures"));
        writeTexture(textures.resolve("BumpWater0.png"));
        writeTexture(textures.resolve("BumpWaterEnvMap0.png"));
        for (int frame = 0; frame < 32; frame++) {
            writeTexture(textures.resolve("BumpWaterBumpmap" + frame + ".png"));
        }

        boolean previous = Settings.getInstance().getBoolean(Settings.Setting.ENHANCED_LIQUIDS);
        DesktopAssetManager assets = new DesktopAssetManager(true);
        try {
            assets.registerLocator(directory.toString(), com.jme3.asset.plugins.FileLocator.class);
            Material material = new Material(assets, "MatDefs/DkLighting/Vertex.j3md");
            ByteBuffer pixel = BufferUtils.createByteBuffer(4);
            pixel.put(new byte[]{(byte) 255, (byte) 255, (byte) 255, (byte) 255}).flip();
            Texture2D classic = new Texture2D(new Image(Image.Format.RGBA8, 1, 1, pixel));
            material.setTexture("DiffuseMap", classic);
            Mesh classicMesh = new Mesh();
            Mesh enhancedMesh = new Mesh();
            Geometry geometry = new Geometry("water");
            geometry.setMesh(classicMesh);
            geometry.setMaterial(material);
            LiquidSurfaceControl control = new LiquidSurfaceControl(true, true, assets, enhancedMesh);
            geometry.addControl(control);

            Settings.getInstance().setSetting(Settings.Setting.ENHANCED_LIQUIDS, false);
            control.update(1f / 60f);
            assertFalse(control.isEnhancedActive());
            assertNull(material.getParam("EmbmEnabled"));

            Settings.getInstance().setSetting(Settings.Setting.ENHANCED_LIQUIDS, true);
            control.update(1f / 60f);
            assertFalse(control.isEnhancedActive()); // Wait for renderer capability discovery.
            control.captureRendererCapabilities(EnumSet.of(Caps.TextureArray));
            control.update(1f / 60f);
            assertTrue(control.isEnhancedActive());
            assertNotNull(material.getParam("EmbmEnabled"));
            assertNotNull(material.getTextureParam("EmbmBumpFrames"));
            assertEquals(32, material.getParam("EmbmBumpFrameCount").getValue());
            assertEquals(Boolean.TRUE, material.getParam("LiquidWavesEnabled").getValue());
            assertSame(enhancedMesh, geometry.getMesh());

            Settings.getInstance().setSetting(Settings.Setting.ENHANCED_LIQUIDS, false);
            control.update(1f / 60f);
            assertFalse(control.isEnhancedActive());
            assertNull(material.getParam("EmbmEnabled"));
            assertNull(material.getTextureParam("EmbmBumpFrames"));
            assertNull(material.getParam("LiquidWavesEnabled"));
            assertSame(classicMesh, geometry.getMesh());
            assertSame(classic, material.getTextureParam("DiffuseMap").getTextureValue());
        } finally {
            Settings.getInstance().setSetting(Settings.Setting.ENHANCED_LIQUIDS, previous);
        }
    }

    @Test
    @SuppressWarnings("deprecation") // jME exposes update() as deprecated test support.
    void keepsClassicRenderingWhenTextureArraysAreUnsupported() {
        boolean previous = Settings.getInstance().getBoolean(Settings.Setting.ENHANCED_LIQUIDS);
        try {
            Settings.getInstance().setSetting(Settings.Setting.ENHANCED_LIQUIDS, true);
            DesktopAssetManager assets = new DesktopAssetManager(true);
            Material material = new Material(assets, "MatDefs/DkLighting/Vertex.j3md");
            Mesh classicMesh = new Mesh();
            Mesh enhancedMesh = new Mesh();
            Geometry geometry = new Geometry("water");
            geometry.setMesh(classicMesh);
            geometry.setMaterial(material);
            LiquidSurfaceControl control = new LiquidSurfaceControl(true, true, assets, enhancedMesh);
            geometry.addControl(control);

            control.captureRendererCapabilities(EnumSet.noneOf(Caps.class));
            control.update(1f / 60f);

            assertFalse(control.isEnhancedActive());
            assertNull(material.getParam("EmbmEnabled"));
            assertSame(classicMesh, geometry.getMesh());
        } finally {
            Settings.getInstance().setSetting(Settings.Setting.ENHANCED_LIQUIDS, previous);
        }
    }

    private static void writeTexture(Path path) throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                image.setRGB(x, y, 0xff808080);
            }
        }
        ImageIO.write(image, "png", path.toFile());
    }
}
