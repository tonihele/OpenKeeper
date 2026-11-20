/*
 * Copyright (C) 2026 OpenKeeper
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package toniarts.openkeeper.view.material;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import toniarts.openkeeper.utils.AssetUtils;

/** Converts supported entity materials once, without modifying shared source materials. */
public final class JmeMaterialProvider implements MaterialProvider {
    private final AssetManager assetManager;

    public JmeMaterialProvider(AssetManager assetManager) {
        this.assetManager = Objects.requireNonNull(assetManager);
    }

    @Override
    public List<Material> prepare(Spatial root, MaterialSetupPolicy policy, List<Material> previous) {
        return prepare(assetManager, root, policy, previous, true);
    }

    /** Uncached compatibility path; runtime owners use {@link EntityMaterialSetup}. */
    public static List<Material> prepare(AssetManager assets, Spatial root, MaterialSetupPolicy policy,
            List<Material> previous, boolean isolate) {
        var owned = new IdentityHashMap<Material, Boolean>();
        for (Material material : previous) {
            owned.put(material, Boolean.TRUE);
        }
        var replacements = new IdentityHashMap<Material, Material>();
        List<Material> result = new ArrayList<>();
        visitGeometry(root, true, geometry -> {
            if (geometry.isBatched()) {
                return;
            }
            Material source = geometry.getMaterial();
            if (source == null || (!AssetUtils.isVertexMaterial(source)
                    && source.getMaterialDef().getMaterialParam("FlatLight") == null)) {
                return;
            }
            Material material = replacements.get(source);
            if (material == null) {
                if (!policy.fullBright() && !AssetUtils.isVertexMaterial(source)) {
                    material = AssetUtils.createDkSurfaceMaterial(assets, source);
                    material.getAdditionalRenderState().setFaceCullMode(source.getAdditionalRenderState().getFaceCullMode());
                    material.getAdditionalRenderState().setDepthTest(source.getAdditionalRenderState().isDepthTest());
                    material.getAdditionalRenderState().setDepthWrite(source.getAdditionalRenderState().isDepthWrite());
                } else {
                    material = isolate && !owned.containsKey(source) ? source.clone() : source;
                }
                replacements.put(source, material);
                applyPolicy(material, policy);
                result.add(material);
            }
            if (material != source) {
                geometry.setMaterial(material);
            }
        });
        return result;
    }

    private static void visitGeometry(Spatial spatial, boolean root, Consumer<Geometry> visitor) {
        if (!root && EntityMaterialSetup.isRegisteredRoot(spatial)) {
            return;
        }
        if (spatial instanceof Geometry geometry) {
            visitor.accept(geometry);
        } else if (spatial instanceof Node node) {
            var children = node.getChildren();
            for (int i = 0; i < children.size(); i++) {
                visitGeometry(children.get(i), false, visitor);
            }
        }
    }

    private static void applyPolicy(Material material, MaterialSetupPolicy policy) {
        if (AssetUtils.isVertexMaterial(material)) {
            if (material.getMaterialDef().getMaterialParam("LightIndexData") != null) {
                material.clearParam("LightIndexData");
                material.clearParam("LightIndexRow");
                material.clearParam("LightIndexCount");
            }
            material.setFloat("MinLight", policy.fullBright() ? 1f : 0f);
            material.setFloat("RockFloor", policy.fullBright() ? 1f : 0f);
            material.setBoolean("IgnoreObjectLights", policy.ignoreObjectLights());
            material.setFloat("AmbientScale", policy.useTileAmbient() ? 1f : 0f);
            if (policy.fullBright()) {
                material.clearParam("LightData");
                material.clearParam("AmbientMap");
                material.clearParam("AmbientMapSize");
                material.clearParam("MeshCenter");
            }
        } else if (policy.fullBright()) {
            material.setColor("FlatLight", ColorRGBA.White);
            material.setColor("GlobalAmbient", ColorRGBA.BlackNoAlpha);
        }
    }
}
