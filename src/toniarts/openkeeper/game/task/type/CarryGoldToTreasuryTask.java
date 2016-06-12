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
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.creature.CreatureControl;

/**
 * Carry gold to treasury
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CarryGoldToTreasuryTask extends AbstractTileTask {

    public CarryGoldToTreasuryTask(WorldState worldState, int x, int y, short playerId) {
        super(worldState, x, y, playerId);
    }

    @Override
    public boolean isValid() {
        // TODO:
        return true;
    }

    @Override
    public Vector2f getTarget(CreatureControl creature) {
        return new Vector2f(getTaskLocation().x + 0.5f, getTaskLocation().y + 0.5f);
    }

    @Override
    protected String getStringId() {
        return "2786";
    }

}
