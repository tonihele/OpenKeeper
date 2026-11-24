/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.animation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import com.jme3.util.clone.Cloner;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PoseTrackTest {

    private static final float[] TIMES = {1f / 30f, 2f / 30f, 3f / 30f};
    private static final float[] BIND_POSE = {
        0f, 0f, 0f,
        1f, 1f, 1f,
        2f, 2f, 2f,
        3f, 3f, 3f
    };
    private static final PoseTrack.PoseFrame[] FRAMES = {
        frame(new int[]{0, 2}, new float[][]{{0f, 2f, 4f}, {2f, 4f, 6f}},
                new float[][]{{2f, 4f, 6f}, {4f, 6f, 8f}}, 0.5f),
        frame(new int[]{1}, new float[][]{{-1f, 0f, 1f}},
                new float[][]{{1f, 2f, 3f}}, 0.25f),
        frame(new int[]{0, 3}, new float[][]{{-2f, -2f, -2f}, {1f, 1f, 1f}},
                new float[][]{{2f, 2f, 2f}, {5f, 5f, 5f}}, 0.75f)
    };

    @Test
    void defaultEvaluatorMatchesOriginalFrameSelectionAndInterpolation() {
        double[] sampleTimes = {
            -1d,
            TIMES[0] - 0.000001d,
            TIMES[0],
            TIMES[0] + 0.000001d,
            (TIMES[0] + TIMES[1]) / 2d,
            TIMES[1],
            TIMES[2],
            TIMES[2] + 0.000001d,
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.NaN
        };

        PoseTrack track = newTrack(PoseTrackEvaluator.standard());
        for (double time : sampleTimes) {
            track.getDataAtTime(time, null);
            assertArrayEquals(evaluateWithOriginalAlgorithm(time), positionData(track), 0.000001f,
                    "pose output at time " + time);
        }
    }

    @Test
    void evaluatorCanBeInjectedAndIsInvokedForEachTrackEvaluation() {
        AtomicInteger evaluations = new AtomicInteger();
        PoseTrackEvaluator evaluator = (mesh, times, frames, time) -> evaluations.incrementAndGet();
        PoseTrack track = newTrack(evaluator);

        track.getDataAtTime(0.1, null);
        track.getDataAtTime(0.2, null);

        assertEquals(2, evaluations.get());
    }

    @Test
    void evaluatorCanCacheOnlyTimesWithTheSameStableOutputKey() {
        AtomicInteger evaluations = new AtomicInteger();
        PoseTrack track = newTrack(keyedEvaluator(evaluations));

        track.getDataAtTime(0.01, null);
        track.getDataAtTime(0.02, null);
        track.getDataAtTime(0.11, null);
        track.getDataAtTime(0.12, null);
        track.getDataAtTime(0.02, null);

        assertEquals(3, evaluations.get());
    }

    @Test
    void clonesStartWithAnInvalidEvaluationCache() throws CloneNotSupportedException {
        AtomicInteger evaluations = new AtomicInteger();
        PoseTrack track = newTrack(keyedEvaluator(evaluations));
        track.getDataAtTime(0.01, null);

        PoseTrack clone = track.clone();
        assertNotSame(track, clone);
        clone.getDataAtTime(0.01, null);

        assertEquals(2, evaluations.get());
    }

    @Test
    void jmeClonesHaveIndependentPoseAndEvaluationState() {
        AtomicInteger evaluations = new AtomicInteger();
        PoseTrack track = newTrack(keyedEvaluator(evaluations));
        track.getDataAtTime(0.01, null);

        PoseTrack clone = new Cloner().clone(track);
        assertNotSame(track, clone);
        assertNotSame(track.getTarget(), clone.getTarget());
        clone.getDataAtTime(0.02, null);

        assertEquals(2, evaluations.get());
    }

    @Test
    void eachFrameStartsFromBindPoseAndDoesNotRetainPreviousSparseVertices() {
        PoseTrack track = newTrack(PoseTrackEvaluator.standard());

        track.getDataAtTime(0d, null);
        assertEquals(1f, positionData(track)[3], 0.000001f,
                "vertex 1 must remain at its bind-pose value");

        track.getDataAtTime(TIMES[0], null);
        assertEquals(0f, positionData(track)[0], 0.000001f,
                "vertex 0 from the previous frame must be reset to bind pose");
        assertEquals(-0.5f, positionData(track)[3], 0.000001f,
                "the current frame pose should be applied to vertex 1");
    }

    @Test
    void singleFrameAnimationUsesItsOnlyFrameAtAnyTime() {
        PoseTrack track = new PoseTrack(
                new Geometry("single-frame", newMesh()),
                new float[]{TIMES[0]},
                new PoseTrack.PoseFrame[]{FRAMES[0]},
                PoseTrackEvaluator.standard());

        float[] expected = {
            1f, 3f, 5f,
            1f, 1f, 1f,
            3f, 5f, 7f,
            3f, 3f, 3f
        };
        for (double time : new double[]{-1d, TIMES[0], 2d, Double.NaN}) {
            track.getDataAtTime(time, null);
            assertArrayEquals(expected, positionData(track), 0.000001f);
        }
    }

    private static PoseTrack newTrack(PoseTrackEvaluator evaluator) {
        return new PoseTrack(new Geometry("test-pose", newMesh()), TIMES.clone(), FRAMES.clone(), evaluator);
    }

    private static Mesh newMesh() {
        Mesh mesh = new Mesh();
        FloatBuffer bind = BufferUtils.createFloatBuffer(BIND_POSE);
        mesh.setBuffer(Type.BindPosePosition, 3, bind);
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(BIND_POSE));
        return mesh;
    }

    private static PoseTrackEvaluator keyedEvaluator(AtomicInteger evaluations) {
        return new PoseTrackEvaluator() {
            @Override
            public int evaluationKey(Mesh mesh, float[] times, PoseTrack.PoseFrame[] frames, double time) {
                return time < 0.1 ? 0 : 1;
            }

            @Override
            public void evaluate(Mesh mesh, float[] times, PoseTrack.PoseFrame[] frames, double time) {
                evaluations.incrementAndGet();
            }
        };
    }

    private static PoseTrack.PoseFrame frame(int[] indices, float[][] start, float[][] end, float weight) {
        Vector3f[] startOffsets = new Vector3f[start.length];
        Vector3f[] endOffsets = new Vector3f[end.length];
        for (int i = 0; i < start.length; i++) {
            startOffsets[i] = new Vector3f(start[i][0], start[i][1], start[i][2]);
            endOffsets[i] = new Vector3f(end[i][0], end[i][1], end[i][2]);
        }
        Pose startPose = new Pose("start", startOffsets, indices);
        Pose endPose = new Pose("end", endOffsets, indices.clone());
        return new PoseTrack.PoseFrame(new Pose[]{startPose, endPose}, new float[]{weight});
    }

    private static float[] evaluateWithOriginalAlgorithm(double time) {
        float[] result = BIND_POSE.clone();
        int frameIndex;
        if (time < TIMES[0]) {
            frameIndex = 0;
        } else if (time > TIMES[TIMES.length - 1]) {
            frameIndex = TIMES.length - 1;
        } else {
            int startFrame = 0;
            for (int i = 0; i < TIMES.length; i++) {
                if (TIMES[i] < time) {
                    startFrame = i;
                }
            }
            frameIndex = startFrame + 1;
        }

        PoseTrack.PoseFrame frame = FRAMES[frameIndex];
        for (int pair = 0; pair < frame.poses.length / 2; pair++) {
            Pose startPose = frame.poses[pair * 2];
            if (startPose == null) {
                continue;
            }
            Pose endPose = frame.poses[pair * 2 + 1];
            int[] indices = startPose.getIndices();
            for (int i = 0; i < indices.length; i++) {
                Vector3f interpolated = FastMath.interpolateLinear(frame.weights[pair],
                        startPose.getOffsets()[i], endPose.getOffsets()[i], new Vector3f());
                int offset = indices[i] * 3;
                result[offset] = interpolated.x;
                result[offset + 1] = interpolated.y;
                result[offset + 2] = interpolated.z;
            }
        }
        return result;
    }

    private static float[] positionData(PoseTrack track) {
        FloatBuffer buffer = (FloatBuffer) track.getTarget().getMesh().getBuffer(Type.Position).getData();
        float[] result = new float[buffer.limit()];
        for (int i = 0; i < result.length; i++) {
            result[i] = buffer.get(i);
        }
        return result;
    }
}
