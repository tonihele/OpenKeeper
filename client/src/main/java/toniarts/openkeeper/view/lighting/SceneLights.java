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
import com.jme3.math.Plane;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/**
 * Turns the current set of {@link LightSource}s into what the two
 * {@code DkLighting} material techniques (the two evaluation
 * modes) actually consume:
 * <ul>
 * <li><b>Per-vertex terrain</b> ({@code Vertex.j3md}): one shared list of
 * every light near the camera, written once per frame into a data texture
 * ({@code LightData}) that every terrain material reads. All materials see
 * exactly the same lights, so neighbouring tiles, room meshes and page
 * batches can never disagree about a light that reaches their shared edge
 * (per-material light lists would cut hard at tile borders). The
 * shader evaluates the model exactly, per vertex; there is no cap on
 * the number of lights beyond the texture size ({@link #MAX_LIGHTS}).</li>
 * <li><b>Flat things</b> ({@code Flat.j3md}): {@link #computeFlatLight}
 * is a pure function callers (entity containers, {@code VisualEffect})
 * invoke themselves each frame with the owner's current position -- this class does not track thing positions.</li>
 * </ul>
 * This is the only place that knows how sources get turned into pixels.
 */
public final class SceneLights {

    private static final Logger logger = Logger.getLogger(SceneLights.class.getName());

    /**
     * Most lights the data texture holds; must match {@code DK_MAX_LIGHTS} in
     * {@code Vertex.vert}.
     */
    public static final int MAX_LIGHTS = 3084;

    /**
     * Layout of {@code LightData}: texel 0 is a header (x = light count,
     * yzw = global ambient), then three texels per light: (x, y, z, radius),
w     * (r, g, b, intensity), (flags, 0, 0, 0). One row, RGBA32F. The flag bits
     * are described in {@code Vertex.vert}.
     */
    private static final int TEXELS_PER_LIGHT = 3;

    /**
     * How far outside the camera's view a light is still uploaded, on top of
     * its own reach, so lights just off-screen are already there when the
     * camera swings round and tall walls near the edge are lit correctly.
     */
    private static final float FRUSTUM_MARGIN_TILES = 5f;

    private ColorRGBA globalAmbient = ColorRGBA.Black.clone();
    private IFogOfWarInformation fogOfWar = IFogOfWarInformation.ALWAYS_VISIBLE;
    private final Texture2D lightData;
    private final FloatBuffer lightFloats;
    private boolean warnedAboutLightCount;
    private volatile Texture2D ambientMap;
    private int ambientMapWidth;
    private int ambientMapHeight;

    public SceneLights() {
        int width = 1 + TEXELS_PER_LIGHT * MAX_LIGHTS;
        ByteBuffer data = BufferUtils.createByteBuffer(width * 4 * Float.BYTES);
        lightFloats = data.asFloatBuffer();
        Image image = new Image(Image.Format.RGBA32F, width, 1, data, ColorSpace.Linear);
        lightData = new Texture2D(image);
        lightData.setMagFilter(Texture.MagFilter.Nearest);
        lightData.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        lightData.setWrap(Texture.WrapMode.EdgeClamp);
    }

    /**
     * The viewer's fog. The shader hides coloured light over unexplored
     * ground (a tile's explored flag is in the ambient texture); this is only
     * used to mark which lights are exempt: terrain lights owned by an
     * unexplored tile, i.e. the claim-scan glow.
     */
    public void setFogOfWar(IFogOfWarInformation fogOfWar) {
        this.fogOfWar = fogOfWar;
    }

    /**
     * The video "ambient light" setting. It travels in the
     * light data header, and is available to callers of {@link
     * #computeFlatLight} for their own {@code Flat} materials.
     */
    public void setGlobalAmbient(ColorRGBA globalAmbient) {
        this.globalAmbient = globalAmbient;
    }

    public ColorRGBA getGlobalAmbient() {
        return globalAmbient;
    }

    /**
     * Hooks Vertex materials up to the shared light and ambient textures.
     * Nothing else is needed: they are lit from then on by {@link #update}, and
     * a dropped material is simply no longer rendered, so there is nothing to
     * undo. Binding the same material again is harmless.
     */
    public void bindMaterials(List<Material> materials) {
        for (Material material : materials) {
            if (material.getTextureParam("LightData") == null) {
                material.setTexture("LightData", lightData);
            }
            bindAmbientMap(material);
        }
    }

