/*
 * Copyright (C) 2026 OpenKeeper
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
package toniarts.openkeeper.tools.convert;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Prepares static transparent meshes for jME's geometry-level depth sorting.
 * Overlapping surfaces need independently sortable triangles; otherwise one
 * whole surface draws over another even where their depth order reverses.
 * Shared vertex buffers keep the extra geometry small, while disabling batching
 * preserves the individual draw bounds after loading the converted model.
 */
final class TransparentSurfaces {
    private TransparentSurfaces() {
    }

    static void prepare(Spatial model) {
        List<Geometry> surfaces = new ArrayList<>();
        model.depthFirstTraversal(spatial -> {
            if (spatial instanceof Geometry geometry && geometry.getMaterial().isTransparent()) {
                surfaces.add(geometry);
            }
        });
        for (Geometry surface : surfaces) {
            Node triangles = new Node(surface.getName());
            triangles.setLocalTransform(surface.getLocalTransform().clone());
            triangles.setShadowMode(surface.getLocalShadowMode());
            Mesh original = surface.getMesh();
            int[] indices = new int[3];
            Vector3f a = new Vector3f(), b = new Vector3f(), c = new Vector3f();
            for (int i = 0; i < original.getTriangleCount(); i++) {
                original.getTriangle(i, indices);
                original.getTriangle(i, a, b, c);
                Mesh mesh = new Mesh();
                // Share the immutable vertex buffers; only indices and bounds differ.
                for (VertexBuffer buffer : original.getBufferList()) {
                    if (buffer.getBufferType() != VertexBuffer.Type.Index) {
                        mesh.setBuffer(buffer);
                    }
                }
                mesh.setBuffer(VertexBuffer.Type.Index, 3, indices.clone());
                Vector3f min = a.clone().minLocal(b).minLocal(c);
                Vector3f max = a.clone().maxLocal(b).maxLocal(c);
                BoundingBox bound = new BoundingBox();
                bound.setMinMax(min, max);
                mesh.setBound(bound);
                Geometry triangle = new Geometry(surface.getName() + ":" + i, mesh);
                triangle.setMaterial(surface.getMaterial());
                // Keep alternative textures and other converter metadata.
                for (String key : surface.getUserDataKeys()) {
                    triangle.setUserData(key, surface.getUserData(key));
                }
                triangle.setQueueBucket(Bucket.Transparent);
                // Batching would combine these again and discard triangle-level ordering.
                triangle.setBatchHint(Spatial.BatchHint.Never);
                triangles.attachChild(triangle);
            }
            surface.getParent().attachChild(triangles);
            surface.removeFromParent();
        }
    }
}
