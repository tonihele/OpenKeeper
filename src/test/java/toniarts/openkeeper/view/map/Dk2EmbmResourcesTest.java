package toniarts.openkeeper.view.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.texture.Image;
import com.jme3.texture.image.ColorSpace;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.DesktopAssetManager;
import java.util.List;
import org.junit.jupiter.api.Test;

class Dk2EmbmResourcesTest {

    @Test
    void waterUsesAll32OriginalBumpFramesInNumericOrder() {
        List<String> frameNames = Dk2EmbmResources.waterBumpFrameNames();

        assertEquals(32, frameNames.size());
        assertEquals("Textures/BumpWaterBumpmap0.png", frameNames.getFirst());
        assertEquals("Textures/BumpWaterBumpmap31.png", frameNames.getLast());
        assertTrue(frameNames.get(2).endsWith("BumpWaterBumpmap2.png"));
    }

    @Test
    void missingOriginalSetFallsBackWithoutPartialResource() {
        DesktopAssetManager assets = new DesktopAssetManager(false) {
            @Override
            public AssetInfo locateAsset(AssetKey key) {
                return null;
            }
        };

        assertTrue(Dk2EmbmResources.load(assets, Dk2EmbmResources.Liquid.WATER).isEmpty());
        assertTrue(Dk2EmbmResources.load(assets, Dk2EmbmResources.Liquid.LAVA).isEmpty());
    }

    @Test
    void convertsFlatHeightFieldsToNeutralLinearBumpDataAndKeepsAlpha() {
        BufferedImage source = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                source.setRGB(x, y, (64 << 24) | (128 << 16) | (128 << 8) | 128);
            }
        }

        Image bump = Dk2EmbmResources.bumpDeltaImage(source);
        ByteBuffer pixels = bump.getData(0);

        assertEquals(ColorSpace.Linear, bump.getColorSpace());
        assertEquals(Image.Format.RGBA8, bump.getFormat());
        for (int offset = 0; offset < pixels.limit(); offset += 4) {
            assertEquals(128, Byte.toUnsignedInt(pixels.get(offset)));
            assertEquals(128, Byte.toUnsignedInt(pixels.get(offset + 1)));
            assertEquals(64, Byte.toUnsignedInt(pixels.get(offset + 3)));
        }
    }

    @Test
    void convertsHeightSlopesToSignedOffsetChannelsWithWrappedBorders() {
        BufferedImage source = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int value = x * 64;
                source.setRGB(x, y, (value << 16) | (value << 8) | value);
            }
        }

        ByteBuffer pixels = Dk2EmbmResources.bumpDeltaImage(source).getData(0);
        int middle = (1 * source.getWidth() + 1) * 4;
        int wrapped = (1 * source.getWidth()) * 4;

        assertTrue(Byte.toUnsignedInt(pixels.get(middle)) > 128);
        assertEquals(128, Byte.toUnsignedInt(pixels.get(middle + 1)));
        assertTrue(Byte.toUnsignedInt(pixels.get(wrapped)) < 128);
    }
}
