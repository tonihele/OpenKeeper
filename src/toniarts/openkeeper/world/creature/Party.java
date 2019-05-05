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
package toniarts.openkeeper.world.creature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.tools.convert.IValueEnum;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.door.DoorControl;
import toniarts.openkeeper.world.pathfinding.PathFindable;

/**
 * Represents a party, a group of creatures. When the Leader is incapacitated, a
 * new Leader is chosen at random. Any triggers and actions which hung off the
 * old Leader are transferred to the new.
 *
 * @author ArchDemon
 */
@Deprecated
public class Party implements PathFindable {

    public enum Type implements IValueEnum {

        NONE(0x0),
        INVASION_PARTY(0x1),
        RANDOM_CREATURE_TYPES(0x2);

        private Type(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    };
    private final int id;
    private final int triggerId;
    private final String name;
    private final Map<Thing.GoodCreature, CreatureControl> members;
    private Type type;
    private boolean created = false;
    private CreatureControl leader;

    public Party(Thing.HeroParty heroParty) {
        id = heroParty.getId();
        name = heroParty.getName();
        triggerId = heroParty.getTriggerId();
        members = new LinkedHashMap<>(heroParty.getHeroPartyMembers().size());
        for (Thing.GoodCreature creature : heroParty.getHeroPartyMembers()) {
            members.put(creature, null);
        }
    }

    public int getId() {
        return id;
    }

    public int getTriggerId() {
        return triggerId;
    }

    public boolean isCreated() {
        return created;
    }

    public void setCreated(boolean created) {
        this.created = created;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Get members
     *
     * @see #getActualMembers()
     * @return the members
     */
    public Set<Thing.GoodCreature> getMembers() {
        return members.keySet();
    }

    public void addMemberInstance(Thing.GoodCreature creature, CreatureControl creatureInstance) {
        members.put(creature, creatureInstance);

        if (leader == null && creature.getFlags().contains(Thing.Creature.CreatureFlag.LEADER)) {
            leader = creatureInstance;
        }
    }

    /**
     * Get the party members, as real instances, if the party has been created
     *
     * @return the member instances
     */
    public Collection<CreatureControl> getActualMembers() {
        return members.values();
    }

    public String getName() {
        return name;
    }

    /**
     * Get the leader of this party
     *
     * @return party leader instance
     */
    public CreatureControl getPartyLeader() {
        return leader;
    }

    /**
     * Test if the given creature is the party leader
     *
     * @param creature the creature to test
     * @return true if the creature is the party leader
     */
    public boolean isPartyLeader(CreatureControl creature) {
        return creature.equals(getPartyLeader());
    }

    /**
     * A party member is incapacitated, if it was the leader. Swap the duties
     * and objectives
     *
     * @param creature the creature incapacitated
     */
    protected void partyMemberIncapacitated(CreatureControl creature) {
        if (isPartyLeader(creature)) {
            List<CreatureControl> leaderCandidates = new ArrayList<>(getActualMembers());
            Iterator<CreatureControl> iter = leaderCandidates.iterator();
            while (iter.hasNext()) {
                CreatureControl c = iter.next();
                if (c.isIncapacitated() || c.equals(creature)) {
                    iter.remove();
                }
            }

            // See if any left
            if (!leaderCandidates.isEmpty()) {
                leader = Utils.getRandomItem(leaderCandidates);

                // Swap duties
                leader.setObjectiveTargetActionPoint(creature.getObjectiveTargetActionPoint());
                leader.setObjective(creature.getObjective());
            }
        }
    }

    /**
     * Whether this party has access to workers. Defines the tactic we can use
     * to go kill the enemy.
     *
     * @return does the party contain workers
     */
    public boolean isWorkersAvailable() {
        for (CreatureControl creature : getActualMembers()) {
            if (!creature.isIncapacitated() && creature.isWorker()) {
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
        for (CreatureControl creature : getActualMembers()) {
            if (!creature.isIncapacitated() && !creature.canFly()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canWalkOnWater() {
        for (CreatureControl creature : getActualMembers()) {
            if (!creature.isIncapacitated() && !creature.canWalkOnWater()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canWalkOnLava() {
        for (CreatureControl creature : getActualMembers()) {
            if (!creature.isIncapacitated() && !creature.canWalkOnLava()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Float getCost(TileData from, TileData to, WorldState worldState) {
        Float cost = PathFindable.super.getCost(from, to, worldState);
        if (cost == null) {

            // No path by ordinary means, but we might want to tunnel or smash our way through obstacles
            Terrain terrain = to.getTerrain();
            if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID) && isWorkersAvailable()) {
                if (terrain.getFlags().contains(Terrain.TerrainFlag.DWARF_CAN_DIG_THROUGH)) {
                    return 1.0f; // Dig our selves in
                }
                if (terrain.getFlags().contains(Terrain.TerrainFlag.ATTACKABLE)) {
                    return 2.0f; // It seems that everybody can attack reinforced walls i.e. ?
                }
            }

            // Check if any obstacles lies in our path, we can smash through enemy doors but not our own locked doors
            // FIXME: now just doors
            DoorControl doorControl = worldState.getThingLoader().getDoor(to.getLocation());
            if (doorControl != null && doorControl.getOwnerId() != getOwnerId()) {
                return 1.5f;
            }
        }
        return cost;
    }

    @Override
    public boolean canMoveDiagonally() {

        // Maybe not a perfect solution, but if workers don't allow diagonal paths so that we don't dig diagonally
        return !isWorkersAvailable();
    }

}
