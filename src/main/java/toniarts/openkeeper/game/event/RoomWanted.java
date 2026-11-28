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
package toniarts.openkeeper.game.event;

import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;

/**
 * A creature of a keeper looked for a room to do something in and the
 * keeper has no room of that kind, like DK II's "room wanted" message (0x29),
 * or its rooms of the kind are full, like DK II's "room too small" (0x2A)
 *
 * @param playerId the keeper
 * @param objectType what the creature wanted the room for
 */
public record RoomWanted(short playerId, ObjectType objectType) {
}
