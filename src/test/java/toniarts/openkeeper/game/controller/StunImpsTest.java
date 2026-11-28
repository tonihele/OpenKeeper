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
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.tools.convert.map.CreatureGoldFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The Stun Imps special. DK II (0x4A4870) stuns every creature with the
 * worker flag of the other players, not only the Imps.
 */
class StunImpsTest {

    private static final short KEEPER = 3;
    private static final short OTHER_KEEPER = 4;

    private final EntityData entityData = new DefaultEntityData();
    private final CreaturesController controller = new CreaturesController(kwdFile(), entityData,
            Collections.emptyMap(), () -> 0, stub(IGameController.class), stub(IMapController.class), null);

    @Test
    void everyWorkerOfThePlayerIsStunnedAndNothingElse() {
        ICreatureController imp = creature(1, true, KEEPER);
        ICreatureController dwarf = creature(7, true, KEEPER);
        ICreatureController knight = creature(22, false, KEEPER);
        ICreatureController otherImp = creature(1, true, OTHER_KEEPER);
        entityData.setComponent(dwarf.getEntityId(), new Navigation(null, null, null));

        controller.stunImps(KEEPER);

        assertEquals(CreatureState.STUNNED, imp.getStateMachine().getCurrentState());
        assertEquals(CreatureState.STUNNED, dwarf.getStateMachine().getCurrentState());
        assertNull(entityData.getComponent(dwarf.getEntityId(), Navigation.class));
        assertNotEquals(CreatureState.STUNNED, knight.getStateMachine().getCurrentState());
        assertNotEquals(CreatureState.STUNNED, otherImp.getStateMachine().getCurrentState());
    }

    @Test
    void workerInTheHandIsLeftAlone() {
        ICreatureController imp = creature(1, true, KEEPER);
        entityData.setComponent(imp.getEntityId(), new InHand());

        controller.stunImps(KEEPER);

        assertNotEquals(CreatureState.STUNNED, imp.getStateMachine().getCurrentState());
    }

    private ICreatureController creature(int creatureId, boolean worker, short ownerId) {
        EntityId entity = entityData.createEntity();
        CreatureComponent component = new CreatureComponent();
        component.creatureId = (short) creatureId;
        component.worker = worker;
        entityData.setComponents(entity, component, new Owner(ownerId, ownerId), new Health(100, 100),
                new CreatureAi(0, CreatureState.IDLE, (short) creatureId),
                new Position(0, new Vector3f(5, WorldUtils.FLOOR_HEIGHT, 5)));

        // The controller is kept by a weak reference, the test holds on to it
        return controller.createController(entity);
    }

    private static IKwdFile kwdFile() {
        return (IKwdFile) Proxy.newProxyInstance(StunImpsTest.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getCreature" ->
                CreatureGoldFixtures.goldCreature((Short) args[0], 0, (short) 0);
            case "getThings" ->
                List.of();
            default ->
                defaultValue(method.getReturnType());
        });
    }

    private static <T> T stub(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(StunImpsTest.class.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> method.getReturnType().isInterface()
                ? stub(method.getReturnType()) : defaultValue(method.getReturnType())));
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == double.class) {
            return 0d;
        }
        if (type == float.class) {
            return 0f;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        return null;
    }
}
