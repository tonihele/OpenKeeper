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
 * The default {@link IFogOfWarQuery}: everything is explored and
 * perceived. Used where there is no fog (tests, previews); in the game
 * {@code MapViewController} supplies a query backed by the real fog state.
 *
 * @author Toni Helenius &lt;helenius.toni@gmail.com&gt;
 */
public final class NoFogOfWarQuery implements IFogOfWarQuery {

    @Override
    public boolean isExplored(Point tile) {
        return true;
    }

    @Override
    public boolean isPerceived(Point tile) {
        return true;
    }
}
