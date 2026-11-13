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
package toniarts.openkeeper.game.task.objective;

import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.StolenGold;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.creature.IPartyController;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertSame;

class PartyObjectiveActorSelectorTest {

    @Test
    void interruptedCarrierIsChosenAgainBeforeASecondRaider() {
        var entityData = new DefaultEntityData();
        EntityId leaderId = entityData.createEntity();
        EntityId carrierId = entityData.createEntity();
        List<ICreatureController> members = new ArrayList<>();
        IPartyController party = stub(IPartyController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getActualMembers" -> members;
            default -> null;
        });
        ICreatureController leader = creature(leaderId, party);
        ICreatureController carrier = creature(carrierId, party);
        members.add(leader);
        members.add(carrier);
        entityData.setComponent(carrierId, new StolenGold(new Point(2, 2)));

        assertSame(carrier, new PartyObjectiveActorSelector(entityData).selectStealGoldActor(leader));
    }

    private static ICreatureController creature(EntityId entityId, IPartyController party) {
        return stub(ICreatureController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getEntityId" -> entityId;
            case "getParty" -> party;
            case "isRemoved", "isDead", "isIncapacitated" -> false;
            default -> null;
        });
    }

    @FunctionalInterface
    private interface Answer {

        Object answer(Object proxy, java.lang.reflect.Method method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getName().equals("equals")) {
                return proxy == args[0];
            }
            if (method.getName().equals("hashCode")) {
                return System.identityHashCode(proxy);
            }
            Object result = answer.answer(proxy, method, args);
            if (result == null && method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            if (result == null && method.getReturnType() == boolean.class) {
                return false;
            }
            return result;
        });
    }
}
