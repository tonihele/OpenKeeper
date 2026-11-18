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
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import java.io.IOException;
import java.nio.FloatBuffer;

/**
 * A single track of pose animation associated with a certain mesh.
 */
public final class PoseTrack extends MorphTrack {

    private PoseTrack.PoseFrame[] frames;

    public static final class PoseFrame implements Savable, Cloneable {

        Pose[] poses;
        float[] weights;

        public PoseFrame(Pose[] poses, float[] weights) {
            this.poses = poses;
            this.weights = weights;
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

    public PoseTrack(Geometry target, float[] times, PoseTrack.PoseFrame[] frames)
    {
        super(target, times, new float[times.length], 1);
        this.frames = frames;
    }

    /**
     * Serialization-only. Do not use.
     */
    public PoseTrack() {
    }

    // Reused per track instance. Sparse pose pairs may address different vertex
    // sets in adjacent frames, so blend decoded vertex positions, not pose arrays.
    private transient float[] lowerVertices;
    private transient float[] upperVertices;
    private transient int lowerIndex = -1;
    private transient int upperIndex = -1;

    private void decodeFrame(int index, float[] vertices, FloatBuffer bindPose) {
        for (int i = 0; i < vertices.length; i++) {
            vertices[i] = bindPose.get(i);
        }
        PoseFrame frame = frames[index];
        for (int pair = 0; pair < frame.poses.length / 2; pair++) {
            Pose start = frame.poses[pair * 2];
            Pose end = frame.poses[pair * 2 + 1];
            if (start == null || end == null) {
                continue;
            }
            float weight = frame.weights[pair];
            for (int i = 0; i < start.getIndices().length; i++) {
                int vertex = start.getIndices()[i] * 3;
                Vector3f a = start.getOffsets()[i];
                Vector3f b = end.getOffsets()[i];
                vertices[vertex] = a.x + (b.x - a.x) * weight;
                vertices[vertex + 1] = a.y + (b.y - a.y) * weight;
                vertices[vertex + 2] = a.z + (b.z - a.z) * weight;
            }
        }
    }

    @Override
    public void getDataAtTime(double time, float[] store) {
        if (frames == null || frames.length == 0) {
            return;
        }
        Mesh mesh = getTarget().getMesh();
        FloatBuffer bindPose = (FloatBuffer) mesh.getBuffer(Type.BindPosePosition).getData();
        VertexBuffer position = mesh.getBuffer(Type.Position);
        FloatBuffer vertices = (FloatBuffer) position.getData();
        if (lowerVertices == null || lowerVertices.length != bindPose.limit()) {
            lowerVertices = new float[bindPose.limit()];
            upperVertices = new float[bindPose.limit()];
            lowerIndex = upperIndex = -1;
        }
        float[] times = getTimes();
        // MorphTrack.getLength() is lastTime - firstTime. Old converted KMF
        // caches start at 1/30; new/zero-origin tracks work without reconversion.
        float sampleTime = (float) Math.clamp(time + times[0], times[0], times[times.length - 1]);
        int result = java.util.Arrays.binarySearch(times, sampleTime);
        int lower = result >= 0 ? result : Math.max(0, -result - 2);
        int upper = result >= 0 ? result : Math.min(times.length - 1, lower + 1);
        if (lowerIndex != lower) {
            decodeFrame(lower, lowerVertices, bindPose);
            lowerIndex = lower;
        }
        if (upperIndex != upper) {
            decodeFrame(upper, upperVertices, bindPose);
            upperIndex = upper;
        }
        float weight = upper == lower ? 0 : (sampleTime - times[lower]) / (times[upper] - times[lower]);
        for (int i = 0; i < vertices.limit(); i++) {
            vertices.put(i, lowerVertices[i] + (upperVertices[i] - lowerVertices[i]) * weight);
        }
        position.updateData(vertices);
    }

    private void clearSampleCache() {
        lowerVertices = upperVertices = null;
        lowerIndex = upperIndex = -1;
    }

    @Override
    public void cloneFields(com.jme3.util.clone.Cloner cloner, Object original) {
        super.cloneFields(cloner, original);
        clearSampleCache();
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
            result.clearSampleCache();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public Object jmeClone() {
        try {
            PoseTrack clone = (PoseTrack) super.clone();
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

        InputCapsule in = i.getCapsule(this);
        Savable[] readSavableArray = in.readSavableArray("frames", null);
        if (readSavableArray != null) {
            frames = new PoseTrack.PoseFrame[readSavableArray.length];
            System.arraycopy(readSavableArray, 0, frames, 0, readSavableArray.length);
        }
    }
}
