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
package toniarts.openkeeper.game.controller.creature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.door.IDoorController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Utils;

/**
 * Represents a party, a group of creatures. When the Leader is incapacitated, a
 * new Leader is chosen at random. Any triggers and actions which hung off the
 * old Leader are transferred to the new.
 *
 * @author ArchDemon
 */
public final class PartyController implements IPartyController {

    private static final Logger logger = System.getLogger(PartyController.class.getName());
    private static final AtomicLong PARTY_ID_GENERATOR = new AtomicLong(1);

    private long id;
    private final short partyId;
    private final int triggerId;
    private final String name;
    private final SequencedMap<Thing.GoodCreature, ICreatureController> members;
    private final List<ICreatureController> actualMembers = new ArrayList<>();
    private final Collection<ICreatureController> actualMembersView = Collections.unmodifiableList(actualMembers);
    private final Thing.GoodCreature leaderDefinition;
    private PartyType type;
    private ICreatureController leader;
    private PartyObjectiveOrder objectiveOrder;
    private boolean created = false;

    public PartyController(Thing.HeroParty heroParty) {
        Objects.requireNonNull(heroParty, "heroParty");
        partyId = heroParty.getId();
        name = heroParty.getName();
        triggerId = heroParty.getTriggerId();
        List<Thing.GoodCreature> definitions = heroParty.getHeroPartyMembers() != null
                ? heroParty.getHeroPartyMembers().stream().filter(Objects::nonNull).toList() : List.of();
        leaderDefinition = selectLeaderMember(definitions);
        long flaggedLeaderCount = definitions.stream().filter(PartyController::isLeaderDefinition).count();
        if (!definitions.isEmpty() && flaggedLeaderCount == 0) {
            logger.log(Level.WARNING, "Hero party {0} has no leader flag; using its first member as leader", partyId);
        } else if (flaggedLeaderCount > 1) {
            logger.log(Level.WARNING, "Hero party {0} has {1} leader flags; using the first flagged member", partyId, flaggedLeaderCount);
        }

        members = new LinkedHashMap<>();
        for (Thing.GoodCreature creature : definitions) {
            members.put(creature, null);
        }
        objectiveOrder = leaderDefinition != null
                ? new PartyObjectiveOrder(leaderDefinition.getObjective(), leaderDefinition.getObjectiveTargetPlayerId(),
                        leaderDefinition.getObjectiveTargetActionPointId(), null)
                : null;
    }

    /**
     * Selects the authored leader, falling back to the first member when a map
     * omits the flag. The same deterministic rule is used by trigger binding.
     *
     * @param heroParty the map party definition
     * @return the selected leader member, or {@code null} for an empty party
     */
    public static Thing.GoodCreature selectLeaderMember(Thing.HeroParty heroParty) {
        if (heroParty == null || heroParty.getHeroPartyMembers() == null) {
            return null;
        }
        return selectLeaderMember(heroParty.getHeroPartyMembers());
    }

    private static Thing.GoodCreature selectLeaderMember(List<Thing.GoodCreature> definitions) {
        return definitions.stream().filter(Objects::nonNull).filter(PartyController::isLeaderDefinition)
                .findFirst().orElseGet(() -> definitions.stream().filter(Objects::nonNull).findFirst().orElse(null));
    }

    private static boolean isLeaderDefinition(Thing.GoodCreature creature) {
        return creature.getFlags() != null && creature.getFlags().contains(Thing.Creature.CreatureFlag.LEADER);
    }

    @Override
    public void create() {
        id = PARTY_ID_GENERATOR.getAndIncrement();
        created = true;
    }

