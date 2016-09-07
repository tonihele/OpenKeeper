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
package toniarts.openkeeper.game.trigger.creature;

import com.jme3.app.state.AppStateManager;
import java.util.logging.Logger;
import toniarts.openkeeper.ai.creature.CreatureState;
import toniarts.openkeeper.game.trigger.AbstractThingTriggerControl;
import toniarts.openkeeper.game.trigger.TriggerActionData;
import toniarts.openkeeper.game.trigger.TriggerGenericData;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;
import toniarts.openkeeper.world.creature.CreatureControl;

/**
 *
 * @author ArchDemon
 */
public class CreatureTriggerControl extends AbstractThingTriggerControl<CreatureControl> {

    private static final Logger logger = Logger.getLogger(CreatureTriggerControl.class.getName());

    public CreatureTriggerControl() { // empty serialization constructor
        super();
    }

    public CreatureTriggerControl(final AppStateManager stateManager, int triggerId) {
        super(stateManager, triggerId);
    }

    @Override
    protected boolean isActive(TriggerGenericData trigger) {
        boolean result = super.isActive(trigger);
        if (checked) {
            return result;
        }

        result = false;
        float target = 0;

        TriggerGeneric.TargetType targetType = trigger.getType();
        switch (targetType) {
            case CREATURE_CREATED:
                return instanceControl != null;
            case CREATURE_KILLED:
                if (instanceControl != null && instanceControl.getStateMachine().getCurrentState() != null) {
                    return instanceControl.getStateMachine().getCurrentState().equals(CreatureState.DEAD);
                }
                return false;
            case CREATURE_SLAPPED:
                return false;
            case CREATURE_ATTACKED:
                return false;
            case CREATURE_IMPRISONED:
                return false;
            case CREATURE_TORTURED:
                return false;
            case CREATURE_CONVERTED:
                return false;
            case CREATURE_CLAIMED:
                return false;
            case CREATURE_ANGRY:
                return false;
            case CREATURE_AFRAID:
                return false;
            case CREATURE_STEALS:
                return false;
            case CREATURE_LEAVES:
                return false;
            case CREATURE_STUNNED:
                return false;
            case CREATURE_DYING:
                return false;
            case CREATURE_HEALTH:
                if (instanceControl != null) {
                    target = ((float) instanceControl.getHealth() / instanceControl.getMaxHealth()) * 100; // Percentage
                    break;
                }
                return false;
            case CREATURE_GOLD_HELD:
                return false;
            case CREATURE_EXPERIENCE_LEVEL:
                return false;
            case CREATURE_HUNGER_SATED:
                return false;
            case CREATURE_PICKS_UP_PORTAL_GEM:
                return false;
            case CREATURE_SACKED:
                return false;
            case CREATURE_PICKED_UP:
                return false;
            default:
                logger.warning("Target Type not supported");
                return false;
        }

        TriggerGeneric.ComparisonType comparisonType = trigger.getComparison();
        if (comparisonType != null && comparisonType != TriggerGeneric.ComparisonType.NONE) {
            result = compare(target, comparisonType, (int) trigger.getUserData("value"));
        }

        return result;
    }

    @Override
    protected void doAction(TriggerActionData trigger) {
        TriggerAction.ActionType type = trigger.getType();

        // Some triggers are bound to the creature itself
        switch (type) {
            case ATTACH_PORTAL_GEM: {
                break;
            }
            case MAKE_HUNGRY: {
                break;
            }
            case SHOW_HEALTH_FLOWER: {
                if (instanceControl != null) {
                    stateManager.getApplication().enqueue(() -> {

                        instanceControl.showUnitFlower(trigger.getUserData("value", Integer.class));

                        return null;
                    });
                }
                break;
            }
            // TODO: Undiscovered
//            case ALTER_SPEED: {
//                break;
//            }
            case REMOVE_FROM_MAP: {
                break;
            }
            case SET_FIGHT_FLAG: {
                break;
            }
            case ZOOM_TO: {
                break;
            }
            default: {
                super.doAction(trigger);
            }
        }
    }

}
