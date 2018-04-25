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
package toniarts.openkeeper.game.trigger.door;

import com.jme3.app.state.AppStateManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.game.trigger.AbstractThingTriggerControl;
import toniarts.openkeeper.game.trigger.AbstractThingTriggerState;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.door.DoorControl;

/**
 * A state for handling door triggers
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class DoorTriggerState extends AbstractThingTriggerState<DoorControl> {

    public DoorTriggerState() {
    }

    public DoorTriggerState(boolean enabled) {
        super(enabled);
    }

    @Override
    protected Map<Integer, AbstractThingTriggerControl<DoorControl>> initTriggers(List<Thing> things, AppStateManager stateManager) {
        Map<Integer, AbstractThingTriggerControl<DoorControl>> doorTriggers = new HashMap<>();
        for (Thing thing : things) {
            if (thing instanceof Thing.Door) {
                Thing.Door door = (Thing.Door) thing;
                if (door.getTriggerId() != 0) {
                    doorTriggers.put(door.getTriggerId(), new DoorTriggerControl(stateManager, door.getTriggerId()));
                }
            }
        }
        return doorTriggers;
    }

}
