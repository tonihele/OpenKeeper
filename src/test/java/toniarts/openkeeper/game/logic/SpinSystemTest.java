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
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.Spin;
import toniarts.openkeeper.tools.convert.map.GameObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A spin stops after 21 game ticks, as in the original
 */
class SpinSystemTest {

    private static final int TICKS_PER_SECOND = 4;

    private final EntityData entityData = new DefaultEntityData();
    private final SpinSystem spinSystem = new SpinSystem(entityData, this::getGameTime);
    private int ticks;

    @Test
    void theWheelGoesBackToItsNormalStateWhenTheSpinStops() {
        EntityId wheel = entityData.createEntity();
        spin(wheel);

        tick(SpinSystem.SPIN_TICKS);
        assertEquals(GameObject.State.SPINNING, getState(wheel));

        tick(1);
        assertEquals(GameObject.State.NONE, getState(wheel));
        assertNull(entityData.getComponent(wheel, Spin.class));
    }

    @Test
    void aNewSpinStartsOver() {
        EntityId wheel = entityData.createEntity();
        spin(wheel);
        tick(16);

        spin(wheel);
        tick(SpinSystem.SPIN_TICKS);
        assertEquals(GameObject.State.SPINNING, getState(wheel));

        tick(1);
        assertEquals(GameObject.State.NONE, getState(wheel));
    }

    @Test
    void anObjectThatDoesNotSpinIsLeftAlone() {
        EntityId wheel = entityData.createEntity();
        setState(wheel, GameObject.State.SPINNING);

        tick(SpinSystem.SPIN_TICKS + 1);
        assertEquals(GameObject.State.SPINNING, getState(wheel));
        assertNull(entityData.getComponent(wheel, Spin.class));
    }

    private void tick(int count) {
        for (int i = 0; i < count; i++) {
            spinSystem.processTick(1f / TICKS_PER_SECOND);
            ticks++;
        }
    }

    private double getGameTime() {
        return (double) ticks / TICKS_PER_SECOND;
    }

    private void spin(EntityId wheel) {
        setState(wheel, GameObject.State.SPINNING);
        entityData.setComponent(wheel, new Spin(getGameTime() + (double) SpinSystem.SPIN_TICKS / TICKS_PER_SECOND));
    }

    private void setState(EntityId wheel, GameObject.State state) {
        entityData.setComponent(wheel, new ObjectViewState((short) 30, state, ObjectViewState.GameObjectAnimState.MESH_RESOURCE, true));
    }

    private GameObject.State getState(EntityId wheel) {
        return entityData.getComponent(wheel, ObjectViewState.class).state;
    }

}
