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
 * These are the identifiers for each different data type
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public enum MapDataTypeEnum implements IValueEnum {

    GLOBALS(0), // As in overrides the regular ones
    MAP(100),
    TERRAIN(110),
    ROOMS(120),
    TRAPS(130),
    DOORS(140),
    KEEPER_SPELLS(150),
    CREATURE_SPELLS(160),
    CREATURES(170),
    PLAYERS(180),
    THINGS(190),
    TRIGGERS(210),
    LEVEL(220),
    VARIABLES(230),
    OBJECTS(240),
    EFFECT_ELEMENTS(250),
    SHOTS(260),
    EFFECTS(270);

    private MapDataTypeEnum(int id) {
        this.id = id;
    }

    @Override
    public int getValue() {
        return id;
    }
    private final int id;
}
