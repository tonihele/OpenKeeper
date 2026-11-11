/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.map;

import com.jme3.bounding.BoundingVolume;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PillarGeometryTest {

    @Test
    void alignsModelsWithDifferentLocalOriginsToTheRequestedFloor() {
        for (float localBase : new float[]{0f, 0.447f, 0.5f, 1.2f}) {
            for (float floorY : new float[]{0.5f, 1f, 1.75f}) {
                Spatial model = model(localBase, localBase + 1f);
                model.setLocalTranslation(4f, 1f, 6f);

                PillarGeometry.alignBaseToFloor(model, floorY);

                BoundingVolume bound = model.getWorldBound();
                assertNotNull(bound);
                assertEquals(floorY, minimumY(bound), 1e-5f,
                        "local base " + localBase + " at floor " + floorY);
            }
        }
    }

    private static Spatial model(float minY, float maxY) {
        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(
                new Vector3f(0, minY, 0), new Vector3f(1, minY, 0), new Vector3f(0, maxY, 1)));
        mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(new int[]{0, 1, 2}));
        mesh.updateBound();
        Node model = new Node("pillar model");
        model.attachChild(new Geometry("pillar mesh", mesh));
        model.updateGeometricState();
        return model;
    }

    private static float minimumY(BoundingVolume bound) {
        BoundingBox box = assertInstanceOf(BoundingBox.class, bound);
        return box.getCenter().y - box.getYExtent();
    }
}
