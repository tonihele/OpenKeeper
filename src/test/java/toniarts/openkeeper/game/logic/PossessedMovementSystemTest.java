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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.PossessedMovement;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapData;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PossessedMovementSystemTest {

    @Test
    void possessedCreatureStopsBeforeAnImpassableTileAtLargeDelta() {
        DefaultEntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature,
                new Possessed(),
                new PossessedMovement(new Vector2f(1, 0), 0f, 5f),
                new Position(0f, new Vector3f(0, 0, 0)));

        IMapController mapController = mapWithWallAtXOne();
        ICreaturesController creaturesController = creaturesWithWallAtXOne();

        PossessedMovementSystem system = new PossessedMovementSystem(entityData, mapController, null, creaturesController);
        system.processTick(0.5f);

        float x = entityData.getComponent(creature, Position.class).position.x;
        assertTrue(x > 0f && x <= 1f / 3f + 0.001f, "creature crossed wall to x=" + x);
        system.stop();
        entityData.close();
    }

    @Test
    void malformedPossessedInputCannotWriteInvalidPosition() {
        DefaultEntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature,
                new Possessed(),
                new PossessedMovement(new Vector2f(Float.NaN, 0f), Float.NaN, 5f),
                new Position(0f, new Vector3f(0, 0, 0)));
        GridMovementCollision collision = new GridMovementCollision(null, null, MovementSettings.DEFAULT);
        PossessedMovementSystem system = new PossessedMovementSystem(entityData, null, collision);

        system.processTick(1f / 20f);

        Position position = entityData.getComponent(creature, Position.class);
        assertEquals(0f, position.rotation, 0f);
        assertEquals(0f, position.position.x, 0f);
        assertEquals(0f, position.position.z, 0f);
        system.stop();
        entityData.close();
    }

    static IMapController mapWithWallAtXOne() {
        IMapData mapData = proxy(IMapData.class, (object, method, args) -> {
            if (method.getName().equals("getTile")) {
                Point location = args[0] instanceof Point point
                        ? point : new Point((int) args[0], (int) args[1]);
                return tileAt(location);
            }
            return defaultValue(method.getReturnType());
        });
        return proxy(IMapController.class, (object, method, args) ->
                method.getName().equals("getMapData") ? mapData : defaultValue(method.getReturnType()));
    }

    static ICreaturesController creaturesWithWallAtXOne() {
        ICreatureController creatureController = proxy(ICreatureController.class, (object, method, args) -> {
            if (method.getName().equals("getCost")) {
                IMapTileController destination = (IMapTileController) args[1];
                return destination.getLocation().x > 0 ? null : 1f;
            }
            return defaultValue(method.getReturnType());
        });
        return proxy(ICreaturesController.class, (object, method, args) ->
                method.getName().equals("createController") ? creatureController : defaultValue(method.getReturnType()));
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    private static IMapTileController tileAt(Point location) {
        return proxy(IMapTileController.class, (object, method, args) -> {
            if (method.getName().equals("getLocation")) {
                return location;
            }
            if (method.getName().equals("getX")) {
                return location.x;
            }
            if (method.getName().equals("getY")) {
                return location.y;
            }
            return defaultValue(method.getReturnType());
        });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0f;
        }
        if (type == double.class) {
            return 0d;
        }
        return null;
    }
}
