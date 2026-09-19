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
 * The kinds of light source the lighting system creates. See
 * {@code lighting_design.md} &sect;2/&sect;5.
 * <p>
 * There is deliberately no {@code SlabLight} member (design &sect;6.7): this
 * codebase's {@code Terrain} parser reads the community-template "stringId"
 * block as a {@code StringId}, not as a {@code Light} struct, so there is no
 * data to drive slab lights with. Per decision D2 ("no stock terrain uses
 * it, exists for custom terrain"), slab lights are skipped rather than
 * requiring new binary-format parsing work out of scope for lighting.
 *
 * @author Toni Helenius &lt;helenius.toni@gmail.com&gt;
 */
public enum LightKind {
    THING,
    TORCH,
    TERRAIN,
    PORTAL,
    CURSOR
}
