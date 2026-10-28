/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.view.map.construction;

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

/** Let the transparent queue interleave the Portal's chasm and crystal triangles. */
final class PortalTransparentSurfaces {
    private PortalTransparentSurfaces() {
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
