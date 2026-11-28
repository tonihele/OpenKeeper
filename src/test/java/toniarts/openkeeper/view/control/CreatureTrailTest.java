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

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DK II's trail strips (0x5A14E0)
 */
class CreatureTrailTest {

    @Test
    void eachRunIsAStripThroughItsPointsOverTheFrames() {

        // A run of three points and a run of two, over three frames
        Mesh mesh = CreatureTrail.createMesh(frames(3, 5), new int[]{3, 2}, CreatureTrail.SPEED_BLUR);

        // Two quads a frame step for the first run, one for the second, two frame steps
        assertEquals((2 + 1) * 2 * 2, mesh.getTriangleCount());
        assertEquals(15, mesh.getVertexCount());
    }

    @Test
    void theNewestFrameIsTheBrightest() {
        Mesh mesh = CreatureTrail.createMesh(frames(CreatureTrail.FRAMES, 2), new int[]{2}, CreatureTrail.SPEED_BLUR);
        FloatBuffer colors = (FloatBuffer) mesh.getBuffer(VertexBuffer.Type.Color).getData();

        // (0, 0.35, 0.35) times half of (6 - age) / 6, nothing in red or alpha
        assertEquals(0, colors.get(0));
        assertEquals(0.35f * 0.5f, colors.get(1), 0.0001f);
        assertEquals(0.35f * 0.5f, colors.get(2), 0.0001f);
        assertEquals(0, colors.get(3));
        int oldest = (CreatureTrail.FRAMES - 1) * 2 * 4;
        assertEquals(0.35f * 0.5f / CreatureTrail.FRAMES, colors.get(oldest + 1), 0.0001f);
    }

    @Test
    void theAttackTrailIsMagenta() {
        Mesh mesh = CreatureTrail.createMesh(frames(CreatureTrail.FRAMES, 2), new int[]{2}, CreatureTrail.ATTACK);
        FloatBuffer colors = (FloatBuffer) mesh.getBuffer(VertexBuffer.Type.Color).getData();

        // (0.5, 0, 0.35) times half of (6 - age) / 6
        assertEquals(0.5f * 0.5f, colors.get(0), 0.0001f);
        assertEquals(0, colors.get(1));
        assertEquals(0.35f * 0.5f, colors.get(2), 0.0001f);
        assertEquals(0, colors.get(3));
    }

    @Test
    void oneFrameIsNoTrailYet() {
        assertEquals(0, CreatureTrail.createMesh(frames(1, 4), new int[]{4}, CreatureTrail.SPEED_BLUR).getTriangleCount());
    }

    @Test
    void aRunOfOnePointIsNoStrip() {
        assertEquals(2 * 2, CreatureTrail.createMesh(frames(3, 3), new int[]{1, 2}, CreatureTrail.SPEED_BLUR).getTriangleCount());
    }

    private static List<Vector3f[]> frames(int frames, int points) {
        List<Vector3f[]> history = new ArrayList<>();
        for (int frame = 0; frame < frames; frame++) {
            Vector3f[] positions = new Vector3f[points];
            for (int point = 0; point < points; point++) {
                positions[point] = new Vector3f(point, 0, frame);
            }
            history.add(positions);
        }
        return history;
    }

}
