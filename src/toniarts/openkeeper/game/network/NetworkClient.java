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
package toniarts.openkeeper.game.network;

import com.jme3.network.Client;
import com.jme3.network.ClientStateListener;
import com.jme3.network.Network;
import com.jme3.network.service.ClientService;
import com.jme3.network.service.rmi.RmiClientService;
import com.jme3.network.service.rpc.RpcClientService;
import com.simsilica.es.client.EntityDataClientService;
import com.simsilica.ethereal.EtherealClient;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.network.chat.ChatClientService;
import toniarts.openkeeper.game.network.chat.ChatSession;
import toniarts.openkeeper.game.network.chat.ChatSessionListener;
import toniarts.openkeeper.game.network.game.GameClientService;
import toniarts.openkeeper.game.network.lobby.LobbyClientService;
import toniarts.openkeeper.game.network.message.MessagePlayerInfo;
import toniarts.openkeeper.game.network.message.MessageServerInfo;
import toniarts.openkeeper.game.network.message.MessageTime;
import toniarts.openkeeper.game.network.session.AccountClientService;
import toniarts.openkeeper.game.network.streaming.StreamingClientService;

/**
 *
 * @author ArchDemon
 */
public class NetworkClient implements ChatSession {
    
    private static final Logger LOGGER = Logger.getLogger(NetworkClient.class.getName());

    private final Client client;

    private final long frameDelay = 200 * 1000000L; // 200 ms
    private long renderTime;
    private long serverTimeOffset;
    private final long pingDelay = 500 * 1000000L; // 500 ms
    private long nextPing;

    public NetworkClient(String host, int port) throws IOException {
        client = Network.connectToServer(NetworkConstants.GAME_NAME, NetworkConstants.PROTOCOL_VERSION, host, port);

        client.addClientStateListener(new ClientStateChangeListener(this));

        client.getServices().addServices(new RpcClientService(),
                new RmiClientService(),
                new StreamingClientService(),
                new AccountClientService(),
                new LobbyClientService(),
                new ChatClientService(), new EtherealClient(NetworkConstants.OBJECT_PROTOCOL,
                        NetworkConstants.ZONE_GRID,
                        NetworkConstants.ZONE_RADIUS),
                new EntityDataClientService(NetworkConstants.ES_CHANNEL),
                new GameClientService()
        );
    }

    public final long getGameTime() {
        return System.nanoTime() - serverTimeOffset;
    }

    public final long getRenderTime() {
        return renderTime;
    }

    public void updateRenderTime() {
        renderTime = getGameTime() - frameDelay;
        if (renderTime > nextPing) {
            nextPing = renderTime + pingDelay;
            sendPing();
        }
    }

    /**
     * Send our latest time and let the server ping us back
     */
    protected void sendPing() {
        client.send(new MessageTime(getGameTime()).setReliable(true));
    }

    public void start() throws IOException {
        LOGGER.info("Network: Player starting");
        client.start();
    }

    public <T extends ClientService> T getService(Class<T> type) {
        return client.getServices().getService(type);
    }

    public void close() {
        LOGGER.info("Network: closing client connection");

        if (client != null && client.isConnected()) {
            client.close();
        }
    }

    public void addChatSessionListener(ChatSessionListener listener) {
        client.getServices().getService(ChatClientService.class).addChatSessionListener(listener);
    }

    public void removeChatSessionListener(ChatSessionListener listener) {
        client.getServices().getService(ChatClientService.class).removeChatSessionListener(listener);
    }

    @Override
    public void sendMessage(String message) {
        client.getServices().getService(ChatClientService.class).sendMessage(message);
    }

    @Override
    public List<String> getPlayerNames() {
        return client.getServices().getService(ChatClientService.class).getPlayerNames();
    }

    public Client getClient() {
        return client;
    }

    protected void onMessageTime(MessageTime msg) {
        //TODO remove all System.out.println()
        //System.out.println( "onMessageTime:" + msg );
        if (msg.getTime() != msg.getSentTime()) {
            long gt = getGameTime();
            //System.out.println( "game time:" + gt );
            long ping = Math.round((gt - msg.getSentTime()) / 1000000.0);
            System.out.println(String.format("PING: %s ms"));
        }
        long time = msg.getTime();
        long now = System.nanoTime();
        long predictedOffset = now - time;
        //System.out.println( "predicted offset:" + predictedOffset );
        if (Math.abs(predictedOffset - serverTimeOffset) > 15000000) {
            //System.out.println( "Adjusting time offset." );
            // If it's more than 15 ms then we will adjust
            serverTimeOffset = predictedOffset;
        }
    }

    protected void onMessagePlayerInfo(MessagePlayerInfo msg) {
        LOGGER.log(Level.INFO, "Network: player info {0}", msg);
        //entity = msg.getEntityId();
    }

    protected void onMessageServerInfo(MessageServerInfo msg) {
        LOGGER.log(Level.INFO, "Network: server info {0}", msg);
    }

    protected void onConnected() {
        LOGGER.info("Network: Player connected");
    }

    protected void onDisconnected(ClientStateListener.DisconnectInfo di) {
        LOGGER.log(Level.INFO, "Network: player disconnected {0}", di);
    }

}
