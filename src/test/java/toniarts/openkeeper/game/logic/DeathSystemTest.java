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
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.tools.convert.map.GraveyardFixtures;
import toniarts.openkeeper.tools.convert.map.Variable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class DeathSystemTest {

    private final EntityData entityData = new DefaultEntityData();
    private final DeathSystem system = new DeathSystem(entityData, Map.of(
            Variable.MiscVariable.MiscType.DEAD_BODY_DIES_AFTER_SECONDS,
            GraveyardFixtures.miscVariable(Variable.MiscVariable.MiscType.DEAD_BODY_DIES_AFTER_SECONDS, 2)),
            positionLookup());

    @Test
    void bodyDecaysAfterTheLevelTime() {
        EntityId corpse = createCorpse();

        tick(6);
        assertNotNull(entityData.getComponent(corpse, Death.class));

        tick(2);
        assertNull(entityData.getComponent(corpse, Death.class));
    }

    @Test
    void bodyDoesNotDecayWhileItIsCarried() {
        EntityId corpse = createCorpse();
        EntityId imp = entityData.createEntity();
        entityData.setComponent(corpse, new HauledBy(imp));

        tick(20);
        assertNotNull(entityData.getComponent(corpse, Death.class));

        entityData.removeComponent(corpse, HauledBy.class);
        tick(6);
        assertNotNull(entityData.getComponent(corpse, Death.class));

        tick(3);
        assertNull(entityData.getComponent(corpse, Death.class));
    }

    private EntityId createCorpse() {
        EntityId corpse = entityData.createEntity();
        entityData.setComponent(corpse, new Death(0));
        tick(1);
        return corpse;
    }

    private void tick(int ticks) {
        for (int i = 0; i < ticks; i++) {
            system.processTick(0.25f);
        }
    }

    private IEntityPositionLookup positionLookup() {
        IEntityController entityController = (IEntityController) Proxy.newProxyInstance(
                IEntityController.class.getClassLoader(), new Class<?>[]{IEntityController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "removePossession" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return (IEntityPositionLookup) Proxy.newProxyInstance(
                IEntityPositionLookup.class.getClassLoader(), new Class<?>[]{IEntityPositionLookup.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityController" -> entityController;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
