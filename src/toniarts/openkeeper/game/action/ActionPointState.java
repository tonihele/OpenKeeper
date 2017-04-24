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
package toniarts.openkeeper.game.action;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import java.util.HashMap;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.state.GameState;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.WorldState;

/**
 *
 * @author ArchDemon
 */
public class ActionPointState extends AbstractAppState {

    private AppStateManager stateManager;
    private Main app;
    private HashMap<Integer, ActionPoint> actionPoints = null;
    private HashMap<Integer, ActionPointTriggerControl> triggers = null;

    public ActionPointState() {
    }

    public ActionPointState(boolean enabled) {
        super.setEnabled(enabled);
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        this.stateManager = stateManager;
        this.app = (Main) app;

        actionPoints = new HashMap<>();
        triggers = new HashMap<>();

        for (Thing thing : this.stateManager.getState(GameState.class).getLevelData().getThings()) {
            if (thing instanceof Thing.ActionPoint) {
                Thing.ActionPoint temp = (Thing.ActionPoint) thing;
                ActionPoint ap = new ActionPoint(temp);
                ap.setParent(this);
                if (temp.getTriggerId() != 0) {
                    triggers.put(ap.getId(), new ActionPointTriggerControl(this.stateManager, temp.getTriggerId(), ap));
                }
                actionPoints.put(ap.getId(), ap);
            }
        }
    }

    @Override
    public void update(float tpf) {
        if (!isEnabled() || !isInitialized()) {
            return;
        }

        for (ActionPointTriggerControl triggerControl : triggers.values()) {
            triggerControl.update(tpf);
        }

        super.update(tpf);
    }

    /**
     * FIXME: rather quirky design this is, due to action point controls are
     * visual and FPS related
     *
     * @param tpf
     */
    public void updateControls(float tpf) {
        if (!isEnabled() || !isInitialized()) {
            return;
        }
        // FIXME java.util.ConcurrentModificationException
        for (ActionPoint actionPoint : actionPoints.values()) {
            actionPoint.update(tpf);
        }
    }

    public WorldState getWorldState() {
        return stateManager.getState(WorldState.class);
    }

    public ActionPoint getActionPoint(int id) {
        return actionPoints.get(id);
    }
}
