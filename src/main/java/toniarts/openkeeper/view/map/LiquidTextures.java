/* Copyright (C) 2026 OpenKeeper. Licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.map;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import javax.imageio.ImageIO;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.utils.PathUtils;

/** Original liquid maps. Bump heights are data, never sRGB colours or opacity. */
public final class LiquidTextures {
    public record Maps(Texture base, Texture bump, Texture environment, int frames) { }
    private static final Map<AssetManager, Map<Boolean, Optional<Maps>>> CACHE = new WeakHashMap<>();
    private static final System.Logger LOG = System.getLogger(LiquidTextures.class.getName());

    private LiquidTextures() { }

    public static synchronized Optional<Maps> load(AssetManager assets, boolean water) {
        return CACHE.computeIfAbsent(assets, key -> new HashMap<>()).computeIfAbsent(water, key -> {
            String prefix = water ? "BumpWater" : "BumpLava";
            try {
                int count = water ? 32 : 1;
                BufferedImage[] frames = new BufferedImage[count];
                for (int i = 0; i < count; i++) frames[i] = read(assets, prefix + "Bumpmap" + i + ".png");
                return Optional.of(new Maps(colour(read(assets, prefix + "0.png")), bumpAtlas(frames),
                        colour(read(assets, prefix + "EnvMap0.png")), count));
            } catch (IOException | RuntimeException ex) {
                LOG.log(System.Logger.Level.WARNING,
                        "Enhanced {0} textures unavailable; using Classic liquids: {1}", water ? "water" : "lava", ex.getMessage());
                return Optional.empty();
            }
        });
    }

    private static BufferedImage read(AssetManager assets, String name) throws IOException {
        String key = null;
        String converted = AssetsConverter.getAssetsFolder();
        try {
            String file = PathUtils.getRealFileName(converted, "Textures/" + name);
            key = Path.of(converted).relativize(Path.of(file)).toString().replace('\\', '/');
        } catch (IOException missingConvertedTexture) {
            // Expected for installations whose enhanced maps are loose original PNGs.
        }
        var asset = key == null ? null : assets.locateAsset(new AssetKey<>(key));
        BufferedImage image;
        if (asset != null) {
            try (InputStream input = asset.openStream()) { image = ImageIO.read(input); }
        } else {
            String root = PathUtils.getDKIIFolder();
            if (root == null) throw new IOException("Original Dungeon Keeper II folder is unset");
            // Some releases store the enhanced PNGs loose, outside the texture cache/WAD.
            Path file = Path.of(PathUtils.getRealFileName(PathUtils.fixFilePath(root), "Data/EngineTextures/" + name));
            try (InputStream input = Files.newInputStream(file)) { image = ImageIO.read(input); }
        }
        if (image == null) throw new IOException("Unsupported liquid image: " + name);
        return image;
    }

    static Texture colour(BufferedImage source) {
        ByteBuffer data = BufferUtils.createByteBuffer(source.getWidth() * source.getHeight() * 4);
        for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++) {
            int pixel = source.getRGB(x, y);
            data.put((byte) (pixel >> 16)).put((byte) (pixel >> 8)).put((byte) pixel).put((byte) (pixel >> 24));
        }
        data.flip();
        return texture(data, source.getWidth(), source.getHeight(), ColorSpace.sRGB);
    }

    /** Build a horizontal displacement atlas from wrapped central height differences. */
    static Texture bumpAtlas(BufferedImage[] frames) {
        int width = frames[0].getWidth(), height = frames[0].getHeight();
        ByteBuffer data = BufferUtils.createByteBuffer(width * height * frames.length * 4);
        for (int y = 0; y < height; y++) for (BufferedImage frame : frames) {
            if (frame.getWidth() != width || frame.getHeight() != height) throw new IllegalArgumentException("Liquid frame dimensions differ");
            for (int x = 0; x < width; x++) {
                // Raster samples avoid Java's grayscale-to-sRGB colour conversion.
                int du = height(frame, (x - 1 + width) % width, y) - height(frame, (x + 1) % width, y);
                int dv = height(frame, x, (y - 1 + height) % height) - height(frame, x, (y + 1) % height);
                data.put(encode(du)).put(encode(dv)).put((byte) 0).put((byte) 255);
            }
        }
        data.flip();
        return texture(data, width * frames.length, height, ColorSpace.Linear);
    }

    private static int height(BufferedImage image, int x, int y) { return image.getRaster().getSample(x, y, 0); }
    private static byte encode(int difference) { return (byte) Math.max(0, Math.min(255, difference + 128)); }
    private static Texture texture(ByteBuffer data, int width, int height, ColorSpace space) {
        Texture texture = new Texture2D(new Image(Image.Format.RGBA8, width, height, data, space));
        texture.setWrap(Texture.WrapMode.Repeat);
        texture.setMagFilter(Texture.MagFilter.Bilinear);
        // No mipmaps: a mip chain must not blend neighbouring animation frames.
        texture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        return texture;
    }
}
