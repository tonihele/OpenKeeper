/*
 * Copyright (C) 2014-2018 OpenKeeper
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

import java.util.Collection;
import java.util.Set;
import toniarts.openkeeper.tools.convert.map.Thing;

/**
 * Creature party controller interface
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IPartyController {

    /**
     * Get the party members, as real instances, if the party has been created
     *
     * @return the member instances
     */
    Collection<ICreatureController> getActualMembers();

    /**
     * Get the unique ID of this party
     *
     * @return the unique ID
     */
    long getId();

    String getName();

    short getOwnerId();

    /**
     * Get the party ID. The party ID is defined in the map. There can be
     * multiple parties with the same party ID.
     *
     * @return the party ID
     */
    short getPartyId();

    /**
     * Get the leader of this party
     *
     * @return party leader instance
     */
    ICreatureController getPartyLeader();

    int getTriggerId();

    PartyType getType();

    /**
     * Test if the given creature is the party leader
     *
     * @param creature the creature to test
     * @return true if the creature is the party leader
     */
    boolean isPartyLeader(ICreatureController creature);

    /**
     * Whether this party has access to workers. Defines the tactic we can use
     * to go kill the enemy.
     *
     * @return does the party contain workers
     */
    boolean isWorkersAvailable();

    void setType(PartyType type);

    boolean isCreated();

    Set<Thing.GoodCreature> getMembers();

    void addMemberInstance(Thing.GoodCreature creature, ICreatureController entity);

    /**
     * Marks the party as created
     */
    void create();

}
