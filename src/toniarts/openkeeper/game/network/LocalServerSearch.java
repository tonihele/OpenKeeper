/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

            for (short i = 1; i < 255; i++) {                
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
    
    
    