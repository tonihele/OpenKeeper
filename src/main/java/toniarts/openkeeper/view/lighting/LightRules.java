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
import com.jme3.renderer.Camera;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.tools.convert.map.Light;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/**
 * The single creator/destroyer of {@link LightSource}s. Callers (the mesh-building code that already knows the
 * resolved position/colour/radius for a torch, terrain tile, thing or
 * room) ask this class to create or drop a light; this class owns the
 * owner-keyed registries needed to find and remove them again, and drives
 * both {@link LightModulators} and {@link SceneLights} once per frame.
 * <p>
 * Deliberately presentation-only: nothing here is read by the simulation
 * state.
 * <p>
 * <b>Lifecycle.</b> Instances start in {@link Phase#LOADING}. The loader
 * thread (or a synchronous load on the caller thread) may register lights,
 * bind materials and fill ambient/page data. Consumers on the render thread
 * must not drive the frame upload until {@link #publishReady()} — a volatile
 * write that publishes the completed registries. After that, only the render
 * thread mutates and consumes. {@link #dispose()} ends the lifecycle; all
 * consumer APIs become no-ops. Readiness is owned here so callers cannot
 * reintroduce load races by forgetting an external gate.
 */
public final class LightRules {

    /**
     * Publication phases for the light registries and {@link SceneLights}
     * page/material bindings.
     */
    public enum Phase {
        /** Map/mesh load may still be mutating registries. */
        LOADING,
        /** Load finished; safe for per-frame update and flat-light sampling. */
        READY,
        /** Torn down; consumer APIs no-op. */
        DISPOSED
    }

    private final LightModulators modulators = new LightModulators();
    private final SceneLights sceneLights;
    private final IFogOfWarInformation fogOfWar;
    private boolean cheapLighting;
    // Replaced, never mutated: read on the render thread, set from the UI
    private volatile EnumSet<LightSource.Group> disabledGroups = EnumSet.noneOf(LightSource.Group.class);
    /** Happens-before publication of load-time light/material state. */
    private volatile Phase phase = Phase.LOADING;

    /**
     * Torch and terrain lights, keyed by their owner tile -- a tile
     * rebuild removes and recreates both together, mirroring how the mesh
     * builder already replaces the whole tile subtree.
     */
    private final Map<Point, List<LightSource>> tileLights = new HashMap<>();

    private final Map<Object, LightSource> roomLights = new HashMap<>();
    private final Map<Object, LightSource> thingLights = new HashMap<>();

    private LightSource cursorLight;
    /** Reused when debug groups filter the upload set. */
    private LightSource[] filteredSnapshot = new LightSource[0];
    private final Vector3f thingPosScratch = new Vector3f();

    public LightRules() {
        this(IFogOfWarInformation.ALWAYS_VISIBLE);
    }

    public LightRules(IFogOfWarInformation fogOfWar) {
        this.sceneLights = new SceneLights();
        this.fogOfWar = fogOfWar;
        this.sceneLights.setFogOfWar(fogOfWar);
    }

    public IFogOfWarInformation getFogOfWar() {
        return fogOfWar;
    }

    /**
     * Current publication phase. Prefer {@link #isReady()} at call sites.
     *
     * @return lifecycle phase
     */
    public Phase getPhase() {
        return phase;
    }

    /**
     * {@code true} once {@link #publishReady()} has run and before
     * {@link #dispose()}. Safe to call from any thread.
     *
     * @return whether per-frame lighting consumers may use this instance
     */
    public boolean isReady() {
        return phase == Phase.READY;
    }

    /**
     * Publishes load-time registries for consumption. Call exactly once after
     * the last load-time mutation (same critical section as any external
     * "map loaded" flag). Idempotent while still {@link Phase#LOADING} or
     * already {@link Phase#READY}; ignored after {@link #dispose()}.
     */
    public void publishReady() {
        Phase current = phase;
        if (current == Phase.LOADING) {
            phase = Phase.READY;
        }
    }

    /**
     * Ends the lifecycle. Further {@link #update}, flat-light samples and
     * settings flips become no-ops. Idempotent.
     */
    public void dispose() {
        phase = Phase.DISPOSED;
    }

