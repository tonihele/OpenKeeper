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

import com.simsilica.es.EntityId;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClaimLairTest {

    private static final EntityId BED = new EntityId(2);

    @Test
    void hurtCreatureGoesToBedOnceItHasMadeItsHome() {
        assertEquals(BED, claimLair(49));
    }

    @Test
    void creatureAtTheSleepThresholdStaysAwake() {
        assertNull(claimLair(50));
    }

    /**
     * Makes a home for a creature at the given health in a level that sends
     * creatures below 50 % health to sleep.
     *
     * @return the bed the creature was sent to, or {@code null} if it wasn't
     */
    private static EntityId claimLair(int healthPercentage) {
        AtomicReference<EntityId> lair = new AtomicReference<>();
        AtomicReference<EntityId> sentToBed = new AtomicReference<>();
        IRoomObjectControl<?> bedControl = proxy(IRoomObjectControl.class, (method, args) -> BED);
        IRoomController room = proxy(IRoomController.class, (method, args) -> bedControl);
        ICreatureController creature = proxy(ICreatureController.class, (method, args) -> switch (method) {
            case "getCreature" -> CreatureSleepFixtures.sleeper(36);
            case "getEntityId" -> new EntityId(1);
            case "getHealthPercentage" -> healthPercentage;
            case "setCreatureLair" -> {
                lair.set((EntityId) args[0]);
                yield null;
            }
            case "goToSleepAfterLairDrop" -> {
                sentToBed.set(lair.get());
                yield true;
            }
            default -> throw new UnsupportedOperationException(method);
        });

        ClaimLair task = new ClaimLair(null, null, new Point(1, 1), (short) 3, room, null,
                Map.of(MiscType.CREATURE_SLEEPS_WHEN_BELOW_PERCENT_HEALTH,
                        CreatureSleepFixtures.variable(MiscType.CREATURE_SLEEPS_WHEN_BELOW_PERCENT_HEALTH, 50)));
        task.executeTask(creature, 0);

        assertEquals(BED, lair.get());
        return sentToBed.get();
    }

    private interface Handler {

        Object invoke(String method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> handler.invoke(method.getName(), args));
    }
}
