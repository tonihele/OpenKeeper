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
package toniarts.openkeeper.view.map;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.FloatBuffer;
import java.util.function.BiFunction;
import toniarts.openkeeper.tools.convert.map.Terrain;

/**
 * Bends the (tile exact) terrain and room meshes so they look carved by hand,
 * like the original game does with the {@code wibble} and {@code lean} values
 * of the terrain rows.
 * <p>
 * A smooth displacement field is sampled from a lattice with a point at every
 * tile corner, edge middle and centre (half tile spacing) and 5 height levels:
 * below the floor, floor, middle, wall top and above the top. Only the 3
 * middle levels move. Every vertex goes through the same field by its world
 * position, so pieces that meet at a vertex still meet after the deformation
 * and no stitching is needed. It is purely visual, the game logic never reads
 * it.
 * <ul>
 * <li><b>lean</b> pushes the points on a solid/open boundary into the solid
 * side, so the walls slope back from corridors and rooms,</li>
 * <li><b>wibble</b> is a seeded jitter that is shifted into the solid side, so
 * wall bumps never intrude into walkable tiles.</li>
 * </ul>
 * The lattice values are computed lazily and are a pure function of the tiles
 * around, so a rebuilt tile always comes out identical. Call
 * {@link #invalidateTile} when a tile changes (including it getting explored).
 * <p>
 * World frame: tile (x, y) covers x - 0.5 .. x + 0.5 and z (y) alike, the floor
 * is at y = 1 ({@link toniarts.openkeeper.utils.WorldUtils#FLOOR_HEIGHT}) and
 * the top of the wall at y = 2.
 */
public final class TerrainDeformer {

    private static final Logger logger = System.getLogger(TerrainDeformer.class.getName());

    /**
     * What the deformation needs to know about a tile. The values are in
     * tiles, the lean arrays are indexed top, middle, floor.
     */
    public record TileShape(boolean solid, float wibbleH, float wibbleV, float[] leanH, float[] leanV) {

        private static final float[] NO_LEAN = new float[3];

        /**
         * Reads the terrain row. Same scale as the original: the raw wibble
         * byte is 1/1024 tiles, the lean byte 1/512 tiles.
         */
        public static TileShape of(Terrain terrain) {
            return new TileShape(terrain.getFlags().contains(Terrain.TerrainFlag.SOLID),
                    terrain.getWibbleH() / 1024f, terrain.getWibbleV() / 1024f,
                    scale(terrain.getLeanH()), scale(terrain.getLeanV()));
        }

        private static float[] scale(short[] raw) {
            float[] result = new float[3];
            for (int i = 0; i < result.length && raw != null && i < raw.length; i++) {
                result[i] = raw[i] / 512f;
            }
            return result;
        }
    }

    /**
     * Whatever is outside of the map: solid, the default wibble of the game
     * rules and no lean, so the edge of the map stays straight.
     */
    private static final TileShape OFF_MAP = new TileShape(true, 0.1875f, 0.094f, TileShape.NO_LEAN, TileShape.NO_LEAN);

    private static final float DIAGONAL = 1.4f; // ~sqrt(2)
    private static final float SWELL_BASE = 0.15f;
    private static final float SWELL_AMPLITUDE = 0.125f;

    // The values of the original look a bit too much here, tone each down. All of them can be
    // scaled further with -Dopenkeeper.deformScale=<factor>, 0 is flat.
    private static final float STRENGTH = Float.parseFloat(System.getProperty("openkeeper.deformScale", "1"));
    private static final float WIBBLE_SCALE = 0.6f * STRENGTH;
    private static final float LEAN_SCALE = 0.5f * STRENGTH;
    private static final float SWELL_SCALE = 0.5f * STRENGTH;
    private static final float FLOOR_Y = 1f; // Rest height of lattice level 1, the levels are half a tile apart
    private static final int LEVELS = 3; // The ones that move
    private static final int SEED = 0x4F4B3250;

    private final int width;
    private final int height;
    private final int pointsX;
    private final int pointsY;
    private final BiFunction<Integer, Integer, TileShape> tiles;
    private final float[] displacements; // x, y, z for each of the levels, for each point
    private final boolean[] valid;

