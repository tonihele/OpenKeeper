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
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.math.Vector4f;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureArray;
import com.jme3.texture.image.ColorSpace;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.tools.convert.AssetsConverter;

/**
 * Loads and shares the original DKII water/lava EMBM textures for one
 * AssetManager. The bump inputs stay linear data; color maps use jME's normal
 * image color-space handling.
 */
final class Dk2EmbmResources {

    enum Liquid {
        WATER("BumpWater0.png", "BumpWaterEnvMap0.png", null,
                0.035f, 0.85f, 12.0f, false, 1.0f, 0.0f, 0.0f,
                1.0f, true, true, false),
        LAVA("BumpLava0.png", "BumpLavaEnvMap0.png", "BumpLavaBumpmap0.png",
                0.12f, 1.25f, 0.10f, true, 1.0f, 0.0f, 1.0f,
                1.0f, true, true, false);

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
        private final boolean heightMap;
        private final boolean sphereEnvironmentUv;
        private final boolean reflectionUsesSurfaceLighting;

        Liquid(String baseMap, String environmentMap, String bumpMap,
                float bumpScale, float reflectionIntensity, float bumpAnimationSpeed,
                boolean useLuminance, float luminanceScale, float luminanceOffset, float baseAlpha,
                float baseColorMix, boolean heightMap, boolean sphereEnvironmentUv,
                boolean reflectionUsesSurfaceLighting) {
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
            this.heightMap = heightMap;
            this.sphereEnvironmentUv = sphereEnvironmentUv;
            this.reflectionUsesSurfaceLighting = reflectionUsesSurfaceLighting;
        }
    }

    private record Textures(Texture2D baseMap, Texture2D bumpMap, TextureArray bumpFrames,
            Texture2D environmentMap) {

        void apply(Material material, Liquid liquid) {
            material.setTexture("EmbmBaseMap", baseMap);
            material.setTexture("EmbmEnvironmentMap", environmentMap);
            if (bumpFrames != null) {
                material.setTexture("EmbmBumpFrames", bumpFrames);
                material.setInt("EmbmBumpFrameCount", bumpFrames.getImage().getDepth());
            } else {
                material.setTexture("EmbmBumpMap", bumpMap);
            }
            float bumpIntensity = multiplier(Settings.Setting.EMBM_BUMP_INTENSITY);
            material.setVector4("EmbmBumpMatrix", new Vector4f(liquid.bumpScale * bumpIntensity, 0f, 0f, liquid.bumpScale * bumpIntensity));
            material.setFloat("EmbmReflectionIntensity", liquid.reflectionIntensity
                    * multiplier(Settings.Setting.EMBM_REFLECTION_INTENSITY));
            float animationSpeed = multiplier(Settings.Setting.EMBM_ANIMATION_SPEED);
            material.setFloat("EmbmBumpAnimationSpeed", liquid.bumpAnimationSpeed * animationSpeed);
            material.setFloat("EmbmLuminanceScale", liquid.luminanceScale);
            material.setFloat("EmbmLuminanceOffset", liquid.luminanceOffset);
            material.setFloat("EmbmBaseAlpha", liquid.baseAlpha);
            material.setFloat("EmbmBaseColorMix", liquid.baseColorMix);
            material.setBoolean("EmbmUseLuminance", liquid.useLuminance);
            material.setBoolean("EmbmUseHeightMap", liquid.heightMap);
            material.setBoolean("EmbmUseSphereEnvironmentUv", liquid.sphereEnvironmentUv);
            material.setBoolean("EmbmUseSurfaceLighting", liquid.reflectionUsesSurfaceLighting);

            material.setBoolean("EmbmEnabled", true);
            if (liquid == Liquid.LAVA) {
                // Keep the existing OpenKeeper lava sway on the GPU without adding vertices.
                // These wave parameters are separate from EMBM texture perturbation.
                material.setVector4("LavaWaveParameters", new Vector4f(0.04f, 0.04f, 3.0f, 3.0f * animationSpeed));
                material.setBoolean("LavaWavesEnabled", true);
            }
        }
    }

    private static final Map<AssetManager, Map<Liquid, Optional<Textures>>> CACHE = new WeakHashMap<>();

    private static float multiplier(Settings.Setting setting) {
        float value = Settings.getInstance().getFloat(setting);
        return Float.isFinite(value) ? Math.max(0f, Math.min(value, 4f)) : 1f;
    }

    static Optional<Dk2EmbmResources> load(AssetManager assetManager, Liquid liquid) {
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

    void apply(Material material) {
        textures.apply(material, liquid);
    }

    static List<String> waterBumpFrameNames() {
        List<String> names = new ArrayList<>(32);
        for (int frame = 0; frame < 32; frame++) {
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
            if (liquid == Liquid.WATER) {
                List<Image> frames = new ArrayList<>(32);
                for (String frameName : waterBumpFrameNames()) {
                    frames.add(loadLinearImage(assetManager, frameName));
                }
                validateFrames(frames);
                bumpFrames = new TextureArray(frames);
                // The frames are scalar height fields authored to tile seamlessly.
                bumpFrames.setWrap(Texture.WrapMode.Repeat);
            } else {
                bump = new Texture2D(loadLinearImage(assetManager, texturePath(liquid.bumpMap)));
                bump.setWrap(Texture.WrapMode.Repeat);
            }
            environment.setWrap(Texture.WrapMode.EdgeClamp);
            return Optional.of(new Textures(base, bump, bumpFrames, environment));
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

    private static Image loadLinearImage(AssetManager assetManager, String name) {
        Image image = loadTexture(assetManager, name).getImage().clone();
        image.setColorSpace(ColorSpace.Linear);
        return image;
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
