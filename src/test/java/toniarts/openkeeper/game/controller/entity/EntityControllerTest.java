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
package toniarts.openkeeper.game.controller.entity;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.PortalGem;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityControllerTest {

    private final EntityData entityData = new DefaultEntityData();
    private final List<EntityId> droppedFrom = new ArrayList<>();

    @Test
    void carrierDropsThePortalGemWhenItsPossessionIsRemoved() {
        EntityId lord = entityData.createEntity();
        entityData.setComponent(lord, new PortalGem());
        entityData.setComponent(lord, new Position(0, WorldUtils.pointToVector3f(4, 5)));

        controller(lord).removePossession();

        assertEquals(List.of(lord), droppedFrom);
    }

    @Test
    void creatureWithoutPortalGemDropsNothing() {
        EntityId creature = entityData.createEntity();
        entityData.setComponent(creature, new Position(0, WorldUtils.pointToVector3f(4, 5)));

        controller(creature).removePossession();

        assertEquals(List.of(), droppedFrom);
    }

    private EntityController controller(EntityId entityId) {
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("dropPortalGem")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    droppedFrom.add((EntityId) args[0]);
                    return null;
                });
        return new EntityController(entityId, entityData, objectsController, null);
    }

}
