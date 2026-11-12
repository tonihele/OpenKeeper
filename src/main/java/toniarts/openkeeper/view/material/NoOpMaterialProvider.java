/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.material;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.view.lighting.LightRules;

/** No-op lighting provider for headless paths that never create render materials. */
public final class NoOpMaterialProvider implements MaterialProvider {

    public static final NoOpMaterialProvider INSTANCE = new NoOpMaterialProvider();

    private NoOpMaterialProvider() {
    }

    @Override
    public Material createVertexMaterial(Material original, ColorRGBA ambient) {
        throw new IllegalStateException("A no-op material provider cannot create render materials");
    }

    @Override
    public Material createDkSurfaceMaterial(Material original) {
        throw new IllegalStateException("A no-op material provider cannot create render materials");
    }

    @Override
    public void applyFlatLight(Spatial spatial, LightRules lightRules, boolean ignoreObjectLights) {
    }

    @Override
    public void applyVertexLight(Spatial spatial, LightRules lightRules,
            boolean ignoreObjectLights, boolean useTileAmbient) {
    }

    @Override
    public void applyFullLight(Spatial spatial) {
    }
}
