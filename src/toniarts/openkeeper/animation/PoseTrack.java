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

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.ClonableTrack;
import com.jme3.animation.Track;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import com.jme3.util.TempVars;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.nio.FloatBuffer;

/**
 * A single track of pose animation associated with a certain mesh.
 */
public final class PoseTrack implements Track, ClonableTrack {

    private int targetMeshIndex;
    private PoseTrack.PoseFrame[] frames;
    private float[] times;

    public static class PoseFrame implements Savable, Cloneable {

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
        public PoseTrack.PoseFrame clone() {
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

    public PoseTrack(int targetMeshIndex, float[] times, PoseTrack.PoseFrame[] frames) {
        this.targetMeshIndex = targetMeshIndex;
        this.times = times;
        this.frames = frames;
    }

    /**
     * Serialization-only. Do not use.
     */
    public PoseTrack() {
    }

    @Override
    public float[] getKeyFrameTimes() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Track cloneForSpatial(Spatial spatial) {
        PoseTrack track = new PoseTrack(targetMeshIndex, times, frames);
        return track;
    }

    @Override
    public void cleanUp() {

    }

    private void applyFrame(Mesh target, int frameIndex) {
        PoseFrame frame = frames[frameIndex];
        VertexBuffer pb = target.getBuffer(Type.Position);
        for (int i = 0; i < frame.poses.length / 2; i++) {

            // Poses come in pairs of two [startPose] + [endPose], weight tells us how close we are to the end
            // The pose pair must have the same vertices in the same order
            applyPose(frame.poses[i * 2], frame.poses[i * 2 + 1], frame.weights[i], (FloatBuffer) pb.getData());
        }

        // force to re-upload data to gpu
        pb.updateData(pb.getData());
    }

    /**
     * Applies pose for this frame
     *
     * @param startPose starting pose for the vertices
     * @param endPose ending pose for the vertices
     * @param weight weight on which to apply the interpolation
     * @param vertexBuffer the vertex buffer
     */
    private void applyPose(Pose startPose, Pose endPose, float weight, FloatBuffer vertexBuffer) {
        int[] startingIndices = startPose.getIndices();
        Vector3f interpOffset = new Vector3f();
        for (int i = 0; i < startingIndices.length; i++) {
            int vertIndex = startingIndices[i];
            Vector3f startOffset = startPose.getOffsets()[i];
            Vector3f endOffset = endPose.getOffsets()[i];

            // Interpolate
            interpOffset.set(endOffset);
            interpOffset.subtractLocal(startOffset);
            interpOffset.multLocal(weight);
            interpOffset.addLocal(startOffset);

            // Write modified vertex
            BufferUtils.setInBuffer(interpOffset, vertexBuffer, vertIndex);
        }
    }

    @Override
    public void setTime(float time, float weight, AnimControl control,
            AnimChannel channel, TempVars vars) {
        Spatial spat = control.getSpatial();
        Geometry geom = findGeom(spat);
        Mesh target = geom.getMesh();

        VertexBuffer bindPos = target.getBuffer(Type.BindPosePosition);
        VertexBuffer pos = target.getBuffer(Type.Position);
        FloatBuffer pb = (FloatBuffer) pos.getData();
        FloatBuffer bpb = (FloatBuffer) bindPos.getData();
        pb.clear();
        bpb.clear();
        pb.put(bpb).clear();

        if (time < times[0]) {
            applyFrame(target, 0);
        } else if (time > times[times.length - 1]) {
            applyFrame(target, times.length - 1);
        } else {
            int startFrame = 0;
            for (int i = 0; i < times.length; i++) {
                if (times[i] < time) {
                    startFrame = i;
                }
            }

            int endFrame = startFrame + 1;
            applyFrame(target, endFrame);
        }
    }

    public Geometry findGeom(Spatial spatial) {
        if (spatial instanceof Node) {
            Node findingnode = (Node) spatial;
            Spatial child = findingnode.getChild(targetMeshIndex);
            Geometry result = findGeom(child);
            if (result != null) {
                return result;
            }
        } else if (spatial instanceof Geometry) {
            return (Geometry) spatial;
        }
        return null;
    }

    /**
     * @return the length of the track
     */
    @Override
    public float getLength() {
        return times == null ? 0 : times[times.length - 1] - times[0];
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
            result.times = this.times.clone();
            if (this.frames != null) {
                result.frames = new PoseTrack.PoseFrame[this.frames.length];
                for (int i = 0; i < this.frames.length; ++i) {
                    result.frames[i] = this.frames[i].clone();
                }
            }
            return result;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public Object jmeClone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Error cloning", e);
        }
    }

    @Override
    public void cloneFields(Cloner cloner, Object o) {
        PoseTrack poseTrack = (PoseTrack) o;
        this.times = poseTrack.times;
        this.frames = poseTrack.frames;
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        OutputCapsule out = e.getCapsule(this);
        out.write(targetMeshIndex, "meshIndex", 0);
        out.write(frames, "frames", null);
        out.write(times, "times", null);
    }

    @Override
    public void read(JmeImporter i) throws IOException {
        InputCapsule in = i.getCapsule(this);
        targetMeshIndex = in.readInt("meshIndex", 0);
        times = in.readFloatArray("times", null);

        Savable[] readSavableArray = in.readSavableArray("frames", null);
        if (readSavableArray != null) {
            frames = new PoseTrack.PoseFrame[readSavableArray.length];
            System.arraycopy(readSavableArray, 0, frames, 0, readSavableArray.length);
        }
    }
}
