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
package toniarts.openkeeper.view;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.WatchedEntity;
import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.es.filter.FieldFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Owner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientEntityDataTest {

    private static final short PLAYER = 3;
    private static final short ENEMY = 4;

    private EntityData server;
    private EntityData client;
    private EntityId entity;

    @BeforeEach
    void setUp() {
        server = new DefaultEntityData();
        client = new ClientEntityData(server);
        entity = server.createEntity();
        server.setComponent(entity, new Owner(PLAYER, PLAYER));
    }

    @Test
    void entitySetsFollowTheServer() {
        EntitySet owned = client.getEntities(Owner.class);
        owned.applyChanges();
        assertTrue(owned.containsId(entity));

        server.setComponent(entity, new Owner(PLAYER, ENEMY));
        owned.applyChanges();

        assertEquals(ENEMY, owned.getEntity(entity).get(Owner.class).controlId);
        owned.release();
    }

    @Test
    void watchedEntitiesFollowTheServer() {
        WatchedEntity watched = client.watchEntity(entity, Owner.class);
        assertNotNull(watched.get(Owner.class));

        server.setComponent(entity, new Owner(PLAYER, ENEMY));
        watched.applyChanges();

        assertEquals(ENEMY, watched.get(Owner.class).controlId);
        watched.release();
    }

    @Test
    void lookingUpFromTheServerIsRefused() {
        assertThrows(UnsupportedOperationException.class, () -> client.getComponent(entity, Owner.class));
        assertThrows(UnsupportedOperationException.class, () -> client.getEntity(entity, Owner.class));
        assertThrows(UnsupportedOperationException.class, () -> client.findEntity(null, Owner.class));
        assertThrows(UnsupportedOperationException.class,
                () -> client.findEntities(new FieldFilter<>(Owner.class, "ownerId", PLAYER), Owner.class));
    }

    @Test
    void clientIsReadOnly() {
        assertThrows(UnsupportedOperationException.class, () -> client.createEntity());
        assertThrows(UnsupportedOperationException.class, () -> client.removeEntity(entity));
        assertThrows(UnsupportedOperationException.class, () -> client.setComponent(entity, new Owner(ENEMY, ENEMY)));
        assertThrows(UnsupportedOperationException.class, () -> client.setComponents(entity, new Owner(ENEMY, ENEMY)));
        assertThrows(UnsupportedOperationException.class, () -> client.removeComponent(entity, Owner.class));
        assertThrows(UnsupportedOperationException.class, () -> client.removeComponents(entity, Owner.class));

        assertEquals(PLAYER, server.getComponent(entity, Owner.class).ownerId);
    }

    @Test
    void closingTheClientLeavesTheServerOpen() {
        client.close();

        assertNotNull(server.getComponent(entity, Owner.class));
        assertNotNull(server.createEntity());
    }

}
