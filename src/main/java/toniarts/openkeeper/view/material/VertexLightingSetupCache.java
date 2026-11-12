/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.material;

import com.jme3.scene.Spatial;

import java.util.Map;
import java.util.WeakHashMap;

/** Weak, synchronized cache for stable per-spatial lighting setup. */
final class VertexLightingSetupCache {

    private final Map<Spatial, Entry> entries = new WeakHashMap<>();

    synchronized boolean isConfigured(Spatial spatial, Object lightContext, long bindingRevision,
            boolean ignoreObjectLights, boolean useTileAmbient) {
        Entry entry = entries.get(spatial);
        return entry != null && entry.matches(lightContext, bindingRevision, ignoreObjectLights, useTileAmbient);
    }

    synchronized void markConfigured(Spatial spatial, Object lightContext, long bindingRevision,
            boolean ignoreObjectLights, boolean useTileAmbient) {
        entries.put(spatial, new Entry(lightContext, bindingRevision, ignoreObjectLights, useTileAmbient));
    }

    synchronized void invalidate(Spatial spatial) {
        entries.remove(spatial);
    }

    synchronized void clear() {
        entries.clear();
    }

    private record Entry(Object lightContext, long bindingRevision,
            boolean ignoreObjectLights, boolean useTileAmbient) {

        boolean matches(Object context, long revision, boolean ignore, boolean tileAmbient) {
            return lightContext == context && bindingRevision == revision
                    && ignoreObjectLights == ignore && useTileAmbient == tileAmbient;
        }
    }
}