    /**
     * Number of lights written into {@code LightData} by the last
     * {@link SceneLights#update} call.
     *
     * @return uploaded light count
     */
    public int getUploadedLightCount() {
        return sceneLights.getUploadedLightCount();
    }

    /**
     * Live light sources currently owned by the lighting system.
     *
     * @return source count
     */
    public int getSourceCount() {
        return modulators.size();
    }

    /**
     * Reseeds presentation flicker RNG for reproducible benchmarks.
     *
     * @param seed flicker seed
     */
    public void setModulatorSeed(long seed) {
        modulators.setSeed(seed);
    }

    /**
     * Flips {@code flickers} on every currently-live flicker-capable
     * source when the cheap-lighting setting changes.
     * Newly-created torches/things pick up the current setting directly
     * from the current setting at creation time.
     */
    public void setCheapLighting(boolean cheapLighting) {
        if (phase == Phase.DISPOSED) {
            return;
        }
        if (phase == Phase.LOADING) {
            // Flag only: new sources pick it up at create. Do not iterate
            // registries — that would race a concurrent loader thread.
            this.cheapLighting = cheapLighting;
            return;
        }
        if (this.cheapLighting == cheapLighting) {
            return;
        }
        this.cheapLighting = cheapLighting;
        for (List<LightSource> lights : tileLights.values()) {
            for (LightSource light : lights) {
                light.applyCheapLighting(cheapLighting);
            }
        }
        for (LightSource light : roomLights.values()) {
            light.applyCheapLighting(cheapLighting);
        }
        for (LightSource light : thingLights.values()) {
            light.applyCheapLighting(cheapLighting);
        }
    }

    /**
     * Advances every source's modulators and re-uploads the affected
     * terrain materials' light arrays. Call once per frame (or per
     * simulation tick for {@code tpf} accounting -- {@link LightModulators}
     * does its own fixed-step accumulation either way). {@code camera} is
     * used to cull the upload pass to tiles/regions near the view frustum;
     * may be {@code null} (then nothing is culled).
     *
     * @param tpf frame delta
     * @param camera the camera whose frustum gates the per-frame upload
     */
    public void update(float tpf, Camera camera) {
        // No-op until publishReady(): avoids racing the loader's registry and
        // terrain-page mutations (ConcurrentModificationException / torn GL state).
        if (phase != Phase.READY) {
            return;
        }
        modulators.update(tpf);
        int n = modulators.captureSnapshot();
        LightSource[] snap = modulators.currentSnapshot();
        Set<LightSource.Group> disabled = disabledGroups;
        if (disabled.isEmpty()) {
            sceneLights.update(snap, n, camera);
            return;
        }
        if (filteredSnapshot.length < n) {
            filteredSnapshot = new LightSource[Math.max(n, filteredSnapshot.length == 0 ? 256 : filteredSnapshot.length * 2)];
        }
        int kept = 0;
        for (int i = 0; i < n; i++) {
            LightSource light = snap[i];
            if (light != null && !disabled.contains(LightSource.Group.of(light))) {
                filteredSnapshot[kept++] = light;
            }
        }
        sceneLights.update(filteredSnapshot, kept, camera);
    }

    /**
     * @see SceneLights#clearTerrainPages()
     */
    public void clearTerrainPages() {
        sceneLights.clearTerrainPages();
    }

    /**
     * @see SceneLights#setTerrainPage(int, float, float, float, float, float, float, List)
     */
    public void setTerrainPage(int pageIndex, float minX, float maxX,
            float minY, float maxY, float minZ, float maxZ, List<Material> materials) {
        sceneLights.setTerrainPage(pageIndex, minX, maxX, minY, maxY, minZ, maxZ, materials);
    }

    /**
     * @see SceneLights#getPageLightIndexAvg()
     */
    public double getPageLightIndexAvg() {
        return sceneLights.getPageLightIndexAvg();
    }

    /**
     * @see SceneLights#getPageLightIndexMax()
     */
    public int getPageLightIndexMax() {
        return sceneLights.getPageLightIndexMax();
    }

    /**
     * @see SceneLights#getPageFullListFallbacks()
     */
    public int getPageFullListFallbacks() {
        return sceneLights.getPageFullListFallbacks();
    }

    /**
     * @see SceneLights#getTerrainPageCount()
     */
    public int getTerrainPageCount() {
        return sceneLights.getTerrainPageCount();
    }

