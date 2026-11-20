/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.view.effect.procedural;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Random;

/**
 * A lightning bolt from above, striking down at the origin. The bolt is a
 * jagged line in the local X-Y plane, drawn as ribbons along X, so a
 * {@link LightningBoltControl} turning it about Y to the camera makes it look
 * the same from every angle. It comes in two layers, a thin bright core and a
 * wide dim glow around it.
 * <p>
 * The line is re-rolled to make the bolt flicker, that only rewrites the
 * existing buffers.
 */
public final class LightningBoltMesh extends Mesh {

    /**
     * Across, a ribbon is three vertices: a faded edge, the bright centre and
     * another faded edge. With additive blending that reads as a glow.
     */
    static final int VERTICES_ACROSS = 3;

    static final float[] LAYER_HALF_WIDTHS = {0.03f, 0.14f};
    private static final ColorRGBA[] LAYER_COLORS = {
        new ColorRGBA(0.9f, 0.95f, 1f, 1f),
        new ColorRGBA(0.45f, 0.6f, 1f, 0.5f)
    };

    /**
     * How far one step up the bolt may wander sideways, as a share of the
     * jitter
     */
    private static final float STEP_SHARE_OF_JITTER = 0.6f;
    private static final float BOUND_MARGIN = 0.01f;

    /**
     * How many points at the top fade out, so that the bolt comes out of
     * nowhere instead of starting at a hard edge
     */
    private static final int FADING_POINTS_AT_TOP = 3;

    private final Vector3f[] path;
    private final float height;
    private final float jitter;
    private final FloatBuffer positions;
    private final FloatBuffer colors;

    /**
     * Creates a straight bolt, call {@link #randomizePath(Random)} to give it
     * its shape
     *
     * @param segments how many straight pieces the bolt is made of
     * @param height how far up the bolt starts
     * @param jitter how far the bolt may wander sideways from the vertical
     */
    public LightningBoltMesh(int segments, float height, float jitter) {
        this.height = height;
        this.jitter = jitter;

        path = new Vector3f[segments + 1];
        for (int i = 0; i < path.length; i++) {
            path[i] = new Vector3f(0, getHeightAt(i), 0);
        }

        int vertexCount = LAYER_HALF_WIDTHS.length * path.length * VERTICES_ACROSS;
        positions = BufferUtils.createFloatBuffer(vertexCount * 3);
        colors = BufferUtils.createFloatBuffer(vertexCount * 4);
        setBuffer(VertexBuffer.Type.Position, 3, positions);
        setBuffer(VertexBuffer.Type.Color, 4, colors);
        setBuffer(VertexBuffer.Type.Index, 3, createIndices(segments));
        getBuffer(VertexBuffer.Type.Position).setUsage(VertexBuffer.Usage.Stream);
        getBuffer(VertexBuffer.Type.Color).setUsage(VertexBuffer.Usage.Stream);

        setIntensity(1f);
        updatePositions();

        // The shape changes all the time, so use one bound that covers every shape the bolt can take, and a hair
        // more for rounding
        float reach = jitter + LAYER_HALF_WIDTHS[LAYER_HALF_WIDTHS.length - 1] + BOUND_MARGIN;
        setBound(new BoundingBox(new Vector3f(0, height / 2, 0), reach, height / 2 + reach, BOUND_MARGIN));
    }

    /**
     * Gives the bolt a new shape. It wanders sideways in small steps on its way
     * up, the bottom stays where it strikes.
     *
     * @param random the random to roll the shape with
     */
    public void randomizePath(Random random) {
        float step = jitter * STEP_SHARE_OF_JITTER;
        float x = 0;
        for (int i = 1; i < path.length; i++) {
            x = FastMath.clamp(x + (random.nextFloat() * 2 - 1) * step, -jitter, jitter);
            path[i].set(x, getHeightAt(i), 0);
        }
        updatePositions();
    }

    /**
     * Lays the ribbons along the line, each spreads sideways along X
     */
    private void updatePositions() {
        int vertex = 0;
        for (float halfWidth : LAYER_HALF_WIDTHS) {
            for (Vector3f point : path) {
                putPosition(vertex++, point.x - halfWidth, point.y, point.z);
                putPosition(vertex++, point.x, point.y, point.z);
                putPosition(vertex++, point.x + halfWidth, point.y, point.z);
            }
        }
        getBuffer(VertexBuffer.Type.Position).setUpdateNeeded();
    }

    /**
     * Sets how bright the bolt is
     *
     * @param intensity 0 is invisible, 1 is full brightness
     */
    public void setIntensity(float intensity) {
        int vertex = 0;
        int top = path.length - 1;
        for (ColorRGBA color : LAYER_COLORS) {
            for (int i = 0; i < path.length; i++) {
                float fade = Math.min(1f, (top - i) / (float) FADING_POINTS_AT_TOP);
                for (int across = 0; across < VERTICES_ACROSS; across++) {
                    float alpha = across == VERTICES_ACROSS / 2 ? color.a * intensity * fade : 0f;
                    putColor(vertex++, color.r, color.g, color.b, alpha);
                }
            }
        }
        getBuffer(VertexBuffer.Type.Color).setUpdateNeeded();
    }

    int getPointCount() {
        return path.length;
    }

    Vector3f getPoint(int index) {
        return path[index];
    }

    private float getHeightAt(int index) {
        return height * index / (path.length - 1);
    }

    private void putPosition(int vertex, float x, float y, float z) {
        int index = vertex * 3;
        positions.put(index, x).put(index + 1, y).put(index + 2, z);
    }

    private void putColor(int vertex, float r, float g, float b, float a) {
        int index = vertex * 4;
        colors.put(index, r).put(index + 1, g).put(index + 2, b).put(index + 3, a);
    }

    private static ShortBuffer createIndices(int segments) {
        int pointsPerLayer = segments + 1;
        ShortBuffer indices = BufferUtils.createShortBuffer(LAYER_HALF_WIDTHS.length * segments * 4 * 3);
        for (int layer = 0; layer < LAYER_HALF_WIDTHS.length; layer++) {
            int layerStart = layer * pointsPerLayer * VERTICES_ACROSS;
            for (int i = 0; i < segments; i++) {
                int below = layerStart + i * VERTICES_ACROSS;
                int above = below + VERTICES_ACROSS;

                // Two quads per segment, edge to centre and centre to edge
                for (int across = 0; across < VERTICES_ACROSS - 1; across++) {
                    putTriangle(indices, below + across, below + across + 1, above + across + 1);
                    putTriangle(indices, below + across, above + across + 1, above + across);
                }
            }
        }
        indices.flip();

        return indices;
    }

    private static void putTriangle(ShortBuffer indices, int a, int b, int c) {
        indices.put((short) a).put((short) b).put((short) c);
    }

}
