/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.map;

import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.math.Vector4f;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureArray;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import toniarts.openkeeper.tools.convert.AssetsConverter;

/**
 * Loads and shares the original DKII water/lava EMBM textures for one
 * AssetManager. DKII's shipped bump PNGs are grayscale height fields, so they
 * are converted once to signed dU/dV data before upload. Color maps use jME's
 * normal image color-space handling.
 */
public final class Dk2EmbmResources {

    private static final System.Logger LOG = System.getLogger(Dk2EmbmResources.class.getName());
    private static final int WATER_BUMP_FRAME_COUNT = 32;

    public enum Liquid {
        WATER("BumpWater0.png", "BumpWaterEnvMap0.png", null,
                0.035f, 0.85f, 8.0f, false, 1.0f, 0.0f, 0.0f,
                1.0f, true),
        LAVA("BumpLava0.png", "BumpLavaEnvMap0.png", "BumpLavaBumpmap0.png",
                0.12f, 1.25f, 0.0f, true, 1.0f, 0.0f, 1.0f,
                1.0f, true);

        private final String baseMap;
        private final String environmentMap;
        private final String bumpMap;
        private final float bumpScale;
        private final float reflectionIntensity;
        private final float bumpAnimationSpeed;
        private final boolean useLuminance;
        private final float luminanceScale;
        private final float luminanceOffset;
        private final float baseAlpha;
        private final float baseColorMix;
        private final boolean sphereEnvironmentUv;

        Liquid(String baseMap, String environmentMap, String bumpMap,
                float bumpScale, float reflectionIntensity, float bumpAnimationSpeed,
                boolean useLuminance, float luminanceScale, float luminanceOffset, float baseAlpha,
                float baseColorMix, boolean sphereEnvironmentUv) {
            this.baseMap = baseMap;
            this.environmentMap = environmentMap;
            this.bumpMap = bumpMap;
            this.bumpScale = bumpScale;
            this.reflectionIntensity = reflectionIntensity;
            this.bumpAnimationSpeed = bumpAnimationSpeed;
            this.useLuminance = useLuminance;
            this.luminanceScale = luminanceScale;
            this.luminanceOffset = luminanceOffset;
            this.baseAlpha = baseAlpha;
            this.baseColorMix = baseColorMix;
            this.sphereEnvironmentUv = sphereEnvironmentUv;
        }
    }

    private record Textures(Texture2D baseMap, Texture2D bumpMap, TextureArray bumpFrames,
            int bumpFrameCount, Texture2D environmentMap) {

        void apply(Material material, Liquid liquid) {
            material.setTexture("EmbmBaseMap", baseMap);
            material.setTexture("EmbmEnvironmentMap", environmentMap);
            if (bumpFrames != null) {
                material.setTexture("EmbmBumpFrames", bumpFrames);
                material.setInt("EmbmBumpFrameCount", bumpFrameCount);
            } else {
                material.setTexture("EmbmBumpMap", bumpMap);
            }
            // The precomputed data stores the [-2, 2] finite difference in
            // [-1, 1], so double the matrix scale to retain the prototype's
            // displacement tuning.
            material.setVector4("EmbmBumpMatrix", new Vector4f(liquid.bumpScale * 2f, 0f, 0f,
                    liquid.bumpScale * 2f));
            material.setFloat("EmbmReflectionIntensity", liquid.reflectionIntensity);
            material.setFloat("EmbmBumpAnimationSpeed", liquid.bumpAnimationSpeed);
            material.setFloat("EmbmLuminanceScale", liquid.luminanceScale);
            material.setFloat("EmbmLuminanceOffset", liquid.luminanceOffset);
            material.setFloat("EmbmBaseAlpha", liquid.baseAlpha);
            material.setFloat("EmbmBaseColorMix", liquid.baseColorMix);
            material.setBoolean("EmbmUseLuminance", liquid.useLuminance);
            material.setBoolean("EmbmUseSphereEnvironmentUv", liquid.sphereEnvironmentUv);
            material.setBoolean("EmbmEnabled", true);
        }
    }

    private static final Map<AssetManager, Map<Liquid, Optional<Textures>>> CACHE = new WeakHashMap<>();

    public static Optional<Dk2EmbmResources> load(AssetManager assetManager, Liquid liquid) {
        synchronized (CACHE) {
            Map<Liquid, Optional<Textures>> byLiquid = CACHE.computeIfAbsent(assetManager, ignored -> new HashMap<>());
            Optional<Textures> cached = byLiquid.computeIfAbsent(liquid, ignored -> loadTextures(assetManager, liquid));
            return cached.map(textures -> new Dk2EmbmResources(textures, liquid));
        }
    }

    private final Textures textures;
    private final Liquid liquid;

    private Dk2EmbmResources(Textures textures, Liquid liquid) {
        this.textures = textures;
        this.liquid = liquid;
    }

    public void apply(Material material) {
        textures.apply(material, liquid);
    }

    public static List<String> waterBumpFrameNames() {
        List<String> names = new ArrayList<>(WATER_BUMP_FRAME_COUNT);
        for (int frame = 0; frame < WATER_BUMP_FRAME_COUNT; frame++) {
            names.add(texturePath("BumpWaterBumpmap" + frame + ".png"));
        }
        return List.copyOf(names);
    }

