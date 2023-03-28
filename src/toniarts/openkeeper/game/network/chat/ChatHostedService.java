/*
 * $Id$
 *
 * Copyright (c) 2016, Simsilica, LLC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package toniarts.openkeeper.game.network.chat;

import com.jme3.network.HostedConnection;
import com.jme3.network.service.AbstractHostedConnectionService;
import com.jme3.network.service.HostedServiceManager;
import com.jme3.network.service.rmi.RmiHostedService;
import com.jme3.network.service.rmi.RmiRegistry;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.network.NetworkConstants;
import static toniarts.openkeeper.game.network.lobby.LobbyHostedService.ATTRIBUTE_KEEPER_ID;

/**
 * HostedService providing a chat server for connected players. Some time during
 * player connection setup, the game must start hosting and provide the player
 * name in order for the client to participate.
 *
 * @author Paul Speed
 */
public class ChatHostedService extends AbstractHostedConnectionService {

    private static final Logger logger = Logger.getLogger(ChatHostedService.class.getName());

    private static final String ATTRIBUTE_SESSION = "chat.session";

    private RmiHostedService rmiService;

    private final List<ChatSessionImpl> players = new CopyOnWriteArrayList<>();

    /**
     * Creates a new chat service that will use the specified channel for
     * reliable communication.
     */
    public ChatHostedService() {
        super(false);
    }

    private ChatSessionImpl getChatSession(HostedConnection conn) {
        return conn.getAttribute(ATTRIBUTE_SESSION);
    }

    @Override
    protected void onInitialize(HostedServiceManager s) {

        // Grab the RMI service so we can easily use it later
        this.rmiService = getService(RmiHostedService.class);
        if (rmiService == null) {
            throw new RuntimeException(getClass().getName() + " requires an RMI service.");
        }
    }

    /**
     * Starts hosting the chat services on the specified connection using a
     * specified player name. This causes the player to 'enter' the chat room
     * and will then be able to send/receive messages.
     */
    public void startHostingOnConnection(HostedConnection conn, String playerName) {
        logger.log(Level.FINER, "startHostingOnConnection({0})", conn);

        ChatSessionImpl session = new ChatSessionImpl(conn, playerName);
        conn.setAttribute(ATTRIBUTE_SESSION, session);

        // Expose the session as an RMI resource to the client
        RmiRegistry rmi = rmiService.getRmiRegistry(conn);
        rmi.share(NetworkConstants.CHAT_CHANNEL, session, ChatSession.class);

        players.add(session);

        // Send the enter event to other players
        for (ChatSessionImpl chatter : players) {
            if (chatter == session) {
                // Don't send our enter event to ourselves
                continue;
            }
            chatter.playerJoined(conn.getId(), playerName);
        }
    }

    /**
     * Starts hosting the chat services on the specified connection using a
     * generated player name.
     */
    @Override
    public void startHostingOnConnection(HostedConnection conn) {
        startHostingOnConnection(conn, "Client:" + conn.getId());
    }

    @Override
    public void stopHostingOnConnection(HostedConnection conn) {
        logger.log(Level.FINER, "stopHostingOnConnection({0})", conn);
        ChatSessionImpl player = getChatSession(conn);
        if (player != null) {

            // Then we are still hosting on the connection... it's
            // possible that stopHostingOnConnection() is called more than
            // once for a particular connection since some other game code
            // may call it and it will also be called during connection shutdown.
            conn.setAttribute(ATTRIBUTE_SESSION, null);

            // Remove player session from the active sessions list
            players.remove(player);

            // Send the leave event to other players
            for (ChatSessionImpl chatter : players) {
                if (chatter == player) {
                    // Don't send our enter event to ourselves
                    continue;
                }
                chatter.playerLeft(conn.getId(), player.name);
            }
        }
    }

    protected void postMessage(ChatSessionImpl from, String message) {
        logger.log(Level.INFO, "chat> {0} said:{1}", new Object[]{from.name, message});
        for (ChatSessionImpl chatter : players) {
            chatter.newMessage(from.conn.getId(), from.conn.getAttribute(ATTRIBUTE_KEEPER_ID), from.name, message);
        }
    }

    /**
     * The connection-specific 'host' for the ChatSession. For convenience this
     * also implements the ChatSessionListener. Since the methods don't collide
     * at all it's convenient for our other code not to have to worry about the
     * internal delegate.
     */
    private class ChatSessionImpl implements ChatSession, ChatSessionListener {

        private HostedConnection conn;
        private ChatSessionListener callback;
        private String name;

        public ChatSessionImpl(HostedConnection conn, String name) {
            this.conn = conn;
            this.name = name;

            // Note: at this point we won't be able to look up the callback
            // because we haven't received the client's RMI shared objects yet.
        }

        protected ChatSessionListener getCallback() {
            if (callback == null) {
                RmiRegistry rmi = rmiService.getRmiRegistry(conn);
                callback = rmi.getRemoteObject(ChatSessionListener.class);
                if (callback == null) {
                    throw new RuntimeException("Unable to locate client callback for ChatSessionListener");
                }
            }
            return callback;
        }

        @Override
        public void sendMessage(String message) {
            postMessage(this, message);
        }

        @Override
        public List<String> getPlayerNames() {
            List<String> results = new ArrayList<>();
            for (ChatSessionImpl chatter : players) {
                results.add(chatter.name);
            }
            return results;
        }

        @Override
        public void playerJoined(int playerId, String playerName) {
            getCallback().playerJoined(playerId, playerName);
        }

        @Override
        public void newMessage(int playerId, Short keeperId, String playerName, String message) {
            getCallback().newMessage(playerId, keeperId, playerName, message);
        }

        @Override
        public void playerLeft(int playerId, String playerName) {
            getCallback().playerLeft(playerId, playerName);
        }
    }
}
