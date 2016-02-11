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

import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.trigger.TriggerControl;
import toniarts.openkeeper.game.trigger.TriggerGenericData;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;


/**
 *
 * @author ArchDemon
 */


public class ActionPointTriggerControl extends TriggerControl {

    private static final Logger logger = Logger.getLogger(ActionPointTriggerControl.class.getName());

    public ActionPointTriggerControl() { // empty serialization constructor
        super();
    }

    public ActionPointTriggerControl(final Main app, int triggerId) {
        super(app, triggerId);
    }

    @Override
    protected boolean isActive(TriggerGenericData trigger) {
        boolean result = super.isActive(trigger);
        if (result) {
            return result;
        }
        
        float target = 0;
        result = false;

        TriggerGeneric.TargetType targetType = trigger.getType();
        switch (targetType) {
            case AP_CONGREGATE_IN:
                return false;
            case AP_CLAIM_PART_OF:
                return false;
            case AP_CLAIM_ALL_OF:
                return false;
            case AP_SLAP_TYPES:
                return false;
            case AP_TAG_PART_OF:
                return false;
            case AP_TAG_ALL_OF:
                return false;
            case AP_POSESSED_CREATURE_ENTERS:
                return false;
        }

        TriggerGeneric.ComparisonType comparisonType = trigger.getComparison();
        if (comparisonType != null && comparisonType != TriggerGeneric.ComparisonType.NONE) {
            result = compare(target, comparisonType, (int) trigger.getUserData("value"));
        }

        return result;
    }
}