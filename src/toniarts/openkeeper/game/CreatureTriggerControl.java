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
package toniarts.openkeeper.game;

import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.trigger.TriggerControl;
import toniarts.openkeeper.game.trigger.TriggerGenericData;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

/**
 *
 * @author ArchDemon
 */


public class CreatureTriggerControl extends TriggerControl {

    private static final Logger logger = Logger.getLogger(CreatureTriggerControl.class.getName());

    public CreatureTriggerControl() { // empty serialization constructor
        super();
    }

    public CreatureTriggerControl(final Main app, int triggerId) {
        super(app, triggerId);
    }

    @Override
    protected boolean isActive(TriggerGenericData trigger) {
        boolean result = super.isActive(trigger);
        if (result) {
            return result;
        }

        result = false;
        float target = 0;

        TriggerGeneric.TargetType targetType = trigger.getType();
        switch (targetType) {
            case CREATURE_CREATED:
                return false;
            case CREATURE_KILLED:
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
        }

        TriggerGeneric.ComparisonType comparisonType = trigger.getComparison();
        if (comparisonType != null && comparisonType != TriggerGeneric.ComparisonType.NONE) {
            result = compare(target, comparisonType, (int) trigger.getUserData("value"));
        }

        return result;
    }
}
