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

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingVolume;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.lighting.LightSource;

/**
 * Real time shadows of creatures and objects. They are not shadow maps or
 * volumes but projected blobs: the silhouette of the caster is rasterised
 * into a small coverage texture, which is drawn on a flat quad on the floor
 * under the caster, darkening whatever is beneath.
 * <p>
 * A caster gets up to three shadows, one per light, from the nearest lights
 * that cast (torches, the hand, object and effect lights; the glow of the
 * terrain does not). The direction, length, tilt and darkness of a shadow
 * come from the horizontal distance and direction between the light and the
 * caster alone; the light's height, colour and flicker play no part.
 * <p>
 * The quality level decides how the silhouette is made: 1 a footprint seen
 * from straight above, built once per model and light direction and so not
 * animated; 2 the same, rebuilt every frame; 3 seen along the light, so the
 * further away the light, the more from the side.
 * <p>
 * Call {@link #render} once per frame, before the scene graph's geometric
 * update.
 */
public final class ShadowRenderer {

    /**
     * Something that casts a shadow
     *
     * @param spatial the model
     * @param ownLight the light the caster carries itself, which never
     * shadows it; may be {@code null}
     */
    public record Caster(Spatial spatial, LightSource ownLight) {

    }

    /**
     * A light that shadows a caster, and its 3D distance to it
     */
    record Candidate(LightSource light, float distance) {

    }

    /**
     * @param chosen the nearest lights, at most {@link #MAX_SHADOWS}
     * @param fadeRange the range the shadows fade over: the global one, or
     * the distance of the fourth nearest light if there is one, so the
     * farthest shadow has faded when another light takes its place
     */
    record Selection(List<Candidate> chosen, float fadeRange) {

    }

    private static final int MAX_SHADOWS = 3;
    private static final float MAX_RANGE = 2.5f;
    private static final int NEIGHBOURHOOD = 3; // Tiles looked at for lights, each way
    private static final float MAX_ALPHA = 0.7f;
    private static final float FADE_SLOPE = 1.5f;

    /**
     * The tilt from straight down of the view per tile of distance between
     * light and caster: 67.5 degrees at the full range
     */
    private static final float TILT_PER_TILE = 0.4712f;
    private static final int FROZEN_HEADING_STEPS = 16;
    private static final float FLOOR_PLANE = WorldUtils.FLOOR_HEIGHT + 0.01f;

    private final AssetManager assetManager;
    private final Node node = new Node("Shadows");
    private final Mesh quadMesh = createQuadMesh();
    private final Silhouette silhouette = new Silhouette();
    private final List<ShadowQuad> quads = new ArrayList<>();
    private final Map<Long, List<LightSource>> lightsByTile = new HashMap<>();
    private final Map<String, Silhouette> frozenSilhouettes = new HashMap<>();
    private final Quaternion rotation = new Quaternion();
    private float range = MAX_RANGE;
    private int usedQuads;

