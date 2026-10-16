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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Trigger;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

/**
 *
 * Loads all the triggers from a KWD file
 *
 * @author ArchDemon
 */
public final class TriggerLoader {

    private static final Logger logger = System.getLogger(TriggerLoader.class.getName());

    private final Map<Integer, Trigger> triggers;

    public TriggerLoader(IKwdFile kwdFile) {
        this.triggers = kwdFile.getTriggers();
    }

    /**
     * Load a trigger tree. The returned node is a synthetic container whose
     * children are the sibling chain starting from {@code triggerId}. Its
     * repeat is the repeat of that first node, which is what the original game
     * hands down to the actions sitting directly in an owner's root chain.
     *
     * @param triggerId the root trigger ID, 0 = no tree
     * @return the container, {@code null} if there is no tree
     */
    public TriggerGenericData load(int triggerId) {
        TriggerGenericData root = null;
        if (triggerId != 0) {
            root = new TriggerGenericData();
            parse(root, triggerId);
            if (root.getQuantity() > 0) {
                root.setRepeatTimes(root.getChild(0).getRepeatTimes());
            }
        }
        return root;
    }

    private void parse(TriggerGenericData parent, int id) {
        Set<Integer> visited = new HashSet<>();
        while (id != 0) {
            if (!visited.add(id)) {
                logger.log(Level.WARNING, "Trigger {0} is in a sibling loop, chain cut!", id);
                break;
            }

            Trigger temp = triggers.get(id);
            if (temp == null) {

                // The original quietly resolves an unknown ID to the first trigger of the file,
                // which would only make the chain run in circles. The shipped maps have no such IDs
                logger.log(Level.WARNING, "Trigger {0} does not exist, chain cut!", id);
                break;
            }

            TriggerData trigger;
            switch (temp) {
                case TriggerGeneric triggerGeneric -> {
                    trigger = new TriggerGenericData(id, temp.getRepeatTimes());
                    ((TriggerGenericData) trigger).setType(triggerGeneric.getType());
                    ((TriggerGenericData) trigger).setComparison(triggerGeneric.getTargetValueComparison());

                    trigger.setPayload(temp.getPayload());

                    // A condition with repeat 0 starts dormant, only arming a tree can wake it up
                    trigger.setEnabled(temp.getRepeatTimes() > 0);

                    for (String key : temp.getUserDataKeys()) {
                        trigger.setUserData(key, temp.getUserData(key));
                    }

                    parent.attachChild(trigger);

                    if (temp.hasChildren()) {
                        parse(((TriggerGenericData) trigger), temp.getIdChild());
                    }

                }
                case TriggerAction triggerAction -> {
                    trigger = new TriggerActionData(id);
                    ((TriggerActionData) trigger).setType(triggerAction.getType());
                    trigger.setRepeatTimes(temp.getRepeatTimes());
                    trigger.setPayload(temp.getPayload());

                    for (String key : temp.getUserDataKeys()) {
                        trigger.setUserData(key, temp.getUserData(key));
                    }

                    parent.attachChild(trigger);

                }
                default ->
                    throw new RuntimeException("Unexpected class " + temp + "!");
            }

            id = temp.getIdNext();
        }
    }
}
