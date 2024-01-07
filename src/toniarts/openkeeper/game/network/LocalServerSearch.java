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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import toniarts.openkeeper.tools.convert.ConversionUtils;

/**
 * @deprecated
 * @author ArchDemon
 */
@Deprecated
public abstract class LocalServerSearch {
    
    private static final Logger logger = System.getLogger(LocalServerSearch.class.getName());
    
    private static final int nThreads = 10;
    
    private int port = 7575;
    private final ServerQuery[] threads;
    private final LinkedList<NetworkServer> queue;
    private final List<NetworkServer> servers = new ArrayList();

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
                public void onFound(NetworkServer server) {
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
                add(new NetworkServer(host, port));
            }
        } catch (UnknownHostException ex) {
            logger.log(Level.ERROR, ex);
        }
    }

    private void add(NetworkServer r) {
        synchronized(queue) {
            queue.addLast(r);
            queue.notify();
        }
    }

    public List<NetworkServer> getServers() {
        return servers;
    }

    public abstract void onFound(NetworkServer server);


    private void getInterfaces() throws SocketException {

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
          NetworkInterface ni = (NetworkInterface) interfaces.nextElement();
          Enumeration addresses = ni.getInetAddresses();
          while (addresses.hasMoreElements()) {
            InetAddress address = (InetAddress)addresses.nextElement();
            if (address instanceof Inet6Address || address.isLoopbackAddress()) {
                continue;
            }
            System.out.printf("InetAddress: %s%n", address);
            //DatagramSocket socket = new DatagramSocket(port, address);
             //Try to read the broadcast messages from socket here
          }
        }
    }
}