/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.view.map;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the cave ceiling: a merged mesh of one patch per open, visible tile,
 * domed by {@link ClearanceField} so it presses down to a fixed height at a
 * wall face and rises toward open ground, ending exactly at the fog boundary
 * because only visible tiles get a patch at all.
 */
public final class Ceiling {

    /**
     * The asymptote the ceiling height rises toward as clearance grows -
     * never quite reached, even at {@link ClearanceField#MAX_CLEARANCE}. At
     * {@code clearance == 0} (a wall face) the height curve below evaluates
     * to {@code HEIGHT_APEX / 2}.
     */
    private static final float HEIGHT_APEX = 4f;

    /** A practical default that gives each tile a 4 by 4 ceiling grid. */
    public static final int DEFAULT_SUBDIVISIONS = 4;

    /** Bounds the optional quality setting; 16 is the developer-facing extreme. */
    public static final int MIN_SUBDIVISIONS = 2;
    public static final int MAX_SUBDIVISIONS = 16;

    private Ceiling() {
        // Nope
    }

    /**
     * Constructs the merged ceiling mesh for one batch of tiles that all
     * resolve to the same material.
     *
     * @param assetManager asset manager instance
     * @param clearanceField the clearance field for the whole map
     * @param terrain the terrain to draw the material's static lighting tint
     * from - see {@link MapViewController#setTerrainMaterialLighting}
     * @param ceilingResource the room's ceiling texture override, or
     * {@code null} to use the default engine ceiling texture
     * @param tiles the tile coordinates in this batch
     * @param deformer bends the ceiling together with the walls it rests on,
     * may be {@code null}
     * @return the visual representation of this batch
     */
    public static Geometry construct(AssetManager assetManager, ClearanceField clearanceField,
            Terrain terrain, ArtResource ceilingResource, List<Point> tiles, TerrainDeformer deformer) {
        return construct(assetManager, clearanceField, terrain, ceilingResource, tiles,
                deformer, DEFAULT_SUBDIVISIONS);
    }

    public static Geometry construct(AssetManager assetManager, ClearanceField clearanceField,
            Terrain terrain, ArtResource ceilingResource, List<Point> tiles,
            TerrainDeformer deformer, int subdivisions) {
        Mesh mesh = createMesh(clearanceField, tiles, deformer, subdivisions);

        Geometry geo = new Geometry("Ceiling", mesh);
        geo.setMaterial(createMaterial(assetManager, terrain, ceilingResource));
        geo.setShadowMode(RenderQueue.ShadowMode.Off);

        return geo;
    }

    /**
     * Builds the closed outer edge of the cave roof. The map is finite, so a
     * camera looking toward an exposed map edge could otherwise see around the
     * ceiling surface and into the renderer's clear color. This narrow shell
     * closes that edge in possession view; the map view culls it with the rest
     * of the ceiling.
     */
    static Geometry constructBoundary(AssetManager assetManager, Terrain terrain,
            int width, int height, TerrainDeformer deformer) {
        Geometry geometry = new Geometry("CeilingBoundary", createBoundaryMesh(width, height, deformer));
        geometry.setMaterial(createMaterial(assetManager, terrain, null));
        geometry.setShadowMode(RenderQueue.ShadowMode.Off);
        return geometry;
    }

