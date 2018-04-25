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
package toniarts.openkeeper.game.trigger.object;

import com.jme3.app.state.AppStateManager;
import java.util.logging.Logger;
import toniarts.openkeeper.game.trigger.AbstractThingTriggerControl;
import toniarts.openkeeper.game.trigger.TriggerGenericData;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;
import toniarts.openkeeper.world.object.ObjectControl;

/**
 *
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ObjectTriggerControl extends AbstractThingTriggerControl<ObjectControl> {

    private static final Logger logger = Logger.getLogger(ObjectTriggerControl.class.getName());

    public ObjectTriggerControl() { // empty serialization constructor
        super();
    }

    public ObjectTriggerControl(final AppStateManager stateManager, int triggerId) {
        super(stateManager, triggerId);
    }

    @Override
    protected boolean isActive(TriggerGenericData trigger) {
        boolean result = false;

        float target = 0;

        TriggerGeneric.TargetType targetType = trigger.getType();
        switch (targetType) {
            case OBJECT_CLAIMED:
                return false;
            default:
                return super.isActive(trigger);
        }

//        TriggerGeneric.ComparisonType comparisonType = trigger.getComparison();
//        if (comparisonType != null && comparisonType != TriggerGeneric.ComparisonType.NONE) {
//            result = compare(target, comparisonType, (int) trigger.getUserData("value"));
//        }
//
//        return result;
    }
}
