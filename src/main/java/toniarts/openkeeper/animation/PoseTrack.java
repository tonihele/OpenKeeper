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
import com.jme3.bounding.BoundingBox;
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
    // Immutable source data may be shared by scene clones. Scratch/output remains owner-local.
    private transient VertexClip packedData;

    VertexClip packedData() { return packedData; }
    void preparePackedData(int availableBytes) {
        if (packedData == null) { packedData = VertexClip.bake(this, availableBytes); clearSampleCache(); }
    }
    public boolean isPackedSampling() { return packedData != null; }
    void discardPackedData() { packedData = null; clearSampleCache(); }

    @Override public void setTarget(Geometry target) {
        super.setTarget(target); packedData = null; clearSampleCache();
    }
    @Override public void setTimes(float[] times) {
        super.setTimes(times); packedData = null; clearSampleCache();
    }

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
                        result.poses[i] = this.poses[i] == null ? null : this.poses[i].clone();
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
        validateFrames();
        installAnimationBound();
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
    private transient Mesh sampledMesh;
    private transient FloatBuffer sampledBuffer;
    private transient float lastSampleTime = Float.NaN;
    private transient double pendingSampleTime = Double.NaN;

    private void validateFrames() {
        Mesh mesh = getTarget().getMesh();
        var position = mesh.getBuffer(Type.Position);
        var bind = mesh.getBuffer(Type.BindPosePosition);
        if (position == null || bind == null || position.getNumComponents() != 3 || bind.getNumComponents() != 3
                || !(position.getData() instanceof FloatBuffer positions) || !(bind.getData() instanceof FloatBuffer binds)
                || positions.limit() != binds.limit() || positions.limit() % 3 != 0) {
            throw new IllegalArgumentException("Pose position and bind buffers must have matching XYZ layouts");
        }
        if (frames == null || frames.length != getTimes().length) {
            throw new IllegalArgumentException("Pose frame count must match key times");
        }
        float previous = Float.NEGATIVE_INFINITY;
        for (float time : getTimes()) {
            if (!Float.isFinite(time) || time <= previous) throw new IllegalArgumentException("Invalid pose key times");
            previous = time;
        }
        int vertexCount = getTarget().getMesh().getVertexCount();
        for (PoseFrame frame : frames) {
            if (frame == null || frame.poses == null || frame.weights == null
                    || frame.poses.length % 2 != 0 || frame.weights.length != frame.poses.length / 2) {
                throw new IllegalArgumentException("Invalid pose frame layout");
            }
            for (int pair = 0; pair < frame.weights.length; pair++) {
                Pose a = frame.poses[pair * 2], b = frame.poses[pair * 2 + 1];
                if (a == null && b == null) continue; // Unused entries in historical converters.
                if (a == null || b == null || a.getIndices() == null || b.getIndices() == null
                        || a.getOffsets() == null || b.getOffsets() == null
                        || !java.util.Arrays.equals(a.getIndices(), b.getIndices())
                        || a.getIndices().length != a.getOffsets().length
                        || b.getIndices().length != b.getOffsets().length) {
                    throw new IllegalArgumentException("Pose pairs must address identical vertices");
                }
                for (int i = 0; i < a.getIndices().length; i++) {
                    if (a.getIndices()[i] < 0 || a.getIndices()[i] >= vertexCount
                            || !Vector3f.isValidVector(a.getOffsets()[i]) || !Vector3f.isValidVector(b.getOffsets()[i])) {
                        throw new IllegalArgumentException("Invalid pose vertex or coordinate");
                    }
                }
                // Old J3Os encode 0/0 for terminal self-pairs. Only identical endpoints
                // have an unambiguous repair; other non-finite weights are corruption.
                if (!Float.isFinite(frame.weights[pair])
                        && !java.util.Arrays.equals(a.getOffsets(), b.getOffsets())) {
                    throw new IllegalArgumentException("Non-finite weight between distinct poses");
                }
                if (Float.isFinite(frame.weights[pair]) && (frame.weights[pair] < 0 || frame.weights[pair] > 1)) {
                    throw new IllegalArgumentException("Pose weight outside its key interval");
                }
            }
        }
    }

    /** A clip-wide envelope is valid for every linear interpolation of its frames. */
    private void installAnimationBound() {
        FloatBuffer bindPose = (FloatBuffer) getTarget().getMesh().getBuffer(Type.BindPosePosition).getData();
        float[] decoded = new float[bindPose.limit()];
        Vector3f min = new Vector3f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        for (int frame = 0; frame < frames.length; frame++) {
            decodeFrame(frame, decoded, bindPose);
            for (int i = 0; i < decoded.length; i += 3) {
                float x = decoded[i], y = decoded[i + 1], z = decoded[i + 2];
                if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
                    throw new IllegalArgumentException("Non-finite decoded pose coordinate");
                }
                min.x = Math.min(min.x, x); min.y = Math.min(min.y, y); min.z = Math.min(min.z, z);
                max.x = Math.max(max.x, x); max.y = Math.max(max.y, y); max.z = Math.max(max.z, z);
            }
        }
        if (decoded.length != 0) {
            // Include exact boundary vertices despite center/extent rounding and
            // BoundingBox.contains() using strict comparisons at its surfaces.
            float magnitude = Math.max(Math.max(Math.abs(min.x), Math.abs(max.x)),
                    Math.max(Math.max(Math.abs(min.y), Math.abs(max.y)), Math.max(Math.abs(min.z), Math.abs(max.z))));
            float padding = Math.max(1e-5f, magnitude * 1e-6f);
            min.subtractLocal(padding, padding, padding); max.addLocal(padding, padding, padding);
            getTarget().setModelBound(new BoundingBox(min, max));
        }
    }

    /** Copies one decoded source key without changing live pose, bounds, collision data or sampler caches. */
    public void copyFrame(int index, float[] destination) {
        FloatBuffer bind = (FloatBuffer)getTarget().getMesh().getBuffer(Type.BindPosePosition).getData();
        if (index < 0 || index >= frames.length || destination == null || destination.length != bind.limit())
            throw new IllegalArgumentException("Expected valid source key and matching XYZ destination");
        decodeFrame(index, destination, bind);
    }

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
            float weight = Float.isFinite(frame.weights[pair]) ? frame.weights[pair] : 0f;
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
        if (!Double.isFinite(time)) throw new IllegalArgumentException("Pose time must be finite");
        if (frames == null || frames.length == 0) {
            return;
        }
        // Explicitly hidden subtrees cannot be visible to any render camera.
        // Do not use last-frame frustum results: shadows/other cameras may need us.
        for (com.jme3.scene.Spatial node = getTarget(); node != null; node = node.getParent()) {
            if (node.getCullHint() == com.jme3.scene.Spatial.CullHint.Always) {
                pendingSampleTime = time;
                return;
            }
        }
        pendingSampleTime = Double.NaN;
        Mesh mesh = getTarget().getMesh();
        FloatBuffer bindPose = (FloatBuffer) mesh.getBuffer(Type.BindPosePosition).getData();
        VertexBuffer position = mesh.getBuffer(Type.Position);
        FloatBuffer vertices = (FloatBuffer) position.getData();
        if (mesh != sampledMesh || vertices != sampledBuffer) clearSampleCache();
        float[] times = getTimes();
        // MorphTrack.getLength() is lastTime - firstTime. Old converted KMF
        // caches start at 1/30; new/zero-origin tracks work without reconversion.
        float sampleTime = (float) Math.clamp(time + times[0], times[0], times[times.length - 1]);
        if (sampleTime == lastSampleTime) return;
        if (lowerVertices == null || lowerVertices.length != bindPose.limit()) {
            lowerVertices = new float[bindPose.limit()];
            upperVertices = packedData == null ? new float[bindPose.limit()] : null;
            lowerIndex = upperIndex = -1;
        }
        if (packedData != null) {
            packedData.sample(time,lowerVertices);
            vertices.put(0,lowerVertices,0,lowerVertices.length);
        } else {
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
        }
        position.updateData(vertices);
        mesh.clearCollisionData(); // Rebuild lazily if an animated surface is picked.
        sampledMesh = mesh;
        sampledBuffer = vertices;
        lastSampleTime = sampleTime;
    }

    private void clearSampleCache() {
        lowerVertices = upperVertices = null;
        lowerIndex = upperIndex = -1;
        sampledMesh = null;
        sampledBuffer = null;
        lastSampleTime = Float.NaN;
        pendingSampleTime = Double.NaN;
    }

    /** A hidden one-shot may have disabled its composer before becoming visible again. */
    public void refreshPendingPose() {
        if (!Double.isNaN(pendingSampleTime)) getDataAtTime(pendingSampleTime,null);
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
        packedData = null;

        InputCapsule in = i.getCapsule(this);
        Savable[] readSavableArray = in.readSavableArray("frames", null);
        if (readSavableArray != null) {
            frames = new PoseTrack.PoseFrame[readSavableArray.length];
            System.arraycopy(readSavableArray, 0, frames, 0, readSavableArray.length);
        }
        try {
            validateFrames();
            installAnimationBound();
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid pose animation", exception);
        }
        clearSampleCache();
    }
}
