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

import com.jme3.math.Vector2f;
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

    /**
     * Spawns creatures of any owner on a tile, god mode only
     *
     * @param creatureId the creature to spawn
     * @param amount how many creatures to spawn
     * @param ownerId the owner of the creatures
     * @param position the tile to spawn on
     * @param playerId the player who cheats
     */
    void spawnCreature(short creatureId, int amount, short ownerId, Vector2f position, short playerId);

    /**
     * Adds gold to the given player
     *
     * @param amount how much gold to add
     * @param playerId the player who gets the gold
     */
    void addGold(int amount, short playerId);

    /**
     * Adds mana to the given player
     *
     * @param amount how much mana to add
     * @param playerId the player who gets the mana
     */
    void addMana(int amount, short playerId);

    /**
     * Tells whether a cheat mode, such as {@link CheatState.CheatType#INSTANT_DIG}, is on for the player
     *
     * @param cheat the cheat
     * @param playerId the player
     * @return {@code true} if the cheat is on
     */
    boolean isCheat(CheatState.CheatType cheat, short playerId);

    /**
     * God mode: dig out the solid tiles of an area right away
     *
     * @param start start coordinates
     * @param end end coordinates
     * @param playerId the player in god mode
     */
    void dig(Vector2f start, Vector2f end, short playerId);

    /**
     * God mode: change the terrain of an area, or claim it
     *
     * @param start start coordinates
     * @param end end coordinates
     * @param terrainId the new terrain, or {@link #CLAIM_TERRAIN} to claim the area, rooms included
     * @param ownerId the owner of the new terrain, if it can be owned, the neutral player for nobody
     * @param playerId the player in god mode
     */
    void changeTerrain(Vector2f start, Vector2f end, short terrainId, short ownerId, short playerId);

    /**
     * Pseudo terrain id for {@link #changeTerrain}: claim instead of changing the terrain
     */
    short CLAIM_TERRAIN = -1;

}
