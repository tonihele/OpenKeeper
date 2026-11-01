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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.lighting.LightSource;

import static toniarts.openkeeper.view.shadow.ShadowConstants.SILHOUETTE_SIZE;

/**
 * Real time shadows of creatures and objects, in the manner of the original:
 * not shadow maps or volumes but projected blobs. The silhouette of the caster
 * is rasterised into a small coverage texture, which is drawn on one flat quad
 * lying on the floor under the caster, darkening whatever is beneath.
 * A caster gets up to three shadows, one per light, from the nearest lights
 * that are allowed to cast. The direction, length, tilt and darkness of a
 * shadow come from the horizontal distance and direction between the light and
 * the caster alone; the light's height, colour, intensity and flicker play no
 * part.
 * <p>
 * Call {@link #update(float)} and {@link #render} once per frame, before the
 * scene graph's geometric update.
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

    private static final float FLOOR_PLANE = WorldUtils.FLOOR_HEIGHT + ShadowConstants.FLOOR_LIFT;

    private final AssetManager assetManager;
    private final Node node = new Node("Shadows");
    private final Mesh quadMesh = createQuadMesh();
    private final ShadowRange range = new ShadowRange();
    private final TileLightSets tileLights = new TileLightSets();
    private final CasterMesh casterMesh = new CasterMesh();
    private final SilhouetteRasterizer rasterizer = new SilhouetteRasterizer();
    private final List<ShadowQuad> quads = new ArrayList<>();
    private final Map<String, SilhouetteRasterizer> frozenSilhouettes = new HashMap<>();
    private final Quaternion rotation = new Quaternion();
    private final Vector3f axisU = new Vector3f();
    private final Vector3f axisV = new Vector3f();
    private int usedQuads;

    public ShadowRenderer(AssetManager assetManager) {
        this.assetManager = assetManager;
        node.setShadowMode(RenderQueue.ShadowMode.Off);
    }

    public void attachTo(Node parent) {
        parent.attachChild(node);
    }

    public void detach() {
        node.removeFromParent();
    }

    /**
     * Updates the shadow range from the frame time
     */
    public void update(float tpf) {
        range.update(tpf);
    }

    public float getRange() {
        return range.get();
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
        if (level > 0 && range.get() > 0f) {
            tileLights.beginFrame(lights);
            for (Caster caster : casters) {
                renderCaster(Math.min(level, ShadowConstants.MAX_LEVEL), caster, camera);
            }
        }
        for (int i = usedQuads; i < quads.size(); i++) {
            quads.get(i).hide();
        }
    }

    private void renderCaster(int level, Caster caster, Camera camera) {
        Spatial spatial = caster.spatial();
        if (camera != null) {
            BoundingVolume bound = spatial.getWorldBound();
            if (bound != null && camera.contains(bound) == Camera.FrustumIntersect.Outside) {
                return;
            }
        }
        if (!casterMesh.collect(spatial)) {
            return;
        }
        Vector3f centre = casterMesh.getCentre();
        float radius = casterMesh.getRadius();

        Vector3f position = spatial.getWorldTranslation();
        List<LightSource> tileSet = tileLights.get(TileLightSets.tile(position.x), TileLightSets.tile(position.z));
        ShadowLightSelector.Selection selection = ShadowLightSelector.select(tileSet, centre, range.get(), caster.ownLight());

        float height = centre.y - FLOOR_PLANE;
        for (ShadowLightSelector.Candidate candidate : selection.chosen()) {
            Vector3f lightPosition = candidate.light().getPosition();
            float dx = centre.x - lightPosition.x;
            float dz = centre.z - lightPosition.z;
            float distance = (float) Math.sqrt(dx * dx + dz * dz);
            float alpha = ShadowMath.opacity(selection.fadeRange(), distance);
            if (alpha <= 0f) {
                continue;
            }

            // Straight under the light there is no direction, any will do
            float ux = distance > 1e-4f ? dx / distance : 1f;
            float uz = distance > 1e-4f ? dz / distance : 0f;

            ShadowQuad quad = acquireQuad();
            float theta = level >= 3 ? ShadowMath.tilt(distance) : 0f;
            if (level == 1) {
                fillFrozen(quad, caster, ux, uz);
            } else {
                rasterizer.clear();
                rasterizer.draw(casterMesh.getVertices(), casterMesh.getVertexCount(), casterMesh.getTriangles(),
                        casterMesh.getTriangleCount(), centre.x, centre.y, centre.z, radius, ux, uz, theta);
                quad.upload(rasterizer);
            }

            ShadowMath.Extent extent = ShadowMath.extent(level, distance, height, radius);
            axisU.set(ux, 0f, uz);
            axisV.set(-uz, 0f, ux);
            rotation.fromAxes(axisU, Vector3f.UNIT_Y, axisV);
            quad.place(centre.x + ux * extent.near(), centre.z + uz * extent.near(), rotation,
                    Math.max(extent.far() - extent.near(), 1e-3f), radius, alpha);
        }
    }

    /**
     * Level 1: the footprint of a model is built once, in the pose it is first
     * seen in, and shared by all its instances. It depends only on the
     * direction of the light relative to the caster's heading, in steps.
     */
    private void fillFrozen(ShadowQuad quad, Caster caster, float ux, float uz) {
        float heading = caster.spatial().getWorldRotation().toAngles(null)[1];
        float step = FastMath.TWO_PI / ShadowConstants.LEVEL_1_HEADING_STEPS;
        int bucket = Math.floorMod(Math.round((FastMath.atan2(uz, ux) - heading) / step), ShadowConstants.LEVEL_1_HEADING_STEPS);
        String key = caster.spatial().getName() + '#' + bucket;

        SilhouetteRasterizer frozen = frozenSilhouettes.get(key);
        if (frozen == null) {
            frozen = new SilhouetteRasterizer();
            float angle = heading + bucket * step;
            Vector3f centre = casterMesh.getCentre();
            frozen.draw(casterMesh.getVertices(), casterMesh.getVertexCount(), casterMesh.getTriangles(),
                    casterMesh.getTriangleCount(), centre.x, centre.y, centre.z, casterMesh.getRadius(),
                    FastMath.cos(angle), FastMath.sin(angle), 0f);
            frozenSilhouettes.put(key, frozen);
        }
        quad.upload(frozen);
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
        mesh.setStatic();
        return mesh;
    }

    /**
     * One shadow: a flat quad with its own coverage texture, as every shadow
     * differs from frame to frame
     */
    private final class ShadowQuad {

        private final Geometry geometry = new Geometry("Shadow", quadMesh);
        private final Material material;
        private final ByteBuffer pixels = BufferUtils.createByteBuffer(SILHOUETTE_SIZE * SILHOUETTE_SIZE * 4);
        private final Image image = new Image(Image.Format.RGBA8, SILHOUETTE_SIZE, SILHOUETTE_SIZE, pixels, ColorSpace.Linear);
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
            material.setColor("Color", colour);
            RenderState state = material.getAdditionalRenderState();
            state.setBlendMode(RenderState.BlendMode.Alpha);
            state.setDepthWrite(false);
            state.setFaceCullMode(RenderState.FaceCullMode.Off);
            state.setPolyOffset(-1f, -1f);

            geometry.setMaterial(material);
            geometry.setQueueBucket(RenderQueue.Bucket.Transparent);
            geometry.setShadowMode(RenderQueue.ShadowMode.Off);
            geometry.setCullHint(Spatial.CullHint.Always);
        }

        void upload(SilhouetteRasterizer silhouette) {
            silhouette.writeTo(pixels);
            image.setUpdateNeeded();
        }

        void place(float x, float z, Quaternion rotation, float length, float halfWidth, float alpha) {
            colour.a = alpha;
            material.setColor("Color", colour);
            geometry.setLocalTranslation(x, FLOOR_PLANE, z);
            geometry.setLocalRotation(rotation);
            geometry.setLocalScale(length, 1f, halfWidth);
            geometry.setCullHint(Spatial.CullHint.Dynamic);
        }

        void hide() {
            geometry.setCullHint(Spatial.CullHint.Always);
        }
    }
}
