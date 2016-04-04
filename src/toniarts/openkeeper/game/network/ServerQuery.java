/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.game.network;

import com.jme3.network.Client;
import com.jme3.network.Network;
import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.logging.Logger;

/**
 *
 * @author ArchDemon
 */
public abstract class ServerQuery extends Thread {
    private LinkedList<ServerInfo> queue;
    private static final Logger logger = Logger.getLogger(ServerQuery.class.getName());

    public ServerQuery(LinkedList queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        ServerInfo server;

        while (true) {
            synchronized(queue) {
                if (queue.isEmpty() || isInterrupted()) {
                    break;
                }
                server = queue.pop();
            }

            try {
                InetAddress ipAddress = InetAddress.getByName(server.getHost());
                //if (!ipAddress.isReachable(null, 255, 500)) { continue; }
                Client client = Network.connectToServer(server.getHost(), server.getPort());
                onFound(server);
                client.close();
                break;
            } catch (IOException ex) {
                logger.log(java.util.logging.Level.SEVERE, String.format("No {0}. {1}", server.getHost(), ex.getLocalizedMessage()));
            }

        }
    }

    public abstract void onFound(ServerInfo server);
}