    @Override
    public boolean isCreated() {
        return created;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public short getPartyId() {
        return partyId;
    }

    @Override
    public int getTriggerId() {
        return triggerId;
    }

    @Override
    public PartyObjectiveOrder getObjectiveOrder() {
        return objectiveOrder;
    }

    @Override
    public void setObjectiveOrder(PartyObjectiveOrder order) {
        objectiveOrder = order;
        if (leader != null) {
            leader.setPartyObjectiveOrder(order);
        }
    }

    @Override
    public PartyType getType() {
        return type;
    }

    @Override
    public void setType(PartyType type) {
        this.type = type;
    }

    /**
     * Get members
     *
     * @see #getActualMembers()
     * @return the members
     */
    @Override
    public Set<Thing.GoodCreature> getMembers() {
        return members.keySet();
    }

    @Override
    public void addMemberInstance(Thing.GoodCreature creature, ICreatureController entity) {
        if (creature == null || entity == null || !members.containsKey(creature)) {
            logger.log(Level.WARNING, "Ignoring invalid member instance for hero party {0}", partyId);
            return;
        }
        ICreatureController previous = members.put(creature, entity);
        if (previous == null) {
            actualMembers.add(entity);
        } else if (previous != entity) {
            int memberIndex = actualMembers.indexOf(previous);
            if (memberIndex < 0) {
                actualMembers.add(entity);
            } else {
                actualMembers.set(memberIndex, entity);
            }
        }

        if (creature.equals(leaderDefinition)) {
            leader = entity;
            leader.setPartyObjectiveOrder(objectiveOrder);
        }
    }

    @Override
    public Collection<ICreatureController> getActualMembers() {
        return actualMembersView;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ICreatureController getPartyLeader() {
        checkPartyLeader();
        return leader;
    }

    /**
     * Test if the given creature is the party leader
     *
     * @param creature the creature to test
     * @return true if the creature is the party leader
     */
    @Override
    public boolean isPartyLeader(ICreatureController creature) {
        return creature != null && Objects.equals(creature, getPartyLeader());
    }

    /**
     * A party member is incapacitated, if it was the leader. Swap the duties
     * and objectives
     */
    private void checkPartyLeader() {
        if (isActiveMember(leader)) {
            return;
        }

        List<ICreatureController> leaderCandidates = new ArrayList<>();
        for (ICreatureController member : getActualMembers()) {
            if (isActiveMember(member)) {
                leaderCandidates.add(member);
            }
        }

        leader = leaderCandidates.isEmpty() ? null : Utils.getRandomItem(leaderCandidates);
        if (leader != null) {
            leader.setPartyObjectiveOrder(objectiveOrder);
        }
    }

    private static boolean isActiveMember(ICreatureController creature) {
        return creature != null && !creature.isRemoved() && !creature.isDead() && !creature.isIncapacitated();
    }

    /**
     * Whether this party has access to workers. Defines the tactic we can use
     * to go kill the enemy.
     *
     * @return does the party contain workers
     */
    @Override
    public boolean isWorkersAvailable() {
        for (ICreatureController creature : getActualMembers()) {
            if (isActiveMember(creature) && creature.isWorker()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public short getOwnerId() {
        return Player.GOOD_PLAYER_ID; // Always good player
    }

    @Override
    public boolean canFly() {
        boolean hasActiveMember = false;
        for (ICreatureController creature : getActualMembers()) {
            if (!isActiveMember(creature)) {
                continue;
            }
            hasActiveMember = true;
            if (!creature.canFly()) {
                return false;
            }
        }
        return hasActiveMember;
    }

    @Override
    public boolean canWalkOnWater() {
        boolean hasActiveMember = false;
        for (ICreatureController creature : getActualMembers()) {
            if (!isActiveMember(creature)) {
                continue;
            }
            hasActiveMember = true;
            if (!creature.canWalkOnWater()) {
                return false;
            }
        }
        return hasActiveMember;
    }

    @Override
    public boolean canWalkOnLava() {
        boolean hasActiveMember = false;
        for (ICreatureController creature : getActualMembers()) {
            if (!isActiveMember(creature)) {
                continue;
            }
            hasActiveMember = true;
            if (!creature.canWalkOnLava()) {
                return false;
            }
        }
        return hasActiveMember;
    }

    @Override
    public Float getCost(IMapTileInformation from, IMapTileInformation to, IMapController mapController, IEntityPositionLookup entityPositionLookup) {
        Float cost = IPartyController.super.getCost(from, to, mapController, entityPositionLookup);
        if (cost == null) {

            // No path by ordinary means, but we might want to tunnel or smash our way through obstacles
            Terrain terrain = mapController.getTerrain(to);
            if (isWorkersAvailable()) {
                Float tunnelCost = Tunneller.getTunnelCost(terrain);
                if (tunnelCost != null) {
                    return tunnelCost;
                }
            }

            // Check if any obstacles lies in our path, we can smash through enemy doors but not our own locked doors
            // FIXME: now just doors
            for (IDoorController doorController : entityPositionLookup.getEntityTypesInLocation(to, IDoorController.class)) {
                if (doorController.getOwnerId() != getOwnerId()) {
                    return 3f;
                }
            }
        }
        return cost;
    }

    @Override
    public boolean canMoveDiagonally() {

        // Maybe not a perfect solution, but if workers don't allow diagonal paths so that we don't dig diagonally
        return !isWorkersAvailable();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (int) (this.id ^ (this.id >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PartyController other = (PartyController) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

}
