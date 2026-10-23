/*
 * Copyright (C) 2014-2025 OpenKeeper
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

/**
 * The kinds of light source the lighting system creates.
 * {@link #SLAB} is the per-terrain {@code Light} block;
 * no stock terrain uses it, it exists for custom terrain.
 */
public enum LightKind {
    THING,
    TORCH,
    TERRAIN,
    SLAB,
    PORTAL,
    CURSOR;

    /**
     * Whether lights of this kind belong to a map tile (and so follow that
     * tile's fog of war), as opposed to a thing, a room or the cursor.
     */
    public boolean isTileOwned() {
        return this == TORCH || this == TERRAIN || this == SLAB;
    }
}
