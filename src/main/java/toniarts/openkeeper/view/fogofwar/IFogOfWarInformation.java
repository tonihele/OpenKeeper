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
package toniarts.openkeeper.view.fogofwar;

import toniarts.openkeeper.utils.Point;

/**
 * The read-only fog-of-war query surface for a single viewer. This is the
 * only way the presentation layer (and the handful of gameplay consumers
 * listed in the fog-of-war design document, §8.5) may look at fog - nothing
 * should read raw explored/perceived state directly.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IFogOfWarInformation {

    /**
     * Should this tile be rendered/treated as visible right now? Folds in
     * perception (for terrain that reveals through fog) and the camera
     * mode (first-person/possession bypasses fog entirely).
     */
    boolean isVisible(Point p);

    /**
     * Has this tile been explored at some point? Sticky.
     */
    boolean isExplored(Point p);

    /**
     * Has a creature of the viewer been within perception range of this
     * tile at some point? Sticky, never cleared during play.
     */
    boolean isPerceived(Point p);

    /**
     * The drag-box highlight rule (§8.4): unexplored, or explored and
     * taggable, or perceived, taggable and revealed-through-fog terrain.
     */
    boolean isHighlightable(Point p);

}
