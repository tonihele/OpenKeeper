/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.game.network;

/**
 *
 * @author ArchDemon
 */
public class ServerInfo {
    private String name;
    private String player;
    private int port;
    private String host;

    public ServerInfo(String name, String player, int port) {
        this.name = name;
        this.player = player;
        this.port = port;
    }
    
    public ServerInfo(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public String getPlayer() {
        return player;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }    
}
