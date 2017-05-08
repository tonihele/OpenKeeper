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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.AI;
import toniarts.openkeeper.utils.Utils;

/**
 * A local lobby
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class LocalLobby implements LobbyService, LobbyClientService {

    private final Map<Integer, ClientInfo> players = new HashMap<>(4);
    private final List<LobbySessionListener> listeners = new ArrayList<>();
    private String map;
    private int maxPlayers = 0;
    private int idCounter = 0;
    private boolean ready = false;

    public LocalLobby() {
        Keeper keeper = new Keeper(false, Keeper.KEEPER1_ID, null);
        ClientInfo clientInfo = createClientInfo(keeper, Utils.getMainTextResourceBundle().getString("58"));
        players.put(clientInfo.getId(), clientInfo);
        keeper = new Keeper(true, Keeper.KEEPER2_ID, null);
        clientInfo = createClientInfo(keeper, null);
        players.put(clientInfo.getId(), clientInfo);
    }

    private ClientInfo createClientInfo(Keeper keeper, String name) {
        ClientInfo clientInfo = new ClientInfo(0, null, idCounter);
        clientInfo.setName(name);
        clientInfo.setKeeper(keeper);
        clientInfo.setReady(keeper.isAi());
        idCounter++;
        return clientInfo;
    }

    @Override
    public void setMap(String mapName, int maxPlayers) {
        this.map = mapName;
        this.maxPlayers = maxPlayers;
        for (ClientInfo clientInfo : LobbyUtils.compactPlayers(new HashSet<>(players.values()), maxPlayers)) {
            players.remove(clientInfo.getId());
        }
        for (LobbySessionListener l : listeners) {
            l.onMapChanged(mapName);
        }
        notifyPlayerListChange();
    }

    @Override
    public void addPlayer() {
        if (players.size() < maxPlayers) {
            ClientInfo clientInfo = createClientInfo(LobbyUtils.getNextKeeper(true, new HashSet<>(players.values())), Utils.getMainTextResourceBundle().getString("58"));
            players.put(clientInfo.getId(), clientInfo);
            notifyPlayerListChange();
        }
    }

    @Override
    public void removePlayer(ClientInfo keeper) {
        players.remove(keeper.getId());
        notifyPlayerListChange();
    }

    private void notifyPlayerListChange() {
        for (LobbySessionListener l : listeners) {
            l.onPlayerListChanged(getPlayers());
        }
    }

    @Override
    public void setReady(boolean ready) {
        this.ready = ready;
        players.get(getPlayerId()).setReady(ready);
        notifyPlayerListChange();
    }

    @Override
    public List<ClientInfo> getPlayers() {
        List<ClientInfo> keepers = new ArrayList<>(players.values());
        Collections.sort(keepers, (ClientInfo o1, ClientInfo o2) -> Short.compare(o1.getKeeper().getId(), o2.getKeeper().getId()));
        return keepers;
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

    @Override
    public void changeAIType(ClientInfo keeper, AI.AIType type) {
        players.get(keeper.getId()).getKeeper().setAiType(type);
        notifyPlayerListChange();
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public int getPlayerId() {
        return 0;
    }
}
