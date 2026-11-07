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
package toniarts.openkeeper.tools.convert.kmf;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.vecmath.Vector3f;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import static org.junit.jupiter.api.Assertions.*;

class StaticTransparencyConversionTest {
    @Test
    void staticConversionSplitsTransparentTrianglesAndKeepsOpaqueMesh() throws Exception {
        var source = new Mesh();
        source.setName("Synthetic model");
        source.setPos(0, 0, 0);
        source.setGeometries(List.of(new Vector3f(0, -1, 0), new Vector3f(1, -1, 0), new Vector3f(0, -1, -1),
                new Vector3f(0, 1, 0), new Vector3f(1, 1, 0), new Vector3f(0, 1, -1)));
        var vertices = new ArrayList<MeshVertex>();
        for (int i = 0; i < 6; i++) {
            var vertex = new MeshVertex();
            vertex.setGeomIndex(i);
            vertex.setNormal(0, 0, -1);
            vertex.setUv(new Uv(i % 3 == 1 ? 32768 : 0, i % 3 == 2 ? 32768 : 0));
            vertices.add(vertex);
        }
        var sprites = new ArrayList<MeshSprite>();
        for (int i = 0; i < 2; i++) {
            var sprite = new MeshSprite();
            sprite.setVertices(vertices);
            sprite.setTriangles(List.of(List.of(new Triangle((short) 0, (short) 1, (short) 2),
                    new Triangle((short) 3, (short) 4, (short) 5))));
            sprite.setMaterialIndex(i);
            sprites.add(sprite);
        }
        source.setSprites(sprites);
        var assets = new DesktopAssetManager(true);
        var transparent = new com.jme3.material.Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        transparent.setTransparent(true);
        var opaque = new com.jme3.material.Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        var convert = KmfModelLoader.class.getDeclaredMethod("handleMesh", Mesh.class, Map.class);
        convert.setAccessible(true);
        var model = (Node) convert.invoke(new KmfModelLoader(), source, Map.of(0, List.of(transparent), 1, List.of(opaque)));
        var triangles = (Node) model.getChild("Synthetic model_0");
        assertEquals(2, triangles.getQuantity());
        for (Spatial triangle : triangles.getChildren()) {
            assertEquals(1, ((Geometry) triangle).getMesh().getTriangleCount());
            assertEquals(Spatial.BatchHint.Never, triangle.getBatchHint());
        }
        assertEquals(2, ((Geometry) model.getChild("Synthetic model_1")).getMesh().getTriangleCount());
    }
}
