/*
 * Copyright (C) 2014-2015 OpenKeeper
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.tools.convert.map.AI.AIType;
import toniarts.openkeeper.tools.convert.map.Player;

/**
 * Your FRIENDLY neighbourhood Keeper, or not
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Keeper implements Comparable<Keeper>, IIndexable {

    private boolean ai;
    private AIType aiType = AIType.MASTER_KEEPER;

    private int gold;
    private int goldMined;
    private int mana;
    private int manaGain;
    private int manaLoose;
    private int maxMana;
    private List<Short> availableRooms = new ArrayList<>();
    private List<Short> availableSpells = new ArrayList<>();
    private List<Short> availableCreatures = new ArrayList<>();

    private transient Player player;
    private short id;
    private boolean destroyed = false;
    private final Set<Short> allies = new HashSet<>(4);

    public Keeper() {

    }

    public Keeper(boolean ai, short id) {
        this.ai = ai;
        this.id = id;
    }

    public Keeper(Player player) {
        this.player = player;
        this.id = player.getPlayerId();
    }

    @Override
    public short getId() {
        return id;
    }

    public void setId(short id) {
        this.id = id;
    }

    public Player getPlayer() {
        return player;
    }

    public int getGoldMined() {
        return goldMined;
    }

    public void setGoldMined(int goldMined) {
        this.goldMined = goldMined;
    }

    public void setManaGain(int manaGain) {
        this.manaGain = manaGain;
    }

    public int getManaGain() {
        return manaGain;
    }

    public void setManaLoose(int manaLoose) {
        this.manaLoose = manaLoose;
    }

    public int getManaLoose() {
        return manaLoose;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    @Override
    public String toString() {
        return Short.toString(id);
    }

    /**
     * Check whether the player is destroyed or not
     *
     * @return is destroyed
     */
    public boolean isDestroyed() {
        return destroyed;
    }

    public AIType getAiType() {
        return aiType;
    }

    public void setAiType(AIType aiType) {
        this.aiType = aiType;
    }

    public boolean isAi() {
        return ai;
    }

    /**
     * Is the given player an enemy of ours
     *
     * @param playerId the other player
     * @return is the player enemy of ours
     * @see #isAlly(short)
     */
    public boolean isEnemy(short playerId) {
        if (playerId == id || playerId == Player.NEUTRAL_PLAYER_ID || id == Player.NEUTRAL_PLAYER_ID) {
            return false; // Neutral player has no enemies
        }
        return !allies.contains(playerId);
    }

    /**
     * Is the player an ally of us. If not, it is not necessarily the enemy. But
     * we do not share vision, battless etc. with non-allies
     *
     * @param playerId the other player
     * @return is the player an ally of ours
     * @see #isEnemy(short)
     */
    public boolean isAlly(short playerId) {
        return id == playerId || allies.contains(playerId);
    }

    /**
     * Create alliance between two players, remember to set both players as
     * allies
     *
     * @param playerId the player to form alliance with
     */
    public void createAlliance(short playerId) {
        allies.add(playerId);
    }

    /**
     * Breaks alliance between two players, remember to break the alliance on
     * both players
     *
     * @param playerId the player to break alliance with
     */
    public void breakAlliance(short playerId) {
        allies.remove(playerId);
    }

    public int getGold() {
        return gold;
    }

    public int getMana() {
        return mana;
    }

    public void setGold(int gold) {
        this.gold = gold;
    }

    public void setMana(int mana) {
        this.mana = mana;
    }

    public int getMaxMana() {
        return maxMana;
    }

    public void setMaxMana(int maxMana) {
        this.maxMana = maxMana;
    }

    public List<Short> getAvailableRooms() {
        return availableRooms;
    }

    public List<Short> getAvailableSpells() {
        return availableSpells;
    }

    public List<Short> getAvailableCreatures() {
        return availableCreatures;
    }

    @Override
    public int compareTo(Keeper keeper) {
        return Short.compare(id, keeper.id);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Keeper other = (Keeper) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

}
