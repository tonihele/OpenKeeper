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
package toniarts.openkeeper.view.lighting;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.shader.VarType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.utils.Point;

/**
 * Turns the current set of {@link LightSource}s into what the two
 * {@code DkLighting} material techniques (design &sect;9's two evaluation
 * modes) actually consume:
 * <ul>
 * <li><b>Per-vertex terrain</b> ({@code DkTerrain.j3md}): every tile's
 * geometry gets an array of its nearest in-range lights uploaded as
 * material parameters, refreshed every frame (lights flicker/pulse/fade,
 * so this can't be a one-time upload). Tile-vs-light selection is an
 * approximation by tile-centre distance (padded, see
 * {@link #TILE_SELECTION_PADDING}) -- a renderer/culling detail per D9,
 * not a change to the lighting model itself, which is still evaluated
 * exactly, per vertex, in the shader.</li>
 * <li><b>Flat things</b> ({@code DkFlat.j3md}): {@link #computeFlatLight}
 * is a pure function callers (entity containers, {@code VisualEffect})
 * invoke themselves each frame with the owner's current position, per
 * design &sect;7.4 -- this class does not track thing positions.</li>
 * </ul>
 * This is the {@code SceneLights} of {@code lighting_design.md} &sect;4/
 * &sect;11: the only place that knows how sources get turned into pixels.
 *
 * @author Toni Helenius &lt;helenius.toni@gmail.com&gt;
 */
public final class SceneLights {

    /**
     * Must match {@code DK_MAX_LIGHTS} in {@code DkTerrain.vert}.
     */
    public static final int MAX_LIGHTS_PER_MATERIAL = 16;

    /**
     * Extra distance added to a light's radius when deciding whether it
     * might reach a tile, to account for selecting by tile-centre distance
     * rather than by each vertex (wall geometry extends above the tile,
     * corners are up to ~0.71 tiles from centre).
     */
    private static final float TILE_SELECTION_PADDING = 1.0f;

    private final Map<Point, List<Material>> terrainMaterialsByTile = new HashMap<>();
    private final Map<Object, Region> regionMaterials = new HashMap<>();
    private ColorRGBA globalAmbient = ColorRGBA.Black.clone();

    /**
     * A room's whole floor/wall is built (and batched) as one big mesh by
     * {@code RoomConstructor}, spanning many tiles -- it can't be keyed by
     * a single tile the way ordinary terrain geometry is, so it's
     * registered against its own centre + a bounding radius instead (see
     * {@link #registerRegionMaterials}).
     */
    private static final class Region {
        final Vector3f center;
        final float radius;
        final List<Material> materials;

        Region(Vector3f center, float radius, List<Material> materials) {
            this.center = center;
            this.radius = radius;
            this.materials = materials;
        }
    }

    /**
     * The video "ambient light" setting (design &sect;8.2), pushed into
     * every registered terrain material every frame (see {@link
     * #update(List)}) and available for callers of {@link
     * #computeFlatLight} to set on their own {@code DkFlat} materials
     * alongside the flat-light colour.
     */
    public void setGlobalAmbient(ColorRGBA globalAmbient) {
        this.globalAmbient = globalAmbient;
    }

    public ColorRGBA getGlobalAmbient() {
        return globalAmbient;
    }

    /**
     * Registers (appending to any already-registered materials for this
     * tile) the DkTerrain materials belonging to one piece of a tile's
     * geometry (e.g. its floor, then separately its walls). Call {@link
     * #unregisterTerrainTile} first on a rebuild so this doesn't grow
     * unbounded.
     */
    public void registerTerrainTile(Point tile, List<Material> materials) {
        terrainMaterialsByTile.computeIfAbsent(tile, k -> new ArrayList<>()).addAll(materials);
    }

    public void unregisterTerrainTile(Point tile) {
        terrainMaterialsByTile.remove(tile);
    }

    /**
     * Registers the materials belonging to a whole room's batched floor/
     * wall mesh (design §8.1's per-room-type ambient still applies, it's
     * just not keyed per-tile like ordinary terrain). Replaces any
     * previous registration under the same key -- rooms are constructed
     * once, not incrementally, so there's nothing to append to.
     */
    public void registerRegionMaterials(Object key, Vector3f center, float radius, List<Material> materials) {
        regionMaterials.put(key, new Region(center, radius, materials));
    }

    public void unregisterRegionMaterials(Object key) {
        regionMaterials.remove(key);
    }