    /**
     * Switches a whole group of lights on or off, e.g. for debugging. The
     * lights keep being simulated, they just stop contributing.
     */
    public void setGroupEnabled(LightSource.Group group, boolean enabled) {
        EnumSet<LightSource.Group> copy = EnumSet.copyOf(disabledGroups);
        if (enabled ? copy.remove(group) : copy.add(group)) {
            disabledGroups = copy;
        }
    }

    public boolean isGroupEnabled(LightSource.Group group) {
        return !disabledGroups.contains(group);
    }

    private List<LightSource> activeSources() {
        int n = modulators.captureSnapshot();
        LightSource[] snap = modulators.currentSnapshot();
        Set<LightSource.Group> disabled = disabledGroups;
        List<LightSource> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            LightSource light = snap[i];
            if (light != null && (disabled.isEmpty() || !disabled.contains(LightSource.Group.of(light)))) {
                result.add(light);
            }
        }
        return result;
    }

    /**
     * @see SceneLights#initAmbientMap(int, int)
     */
    public void initAmbientMap(int width, int height) {
        sceneLights.initAmbientMap(width, height);
    }

    /**
     * @see SceneLights#hasAmbientMap()
     */
    public boolean hasAmbientMap() {
        return sceneLights.hasAmbientMap();
    }

    /**
     * @see SceneLights#setTileAmbient(int, int, ColorRGBA, boolean)
     */
    public void setTileAmbient(int x, int y, ColorRGBA ambient, boolean explored) {
        sceneLights.setTileAmbient(x, y, ambient, explored);
    }

    public void setGlobalAmbient(ColorRGBA globalAmbient) {
        sceneLights.setGlobalAmbient(globalAmbient);
    }

    public ColorRGBA getGlobalAmbient() {
        return sceneLights.getGlobalAmbient();
    }

    /**
     * @see SceneLights#bindMaterials(List)
     */
    public void bindMaterials(List<Material> materials) {
        sceneLights.bindMaterials(materials);
    }

    /**
     * The "flat" sample for a thing/effect's material. Callers own re-deriving {@code worldPos} each frame
     * from the owner's current position + light offset. Returns global ambient
     * only when not yet {@link #isReady()} (safe during load).
     */
    public ColorRGBA computeFlatLight(Vector3f worldPos) {
        if (phase != Phase.READY) {
            return sceneLights.getGlobalAmbient().clone();
        }
        return SceneLights.computeFlatLight(worldPos, activeSources());
    }

    /**
     * As {@link #computeFlatLight(Vector3f)}, optionally leaving out the
     * lights carried by game objects
     */
    public ColorRGBA computeFlatLight(Vector3f worldPos, boolean ignoreObjectLights) {
        if (phase != Phase.READY) {
            return sceneLights.getGlobalAmbient().clone();
        }
        return SceneLights.computeFlatLight(worldPos, activeSources(), ignoreObjectLights);
    }

    // ------------------------------------------------------------------
    // Tile-owned lights: torches, terrain lights
    // ------------------------------------------------------------------

    /**
     * Removes every torch/terrain light owned by {@code tile} -- called
     * before a tile is rebuilt, mirroring how the mesh builder replaces
     * the whole tile subtree.
     */
    public void clearTileLights(Point tile) {
        List<LightSource> removed = tileLights.remove(tile);
        if (removed != null) {
            for (LightSource light : removed) {
                modulators.remove(light);
            }
        }
    }

    public LightSource addTorchLight(Point owner, Vector3f position,
            int red, int green, int blue, float intensity, float radius) {
        LightSource light = new LightSource(LightSource.Kind.TORCH, owner, position,
                red, green, blue, intensity, radius);
        light.setFlickerCapable(true);
        light.applyCheapLighting(cheapLighting);
        addTileLight(owner, light);
        return light;
    }

    public LightSource addTerrainLight(Point owner, Vector3f position,
            int red, int green, int blue) {
        return addTerrainLight(owner, position, red, green, blue, LightConstants.TERRAIN_LIGHT_INTENSITY);
    }

    public LightSource addTerrainLight(Point owner, Vector3f position,
            int red, int green, int blue, float intensity) {
        LightSource light = new LightSource(LightSource.Kind.TERRAIN, owner, position,
                red, green, blue, intensity,
                LightConstants.TERRAIN_LIGHT_RADIUS_TILES);
        addTileLight(owner, light);
        return light;
    }

    /**
     * The per-terrain {@code Light} block, one source per
     * slab. Unlike thing lights the file radius is <em>not</em> scaled by 2/3;
     * {@code PULSE}/{@code FLICKER} are honoured. {@code position} already
     * includes the block's {@code mKPos} offset.
     */
    public LightSource addSlabLight(Point owner, Vector3f position,
            int red, int green, int blue, float fileRadius, boolean pulse, boolean flicker) {
        LightSource light = new LightSource(LightSource.Kind.SLAB, owner, position,
                red, green, blue, LightConstants.SLAB_LIGHT_INTENSITY, fileRadius);
        if (pulse) {
            light.startPulse();
        }
        light.setFlickerCapable(flicker);
        light.applyCheapLighting(cheapLighting);
        addTileLight(owner, light);
        return light;
    }

    private void addTileLight(Point owner, LightSource light) {
        tileLights.computeIfAbsent(owner, k -> new ArrayList<>()).add(light);
        modulators.add(light);
    }

    // ------------------------------------------------------------------
    // Room-owned lights: the portal light
    // ------------------------------------------------------------------

    public LightSource addPortalLight(Object room, Vector3f position) {
        LightSource light = new LightSource(LightSource.Kind.PORTAL, room, position,
                LightConstants.PORTAL_LIGHT_RED, LightConstants.PORTAL_LIGHT_GREEN,
                LightConstants.PORTAL_LIGHT_BLUE, LightConstants.PORTAL_LIGHT_INTENSITY,
                LightConstants.PORTAL_LIGHT_RADIUS_TILES);
        light.startPulse();
        roomLights.put(room, light);
        modulators.add(light);
        return light;
    }

    public void removeRoomLight(Object room) {
        LightSource removed = roomLights.remove(room);
        if (removed != null) {
            modulators.remove(removed);
        }
    }

    // ------------------------------------------------------------------
    // Thing-owned lights: objects/creatures/shots/effects
    // ------------------------------------------------------------------

    /**
     * Creates the light of an object, creature or effect from its data
     * {@code Light} block.
     *
     * @param ownerId the owning player, used when the light is
     * {@code PLAYER_COLOURED}; may be {@code null}
     * @param honourPulse whether the block's {@code PULSE}/{@code FLICKER}
     * flags are honoured
     * @see #addThingLight(Object, Vector3f, Vector3f, int, int, int, boolean, boolean, boolean, boolean, int[], float, float, boolean, boolean)
     */
    public LightSource addThingLight(Object owner, Vector3f ownerPosition, Light light, Short ownerId,
            float intensity, boolean honourPulse) {
        return addThingLight(owner, ownerPosition, light, ownerId, intensity, honourPulse, true);
    }

    /**
     * @param applyPlayerColour false ignores the block's {@code PLAYER_COLOURED}
     * flag, i.e. the light of a creature the player is possessing
     */
    public LightSource addThingLight(Object owner, Vector3f ownerPosition, Light light, Short ownerId,
            float intensity, boolean honourPulse, boolean applyPlayerColour) {
        toniarts.openkeeper.utils.Color c = light.getColor();
        Set<Light.LightFlag> flags = light.getFlags();
        boolean playerColoured = applyPlayerColour && flags.contains(Light.LightFlag.PLAYER_COLOURED);
        return addThingLight(owner, ownerPosition, LightConstants.toJmeOffset(light.getmKPos()),
                c.getRed(), c.getGreen(), c.getBlue(),
                flags.contains(Light.LightFlag.COLOR_RED),
                flags.contains(Light.LightFlag.COLOR_GREEN),
                flags.contains(Light.LightFlag.COLOR_BLUE),
                playerColoured, playerColoured ? LightConstants.getPlayerLightColour(ownerId) : null,
                light.getRadius(), intensity,
                honourPulse && flags.contains(Light.LightFlag.PULSE),
                honourPulse && flags.contains(Light.LightFlag.FLICKER));
    }

    /**
     * The thing-light formula. {@code offset} is the
     * {@code Light} struct's {@code mKPos}; the caller re-derives
     * {@code ownerPosition} each frame and calls this again (or moves the
     * returned source directly).
     */
    public LightSource addThingLight(Object owner, Vector3f ownerPosition, Vector3f offset,
            int red, int green, int blue, boolean colorRed, boolean colorGreen, boolean colorBlue,
            boolean playerColoured, int[] playerColour, float fileRadius, float intensity,
            boolean pulse, boolean flicker) {
        int r = LightConstants.withBonus(red, colorRed, LightConstants.OVER_BRIGHT_BONUS);
        int g = LightConstants.withBonus(green, colorGreen, LightConstants.OVER_BRIGHT_BONUS);
        int b = LightConstants.withBonus(blue, colorBlue, LightConstants.OVER_BRIGHT_BONUS);
        if (playerColoured && playerColour != null) {
            r = Math.max(LightConstants.PLAYER_COLOURED_MIN, r * playerColour[0] / 255);
            g = Math.max(LightConstants.PLAYER_COLOURED_MIN, g * playerColour[1] / 255);
            b = Math.max(LightConstants.PLAYER_COLOURED_MIN, b * playerColour[2] / 255);
        }
        float radius = fileRadius * LightConstants.THING_LIGHT_RADIUS_SCALE;
        if (playerColoured && playerColour != null) {
            radius = Math.min(radius, LightConstants.PLAYER_COLOURED_LIGHT_MAX_RADIUS_TILES);
        }
        // Scratch is copied inside LightSource — safe across creates
        thingPosScratch.set(ownerPosition).addLocal(offset);

        LightSource light = new LightSource(LightSource.Kind.THING, owner, thingPosScratch,
                r, g, b, intensity, radius);
        if (pulse) {
            light.startPulse();
        }
        light.setFlickerCapable(flicker);
        light.applyCheapLighting(cheapLighting);

        LightSource previous = thingLights.put(owner, light);
        if (previous != null) {
            modulators.remove(previous);
        }
        modulators.add(light);
        return light;
    }

    /**
     * Re-derives an already-created thing light's position from the
     * owner's current position + light offset, without
     * touching its modulator/flicker state. No-op if {@code owner} has no
     * light.
     */
    public void updateThingLightPosition(Object owner, Vector3f ownerPosition, Vector3f offset) {
        LightSource light = thingLights.get(owner);
        if (light != null) {
            // Mutate in place — avoids a Vector3f alloc every frame per moving light
            light.getPosition().set(ownerPosition).addLocal(offset);
        }
    }

    public void removeThingLight(Object owner) {
        LightSource removed = thingLights.remove(owner);
        if (removed != null) {
            modulators.remove(removed);
        }
    }

    public LightSource getThingLight(Object owner) {
        return thingLights.get(owner);
    }

    // ------------------------------------------------------------------
    // The cursor light
    // ------------------------------------------------------------------

    public void createCursorLight() {
        cursorLight = new LightSource(LightSource.Kind.CURSOR, null, Vector3f.ZERO,
                LightConstants.MAX_COLOUR, LightConstants.MAX_COLOUR, LightConstants.MAX_COLOUR, LightConstants.CURSOR_LIGHT_INTENSITY,
                LightConstants.CURSOR_LIGHT_RADIUS_TILES);
        modulators.add(cursorLight);
    }

    public void destroyCursorLight() {
        if (cursorLight != null) {
            modulators.remove(cursorLight);
            cursorLight = null;
        }
    }

    /**
     * {@code visible = false} hides the light entirely
     * (build mode, cursor not over the world, off-map); otherwise it moves
     * to {@code position} and is white if {@code explored}, else black
     * (still lit, not hidden, which differs from
     * "invisible"). Deviation from the original's tile-centred
     * {@code cursorTile.x + 0.5}: the caller passes the continuous cursor
     * point, so the glow glides instead of snapping between tiles. The
     * caller also picks the height, and so must cast the mouse ray to that
     * height or the light lands off-centre of the pointer.
     */
    public void updateCursorLight(boolean visible, Vector3f position, boolean explored) {
        if (cursorLight == null) {
            return;
        }
        cursorLight.setEnabled(visible);
        if (!visible) {
            return;
        }
        cursorLight.setPosition(position);
        int value = explored ? LightConstants.MAX_COLOUR : 0;
        cursorLight.setBaseColour(value, value, value);
    }
}