    /**
     * Writes this frame's lights into the shared data texture. Call once per
     * frame (see {@link LightRules#update(float, Camera)}) -- flicker, pulse,
     * fade and moving creatures mean this can't be a one-shot upload.
     * Lights well outside the camera's view are left out (an optimisation
     * only: they cannot reach anything on screen).
     */
    public void update(List<LightSource> lights, Camera camera) {
        Plane[] frustum = camera != null ? getFrustumPlanes(camera) : null;

        int count = 0;
        for (LightSource light : lights) {
            if (!light.isEnabled() || hiddenByFog(light)) {
                continue;
            }
            Vector3f pos = light.getPosition();
            float reach = light.getRadius() + LightConstants.SOFT_FALLOFF_TAIL_TILES + FRUSTUM_MARGIN_TILES;
            if (frustum != null && !reachableFromCamera(frustum, reach, pos.x, pos.z)) {
                continue;
            }
            if (count >= MAX_LIGHTS) {
                if (!warnedAboutLightCount) {
                    warnedAboutLightCount = true;
                    logger.log(Level.WARNING, "More than {0} lights near the camera, the rest are dropped", MAX_LIGHTS);
                }
                break;
            }
            writeLight(count, light);
            count++;
        }
        lightFloats.put(0, count);
        lightFloats.put(1, globalAmbient.r);
        lightFloats.put(2, globalAmbient.g);
        lightFloats.put(3, globalAmbient.b);
        lightData.getImage().setUpdateNeeded();
    }

    private static Plane[] getFrustumPlanes(Camera camera) {
        // jME 3.9 Camera folded the classic Frustum's six world planes
        // into getWorldPlane(int); the plane index constants are private
        // there, but indices 0..5 are fixed as LEFT, RIGHT, BOTTOM, TOP,
        // FAR, NEAR -- a cull test doesn't care about their order.
        Plane[] planes = new Plane[6];
        for (int i = 0; i < planes.length; i++) {
            planes[i] = camera.getWorldPlane(i);
        }
        return planes;
    }

    /**
     * The intensity a light is rendered with: its own, scaled down for creature
     * and effect lights
     */
    private static float litIntensity(LightSource light) {
        float intensity = light.getEffectiveIntensity();
        return LightSource.Group.of(light) == LightSource.Group.CREATURE ? intensity * LightConstants.CREATURE_LIGHT_STRENGTH : intensity;
    }

    /**
     * Writes one light into its three texels of the data texture, see
     * {@link #TEXELS_PER_LIGHT}.
     */
    private void writeLight(int index, LightSource light) {
        Vector3f pos = light.getPosition();
        int base = (1 + TEXELS_PER_LIGHT * index) * 4;
        lightFloats.put(base, pos.x);
        lightFloats.put(base + 1, pos.y);
        lightFloats.put(base + 2, pos.z);
        lightFloats.put(base + 3, light.getRadius());
        lightFloats.put(base + 4, light.getRed() / LightConstants.COLOUR_SCALE);
        lightFloats.put(base + 5, light.getGreen() / LightConstants.COLOUR_SCALE);
        lightFloats.put(base + 6, light.getBlue() / LightConstants.COLOUR_SCALE);
        lightFloats.put(base + 7, litIntensity(light));
        lightFloats.put(base + 8, (light.needsSmoothFalloff() ? 1 : 0)
                | (lightsUnexploredTiles(light) ? 2 : 0)
                | (light.yieldsToTorches() ? 4 : 0)
                | (light.getKind() == LightSource.Kind.TORCH ? 8 : 0)
                | (LightSource.Group.of(light) == LightSource.Group.CREATURE ? 16 : 0)
                | (light.isObjectLight() ? 32 : 0));
    }

    /**
     * Whether a point (at ground height) is inside the camera frustum
     * expanded by {@code margin} tiles on each side. The frustum planes'
     * normals point inward, so {@code distance >= -margin} keeps a point up
     * to {@code margin} outside a plane.
     */
    private static boolean reachableFromCamera(Plane[] planes, float margin, float x, float z) {
        // Only the four side planes (indices 0..3): near/far are meaningless
        // for a ground-level point seen at an oblique angle, and walls rise
        // above the point being tested.
        for (int i = 0; i < 4; i++) {
            Plane plane = planes[i];
            // pseudoDistance = normal.Dot(point) - constant, computed without
            // allocating a Vector3f per light every frame
            Vector3f normal = plane.getNormal();
            float distance = normal.x * x + normal.z * z - plane.getConstant();
            if (distance < -margin) {
                return false;
            }
        }
        return true;
    }

    /**
     * Fog gates the whole lighting pass: a tile-owned torch, terrain or slab
     * light on a tile the viewer has neither explored nor perceived is not
     * submitted at all (rather than being submitted and hidden by the shader).
     * Perceived tiles stay, which keeps the claim-scan / gold-seam glow.
     * Thing, portal and cursor lights are not tile-gated.
     */
    private boolean hiddenByFog(LightSource light) {
        return light.getKind().isTileOwned()
                && light.getOwner() instanceof Point owner
                && !fogOfWar.isExplored(owner)
                && !fogOfWar.isPerceived(owner);
    }

    /**
     * Whether a light may shine on unexplored ground: the cursor light (the
     * keeper's hand) and terrain lights owned by an unexplored tile (the
     * claim-scan sweep). Coloured light from
     * torches, creatures, effects and explored terrain must not reveal it.
     */
    private boolean lightsUnexploredTiles(LightSource light) {
        if (light.getKind() == LightSource.Kind.CURSOR) {
            return true; // the keeper's hand lights the fog it points at
        }
        return light.getKind() == LightSource.Kind.TERRAIN
                && light.getOwner() instanceof Point owner
                && !fogOfWar.isExplored(owner);
    }

