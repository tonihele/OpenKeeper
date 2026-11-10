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
package toniarts.openkeeper.game.map;

import java.util.HashSet;
import java.util.Set;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/**
 * An {@link IFogOfWarInformation} test double driven directly by test setup
 * rather than by simulating real fog rules. Public: also reused by
 * {@code view.minimap}'s rendering tests.
 */
public final class FakeFogOfWarInformation implements IFogOfWarInformation {

    public final Set<Point> explored = new HashSet<>();
    public final Set<Point> perceived = new HashSet<>();
    public final Set<Point> highlightable = new HashSet<>();

    @Override
    public boolean isVisible(Point p) {
        return isExplored(p) || isPerceived(p);
    }

    @Override
    public boolean isExplored(Point p) {
        return explored.contains(p);
    }

    @Override
    public boolean isPerceived(Point p) {
        return perceived.contains(p);
    }

    @Override
    public boolean isHighlightable(Point p) {
        return highlightable.contains(p);
    }

    @Override
    public boolean isPendingTagged(Point p) {
        return false;
    }

}
