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
package toniarts.openkeeper.game.controller;

import toniarts.openkeeper.game.state.CheatState;

/**
 * Executes cheats. Implementations are free to refuse, e.g. in multiplayer games.
 */
public interface ICheatController {

    /**
     * Executes a cheat
     *
     * @param cheat the cheat to execute
     * @param playerId the player who triggered the cheat
     */
    void onCheat(CheatState.CheatType cheat, short playerId);

    /**
     * Spawns creatures at the given player's dungeon heart entrance
     *
     * @param creatureId the creature to spawn
     * @param level the creature level
     * @param amount how many creatures to spawn
     * @param playerId the owner, and whose dungeon heart entrance to spawn at
     */
    void spawnCreature(short creatureId, int level, int amount, short playerId);

}
