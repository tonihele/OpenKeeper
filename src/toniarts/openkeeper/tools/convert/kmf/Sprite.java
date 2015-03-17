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
package toniarts.openkeeper.tools.convert.kmf;

import java.util.HashMap;
import java.util.List;

/**
 * KMF Sprite wrapper
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class Sprite<T extends Vertex> {

    //HEAD
    /**
     * Triangle count for each LOD level
     */
    private List<Integer> triangleCounts;
    private int verticeCount;
    private float mmFactor;
    //DATA
    private int materialIndex;
    /**
     * LOD level, triangles
     */
    private HashMap<Integer, List<Triangle>> triangles;
    private List<T> vertices;

    protected List<Integer> getTriangleCounts() {
        return triangleCounts;
    }

    protected void setTriangleCounts(List<Integer> triangleCounts) {
        this.triangleCounts = triangleCounts;
    }

    protected int getVerticeCount() {
        return verticeCount;
    }

    protected void setVerticeCount(int verticeCount) {
        this.verticeCount = verticeCount;
    }

    public float getMmFactor() {
        return mmFactor;
    }

    protected void setMmFactor(float mmFactor) {
        this.mmFactor = mmFactor;
    }

    public int getMaterialIndex() {
        return materialIndex;
    }

    protected void setMaterialIndex(int materialIndex) {
        this.materialIndex = materialIndex;
    }

    public HashMap<Integer, List<Triangle>> getTriangles() {
        return triangles;
    }

    protected void setTriangles(HashMap<Integer, List<Triangle>> triangles) {
        this.triangles = triangles;
    }

    public List<T> getVertices() {
        return vertices;
    }

    protected void setVertices(List<T> vertices) {
        this.vertices = vertices;
    }
}
