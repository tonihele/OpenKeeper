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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.utils.Utils;

/**
 * A local lobby
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class LocalLobby implements LobbyService, LobbyClientService {

    private final Set<ClientInfo> players = new HashSet<>(4);
    private final List<LobbySessionListener> listeners = new ArrayList<>();
    private String map;

    public LocalLobby() {
        Keeper keeper = new Keeper(false, Keeper.KEEPER1_ID, null);
        players.add(createClientInfo(keeper, Utils.getMainTextResourceBundle().getString("58")));
        keeper = new Keeper(true, Keeper.KEEPER2_ID, null);
        players.add(createClientInfo(keeper, null));
    }

    private ClientInfo createClientInfo(Keeper keeper, String name) {
        ClientInfo clientInfo = new ClientInfo(0, null);
        clientInfo.setName(name);
        clientInfo.setKeeper(keeper);
        clientInfo.setReady(keeper.isAi());
        return clientInfo;
    }

    @Override
    public void setMap(String mapName) {
        this.map = mapName;
        for (LobbySessionListener l : listeners) {
            l.onMapChanged(mapName);
        }
    }

    @Override
    public void addPlayer() {
        //players.add(e);
        for (LobbySessionListener l : listeners) {
            l.onPlayerListChanged(getPlayers());
        }
    }

    @Override
    public void removePlayer(ClientInfo keeper) {
        players.remove(keeper);
        for (LobbySessionListener l : listeners) {
            l.onPlayerListChanged(getPlayers());
        }
    }

    @Override
    public void setReady(boolean ready) {

        // We should start the game
    }

    @Override
    public List<ClientInfo> getPlayers() {
        return new ArrayList<>(players);
    }

    @Override
    public String getMap() {
        return map;
    }

    @Override
    public void addLobbySessionListener(LobbySessionListener l) {
        listeners.add(l);
    }

    @Override
    public void removeLobbySessionListener(LobbySessionListener l) {
        listeners.remove(l);
    }

}
