/*
 * Copyright (C) 2014-2026 OpenKeeper
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

/** Resolves level countdown warnings to the localized COUNT mentor recordings. */
final class TimeLimitSpeech {
    private TimeLimitSpeech() {
    }

    static int forSeconds(int seconds) {
        if (seconds >= 1 && seconds <= 10) {
            return 19 - seconds;
        }
        return switch (seconds) {
            case 1800 -> MentorType.COUNT001.getId();
            case 1200 -> MentorType.COUNT002.getId();
            case 600 -> MentorType.COUNT003.getId();
            case 300 -> MentorType.COUNT004.getId();
            case 240 -> MentorType.COUNT005.getId();
            case 180 -> MentorType.COUNT006.getId();
            case 120 -> MentorType.COUNT007.getId();
            case 60 -> MentorType.COUNT008.getId();
            default -> 0;
        };
    }
}
