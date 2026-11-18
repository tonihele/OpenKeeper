/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.view.texture;

import static org.junit.jupiter.api.Assertions.*;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import toniarts.openkeeper.game.data.TextureFiltering;
import toniarts.openkeeper.utils.AssetUtils;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import javax.imageio.ImageIO;

class WorldModelCacheTest {
    @TempDir Path directory;

    @Test
    void applicationStrongAndWeakTemplateCachesRemainCurrentWithoutCopyingImages()
            throws Exception {
        Files.createDirectories(directory.resolve("Textures"));
        Files.createDirectories(directory.resolve("Models"));
        ImageIO.write(
                new BufferedImage(16, 32, BufferedImage.TYPE_INT_ARGB),
                "png",
                directory.resolve("Textures/body.png").toFile());
        var raw = new DesktopAssetManager(true);
        raw.registerLocator(directory.toString(), FileLocator.class);
        var material = new Material(raw, "MatDefs/DkLighting/Vertex.j3md");
        material.setTexture(
                "DiffuseMap", raw.loadTexture(new TextureKey("Textures/body.png", false)));
        var geometry = new Geometry("body", new Quad(1, 1));
        geometry.setMaterial(material);
        var template = new Node();
        template.attachChild(geometry);
        var policy = new WorldTextureFiltering(TextureFiltering.TRILINEAR, () -> 16);
        var world = new WorldAssetManager(raw, policy);
        for (boolean weak : new boolean[] {false, true}) {
            String name = "o3-cache-" + UUID.randomUUID();
            BinaryExporter.getInstance()
                    .save(template, directory.resolve("Models/" + name + ".j3o").toFile());
            var first = AssetUtils.loadModel(world, name, null, true, weak);
            var source = texture(first);
            assertEquals(Texture.MinFilter.Trilinear, source.getMinFilter());
            policy.refresh(TextureFiltering.BILINEAR);
            var second = AssetUtils.loadModel(world, name, null, true, weak);
            assertSame(source, texture(second));
            assertEquals(Texture.MinFilter.BilinearNearestMipMap, texture(second).getMinFilter());
            policy.refresh(TextureFiltering.TRILINEAR);
        }
    }

    private static Texture texture(Spatial model) {
        return ((Geometry) ((Node) model).getChild(0))
                .getMaterial()
                .getTextureParam("DiffuseMap")
                .getTextureValue();
    }
}
