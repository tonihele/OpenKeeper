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
import java.nio.FloatBuffer;
import java.util.Arrays;

/**
 * The triangles a caster casts a shadow with, in world space, as they are
 * posed right now. Collected from the opaque parts of a model: the
 * translucent, additive and alpha-tested parts, glass and glowing things, cast
 * nothing and leave holes in the silhouette.
 */
final class CasterMesh {

    private float[] vertices = new float[3 * 1024];
    private int vertexCount;
    private int[] triangles = new int[3 * 2048];
    private int triangleCount;
    private final Vector3f centre = new Vector3f();
    private float radius;
    private final Matrix3f rotation = new Matrix3f();

    /**
     * Collects the triangles of a model, and its bounding sphere
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

        // The bounding sphere of the bounding box, which does not shrink and
        // grow in an animation as the sphere that fits tight would
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (int i = 0; i < vertexCount; i++) {
            float x = vertices[i * 3];
            float y = vertices[i * 3 + 1];
            float z = vertices[i * 3 + 2];
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }
        centre.set((minX + maxX) / 2f, (minY + maxY) / 2f, (minZ + maxZ) / 2f);
        float dx = (maxX - minX) / 2f;
        float dy = (maxY - minY) / 2f;
        float dz = (maxZ - minZ) / 2f;
        radius = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
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
        } else if (spatial instanceof Geometry geometry && castsShadow(geometry)) {
            collectGeometry(geometry);
        }
    }

    private static boolean castsShadow(Geometry geometry) {
        Material material = geometry.getMaterial();
        if (material == null) {
            return false;
        }
        RenderQueue.Bucket bucket = geometry.getQueueBucket();
        if (bucket == RenderQueue.Bucket.Transparent || bucket == RenderQueue.Bucket.Translucent
                || bucket == RenderQueue.Bucket.Gui || bucket == RenderQueue.Bucket.Sky) {
            return false;
        }
        if (material.getAdditionalRenderState().getBlendMode() != RenderState.BlendMode.Off) {
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

        ensureVertexCapacity(vertexCount + meshVertices);
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

        int meshTriangles = indices.size() / 3;
        if (triangles.length < (triangleCount + meshTriangles) * 3) {
            triangles = Arrays.copyOf(triangles, Math.max(triangles.length * 2, (triangleCount + meshTriangles) * 3));
        }
        for (int i = 0; i < meshTriangles * 3; i++) {
            triangles[triangleCount * 3 + i] = vertexCount + indices.get(i);
        }
        triangleCount += meshTriangles;
        vertexCount += meshVertices;
    }

    private void ensureVertexCapacity(int required) {
        if (vertices.length < required * 3) {
            vertices = Arrays.copyOf(vertices, Math.max(vertices.length * 2, required * 3));
        }
    }

    float[] getVertices() {
        return vertices;
    }

    int getVertexCount() {
        return vertexCount;
    }

    int[] getTriangles() {
        return triangles;
    }

    int getTriangleCount() {
        return triangleCount;
    }

    /**
     * The centre of the bounding sphere, in world space
     */
    Vector3f getCentre() {
        return centre;
    }

    float getRadius() {
        return radius;
    }
}