    public ShadowRenderer(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public void attachTo(Node parent) {
        parent.attachChild(node);
    }

    public void detach() {
        node.removeFromParent();
    }

    /**
     * The global shadow range, a slow level of detail control driven by the
     * frame rate: at 20 fps and above it is at its cap, at 10 and below the
     * shadows are gone. Call once per frame.
     */
    public void updateRange(float tpf) {
        if (tpf <= 0f) {
            return;
        }
        float target = (1f / tpf - 10f) * 0.25f;
        if (target < range - 0.5f) {
            range -= 0.1f;
        } else if (target > range + 0.5f) {
            range += 0.1f;
        }
        range = Math.clamp(range, 0f, MAX_RANGE);
    }

    float getRange() {
        return range;
    }

    /**
     * Draws this frame's shadows.
     *
     * @param level the shadow quality, 0 is off
     * @param lights all the lights of the scene
     * @param casters the things that cast a shadow right now
     * @param camera the camera, to leave out what is not seen; may be
     * {@code null}
     */
    public void render(int level, List<LightSource> lights, List<Caster> casters, Camera camera) {
        usedQuads = 0;
        if (level > 0 && range > 0f && !lights.isEmpty()) {
            sortLightsByTile(lights);
            for (Caster caster : casters) {
                renderCaster(Math.min(level, 3), caster, camera);
            }
        }
        for (int i = usedQuads; i < quads.size(); i++) {
            quads.get(i).geometry.setCullHint(Spatial.CullHint.Always);
        }
    }

    private void renderCaster(int level, Caster caster, Camera camera) {
        Spatial spatial = caster.spatial();
        BoundingVolume bound = spatial.getWorldBound();
        if (camera != null && bound != null && camera.contains(bound) == Camera.FrustumIntersect.Outside
                || !silhouette.collect(spatial)) {
            return;
        }
        Vector3f centre = silhouette.getCentre();
        float radius = silhouette.getRadius();
        Vector3f position = spatial.getWorldTranslation();
        Selection selection = select((int) Math.floor(position.x), (int) Math.floor(position.z), centre, caster.ownLight());

        for (Candidate candidate : selection.chosen()) {
            Vector3f lightPosition = candidate.light().getPosition();
            float dx = centre.x - lightPosition.x;
            float dz = centre.z - lightPosition.z;
            float distance = (float) Math.sqrt(dx * dx + dz * dz);
            float alpha = opacity(selection.fadeRange(), distance);
            if (alpha <= 0f) {
                continue;
            }

            // Straight under the light there is no direction, any will do
            float ux = distance > 1e-4f ? dx / distance : 1f;
            float uz = distance > 1e-4f ? dz / distance : 0f;
            if (level == 1) {
                drawFrozen(spatial, ux, uz);
            } else {
                silhouette.draw(ux, uz, level >= 3 ? distance * TILT_PER_TILE : 0f);
            }

            float[] extent = extent(level, distance, centre.y - FLOOR_PLANE, radius);
            rotation.fromAxes(new Vector3f(ux, 0f, uz), Vector3f.UNIT_Y, new Vector3f(-uz, 0f, ux));
            acquireQuad().show(silhouette, centre.x + ux * extent[0], centre.z + uz * extent[0],
                    rotation, extent[1] - extent[0], radius, alpha);
        }
    }

    /**
     * Where a shadow quad lies along the light direction, measured from the
     * caster's foot point.
     *
     * @param distance the horizontal distance between the light and the caster
     * @param height the caster's centre above the shadow plane
     * @param radius the caster's bounding radius
     * @return near and far
     */
    static float[] extent(int level, float distance, float height, float radius) {
        if (level < 3) {
            // A point light at a virtual height of 2, whatever the real one's
            float half = distance / 2f;
            return new float[]{half * (height - radius), half * (height + radius)};
        }

        // The image rows (along the tilted view) map linearly to the floor
        // along the light direction: row / cos(theta) + height * tan(theta)
        float theta = distance * TILT_PER_TILE;
        float shift = height * (float) Math.tan(theta);
        float reach = radius / (float) Math.cos(theta);
        return new float[]{shift - reach, shift + reach};
    }

    /**
     * Level 1: the footprint is built once, in the pose the model is first
     * seen in, and shared by all models of the name. It depends on the
     * direction of the light relative to the model's heading, in steps.
     */
    private void drawFrozen(Spatial spatial, float ux, float uz) {
        float heading = spatial.getWorldRotation().toAngles(null)[1];
        float step = FastMath.TWO_PI / FROZEN_HEADING_STEPS;
        int bucket = Math.floorMod(Math.round((FastMath.atan2(uz, ux) - heading) / step), FROZEN_HEADING_STEPS);
        String key = spatial.getName() + '#' + bucket;

        Silhouette frozen = frozenSilhouettes.get(key);
        if (frozen == null) {
            float angle = heading + bucket * step;
            silhouette.draw(FastMath.cos(angle), FastMath.sin(angle), 0f);
            frozen = new Silhouette();
            frozen.copyCoverageFrom(silhouette);
            frozenSilhouettes.put(key, frozen);
        } else {
            silhouette.copyCoverageFrom(frozen);
        }
    }

    /**
     * The opacity of a shadow: full strength up to a bit over half of the
     * fade range, then falling linearly to nothing at it.
     *
     * @param fadeRange the range the caster's shadows fade over
     * @param distance the horizontal distance between the light and the caster
     * @return the opacity, 0 or less if there is nothing to draw
     */
    static float opacity(float fadeRange, float distance) {
        if (fadeRange <= 0f) {
            return 0f;
        }
        return Math.min(MAX_ALPHA, FADE_SLOPE * (fadeRange - distance) / fadeRange);
    }

    void sortLightsByTile(List<LightSource> lights) {
        lightsByTile.clear();
        for (LightSource light : lights) {
            if (light.isEnabled() && castsShadows(light)) {
                Vector3f position = light.getPosition();
                lightsByTile.computeIfAbsent(tileKey((int) Math.floor(position.x), (int) Math.floor(position.z)),
                        k -> new ArrayList<>()).add(light);
            }
        }
    }

    /**
     * Whether a light casts shadows: torches and the lights that move or come
     * and go (objects, the hand, effects). The glow of the terrain lights the
     * world but never casts.
     */
    static boolean castsShadows(LightSource light) {
        return switch (light.getKind()) {
            case TORCH, THING, CURSOR ->
                true;
            case TERRAIN, SLAB, PORTAL ->
                false;
        };
    }

    /**
     * Picks the lights that shadow a caster. A light reaches the tiles whose
     * centre is within its radius plus one tile, and the caster if it is
     * within the global range.
     *
     * @param tileX the tile the caster stands on
     * @param centre the centre of the caster
     * @param ownLight the caster's own light, which never shadows it
     */
    Selection select(int tileX, int tileZ, Vector3f centre, LightSource ownLight) {
        float tileCentreX = tileX + 0.5f;
        float tileCentreZ = tileZ + 0.5f;
        List<Candidate> eligible = new ArrayList<>();
        for (int x = tileX - NEIGHBOURHOOD; x <= tileX + NEIGHBOURHOOD; x++) {
            for (int z = tileZ - NEIGHBOURHOOD; z <= tileZ + NEIGHBOURHOOD; z++) {
                for (LightSource light : lightsByTile.getOrDefault(tileKey(x, z), List.of())) {
                    Vector3f position = light.getPosition();
                    float reach = light.getRadius() + 1f;
                    float dx = position.x - tileCentreX;
                    float dz = position.z - tileCentreZ;
                    float distance = position.distance(centre);
                    if (light != ownLight && dx * dx + dz * dz < reach * reach && distance < range) {
                        eligible.add(new Candidate(light, distance));
                    }
                }
            }
        }
        eligible.sort(Comparator.comparingDouble(Candidate::distance));
        if (eligible.size() <= MAX_SHADOWS) {
            return new Selection(eligible, range);
        }
        return new Selection(eligible.subList(0, MAX_SHADOWS), eligible.get(MAX_SHADOWS).distance());
    }

    private static long tileKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private ShadowQuad acquireQuad() {
        if (usedQuads == quads.size()) {
            ShadowQuad quad = new ShadowQuad();
            quads.add(quad);
            node.attachChild(quad.geometry);
        }
        return quads.get(usedQuads++);
    }

    /**
     * A unit quad lying flat: x runs along the light direction from 0 to 1, z
     * across it from -1 to 1. The texture's columns run across and its rows
     * along.
     */
    private static Mesh createQuadMesh() {
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(
                0, 0, -1,
                1, 0, -1,
                1, 0, 1,
                0, 0, 1));
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(
                0, 0,
                0, 1,
                1, 1,
                1, 0));
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(
                0, 1, 0,
                0, 1, 0,
                0, 1, 0,
                0, 1, 0));
        mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(0, 2, 1, 0, 3, 2));
        mesh.updateBound();
        return mesh;
    }

    /**
     * One shadow: a flat quad with its own coverage texture
     */
    private final class ShadowQuad {

        private final Geometry geometry = new Geometry("Shadow", quadMesh);
        private final Material material;
        private final ByteBuffer pixels = BufferUtils.createByteBuffer(Silhouette.SIZE * Silhouette.SIZE * 4);
        private final Image image = new Image(Image.Format.RGBA8, Silhouette.SIZE, Silhouette.SIZE, pixels, ColorSpace.Linear);
        private final ColorRGBA colour = new ColorRGBA(0f, 0f, 0f, 0f);

        ShadowQuad() {
            Texture2D texture = new Texture2D(image);
            texture.setMagFilter(Texture.MagFilter.Bilinear);
            texture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
            texture.setWrap(Texture.WrapMode.EdgeClamp);

            // Black, with the coverage times the opacity as alpha: the floor
            // behind is multiplied by 1 - coverage * opacity
            material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            material.setTexture("ColorMap", texture);
            RenderState state = material.getAdditionalRenderState();
            state.setBlendMode(RenderState.BlendMode.Alpha);
            state.setDepthWrite(false);
            state.setFaceCullMode(RenderState.FaceCullMode.Off);
            state.setPolyOffset(-1f, -1f);

            geometry.setMaterial(material);
            geometry.setQueueBucket(RenderQueue.Bucket.Transparent);
        }

        void show(Silhouette silhouette, float x, float z, Quaternion rotation, float length, float halfWidth, float alpha) {
            silhouette.writeTo(pixels);
            image.setUpdateNeeded();
            colour.a = alpha;
            material.setColor("Color", colour);
            geometry.setLocalTranslation(x, FLOOR_PLANE, z);
            geometry.setLocalRotation(rotation);
            geometry.setLocalScale(length, 1f, halfWidth);
            geometry.setCullHint(Spatial.CullHint.Dynamic);
        }
    }
}
