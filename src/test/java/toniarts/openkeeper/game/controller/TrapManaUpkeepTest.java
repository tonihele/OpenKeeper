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
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.TrapFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * An armed trap costs its owner mana every second
 */
class TrapManaUpkeepTest {

    private static final short KEEPER = 3;
    private static final short SENTRY = 3;
    private static final short GUARD_POST = 11;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Integer, Trap> traps = new HashMap<>();
    private final TrapsController trapsController;

    TrapManaUpkeepTest() {

        // The mana usage of the original data: the Sentry Trap 5, the Guard Post none
        traps.put((int) SENTRY, TrapFixtures.trapWithManaUsage(SENTRY, 5));
        traps.put((int) GUARD_POST, TrapFixtures.trapWithManaUsage(GUARD_POST, 0));
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getThings" ->
                Collections.emptyList();
            case "getTrapById" ->
                traps.get(((Number) args[0]).intValue());
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
        trapsController = new TrapsController(kwdFile, entityData, Collections.emptyMap(), null, null);
    }

    @Test
    void blueprintCostsNoMana() {
        EntityId blueprint = trapsController.addTrapBlueprint(SENTRY, new Point(10, 10), KEEPER);

        assertNull(entityData.getComponent(blueprint, Mana.class));
    }

    @Test
    void armedTrapCostsItsManaUsage() {
        EntityId trap = trapsController.addTrapBlueprint(SENTRY, new Point(10, 10), KEEPER);

        trapsController.armTrap(trap);

        assertEquals(-5, entityData.getComponent(trap, Mana.class).manaGeneration);
    }

    @Test
    void trapWithoutManaUsageCostsNothing() {
        EntityId trap = trapsController.addTrapBlueprint(GUARD_POST, new Point(10, 10), KEEPER);

        trapsController.armTrap(trap);

        assertNull(entityData.getComponent(trap, Mana.class));
    }

}
