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
package toniarts.openkeeper.game.task.creature;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Torture job of the Mistress: whip a victim, or be tortured on a free
 * device
 */
class TortureTest {

    private static final short KEEPER = 3;
    private static final Point DEVICE_TILE = new Point(5, 6);

    private final EntityData entityData = new DefaultEntityData();
    private final List<EntityId> victims = new ArrayList<>();
    private final EntityId device = entityData.createEntity();
    private boolean full;

    TortureTest() {
        entityData.setComponent(device, new Position(0, new Vector3f(5, 1, 6)));
    }

    @Test
    void creatureGetsOnAFreeDeviceUntilAVictimIsPutOnIt() {
        Torture torture = torture();

        assertTrue(torture.isValid(null));
        assertEquals(device, torture.getTaskTarget());
        assertEquals(new Vector2f(5, 6), torture.getTarget(null));
        assertFalse(torture.isFaceTarget());
        assertEquals(TaskType.TORTURE, torture.getTaskType());

        victims.add(entityData.createEntity());
        assertFalse(torture.isValid(null));
    }

    @Test
    void creatureWhipsTheVictimOnTheDeviceUntilItIsGone() {
        EntityId victim = entityData.createEntity();
        victims.add(victim);
        full = true;
        Torture torture = torture();

        assertTrue(torture.isValid(null));
        assertEquals(victim, torture.getTaskTarget());
        assertTrue(torture.isFaceTarget());

        victims.clear();
        assertFalse(torture.isValid(null));
    }

    private Torture torture() {
        IRoomObjectControl control = (IRoomObjectControl) Proxy.newProxyInstance(IRoomObjectControl.class.getClassLoader(), new Class<?>[]{IRoomObjectControl.class}, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "getItems" ->
                    DEVICE_TILE.equals(args[0]) ? new ArrayList<>(victims) : Collections.emptyList();
                case "isFullCapacity" ->
                    full;
                default ->
                    throw new UnsupportedOperationException(method.getName());
            };
        });
        IRoomController room = (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "isRemoved", "isDestroyed" ->
                    false;
                case "getOwnerId" ->
                    KEEPER;
                case "getObjectControl" ->
                    control;
                case "getFloorFurniture" ->
                    Set.of(device);
                case "getWallFurniture" ->
                    Set.of();
                default ->
                    throw new UnsupportedOperationException(method.getName());
            };
        });
        return new Torture(null, null, DEVICE_TILE, KEEPER, room, null, entityData);
    }

}
