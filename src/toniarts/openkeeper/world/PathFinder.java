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
package toniarts.openkeeper.world;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Tile;

/**
 *
 * @author ArchDemon
 */
public class PathFinder {

    private Integer[][] tree;
    private final Integer[][] original;
    public Vector2f start;
    public Vector2f end;
    private Queue<Vector3f> queue;

    public PathFinder(KwdFile kwdFile) {

        Tile[][] tiles = kwdFile.getMap().getTiles();
        this.original = new Integer[tiles.length][tiles[0].length];

        for (int x = 0; x < tiles.length; x++) {
            for (int y = 0; y < tiles[x].length; y++) {

                Terrain terrain = kwdFile.getTerrain(kwdFile.getMap().getTile(x, y).getTerrainId());
                original[x][y] = (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) ? -1 : -2;
            }
        }
    }

    public boolean hasPath(Vector2f from, Vector2f to) {
        if (from == null) {
            from = start;
        }

        tree = new Integer[original.length][original[0].length];
        for (int x = 0; x < original.length; x++) {
            System.arraycopy(original[x], 0, tree[x], 0, original[x].length);
        }
        queue = new LinkedList<>();
        queue.add(new Vector3f(from.x, from.y, 0));
        while (!queue.isEmpty()) {
            Vector3f p = queue.poll();
            this.fill((int) p.x, (int) p.y, (int) p.z, (int) to.x, (int) to.y);
        }

        return (tree[(int) to.x][(int) to.y] >= 0);
    }

    public void fill(int x, int y, int weight, int cx, int cy) {

        tree[x][y] = weight;
        if (x == cx && y == cy) {
            queue.clear();
            return;
        }

        Vector2f[] dir = {new Vector2f(1, 0), new Vector2f(-1, 0), new Vector2f(0, 1), new Vector2f(0, -1)};

        for (Vector2f d : dir) {
            if ((x + d.x) < 0
                    || (x + d.x) >= tree.length
                    || (y + d.y) < 0
                    || (y + d.y) >= tree[(int) (x + d.x)].length) {
                continue;
            }
            if (tree[(int) (x + d.x)][(int) (y + d.y)] == -2
                    || tree[(int) (x + d.x)][(int) (y + d.y)] != -1
                    && tree[(int) (x + d.x)][(int) (y + d.y)] > weight
                    && !queue.contains(new Vector3f(x + d.x, y + d.y, weight + 1))) {
                tree[(int) (x + d.x)][(int) (y + d.y)] = weight + 1;
                queue.add(new Vector3f(x + d.x, y + d.y, weight + 1));
            }
        }

    }

    public List<Vector2f> getPath(Vector2f from, Vector2f to) {
        List<Vector2f> result = new ArrayList<>();
        if (!hasPath(from, to)) {
            return result;
        }

        if (from == null) {
            from = start;
        }

        Vector2f[] dir = {new Vector2f(1, 0), new Vector2f(-1, 0), new Vector2f(0, 1), new Vector2f(0, -1)};

        while (!from.equals(to)) {
            int current = tree[(int) to.x][(int) to.y];
            int next;
            for (Vector2f d : dir) {
                if ((to.x + d.x) < 0 || (to.x + d.x) >= tree.length
                        || (to.y + d.y) < 0 || (to.y + d.y) >= tree[(int) (to.x + d.x)].length) {
                    continue;
                }

                next = tree[(int) (to.x + d.x)][(int) (to.y + d.y)];

                if (next >= 0 && next < current) {
                    to.addLocal(d);
                    result.add(to.clone());
                    break;
                }
            }

        }

        return result;
    }

    @Override
    public String toString() {
        String result = "\n";
        for (int x = 0; x < tree.length; x++) {
            for (int y = 0; y < tree[x].length; y++) {
                result += tree[x][y] + "   ";
            }
            result += "\n";
        }
        return result;
    }
}
