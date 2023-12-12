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

import java.lang.System.Logger;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.creature.IPartyController;
import toniarts.openkeeper.game.trigger.TriggerControl;
import toniarts.openkeeper.game.trigger.TriggerGenericData;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

public class PartyTriggerControl extends TriggerControl {
    
    private static final Logger LOGGER = System.getLogger(PartyTriggerControl.class.getName());

    private IPartyController partyController;

    public PartyTriggerControl() { // empty serialization constructor
        super();
    }

    public PartyTriggerControl(final IGameController gameController, final ILevelInfo levelInfo, final IGameTimer gameTimer,
            final IMapController mapController, final ICreaturesController creaturesController, int triggerId,
            final IPartyController partyController) {
        super(gameController, levelInfo, gameTimer, mapController, creaturesController, triggerId);
        this.partyController = partyController;
    }

    @Override
    protected boolean isActive(TriggerGenericData trigger) {
        boolean result = false;

        float target = 0;
        int value = 0;

        TriggerGeneric.TargetType targetType = trigger.getType();
        switch (targetType) {
            case PARTY_CREATED:
                return partyController.isCreated();

            case PARTY_MEMBERS_KILLED:
                short unknown = (short) trigger.getUserData("unknown");
                value = (int) trigger.getUserData("value");
                if (partyController.isCreated()) {
                    for (ICreatureController creature : partyController.getActualMembers()) {
                        if (creature.isDead()) {
                            target++;
                        }
                    }
                    break;
                }
                return false;

            case PARTY_MEMBERS_CAPTURED:
                value = (int) trigger.getUserData("value");
                break;

            case PARTY_MEMBERS_INCAPACITATED:
                unknown = (short) trigger.getUserData("unknown");
                value = (int) trigger.getUserData("value");
                if (partyController.isCreated()) {
                    for (ICreatureController creature : partyController.getActualMembers()) {
                        if (creature.isIncapacitated()) {
                            target++;
                        }
                    }
                    break;
                }
                return false;

            default:
                return super.isActive(trigger);
        }

        TriggerGeneric.ComparisonType comparisonType = trigger.getComparison();
        if (comparisonType != null && comparisonType != TriggerGeneric.ComparisonType.NONE) {
            result = compare(target, comparisonType, value);
        }

        return result;
    }
}
