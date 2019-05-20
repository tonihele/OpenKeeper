/*
 * Copyright (C) 2014-2019 OpenKeeper
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

import toniarts.openkeeper.game.data.IIndexable;

/**
 * Converted/Sounds/speech_horny/back/speech_hornyHD contains older version of horny sound
 *
 * @deprecated temporary file. Maybe we should delete it
 * @author ArchDemon
 */
public enum HornyType implements IIndexable {

    HORNG014(1);

    /**
     *
     * @param value
     */
    private HornyType(int value) {
        this.value = (short) value;
    }

    private final short value;

    @Override
    public short getId() {
        return value;
    }
}
