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

    private static final PlayerMessageSubTable LORD_OF_THE_LAND_EVENT = PlayerMessageSubTable.of(0, 20,
            1, 1293, 2, 1294, 3, 1295, 4, 1296);

    private static final PlayerMessageSubTable KING_EVENT = PlayerMessageSubTable.of(0, 20,
            1, 1297, 2, 1298, 3, 1299, 4, 1300);

    /**
     * Keys are the crate object ids, not the trap or door ids
     */
    private static final PlayerMessageSubTable WORKSHOP_ITEM_BUILT = PlayerMessageSubTable.of(0, 30,
            5, 1399, 36, 1397, 37, 1404, 38, 1400, 39, 1402, 40, 1396, 41, 1401, 42, 1398, 43, 1405, 44, 1403,
            52, 1406, 53, 1408, 54, 1407, 55, 1409, 56, 1411, 57, 1410, 92, 1486);

    private static final PlayerMessageSubTable WORKERS_DISTURBED = PlayerMessageSubTable.of(1304, 20,
            1, 396, 2, 1301, 3, 361, 4, 1302, 5, 350, 6, 1303, 8, 1305);

    private static final PlayerMessageSubTable LORD_OF_THE_LAND_DEFEATED = PlayerMessageSubTable.of(0, 20,
            1, 34, 2, 35, 3, 35);

    private static final PlayerMessageSubTable ROOM_UNDER_ATTACK = PlayerMessageSubTable.of(0, 30,
            1, 156, 2, 157, 3, 165, 4, 158, 6, 168, 7, 159, 8, 170, 9, 169, 10, 162, 11, 167, 12, 166, 13, 161, 14, 164,
            15, 163, 16, 160, 17, 170, 22, 170).withDefault(171, 20);

    private static final PlayerMessageSubTable ROOM_DESTROYED = PlayerMessageSubTable.of(108, 20,
            1, 94, 2, 95, 4, 96, 6, 105, 7, 97, 8, 107, 9, 106, 10, 100, 11, 104, 12, 103, 13, 99, 14, 102, 15, 101,
            16, 98, 17, 107, 22, 107, 287, 450);

    private static final PlayerMessageSubTable ONE_OF_ROOMS_DESTROYED = PlayerMessageSubTable.of(123, 20,
            1, 109, 2, 110, 4, 111, 6, 120, 7, 112, 8, 122, 9, 121, 10, 115, 11, 119, 12, 118, 13, 114, 14, 117, 15, 116,
            16, 113, 17, 122, 22, 122, 287, 451);

    private static final PlayerMessageSubTable NEED_ROOM = PlayerMessageSubTable.of(0, 300,
            1, 1306, 2, 1307, 4, 1308, 14, 1309, 11, 1311, 12, 1310, 6, 1505, 10, 1506, 15, 1507, 7, 1508, 16, 1509,
            13, 1510, 9, 1511)
            .with(1, 1306, 420).with(2, 1307, 180).with(4, 1308, 180).with(14, 1309, 600).with(11, 1311, 600);

    private static final PlayerMessageSubTable ROOM_TOO_SMALL = PlayerMessageSubTable.of(212, 300,
            1, 197, 2, 198, 4, 199, 6, 209, 7, 200, 9, 210, 10, 203, 11, 208, 12, 207, 13, 202, 14, 205, 15, 204, 16, 201)
            .with(1, 197, 240).with(2, 198, 90).with(4, 199, 240);

    private static final PlayerMessageSubTable ROOM_UNREACHABLE = PlayerMessageSubTable.of(0, 300,
            1, 1312, 2, 1313, 3, 1321, 4, 1314, 6, 1326, 7, 1315, 9, 1327, 10, 1318, 11, 1325, 12, 1324, 13, 1317, 14, 1320,
            15, 1319, 16, 1316)
            .with(11, 1325, 360).with(12, 1324, 360);

    private static final PlayerMessageSubTable ROOM_NOT_USABLE = PlayerMessageSubTable.of(196, 240,
            1, 172, 2, 173, 4, 175, 6, 193, 7, 177, 9, 195, 10, 183, 11, 191, 12, 189, 13, 181, 14, 187, 15, 185, 16, 179)
            .with(1, 172, 120).with(2, 173, 120).with(4, 175, 120);

    private static final PlayerMessageSubTable ROOM_FULL = PlayerMessageSubTable.of(0, 120,
            6, 392, 10, 219);

    private static final PlayerMessageSubTable MANA_LOW = PlayerMessageSubTable.of(0, 20,
            1, 446, 2, 1328, 3, 1329, 4, 1330);

    private static final PlayerMessageSubTable NOT_ENOUGH_MANA = PlayerMessageSubTable.of(448, 20,
            1, 453, 2, 1331, 3, 1332, 4, 1333, 5, 1334);

    private static final PlayerMessageSubTable GOLD_LOW = PlayerMessageSubTable.of(0, 20,
            1, 444, 2, 1335, 3, 1336, 4, 1337);

    /**
     * Keys 1 - 17 are the outcomes, 18 - 45 the granted creatures in the
     * order of the table (Horny first, King at 41), 46 an unspecified
     * creature. The texts are consecutive in Speech.str, except the first.
     */
    private static final PlayerMessageSubTable SACRIFICE_RESULT = createSacrificeResult();

    private static final PlayerMessageSubTable ALLIANCE = PlayerMessageSubTable.of(0, 30,
            1, 1347, 2, 454, 3, 455, 4, 456, 5, 457, 6, 458, 7, 459);

    private static final PlayerMessageSubTable PLAYER_DEFEATED_MULTIPLAYER = PlayerMessageSubTable.of(0, 20,
            1, 1516, 2, 1517, 3, 1518, 4, 1519);

    private static final PlayerMessageSubTable PRAISE = PlayerMessageSubTable.of(0, 20,
            1, 508, 2, 509, 3, 510, 4, 511, 5, 512, 6, 513, 7, 514, 8, 515);

    private static final PlayerMessageSubTable JOKE = PlayerMessageSubTable.of(0, 20,
            1, 1167, 2, 1168, 3, 1171, 4, 1173, 5, 1174, 6, 1175, 7, 1176, 8, 1177, 9, 1178, 10, 1179, 11, 1180,
            12, 1181, 13, 1182, 14, 1183, 15, 1184, 16, 1186, 17, 1187, 18, 1188, 19, 1189, 20, 1190, 21, 1191,
            22, 1192, 23, 1193, 24, 1194, 25, 1195, 26, 1196, 27, 1198);

    /**
     * Same keys and timeouts as NEED_ROOM, but one text for all
     */
    private static final PlayerMessageSubTable IRON_MAIDENS_NEED_TORTURE_CHAMBER = PlayerMessageSubTable.of(0, 300,
            1, 1512, 2, 1512, 4, 1512, 14, 1512, 11, 1512, 12, 1512, 6, 1512, 10, 1512, 15, 1512, 7, 1512, 16, 1512,
            13, 1512, 9, 1512)
            .with(1, 1512, 420).with(2, 1512, 180).with(4, 1512, 180).with(14, 1512, 600).with(11, 1512, 600);

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
            case LORD_OF_THE_LAND_EVENT -> LORD_OF_THE_LAND_EVENT;
            case KING_EVENT -> KING_EVENT;
            case WORKSHOP_ITEM_BUILT -> WORKSHOP_ITEM_BUILT;
            case WORKERS_DISTURBED -> WORKERS_DISTURBED;
            case LORD_OF_THE_LAND_DEFEATED -> LORD_OF_THE_LAND_DEFEATED;
            case ROOM_UNDER_ATTACK -> ROOM_UNDER_ATTACK;
            case ROOM_DESTROYED -> ROOM_DESTROYED;
            case ONE_OF_ROOMS_DESTROYED -> ONE_OF_ROOMS_DESTROYED;
            case NEED_ROOM -> NEED_ROOM;
            case ROOM_TOO_SMALL -> ROOM_TOO_SMALL;
            case ROOM_UNREACHABLE -> ROOM_UNREACHABLE;
            case ROOM_NOT_USABLE -> ROOM_NOT_USABLE;
            case ROOM_FULL -> ROOM_FULL;
            case MANA_LOW -> MANA_LOW;
            case NOT_ENOUGH_MANA -> NOT_ENOUGH_MANA;
            case GOLD_LOW -> GOLD_LOW;
            case SACRIFICE_RESULT -> SACRIFICE_RESULT;
            case ALLIANCE -> ALLIANCE;
            case PLAYER_DEFEATED_MULTIPLAYER -> PLAYER_DEFEATED_MULTIPLAYER;
            case PRAISE -> PRAISE;
            case JOKE -> JOKE;
            case IRON_MAIDENS_NEED_TORTURE_CHAMBER -> IRON_MAIDENS_NEED_TORTURE_CHAMBER;
            default -> null;
        };
    }

    private static PlayerMessageSubTable createSacrificeResult() {
        PlayerMessageSubTable table = PlayerMessageSubTable.of(0, 5, 1, 1343);
        for (int key = 2; key <= 46; key++) {
            table.with(key, 397 + key, 5);
        }
        return table;
    }
}
