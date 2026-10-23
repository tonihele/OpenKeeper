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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShotsControllerTest {

    private static final short HEAL_ID = 1;
    private static final short CREATE_OBJECT_ID = 2;
    private static final short SIGHT_OF_EVIL_ID = 33;
    private static final short PLAYER_ID = 3;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Short, Shot> shotsById = new HashMap<>();
    private final List<Object[]> loadedObjects = new ArrayList<>();
    private final ShotsController shotsController;

    ShotsControllerTest() {
        addShot(HEAL_ID, Shot.ProcessType.MODIFY_HEALTH);
        addShot(CREATE_OBJECT_ID, Shot.ProcessType.CREATE_OBJECT);
        addShot(SIGHT_OF_EVIL_ID, Shot.ProcessType.SIGHT_OF_EVIL);
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> method.getName().equals("getShotById") ? shotsById.get((Short) args[0]) : null);
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(), new Class<?>[]{IObjectsController.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("loadObject")) {
                        loadedObjects.add(args);
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

        shotsController = new ShotsController(kwdFile, entityData, null, () -> 0.0, null, null, null, objectsController, null);
    }

    @Test
    void healingShotTakesTheDamageAway() {
        EntityId creature = entityData.createEntity();

        shotsController.createShot(HEAL_ID, 50, 0, PLAYER_ID, new Vector3f(), creature);

        assertEquals(-50, entityData.getComponent(creature, Damage.class).damage);
    }

    @Test
    void objectShotCreatesTheObjectForThePlayer() {
        Vector3f position = new Vector3f(1.5f, 1f, 2.5f);

        shotsController.createShot(CREATE_OBJECT_ID, 7, 0, PLAYER_ID, position, null);

        assertEquals(1, loadedObjects.size());
        assertEquals((short) 7, loadedObjects.get(0)[0]);
        assertEquals(PLAYER_ID, loadedObjects.get(0)[1]);
        assertEquals(position, loadedObjects.get(0)[2]);
    }

    @Test
    void notImplementedProcessTypeDoesNothing() {
        shotsController.createShot(SIGHT_OF_EVIL_ID, 5, 0, PLAYER_ID, new Vector3f(), null);

        assertTrue(loadedObjects.isEmpty());
    }

    private void addShot(short shotId, Shot.ProcessType processType) {
        shotsById.put(shotId, ShotFixtures.shot(shotId, processType, 0, EnumSet.noneOf(Shot.ShotProcessFlag.class)));
    }

}
