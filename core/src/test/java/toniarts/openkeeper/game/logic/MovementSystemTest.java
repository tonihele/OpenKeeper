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
import java.util.ArrayList;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementSystemTest {

    /**
     * A creature standing on 15,23 turns on the spot to face 16,25. Facing is
     * (sin r, cos r) on x, z for the Position rotation r.
     */
    @ParameterizedTest
    @ValueSource(floats = {0f, -0.46f, 1f, -1f, 3f, -3f})
    void turnsOnTheSpotToFaceTheTarget(float rotation) {
        EntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature, new Position(rotation, new Vector3f(15, 0, 23)), new Mobile(false, false, false, 1),
                new Navigation(new Point(15, 23), new Point(16, 25), new ArrayList<>()));
        MovementSystem movementSystem = new MovementSystem(entityData);

        for (int frame = 0; frame < 180 && entityData.getComponent(creature, Navigation.class) != null; frame++) {
            movementSystem.processTick(1 / 60f);
        }

        assertNull(entityData.getComponent(creature, Navigation.class), "still turning after 3 seconds");
        float facing = entityData.getComponent(creature, Position.class).rotation;
        double cos = (Math.sin(facing) * 1 + Math.cos(facing) * 2) / Math.sqrt(5);
        assertTrue(cos > Math.cos(0.2), "facing " + Math.toDegrees(Math.acos(cos)) + " degrees away");
    }
}
