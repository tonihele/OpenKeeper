/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.animation;

import com.jme3.scene.Mesh;

/**
 * Strategy for evaluating one KMF pose frame into a jME mesh.
 * Implementations must be stateless and thread-safe because asset tracks may
 * share an evaluator. The default implementation preserves the KMF frame
 * selection and bind-pose reset behavior.
 */
@FunctionalInterface
public interface PoseTrackEvaluator {

    /**
     * Returns a non-negative key when multiple clip times produce identical
     * mesh output. A negative result disables pose caching for this evaluator.
     * Custom evaluators should opt in only when their input data is immutable
     * and the mesh has not been modified outside this track.
     *
     * @param mesh the mesh whose position buffer is updated
     * @param times sorted keyframe times in seconds
     * @param frames pose data aligned with the keyframe times
     * @param time clip time in seconds
     * @return a stable output key, or a negative value when caching is unsafe
     */
    default int evaluationKey(Mesh mesh, float[] times, PoseTrack.PoseFrame[] frames, double time) {
        return -1;
    }

    /**
     * Evaluates the pose at the requested clip time.
     *
     * @param mesh the mesh whose position buffer is updated
     * @param times sorted keyframe times in seconds
     * @param frames pose data aligned with the keyframe times
     * @param time clip time in seconds
     */
    void evaluate(Mesh mesh, float[] times, PoseTrack.PoseFrame[] frames, double time);

    /** Returns the built-in CPU evaluator used for KMF animations. */
    static PoseTrackEvaluator standard() {
        return DefaultPoseTrackEvaluator.INSTANCE;
    }
}
