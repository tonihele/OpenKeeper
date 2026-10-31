/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.game.state.session;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.view.ClientEntityData;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * In a local game the server and the client share the entity data, but the
 * client gets it like over the network: read-only and without waiting for the
 * server
 */
class LocalGameSessionTest {

    private static final short PLAYER = 3;

    @Test
    void serverWritesAndClientReadsThroughEntitySets() {
        LocalGameSession session = new LocalGameSession();
        EntityData server = session.getServerEntityData();
        EntityId entity = server.createEntity();
        server.setComponent(entity, new Owner(PLAYER, PLAYER));

        EntityData client = session.getEntityData();
        EntitySet owned = client.getEntities(Owner.class);
        owned.applyChanges();

        assertTrue(owned.containsId(entity));
        assertThrows(UnsupportedOperationException.class, () -> client.getComponent(entity, Owner.class));
        owned.release();
    }

    @Test
    void clientAlwaysGetsTheSameEntityData() {
        LocalGameSession session = new LocalGameSession();

        assertInstanceOf(ClientEntityData.class, session.getEntityData());
        assertSame(session.getEntityData(), session.getEntityData());
    }

}
