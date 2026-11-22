/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.game.listener;

import com.jme3.network.service.rmi.Asynchronous;
import toniarts.openkeeper.utils.Point;
import java.util.List;

/**
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface PlayerActionListener {

    @Asynchronous
    public void onBuild(short keeperId, List<Point> tiles);

    @Asynchronous
    public void onSold(short keeperId, List<Point> tiles);

    /**
     * Called by the server once after the last wave of a room build or sale.
     * The tile list contains all room tiles actually changed by that selection, in
     * wave order. Rejected, cancelled and unchanged selections do not notify.
     * Per-wave onBuild/onSold callbacks remain available for incremental work.
     * Server listeners can use this hook for selection-wide job reassessment.
     */
    @Asynchronous
    default void onRoomSelectionComplete(short keeperId, List<Point> tiles) {
    }

}
