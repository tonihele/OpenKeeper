/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.game.state.lobby;

import toniarts.openkeeper.game.data.Keeper;

/**
 * Small container to hold info about the connected client
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ClientInfo {

    private int systemMemory;
    private long ping;
    private String address;
    private Keeper keeper;
    private String name;
    private boolean ready = false;

    public ClientInfo() {

    }

    public ClientInfo(int systemMemory, String address) {
        this.systemMemory = systemMemory;
        this.address = address;
    }

    public long getPing() {
        return ping;
    }

    public void setPing(long ping) {
        this.ping = ping;
    }

    public String getAddress() {
        return address;
    }

    public Keeper getKeeper() {
        return keeper;
    }

    public void setKeeper(Keeper keeper) {
        this.keeper = keeper;
    }

    public int getSystemMemory() {
        return systemMemory;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        if (keeper.isAi()) {
            return keeper.getAiType().toString();
        }
        return name;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }
}
