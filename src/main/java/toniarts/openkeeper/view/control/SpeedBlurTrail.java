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
package toniarts.openkeeper.view.control;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import toniarts.openkeeper.tools.convert.KmfModelLoader;

/**
 * DK II's speed blur (0x5A3AA0, 0x5A14E0). While the creature has its speed
 * blur, the last 6 drawn positions of the speed blur points of its animation
 * are kept, and each run of points is drawn as a strip through them. The strip
 * is added to the scene in (0, 0.35, 0.35) times half of (frames - age) /
 * frames, so the newest edge is the brightest. Without the blur, or when the
 * creature wasn't drawn, the positions are dropped
 */
final class SpeedBlurTrail {

    /**
     * How many drawn frames the trail goes back
     */
    static final int FRAMES = 6;
    private static final ColorRGBA COLOR = new ColorRGBA(0, 0.35f, 0.35f, 0);

    private final Geometry geometry;
    private final List<Vector3f[]> history = new ArrayList<>(FRAMES);
    private Node animation;
    private int[][] points;
    private int[] runLengths;

    SpeedBlurTrail(AssetManager assetManager) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setBoolean("VertexColor", true);
        RenderState renderState = material.getAdditionalRenderState();
        renderState.setBlendMode(RenderState.BlendMode.Additive);
        renderState.setDepthWrite(false);
        renderState.setFaceCullMode(RenderState.FaceCullMode.Off);

        geometry = new Geometry("SpeedBlur", new Mesh());
        geometry.setMaterial(material);
        geometry.setQueueBucket(RenderQueue.Bucket.Transparent);
        geometry.setShadowMode(RenderQueue.ShadowMode.Off);
    }

    /**
     * Takes the positions of this frame and redraws the trail
     *
     * @param creature the creature model
     * @param blur whether the creature has its speed blur
     */
    void update(Spatial creature, boolean blur) {
        Node shown = blur && creature.getParent() != null && creature.getCullHint() != Spatial.CullHint.Always
                ? findAnimation(creature) : null;
        if (shown != animation) {
            history.clear();
            animation = shown;
            if (animation != null) {
                parsePoints(animation.getUserData(KmfModelLoader.SPEED_BLUR_POINTS));
            }
        }
        if (animation == null) {
            geometry.removeFromParent();
            return;
        }

        Node parent = creature.getParent();
        Vector3f[] positions = new Vector3f[points.length];
        for (int i = 0; i < points.length; i++) {
            Geometry part = (Geometry) animation.getChild(points[i][0]);
            FloatBuffer buffer = (FloatBuffer) part.getMesh().getBuffer(VertexBuffer.Type.Position).getData();
            int index = points[i][1] * 3;
            Vector3f world = part.localToWorld(new Vector3f(buffer.get(index), buffer.get(index + 1), buffer.get(index + 2)), null);
            positions[i] = parent.worldToLocal(world, null);
        }
        history.addFirst(positions);
        if (history.size() > FRAMES) {
            history.removeLast();
        }

        if (geometry.getParent() != parent) {
            parent.attachChild(geometry);
        }
        geometry.setMesh(createMesh(history, runLengths));
    }

    /**
     * Takes the trail off the scene
     */
    void remove() {
        geometry.removeFromParent();
        history.clear();
        animation = null;
    }

    /**
     * Builds the strips of the trail
     *
     * @param history the positions of the points, the newest frame first
     * @param runLengths how many points each run has, in the order of the
     * points
     * @return the mesh
     */
    static Mesh createMesh(List<Vector3f[]> history, int[] runLengths) {
        int frames = history.size();
        int count = frames > 0 ? history.getFirst().length : 0;
        Vector3f[] positions = new Vector3f[frames * count];
        ColorRGBA[] colors = new ColorRGBA[frames * count];
        for (int frame = 0; frame < frames; frame++) {
            ColorRGBA color = COLOR.mult((frames - frame) / (float) frames * 0.5f);
            for (int point = 0; point < count; point++) {
                positions[frame * count + point] = history.get(frame)[point];
                colors[frame * count + point] = color;
            }
        }

        List<Integer> indices = new ArrayList<>();
        int start = 0;
        for (int runLength : runLengths) {
            for (int frame = 0; frame < frames - 1; frame++) {
                for (int point = start; point < start + runLength - 1; point++) {
                    int corner = frame * count + point;
                    indices.addAll(Arrays.asList(corner, corner + 1, corner + count + 1, corner, corner + count + 1, corner + count));
                }
            }
            start += runLength;
        }

        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(positions));
        mesh.setBuffer(VertexBuffer.Type.Color, 4, BufferUtils.createFloatBuffer(colors));
        mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(indices.stream().mapToInt(Integer::intValue).toArray()));
        mesh.updateBound();
        return mesh;
    }

    private static Node findAnimation(Spatial creature) {
        Node[] found = new Node[1];
        creature.depthFirstTraversal(spatial -> {
            if (found[0] == null && spatial instanceof Node node && node.getUserData(KmfModelLoader.SPEED_BLUR_POINTS) != null
                    && node.getCullHint() != Spatial.CullHint.Always) {
                found[0] = node;
            }
        });
        return found[0];
    }

    private void parsePoints(String data) {
        String[] runs = data.split(";");
        List<int[]> parsed = new ArrayList<>();
        runLengths = new int[runs.length];
        for (int run = 0; run < runs.length; run++) {
            String[] runPoints = runs[run].split(",");
            runLengths[run] = runPoints.length;
            for (String point : runPoints) {
                String[] parts = point.split(":");
                parsed.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
            }
        }
        points = parsed.toArray(new int[0][]);
    }

}
