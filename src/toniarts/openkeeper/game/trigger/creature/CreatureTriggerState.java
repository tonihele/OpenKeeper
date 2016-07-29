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
package toniarts.openkeeper.game.trigger.creature;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.state.GameState;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.creature.CreatureControl;

/**
 * A state for handling creature triggers
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureTriggerState extends AbstractAppState {

    private AppStateManager stateManager;
    private Main app;
    private Map<Integer, CreatureTriggerControl> creatureTriggers = null;

    public CreatureTriggerState() {
    }

    public CreatureTriggerState(boolean enabled) {
        super.setEnabled(false);
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        this.stateManager = stateManager;
        this.app = (Main) app;

        // Get all the map creature triggers
        creatureTriggers = new HashMap<>();
        for (Thing thing : this.stateManager.getState(GameState.class).getLevelData().getThings()) {
            if (thing instanceof Thing.GoodCreature) {
                Thing.GoodCreature creature = (Thing.GoodCreature) thing;
                if (creature.getTriggerId() != 0) {
                    creatureTriggers.put(creature.getTriggerId(), new CreatureTriggerControl(stateManager, creature.getTriggerId()));
                }
            } else if (thing instanceof Thing.KeeperCreature) {
                Thing.KeeperCreature creature = (Thing.KeeperCreature) thing;
                if (creature.getTriggerId() != 0) {
                    creatureTriggers.put(creature.getTriggerId(), new CreatureTriggerControl(stateManager, creature.getTriggerId()));
                }
            } else if (thing instanceof Thing.NeutralCreature) {
                Thing.NeutralCreature creature = (Thing.NeutralCreature) thing;
                if (creature.getTriggerId() != 0) {
                    creatureTriggers.put(creature.getTriggerId(), new CreatureTriggerControl(stateManager, creature.getTriggerId()));
                }
            } else if (thing instanceof Thing.HeroParty) {
                Thing.HeroParty heroParty = (Thing.HeroParty) thing;
                for (Thing.GoodCreature creature : heroParty.getHeroPartyMembers()) {
                    if (creature.getTriggerId() != 0) {
                        creatureTriggers.put(creature.getTriggerId(), new CreatureTriggerControl(stateManager, creature.getTriggerId()));
                    }
                }
            }
        }
    }

    @Override
    public void update(float tpf) {
        if (!isEnabled() || !isInitialized()) {
            return;
        }

        // Update creature triggers
        for (CreatureTriggerControl creatureTrigger : creatureTriggers.values()) {
            creatureTrigger.update(tpf);
        }

        super.update(tpf);
    }

    /**
     * Add a creature instance to a creature trigger
     *
     * @param triggerId the trigger ID
     * @param creatureInstance the creature instance
     */
    public void addCreature(int triggerId, CreatureControl creatureInstance) {
        creatureTriggers.get(triggerId).addCreature(creatureInstance);
    }
}
