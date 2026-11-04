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
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Creature;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CreatureViewSystemTest {

    private static final short TORTURE_WHEEL = 8;
    private static final short ELECTRIC_CHAIR = 61;

    private final EntityData entityData = new DefaultEntityData();

    private EntityId victim(short deviceObjectId) {
        EntityId device = entityData.createEntity();
        entityData.setComponents(device, new ObjectComponent(deviceObjectId, AbstractRoomController.ObjectType.TORTUREE));
        EntityId victim = entityData.createEntity();
        entityData.setComponents(victim, new CreatureViewState((short) 1, 0, Creature.AnimationType.IDLE_1),
                new Position(0, new Vector3f()),
                new CreatureAi(0, CreatureState.TORTURED, (short) 1),
                new CreatureTortured(0, 0, 0, device));
        return victim;
    }

    private Creature.AnimationType tortureAnimation(short deviceObjectId) {
        EntityId victim = victim(deviceObjectId);
        CreatureViewSystem system = new CreatureViewSystem(entityData);
        system.processTick(1);
        return entityData.getComponent(victim, CreatureViewState.class).state;
    }

    @Test
    void victimOnTheElectricChairIsElectrocutedFirst() {
        assertEquals(Creature.AnimationType.TORTURED_CHAIR_SKELETON, tortureAnimation(ELECTRIC_CHAIR));
    }

    @Test
    void victimOnTheElectricChairIsElectrocutedForThreeSecondsThenSitsForThreeSeconds() {
        EntityId victim = victim(ELECTRIC_CHAIR);
        CreatureViewSystem system = new CreatureViewSystem(entityData);

        // Four ticks a second, like in DK II
        Creature.AnimationType[] seconds = new Creature.AnimationType[12];
        for (int tick = 0; tick < seconds.length * 4; tick++) {
            system.processTick(0.25f);
            if (tick % 4 == 0) {
                seconds[tick / 4] = entityData.getComponent(victim, CreatureViewState.class).state;
            }
        }

        Creature.AnimationType skeleton = Creature.AnimationType.TORTURED_CHAIR_SKELETON;
        Creature.AnimationType chair = Creature.AnimationType.TORTURED_CHAIR;
        assertArrayEquals(new Creature.AnimationType[]{skeleton, skeleton, skeleton, chair, chair, chair,
            skeleton, skeleton, skeleton, chair, chair, chair}, seconds);
    }

    @Test
    void victimOnATortureWheelIsOnTheWheel() {
        assertEquals(Creature.AnimationType.TORTURED_WHEEL, tortureAnimation(TORTURE_WHEEL));
    }

    @Test
    void mistressWhipsTheVictimSheTortures() {
        EntityId victim = entityData.createEntity();
        entityData.setComponent(victim, new CreatureComponent());
        assertEquals(Creature.AnimationType.SPECIAL_1, torturerAnimation(victim));
    }

    @Test
    void mistressOnATortureDeviceIsTorturedLikeAVictim() {
        EntityId wheel = entityData.createEntity();
        entityData.setComponent(wheel, new ObjectComponent(TORTURE_WHEEL, AbstractRoomController.ObjectType.TORTUREE));
        EntityId chair = entityData.createEntity();
        entityData.setComponent(chair, new ObjectComponent(ELECTRIC_CHAIR, AbstractRoomController.ObjectType.TORTUREE));

        assertEquals(Creature.AnimationType.TORTURED_WHEEL, torturerAnimation(wheel));
        assertEquals(Creature.AnimationType.TORTURED_CHAIR_SKELETON, torturerAnimation(chair));
    }

    private Creature.AnimationType torturerAnimation(EntityId target) {
        EntityId mistress = entityData.createEntity();
        entityData.setComponents(mistress, new CreatureViewState((short) 4, 0, Creature.AnimationType.IDLE_1),
                new Position(0, new Vector3f()),
                new CreatureAi(0, CreatureState.WORK, (short) 4),
                new TaskComponent(1, target, null, TaskType.TORTURE, 0, true));
        CreatureViewSystem system = new CreatureViewSystem(entityData);
        system.processTick(1);
        return entityData.getComponent(mistress, CreatureViewState.class).state;
    }
}
