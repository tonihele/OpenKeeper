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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

/**
 * Container class for *Triggers.kld
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class Trigger {

    protected KwdFile kwdFile; // For toStrings()
    private int id;
    private int idNext; // SiblingID
    private int idChild; // ChildID
    private short repeatTimes; // Repeat x times, 255 = always
    protected HashMap<String, Number> userData = null;
    private boolean closed = false;

    public Trigger(KwdFile kwdFile) {
        this.kwdFile = kwdFile;
    }

    public void setUserData(String key, Number data) {
        if (userData == null) {
            userData = new HashMap<>();
        }

        if (data == null) {
            userData.remove(key);
        } else if (data instanceof Number) {
            userData.put(key, data);
        } else {
            throw new RuntimeException("unexpected value");
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getUserData(String key) {
        if (userData == null) {
            return null;
        }

        Number s = userData.get(key);
        return (T) s;
    }

    public Collection<String> getUserDataKeys() {
        if (userData != null) {
            return userData.keySet();
        }

        return Collections.EMPTY_SET;
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
     * Get the child trigger id
     *
     * @see #hasChildren()
     * @return the child ID, or 0 if no children
     */
    public int getIdChild() {
        return idChild;
    }

    protected void setIdChild(int id) {
        this.idChild = id;
    }

    public short getRepeatTimes() {
        return repeatTimes;
    }

    protected void setRepeatTimes(short repeatTimes) {
        this.repeatTimes = repeatTimes;
    }

    public void subRepeatTimes() {
        if (repeatTimes != 255 && !closed) {
            if (--repeatTimes <= 0) {
                closed = true;
            }
        }
    }

    /**
     * Does this trigger have next trigger
     *
     * @see #getIdNext()
     * @return true if we have for sure
     */
    public boolean hasNext() {
        return (getIdNext() != 0);
    }

    /**
     * Does this trigger have children
     *
     * @see #getIdChild()
     * @return true if we have for sure
     */
    public boolean hasChildren() {
        return (getIdChild() != 0);
    }

    public boolean isClosed() {
        return closed;
    }

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
