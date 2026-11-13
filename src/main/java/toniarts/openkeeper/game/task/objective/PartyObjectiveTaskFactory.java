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
package toniarts.openkeeper.game.task.objective;

import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.tools.convert.map.Thing;

/**
 * Creates the assignments used to execute a hero-party leader's objective.
 *
 * <p>TaskManager owns the default objective mappings and accepts overrides so
 * map-specific or future DK2 objectives can add coordinated actor tasks
 * without changing TaskManager's general assignment flow.</p>
 */
@FunctionalInterface
public interface PartyObjectiveTaskFactory {

    /**
     * Create the objective task plan for the current party leader.
     *
     * @param leader the creature executing the party order
     * @param objective the authoritative objective
     * @return the plan, which may be empty when the order has no actionable
     * target yet
     */
    PartyObjectiveTaskPlan create(ICreatureController leader, Thing.HeroParty.Objective objective);
}
