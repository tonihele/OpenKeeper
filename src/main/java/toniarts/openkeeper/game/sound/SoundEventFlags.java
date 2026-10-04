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

/**
 * Event flag bits as stored in {@code SFX.map}.
 */
public final class SoundEventFlags {

    public static final int DEFERRED = 0x0001;
    public static final int SEGMENTED = 0x0002;
    public static final int REPEATING = 0x0004;
    public static final int LOOP_EACH = 0x0008;
    public static final int LINEAR_SEQUENCE = 0x0010;
    public static final int BACKEND_EVENT = 0x0020;
    public static final int HEAD_RELATIVE = 0x0200;
    public static final int PROPERTY_LINKS = 0x0400;

    private SoundEventFlags() {
    }

    public static boolean isSet(int flags, int mask) {
        return (flags & mask) != 0;
    }

    public static SoundVoiceBehaviour behaviourOf(int flags) {
        if (isSet(flags, BACKEND_EVENT)) {
            return SoundVoiceBehaviour.BACKEND_EVENT;
        }
        if (isSet(flags, REPEATING)) {
            if (isSet(flags, LOOP_EACH)) {
                return SoundVoiceBehaviour.REPEAT_LOOP_EACH;
            }
            if (isSet(flags, SEGMENTED)) {
                return SoundVoiceBehaviour.LINEAR_SEQUENCE;
            }
            return SoundVoiceBehaviour.REPEAT;
        }
        if (isSet(flags, SEGMENTED)) {
            return SoundVoiceBehaviour.PLAY_ONCE_SEGMENTED;
        }
        return SoundVoiceBehaviour.ONE_SHOT;
    }
}