    /**
     * Refreshes every registered terrain tile's light-array material
     * parameters from the current state of {@code lights}. Meant to be
     * called once per frame (see {@link LightRules#update(float)}) --
     * flicker/pulse/fade mean this can't be a one-shot upload done only
     * when lights are created or moved.
     * <p>
     * This is an unconditional full pass over every registered tile,
     * deliberately simple rather than spatially indexed -- design's D9
     * explicitly leaves renderer batching/culling as the renderer's own
     * business, and profiling this before adding any smarter indexing is
     * called out as its own follow-up (see the plan's settings/cleanup
     * phase) rather than something to guess at up front.
     */
    public void update(List<LightSource> lights) {
        if (terrainMaterialsByTile.isEmpty() && regionMaterials.isEmpty()) {
            return;
        }
        if (lights.isEmpty()) {
            for (List<Material> materials : terrainMaterialsByTile.values()) {
                for (Material material : materials) {
                    clearLights(material);
                }
            }
            for (Region region : regionMaterials.values()) {
                for (Material material : region.materials) {
                    clearLights(material);
                }
            }
            return;
        }

        List<LightSource> nearby = new ArrayList<>(MAX_LIGHTS_PER_MATERIAL + 1);
        for (Map.Entry<Point, List<Material>> entry : terrainMaterialsByTile.entrySet()) {
            Point tile = entry.getKey();
            updateGroup(entry.getValue(), tile.x + 0.5f, tile.y + 0.5f, TILE_SELECTION_PADDING, lights, nearby);
        }
        for (Region region : regionMaterials.values()) {
            updateGroup(region.materials, region.center.x, region.center.z, region.radius, lights, nearby);
        }
    }

    private void updateGroup(List<Material> materials, float centerX, float centerZ, float padding,
            List<LightSource> lights, List<LightSource> nearby) {
        nearby.clear();
        for (LightSource light : lights) {
            if (!light.isEnabled()) {
                continue;
            }
            Vector3f pos = light.getPosition();
            float dx = pos.x - centerX;
            float dz = pos.z - centerZ;
            float reach = light.getRadius() + padding;
            if (dx * dx + dz * dz <= reach * reach) {
                nearby.add(light);
            }
        }

        if (nearby.size() > MAX_LIGHTS_PER_MATERIAL) {
            nearby.sort(Comparator.comparingDouble(l -> distanceSquared(l, centerX, centerZ)));
        }

        int count = Math.min(nearby.size(), MAX_LIGHTS_PER_MATERIAL);
        for (Material material : materials) {
            applyLights(material, nearby, count);
        }
    }

    private static float distanceSquared(LightSource light, float centerX, float centerZ) {
        Vector3f pos = light.getPosition();
        float dx = pos.x - centerX;
        float dz = pos.z - centerZ;
        return dx * dx + dz * dz;
    }

    private void applyLights(Material material, List<LightSource> nearby, int count) {
        Vector4f[] posRadius = new Vector4f[count];
        Vector4f[] colorIntensity = new Vector4f[count];
        for (int i = 0; i < count; i++) {
            LightSource light = nearby.get(i);
            Vector3f pos = light.getPosition();
            posRadius[i] = new Vector4f(pos.x, pos.y, pos.z, light.getRadius());
            colorIntensity[i] = new Vector4f(
                    light.getRed() / 255f, light.getGreen() / 255f, light.getBlue() / 255f,
                    light.getEffectiveIntensity());
        }
        material.setParam("LightPosRadius", VarType.Vector4Array, posRadius);
        material.setParam("LightColorIntensity", VarType.Vector4Array, colorIntensity);
        material.setInt("NumLights", count);
        material.setColor("GlobalAmbient", globalAmbient);
    }

    private void clearLights(Material material) {
        material.setInt("NumLights", 0);
        material.setColor("GlobalAmbient", globalAmbient);
    }

    /**
     * The "flat" evaluation from design &sect;9: one sample at
     * {@code worldPos}, normal term dropped, summed over every light
     * (there is no per-material cap here -- this is a single CPU-side
     * point sample, not a per-vertex GPU loop). Pure function; callers
     * (entity containers, {@code VisualEffect}) invoke this themselves
     * each frame with the owner's current position (design &sect;7.4) and
     * set the result as their material's {@code FlatLight} parameter.
     */
    public static ColorRGBA computeFlatLight(Vector3f worldPos, Iterable<LightSource> lights) {
        float r = 0, g = 0, b = 0;
        for (LightSource light : lights) {
            if (!light.isEnabled()) {
                continue;
            }
            Vector3f pos = light.getPosition();
            float dx = pos.x - worldPos.x;
            float dy = pos.y - worldPos.y;
            float dz = pos.z - worldPos.z;
            float d2 = dx * dx + dy * dy + dz * dz;
            float radius = light.getRadius();
            float r2 = radius * radius;
            if (r2 <= 0f || d2 >= r2) {
                continue;
            }

            float n = (float) Math.floor(16.0 * d2);
            float f = dkFalloff(n);
            float k = (r2 - d2) * f * (1f / r2) * light.getEffectiveIntensity();
            k = Math.min(k, 1f);
            if (k <= 0f) {
                continue;
            }

            r += (light.getRed() / 255f) * k;
            g += (light.getGreen() / 255f) * k;
            b += (light.getBlue() / 255f) * k;
        }
        return new ColorRGBA(r, g, b, 1f);
    }

    /**
     * Java-side twin of {@code DkFalloff.glsllib}'s {@code dkFalloff} --
     * keep the two in sync, see that file for the design rationale
     * (&sect;9, decision D7).
     */
    private static float dkFalloff(float n) {
        if (n < 0.5f) {
            return 1e6f;
        }
        if (n < 256f) {
            return 0.25f + 16f / n;
        }
        return n - 256f;
    }
}
