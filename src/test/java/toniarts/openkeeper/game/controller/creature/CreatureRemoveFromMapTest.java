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
package toniarts.openkeeper.game.controller.creature;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IObjectsController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CreatureRemoveFromMapTest {

    @Test
    void removedCreatureIsGoneAndItsGoldVanishes() {
        EntityData entityData = new DefaultEntityData();
        AtomicInteger goldPiles = new AtomicInteger();
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(
                IObjectsController.class.getClassLoader(), new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> {
                    if (method.getName().equals("addLooseGold")) {
                        goldPiles.incrementAndGet();
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new Position(0, new Vector3f(5.5f, 0, 7.5f)));
        entityData.setComponent(entityId, new Owner((short) 3, (short) 3));
        entityData.setComponent(entityId, new Gold(100, 300));

        new CreatureController(entityId, entityData, null, null, null, null, null, null, null, null, null,
                objectsController, null, null).removeFromMap();

        assertNull(entityData.getComponent(entityId, Position.class));
        assertNull(entityData.getComponent(entityId, Gold.class));
        assertEquals(0, goldPiles.get());
    }

}
