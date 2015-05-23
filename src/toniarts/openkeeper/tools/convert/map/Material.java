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

import toniarts.openkeeper.tools.convert.IValueEnum;

/**
 * Enumeration for material value found in map files
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public enum Material implements IValueEnum {

    NONE(0),
    FLESH(1),
    ROCK(2),
    WOOD(3),
    METAL1(4),
    METAL2(5),
    MAGIC(6),
    GLASS(7);

    private Material(int id) {
        this.id = id;
    }

    @Override
    public int getValue() {
        return id;
    }
    private final int id;
}
