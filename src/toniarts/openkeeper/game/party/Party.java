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

import java.util.EnumSet;
import java.util.List;
import toniarts.openkeeper.game.control.Container;
import toniarts.openkeeper.tools.convert.map.Thing;

/**
 *
 * @author ArchDemon
 */


public class Party extends Container {

    private int id;
    private int triggerId;
    private String name;
    List<Thing.GoodCreature> members;
    private boolean created = false;

    public Party(Thing.HeroParty heroParty) {
        id = heroParty.getId();
        name = heroParty.getName();
        //objective = heroParty.get();
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

    protected void setCreated(boolean created) {
        this.created = created;
    }
}
