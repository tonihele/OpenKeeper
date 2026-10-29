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
package toniarts.openkeeper.game.controller.room;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Dungeon Heart starts with the health set for it in the level editor.
 */
class HeartInitialHealthTest {

    private static final int HEART_HEALTH = 10000;

    private final EntityData entityData = new DefaultEntityData();
    private EntityId heartObject;

    @Test
    void heartStartsWithTheEditorHealth() {
        heart(1).constructObjects();

        assertEquals(100, entityData.getComponent(heartObject, Health.class).health);
        assertEquals(HEART_HEALTH, entityData.getComponent(heartObject, Health.class).maxHealth);
    }

    @Test
    void heartWithoutEditorHealthStartsAtFullHealth() {
        heart(100).constructObjects();

        assertEquals(HEART_HEALTH, entityData.getComponent(heartObject, Health.class).health);
    }

    private FiveByFiveRotatedController heart(int initialHealth) {
        RoomInstance room = new RoomInstance(new Room(), RoomFixtures.roomThing(initialHealth));
        room.setOwnerId((short) 3);
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                room.addCoordinate(new Point(x, y));
            }
        }
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> {
                    EntityId object = entityData.createEntity();
                    if ((short) args[0] == FiveByFiveRotatedController.OBJECT_HEART_ID) {
                        entityData.setComponent(object, new Health(HEART_HEALTH, HEART_HEALTH));
                        heartObject = object;
                    }
                    return object;
                });
        return new FiveByFiveRotatedController(entityData.createEntity(), entityData, null, room, objectsController,
                Map.of(MiscType.MAX_GOLD_PER_DUNGEON_HEART_TILE, CreatureSleepFixtures.variable(MiscType.MAX_GOLD_PER_DUNGEON_HEART_TILE, 100)),
                null);
    }
}
