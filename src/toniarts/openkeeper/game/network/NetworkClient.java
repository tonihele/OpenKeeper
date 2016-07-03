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
import com.jme3.network.Network;
import de.lessvoid.nifty.controls.Chat;
import java.io.IOException;

/**
 *
 * @author ArchDemon
 */
public class NetworkClient {
    
    public enum Role {
        MASTER,
        SLAVE
    }
    
    private final String name;    
    private final Role role;
    
    private Chat chat;
    private Client client;    
    
    public NetworkClient(String name, Role role) {
        this.name = name;
        this.role = role;
    }
    
    public NetworkClient(String name) {
        this(name, Role.MASTER);
    }
    
    public void start(String ip, int port) throws IOException {
        if (client == null) {
            client = Network.connectToServer(ip, port);
        }

        client.addMessageListener(new ClientListener(this), MessageChat.class);
        client.addClientStateListener(new ClientStateChangeListener());
        client.start();
    }
        
    public void close() {
        if (client != null && (client.isStarted() || client.isConnected())) {
            client.close();
        }
    }
    
    public String getName() {
        return name;
    }

    public Client getClient() {
        return client;
    }

    public Role getRole() {
        return role;
    }

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }
    
    protected void onChatMessageRecive(String message){
        if (chat != null) {
            chat.receivedChatLine(message, null, "chat");
        }
    }
}
