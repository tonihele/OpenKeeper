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
package toniarts.openkeeper.view.control;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.Position;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static toniarts.openkeeper.tools.convert.map.GameObjectFixtures.gameObject;

class ObjectViewControlHaulingTest {

    @Test
    void followsTheHaulerEachUpdateAndKeepsTheObjectsHeightAndRotation() {
        EntityData entityData = new DefaultEntityData();
        EntityId hauler = entityData.createEntity();
        EntityId carried = entityData.createEntity();
        entityData.setComponent(hauler, new Position(0.3f, new Vector3f(1, 0, 2)));
        entityData.setComponents(carried, new HauledBy(hauler), new Position(0.8f, new Vector3f(0, 1.5f, 0)));

        // Object 109 skips its starting animation, so this control test needs no asset manager.
        ObjectViewControl control = new ObjectViewControl(carried, entityData,
                gameObject("Test", (short) 109, 0), new ObjectViewState(), null, null);
        Node spatial = new Node();
        control.setSpatial(spatial);
        control.update(1.0f / 60);

        assertEquals(new Vector3f(1, 1.5f, 2), spatial.getLocalTranslation());

        entityData.setComponent(hauler, new Position(0.5f, new Vector3f(4.25f, 0, 7.75f)));
        control.update(1.0f / 60);

        assertEquals(new Vector3f(4.25f, 1.5f, 7.75f), spatial.getLocalTranslation());
        assertEquals(0.8f, entityData.getComponent(carried, Position.class).rotation);

        entityData.removeComponent(carried, HauledBy.class);
        entityData.setComponent(hauler, new Position(0.2f, new Vector3f(9, 0, 11)));
        control.update(1.0f / 60);

        assertEquals(new Vector3f(4.25f, 1.5f, 7.75f), spatial.getLocalTranslation());
        control.cleanup();
    }

    @Test
    void usesTheInjectedPositionProvider() {
        EntityData entityData = new DefaultEntityData();
        EntityId hauler = entityData.createEntity();
        EntityId carried = entityData.createEntity();
        entityData.setComponent(carried, new HauledBy(hauler));
        entityData.setComponent(carried, new Position(0.8f, new Vector3f(0, 1.5f, 0)));

        HauledObjectPositionProvider positionProvider = (data, haulerId, objectPosition, store) -> {
            assertSame(entityData, data);
            assertEquals(hauler, haulerId);
            assertEquals(1.5f, objectPosition.position.y);
            store.set(8, objectPosition.position.y, 3);
            return true;
        };
        ObjectViewControl control = new ObjectViewControl(carried, entityData,
                gameObject("Test", (short) 109, 0), new ObjectViewState(), null, null, null, false,
                positionProvider);
        Node spatial = new Node();
        control.setSpatial(spatial);
        control.update(1.0f / 60);

        assertEquals(new Vector3f(8, 1.5f, 3), spatial.getLocalTranslation());
        control.cleanup();
    }
}
