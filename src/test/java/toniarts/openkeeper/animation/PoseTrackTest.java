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
    @Test void packedBakeIsReadOnlyAndMatchesSparseSamplingAtFractionalTimes() {
        var source = track(1f/30);
        source.getDataAtTime(.25,null);
        var buffer = source.getTarget().getMesh().getBuffer(VertexBuffer.Type.Position);
        float before = x(source); buffer.clearUpdateNeeded();
        var packed = VertexClip.bake(source,1024*1024);
        assertEquals(before,x(source)); assertFalse(buffer.isUpdateNeeded());
        float[] out = new float[packed.vertexCount()*3];
        for (int i=-5;i<120;i++) {
            double time=i/100.0; packed.sample(time,out); source.getDataAtTime(time,null);
            var actual=(FloatBuffer)buffer.getData();
            for (int j=0;j<out.length;j++) assertEquals(actual.get(j),out[j],0);
        }
        assertThrows(IllegalArgumentException.class,()->VertexClip.bake(source,1));
    }
    @Test void mismatchedPositionAndBindLayoutsAreRejectedBeforeAnySample() {
        var mesh = new Mesh(); mesh.setBuffer(VertexBuffer.Type.Position,3,new float[]{0,0,0,1,1,1});
        mesh.setBuffer(VertexBuffer.Type.BindPosePosition,3,new float[]{0,0,0});
        var target = new Geometry("invalid-layout",mesh);
        var pose = new Pose("a",new Vector3f[]{new Vector3f()},new int[]{0});
        assertThrows(IllegalArgumentException.class,()->new PoseTrack(target,new float[]{0},
                new PoseTrack.PoseFrame[]{new PoseTrack.PoseFrame(new Pose[]{pose,pose},new float[]{0})}));
    }
    @Test void completedHiddenOneShotCanFlushItsFinalPoseWithoutAnotherComposerUpdate() {
        var track = track(0); Node root = new Node(); root.attachChild(track.getTarget());
        track.getDataAtTime(.25,null); assertEquals(.5,x(track),1e-6);
        root.setCullHint(Spatial.CullHint.Always); track.getDataAtTime(1,null);
        track.refreshPendingPose(); assertEquals(.5,x(track),1e-6);
        root.setCullHint(Spatial.CullHint.Dynamic); track.refreshPendingPose();
        assertEquals(2,x(track),1e-6);
        var buffer = track.getTarget().getMesh().getBuffer(VertexBuffer.Type.Position); buffer.clearUpdateNeeded();
        track.refreshPendingPose(); assertFalse(buffer.isUpdateNeeded());
    }
    @Test void unchangedAndHiddenSamplesDoNotDirtyBuffersButReentryUsesCurrentPhase() {
        var track = track(0); Node owner = new Node(); owner.attachChild(track.getTarget());
        var composer = new com.jme3.anim.AnimComposer();
        var clip = new com.jme3.anim.AnimClip("test"); clip.setTracks(new PoseTrack[]{track});
        composer.addAnimClip(clip); owner.addControl(composer); composer.setCurrentAction("test");
        owner.updateLogicalState(.25f); assertEquals(.5,x(track),1e-6);
        var positions = track.getTarget().getMesh().getBuffer(VertexBuffer.Type.Position);
        positions.clearUpdateNeeded(); owner.updateLogicalState(0); assertFalse(positions.isUpdateNeeded());
        owner.setCullHint(Spatial.CullHint.Always); owner.updateLogicalState(.5f);
        assertEquals(.75,composer.getTime(),1e-6); assertEquals(.5,x(track),1e-6); assertFalse(positions.isUpdateNeeded());
        owner.setCullHint(Spatial.CullHint.Dynamic); owner.updateLogicalState(0);
        assertEquals(1.5,x(track),1e-6); assertTrue(positions.isUpdateNeeded());
        positions.clearUpdateNeeded(); track.getDataAtTime(100,null); positions.clearUpdateNeeded();
        track.getDataAtTime(200,null); assertFalse(positions.isUpdateNeeded());
    }
    @Test void pickingRebuildsCollisionTreeAfterDeformation() {
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, new float[]{0,0,0, 1,0,0, 0,1,0});
        mesh.setBuffer(VertexBuffer.Type.BindPosePosition, 3, new float[]{0,0,0, 1,0,0, 0,1,0});
        mesh.setBuffer(VertexBuffer.Type.Index, 3, new int[]{0,1,2});
        Geometry target = new Geometry("pickable", mesh);
        Pose a = new Pose("a", new Vector3f[]{new Vector3f(), new Vector3f(1,0,0), new Vector3f(0,1,0)}, new int[]{0,1,2});
        Pose b = new Pose("b", new Vector3f[]{new Vector3f(0,0,1), new Vector3f(1,0,1), new Vector3f(0,1,1)}, new int[]{0,1,2});
        var track = new PoseTrack(target, new float[]{0,1}, new PoseTrack.PoseFrame[]{
            new PoseTrack.PoseFrame(new Pose[]{a,b}, new float[]{0}), new PoseTrack.PoseFrame(new Pose[]{a,b}, new float[]{1})});
        var ray = new com.jme3.math.Ray(new Vector3f(.25f,.25f,3), new Vector3f(0,0,-1));
        target.updateGeometricState(); track.getDataAtTime(0, null);
        var initial = new com.jme3.collision.CollisionResults(); target.collideWith(ray, initial);
        assertEquals(3, initial.getClosestCollision().getDistance(), 1e-6);
        track.getDataAtTime(1, null);
        var moved = new com.jme3.collision.CollisionResults(); target.collideWith(ray, moved);
        assertEquals(2, moved.getClosestCollision().getDistance(), 1e-6);
    }
    @Test void clipEnvelopeContainsAllSubframesAndSurvivesImport() throws Exception {
        var track = track(0);
        var before = track.getTarget().getModelBound().clone();
        for (int sample = -10; sample <= 110; sample++) {
            track.getDataAtTime(sample / 50.0, null);
            var data = (FloatBuffer)track.getTarget().getMesh().getBuffer(VertexBuffer.Type.Position).getData();
            assertTrue(before.contains(new Vector3f(data.get(0), data.get(1), data.get(2))));
        }
        var bytes = new ByteArrayOutputStream(); BinaryExporter.getInstance().save(track, bytes);
        var imported = (PoseTrack)BinaryImporter.getInstance().load(new ByteArrayInputStream(bytes.toByteArray()));
        assertTrue(imported.getTarget().getModelBound().contains(new Vector3f(2, 4, 6)));
        assertEquals(before, imported.getTarget().getModelBound());
    }
    @Test void repairsOnlyUnambiguousLegacyTerminalWeights() throws Exception {
        var original = track(0);
        var target = original.getTarget();
        Pose endpoint = new Pose("terminal", new Vector3f[]{new Vector3f(3, 4, 5)}, new int[]{0});
        var repaired = new PoseTrack(target, new float[]{0}, new PoseTrack.PoseFrame[]{
            new PoseTrack.PoseFrame(new Pose[]{endpoint, endpoint}, new float[]{Float.NaN})});
        repaired.getDataAtTime(100, null);
        assertEquals(3, x(repaired));
        var bytes = new ByteArrayOutputStream();
        BinaryExporter.getInstance().save(repaired, bytes);
        var imported = (PoseTrack) BinaryImporter.getInstance().load(new ByteArrayInputStream(bytes.toByteArray()));
        imported.getDataAtTime(-1, null);
        assertEquals(3, x(imported));
        Pose different = new Pose("invalid", new Vector3f[]{new Vector3f(7, 4, 5)}, new int[]{0});
        assertThrows(IllegalArgumentException.class, () -> new PoseTrack(target, new float[]{0},
            new PoseTrack.PoseFrame[]{new PoseTrack.PoseFrame(new Pose[]{endpoint, different}, new float[]{Float.NaN})}));
    }
    @Test void rejectsInvalidTimesAndMismatchedVertexPairs() {
        var target = track(0).getTarget();
        Pose a = new Pose("a", new Vector3f[]{new Vector3f()}, new int[]{0});
        Pose b = new Pose("b", new Vector3f[]{new Vector3f()}, new int[]{1});
        assertThrows(IllegalArgumentException.class, () -> new PoseTrack(target, new float[]{0},
            new PoseTrack.PoseFrame[]{new PoseTrack.PoseFrame(new Pose[]{a, b}, new float[]{0})}));
        assertThrows(IllegalArgumentException.class, () -> track(0).getDataAtTime(Double.NaN, null));
    }
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
