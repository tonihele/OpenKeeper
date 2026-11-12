/*
 * Copyright (C) 2014-2018 OpenKeeper
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
package toniarts.openkeeper.game.controller.creature;

import toniarts.openkeeper.tools.convert.IValueEnum;

/**
 * Party type
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public enum PartyType implements IValueEnum {

    NONE(0x0),
    INVASION_PARTY(0x1),
    RANDOM_CREATURE_TYPES(0x2);

    private PartyType(int id) {
        this.id = id;
    }

    @Override
    public int getValue() {
        return id;
    }

    private final int id;

}
