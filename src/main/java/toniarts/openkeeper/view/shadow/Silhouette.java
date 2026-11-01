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

import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Matrix3f;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.IndexBuffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

/**
 * The silhouette of a model as a {@link #SIZE} square coverage buffer.
 * {@link #collect} gathers the model's opaque triangles in world space as
 * they are posed right now (translucent, additive and alpha-tested parts cast
 * nothing), {@link #draw} projects them and scan converts them at 8x8
 * subsamples per pixel, each covered subsample adding 4 and saturating at 255.
 */
final class Silhouette {

    static final int SIZE = 32;
    private static final int SUBSAMPLES = 8;
    private static final int UNITS = SIZE * SUBSAMPLES;

    /**
     * The bounding sphere covers +-125 of the +-128 raster units
     */
    private static final float EXTENT = 125f;

    private final int[] coverage = new int[SIZE * SIZE];
    private final Matrix3f rotation = new Matrix3f();
    private final Vector3f centre = new Vector3f();
    private float radius;
    private float[] vertices = new float[3 * 1024];
    private float[] projected = new float[2 * 1024];
    private int vertexCount;
    private int[] triangles = new int[3 * 2048];
    private int triangleCount;

    /**
     * Collects the triangles of a model and its bounding sphere
     *
     * @param model the model, with up to date world transforms
     * @return {@code false} if there is nothing to cast a shadow with
     */
    boolean collect(Spatial model) {
        vertexCount = 0;
        triangleCount = 0;
        collectSpatial(model);
        if (triangleCount == 0) {
            return false;
        }

        // The sphere around the bounding box, which does not shrink and grow
        // as much in an animation as the tightest one would
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (int i = 0; i < vertexCount; i++) {
            minX = Math.min(minX, vertices[i * 3]);
            minY = Math.min(minY, vertices[i * 3 + 1]);
            minZ = Math.min(minZ, vertices[i * 3 + 2]);
            maxX = Math.max(maxX, vertices[i * 3]);
            maxY = Math.max(maxY, vertices[i * 3 + 1]);
            maxZ = Math.max(maxZ, vertices[i * 3 + 2]);
        }
        centre.set((minX + maxX) / 2f, (minY + maxY) / 2f, (minZ + maxZ) / 2f);
        radius = new Vector3f(maxX - minX, maxY - minY, maxZ - minZ).length() / 2f;
        return radius > 0f;
    }

    private void collectSpatial(Spatial spatial) {
        if (spatial.getCullHint() == Spatial.CullHint.Always) {
            return; // For example the animations that are not playing
        }
        if (spatial instanceof Node node) {
            for (Spatial child : node.getChildren()) {
                collectSpatial(child);
            }
        } else if (spatial instanceof Geometry geometry && isOpaque(geometry)) {
            collectGeometry(geometry);
        }
    }

    private static boolean isOpaque(Geometry geometry) {
        Material material = geometry.getMaterial();
        RenderQueue.Bucket bucket = geometry.getQueueBucket();
        if (material == null || bucket == RenderQueue.Bucket.Transparent || bucket == RenderQueue.Bucket.Translucent
                || material.getAdditionalRenderState().getBlendMode() != RenderState.BlendMode.Off) {
            return false;
        }
        MatParam alphaDiscard = material.getParam("AlphaDiscardThreshold");
        return !(alphaDiscard != null && alphaDiscard.getValue() instanceof Float threshold && threshold > 0f);
    }

    private void collectGeometry(Geometry geometry) {
        Mesh mesh = geometry.getMesh();
        FloatBuffer positions = mesh.getFloatBuffer(VertexBuffer.Type.Position);
        IndexBuffer indices = mesh.getIndexBuffer();
        if (mesh.getMode() != Mesh.Mode.Triangles || positions == null || indices == null) {
            return;
        }
        int meshVertices = mesh.getVertexCount();
        int meshTriangles = indices.size() / 3;
        if (vertices.length < (vertexCount + meshVertices) * 3) {
            vertices = Arrays.copyOf(vertices, (vertexCount + meshVertices) * 6);
        }
        if (triangles.length < (triangleCount + meshTriangles) * 3) {
            triangles = Arrays.copyOf(triangles, (triangleCount + meshTriangles) * 6);
        }

        Transform transform = geometry.getWorldTransform();
        transform.getRotation().toRotationMatrix(rotation);
        Vector3f scale = transform.getScale();
        Vector3f translation = transform.getTranslation();
        for (int i = 0; i < meshVertices; i++) {
            float x = positions.get(i * 3) * scale.x;
            float y = positions.get(i * 3 + 1) * scale.y;
            float z = positions.get(i * 3 + 2) * scale.z;
            int target = (vertexCount + i) * 3;
            vertices[target] = rotation.get(0, 0) * x + rotation.get(0, 1) * y + rotation.get(0, 2) * z + translation.x;
            vertices[target + 1] = rotation.get(1, 0) * x + rotation.get(1, 1) * y + rotation.get(1, 2) * z + translation.y;
            vertices[target + 2] = rotation.get(2, 0) * x + rotation.get(2, 1) * y + rotation.get(2, 2) * z + translation.z;
        }
        for (int i = 0; i < meshTriangles * 3; i++) {
            triangles[triangleCount * 3 + i] = vertexCount + indices.get(i);
        }
        vertexCount += meshVertices;
        triangleCount += meshTriangles;
    }

