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

import com.jme3.material.Material;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransparentSurfacesTest {
    @Test
    void convertedTrianglesSurviveSerializationAndRoomBatching() throws Exception {
        var mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, new float[]{
            0, 0, -1, 1, 0, -1, 0, 1, -1,
            0, 0, 1, 1, 0, 1, 0, 1, 1});
        mesh.setBuffer(VertexBuffer.Type.Index, 3, new int[]{0, 1, 2, 3, 4, 5});
        mesh.updateBound();
        var assets = new DesktopAssetManager(true);
        var material = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        material.setTransparent(true);
        material.getAdditionalRenderState().setDepthWrite(false);
        var surface = new Geometry("chasm", mesh);
        surface.setMaterial(material);
        surface.setLocalTranslation(0, 0, 4);
        var model = new Node();
        surface.setUserData(KmfModelLoader.MATERIAL_ALTERNATIVE_TEXTURES,
                new java.util.ArrayList<>(java.util.List.of("Textures/variant.png")));
        model.attachChild(surface);
        var opaqueMaterial = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        var opaque = new Geometry("floor", mesh);
        opaque.setMaterial(opaqueMaterial);
        model.attachChild(opaque);
        TransparentSurfaces.prepare(model);
        var bytes = new java.io.ByteArrayOutputStream();
        com.jme3.export.binary.BinaryExporter.getInstance().save(model, bytes);
        var importer = new com.jme3.export.binary.BinaryImporter();
        importer.setAssetManager(assets);
        model = (Node) importer.load(new java.io.ByteArrayInputStream(bytes.toByteArray()));
        opaque = (Geometry) model.getChild("floor");
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
            assertSame(((Geometry) triangles.getChild(0)).getMaterial(), triangle.getMaterial());
            assertEquals(java.util.List.of("Textures/variant.png"),
                    triangle.getUserData(KmfModelLoader.MATERIAL_ALTERNATIVE_TEXTURES));
            assertFalse(triangle.getMaterial().getAdditionalRenderState().isDepthWrite());
            assertEquals(1, triangle.getMesh().getTriangleCount());
            assertSame(((Geometry) triangles.getChild(0)).getMesh().getBuffer(VertexBuffer.Type.Position),
                    triangle.getMesh().getBuffer(VertexBuffer.Type.Position));
            assertEquals(new Vector3f(.5f, .5f, i == 0 ? 3 : 5),
                    triangle.getWorldBound().getCenter());
        }
    }
}
