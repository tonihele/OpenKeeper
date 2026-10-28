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
package toniarts.openkeeper.game.state;

/**
 * The cheats a keeper can type in
 */
public enum CheatType {

    MONEY("show me the money"),
    MANA("ha ha thisaway ha ha thataway"),
    LEVEL_MAX("feel the power"),
    UNLOCK_ROOMS("this is my church"),
    UNLOCK_SPELLS("i believe its magic"),
    UNLOCK_DOORS_TRAPS("fit the best"),
    REMOVE_FOW("now the rain has gone"),
    RESET_FOW("now the rain has come back"),
    LOOSE_LEVEL("fear the reaper"),
    WIN_LEVEL("do not fear the reaper");
    private final String cheatMessage;

    private CheatType(final String cheatMessage) {
        this.cheatMessage = cheatMessage;
    }

    public String getMessage() {
        return this.cheatMessage;
    }

    public static CheatType getCheatType(final String cheatMessage) {
        for (CheatType type : CheatType.values()) {
            if (type.getMessage().equals(cheatMessage)) {
                return type;
            }
        }
        return null;
    }

    public static boolean hasCheat(final String cheatMessage) {
        for (CheatType type : CheatType.values()) {
            if (type.getMessage().startsWith(cheatMessage)) {
                return true;
            }
        }
        return false;
    }
}
