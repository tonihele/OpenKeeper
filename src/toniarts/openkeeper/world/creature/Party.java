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
import java.util.List;
import toniarts.openkeeper.tools.convert.IValueEnum;
import toniarts.openkeeper.tools.convert.map.Thing;

/**
 * Represents a party, a group of creatures
 *
 * @author ArchDemon
 */
public class Party {

    public enum Type implements IValueEnum {

        NONE(0x0),
        INVANSION_PARTY(0x1),
        RANDOM_CREATYRE_TYPES(0x2);

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
    private final List<Thing.GoodCreature> members;
    private final List<CreatureControl> actualMembers;
    private Type type;
    private boolean created = false;

    public Party(Thing.HeroParty heroParty) {
        id = heroParty.getId();
        name = heroParty.getName();
        triggerId = heroParty.getTriggerId();
        members = heroParty.getHeroPartyMembers();
        actualMembers = new ArrayList<>(members.size());
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
    public List<Thing.GoodCreature> getMembers() {
        return members;
    }

    public void addMemberInstance(CreatureControl creatureInstance) {
        actualMembers.add(creatureInstance);
    }

    /**
     * Get the party members, as real instances, if the party has been created
     *
     * @return the member instances
     */
    public List<CreatureControl> getActualMembers() {
        return actualMembers;
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

        // TODO: now just the first one, and check if he is alive
        return getActualMembers().get(0);
    }

}