    /**
     * Replaces the coverage with that of another silhouette
     */
    void copyCoverageFrom(Silhouette other) {
        System.arraycopy(other.coverage, 0, coverage, 0, coverage.length);
    }

    /**
     * The centre of the collected model's bounding sphere, in world space
     */
    Vector3f getCentre() {
        return centre;
    }

    float getRadius() {
        return radius;
    }

    /**
     * Draws the collected triangles, replacing the coverage
     *
     * @see #draw(float[], int, int[], int, Vector3f, float, float, float, float)
     */
    void draw(float ux, float uz, float theta) {
        draw(vertices, vertexCount, triangles, triangleCount, centre, radius, ux, uz, theta);
    }

    /**
     * Replaces the coverage with the triangles, projected orthographically
     * along a view tilted from straight down. The image columns run along
     * {@code v}, the horizontal perpendicular of {@code u}, and the rows along
     * the view's up, which at {@code theta == 0} (straight down) is
     * {@code u}.
     *
     * @param vertices xyz of the world space vertices
     * @param triangles the vertex indices, three per triangle
     * @param centre the centre of the bounding sphere
     * @param radius the radius of the bounding sphere
     * @param ux the horizontal unit vector from the light to the caster (x)
     * @param uz the same (z)
     * @param theta the tilt from straight down in radians, along {@code u}
     */
    void draw(float[] vertices, int vertexCount, int[] triangles, int triangleCount,
            Vector3f centre, float radius, float ux, float uz, float theta) {
        Arrays.fill(coverage, 0);
        if (radius <= 0f) {
            return;
        }
        if (projected.length < vertexCount * 2) {
            projected = new float[vertexCount * 2];
        }
        float scale = EXTENT / radius;
        float cos = (float) Math.cos(theta);
        float sin = (float) Math.sin(theta);
        for (int i = 0; i < vertexCount; i++) {
            float dx = vertices[i * 3] - centre.x;
            float dy = vertices[i * 3 + 1] - centre.y;
            float dz = vertices[i * 3 + 2] - centre.z;
            projected[i * 2] = UNITS / 2f + scale * (-dx * uz + dz * ux);
            projected[i * 2 + 1] = UNITS / 2f + scale * ((dx * ux + dz * uz) * cos + dy * sin);
        }
        for (int i = 0; i < triangleCount; i++) {
            int a = triangles[i * 3] * 2;
            int b = triangles[i * 3 + 1] * 2;
            int c = triangles[i * 3 + 2] * 2;
            triangle(projected[a], projected[a + 1], projected[b], projected[b + 1], projected[c], projected[c + 1]);
        }
    }

    /**
     * Adds the coverage of a triangle given in raster units (0..256 across)
     */
    void triangle(float x0, float y0, float x1, float y1, float x2, float y2) {
        if (Float.isNaN(x0 + y0 + x1 + y1 + x2 + y2)) {
            return;
        }

        // A subsample is sampled at its centre
        int firstRow = Math.max(0, (int) Math.ceil(Math.min(y0, Math.min(y1, y2)) - 0.5f));
        int lastRow = Math.min(UNITS - 1, (int) Math.floor(Math.max(y0, Math.max(y1, y2)) - 0.5f));
        float[][] edges = {{x0, y0, x1, y1}, {x1, y1, x2, y2}, {x2, y2, x0, y0}};
        for (int row = firstRow; row <= lastRow; row++) {
            float y = row + 0.5f;
            float left = Float.POSITIVE_INFINITY;
            float right = Float.NEGATIVE_INFINITY;
            for (float[] e : edges) {
                if ((e[1] <= y && y < e[3]) || (e[3] <= y && y < e[1])) {
                    float x = e[0] + (y - e[1]) * (e[2] - e[0]) / (e[3] - e[1]);
                    left = Math.min(left, x);
                    right = Math.max(right, x);
                }
            }

            int firstColumn = Math.max(0, (int) Math.ceil(left - 0.5f));
            int lastColumn = Math.min(UNITS - 1, (int) Math.floor(right - 0.5f));
            for (int column = firstColumn; column <= lastColumn; column++) {
                coverage[(row / SUBSAMPLES) * SIZE + column / SUBSAMPLES] += 4;
            }
        }
    }

    /**
     * @return the coverage of a pixel, 0..255
     */
    int get(int column, int row) {
        return Math.min(255, coverage[row * SIZE + column]);
    }

    /**
     * Writes the coverage as the alpha of black RGBA pixels, row 0 first
     */
    void writeTo(ByteBuffer rgba) {
        for (int i = 0; i < coverage.length; i++) {
            rgba.put(i * 4, (byte) 0);
            rgba.put(i * 4 + 1, (byte) 0);
            rgba.put(i * 4 + 2, (byte) 0);
            rgba.put(i * 4 + 3, (byte) Math.min(255, coverage[i]));
        }
    }
}
