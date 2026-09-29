/*
 * Copyright (C) 2014-2015 OpenKeeper
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

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the cave ceiling: a merged mesh of one patch per open, visible tile,
 * domed by {@link ClearanceField} so it presses down to a fixed height at a
 * wall face and rises toward open ground, ending exactly at the fog boundary
 * because only visible tiles get a patch at all.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class Ceiling {

    /**
     * The asymptote the ceiling height rises toward as clearance grows -
     * never quite reached, even at {@link ClearanceField#MAX_CLEARANCE}. At
     * {@code clearance == 0} (a wall face) the height curve below evaluates
     * to {@code HEIGHT_APEX / 2}.
     */
    private static final float HEIGHT_APEX = 4f;

    /**
     * Half the step, in tiles, used both between adjacent patch vertices and
     * for the central-difference normal samples.
     */
    private static final float HALF_STEP = 0.5f;

    private Ceiling() {
        // Nope
    }

    /**
     * Constructs the merged ceiling mesh for one batch of tiles that all
     * resolve to the same material.
     *
     * @param assetManager asset manager instance
     * @param clearanceField the clearance field for the whole map
     * @param terrain the terrain to draw the material's static lighting tint
     * from - see {@link MapViewController#setTerrainMaterialLighting}
     * @param ceilingResource the room's ceiling texture override, or
     * {@code null} to use the default engine ceiling texture
     * @param tiles the tile coordinates in this batch
     * @return the visual representation of this batch
     */
    public static Geometry construct(AssetManager assetManager, ClearanceField clearanceField,
            Terrain terrain, ArtResource ceilingResource, List<Point> tiles) {
        Mesh mesh = createMesh(clearanceField, tiles);

        Geometry geo = new Geometry("Ceiling", mesh);

        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        String textureName = (ceilingResource != null) ? ceilingResource.getName() : "Ceiling";
        TextureKey textureKey = new TextureKey(AssetUtils.getCanonicalAssetKey(
                AssetsConverter.TEXTURES_FOLDER.concat("/").concat(textureName).concat(".png")), false);
        Texture tex = assetManager.loadTexture(textureKey);
        mat.setTexture("DiffuseMap", tex);
        MapViewController.setTerrainMaterialLighting(mat, terrain);

        geo.setMaterial(mat);
        geo.setShadowMode(RenderQueue.ShadowMode.Off); // Nothing above a ceiling to light or shadow it

        return geo;
    }

    private static Mesh createMesh(ClearanceField clearanceField, List<Point> tiles) {
        Mesh mesh = new Mesh();

        List<Vector3f> vertices = new ArrayList<>(tiles.size() * 9);
        List<Vector2f> textureCoordinates = new ArrayList<>(tiles.size() * 9);
        List<Vector3f> normals = new ArrayList<>(tiles.size() * 9);
        List<Integer> indexes = new ArrayList<>(tiles.size() * 24);

        for (Point tile : tiles) {
            int base = vertices.size();

            // The 3x3 grid of vertices over the tile: the 4 corners, 4 edge
            // midpoints and the centre
            for (int j = 0; j < 3; j++) {
                float v = j * HALF_STEP;
                for (int i = 0; i < 3; i++) {
                    float u = i * HALF_STEP;

                    float height = heightAt(clearanceField, tile.x, tile.y, u, v);
                    // The height curve's "0" is the floor plane, but WorldUtils.FLOOR_HEIGHT
                    // is already 1 tile above this engine's ground reference (it's where
                    // creatures stand) - adding it on top double-counts that tile
                    vertices.add(new Vector3f((tile.x - HALF_STEP + u) * WorldUtils.TILE_WIDTH,
                            WorldUtils.UNDERFLOOR_HEIGHT + height,
                            (tile.y - HALF_STEP + v) * WorldUtils.TILE_WIDTH));
                    textureCoordinates.add(new Vector2f(u, v));
                    normals.add(normalAt(clearanceField, tile.x, tile.y, u, v));
                }
            }

            // 2x2 quads over the 3x3 vertex grid, wound to face down
            for (int cj = 0; cj < 2; cj++) {
                for (int ci = 0; ci < 2; ci++) {
                    int topLeft = base + patchVertexIndex(ci, cj);
                    int topRight = base + patchVertexIndex(ci + 1, cj);
                    int bottomLeft = base + patchVertexIndex(ci, cj + 1);
                    int bottomRight = base + patchVertexIndex(ci + 1, cj + 1);

                    indexes.add(topLeft);
                    indexes.add(topRight);
                    indexes.add(bottomLeft);

                    indexes.add(topRight);
                    indexes.add(bottomRight);
                    indexes.add(bottomLeft);
                }
            }
        }

        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices.toArray(new Vector3f[0])));
        mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(textureCoordinates.toArray(new Vector2f[0])));
        mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(toIntArray(indexes)));
        mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normals.toArray(new Vector3f[0])));
        mesh.updateBound();

        return mesh;
    }

    private static int patchVertexIndex(int i, int j) {
        return j * 3 + i;
    }

    /**
     * Bilinearly interpolates the clearance field over tile {@code (tileX,
     * tileY)} at local coordinates {@code u, v} and maps it to a height
     * above the floor. {@code u}/{@code v} may fall outside {@code [0, 1]}
     * for the normal's central-difference samples, which reach half a tile
     * into the neighbouring tile; the interpolated clearance is clamped back
     * into the field's own range before the height curve is applied, since
     * that curve is only valid over {@code [0, MAX_CLEARANCE]}.
     */
    private static float heightAt(ClearanceField clearanceField, int tileX, int tileY, float u, float v) {
        float c00 = clearanceField.at(tileX, tileY);
        float c10 = clearanceField.at(tileX + 1, tileY);
        float c01 = clearanceField.at(tileX, tileY + 1);
        float c11 = clearanceField.at(tileX + 1, tileY + 1);

        float clearance = c00 * (1 - u) * (1 - v) + c10 * u * (1 - v)
                + c01 * (1 - u) * v + c11 * u * v;
        clearance = FastMath.clamp(clearance, 0, ClearanceField.MAX_CLEARANCE);

        // A parabola whose apex sits one step past MAX_CLEARANCE, so it is
        // monotonic and concave across the whole input range: the ceiling
        // rises fastest right next to a wall and flattens out approaching
        // HEIGHT_APEX, which it never quite reaches. A linear ramp here
        // reads as tented rather than domed.
        float t = HEIGHT_APEX - clearance / 4f;
        return HEIGHT_APEX - (t * t) / 8f;
    }

    /**
     * A downward-facing normal from central differences of the (locally
     * extrapolated) height surface, so the patch is lit rather than reading
     * as flat-black under the terrain lighting model every other surface
     * uses.
     */
    private static Vector3f normalAt(ClearanceField clearanceField, int tileX, int tileY, float u, float v) {
        float hu0 = heightAt(clearanceField, tileX, tileY, u - HALF_STEP, v);
        float hu1 = heightAt(clearanceField, tileX, tileY, u + HALF_STEP, v);
        float hv0 = heightAt(clearanceField, tileX, tileY, u, v - HALF_STEP);
        float hv1 = heightAt(clearanceField, tileX, tileY, u, v + HALF_STEP);

        float slopeU = (hu1 - hu0) / (2 * HALF_STEP);
        float slopeV = (hv1 - hv0) / (2 * HALF_STEP);

        return new Vector3f(slopeU, -1, slopeV).normalizeLocal();
    }

    private static int[] toIntArray(final List<Integer> list) {
        int[] ret = new int[list.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = list.get(i);
        }
        return ret;
    }
}
