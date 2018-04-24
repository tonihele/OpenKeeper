/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.game.state.session;

import com.jme3.app.state.AppStateManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.math.Vector2f;
import com.jme3.util.SafeArrayList;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.controller.player.PlayerSpell;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.game.state.GameClientState;
import toniarts.openkeeper.game.state.GameServerState;
import toniarts.openkeeper.game.state.lobby.ClientInfo;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.PathUtils;
import toniarts.openkeeper.utils.Utils;

/**
 * Local game session, a virtual server
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class LocalGameSession implements GameSessionServerService, GameSessionClientService {

    private final EntityData entityData = new DefaultEntityData();
    private final SafeArrayList<GameSessionListener> listeners = new SafeArrayList<>(GameSessionListener.class);
    private final SafeArrayList<GameSessionServiceListener> serverListeners = new SafeArrayList<>(GameSessionServiceListener.class);

    public LocalGameSession() {

    }

    /**
     * Creates and starts a local game session with given level and default
     * players
     *
     * @param kwdFile map as KWD file
     * @param campaign whether to start this level as a campaign level
     * @param stateManager state manager instance for setting up the game
     */
    public static void CreateLocalGame(KwdFile kwdFile, boolean campaign, AppStateManager stateManager) {
        CreateLocalGame(kwdFile, stateManager, campaign);
    }

    /**
     * Creates and starts a local game session with given level and default
     * players
     *
     * @param level the level to load
     * @param campaign whether to start this level as a campaign level
     * @param stateManager state manager instance for setting up the game
     * @throws java.io.IOException Problem with the map file
     */
    public static void CreateLocalGame(String level, boolean campaign, AppStateManager stateManager) throws IOException {

        // Try to load the file
        String mapFile = ConversionUtils.getRealFileName(Main.getDkIIFolder(), PathUtils.DKII_MAPS_FOLDER + level + ".kwd");
        File file = new File(mapFile);
        if (!file.exists()) {
            throw new FileNotFoundException(mapFile);
        }
        KwdFile kwdFile = new KwdFile(Main.getDkIIFolder(), file);

        CreateLocalGame(kwdFile, stateManager, campaign);
    }

    private static void CreateLocalGame(KwdFile kwdFile, AppStateManager stateManager, boolean campaign) {

        // Player and server
        LocalGameSession gameSession = new LocalGameSession();
        Keeper keeper = new Keeper(false, Player.KEEPER1_ID);
        ClientInfo clientInfo = new ClientInfo(0, null, 0);
        clientInfo.setName(Utils.getMainTextResourceBundle().getString("58"));
        clientInfo.setKeeper(keeper);
        clientInfo.setReady(true);

        // The client
        GameClientState gameClientState = new GameClientState(kwdFile, Player.KEEPER1_ID, Arrays.asList(clientInfo), gameSession);
        stateManager.attach(gameClientState);

        // The game server
        GameServerState gameServerState = new GameServerState(kwdFile, campaign ? null : Arrays.asList(keeper), campaign, gameSession);
        stateManager.attach(gameServerState);
    }

    @Override
    public EntityData getEntityData() {
        return entityData;
    }

    @Override
    public void sendGameData(Collection<Keeper> players, MapData mapData) {
        BinaryExporter exporter = BinaryExporter.getInstance();
        BinaryImporter importer = BinaryImporter.getInstance();

        // Clone the map data so it really is different as in normal multiplayer it is
        for (GameSessionListener listener : listeners.getArray()) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                exporter.save(mapData, baos);
                listener.onGameDataLoaded(players, (MapData) importer.load(baos.toByteArray()));
            } catch (IOException ex) {
                Logger.getLogger(LocalGameSession.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void startGame() {
        for (GameSessionListener listener : listeners.getArray()) {
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
        for (GameSessionListener listener : listeners.getArray()) {
            listener.onLoadComplete(Player.KEEPER1_ID);
        }

        // Only one player, start the game once everything ready
        startGame();
    }

    @Override
    public void loadStatus(float progress) {
//        for (GameSessionListener listener : listeners.getArray()) {
//            listener.onLoadStatusUpdate(progress, Player.KEEPER1_ID);
//        }
    }

    @Override
    public void selectTiles(Vector2f start, Vector2f end, boolean select) {
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onSelectTiles(start, end, select, Player.KEEPER1_ID);
        }
    }

    @Override
    public void build(Vector2f start, Vector2f end, short roomId) {
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onBuild(start, end, roomId, Player.KEEPER1_ID);
        }
    }

    @Override
    public void sell(Vector2f start, Vector2f end) {
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onSell(start, end, Player.KEEPER1_ID);
        }
    }

    @Override
    public void markReady() {
        // We don't care really, locally if the client is started before the server, everything is fine
    }

    @Override
    public void interact(EntityId entity) {
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onInteract(entity, Player.KEEPER1_ID);
        }
    }

    @Override
    public void pickUp(EntityId entity) {
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onPickUp(entity, Player.KEEPER1_ID);
        }
    }

    @Override
    public void drop(EntityId entity, Point tile, Vector2f coordinates, EntityId dropOnEntity) {
        for (GameSessionServiceListener listener : serverListeners.getArray()) {
            listener.onDrop(entity, tile, coordinates, dropOnEntity, Player.KEEPER1_ID);
        }
    }

    @Override
    public void updateTiles(List<MapTile> updatedTiles) {
        for (GameSessionListener listener : listeners.getArray()) {
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
        for (GameSessionListener listener : listeners.getArray()) {
            listener.onAdded(spell);
        }
    }

    @Override
    public void onRemoved(PlayerSpell spell) {
        for (GameSessionListener listener : listeners.getArray()) {
            listener.onRemoved(spell);
        }
    }

    @Override
    public void onResearchStatusChanged(PlayerSpell spell) {
        for (GameSessionListener listener : listeners.getArray()) {
            listener.onResearchStatusChanged(spell);
        }
    }

    @Override
    public void onGoldChange(short keeperId, int gold) {
        for (GameSessionListener listener : listeners.getArray()) {
            listener.onGoldChange(keeperId, gold);
        }
    }

    @Override
    public void onManaChange(short keeperId, int mana, int manaLoose, int manaGain) {
        for (GameSessionListener listener : listeners.getArray()) {
            listener.onManaChange(keeperId, mana, manaLoose, manaGain);
        }
    }

    @Override
    public void onBuild(short keeperId, List<MapTile> tiles) {
        for (GameSessionListener listener : listeners.getArray()) {
            listener.onBuild(keeperId, tiles);
        }
    }

    @Override
    public void onSold(short keeperId, List<MapTile> tiles) {
        for (GameSessionListener listener : listeners.getArray()) {
            listener.onSold(keeperId, tiles);
        }
    }

}
