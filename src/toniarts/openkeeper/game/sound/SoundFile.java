/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.game.sound;

/**
 *
 * @author archdemon
 */
public class SoundFile implements Comparable<SoundFile> {
    private final int id;
    private final SoundGroup action;
    private final String filename;

    public SoundFile(SoundGroup action, int id, String filename) {
        this.action = action;
        this.id = id;
        this.filename = filename;
    }

    public SoundGroup getAction() {
        return action;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + this.id;
        hash = 97 * hash + this.action.getId();
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
        final SoundFile other = (SoundFile) obj;
        if (this.id != other.id) {
            return false;
        }
        if (this.action.getId() != other.action.getId()) {
            return false;
        }
        return true;
    }


    @Override
    public String toString() {
        return String.format("%s", filename);
        //return String.format("%d %d %s", action.getId(), id, filename);
    }

    @Override
    public int compareTo(SoundFile o) {
        if (action.getId() == o.action.getId()) {
            return (id - o.id);
        }
        return (action.getId() - o.action.getId());
    }
}
