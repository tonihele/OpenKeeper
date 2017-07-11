/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.game.state.session;

import com.jme3.math.Vector2f;
import com.jme3.util.SafeArrayList;
import com.simsilica.es.EntityData;
import com.simsilica.es.base.DefaultEntityData;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.game.controller.player.PlayerSpell;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.tools.convert.map.Player;

/**
 * Local game session, a virtual server
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class LocalGameSession implements GameSessionServerService, GameSessionClientService {

    private final EntityData entityData = new DefaultEntityData();
    private final List<GameSessionListener> listeners = new SafeArrayList<>(GameSessionListener.class);
    private final List<GameSessionServiceListener> serverListeners = new ArrayList<>();

    public LocalGameSession() {

    }

    @Override
    public EntityData getEntityData() {
        return entityData;
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

    @Override
    public void onAdded(PlayerSpell spell) {
        for (GameSessionListener listener : listeners) {
            listener.onAdded(spell);
        }
    }

    @Override
    public void onRemoved(PlayerSpell spell) {
        for (GameSessionListener listener : listeners) {
            listener.onRemoved(spell);
        }
    }

    @Override
    public void onResearchStatusChanged(PlayerSpell spell) {
        for (GameSessionListener listener : listeners) {
            listener.onResearchStatusChanged(spell);
        }
    }

    @Override
    public void onGoldChange(short keeperId, int gold) {
        for (GameSessionListener listener : listeners) {
            listener.onGoldChange(keeperId, gold);
        }
    }

    @Override
    public void onManaChange(short keeperId, int mana, int manaLoose, int manaGain) {
        for (GameSessionListener listener : listeners) {
            listener.onManaChange(keeperId, mana, manaLoose, manaGain);
        }
    }

}
