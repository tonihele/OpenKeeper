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

import com.simsilica.es.EntityData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import toniarts.openkeeper.game.component.StolenGold;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.creature.IPartyController;
import toniarts.openkeeper.tools.convert.map.Creature;

/**
 * Selects the party member that can physically perform a STEAL_GOLD order.
 * The party leader remains the order owner; an eligible Thief/Rogue or an
 * already carrying raider performs the physical task.
 */
public final class PartyObjectiveActorSelector {

    private static final List<Creature.JobType> STEAL_JOBS = List.of(
            Creature.JobType.STEAL_GOLD, Creature.JobType.STEAL_ENEMY_GOLD);

    private final EntityData entityData;

    public PartyObjectiveActorSelector(EntityData entityData) {
        this.entityData = Objects.requireNonNull(entityData, "entityData");
    }

    /**
     * Select an active, capable party member. A creature already carrying
     * stolen gold takes precedence so an interrupted escape can resume.
     *
     * @param leader the active party leader
     * @return the raider, or {@code null} when nobody can steal
     */
    public ICreatureController selectStealGoldActor(ICreatureController leader) {
        if (leader == null) {
            return null;
        }
        IPartyController party = leader.getParty();
        Collection<ICreatureController> members = party != null ? party.getActualMembers() : List.of();
        List<ICreatureController> activeMembers = new ArrayList<>();
        if (members != null) {
            members.stream().filter(PartyObjectiveActorSelector::isActive).forEach(activeMembers::add);
        }
        if (isActive(leader) && !activeMembers.contains(leader)) {
            activeMembers.add(leader);
        }

        for (ICreatureController member : activeMembers) {
            if (hasStolenGold(member)) {
                return member;
            }
        }
        if (activeMembers.contains(leader) && canStealGold(leader.getCreature())) {
            return leader;
        }
        return activeMembers.stream().filter(member -> canStealGold(member.getCreature())).findFirst().orElse(null);
    }

    /**
     * Whether the creature data advertises a Treasury-stealing job.
     *
     * @param creature creature definition
     * @return whether the creature can perform the physical theft action
     */
    public static boolean canStealGold(Creature creature) {
        return creature != null
                && (hasJob(creature.getHappyJobs()) || hasJob(creature.getUnhappyJobs())
                || hasJob(creature.getAngryJobs()) || hasAlternativeJob(creature.getAlternativeJobs()));
    }

    private boolean hasStolenGold(ICreatureController creature) {
        return entityData.getComponent(creature.getEntityId(), StolenGold.class) != null;
    }

    private static boolean isActive(ICreatureController creature) {
        return creature != null && !creature.isRemoved() && !creature.isDead() && !creature.isIncapacitated();
    }

    private static boolean hasJob(Creature.JobPreference[] preferences) {
        return preferences != null && Arrays.stream(preferences)
                .anyMatch(preference -> preference != null && STEAL_JOBS.contains(preference.getJobType()));
    }

    private static boolean hasAlternativeJob(Creature.JobAlternative[] alternatives) {
        return alternatives != null && Arrays.stream(alternatives)
                .anyMatch(alternative -> alternative != null && STEAL_JOBS.contains(alternative.getJobType()));
    }
}
