/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.game.controller.creature;

import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Point;

/**
 * The current command issued to a hero party through its leader.
 *
 * <p>The order is runtime state owned by the party. Creature objective
 * components remain the serialized/map-facing representation; this value keeps
 * all command parameters together while the party changes leaders or receives
 * a trigger-driven objective update.</p>
 *
 * @param objective the party objective, or {@code null} when none is set
 * @param targetPlayerId the player targeted by the objective
 * @param actionPointId the action point targeted by the objective
 * @param waitPosition the position held by a WAIT order
 */
public record PartyObjectiveOrder(Thing.HeroParty.Objective objective, short targetPlayerId,
        int actionPointId, Point waitPosition) {

    public PartyObjectiveOrder {
        if (objective != Thing.HeroParty.Objective.WAIT) {
            waitPosition = null;
        }
    }

    public PartyObjectiveOrder withObjective(Thing.HeroParty.Objective newObjective, Point currentPosition) {
        Point newWaitPosition = newObjective == Thing.HeroParty.Objective.WAIT ? currentPosition : null;
        return new PartyObjectiveOrder(newObjective, targetPlayerId, actionPointId, newWaitPosition);
    }

    public PartyObjectiveOrder withTargetPlayerId(short newTargetPlayerId) {
        return new PartyObjectiveOrder(objective, newTargetPlayerId, actionPointId, waitPosition);
    }

    public PartyObjectiveOrder withActionPointId(int newActionPointId) {
        return new PartyObjectiveOrder(objective, targetPlayerId, newActionPointId, waitPosition);
    }

    public PartyObjectiveOrder withWaitPosition(Point newWaitPosition) {
        return objective == Thing.HeroParty.Objective.WAIT
                ? new PartyObjectiveOrder(objective, targetPlayerId, actionPointId, newWaitPosition)
                : this;
    }
}
