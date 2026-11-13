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

import java.util.List;
import java.util.Objects;
import toniarts.openkeeper.game.controller.creature.ICreatureController;

/**
 * The task assignments needed to execute one party-level objective.
 *
 * <p>Most objectives assign one task to the leader. Objectives with a distinct
 * physical actor can add a companion assignment while keeping the leader as
 * the party's order owner and navigator.</p>
 *
 * @param assignments tasks and the creatures that execute them
 */
public record PartyObjectiveTaskPlan(List<Assignment> assignments) {

    public PartyObjectiveTaskPlan {
        assignments = List.copyOf(Objects.requireNonNull(assignments, "assignments"));
    }

    public static PartyObjectiveTaskPlan empty() {
        return new PartyObjectiveTaskPlan(List.of());
    }

    public static PartyObjectiveTaskPlan forLeader(ICreatureController leader, AbstractObjectiveTask task) {
        return task == null ? empty() : new PartyObjectiveTaskPlan(List.of(new Assignment(leader, task)));
    }

    /**
     * One objective task assigned to its executor.
     *
     * @param creature the creature to assign
     * @param task the objective task
     */
    public record Assignment(ICreatureController creature, AbstractObjectiveTask task) {

        public Assignment {
            Objects.requireNonNull(creature, "creature");
            Objects.requireNonNull(task, "task");
        }
    }
}
