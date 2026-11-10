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

import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import toniarts.openkeeper.game.network.message.MessagePlayerInfo;
import toniarts.openkeeper.game.network.message.MessageTime;

/**
 * TODO need to process all messages
 *
 * @author ArchDemon
 */
public final class ServerListener implements MessageListener<HostedConnection> {

    private final NetworkServer host;

    public ServerListener(NetworkServer host) {
        this.host = host;
    }

    @Override
    public void messageReceived(HostedConnection source, Message message) {
//        if (message instanceof MessageChat) {
//            // do something with the message
//            MessageChat msg = (MessageChat) message;
//            source.getServer().broadcast(message);
//
//        } else
        if (message instanceof MessageTime msg) {
            // Send the latest game time back
            long time = host.getGameTime();
            source.send(msg.updateGameTime(time).setReliable(true));

        } else if (message instanceof MessagePlayerInfo msg) {

            // Send a message back to the player with their entity ID
            source.send(new MessagePlayerInfo(msg.getName(), msg.getMemory()).setReliable(true));

            // Send the current game time
            long time = host.getGameTime();
            source.send(new MessageTime(time).setReliable(true));

        }
    }
}