    /**
     * @param width map width in tiles
     * @param height map height in tiles
     * @param tiles resolves the shape of an in-bounds tile as the viewer sees
     * it (unexplored tiles being solid)
     */
    public TerrainDeformer(int width, int height, BiFunction<Integer, Integer, TileShape> tiles) {
        this.width = width;
        this.height = height;
        this.pointsX = 2 * width + 1;
        this.pointsY = 2 * height + 1;
        this.tiles = tiles;
        this.displacements = new float[pointsX * pointsY * LEVELS * 3];
        this.valid = new boolean[pointsX * pointsY];
    }

    /**
     * A tile changed, forget the lattice points it can affect (7 x 7)
     */
    public void invalidateTile(int x, int y) {
        for (int j = Math.max(2 * y - 2, 0); j <= Math.min(2 * y + 4, pointsY - 1); j++) {
            for (int i = Math.max(2 * x - 2, 0); i <= Math.min(2 * x + 4, pointsX - 1); i++) {
                valid[j * pointsX + i] = false;
            }
        }
    }

    public void invalidateAll() {
        java.util.Arrays.fill(valid, false);
    }

    /**
     * Moves a world position through the field
     *
     * @param p the position
     * @param result the deformed position, may be {@code p}
     * @param jacobian where to store the derivative of the deformation (for
     * the normals), may be {@code null}
     */
    public void deform(Vector3f p, Vector3f result, Matrix3f jacobian) {
        float u = 2 * (p.x + 0.5f);
        float v = 2 * (p.z + 0.5f);
        float w = 2 * (p.y - FLOOR_Y) + 1; // Level 1 is the floor
        if (jacobian != null) {
            jacobian.loadIdentity();
        }
        if (u < 0 || v < 0 || u > pointsX - 1 || v > pointsY - 1 || w < 0 || w >= 4) {
            result.set(p);
            return;
        }

        int i0 = Math.min((int) u, pointsX - 2);
        int j0 = Math.min((int) v, pointsY - 2);
        int k0 = Math.min((int) w, 3);
        float fu = u - i0;
        float fv = v - j0;
        float fw = w - k0;

        float dx = 0, dy = 0, dz = 0;
        float[] jac = jacobian != null ? new float[9] : null;
        for (int b = 0; b < 2; b++) {
            for (int a = 0; a < 2; a++) {
                ensure(i0 + a, j0 + b);
                float lu = a == 0 ? 1 - fu : fu;
                float lv = b == 0 ? 1 - fv : fv;
                float du = a == 0 ? -1 : 1;
                float dv = b == 0 ? -1 : 1;
                for (int c = 0; c < 2; c++) {
                    int level = k0 + c;
                    if (level < 1 || level > LEVELS) {
                        continue; // The anchors don't move
                    }
                    float lw = c == 0 ? 1 - fw : fw;
                    float dw = c == 0 ? -1 : 1;
                    float weight = lu * lv * lw;
                    int index = ((j0 + b) * pointsX + (i0 + a)) * LEVELS * 3 + (level - 1) * 3;
                    float px = displacements[index];
                    float py = displacements[index + 1];
                    float pz = displacements[index + 2];
                    dx += weight * px;
                    dy += weight * py;
                    dz += weight * pz;
                    if (jac != null) {
                        // d/dx = 2 d/du, d/dy = 2 d/dw, d/dz = 2 d/dv
                        float gx = 2 * du * lv * lw;
                        float gy = 2 * lu * lv * dw;
                        float gz = 2 * lu * dv * lw;
                        jac[0] += gx * px;
                        jac[1] += gy * px;
                        jac[2] += gz * px;
                        jac[3] += gx * py;
                        jac[4] += gy * py;
                        jac[5] += gz * py;
                        jac[6] += gx * pz;
                        jac[7] += gy * pz;
                        jac[8] += gz * pz;
                    }
                }
            }
        }
        if (jacobian != null) {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    jacobian.set(r, c, jacobian.get(r, c) + jac[r * 3 + c]);
                }
            }
        }
        result.set(p.x + dx, p.y + dy, p.z + dz);
    }

    /**
     * Deforms all the geometries under the spatial, in place. The vertices
     * are read in the frame of {@code space}, i.e. the transforms of
     * {@code space} and above are ignored, if that is {@code null} all the
     * parents count. The meshes are copied first, the models come from a
     * cache that shares them.
     * <p>
     * Do this to a piece once, before it gets batched.
     *
     * @param spatial the thing to deform, already in its place
     * @param space an ancestor in whose frame the lattice is
     */
    public void apply(Spatial spatial, Spatial space) {
        if (spatial instanceof Geometry geometry) {
            try {
                deformGeometry(geometry, space);
            } catch (RuntimeException e) {
                // It is only cosmetic, don't lose the whole tile or room over it
                logger.log(Level.WARNING, "Failed to deform geometry " + geometry.getName() + "!", e);
            }
        } else if (spatial instanceof com.jme3.scene.Node node) {
            for (Spatial child : node.getChildren()) {
                apply(child, space);
            }
        }
    }

    private void deformGeometry(Geometry geometry, Spatial space) {
        Mesh original = geometry.getMesh();
        VertexBuffer positionBuffer = original.getBuffer(Type.Position);
        if (positionBuffer == null || positionBuffer.getFormat() != VertexBuffer.Format.Float
                || positionBuffer.getNumComponents() != 3) {
            return;
        }

        // Frame of the vertices in the lattice space
        Transform toSpace = geometry.getLocalTransform().clone();
        for (Spatial parent = geometry.getParent(); parent != null && parent != space; parent = parent.getParent()) {
            toSpace.combineWithParent(parent.getLocalTransform());
        }

        if (geometry.isGrouped()) {
            geometry.unassociateFromGroupNode();
        }
        Mesh mesh = original.deepClone();
        geometry.setMesh(mesh);

        FloatBuffer positions = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
        FloatBuffer normals = vectorBuffer(mesh, Type.Normal);
        FloatBuffer tangents = vectorBuffer(mesh, Type.Tangent);

        Vector3f scale = toSpace.getScale();
        Vector3f inverseScale = new Vector3f(1 / scale.x, 1 / scale.y, 1 / scale.z);
        Matrix3f rotation = toSpace.getRotation().toRotationMatrix();
        Matrix3f inverseRotation = rotation.transpose();

        Vector3f local = new Vector3f();
        Vector3f world = new Vector3f();
        Vector3f moved = new Vector3f();
        Vector3f vector = new Vector3f();
        Matrix3f jacobian = new Matrix3f();
        Matrix3f normalMatrix = new Matrix3f();
        int count = positions.limit() / 3;
        for (int i = 0; i < count; i++) {
            local.set(positions.get(3 * i), positions.get(3 * i + 1), positions.get(3 * i + 2));
            toSpace.transformVector(local, world);
            deform(world, moved, jacobian);
            toSpace.transformInverseVector(moved, local);
            positions.put(3 * i, local.x).put(3 * i + 1, local.y).put(3 * i + 2, local.z);

            if (normals != null) {
                // A normal goes through the inverse transpose of the field
                vector.set(normals.get(3 * i), normals.get(3 * i + 1), normals.get(3 * i + 2));
                rotation.mult(vector.multLocal(inverseScale), vector);
                normalMatrix.set(jacobian).invertLocal().transposeLocal();
                normalMatrix.mult(vector, vector);
                inverseRotation.mult(vector, vector).multLocal(scale).normalizeLocal();
                normals.put(3 * i, vector.x).put(3 * i + 1, vector.y).put(3 * i + 2, vector.z);
            }
            if (tangents != null) {
                // And a tangent through the field itself
                vector.set(tangents.get(4 * i), tangents.get(4 * i + 1), tangents.get(4 * i + 2));
                rotation.mult(vector.multLocal(scale), vector);
                jacobian.mult(vector, vector);
                inverseRotation.mult(vector, vector).multLocal(inverseScale).normalizeLocal();
                tangents.put(4 * i, vector.x).put(4 * i + 1, vector.y).put(4 * i + 2, vector.z);
            }
        }

        mesh.getBuffer(Type.Position).updateData(positions);
        if (normals != null) {
            mesh.getBuffer(Type.Normal).updateData(normals);
        }
        if (tangents != null) {
            mesh.getBuffer(Type.Tangent).updateData(tangents);
        }
        mesh.updateBound();
        mesh.updateCounts();
        geometry.updateModelBound();
    }

    private static FloatBuffer vectorBuffer(Mesh mesh, Type type) {
        VertexBuffer buffer = mesh.getBuffer(type);
        if (buffer == null || buffer.getFormat() != VertexBuffer.Format.Float
                || buffer.getNumComponents() != (type == Type.Tangent ? 4 : 3)) {
            return null;
        }
        return (FloatBuffer) buffer.getData();
    }

    private void ensure(int i, int j) {
        if (!valid[j * pointsX + i]) {
            computePoint(i, j);
            valid[j * pointsX + i] = true;
        }
    }

    private TileShape tile(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return OFF_MAP;
        }
        return tiles.apply(x, y);
    }

    private void computePoint(int i, int j) {
        float[] params = new float[2 + 2 * LEVELS]; // wibble H, V, lean H and V per level
        touchingMinimum(i, j, params);
        float wibbleH = params[0] * WIBBLE_SCALE;
        float wibbleV = params[1] * WIBBLE_SCALE;

        Vector3f direction = new Vector3f();
        Vector3f lean = new Vector3f();
        int base = (j * pointsX + i) * LEVELS * 3;
        for (int level = 1; level <= LEVELS; level++) {
            int index = 3 - level; // Lean is top, middle, floor
            float x = 0, y = 0, z = 0;
            boolean pinned = wibbleH == 0 && wibbleV == 0 && params[2 + index] == 0 && params[2 + LEVELS + index] == 0;
            if (!pinned) {
                lean(i, j, level, lean);
                lean.multLocal(LEAN_SCALE);
                solidDirection(i, j, direction);
                x = lean.x;
                y = lean.y;
                z = lean.z;

                // The jitter is centered and then shifted into the solid side
                x += (random(i, j, level, 0) - 0.5f) * wibbleH + direction.x * wibbleH / 2;
                z += (random(i, j, level, 1) - 0.5f) * wibbleH + direction.z * wibbleH / 2;
                y -= random(i, j, level, 2) * wibbleV;

                // The tops of the rock are not a flat plane
                if (level == 3) {
                    y += SWELL_SCALE * (SWELL_BASE + SWELL_AMPLITUDE * FastMath.sin((i + j) / 2f));
                }
            }
            int at = base + (level - 1) * 3;
            displacements[at] = x;
            displacements[at + 1] = y;
            displacements[at + 2] = z;
        }
    }

    /**
     * The smallest wibble and lean of the tiles that touch the point, so the
     * open tile decides how much the adjacent rock recedes (the rock has a
     * lot) and a tile with zeroes pins its points.
     */
    private void touchingMinimum(int i, int j, float[] params) {
        java.util.Arrays.fill(params, Float.MAX_VALUE);
        // A corner is touched by 4 tiles, an edge middle by 2 and a centre by 1
        int firstX = i % 2 == 0 ? i / 2 - 1 : (i - 1) / 2;
        int lastX = i / 2;
        int firstY = j % 2 == 0 ? j / 2 - 1 : (j - 1) / 2;
        int lastY = j / 2;
        for (int y = firstY; y <= lastY; y++) {
            for (int x = firstX; x <= lastX; x++) {
                TileShape shape = tile(x, y);
                params[0] = Math.min(params[0], shape.wibbleH());
                params[1] = Math.min(params[1], shape.wibbleV());
                for (int k = 0; k < 3; k++) {
                    params[2 + k] = Math.min(params[2 + k], shape.leanH()[k]);
                    params[2 + LEVELS + k] = Math.min(params[2 + LEVELS + k], shape.leanV()[k]);
                }
            }
        }
    }

    private int solid(int x, int y) {
        return tile(x, y).solid() ? 1 : 0;
    }

    /**
     * Which way the solid side is, x and z (tile y) in the result. Longer on
     * the diagonals.
     */
    private void solidDirection(int i, int j, Vector3f result) {
        boolean evenI = i % 2 == 0;
        boolean evenJ = j % 2 == 0;
        if (evenI && evenJ) {
            int a = i / 2;
            int b = j / 2;
            float dx = Math.signum((solid(a, b - 1) + solid(a, b)) - (solid(a - 1, b - 1) + solid(a - 1, b)));
            float dz = Math.signum((solid(a - 1, b) + solid(a, b)) - (solid(a - 1, b - 1) + solid(a, b - 1)));
            float k = dx != 0 && dz != 0 ? DIAGONAL : 1f;
            result.set(dx * k, 0, dz * k);
        } else if (!evenI && evenJ) { // Edge between the tile above and below
            int a = (i - 1) / 2;
            int b = j / 2;
            result.set(0, 0, Math.signum(solid(a, b) - solid(a, b - 1)));
        } else if (evenI) { // Edge between the tile on the left and right
            int a = i / 2;
            int b = (j - 1) / 2;
            result.set(Math.signum(solid(a, b) - solid(a - 1, b)), 0, 0);
        } else { // Centre is the average of its edges
            Vector3f sum = new Vector3f();
            Vector3f edge = new Vector3f();
            solidDirection(i, j - 1, edge);
            sum.addLocal(edge);
            solidDirection(i, j + 1, edge);
            sum.addLocal(edge);
            solidDirection(i - 1, j, edge);
            sum.addLocal(edge);
            solidDirection(i + 1, j, edge);
            sum.addLocal(edge);
            result.set(sum.multLocal(0.25f));
        }
    }

    /**
     * The push toward the solid side of a point, at a height level. The point
     * uses the lean of its own touching tiles.
     */
    private void lean(int i, int j, int level, Vector3f result) {
        boolean evenI = i % 2 == 0;
        boolean evenJ = j % 2 == 0;
        if (!evenI && !evenJ) { // Centre is the average of its edges
            Vector3f sum = new Vector3f();
            Vector3f edge = new Vector3f();
            lean(i, j - 1, level, edge);
            sum.addLocal(edge);
            lean(i, j + 1, level, edge);
            sum.addLocal(edge);
            lean(i - 1, j, level, edge);
            sum.addLocal(edge);
            lean(i + 1, j, level, edge);
            sum.addLocal(edge);
            result.set(sum.multLocal(0.25f));
            return;
        }

        float[] params = new float[2 + 2 * LEVELS];
        touchingMinimum(i, j, params);
        int index = 3 - level;
        float leanH = params[2 + index];
        float leanV = params[2 + LEVELS + index];
        Vector3f direction = new Vector3f();
        solidDirection(i, j, direction);
        result.set(direction.x * leanH, direction.x != 0 || direction.z != 0 ? leanV : 0, direction.z * leanH);

        // An edge bends smoothly between its corners, too
        if (evenI != evenJ) {
            Vector3f corner = new Vector3f();
            Vector3f sum = new Vector3f();
            int di = evenI ? 0 : 1;
            int dj = evenI ? 1 : 0;
            lean(i - di, j - dj, level, corner);
            sum.addLocal(corner);
            lean(i + di, j + dj, level, corner);
            sum.addLocal(corner);
            if (evenI) {
                result.z += sum.z / 2; // Vertical edge, along z
            } else {
                result.x += sum.x / 2;
            }
        }
    }

    /**
     * A stateless value in [0, 1), the same for the same input always
     */
    private static float random(int i, int j, int level, int channel) {
        int h = SEED;
        h = mix(h ^ (i * 0x85ebca6b));
        h = mix(h ^ (j * 0xc2b2ae35));
        h = mix(h ^ (level * 0x27d4eb2f + channel * 0x165667b1));
        return (h >>> 8) / (float) (1 << 24);
    }

    private static int mix(int h) {
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return h;
    }
}
