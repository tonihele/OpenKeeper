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
package toniarts.openkeeper.game.logic;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.KnockedDown;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KnockdownSystemTest {

    private final EntityData entityData = new DefaultEntityData();
    private final Map<EntityId, Integer> knockDownsByCreature = new HashMap<>();
    private final KnockdownSystem knockdownSystem = new KnockdownSystem(entityData, createCreaturesController());

    @Test
    void taggedCreatureIsKnockedDownOnce() {
        EntityId creature = createCreature();
        entityData.setComponent(creature, new KnockedDown());

        knockdownSystem.processTick(0.25f);
        knockdownSystem.processTick(0.25f);

        assertEquals(1, knockDownsByCreature.get(creature));
        assertNull(entityData.getComponent(creature, KnockedDown.class));
    }

    @Test
    void hitsWithinOneTickAreOneKnockDown() {
        EntityId creature = createCreature();
        entityData.setComponent(creature, new KnockedDown());
        entityData.setComponent(creature, new KnockedDown());
        entityData.setComponent(creature, new KnockedDown());

        knockdownSystem.processTick(0.25f);

        assertEquals(1, knockDownsByCreature.get(creature));
    }

    @Test
    void hitsOnLaterTicksKnockDownAgain() {
        EntityId creature = createCreature();

        entityData.setComponent(creature, new KnockedDown());
        knockdownSystem.processTick(0.25f);
        entityData.setComponent(creature, new KnockedDown());
        knockdownSystem.processTick(0.25f);

        assertEquals(2, knockDownsByCreature.get(creature));
    }

    private EntityId createCreature() {
        EntityId creature = entityData.createEntity();
        entityData.setComponent(creature, new CreatureComponent());
        return creature;
    }

    private ICreaturesController createCreaturesController() {
        return (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> {
                    if (method.getName().equals("createController")) {
                        return createCreatureController((EntityId) args[0]);
                    }
                    return null;
                });
    }

    private ICreatureController createCreatureController(EntityId entityId) {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> {
                    if (method.getName().equals("knockDown")) {
                        knockDownsByCreature.merge(entityId, 1, Integer::sum);
                    }
                    return null;
                });
    }

}
