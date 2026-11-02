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
    void convertedMaterialDepthStateSurvivesSerialization() throws Exception {
        var assets = new DesktopAssetManager(true);
        var flagsMethod = toniarts.openkeeper.tools.convert.KmfModelLoader.class.getDeclaredMethod(
                "setMaterialFlags", Material.class, toniarts.openkeeper.tools.convert.kmf.Material.class);
        flagsMethod.setAccessible(true);
        var flagField = toniarts.openkeeper.tools.convert.kmf.Material.class.getDeclaredField("flag");
        flagField.setAccessible(true);
        var alpha = toniarts.openkeeper.tools.convert.kmf.Material.MaterialFlag.HAS_ALPHA;
        var additive = toniarts.openkeeper.tools.convert.kmf.Material.MaterialFlag.ALPHA_ADDITIVE;
        for (var flags : java.util.List.of(EnumSet.noneOf(alpha.getDeclaringClass()),
                EnumSet.of(alpha), EnumSet.of(additive), EnumSet.of(alpha, additive))) {
            var source = new toniarts.openkeeper.tools.convert.kmf.Material();
            flagField.set(source, flags);
            var material = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
            flagsMethod.invoke(new toniarts.openkeeper.tools.convert.KmfModelLoader(), material, source);
            var bytes = new java.io.ByteArrayOutputStream();
            com.jme3.export.binary.BinaryExporter.getInstance().save(material, bytes);
            var importer = new com.jme3.export.binary.BinaryImporter();
            importer.setAssetManager(assets);
            var loaded = (Material) importer.load(new java.io.ByteArrayInputStream(bytes.toByteArray()));
            assertEquals(flags.isEmpty(), loaded.getAdditionalRenderState().isDepthWrite());
            assertTrue(loaded.getAdditionalRenderState().isDepthTest());
            assertEquals(!flags.isEmpty(), loaded.isTransparent());
        }
    }

    private static ArtResource resource(String name, ArtResource.ArtResourceType type) {
        var resource = new ArtResource();
        resource.setName(name);
        resource.setType(type);
        resource.setFlags(EnumSet.noneOf(ArtResource.ArtResourceFlag.class));
        return resource;
    }
}
