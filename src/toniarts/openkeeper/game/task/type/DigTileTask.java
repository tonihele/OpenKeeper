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

import com.jme3.math.Vector2f;
import java.awt.Point;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.creature.CreatureControl;

/**
 * Dig a tile task, for workers
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class DigTileTask extends AbstractTileTask {

    public DigTileTask(WorldState worldState, int x, int y) {
        super(worldState, x, y);
    }

    @Override
    public int getMaxAllowedNumberOfAsignees() {
        // TODO: I think it is 3 per accessible side, but we would need the map data for this
        return 3;
    }

    @Override
    public Vector2f getTarget(CreatureControl creature) {

        // Find an accessible target
        // TODO: entity's location?
        for (Point p : getWorldState().getMapLoader().getSurroundingTiles(getTaskLocation(), false)) {
            if (getWorldState().isAccessible(getWorldState().getMapData().getTile(p), creature.getCreature())) {

                // TODO: intelligent coordinates?
                return new Vector2f(p.x, p.y);
            }
        }

        return null;
    }

}
