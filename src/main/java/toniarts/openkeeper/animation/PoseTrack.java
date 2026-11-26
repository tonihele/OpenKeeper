/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package toniarts.openkeeper.animation;

import com.jme3.anim.MorphTrack;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.scene.Geometry;
import java.io.IOException;
import java.util.Objects;

/**
 * A single track of pose animation associated with a certain mesh.
 */
public final class PoseTrack extends MorphTrack {

    private static final int NO_EVALUATION_KEY = Integer.MIN_VALUE;

    private PoseTrack.PoseFrame[] frames;
    private transient PoseTrackEvaluator evaluator = PoseTrackEvaluator.standard();
    private transient int lastEvaluationKey = NO_EVALUATION_KEY;

    public static final class PoseFrame implements Savable, Cloneable {

        Pose[] poses;
        float[] weights;

        public PoseFrame(Pose[] poses, float[] weights) {
            this.poses = poses;
            this.weights = weights;
        }

        public Pose[] getPoses() {
            return poses;
        }

        public float[] getWeights() {
            return weights;
        }

        /**
         * Serialization-only. Do not use.
         */
        public PoseFrame() {
        }

        /**
         * This method creates a clone of the current object.
         *
         * @return a clone of the current object
         */
        @Override
        public PoseTrack.PoseFrame clone() throws CloneNotSupportedException {
            try {
                PoseTrack.PoseFrame result = (PoseTrack.PoseFrame) super.clone();
                result.weights = this.weights.clone();
                if (this.poses != null) {
                    result.poses = new Pose[this.poses.length];
                    for (int i = 0; i < this.poses.length; ++i) {
                        result.poses[i] = this.poses[i].clone();
                    }
                }
                return result;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }

        @Override
        public void write(JmeExporter e) throws IOException {
            OutputCapsule out = e.getCapsule(this);
            out.write(poses, "poses", null);
            out.write(weights, "weights", null);
        }

        @Override
        public void read(JmeImporter i) throws IOException {
            InputCapsule in = i.getCapsule(this);
            weights = in.readFloatArray("weights", null);

            Savable[] readSavableArray = in.readSavableArray("poses", null);
            if (readSavableArray != null) {
                poses = new Pose[readSavableArray.length];
                System.arraycopy(readSavableArray, 0, poses, 0, readSavableArray.length);
            }
        }
    }

    public PoseTrack(Geometry target, float[] times, PoseTrack.PoseFrame[] frames) {
        this(target, times, frames, PoseTrackEvaluator.standard());
    }

    /**
     * Creates a KMF pose track with a replaceable evaluator. Evaluators should
     * be stateless and safe for use by multiple tracks.
     *
     * @param target geometry updated by this track
     * @param times keyframe times in seconds
     * @param frames pose data aligned with {@code times}
     * @param evaluator the strategy used to evaluate the pose into the mesh
     */
    public PoseTrack(Geometry target, float[] times, PoseTrack.PoseFrame[] frames, PoseTrackEvaluator evaluator) {
        super(target, times, new float[times.length], 1);
        this.frames = frames;
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
    }

    /**
     * Serialization-only. Do not use.
     */
    public PoseTrack() {
    }

    @Override
    public void getDataAtTime(double time, float[] store) {
        PoseTrackEvaluator currentEvaluator = evaluator;
        if (currentEvaluator == null) {
            currentEvaluator = PoseTrackEvaluator.standard();
            evaluator = currentEvaluator;
        }
        var mesh = getTarget().getMesh();
        int evaluationKey = currentEvaluator.evaluationKey(mesh, getTimes(), frames, time);
        if (evaluationKey >= 0 && evaluationKey == lastEvaluationKey) {
            return;
        }
        currentEvaluator.evaluate(mesh, getTimes(), frames, time);
        lastEvaluationKey = evaluationKey;
    }

    /**
     * This method creates a clone of the current object.
     *
     * @return a clone of the current object
     */
    @Override
    public PoseTrack clone() {
        try {
            PoseTrack result = (PoseTrack) super.clone();
            //result.getTimes() = this.getTimes().clone();
            if (this.frames != null) {
                result.frames = new PoseTrack.PoseFrame[this.frames.length];
                for (int i = 0; i < this.frames.length; ++i) {
                    result.frames[i] = this.frames[i].clone();
                }
            }
            result.lastEvaluationKey = NO_EVALUATION_KEY;
            return result;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public Object jmeClone() {
        try {
            PoseTrack clone = (PoseTrack) super.clone();
            clone.lastEvaluationKey = NO_EVALUATION_KEY;
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);

        OutputCapsule out = e.getCapsule(this);
        out.write(frames, "frames", null);
    }

    @Override
    public void read(JmeImporter i) throws IOException {
        super.read(i);

        evaluator = PoseTrackEvaluator.standard();
        lastEvaluationKey = NO_EVALUATION_KEY;

        InputCapsule in = i.getCapsule(this);
        Savable[] readSavableArray = in.readSavableArray("frames", null);
        if (readSavableArray != null) {
            frames = new PoseTrack.PoseFrame[readSavableArray.length];
            System.arraycopy(readSavableArray, 0, frames, 0, readSavableArray.length);
        }
    }
}
