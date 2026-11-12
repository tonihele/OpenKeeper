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

/**
 * Injection boundary for OpenKeeper's runtime material and lighting work.
 * Implementations may provide alternate policies or diagnostics without
 * making gameplay code depend on a concrete jME implementation.
 */
public interface MaterialProvider {

    Material createVertexMaterial(Material original, ColorRGBA ambient);

    Material createDkSurfaceMaterial(Material original);

    void applyFlatLight(Spatial spatial, LightRules lightRules, boolean ignoreObjectLights);

    void applyVertexLight(Spatial spatial, LightRules lightRules,
            boolean ignoreObjectLights, boolean useTileAmbient);

    void applyFullLight(Spatial spatial);

    /**
     * Invalidates cached setup after changing the spatial's geometry or
     * material graph. The next lighting application will configure it again.
     */
    default void invalidate(Spatial spatial) {
    }

    /** Reconfigures runtime policy without changing the caller. */
    default void configure(MaterialProviderConfig config) {
    }
}
