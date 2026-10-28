/*
 * Copyright (C) 2014-2025 OpenKeeper
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

import toniarts.openkeeper.tools.convert.IFlagEnum;

/**
 * Event flag bits as stored in {@code SFX.map}.
 */
public enum SoundEventFlag implements IFlagEnum {

    DEFERRED(0x0001),
    SEGMENTED(0x0002),
    REPEATING(0x0004),
    LOOP_EACH(0x0008),
    LINEAR_SEQUENCE(0x0010),
    BACKEND_EVENT(0x0020),
    HEAD_RELATIVE(0x0200),
    PROPERTY_LINKS(0x0400);

    private final long flagValue;

    SoundEventFlag(long flagValue) {
        this.flagValue = flagValue;
    }

    @Override
    public long getFlagValue() {
        return flagValue;
    }
}
