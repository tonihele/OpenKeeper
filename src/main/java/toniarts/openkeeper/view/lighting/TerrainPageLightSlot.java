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
package toniarts.openkeeper.view.lighting;

import com.jme3.material.Material;
import java.util.ArrayList;
import java.util.List;

/**
 * One terrain {@code BatchNode} page's world AABB and the Vertex materials that
 * should receive that page's light-index row. Rooms / water / entities are not
 * registered here — they keep the full shared {@code LightData} loop.
 */
final class TerrainPageLightSlot {

    final int pageIndex;
    final float minX;
    final float maxX;
    final float minY;
    final float maxY;
    final float minZ;
    final float maxZ;
    final List<Material> materials = new ArrayList<>();
    /** Last LightIndexCount pushed to materials; avoids redundant setInt. */
    int lastUploadedCount = Integer.MIN_VALUE;

    TerrainPageLightSlot(int pageIndex,
            float minX, float maxX,
            float minY, float maxY,
            float minZ, float maxZ) {
        this.pageIndex = pageIndex;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    void setMaterials(List<Material> next) {
        materials.clear();
        if (next != null) {
            materials.addAll(next);
        }
        lastUploadedCount = Integer.MIN_VALUE;
    }
}
