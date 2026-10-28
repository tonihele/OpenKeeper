/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.view.map.construction;

import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.math.Vector3f;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PortalTransparentSurfacesTest {
    @Test
    void batchingRetainsSeparateTransparentDepthBoundsAndBatchesOpaqueSurfaces() {
        var mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, new float[]{
            0, 0, -1, 1, 0, -1, 0, 1, -1,
            0, 0, 1, 1, 0, 1, 0, 1, 1});
        mesh.setBuffer(VertexBuffer.Type.Index, 3, new int[]{0, 1, 2, 3, 4, 5});
        mesh.updateBound();
        var material = new Material(new MaterialDef(null, "alpha"));
        material.setTransparent(true);
        material.getAdditionalRenderState().setDepthWrite(false);
        var surface = new Geometry("chasm", mesh);
        surface.setMaterial(material);
        surface.setLocalTranslation(0, 0, 4);
        var model = new Node();
        model.attachChild(surface);
        var opaqueMaterial = new Material(new MaterialDef(null, "opaque"));
        var opaque = new Geometry("floor", mesh);
        opaque.setMaterial(opaqueMaterial);
        model.attachChild(opaque);
        PortalTransparentSurfaces.prepare(model);
        var floor = new BatchNode();
        floor.attachChild(model);
        floor.batch();
        floor.updateGeometricState();

        assertTrue(opaque.isBatched());
        var triangles = (Node) model.getChild("chasm");
        assertEquals(2, triangles.getQuantity());
        for (int i = 0; i < 2; i++) {
            var triangle = (Geometry) triangles.getChild(i);
            assertFalse(triangle.isBatched());
            assertSame(material, triangle.getMaterial());
            assertFalse(triangle.getMaterial().getAdditionalRenderState().isDepthWrite());
            assertEquals(1, triangle.getMesh().getTriangleCount());
            assertSame(mesh.getBuffer(VertexBuffer.Type.Position),
                    triangle.getMesh().getBuffer(VertexBuffer.Type.Position));
            assertEquals(new Vector3f(.5f, .5f, i == 0 ? 3 : 5),
                    triangle.getWorldBound().getCenter());
        }
    }
}
