/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.world.pathfinding;

import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import toniarts.openkeeper.world.TileData;

/**
 * The actual path finder
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MapPathFinder extends IndexedAStarPathFinder<TileData> {

    public MapPathFinder(MapIndexedGraph graph, boolean calculateMetrics) {
        super(graph, calculateMetrics);
    }

}
