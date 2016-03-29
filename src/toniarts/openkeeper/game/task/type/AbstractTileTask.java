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
package toniarts.openkeeper.game.task.type;

import java.awt.Point;
import toniarts.openkeeper.world.WorldState;

/**
 * Abstract base class for tasks pointing at terrain tiles
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractTileTask extends AbstractTask {

    private final Point location;
    protected final WorldState worldState;
    protected final short playerId;

    public AbstractTileTask(final WorldState worldState, final int x, final int y, final short playerId) {
        location = new Point(x, y);
        this.worldState = worldState;
        this.playerId = playerId;
    }

    @Override
    public Point getTaskLocation() {
        return location;
    }

}
