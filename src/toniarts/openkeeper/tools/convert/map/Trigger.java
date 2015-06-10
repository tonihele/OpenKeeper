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
package toniarts.openkeeper.tools.convert.map;

/**
 * Container class for *Triggers.kld
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class Trigger {

    protected KwdFile kwdFile; // For toStrings()
    private int id;
    private int idNext; // SiblingID

    public Trigger(KwdFile kwdFile) {
        this.kwdFile = kwdFile;
    }

    public int getId() {
        return id;
    }

    protected void setId(int id) {
        this.id = id;
    }

    public int getIdNext() {
        return idNext;
    }

    protected void setIdNext(int id) {
        this.idNext = id;
    }

    /**
     * Does this trigger have children
     *
     * @see #getIdChild()
     * @return true if we have for sure
     */
    public abstract boolean hasChildren();

    /**
     * Get the child trigger id
     *
     * @see #hasChildren()
     * @return the child ID, or 0 if no children
     */
    public abstract int getIdChild();

    @Override
    public int hashCode() {
        int hash = 5;
        return hash;
    }

    @Override
    public boolean equals(java.lang.Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Trigger other = (Trigger) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }
}
