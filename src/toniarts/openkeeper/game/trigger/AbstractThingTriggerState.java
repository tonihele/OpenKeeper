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
package toniarts.openkeeper.game.trigger;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.state.GameState;
import toniarts.openkeeper.tools.convert.map.Thing;

/**
 * A state for handling thing triggers
 *
 * @param <T> the
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractThingTriggerState<T> extends AbstractAppState {

    private AppStateManager stateManager;
    private Main app;
    private Map<Integer, AbstractThingTriggerControl<T>> thingTriggers = null;

    public AbstractThingTriggerState() {
    }

    public AbstractThingTriggerState(boolean enabled) {
        super.setEnabled(enabled);
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        this.stateManager = stateManager;
        this.app = (Main) app;

        // Get all the map thing triggers
        thingTriggers = initTriggers(this.stateManager.getState(GameState.class).getLevelData().getThings(), stateManager);
    }

    /**
     * Initialize the triggers from the level things
     *
     * @param things the level things
     * @param stateManager the state manager
     * @return the trigger ID - trigger mapping
     */
    protected abstract Map<Integer, AbstractThingTriggerControl<T>> initTriggers(List<Thing> things, AppStateManager stateManager);

    @Override
    public void update(float tpf) {
        if (!isEnabled() || !isInitialized()) {
            return;
        }

        // Update creature triggers
        for (AbstractThingTriggerControl thingTrigger : thingTriggers.values()) {
            thingTrigger.update(tpf);
        }

        super.update(tpf);
    }

    /**
     * Add a thing instance to a thing trigger
     *
     * @param triggerId the trigger ID
     * @param instanceControl the thing instance
     */
    public void addThing(int triggerId, T instanceControl) {
        thingTriggers.get(triggerId).addThing(instanceControl);
    }
}
