/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.scene.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnimationTopologyAuditTest {
    private Geometry triangle() {
        Mesh mesh = new Mesh(); mesh.setBuffer(VertexBuffer.Type.Position,3,new float[]{0,0,0,1,0,0,0,1,0});
        mesh.setBuffer(VertexBuffer.Type.TexCoord,2,new float[]{0,0,1,0,0,1}); mesh.setBuffer(VertexBuffer.Type.Index,3,new int[]{0,1,2});
        return new Geometry("test",mesh);
    }
    @Test void motionDoesNotChangeStructuralIdentityButUvOrIndicesDo() {
        var a = triangle(); var b = triangle(); b.getMesh().setBuffer(VertexBuffer.Type.Position,3,new float[]{0,0,9,4,0,0,0,5,0});
        assertEquals(AnimationTopologyAudit.inspect(a).geometry(),AnimationTopologyAudit.inspect(b).geometry());
        b.getMesh().setBuffer(VertexBuffer.Type.TexCoord,2,new float[]{0,0,0,1,1,0});
        assertNotEquals(AnimationTopologyAudit.inspect(a).geometry(),AnimationTopologyAudit.inspect(b).geometry());
        b = triangle(); b.getMesh().setBuffer(VertexBuffer.Type.Index,3,new int[]{0,2,1});
        assertNotEquals(AnimationTopologyAudit.inspect(a).geometry(),AnimationTopologyAudit.inspect(b).geometry());
    }
    @Test void ancestorPlacementIsReportedSeparatelyFromTopology() {
        var a = triangle(); var root = new Node(); root.attachChild(a);
        var before = AnimationTopologyAudit.inspect(root); root.move(1,2,3);
        var after = AnimationTopologyAudit.inspect(root);
        assertEquals(before.geometry(),after.geometry()); assertNotEquals(before.transforms(),after.transforms());
    }
}
