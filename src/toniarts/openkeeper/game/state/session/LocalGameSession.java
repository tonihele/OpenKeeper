/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.game.state.session;

import com.jme3.math.Vector2f;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.tools.convert.map.Player;

/**
 * Local game session, a virtual server
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class LocalGameSession implements GameSessionServerService, GameSessionClientService {

    private final List<GameSessionListener> listeners = new ArrayList<>();
    private final List<GameSessionServiceListener> serverListeners = new ArrayList<>();

    public LocalGameSession() {

    }

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

        // Only one player, start the game once everything ready
        startGame();
    }

    @Override
    public void loadStatus(float progress) {
        for (GameSessionListener listener : listeners) {
            listener.onLoadStatusUpdate(progress, Player.KEEPER1_ID);
        }
    }

    @Override
    public void selectTiles(Vector2f start, Vector2f end, boolean select) {
        for (GameSessionServiceListener listener : serverListeners) {
            listener.onSelectTiles(start, end, select, Player.KEEPER1_ID);
        }
    }

    @Override
    public void markReady() {
        // We don't care really, locally if the client is started before the server, everything is fine
    }

    @Override
    public void updateTiles(List<MapTile> updatedTiles) {
        for (GameSessionListener listener : listeners) {
            listener.onTilesChange(updatedTiles);
        }
    }

    @Override
    public void addGameSessionServiceListener(GameSessionServiceListener l) {
        serverListeners.add(l);
    }

    @Override
    public void removeGameSessionServiceListener(GameSessionServiceListener l) {
        serverListeners.remove(l);
    }

}
