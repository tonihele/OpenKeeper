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

package toniarts.openkeeper.game.network.message;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;


/**
 *  Dual-purpose message that is used to tell the client
 *  about the latest game time but also can be used to 
 *  determine ping times.
 * 
 *  @author    Paul Speed          from  MonkeyTrap example
 */
@Serializable
public final class MessageTime extends AbstractMessage {

    private long time;
    private long sent;

    public MessageTime() {
    }
    
    public MessageTime( long time ) {
        this.time = time;
        this.sent = time;
    }

    public MessageTime( long time, long sent ) {
        this.time = time;
        this.sent = sent;
    }
 
    public MessageTime updateGameTime( long time ) {
        return new MessageTime(time, sent);
    }
    
    public long getTime() {
        return time;
    }
 
    public long getSentTime() {
        return sent;
    }
    
    @Override   
    public String toString() {
        return "MessageTime(time=" + time + ", sent=" + sent + ")";
    }
}
