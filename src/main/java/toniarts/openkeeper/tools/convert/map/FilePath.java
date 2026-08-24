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

import java.util.Objects;

/**
 * Container for file paths found in in the KWD file
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class FilePath implements Comparable<FilePath> {

    private MapDataTypeEnum id; // unsigned int
    private int unknown2;
    private String path; // 64

    public FilePath() {
    }

    public FilePath(MapDataTypeEnum id, String path) {
        this.id = id;
        this.path = path;
        this.unknown2 = 0;
    }

    public MapDataTypeEnum getId() {
        return id;
    }

    protected void setId(MapDataTypeEnum id) {
        this.id = id;
    }

    public int getUnknown2() {
        return unknown2;
    }

    protected void setUnknown2(int unknown2) {
        this.unknown2 = unknown2;
    }

    public String getPath() {
        return path;
    }

    protected void setPath(String path) {
        this.path = path;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + Objects.hashCode(this.id);
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
        final FilePath other = (FilePath) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return getId() + ", " + getPath();
    }

    @Override
    public int compareTo(FilePath o) {
        return this.id == MapDataTypeEnum.MAP ? -1 : o.id == MapDataTypeEnum.MAP ? 1 : 0;
    }
}
