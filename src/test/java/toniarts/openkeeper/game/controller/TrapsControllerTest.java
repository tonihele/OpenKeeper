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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.TrapFiringFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrapsControllerTest {

    private static final short KEEPER = 3;
    private static final short GAS = 1;
    private static final short SENTRY = 3;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Integer, Trap> traps = new HashMap<>();
    private final TrapsController trapsController;

    TrapsControllerTest() {
        traps.put((int) GAS, TrapFiringFixtures.trap(GAS, Trap.TriggerType.PRESSURE, 1, 4, (short) 58));
        traps.get((int) GAS).getFlags().add(Trap.TrapFlag.INVISIBLE);
        traps.put((int) SENTRY, TrapFiringFixtures.trap(SENTRY, Trap.TriggerType.LINE_OF_SIGHT, 5, 6, (short) 44));
        trapsController = new TrapsController(kwdFile(), entityData, Collections.emptyMap(), null, null);
    }

    @Test
    void invisibleTrapIsHiddenFromTheStart() {
        EntityId blueprint = trapsController.addTrapBlueprint(GAS, new Point(10, 10), KEEPER);

        assertTrue(entityData.getComponent(blueprint, TrapViewState.class).hidden);
    }

    @Test
    void armedInvisibleTrapStaysHidden() {
        EntityId blueprint = trapsController.addTrapBlueprint(GAS, new Point(10, 10), KEEPER);

        trapsController.armTrap(blueprint);

        TrapViewState viewState = entityData.getComponent(blueprint, TrapViewState.class);
        assertFalse(viewState.blueprint);
        assertTrue(viewState.hidden);
    }

    @Test
    void otherTrapsAreNotHidden() {
        EntityId blueprint = trapsController.addTrapBlueprint(SENTRY, new Point(10, 10), KEEPER);

        assertFalse(entityData.getComponent(blueprint, TrapViewState.class).hidden);
    }

    private IKwdFile kwdFile() {
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getThings" ->
                Collections.emptyList();
            case "getTrapById" ->
                traps.get(((Number) args[0]).intValue());
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
    }

}
