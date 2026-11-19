/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.math.Vector3f;
import com.jme3.scene.*;
import java.nio.FloatBuffer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.util.clone.Cloner;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PoseTrackTest {
    private static PoseTrack track(float origin) {
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, new float[]{0, 0, 0, 9, 9, 9});
        mesh.setBuffer(VertexBuffer.Type.BindPosePosition, 3, new float[]{0, 0, 0, 9, 9, 9});
        Pose a = new Pose("a", new Vector3f[]{new Vector3f(0, 0, 0)}, new int[]{0});
        Pose b = new Pose("b", new Vector3f[]{new Vector3f(2, 4, 6)}, new int[]{0});
        return new PoseTrack(new Geometry("pose", mesh), new float[]{origin, origin + 1, origin + 2},
            new PoseTrack.PoseFrame[]{
                new PoseTrack.PoseFrame(new Pose[]{a, b}, new float[]{0}),
                new PoseTrack.PoseFrame(new Pose[]{a, b}, new float[]{1}),
                new PoseTrack.PoseFrame(new Pose[]{a, b}, new float[]{0})});
    }
    private static float x(PoseTrack t) {
        return ((FloatBuffer) t.getTarget().getMesh().getBuffer(VertexBuffer.Type.Position).getData()).get(0);
    }
    @Test void interpolatesBetweenDecodedFramesWithoutDroppingSparseVertices() {
        var t = track(0);
        t.getDataAtTime(.25, null);
        assertEquals(.5, x(t), 1e-6);
        t.getDataAtTime(.75, null);
        assertEquals(1.5, x(t), 1e-6);
        t.getDataAtTime(1.5, null);
        assertEquals(1, x(t), 1e-6);
        assertEquals(9, ((FloatBuffer) t.getTarget().getMesh().getBuffer(VertexBuffer.Type.Position).getData()).get(3));
    }
    @Test void oldConvertedTimeOriginIsNormalizedAndWrapEndpointsAgree() {
        var t = track(1f / 30);
        t.getDataAtTime(0, null);
        assertEquals(0, x(t));
        t.getDataAtTime(.5, null);
        assertEquals(1, x(t), 1e-5);
        t.getDataAtTime(t.getLength(), null);
        assertEquals(0, x(t), 1e-5);
    }
    @Test void clonedTracksDoNotShareMutableSampleCaches() {
        var a = track(0);
        a.getDataAtTime(.25, null);
        var b = a.clone();
        b.setTarget((Geometry) a.getTarget().deepClone());
        b.getDataAtTime(.75, null);
        a.getDataAtTime(.25, null);
        assertEquals(.5, x(a), 1e-6);
        assertEquals(1.5, x(b), 1e-6);
    }

    @Test void importedAndSceneClonedTracksRetainInterpolationAndClampTheirEndpoints() throws Exception {
        var source = track(1f / 30);
        source.getDataAtTime(.25, null);
        var sceneClone = new Cloner().clone(source);
        sceneClone.getDataAtTime(.75, null);
        assertEquals(1.5, x(sceneClone), 1e-5);
        assertEquals(.5, x(source), 1e-5);
        var bytes = new ByteArrayOutputStream();
        BinaryExporter.getInstance().save(source, bytes);
        var imported = (PoseTrack) BinaryImporter.getInstance().load(new ByteArrayInputStream(bytes.toByteArray()));
        imported.getDataAtTime(-10, null);
        assertEquals(0, x(imported), 1e-5);
        imported.getDataAtTime(.75, null);
        assertEquals(1.5, x(imported), 1e-5);
        imported.getDataAtTime(100, null);
        assertEquals(0, x(imported), 1e-5);
    }
}
