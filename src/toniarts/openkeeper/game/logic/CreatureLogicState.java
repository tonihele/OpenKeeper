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
package toniarts.openkeeper.game.logic;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import toniarts.openkeeper.world.ThingLoader;
import toniarts.openkeeper.world.creature.CreatureControl;

/**
 * Handles creature logic updates
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureLogicState extends AbstractAppState implements IGameLogicUpdateable {

    private final ThingLoader thingLoader;

    public CreatureLogicState(ThingLoader thingLoader) {
        this.thingLoader = thingLoader;
    }

    @Override
    public void processTick(float tpf, Application app) {
        for (CreatureControl creatureControl : thingLoader.getCreatures()) {
            creatureControl.processTick(tpf, app);
        }
    }

}
