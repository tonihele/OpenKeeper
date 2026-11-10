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
 * Listen to map (tile) changes
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface MapListener {

    /**
     * On tiles changed
     *
     * @param updatedTiles the tiles that changed
     */
    @Asynchronous
    public void onTilesChange(List<Point> updatedTiles);

    /**
     * Map tile should flash
     *
     * @param points   the list of map coordinates that should flash
     * @param enabled  flash on / off
     * @param keeperId the keeper ID to who these tiles should flash to
     */
    @Asynchronous
    public void onTileFlash(List<Point> points, boolean enabled, short keeperId);

    /**
     * Scripted reveal/conceal of an action point's tiles (fog of war)
     *
     * @param points   the map coordinates to reveal/conceal
     * @param explore  {@code true} to explore the tiles, {@code false} to unexplore them
     * @param keeperId the keeper ID whose fog of war is affected
     */
    @Asynchronous
    public void onTilesReveal(List<Point> points, boolean explore, short keeperId);

    /**
     * The {@code REMOVE_FOW} cheat: fog of war should be disabled (everything revealed)
     *
     * @param keeperId the keeper ID whose fog of war is affected
     */
    @Asynchronous
    public void onFogOfWarDisabled(short keeperId);

    /**
     * The {@code RESET_FOW} console command: fog of war should be reset, as if the
     * level had just started
     *
     * @param keeperId the keeper ID whose fog of war is affected
     */
    @Asynchronous
    public void onFogOfWarReset(short keeperId);

}
