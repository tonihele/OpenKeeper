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
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.OnFire;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureFlagFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnFireSystemTest {

    private static final short CREATURE_ID = 1;
    private static final float WIDTH = 0.6f;
    private static final float HEIGHT = 0.8f;
    private static final int FIRE_FLAME_EFFECT_ID = 88;

    private final EntityData entityData = new DefaultEntityData();
    private final List<Vector3f> flames = new ArrayList<>();
    private final Creature creature = CreatureFlagFixtures.creature(CREATURE_ID, EnumSet.noneOf(Creature.CreatureFlag.class));
    private double gameTime = 0;
    private final OnFireSystem system;

    OnFireSystemTest() {
        ShotFixtures.setSize(creature, WIDTH, HEIGHT);
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getCreature" -> (short) args[0] == CREATURE_ID ? creature : null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        GameEventService gameEventService = (GameEventService) Proxy.newProxyInstance(GameEventService.class.getClassLoader(), new Class<?>[]{GameEventService.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("emitEffect")) {
                        assertEquals(FIRE_FLAME_EFFECT_ID, args[0]);
                        flames.add((Vector3f) args[1]);
                    }
                    return null;
                });
        system = new OnFireSystem(entityData, kwdFile, () -> gameTime, gameEventService);
    }

    @Test
    void burningCreatureGivesOffThreeFlamesEveryTickAroundItself() {
        Vector3f position = new Vector3f(10, 1, 10);
        burningCreature(position, 2.5);

        system.processTick(0.25f);
        gameTime = 0.25;
        system.processTick(0.25f);

        assertEquals(6, flames.size());
        for (Vector3f flame : flames) {
            assertTrue(Math.abs(flame.x - position.x) <= WIDTH / 2, flame.toString());
            assertTrue(Math.abs(flame.z - position.z) <= WIDTH / 2, flame.toString());
            assertTrue(flame.y >= position.y && flame.y <= position.y + HEIGHT, flame.toString());
        }
    }

    @Test
    void fireGoesOutWhenItsTimeIsUp() {
        EntityId burning = burningCreature(new Vector3f(10, 1, 10), 2.5);
        gameTime = 2.5;

        system.processTick(0.25f);

        assertNull(entityData.getComponent(burning, OnFire.class));
        assertTrue(flames.isEmpty());
    }

    private EntityId burningCreature(Vector3f position, double endTime) {
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = CREATURE_ID;
        creatureComponent.height = HEIGHT;
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new Position(0, position), creatureComponent, new OnFire(endTime));
        return entity;
    }

}
