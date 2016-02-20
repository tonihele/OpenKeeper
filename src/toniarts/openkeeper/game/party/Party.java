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
package toniarts.openkeeper.game.party;

import java.util.List;
import toniarts.openkeeper.game.control.Container;
import toniarts.openkeeper.tools.convert.IValueEnum;
import toniarts.openkeeper.tools.convert.map.Thing;

/**
 *
 * @author ArchDemon
 */


public class Party extends Container {

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
    private int id;
    private int triggerId;
    private String name;
    private List<Thing.GoodCreature> members;
    private Type type;
    private boolean created = false;

    public Party(Thing.HeroParty heroParty) {
        id = heroParty.getId();
        name = heroParty.getName();
        triggerId = heroParty.getTriggerId();
        members = heroParty.getHeroPartyMembers();
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

    public List<Thing.GoodCreature> getMembers() {
        return members;
    }
}
