/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.game.state.session;

import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.tools.convert.map.Player;

/**
 * Local game session, a virtual server
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class LocalGameSession implements GameSessionService, GameSessionClientService {

    private final List<GameSessionListener> listeners = new ArrayList<>();

    @Override
    public void sendGameData(MapData mapData) {
        for (GameSessionListener listener : listeners) {
            listener.onGameDataLoaded(mapData);
        }
    }

    @Override
    public void startGame() {
        for (GameSessionListener listener : listeners) {
            listener.onGameStarted();
        }
    }

    @Override
    public void addGameSessionListener(GameSessionListener l) {
        listeners.add(l);
    }

    @Override
    public void removeGameSessionListener(GameSessionListener l) {
        listeners.remove(l);
    }

    @Override
    public void loadComplete() {
        for (GameSessionListener listener : listeners) {
            listener.onLoadComplete(Player.KEEPER1_ID);
        }
    }

    @Override
    public void loadStatus(float progress) {
        for (GameSessionListener listener : listeners) {
            listener.onLoadStatusUpdate(progress, Player.KEEPER1_ID);
        }
    }

}
