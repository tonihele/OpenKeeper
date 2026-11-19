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
package toniarts.openkeeper.game.data;

/** Object-specific presentation rules; use alone does not imply an animation. */
public final class FurnitureAnimationPolicy {
    public static final short WORKBENCH_OBJECT_ID = 46;

    private FurnitureAnimationPolicy() { }

    public static boolean pausesWhenIdle(short objectId) {
        return objectId == WORKBENCH_OBJECT_ID;
    }
}
