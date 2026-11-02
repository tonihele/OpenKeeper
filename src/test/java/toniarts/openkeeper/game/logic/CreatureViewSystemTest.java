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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.tools.convert.map.Creature;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreatureViewSystemTest {

    private static final short TORTURE_WHEEL = 8;
    private static final short ELECTRIC_CHAIR = 61;

    private final EntityData entityData = new DefaultEntityData();

    private Creature.AnimationType tortureAnimation(short deviceObjectId) {
        EntityId device = entityData.createEntity();
        entityData.setComponents(device, new ObjectComponent(deviceObjectId, AbstractRoomController.ObjectType.TORTUREE));
        EntityId victim = entityData.createEntity();
        entityData.setComponents(victim, new CreatureViewState((short) 1, 0, Creature.AnimationType.IDLE_1),
                new Position(0, new Vector3f()),
                new CreatureAi(0, CreatureState.TORTURED, (short) 1),
                new CreatureTortured(0, 0, 0, device));

        CreatureViewSystem system = new CreatureViewSystem(entityData);
        system.processTick(1);
        return entityData.getComponent(victim, CreatureViewState.class).state;
    }

    @Test
    void victimOnTheElectricChairSitsInTheChair() {
        assertEquals(Creature.AnimationType.TORTURED_CHAIR, tortureAnimation(ELECTRIC_CHAIR));
    }

    @Test
    void victimOnATortureWheelIsOnTheWheel() {
        assertEquals(Creature.AnimationType.TORTURED_WHEEL, tortureAnimation(TORTURE_WHEEL));
    }
}
