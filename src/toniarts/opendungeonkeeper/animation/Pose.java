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
package toniarts.opendungeonkeeper.animation;

import com.jme3.export.*;
import com.jme3.math.Vector3f;
import java.io.IOException;

/**
 * A pose is a list of offsets that say where a mesh vertices should be for this
 * pose.
 */
public final class Pose implements Savable, Cloneable {

    private String name;
    private Vector3f[] offsets;
    private int[] indices;

    public Pose(String name, Vector3f[] offsets, int[] indices) {
        this.name = name;
        this.offsets = offsets;
        this.indices = indices;
    }

    /**
     * Serialization-only. Do not use.
     */
    public Pose() {
    }

    /**
     * Get the vertice offsets
     *
     * @return the vertice offsets
     */
    public Vector3f[] getOffsets() {
        return offsets;
    }

    /**
     * Get the vertice indices
     *
     * @return the vertice indices
     */
    public int[] getIndices() {
        return indices;
    }

    /**
     * This method creates a clone of the current object.
     *
     * @return a clone of the current object
     */
    @Override
    public Pose clone() {
        try {
            Pose result = (Pose) super.clone();
            result.indices = this.indices.clone();
            if (this.offsets != null) {
                result.offsets = new Vector3f[this.offsets.length];
                for (int i = 0; i < this.offsets.length; ++i) {
                    result.offsets[i] = this.offsets[i].clone();
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
        out.write(name, "name", "");
        out.write(offsets, "offsets", null);
        out.write(indices, "indices", null);
    }

    @Override
    public void read(JmeImporter i) throws IOException {
        InputCapsule in = i.getCapsule(this);
        name = in.readString("name", "");
        indices = in.readIntArray("indices", null);

        Savable[] readSavableArray = in.readSavableArray("offsets", null);
        if (readSavableArray != null) {
            offsets = new Vector3f[readSavableArray.length];
            System.arraycopy(readSavableArray, 0, offsets, 0, readSavableArray.length);
        }
    }
}
