/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.game.network.streaming;

import com.jme3.network.HostedConnection;
import com.jme3.network.serializing.Serializer;
import com.jme3.network.service.AbstractHostedService;
import com.jme3.network.service.HostedServiceManager;
import java.io.IOException;
import java.lang.System.Logger;
import java.nio.ByteBuffer;
import toniarts.openkeeper.game.network.message.StreamedMessage;

/**
 * Streaming data, meaning that if something is needed to be send over to
 * clients that doesn't fit to a single message, this is your service. We simply
 * keep sending messages until everything is transfered. The payload needs to be
 * serializable though...
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class StreamingHostedService extends AbstractHostedService {
    
    private static final Logger logger = System.getLogger(StreamingHostedService.class.getName());

    private static final int MAX_MESSAGE_SIZE = 30000;
    private static final int MAX_TOTAL_SIZE = 10000000;

    @Override
    protected void onInitialize(HostedServiceManager serviceManager) {

    }

    /**
     * Simple blocking send method that sends your serializable object in as
     * many packages as is needed
     *
     * @param messageType the type of messages, so that the receiver can
     * recognice and listen to it
     * @param data the serializable data
     * @param sendTo a specific client to send to, null will broadcast
     * @throws IOException the serialization may fail
     */
    public void sendData(int messageType, Object data, HostedConnection sendTo) throws IOException {

        // Allocate max size buffer and write the object
        ByteBuffer buffer = ByteBuffer.allocate(MAX_TOTAL_SIZE);
        Serializer.writeClassAndObject(buffer, data);

        // Allocate a new job
        int totalSize = buffer.position();
        int written = 0;
        buffer.rewind();
        while (written < totalSize) {
            int write = Math.min(MAX_MESSAGE_SIZE, totalSize - written);
            byte[] part = new byte[write];
            buffer.get(part);
            written += write;
            StreamedMessage message = new StreamedMessage(part, totalSize, messageType);
            message.setReliable(true);

            // Broadcast if receiver is null
            if (sendTo == null) {
                getServer().broadcast(message);
            } else {
                sendTo.send(message);
            }
        }
    }

}
