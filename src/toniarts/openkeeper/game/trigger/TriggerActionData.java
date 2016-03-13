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
package toniarts.openkeeper.game.trigger;

import toniarts.openkeeper.tools.convert.map.TriggerAction;

/**
 *
 * @author ArchDemon
 */


public class TriggerActionData extends TriggerData {

    private TriggerAction.ActionType actionType;

    public TriggerActionData() {
        super();
    }

    public TriggerActionData(int id) {
        super(id);
    }

    public TriggerActionData(int id, TriggerGenericData parent) {
        super(id, parent);
    }

    public TriggerActionData(int id, TriggerGenericData parent, TriggerAction.ActionType actionType) {
        super(id, parent);
        this.actionType = actionType;
    }

    public TriggerAction.ActionType getType() {
        return actionType;
    }

    protected void setType(TriggerAction.ActionType actionType) {
        this.actionType = actionType;
    }

    /**
     * <code>removeFromParent</code> removes this TriggerActionData from it's parent.
     *
     * @return true if it has a parent and performed the remove.
     */
    public boolean removeFromParent() {
        if (parent != null) {
            parent.detachChild(this);
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TriggerActionData other = (TriggerActionData) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "TriggerActionData { id=" + id + ", action=" + actionType + " }";
    }
}
