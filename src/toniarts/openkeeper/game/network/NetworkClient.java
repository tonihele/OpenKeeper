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

import toniarts.openkeeper.game.network.message.MessageChat;
import toniarts.openkeeper.game.network.message.MessageTime;
import com.jme3.network.Client;
import com.jme3.network.ClientStateListener;
import com.jme3.network.Network;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.client.RemoteEntityData;
import com.simsilica.es.net.ObjectMessageDelegator;
import de.lessvoid.nifty.controls.Chat;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.network.message.MessagePlayerInfo;
import toniarts.openkeeper.game.network.message.MessageServerInfo;

/**
 *
 * @author ArchDemon
 */
public class NetworkClient {

    public enum Role {
        MASTER,
        SLAVE
    }

    private Client client;
    private final String player;
    private final Role role;

    private RemoteEntityData ed;
    private EntityId entity;
    //TODO change to event listener
    private Chat chat;

    private long frameDelay = 200 * 1000000L; // 200 ms
    private long renderTime;
    private long serverTimeOffset;
    private long pingDelay = 500 * 1000000L; // 500 ms
    private long nextPing;

    private long lastTime;
    private static final Logger logger = Logger.getLogger(NetworkClient.class.getName());

    static {
        ClassSerializer.initialize();
    }

    public NetworkClient(String player, Role role) {
        this.player = player;
        this.role = role;
    }

    public NetworkClient(String player) {
        this(player, Role.MASTER);
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

    public void start(String ip, int port) throws IOException {
        if (client == null) {
            client = Network.connectToServer(ip, port);
            /*
            this.client = Network.connectToServer(NetworkServer.GAME_NAME,
                                                  NetworkServer.PROTOCOL_VERSION,
                                                  host, port, port);
            */
        }
        this.ed = new RemoteEntityData(client, 0);

        ObjectMessageDelegator delegator = new ObjectMessageDelegator(this, true);
        client.addMessageListener(delegator, delegator.getMessageTypes());

        client.addClientStateListener(new ClientStateChangeListener(this));
        logger.info("Network: Player starting");
        client.start();

        client.send(new MessagePlayerInfo(player).setReliable(true));
    }

    public void close() {
        logger.info("Network: closing client connection");
        if (ed != null) {
            ed.close();
        }

        if (client != null && client.isConnected()) {
            client.close();
        }
    }

    public String getPlayer() {
        return player;
    }

    public Client getClient() {
        return client;
    }

    public Role getRole() {
        return role;
    }

    public EntityData getEntityData() {
        return ed;
    }

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    protected void onMessageChat(MessageChat message){
        if (chat != null) {
            // FIXME bug with num lines. If more than max => crush
            try {
                chat.receivedChatLine(message.getData(), null, "chat");
            } catch (Exception ex) {
            }
        }
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
        if( Math.abs(predictedOffset - serverTimeOffset) > 15000000 ) {
            //System.out.println( "Adjusting time offset." );
            // If it's more than 15 ms then we will adjust
            serverTimeOffset = predictedOffset;
        }
    }

    protected void onMessagePlayerInfo(MessagePlayerInfo msg) {
        logger.log(Level.INFO, "Network: player info {0}", msg);
        entity = msg.getEntityId();
    }

    protected void onMessageServerInfo(MessageServerInfo msg) {
        logger.log(Level.INFO, "Network: server info {0}", msg);
    }

    protected void onConnected() {
        logger.info("Network: Player connected");
    }

    protected void onDisconnected(ClientStateListener.DisconnectInfo di) {
        logger.log(Level.INFO, "Network: player disconnected {0}", di);
    }
}
