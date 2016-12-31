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
package toniarts.openkeeper.game.trigger.party;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.world.creature.Party;

/**
 *
 * @author ArchDemon
 */
public class PartyTriggerState extends AbstractAppState {

    private AppStateManager stateManager;
    private Main app;
    private final Map<Integer, PartyTriggerControl> parties = new HashMap<>();

    public PartyTriggerState() {
    }

    public PartyTriggerState(boolean enabled) {
        super.setEnabled(enabled);
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        this.stateManager = stateManager;
        this.app = (Main) app;
    }

    @Override
    public void update(float tpf) {
        if (!isEnabled() || !isInitialized()) {
            return;
        }

        for (PartyTriggerControl partyTriggerControl : parties.values()) {
            partyTriggerControl.update(tpf);
        }

        super.update(tpf);
    }

    public void addParty(int triggerId, Party party) {
        parties.put(triggerId, new PartyTriggerControl(stateManager, triggerId, party));
    }
}
