/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.material;

/**
 * Runtime policy for material setup optimizations.
 *
 * @param cacheVertexLightingSetup skip repeated setup for spatial instances
 *        whose material graph is unchanged; call
 *        {@link MaterialProvider#invalidate(com.jme3.scene.Spatial)} after
 *        every graph change
 */
public record MaterialProviderConfig(boolean cacheVertexLightingSetup) {

    /** Disable the setup cache for diagnosis or callers with untracked mutations. */
    public static final MaterialProviderConfig UNCACHED = new MaterialProviderConfig(false);

    /** Enable the setup cache after known gameplay mutations invalidate it. */
    public static final MaterialProviderConfig CACHED = new MaterialProviderConfig(true);

    /** Safe default for arbitrary providers and consumers. */
    public static final MaterialProviderConfig DEFAULT = UNCACHED;

    /**
     * Policy for the main gameplay composition root. Animation and blueprint
     * controls invalidate changed entity graphs; other consumers stay uncached
     * unless they opt in explicitly. Override at launch with
     * {@code -Dopenkeeper.material.cacheVertexLightingSetup=false}.
     */
    public static MaterialProviderConfig gameplayDefault() {
        return Boolean.parseBoolean(System.getProperty("openkeeper.material.cacheVertexLightingSetup", "true"))
                ? CACHED : UNCACHED;
    }

}
