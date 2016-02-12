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

/**
 *
 * @author ArchDemon
 */


public class ActionPointState extends AbstractAppState {

    private AppStateManager stateManager;
    private Main app;
    private HashMap<Integer, ActionPoint> actionPoints = null;

    public ActionPointState() {
    }

    public ActionPointState(boolean enabled) {
        super.setEnabled(false);
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        this.stateManager = stateManager;
        this.app = (Main) app;

        actionPoints = new HashMap<>();

        for (Thing thing : this.stateManager.getState(GameState.class).getLevelData().getThings()) {
            if (thing instanceof Thing.ActionPoint) {
                Thing.ActionPoint temp = (Thing.ActionPoint) thing;
                ActionPoint ap = new ActionPoint(temp);
                if (temp.getTriggerId() != 0) {
                    ap.addControl(new ActionPointTriggerControl(this.app, temp.getTriggerId()));
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

        for (ActionPoint actionPoint : actionPoints.values()) {
            actionPoint.update(tpf);
        }

        super.update(tpf);
    }

    public ActionPoint getActionPoint(int id) {
        return actionPoints.get(id);
    }
}
