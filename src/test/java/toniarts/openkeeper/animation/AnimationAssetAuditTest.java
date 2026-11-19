/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.anim.*;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector3f;
import com.jme3.scene.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnimationAssetAuditTest {
    @Test void finiteOutOfBoundsSamplesFailValidationButCanBeReportedForHistoricalBaselines() {
        Mesh mesh = new Mesh(); mesh.setBuffer(VertexBuffer.Type.Position,3,new float[]{0,0,0});
        mesh.setBuffer(VertexBuffer.Type.BindPosePosition,3,new float[]{0,0,0});
        var geometry = new Geometry("audit",mesh);
        var a = new Pose("a",new Vector3f[]{new Vector3f(1,0,0)},new int[]{0});
        var b = new Pose("b",new Vector3f[]{new Vector3f(2,0,0)},new int[]{0});
        var track = new PoseTrack(geometry,new float[]{0,1},new PoseTrack.PoseFrame[]{
            new PoseTrack.PoseFrame(new Pose[]{a,b},new float[]{0}),new PoseTrack.PoseFrame(new Pose[]{a,b},new float[]{1})});
        geometry.setModelBound(new BoundingBox(Vector3f.ZERO,.25f,.25f,.25f));
        Node root = new Node(); root.attachChild(geometry); var composer = new AnimComposer(); var clip = new AnimClip("audit");
        clip.setTracks(new PoseTrack[]{track}); composer.addAnimClip(clip); root.addControl(composer);
        long[] counts = AnimationAssetAudit.inspect(root);
        assertEquals(0,counts[2]); assertTrue(counts[3] > 0);
        assertThrows(AssertionError.class,()->AnimationAssetAudit.validate(counts[2],counts[3],false));
        assertDoesNotThrow(()->AnimationAssetAudit.validate(counts[2],counts[3],true));
    }
}
