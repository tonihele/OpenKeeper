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

import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.serializing.Serializer;
import com.jme3.network.service.AbstractClientService;
import com.jme3.network.service.ClientServiceManager;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.network.message.StreamedMessage;

/**
 * A client that listens for our streamed packages and notifies you when they
 * are done
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class StreamingClientService extends AbstractClientService {
    
    private static final Logger logger = Logger.getLogger(StreamingClientService.class.getName());

    private final Map<Integer, Map<Integer, ByteBuffer>> messageReceiveMap = new ConcurrentHashMap<>();
    private final MessageListener<Client> messageListener = new StreamedMessageServiceListenerImpl();
    private final Map<Integer, List<StreamedMessageListener>> listeners = new HashMap<>();

    @Override
    protected void onInitialize(ClientServiceManager serviceManager) {

        // Start listening for messages
        getClient().addMessageListener(messageListener, StreamedMessage.class);
    }

    @Override
    public void terminate(ClientServiceManager serviceManager) {
        super.terminate(serviceManager);

        // We are not listening for messages anymore
        getClient().removeMessageListener(messageListener, StreamedMessage.class);

        // Get rid of all buffers
        for (Map<Integer, ByteBuffer> buffers : messageReceiveMap.values()) {
            buffers.clear();
        }
    }

    public void addListener(int messageType, StreamedMessageListener listener) {
        List<StreamedMessageListener> messageListeners = listeners.get(messageType);
        if (messageListeners == null) {
            messageListeners = new CopyOnWriteArrayList();
            listeners.put(messageType, messageListeners);
        }
        messageListeners.add(listener);
    }

    public void removeListener(int messageType, StreamedMessageListener listener) {
        List<StreamedMessageListener> messageListeners = listeners.get(messageType);
        if (messageListeners != null) {
            messageListeners.remove(listener);
        }
    }

    /**
     * Listens to the messages and parses them up
     */
    private class StreamedMessageServiceListenerImpl implements MessageListener<Client> {

        @Override
        public void messageReceived(Client source, Message m) {
            StreamedMessage message = (StreamedMessage) m;

            // Set up a buffer for this client
            Map<Integer, ByteBuffer> messageBuffers = messageReceiveMap.get(source.getId());
            if (messageBuffers == null) {
                messageBuffers = new HashMap<>();
                messageReceiveMap.put(source.getId(), messageBuffers);
            }

            // Handle buffer for client's message type
            ByteBuffer messageBuffer = messageBuffers.get(message.getMessageType());
            if (messageBuffer == null) {
                messageBuffer = ByteBuffer.allocate(message.getTotalSize());
                messageBuffers.put(message.getMessageType(), messageBuffer);
            }
            messageBuffer.put(message.getPayload());

            // If the message is complete, notify and discard
            // In theory we should always listen to the whole message, even if nobody is listening when we started
            // There is a possibility that someone starts to listen to it in the middle of transmission
            if (!messageBuffer.hasRemaining()) {
                List<StreamedMessageListener> messageListeners = listeners.get(message.getMessageType());
                if (messageListeners != null) {
                    try {
                        messageBuffer.rewind();

                        // Deserialize
                        Object data = Serializer.readClassAndObject(messageBuffer);

                        // Notify
                        for (StreamedMessageListener listener : messageListeners) {
                            listener.onMessageReceived(data);
                        }
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, "Failed to deserialize the data payload!", ex);
                    }
                }
                messageBuffers.remove(message.getMessageType());
            }
        }

    }

}