    /**
     * Creates the map-sized ambient texture (one texel per tile, centre at
     * {@code tile + 0.5}). The vertex shader samples it with bilinear
     * filtering at the vertex's world position, so the per-terrain vertex
     * ambient and the explored flag fade from one tile to
     * the next instead of stepping at the tile border. Values are stored
     * halved so over-bright ambient (up to 2.0) fits 8 bits.
     */
    public void initAmbientMap(int width, int height) {
        ByteBuffer data = BufferUtils.createByteBuffer(width * height * 4);
        Image image = new Image(Image.Format.RGBA8, width, height, data, ColorSpace.Linear);
        Texture2D texture = new Texture2D(image);
        texture.setMagFilter(Texture.MagFilter.Bilinear);
        texture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        texture.setWrap(Texture.WrapMode.EdgeClamp);
        ambientMap = texture;
        ambientMapWidth = width;
        ambientMapHeight = height;
    }

    /**
     * Sets one tile's vertex ambient and explored flag in the ambient texture. Safe to call
     * from the map loader thread: it only writes bytes and flags the image
     * for re-upload, which the render thread performs.
     */
    public void setTileAmbient(int x, int y, ColorRGBA ambient, boolean explored) {
        Texture2D texture = ambientMap;
        if (texture == null || x < 0 || y < 0 || x >= ambientMapWidth || y >= ambientMapHeight) {
            return;
        }
        ByteBuffer data = texture.getImage().getData(0);
        int index = (y * ambientMapWidth + x) * 4;
        data.put(index, toByte(ambient.r / 2f));
        data.put(index + 1, toByte(ambient.g / 2f));
        data.put(index + 2, toByte(ambient.b / 2f));
        data.put(index + 3, (byte) (explored ? 255 : 0)); // explored flag, blended like the colour
        texture.getImage().setUpdateNeeded();
    }

    private static byte toByte(float value) {
        return (byte) Math.round(Math.max(0f, Math.min(1f, value)) * 255f);
    }

    private void bindAmbientMap(Material material) {
        if (ambientMap != null && material.getTextureParam("AmbientMap") == null) {
            material.setTexture("AmbientMap", ambientMap);
            material.setVector2("AmbientMapSize", new Vector2f(ambientMapWidth, ambientMapHeight));
        }
    }

    /**
     * The "flat" evaluation: one sample at
     * {@code worldPos}, normal term dropped, summed over every light
     * (there is no per-material cap here -- this is a single CPU-side
     * point sample, not a per-vertex GPU loop). Pure function; callers
     * (entity containers, {@code VisualEffect}) invoke this themselves
     * each frame with the owner's current position and
     * set the result as their material's {@code FlatLight} parameter.
     */
    public static ColorRGBA computeFlatLight(Vector3f worldPos, Iterable<LightSource> lights) {
        return computeFlatLight(worldPos, lights, false);
    }

    /**
     * @param ignoreObjectLights skip lights carried by game objects
     */
    public static ColorRGBA computeFlatLight(Vector3f worldPos, Iterable<LightSource> lights,
            boolean ignoreObjectLights) {
        float r = 0, g = 0, b = 0;
        for (LightSource light : lights) {
            if (!light.isEnabled() || (ignoreObjectLights && light.isObjectLight())) {
                continue;
            }
            Vector3f pos = light.getPosition();
            float dx = pos.x - worldPos.x;
            float dy = pos.y - worldPos.y;
            float dz = pos.z - worldPos.z;
            float d2 = dx * dx + dy * dy + dz * dz;
            float radius = light.getRadius() + LightConstants.SOFT_FALLOFF_TAIL_TILES;
            float r2 = radius * radius;
            if (r2 <= 0f || d2 >= r2) {
                continue;
            }

            float n = (float) Math.floor(16.0 * d2);
            float f = light.needsSmoothFalloff() ? dkFalloffMonotonic(n) : dkFalloff(n);
            float k = (r2 - d2) * f * (1f / r2) * litIntensity(light);
            k = Math.min(k, 1f);
            if (k <= 0f) {
                continue;
            }

            r += (light.getRed() / LightConstants.COLOUR_SCALE) * k;
            g += (light.getGreen() / LightConstants.COLOUR_SCALE) * k;
            b += (light.getBlue() / LightConstants.COLOUR_SCALE) * k;
        }
        return new ColorRGBA(r, g, b, 1f);
    }

    /**
     * Java-side twin of {@code DkFalloff.glsllib}'s {@code dkFalloff} --
     * keep the two in sync, see that file for the rationale.
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

    /**
     * Java-side twin of {@code DkFalloff.glsllib}'s {@code dkFalloffMonotonic}
     * -- keep the two in sync. Used for sources that
     * {@link LightSource#needsSmoothFalloff()} (the cursor light): drops the
     * out-of-range {@code n - 256} limb so the glow can't re-saturate into
     * the dark-gap + second full-brightness band beyond 4 tiles.
     */
    private static float dkFalloffMonotonic(float n) {
        if (n < 0.5f) {
            return 1e6f;
        }
        return 0.25f + 16f / n;
    }
}
