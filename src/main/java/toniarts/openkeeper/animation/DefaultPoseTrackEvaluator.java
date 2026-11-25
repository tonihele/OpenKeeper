/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.animation;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import java.nio.FloatBuffer;

/** Default allocation-free evaluator for the game's sparse KMF pose tracks. */
final class DefaultPoseTrackEvaluator implements PoseTrackEvaluator {

    static final DefaultPoseTrackEvaluator INSTANCE = new DefaultPoseTrackEvaluator();

    private DefaultPoseTrackEvaluator() {
    }

    @Override
    public int evaluationKey(Mesh mesh, float[] times, PoseTrack.PoseFrame[] frames, double time) {
        if (mesh == null || times == null || times.length == 0 || frames == null || frames.length == 0) {
            return -1;
        }
        VertexBuffer bindPosition = mesh.getBuffer(Type.BindPosePosition);
        VertexBuffer position = mesh.getBuffer(Type.Position);
        if (bindPosition == null || position == null || bindPosition.getData() == null || position.getData() == null) {
            return -1;
        }
        return frameIndex(times, frames.length, time);
    }

    @Override
    public void evaluate(Mesh mesh, float[] times, PoseTrack.PoseFrame[] frames, double time) {
        if (times == null || times.length == 0 || frames == null || frames.length == 0) {
            return;
        }

        VertexBuffer bindPosition = mesh.getBuffer(Type.BindPosePosition);
        VertexBuffer position = mesh.getBuffer(Type.Position);
        if (bindPosition == null || position == null || bindPosition.getData() == null || position.getData() == null) {
            return;
        }

        FloatBuffer destination = (FloatBuffer) position.getData();
        FloatBuffer bindPose = (FloatBuffer) bindPosition.getData();
        destination.clear();
        bindPose.clear();
        destination.put(bindPose).clear();

        int frameIndex = frameIndex(times, frames.length, time);
        PoseTrack.PoseFrame frame = frames[frameIndex];
        if (frame != null && frame.poses != null && frame.weights != null) {
            int pairCount = Math.min(frame.poses.length / 2, frame.weights.length);
            for (int i = 0; i < pairCount; i++) {
                applyPose(frame.poses[i * 2], frame.poses[i * 2 + 1], frame.weights[i], destination);
            }
        }

        position.updateData(destination);
    }

    private static int frameIndex(float[] times, int frameCount, double time) {
        if (frameCount <= 1) {
            return 0;
        }
        if (Double.isNaN(time)) {
            return 1;
        }
        if (time < times[0]) {
            return 0;
        }
        int low = 0;
        int high = times.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (times[middle] < time) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return Math.max(1, Math.min(low, frameCount - 1));
    }

    private static void applyPose(Pose startPose, Pose endPose, float weight, FloatBuffer destination) {
        if (startPose == null || endPose == null) {
            return;
        }

        int[] indices = startPose.getIndices();
        Vector3f[] startOffsets = startPose.getOffsets();
        Vector3f[] endOffsets = endPose.getOffsets();
        int vertexCount = Math.min(indices.length, Math.min(startOffsets.length, endOffsets.length));
        for (int i = 0; i < vertexCount; i++) {
            int destinationIndex = indices[i] * 3;
            Vector3f start = startOffsets[i];
            Vector3f end = endOffsets[i];
            destination.put(destinationIndex, start.x + (end.x - start.x) * weight);
            destination.put(destinationIndex + 1, start.y + (end.y - start.y) * weight);
            destination.put(destinationIndex + 2, start.z + (end.z - start.z) * weight);
        }
    }
}