    static Mesh createBoundaryMesh(int width, int height, TerrainDeformer deformer) {
        Mesh mesh = new Mesh();
        List<Vector3f> vertices = new ArrayList<>(8 * (width + height));
        List<Vector2f> textureCoordinates = new ArrayList<>(8 * (width + height));
        List<Vector3f> normals = new ArrayList<>(8 * (width + height));
        List<Integer> indexes = new ArrayList<>(12 * (width + height));
        Matrix3f jacobian = deformer == null ? null : new Matrix3f();
        float min = -0.5f;
        float maxY = WorldUtils.UNDERFLOOR_HEIGHT + HEIGHT_APEX;

        for (int i = 0; i < height; i++) {
            addBoundaryQuad(vertices, textureCoordinates, normals, indexes, deformer, jacobian,
                    new Vector3f(min, WorldUtils.FLOOR_HEIGHT, min + i),
                    new Vector3f(min, maxY, min + i),
                    new Vector3f(min, WorldUtils.FLOOR_HEIGHT, min + i + 1),
                    new Vector3f(min, maxY, min + i + 1),
                    new Vector3f(1, 0, 0), i, true);
            addBoundaryQuad(vertices, textureCoordinates, normals, indexes, deformer, jacobian,
                    new Vector3f(width - 0.5f, WorldUtils.FLOOR_HEIGHT, min + i),
                    new Vector3f(width - 0.5f, maxY, min + i),
                    new Vector3f(width - 0.5f, WorldUtils.FLOOR_HEIGHT, min + i + 1),
                    new Vector3f(width - 0.5f, maxY, min + i + 1),
                    new Vector3f(-1, 0, 0), i, false);
        }
        for (int i = 0; i < width; i++) {
            addBoundaryQuad(vertices, textureCoordinates, normals, indexes, deformer, jacobian,
                    new Vector3f(min + i, WorldUtils.FLOOR_HEIGHT, min),
                    new Vector3f(min + i, maxY, min),
                    new Vector3f(min + i + 1, WorldUtils.FLOOR_HEIGHT, min),
                    new Vector3f(min + i + 1, maxY, min),
                    new Vector3f(0, 0, 1), i, false);
            addBoundaryQuad(vertices, textureCoordinates, normals, indexes, deformer, jacobian,
                    new Vector3f(min + i, WorldUtils.FLOOR_HEIGHT, height - 0.5f),
                    new Vector3f(min + i, maxY, height - 0.5f),
                    new Vector3f(min + i + 1, WorldUtils.FLOOR_HEIGHT, height - 0.5f),
                    new Vector3f(min + i + 1, maxY, height - 0.5f),
                    new Vector3f(0, 0, -1), i, true);
        }

        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices.toArray(new Vector3f[0])));
        mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(textureCoordinates.toArray(new Vector2f[0])));
        mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(toIntArray(indexes)));
        mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normals.toArray(new Vector3f[0])));
        mesh.updateBound();
        return mesh;
    }

    private static void addBoundaryQuad(List<Vector3f> vertices, List<Vector2f> textureCoordinates,
            List<Vector3f> normals, List<Integer> indexes, TerrainDeformer deformer, Matrix3f jacobian,
            Vector3f bottomStart, Vector3f topStart, Vector3f bottomEnd, Vector3f topEnd,
            Vector3f normal, int horizontalTextureOffset, boolean facePositive) {
        int base = vertices.size();
        addBoundaryVertex(vertices, textureCoordinates, normals, deformer, jacobian, bottomStart, normal,
                horizontalTextureOffset, 0);
        addBoundaryVertex(vertices, textureCoordinates, normals, deformer, jacobian, topStart, normal,
                horizontalTextureOffset, HEIGHT_APEX);
        addBoundaryVertex(vertices, textureCoordinates, normals, deformer, jacobian, bottomEnd, normal,
                horizontalTextureOffset + 1, 0);
        addBoundaryVertex(vertices, textureCoordinates, normals, deformer, jacobian, topEnd, normal,
                horizontalTextureOffset + 1, HEIGHT_APEX);

        if (facePositive) {
            indexes.add(base);
            indexes.add(base + 1);
            indexes.add(base + 2);
            indexes.add(base + 1);
            indexes.add(base + 3);
            indexes.add(base + 2);
        } else {
            indexes.add(base);
            indexes.add(base + 2);
            indexes.add(base + 1);
            indexes.add(base + 1);
            indexes.add(base + 2);
            indexes.add(base + 3);
        }
    }

    private static void addBoundaryVertex(List<Vector3f> vertices, List<Vector2f> textureCoordinates,
            List<Vector3f> normals, TerrainDeformer deformer, Matrix3f jacobian,
            Vector3f position, Vector3f normal, float u, float v) {
        position.multLocal(WorldUtils.TILE_WIDTH);
        Vector3f vertexNormal = normal.clone();
        if (deformer != null) {
            deformer.deformPositionAndNormal(position, vertexNormal, jacobian);
        }
        vertices.add(position);
        textureCoordinates.add(new Vector2f(u, v));
        normals.add(vertexNormal);
    }

    private static Material createMaterial(AssetManager assetManager, Terrain terrain, ArtResource ceilingResource) {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        String textureName = (ceilingResource != null) ? ceilingResource.getName() : "Ceiling";
        TextureKey textureKey = new TextureKey(AssetUtils.getCanonicalAssetKey(
                AssetsConverter.TEXTURES_FOLDER.concat("/").concat(textureName).concat(".png")), false);
        Texture tex = assetManager.loadTexture(textureKey);
        mat.setTexture("DiffuseMap", tex);
        MapViewController.setTerrainMaterialLighting(mat, terrain);

        // The cave roof uses the same light data and per-tile ambient as other
        // cave surfaces, so torches and room lighting reach its underside.
        ColorRGBA ambient = mat.getParam("Ambient") != null
                ? (ColorRGBA) mat.getParam("Ambient").getValue() : ColorRGBA.Black;
        Material dkMat = AssetUtils.createVertexMaterial(assetManager, mat, ambient);
        return dkMat;
    }

    static Mesh createMesh(ClearanceField clearanceField, List<Point> tiles, TerrainDeformer deformer) {
        return createMesh(clearanceField, tiles, deformer, DEFAULT_SUBDIVISIONS);
    }

    /**
     * Builds a continuous tile-patched surface with a configurable regular
     * grid. Every tile uses the same subdivision count and samples the same
     * world-space height/deformation fields, so shared edges stay coincident.
     *
     * @param subdivisions grid cells per tile edge, clamped to the supported
     * range of {@link #MIN_SUBDIVISIONS} through {@link #MAX_SUBDIVISIONS}
     */
    static Mesh createMesh(ClearanceField clearanceField, List<Point> tiles,
            TerrainDeformer deformer, int subdivisions) {
        subdivisions = normalizeSubdivisions(subdivisions);
        Mesh mesh = new Mesh();
        int side = subdivisions + 1;
        int verticesPerTile = side * side;
        int indexCountPerTile = subdivisions * subdivisions * 6;
        int vertexCount = checkedCapacity((long) tiles.size() * verticesPerTile, "ceiling vertices");
        int indexCount = checkedCapacity((long) tiles.size() * indexCountPerTile, "ceiling indices");

        FloatBuffer positions = BufferUtils.createFloatBuffer(checkedCapacity((long) vertexCount * 3, "ceiling positions"));
        FloatBuffer textureCoordinates = BufferUtils.createFloatBuffer(checkedCapacity((long) vertexCount * 2, "ceiling texture coordinates"));
        FloatBuffer normals = BufferUtils.createFloatBuffer(checkedCapacity((long) vertexCount * 3, "ceiling normals"));
        IntBuffer indexes = BufferUtils.createIntBuffer(indexCount);
        Vector3f vertex = new Vector3f();
        Vector3f normal = new Vector3f();
        Matrix3f jacobian = deformer == null ? null : new Matrix3f();
        float step = 1f / subdivisions;

        for (Point tile : tiles) {
            int base = positions.position() / 3;
            for (int j = 0; j <= subdivisions; j++) {
                for (int i = 0; i <= subdivisions; i++) {
                    float u = i * step;
                    float v = j * step;
                    float px = tile.x - 0.5f + u;
                    float py = tile.y - 0.5f + v;
                    vertex.set(px * WorldUtils.TILE_WIDTH,
                            WorldUtils.UNDERFLOOR_HEIGHT + heightAt(clearanceField, px, py),
                            py * WorldUtils.TILE_WIDTH);
                    normalAt(clearanceField, px, py, step, normal);
                    if (deformer != null) {
                        deformer.deformPositionAndNormal(vertex, normal, jacobian);
                    }
                    positions.put(vertex.x).put(vertex.y).put(vertex.z);
                    textureCoordinates.put(u).put(v);
                    normals.put(normal.x).put(normal.y).put(normal.z);
                }
            }

            // Uniform cells avoid T-junctions between different subdivisions;
            // every shared tile edge has matching positions and topology.
            for (int j = 0; j < subdivisions; j++) {
                for (int i = 0; i < subdivisions; i++) {
                    int topLeft = base + j * side + i;
                    int topRight = topLeft + 1;
                    int bottomLeft = topLeft + side;
                    int bottomRight = bottomLeft + 1;
                    indexes.put(topLeft).put(topRight).put(bottomLeft);
                    indexes.put(topRight).put(bottomRight).put(bottomLeft);
                }
            }
        }

        positions.flip();
        textureCoordinates.flip();
        normals.flip();
        indexes.flip();
        mesh.setBuffer(Type.Position, 3, positions);
        mesh.setBuffer(Type.TexCoord, 2, textureCoordinates);
        mesh.setBuffer(Type.Index, 3, indexes);
        mesh.setBuffer(Type.Normal, 3, normals);
        mesh.updateBound();

        return mesh;
    }

    public static int normalizeSubdivisions(int subdivisions) {
        return Math.max(MIN_SUBDIVISIONS, Math.min(MAX_SUBDIVISIONS, subdivisions));
    }

    private static int checkedCapacity(long capacity, String description) {
        if (capacity > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Too many " + description + " for one ceiling mesh: " + capacity);
        }
        return (int) capacity;
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] result = new int[list.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = list.get(i);
        }
        return result;
    }

    /**
     * The ceiling height directly above a continuous tile-space point, from
     * {@link ClearanceField#clearanceAt} mapped through the height curve.
     */
    private static float heightAt(ClearanceField clearanceField, float px, float py) {
        float clearance = clearanceField.clearanceAt(px, py);

        // A parabola whose apex sits one step past MAX_CLEARANCE, so it is
        // monotonic and concave across the whole input range: the ceiling
        // rises fastest right next to a wall and flattens out approaching
        // HEIGHT_APEX, which it never quite reaches. A linear ramp here
        // reads as tented rather than domed.
        float t = HEIGHT_APEX - clearance / 4f;
        return HEIGHT_APEX - (t * t) / 8f;
    }

    /**
     * A downward-facing normal from central differences of the height
     * surface, so the patch is lit rather than reading as flat-black under
     * the terrain lighting model every other surface uses.
     */
    private static void normalAt(ClearanceField clearanceField, float px, float py,
            float sampleStep, Vector3f result) {
        float hu0 = heightAt(clearanceField, px - sampleStep, py);
        float hu1 = heightAt(clearanceField, px + sampleStep, py);
        float hv0 = heightAt(clearanceField, px, py - sampleStep);
        float hv1 = heightAt(clearanceField, px, py + sampleStep);

        float slopeU = (hu1 - hu0) / (2 * sampleStep);
        float slopeV = (hv1 - hv0) / (2 * sampleStep);

        result.set(slopeU, -1, slopeV).normalizeLocal();
    }
}
