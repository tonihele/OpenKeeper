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

import toniarts.openkeeper.utils.Point;

/**
 * The two fog-of-war queries the lighting rules need (design &sect;6.3's
 * terrain-light selection, &sect;7.3's cursor colour), for the local
 * viewer.
 * <p>
 * This repository has no fog-of-war implementation at all yet (checked:
 * there is no {@code view/fogofwar} package, no exploration tracking
 * anywhere in {@code view/}) -- {@code lighting_jmonkey.md}'s audit
 * assumed one already existed and was wrong. This interface is the seam a
 * real fog-of-war system can be wired in through later without touching
 * any lighting code again; {@link NoFogOfWarQuery} is the only
 * implementation for now.
 *
 * @author Toni Helenius &lt;helenius.toni@gmail.com&gt;
 */
public interface IFogOfWarQuery {

    boolean isExplored(Point tile);

    boolean isPerceived(Point tile);
}
