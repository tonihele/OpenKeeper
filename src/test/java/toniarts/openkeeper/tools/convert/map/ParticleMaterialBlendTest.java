/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.tools.convert.map;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.Material;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import toniarts.openkeeper.utils.AssetUtils;
import static org.junit.jupiter.api.Assertions.*;

class ParticleMaterialBlendTest {
    @TempDir Path directory;

    @Test
    void sameTextureCanUseNativeAdditiveAndStandardAlphaInEitherOrder() throws Exception {
        var assets = new DesktopAssetManager(true);
        assets.registerLocator(directory.toString(), FileLocator.class);
        Files.createDirectories(directory.resolve("Textures"));
        for (boolean additiveFirst : new boolean[]{true, false}) {
            String name = "particle-blend-" + UUID.randomUUID();
            ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png",
                    directory.resolve("Textures/" + name + ".png").toFile());
            ArtResource additive = resource(name, ArtResource.ArtResourceType.ADDITIVE_ALPHA);
            ArtResource alpha = resource(name, ArtResource.ArtResourceType.ALPHA);
            // Exercise the production cache with the same filename and different art types.
            AssetUtils.createParticleMaterial(additiveFirst ? additive : alpha, assets);
            var additiveMaterial = AssetUtils.createParticleMaterial(additive, assets);
            var alphaMaterial = AssetUtils.createParticleMaterial(alpha, assets);
            assertEquals(BlendMode.Additive, additiveMaterial.getAdditionalRenderState().getBlendMode());
            assertEquals(BlendMode.Alpha, alphaMaterial.getAdditionalRenderState().getBlendMode());
            assertTrue(additiveMaterial.getAdditionalRenderState().isDepthTest());
            assertFalse(additiveMaterial.getAdditionalRenderState().isDepthWrite());
            assertFalse(alphaMaterial.getAdditionalRenderState().isDepthWrite());
        }
    }

    @Test
    void convertedTransparentModelsDoNotDepthMaskAdditiveParticles() throws Exception {
        var assets = new DesktopAssetManager(true);
        assets.registerLocator(directory.toString(), FileLocator.class);
        Files.createDirectories(directory.resolve("Textures"));
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png",
                directory.resolve("Textures/chasm.png").toFile());
        var material = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        material.setTexture("DiffuseMap", assets.loadTexture("Textures/chasm.png"));
        material.setTransparent(true);
        material.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        material.getAdditionalRenderState().setDepthWrite(true); // Old converted .j3m state.
        AssetUtils.assignMapsToMaterial(assets, material);
        assertFalse(material.getAdditionalRenderState().isDepthWrite());
        assertTrue(material.getAdditionalRenderState().isDepthTest());
        assertEquals(BlendMode.Alpha, material.getAdditionalRenderState().getBlendMode());

        var opaque = material.clone();
        opaque.setTransparent(false);
        opaque.getAdditionalRenderState().setDepthWrite(true);
        AssetUtils.assignMapsToMaterial(assets, opaque);
        assertTrue(opaque.getAdditionalRenderState().isDepthWrite());
    }

    private static ArtResource resource(String name, ArtResource.ArtResourceType type) {
        var resource = new ArtResource();
        resource.setName(name);
        resource.setType(type);
        resource.setFlags(EnumSet.noneOf(ArtResource.ArtResourceFlag.class));
        return resource;
    }
}
