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
package toniarts.openkeeper.game.data;

/**
 * The sub-tables of the {@link PlayerMessageType}s that have variants
 * The pairs are the key and the Speech.str index. Keys of the room tables
 * are the Rooms.kwd ids, 287 is the Mana Vault
 * (a terrain, not a room).
 *
 * @author ufdada
 */
final class PlayerMessageSubTables {

    private static final PlayerMessageSubTable SPELL_UPGRADED = PlayerMessageSubTable.of(0, 20,
            1, 42, 2, 41, 3, 46, 4, 39, 5, 40, 6, 43, 7, 50, 8, 44, 9, 45, 10, 47, 11, 51, 12, 48, 13, 49);

    private static final PlayerMessageSubTable ROOM_CAPTURED = PlayerMessageSubTable.of(77, 20,
            1, 62, 2, 63, 3, 71, 4, 64, 6, 74, 7, 65, 8, 76, 9, 75, 10, 68, 11, 73, 12, 72, 13, 67, 14, 70, 15, 69,
            16, 66, 17, 76, 22, 76, 287, 449);

    private static final PlayerMessageSubTable ROOM_CLAIMED = PlayerMessageSubTable.of(155, 20,
            1, 140, 2, 141, 3, 149, 4, 142, 6, 152, 7, 143, 8, 154, 9, 153, 10, 146, 11, 151, 12, 150, 13, 145, 14, 148,
            15, 147, 16, 144, 17, 154, 22, 154, 287, 449);

    private static final PlayerMessageSubTable ROOM_BEING_CAPTURED = PlayerMessageSubTable.of(139, 20,
            1, 124, 2, 125, 3, 133, 4, 126, 6, 136, 7, 127, 8, 138, 9, 137, 10, 130, 11, 135, 12, 134, 13, 129, 14, 132,
            15, 131, 16, 128, 17, 138, 22, 138, 287, 450);

    /**
     * Keys are the special event indexes, not the object ids
     */
    private static final PlayerMessageSubTable SPECIAL_EFFECT = PlayerMessageSubTable.of(488, 20,
            2, 489, 6, 491, 7, 492, 8, 493, 10, 494, 11, 495, 12, 496, 13, 497, 14, 498, 15, 499, 16, 500, 17, 501, 18, 502);

    private static final PlayerMessageSubTable NOT_ENOUGH_GOLD = PlayerMessageSubTable.of(445, 20,
            1, 1338, 2, 1339, 3, 1340, 4, 1341).with(5, 1342, 240);

    private PlayerMessageSubTables() {
    }

    /**
     * @param type the message type
     * @return the sub-table of the type, {@code null} if the type has no
     * variants
     */
    static PlayerMessageSubTable get(PlayerMessageType type) {
        return switch (type) {
            case SPELL_UPGRADED -> SPELL_UPGRADED;
            case ROOM_CAPTURED -> ROOM_CAPTURED;
            case ROOM_CLAIMED -> ROOM_CLAIMED;
            case ROOM_BEING_CAPTURED -> ROOM_BEING_CAPTURED;
            case SPECIAL_EFFECT -> SPECIAL_EFFECT;
            case NOT_ENOUGH_GOLD -> NOT_ENOUGH_GOLD;
            default -> null;
        };
    }
}
