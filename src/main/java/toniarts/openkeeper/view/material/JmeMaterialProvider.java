/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.material;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.view.lighting.LightRules;
import toniarts.openkeeper.utils.AssetUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** jMonkeyEngine-backed implementation of {@link MaterialProvider}. */
public final class JmeMaterialProvider implements MaterialProvider {

    private final AssetManager assetManager;
    private final VertexLightingSetupCache setupCache = new VertexLightingSetupCache();
    private volatile MaterialProviderConfig config;

    public JmeMaterialProvider(AssetManager assetManager) {
        this(assetManager, MaterialProviderConfig.DEFAULT);
    }

    public JmeMaterialProvider(AssetManager assetManager, MaterialProviderConfig config) {
        this.assetManager = Objects.requireNonNull(assetManager, "assetManager");
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public Material createVertexMaterial(Material original, ColorRGBA ambient) {
        return createVertexMaterial(assetManager, original, ambient);
    }

    /** Shared implementation used by the legacy AssetUtils facade. */
    public static Material createVertexMaterial(AssetManager assetManager, Material original, ColorRGBA ambient) {
        Material vertex = new Material(assetManager, AssetUtils.VERTEX_MATDEF);
        vertex.setName(original.getName());
        if (original.getTextureParam("DiffuseMap") != null) {
            vertex.setTexture("DiffuseMap", original.getTextureParam("DiffuseMap").getTextureValue());
        }
        if (original.getParam("AlphaDiscardThreshold") != null) {
            vertex.setFloat("AlphaDiscardThreshold", (Float) original.getParam("AlphaDiscardThreshold").getValue());
        }
        vertex.setTransparent(original.isTransparent());
        vertex.setColor("Ambient", ambient);
        return vertex;
    }

    @Override
    public Material createDkSurfaceMaterial(Material original) {
        return createDkSurfaceMaterial(assetManager, original);
    }

    /** Shared implementation used by the legacy AssetUtils facade. */
    public static Material createDkSurfaceMaterial(AssetManager assetManager, Material original) {
        Material vertex = createVertexMaterial(assetManager, original, ColorRGBA.Black);
        if (original.getParam("NumberOfTiles") != null) {
            vertex.setInt("NumberOfTiles", (Integer) original.getParam("NumberOfTiles").getValue());
        }
        if (original.getParam("Speed") != null) {
            vertex.setInt("Speed", (Integer) original.getParam("Speed").getValue());
        }
        vertex.getAdditionalRenderState().setBlendMode(original.getAdditionalRenderState().getBlendMode());
        return vertex;
    }

    @Override
    public void applyFlatLight(Spatial spatial, LightRules lightRules, boolean ignoreObjectLights) {
        invalidate(spatial);
        applyFlatLightUncached(spatial, lightRules, ignoreObjectLights);
    }

    /** Shared implementation used by the legacy AssetUtils facade. */
    public static void applyFlatLightUncached(Spatial spatial, LightRules lightRules, boolean ignoreObjectLights) {
        ColorRGBA flatLight = lightRules.computeFlatLight(spatial.getWorldTranslation(), ignoreObjectLights);
        ColorRGBA globalAmbient = lightRules.getGlobalAmbient();
        spatial.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial s) {
                if (!(s instanceof Geometry geometry)) {
                    return;
                }
                Material material = geometry.getMaterial();
                if (material.getMaterialDef().getMaterialParam("FlatLight") == null) {
                    return;
                }
                material.setColor("FlatLight", flatLight);
                material.setColor("GlobalAmbient", globalAmbient);
            }
        });
    }

    @Override
    public void applyVertexLight(Spatial spatial, LightRules lightRules,
            boolean ignoreObjectLights, boolean useTileAmbient) {
        Objects.requireNonNull(spatial, "spatial");
        Objects.requireNonNull(lightRules, "lightRules");
        MaterialProviderConfig currentConfig = config;
        long bindingRevision = lightRules.getMaterialBindingRevision();
        if (currentConfig.cacheVertexLightingSetup()
                && setupCache.isConfigured(spatial, lightRules, bindingRevision,
                        ignoreObjectLights, useTileAmbient)) {
            return;
        }

        applyVertexLightUncached(assetManager, spatial, lightRules, ignoreObjectLights, useTileAmbient);
        if (currentConfig.cacheVertexLightingSetup()) {
            setupCache.markConfigured(spatial, lightRules, bindingRevision,
                    ignoreObjectLights, useTileAmbient);
        }
    }

    private static void applyVertexLightUncached(AssetManager assetManager, Spatial spatial, LightRules lightRules,
            boolean ignoreObjectLights, boolean useTileAmbient) {
        Objects.requireNonNull(assetManager, "assetManager");
        Objects.requireNonNull(spatial, "spatial");
        Objects.requireNonNull(lightRules, "lightRules");
        List<Material> materials = new ArrayList<>();
        spatial.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial s) {
                if (!(s instanceof Geometry geometry) || geometry.isBatched()) {
                    return;
                }
                Material material = geometry.getMaterial();
                if (!AssetUtils.isVertexMaterial(material)) {
                    if (material.getMaterialDef().getMaterialParam("FlatLight") == null) {
                        return;
                    }
                    Material vertex = createVertexMaterial(assetManager, material, ColorRGBA.Black);
                    vertex.getAdditionalRenderState().setFaceCullMode(material.getAdditionalRenderState().getFaceCullMode());
                    vertex.getAdditionalRenderState().setBlendMode(material.getAdditionalRenderState().getBlendMode());
                    geometry.setMaterial(vertex);
                    material = vertex;
                }
                material.setFloat("MinLight", 0f);
                material.setBoolean("IgnoreObjectLights", ignoreObjectLights);
                material.setFloat("AmbientScale", useTileAmbient ? 1f : 0f);
                materials.add(material);
            }
        });
        lightRules.bindMaterials(materials);
    }

    /** Shared uncached implementation used by the legacy AssetUtils facade. */
    public static void applyVertexLight(AssetManager assetManager, Spatial spatial, LightRules lightRules,
            boolean ignoreObjectLights, boolean useTileAmbient) {
        applyVertexLightUncached(assetManager, spatial, lightRules, ignoreObjectLights, useTileAmbient);
    }

    @Override
    public void applyFullLight(Spatial spatial) {
        invalidate(spatial);
        applyFullLightUncached(spatial);
    }

    /** Shared implementation used by the legacy AssetUtils facade. */
    public static void applyFullLightUncached(Spatial spatial) {
        spatial.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial s) {
                if (!(s instanceof Geometry geometry)) {
                    return;
                }
                Material material = geometry.getMaterial();
                if (AssetUtils.isVertexMaterial(material)) {
                    material.setFloat("MinLight", 1f);
                    material.setFloat("RockFloor", 1f);
                    return;
                }
                if (material.getMaterialDef().getMaterialParam("FlatLight") == null) {
                    return;
                }
                material.setColor("FlatLight", ColorRGBA.White);
                material.setColor("GlobalAmbient", ColorRGBA.BlackNoAlpha);
            }
        });
    }

    @Override
    public void invalidate(Spatial spatial) {
        setupCache.invalidate(spatial);
    }

    @Override
    public void configure(MaterialProviderConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        if (!config.cacheVertexLightingSetup()) {
            setupCache.clear();
        }
    }
}
