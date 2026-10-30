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
package toniarts.openkeeper.game.event;

/**
 * Things that happen in the game, as the game logic tells them. The logic only says what happened, what a client
 * makes of it (a sound, an effect...) is the client's business.
 */
public enum GameEventType {

    // Creatures, the subject is the creature type
    CREATURE_LEVEL_UP,
    CREATURE_LAND,
    /**
     * The creature took damage. Not restarted while the previous one of the same creature still goes on.
     */
    CREATURE_HURT,
    CREATURE_DIE,
    CREATURE_CLAIM_TILE,
    CREATURE_REINFORCE_WALL,
    /**
     * The subject is the attacker's creature type, the target is the victim
     */
    MELEE_HIT,

    // Hand, the subject is the creature or object type
    PICKED_UP,
    DROPPED,
    SLAPPED,
    /**
     * A hand of a player slapped something, only that player gets this
     */
    HAND_SLAP,
    /**
     * A player tagged tiles, only that player gets this
     */
    TILES_TAGGED,

    // Shots and spells
    /**
     * The subject is a shot type or a keeper spell
     */
    SHOT_LAUNCH,
    /**
     * The subject is the shot type, the target is the victim
     */
    SHOT_HIT,
    IMPS_CREATED,

    // Rooms and terrain, the subject is the room or the terrain
    ROOM_TILES_LAID,
    ROOM_TILE_BUILT,
    ROOM_SOLD,
    TERRAIN_DESTROYED
}
