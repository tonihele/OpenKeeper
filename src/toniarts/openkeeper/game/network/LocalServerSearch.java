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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.ConversionUtils;

/**
 *
 * @author ArchDemon
 */


public abstract class LocalServerSearch {

    private final static int nThreads = 10;
    private int port = 7575;
    private final ServerQuery[] threads;
    private final LinkedList<ServerInfo> queue;
    private List<ServerInfo> servers = new ArrayList();
    private static final Logger logger = Logger.getLogger(LocalServerSearch.class.getName());

    public LocalServerSearch(int port) {
        this.port = port;
        queue = new LinkedList();
        threads = new ServerQuery[nThreads];
        addLocalHosts();
    }

    public void start() {
        for (int i = 0; i < nThreads; i++) {
            threads[i] = new ServerQuery(queue) {

                @Override
                public void onFound(ServerInfo server) {
                    servers.add(server);
                    LocalServerSearch.this.onFound(server);
                }
            };
            threads[i].start();
        }
    }

    private void addLocalHosts() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            byte[] ipLan = address.getAddress();

            for (short i = 39; i < 42; i++) {
                String host = String.format("%s.%s.%s.%s",
                        ConversionUtils.toUnsignedByte(ipLan[0]),
                        ConversionUtils.toUnsignedByte(ipLan[1]),
                        ConversionUtils.toUnsignedByte(ipLan[2]),
                        i);
                add(new ServerInfo(host, port));
            }
        } catch (UnknownHostException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
    }

    private void add(ServerInfo r) {
        synchronized(queue) {
            queue.addLast(r);
            queue.notify();
        }
    }

    public List<ServerInfo> getServers() {
        return servers;
    }

    public abstract void onFound(ServerInfo server);
}