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

import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.creature.IPartyController;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.game.trigger.TriggerControl;
import toniarts.openkeeper.game.trigger.TriggerGenericData;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

/**
 * The party scope. The tree of a hero party is dormant until the party is
 * created, and then everything in it is one-shot. Forwards what it does not
 * know to the level.
 */
public final class PartyTriggerControl extends TriggerControl {

    private final IPartyController partyController;

    public PartyTriggerControl(final IGameController gameController, final ILevelInfo levelInfo, final IGameTimer gameTimer,
            final IMapController mapController, final ICreaturesController creaturesController, int triggerId,
            final IPartyController partyController, final PlayerService playerService) {
        super(gameController, levelInfo, gameTimer, mapController, creaturesController, playerService, triggerId);
        this.partyController = partyController;

        // Dormant until the party exists
        setTreeArmed(false);
    }

    @Override
    protected void updateControl(float tpf) {

        // TODO: a party that is spawned again should arm the tree again so that it runs from the
        // start. There is no notification about that, the party controller just stays created
        if (!isArmed() && partyController.isCreated()) {
            setTreeArmed(true);
        }
        super.updateControl(tpf);
    }

    /**
     * The party trees are always walked with the repeat 1
     */
    @Override
    protected short getRootRepeat() {
        return 1;
    }

    @Override
    protected boolean isActive(TriggerGenericData trigger) {
        TriggerGeneric.TargetType targetType = trigger.getType();
        switch (targetType) {
            case PARTY_CREATED:
                return partyController.isCreated();

            case PARTY_MEMBERS_KILLED:
                return compare(trigger, getDefinedCount() - countMembers(false), trigger.getUserData("value", int.class));

            case PARTY_MEMBERS_CAPTURED:

                // A member that is dead, or that is no more with the good, is captured
                return compare(trigger, getDefinedCount() - countMembers(true), trigger.getUserData("value", int.class));

            case PARTY_MEMBERS_INCAPACITATED:
                return compare(trigger, getDefinedCount() - countAbleMembers(), trigger.getUserData("value", int.class));

            default:
                return super.isActive(trigger);
        }
    }

    /**
     * How many members the party is defined with. Counts also the members
     * that have not been spawned yet.
     */
    private int getDefinedCount() {
        return partyController.getMembers().size();
    }

    /**
     * Count the members that are alive
     *
     * @param onlyGood count only the ones that are still on the good side
     */
    private int countMembers(boolean onlyGood) {
        int count = 0;
        if (partyController.isCreated()) {
            for (ICreatureController creature : partyController.getActualMembers()) {
                if (!creature.isDead() && (!onlyGood || creature.getOwnerId() == Player.GOOD_PLAYER_ID)) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countAbleMembers() {
        int count = 0;
        if (partyController.isCreated()) {
            for (ICreatureController creature : partyController.getActualMembers()) {
                if (!creature.isDead() && !creature.isIncapacitated()) {
                    count++;
                }
            }
        }
        return count;
    }
}
