/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.game.state.session;

import com.jme3.math.Vector2f;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.game.controller.MapController;
import toniarts.openkeeper.game.controller.MapListener;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;

/**
 * Local game session, a virtual server
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class LocalGameSession implements GameSessionService, GameSessionClientService {

    private final List<GameSessionListener> listeners = new ArrayList<>();
    private final KwdFile kwdFile;
    private MapController mapController;
    private final MapListener mapListener = new MapListenerImpl();

    public LocalGameSession(KwdFile kwdFile) {
        this.kwdFile = kwdFile;
    }

    @Override
    public void sendGameData(MapData mapData) {
        kwdFile.load();
        mapController = new MapController(mapData, kwdFile);
        for (GameSessionListener listener : listeners) {
            listener.onGameDataLoaded(mapData);
        }

        // Add listeners for tiles
        mapController.addListener(mapListener);
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
    public MapData getMapData() {
        return mapController.getMapData();
    }

    @Override
    public void setTiles(List<MapTile> tiles) {
        mapController.setTiles(tiles);
    }

    @Override
    public boolean isBuildable(int x, int y, Player player, Room room) {
        return mapController.isBuildable(x, y, player, room);
    }

    @Override
    public boolean isClaimable(int x, int y, short playerId) {
        return mapController.isClaimable(x, y, playerId);
    }

    @Override
    public boolean isSelected(int x, int y, short playerId) {
        return mapController.isSelected(x, y, playerId);
    }

    @Override
    public boolean isTaggable(int x, int y) {
        return mapController.isTaggable(x, y);
    }

    @Override
    public void selectTiles(Vector2f start, Vector2f end, boolean select, short playerId) {
        mapController.selectTiles(start, end, select, playerId);
    }

    @Override
    public void markReady() {
        // We don't care really, locally if the client is started before the server, everything is fine
    }

    private class MapListenerImpl implements MapListener {

        @Override
        public void onTilesChange(List<MapTile> updatedTiles) {
            for (GameSessionListener listener : listeners) {
                listener.onTilesChange(updatedTiles);
            }
        }
    }

}
