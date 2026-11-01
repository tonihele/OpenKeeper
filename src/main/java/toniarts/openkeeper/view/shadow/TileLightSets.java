/*
 * Copyright (C) 2014-2025 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.view.shadow;

import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.view.lighting.LightSource;

/**
 * The lights that may affect things standing on a tile: up to
 * {@link ShadowConstants#SET_TOTAL_MAX}, the static ones (at most
 * {@link ShadowConstants#SET_STATIC_MAX}) first. A light gets into the set of
 * a tile if it is registered within the 7x7 tiles around it and the light's
 * radius, plus one tile, reaches the tile's centre. The light's radius
 * is what limits its shadows, and glow lights compete with torches for the
 * static places, nearest first.
 * <p>
 * Light sources are registered per position here, as the lighting system
 * keeps no per tile lists. Sets are built on demand and kept for the frame.
 */
final class TileLightSets {

    private final Map<Long, List<LightSource>> registered = new HashMap<>();
    private final Map<Long, List<LightSource>> sets = new HashMap<>();

    /**
     * Forgets last frame's sets and registers this frame's lights
     */
    void beginFrame(List<LightSource> lights) {
        registered.clear();
        sets.clear();
        for (LightSource light : lights) {
            if (!light.isEnabled()) {
                continue;
            }
            Vector3f position = light.getPosition();
            registered.computeIfAbsent(key(tile(position.x), tile(position.z)), k -> new ArrayList<>()).add(light);
        }
    }

    /**
     * Whether lights of this kind are static, i.e. belong to the world rather
     * than to something that moves or comes and goes
     */
    static boolean isStatic(LightSource light) {
        return switch (light.getKind()) {
            case TORCH, TERRAIN, SLAB, PORTAL ->
                true;
            case THING, CURSOR ->
                false;
        };
    }

    /**
     * Whether a light casts shadows: wall torches and the lights that move or
     * come and go (objects, the hand, effects). The glow of the terrain
     * lights the world but never casts.
     */
    static boolean castsShadows(LightSource light) {
        return switch (light.getKind()) {
            case TORCH, THING, CURSOR ->
                true;
            case TERRAIN, SLAB, PORTAL ->
                false;
        };
    }

    List<LightSource> get(int tileX, int tileZ) {
        return sets.computeIfAbsent(key(tileX, tileZ), k -> gather(tileX, tileZ));
    }

    private List<LightSource> gather(int tileX, int tileZ) {
        float centreX = tileX + 0.5f;
        float centreZ = tileZ + 0.5f;
        List<LightSource> statics = new ArrayList<>();
        List<LightSource> dynamics = new ArrayList<>();

        // The static half first, so the dynamic one knows the places left
        int radius = ShadowConstants.NEIGHBOURHOOD_RADIUS;
        for (boolean staticPass : new boolean[]{true, false}) {
            for (int x = tileX - radius; x <= tileX + radius; x++) {
                for (int z = tileZ - radius; z <= tileZ + radius; z++) {
                    List<LightSource> lights = registered.get(key(x, z));
                    if (lights == null) {
                        continue;
                    }
                    for (LightSource light : lights) {
                        float reach = light.getRadius() + 1f;
                        if (isStatic(light) != staticPass || distanceSquared(light, centreX, centreZ) >= reach * reach) {
                            continue;
                        }
                        if (staticPass) {
                            if (statics.size() < ShadowConstants.SET_STATIC_MAX) {
                                statics.add(light);
                            } else {
                                replaceFarthest(statics, light, centreX, centreZ);
                            }
                        } else {
                            if (statics.size() + dynamics.size() < ShadowConstants.SET_TOTAL_MAX) {
                                dynamics.add(light);
                            } else {
                                replaceFarthest(dynamics, light, centreX, centreZ);
                            }
                        }
                    }
                }
            }
        }

        List<LightSource> result = new ArrayList<>(statics.size() + dynamics.size());
        result.addAll(statics);
        result.addAll(dynamics);
        return result;
    }

    private static void replaceFarthest(List<LightSource> lights, LightSource candidate, float centreX, float centreZ) {
        int farthest = -1;
        float farthestDistance = distanceSquared(candidate, centreX, centreZ);
        for (int i = 0; i < lights.size(); i++) {
            float distance = distanceSquared(lights.get(i), centreX, centreZ);
            if (distance > farthestDistance) {
                farthestDistance = distance;
                farthest = i;
            }
        }
        if (farthest >= 0) {
            lights.set(farthest, candidate);
        }
    }

    private static float distanceSquared(LightSource light, float x, float z) {
        Vector3f position = light.getPosition();
        float dx = position.x - x;
        float dz = position.z - z;
        return dx * dx + dz * dz;
    }

    static int tile(float coordinate) {
        return (int) Math.floor(coordinate);
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
