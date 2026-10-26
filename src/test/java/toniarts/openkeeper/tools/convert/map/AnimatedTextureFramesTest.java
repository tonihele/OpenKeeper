/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.tools.convert.map;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.texture.Texture;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.tools.modelviewer.UniversalArtResourceLoader;
import toniarts.openkeeper.view.effect.EffectGeometry;
import static org.junit.jupiter.api.Assertions.*;

class AnimatedTextureFramesTest {
    @TempDir Path directory;

    @Test
    void mistUsesEightAvailableFramesInsteadOfSixteenDeclaredFrames() throws Exception {
        var assets = assets();
        var resource = resource(16);
        writeFrames(resource, 8);
        assertEquals(8, AssetUtils.getAnimationFrameCount(resource, assets));
        Texture atlas = (Texture) AssetUtils.createParticleMaterial(resource, assets)
                .getParam("Texture").getValue();
        assertEquals(16, atlas.getImage().getWidth()); // Eight 2x3 frames, no duplicated tail.
        assertEquals(3, atlas.getImage().getHeight());
        var material = AssetUtils.createLightningSpriteMaterial(resource, assets);
        assertEquals(8, material.getParam("NumberOfTiles").getValue());
        var preview = (EffectGeometry) UniversalArtResourceLoader.load(assets, resource);
        assertEquals(8, preview.getFrames());
    }

    @Test
    void discoveryIgnoresAnUnderreportedFrameCountAndStopsAtFirstGap() throws Exception {
        var assets = assets();
        var resource = resource(1);
        writeFrames(resource, 3);
        writeFrame(resource, 4); // Frame 3 is missing: do not include frame 4.
        assertEquals(3, AssetUtils.getAnimationFrameCount(resource, assets));
        Texture atlas = (Texture) AssetUtils.createParticleMaterial(resource, assets)
                .getParam("Texture").getValue();
        assertEquals(6, atlas.getImage().getWidth());
    }

    @Test
    void discoveryHasTheNativeThirtyTwoFrameLimit() throws Exception {
        var assets = assets();
        var resource = resource(100);
        writeFrames(resource, 33);
        assertEquals(32, AssetUtils.getAnimationFrameCount(resource, assets));
        Texture atlas = (Texture) AssetUtils.createParticleMaterial(resource, assets)
                .getParam("Texture").getValue();
        assertEquals(64, atlas.getImage().getWidth());
    }

    @Test
    void nonAnimatedResourcesUseOneImageRegardlessOfFrameMetadata() throws Exception {
        var resource = resource(16);
        resource.setFlags(EnumSet.noneOf(ArtResource.ArtResourceFlag.class));
        assertEquals(1, AssetUtils.getAnimationFrameCount(resource, assets()));
    }

    private DesktopAssetManager assets() throws Exception {
        Files.createDirectories(directory.resolve("Textures"));
        var assets = new DesktopAssetManager(true);
        assets.registerLocator(directory.toString(), FileLocator.class);
        return assets;
    }

    private static ArtResource resource(int declaredFrames) {
        var resource = new ArtResource();
        resource.setName("animation-" + UUID.randomUUID());
        resource.setType(ArtResource.ArtResourceType.ALPHA);
        resource.setFlags(EnumSet.of(ArtResource.ArtResourceFlag.ANIMATING_TEXTURE));
        resource.setData(ArtResource.KEY_FRAMES, declaredFrames);
        return resource;
    }

    private void writeFrames(ArtResource resource, int count) throws Exception {
        for (int frame = 0; frame < count; frame++) {
            writeFrame(resource, frame);
        }
    }

    private void writeFrame(ArtResource resource, int frame) throws Exception {
        ImageIO.write(new BufferedImage(2, 3, BufferedImage.TYPE_INT_ARGB), "png",
                directory.resolve("Textures/" + resource.getName() + frame + ".png").toFile());
    }
}
