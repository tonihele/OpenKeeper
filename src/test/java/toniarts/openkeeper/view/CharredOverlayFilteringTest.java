/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.view;

import static org.junit.jupiter.api.Assertions.*;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import toniarts.openkeeper.game.data.TextureFiltering;
import toniarts.openkeeper.view.texture.WorldAssetManager;
import toniarts.openkeeper.view.texture.WorldTextureFiltering;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

class CharredOverlayFilteringTest {
    @TempDir Path directory;

    @Test
    void bakedTextureTracksLiveFilteringAndIsReusedAfterTheSourceSamplerChanges() throws Exception {
        Files.createDirectories(directory.resolve("Textures"));
        for (String name : new String[] {"body", "CharredTexture"}) {
            ImageIO.write(
                    new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB),
                    "png",
                    directory.resolve("Textures/" + name + ".png").toFile());
        }
        var raw = new DesktopAssetManager(true);
        raw.registerLocator(directory.toString(), FileLocator.class);
        var policy = new WorldTextureFiltering(TextureFiltering.ANISOTROPIC_8, () -> 16);
        var world = new WorldAssetManager(raw, policy);
        var source = world.loadTexture(new TextureKey("Textures/body.png", false));
        var first = model(world, source);
        CharredOverlay.apply(first, world);
        var baked = texture(first);
        assertNotSame(source, baked);
        assertNull(baked.getKey());
        assertEquals(8, baked.getAnisotropicFilter());
        assertTrue(baked.getImage().isGeneratedMipmapsRequired());
        policy.refresh(TextureFiltering.BILINEAR);
        assertEquals(Texture.MinFilter.BilinearNearestMipMap, baked.getMinFilter());
        assertEquals(1, baked.getAnisotropicFilter());
        var second = model(world, source);
        CharredOverlay.apply(second, world);
        assertSame(baked, texture(second));
    }

    private static Node model(WorldAssetManager assets, Texture source) {
        var material = new Material(assets, "MatDefs/DkLighting/Vertex.j3md");
        material.setTexture("DiffuseMap", source);
        var geometry = new Geometry("body", new Quad(1, 1));
        geometry.setMaterial(material);
        var node = new Node();
        node.attachChild(geometry);
        return node;
    }

    private static Texture texture(Node node) {
        return ((Geometry) node.getChild(0))
                .getMaterial()
                .getTextureParam("DiffuseMap")
                .getTextureValue();
    }
}
