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
 * The groups of light sources that can be switched on and off independently
 * (see {@link LightRules#setGroupEnabled(LightGroup, boolean)}), a coarser
 * view of {@link LightKind} that also tells object lights from creature and
 * effect lights.
 */
public enum LightGroup {
    TORCH,
    TERRAIN,
    ROOM,
    CREATURE,
    OBJECT,
    CURSOR;

    public static LightGroup of(LightSource light) {
        switch (light.getKind()) {
            case TORCH:
                return TORCH;
            case TERRAIN:
            case SLAB:
                return TERRAIN;
            case PORTAL:
                return ROOM;
            case CURSOR:
                return CURSOR;
            default:
                return light.isObjectLight() ? OBJECT : CREATURE;
        }
    }
}
