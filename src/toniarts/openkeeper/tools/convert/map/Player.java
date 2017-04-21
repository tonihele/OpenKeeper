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
package toniarts.openkeeper.tools.convert.map;

import toniarts.openkeeper.game.data.ITriggerable;

/**
 * Container class for the mapnamePlayer.kld
 *
 * @author Wizand Petteri Loisko petteri.loisko@gmail.com, Toni Helenius
 * <helenius.toni@gmail.com>
 *
 * Thank you https://github.com/werkt
 */
public class Player implements Comparable<Player>, ITriggerable {

    public static final short GOOD_PLAYER_ID = 1;
    public static final short NEUTRAL_PLAYER_ID = 2;

    //
    // struct Player
    // int x00;
    // int x04;
    // uint8_t x08[158];
    // uint16_t xa6;
    // uint8_t xa8;
    // uint16_t xa9;
    // uint16_t xab;
    // char name[32]; // ad
    private boolean ai; // x04, I suspect that this is AI = 1, Human player = 0

    private AI aiAttributes;
    //
    private int startingGold; // x00
    private int triggerId; // xa6, associated trigger
    private short playerId; // 0xa8
    private int startingCameraX; // xa9, 0 based coordinate
    private int startingCameraY; // xab, 0 based coordinate
    private String name; // ad

    public int getStartingGold() {
        return startingGold;
    }

    protected void setStartingGold(int startingGold) {
        this.startingGold = startingGold;
    }

    public boolean isAi() {
        return ai;
    }

    protected void setAi(boolean ai) {
        this.ai = ai;
    }

    @Override
    public int getTriggerId() {
        return triggerId;
    }

    protected void setTriggerId(int triggerId) {
        this.triggerId = triggerId;
    }

    public short getPlayerId() {
        return playerId;
    }

    protected void setPlayerId(short idPlayer) {
        this.playerId = idPlayer;
    }

    public int getStartingCameraX() {
        return startingCameraX;
    }

    protected void setStartingCameraX(int startingCameraX) {
        this.startingCameraX = startingCameraX;
    }

    public int getStartingCameraY() {
        return startingCameraY;
    }

    protected void setStartingCameraY(int startingCameraY) {
        this.startingCameraY = startingCameraY;
    }

    protected void setAiAttributes(AI attributes) {
        this.aiAttributes = attributes;
    }

    public AI getAiAttributes() {
        return aiAttributes;
    }

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(Player o) {
        return Short.compare(playerId, o.playerId);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + this.playerId;
        return hash;
    }

    @Override
    public boolean equals(java.lang.Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Player other = (Player) obj;
        if (this.playerId != other.playerId) {
            return false;
        }
        return true;
    }
}
