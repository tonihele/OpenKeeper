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
package toniarts.openkeeper.game.controller.creature;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.task.worker.FetchObjectTask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A worker picking up a loose gold pile takes only what it can carry and
 * leaves the rest in the same pile. A full worker isn't sent to fetch gold, and
 * keeps its gold when it has nowhere to take it
 */
class CreaturePicksUpGoldTest {

    private static final short PLAYER_ID = 3;

    private final EntityData entityData = new DefaultEntityData();
    private boolean pileRemoved;

    @Test
    void workerLeavesWhatItCantCarryInThePile() {
        CreatureController imp = worker(100, 300);
        EntityId pile = pile(2000);

        imp.giveObject(goldObject(pile));

        assertEquals(300, imp.getGold());
        assertEquals(1800, entityData.getComponent(pile, Gold.class).gold);
        assertFalse(pileRemoved);
    }

    @Test
    void workerTakesAWholePileThatFits() {
        CreatureController imp = worker(100, 300);
        EntityId pile = pile(150);

        imp.giveObject(goldObject(pile));

        assertEquals(250, imp.getGold());
        assertNull(entityData.getComponent(pile, Gold.class));
        assertTrue(pileRemoved);
    }

    @Test
    void fullWorkerIsNotSentToFetchGold() {
        FetchObjectTask task = new FetchObjectTask(null, null, treasuryWithSpace(), goldObject(pile(2000)), PLAYER_ID);

        assertTrue(task.isValid(stub(ICreatureController.class, Map.of("isTooMuchGold", false))));
        assertFalse(task.isValid(stub(ICreatureController.class, Map.of("isTooMuchGold", true))));
    }

    @Test
    void fullWorkerWithNowhereToTakeItsGoldKeepsIt() {
        List<String> calls = new ArrayList<>();
        ICreatureController imp = (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> {
            calls.add(method.getName());
            return switch (method.getName()) {
                case "isAssignedTaskValid", "isAtAssignedTaskTarget", "isTooMuchGold" -> true;
                case "shouldFleeOrAttack", "dropGoldToTreasury" -> false;
                default -> null;
            };
        });

        CreatureState.WORK.update(imp);

        assertTrue(calls.contains("dropGoldToTreasury"));
        assertFalse(calls.contains("dropGold"));
    }

    private CreatureController worker(int gold, int maxGold) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new Gold(gold, maxGold));

        // Leaving gold on the floor as a new pile would call the objects controller
        return new CreatureController(entityId, entityData, null, null, null, null, Map.of(), null, null, null, null,
                stub(IObjectsController.class, Map.of()), null, null);
    }

    private EntityId pile(int gold) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new Gold(gold, 2000));
        return entityId;
    }

    private IObjectController goldObject(EntityId pile) {
        return (IObjectController) Proxy.newProxyInstance(IObjectController.class.getClassLoader(),
                new Class<?>[]{IObjectController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getType" -> ObjectType.GOLD;
            case "getEntityId" -> pile;
            case "getPosition" -> new Vector3f();
            case "isPickableByPlayerCreature" -> true;
            case "isRemoved" -> pileRemoved;
            case "isStoredInRoom", "isDragged" -> false;
            case "remove" -> {
                pileRemoved = true;
                yield null;
            }
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private static IMapController treasuryWithSpace() {
        IRoomController treasury = stub(IRoomController.class, Map.of("isFullCapacity", false));
        return stub(IMapController.class, Map.of("getRoomsByFunction", List.of(treasury)));
    }

    /**
     * Stands in for an interface, answering the given methods by name
     */
    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Map<String, Object> answers) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (!answers.containsKey(method.getName())) {
                throw new UnsupportedOperationException(method.getName());
            }
            return answers.get(method.getName());
        });
    }

}
