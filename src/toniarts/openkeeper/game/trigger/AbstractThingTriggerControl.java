/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.game.trigger;

import com.jme3.app.state.AppStateManager;
import java.util.logging.Logger;
import toniarts.openkeeper.world.control.IInteractiveControl;

/**
 * A base trigger control for Things, they seem to be very similar
 *
 * @param <T> the thing control type
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractThingTriggerControl<T extends IInteractiveControl> extends AbstractPlayerTriggerControl {

    protected T instanceControl;
    private static final Logger logger = Logger.getLogger(AbstractThingTriggerControl.class.getName());

    public AbstractThingTriggerControl() { // empty serialization constructor
        super();
    }

    public AbstractThingTriggerControl(final AppStateManager stateManager, int triggerId) {
        //super(stateManager, triggerId);
    }

    /**
     * Set the actual thing instance to this trigger control
     *
     * @param thingInstance the thing instance
     */
    protected void setThing(T thingInstance) {
        instanceControl = thingInstance;
        super.setPlayer(instanceControl.getOwnerId());
    }
}