    private static Optional<Textures> loadTextures(AssetManager assetManager, Liquid liquid) {
        List<String> required = new ArrayList<>(List.of(texturePath(liquid.baseMap), texturePath(liquid.environmentMap)));
        if (liquid == Liquid.WATER) {
            required.addAll(waterBumpFrameNames());
        } else {
            required.add(texturePath(liquid.bumpMap));
        }
        for (String name : required) {
            if (assetManager.locateAsset(new TextureKey(name, false)) == null) {
                System.getLogger(Dk2EmbmResources.class.getName()).log(System.Logger.Level.WARNING,
                        "Enhanced DKII " + liquid.name().toLowerCase(Locale.ROOT)
                        + " rendering is unavailable; missing " + name + ". Using classic liquid rendering.");
                return Optional.empty();
            }
        }

        try {
            Texture2D base = loadTexture(assetManager, texturePath(liquid.baseMap));
            Texture2D environment = loadTexture(assetManager, texturePath(liquid.environmentMap));
            Texture2D bump = null;
            TextureArray bumpFrames = null;
            int bumpFrameCount = 0;
            if (liquid == Liquid.WATER) {
                List<Image> frames = new ArrayList<>(WATER_BUMP_FRAME_COUNT);
                for (String frameName : waterBumpFrameNames()) {
                    frames.add(loadBumpDeltaImage(assetManager, frameName));
                }
                validateFrames(frames);
                bumpFrames = new TextureArray(frames);
                bumpFrameCount = frames.size();
                bumpFrames.setWrap(Texture.WrapMode.Repeat);
            } else {
                bump = new Texture2D(loadBumpDeltaImage(assetManager, texturePath(liquid.bumpMap)));
                bump.setWrap(Texture.WrapMode.Repeat);
            }
            environment.setWrap(Texture.WrapMode.EdgeClamp);
            LOG.log(System.Logger.Level.INFO,
                    "Loaded DKII enhanced " + liquid.name().toLowerCase(Locale.ROOT)
                    + " textures" + (bumpFrames != null ? " with " + bumpFrameCount
                    + " animated bump frames." : "."));
            return Optional.of(new Textures(base, bump, bumpFrames, bumpFrameCount, environment));
        } catch (RuntimeException ex) {
            System.getLogger(Dk2EmbmResources.class.getName()).log(System.Logger.Level.WARNING,
                    "Failed to load enhanced DKII " + liquid.name().toLowerCase(Locale.ROOT)
                    + " textures. Using classic liquid rendering.", ex);
            return Optional.empty();
        }
    }

    private static Texture2D loadTexture(AssetManager assetManager, String name) {
        return (Texture2D) assetManager.loadTexture(new TextureKey(name, false));
    }

    private static Image loadBumpDeltaImage(AssetManager assetManager, String name) {
        AssetInfo asset = assetManager.locateAsset(new TextureKey(name, false));
        if (asset == null) {
            throw new IllegalArgumentException("Missing DKII bump image " + name);
        }
        try (InputStream stream = asset.openStream()) {
            BufferedImage source = javax.imageio.ImageIO.read(stream);
            if (source == null) {
                throw new IllegalArgumentException("Unsupported DKII bump image " + name);
            }
            return bumpDeltaImage(source);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not read DKII bump image " + name, ex);
        }
    }

    /**
     * Convert a tiling grayscale height field into signed dU/dV channels.
     * The original lava alpha channel is retained for EMBM luminance.
     */
    static Image bumpDeltaImage(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        if (width < 2 || height < 2) {
            throw new IllegalArgumentException("DKII bump height fields must be at least 2 by 2 pixels.");
        }
        ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
        for (int y = 0; y < height; y++) {
            int previousY = (y - 1 + height) % height;
            int nextY = (y + 1) % height;
            for (int x = 0; x < width; x++) {
                int previousX = (x - 1 + width) % width;
                int nextX = (x + 1) % width;
                int center = source.getRGB(x, y);
                float left = red(source.getRGB(previousX, y));
                float right = red(source.getRGB(nextX, y));
                float down = red(source.getRGB(x, previousY));
                float up = red(source.getRGB(x, nextY));
                float du = clamp((right - left) * 0.5f * width, -2f, 2f);
                float dv = clamp((up - down) * 0.5f * height, -2f, 2f);
                // The shader decodes RG as [-1, 1]. Store half the finite
                // difference and compensate in the bump matrix above.
                pixels.put(encodeSigned(du * 0.5f));
                pixels.put(encodeSigned(dv * 0.5f));
                pixels.put((byte) 0);
                pixels.put((byte) (center >>> 24));
            }
        }
        pixels.flip();
        return new Image(Image.Format.RGBA8, width, height, pixels, ColorSpace.Linear);
    }

    private static float red(int argb) {
        return ((argb >>> 16) & 0xff) / 255f;
    }

    private static byte encodeSigned(float value) {
        return (byte) Math.round((clamp(value, -1f, 1f) * 0.5f + 0.5f) * 255f);
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    private static void validateFrames(List<Image> frames) {
        Image first = frames.get(0);
        for (Image frame : frames) {
            if (frame.getWidth() != first.getWidth() || frame.getHeight() != first.getHeight()
                    || frame.getFormat() != first.getFormat()) {
                throw new IllegalArgumentException("DKII water bump frames must have identical dimensions and formats.");
            }
        }
    }

    private static String texturePath(String name) {
        return AssetsConverter.TEXTURES_FOLDER + "/" + name;
    }
}
