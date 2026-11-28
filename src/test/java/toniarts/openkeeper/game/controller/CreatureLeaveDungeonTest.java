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
package toniarts.openkeeper.game.controller;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureTypePenalty;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.logic.PortalCreatureChoice;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Like in DK II, the Portals of a keeper shun the creature type that left the
 * dungeon for a while, whether it walked out or was dropped on the portal
 */
class CreatureLeaveDungeonTest {

    private static final short PLAYER_ID = 3;
    private static final short CREATURE_ID = 5;

    private final EntityData entityData = new DefaultEntityData();
    private final CreaturesController controller = new CreaturesController(stub(IKwdFile.class), entityData,
            Collections.emptyMap(), stub(IGameTimer.class), stub(IGameController.class), stub(IMapController.class), null, null);

    @Test
    void portalsShunTheTypeThatLeft() {
        EntityId creature = creature();
        controller.leaveDungeon(creature);
        controller.leaveDungeon(creature());

        assertNull(entityData.getComponent(creature, CreatureComponent.class), "it is gone");

        // One penalty for the type, however many of its kind left
        Set<EntityId> penalties = entityData.findEntities(null, CreatureTypePenalty.class);
        assertEquals(1, penalties.size());
        CreatureTypePenalty penalty = entityData.getComponent(penalties.iterator().next(), CreatureTypePenalty.class);
        assertEquals(PLAYER_ID, penalty.playerId);
        assertEquals(CREATURE_ID, penalty.creatureId);
        assertEquals(PortalCreatureChoice.PENALTY_GENERATIONS, penalty.penalty);
    }

    private EntityId creature() {
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = CREATURE_ID;
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature, creatureComponent, new Owner(PLAYER_ID, PLAYER_ID));
        return creature;
    }

    private static <T> T stub(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Class<?> returnType = method.getReturnType();
            if (returnType == List.class) {
                return List.of();
            }
            if (returnType.isInterface()) {
                return stub(returnType);
            }
            return returnType.isPrimitive() && returnType != void.class ? Array.get(Array.newInstance(returnType, 1), 0) : null;
        }));
    }

}
