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
 * The two fog-of-war queries the lighting rules need (terrain-light selection and
 * cursor colour), for the local
 * viewer.
 * <p>
 * This is the seam between lighting and the fog-of-war system
 * ({@code view/fogofwar}): {@code MapViewController} adapts its
 * {@code IFogOfWarInformation} to it. {@link NoFogOfWarQuery} is the
 * default for lighting set-ups without fog (tests, previews).
 */
public interface IFogOfWarQuery {

    boolean isExplored(Point tile);

    boolean isPerceived(Point tile);
}
