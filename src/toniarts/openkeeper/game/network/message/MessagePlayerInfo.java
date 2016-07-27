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
import com.simsilica.es.EntityId;

/**
 * Message that is used to tell the server about the new player info
 *
 * @author ArchDemon
 */
@Serializable
public class MessagePlayerInfo extends AbstractMessage {

    private String name;
    private int memory;
    private EntityId entityId;

    public MessagePlayerInfo() {
    }

    public MessagePlayerInfo(String name, int memory) {
        this.name = name;
        this.memory = memory;
    }

    public MessagePlayerInfo(EntityId id) {
        this.entityId = id;
    }

    public String getName() {
        return name;
    }

    public EntityId getEntityId() {
        return entityId;
    }

    public int getMemory() {
        return memory;
    }

    @Override
    public String toString() {
        return "PlayerInfoMessage[name=" + name + ", entityId=" + entityId + "]";
    }
}
