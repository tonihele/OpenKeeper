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

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.creature.IPartyController;
import toniarts.openkeeper.game.task.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A party's dig task goes to the party's workers that can still work
 */
class ObjectiveTaskDecoratorTest {

    @Test
    void deadWorkerIsLeftOutOfThePartyTask() {
        List<ICreatureController> members = new ArrayList<>();
        IPartyController party = stub(IPartyController.class, (name, args) -> "getActualMembers".equals(name) ? members : null);
        ICreatureController leader = creature(party, false);

        // The creature entity of a dead dwarf is gone, asking it anything else fails
        ICreatureController dead = stub(ICreatureController.class, (name, args) -> {
            if ("isIncapacitated".equals(name)) {
                return true;
            }
            throw new NullPointerException(name);
        });
        ICreatureController alive = creature(party, true);
        members.add(leader);
        members.add(dead);
        members.add(alive);
        List<ICreatureController> assigned = new ArrayList<>();
        Task dig = stub(Task.class, (name, args) -> switch (name) {
            case "canAssign" -> true;
            case "assign" -> assigned.add((ICreatureController) args[0]);
            default -> null;
        });

        new ObjectiveTaskDecorator(1, dig) {

            @Override
            public boolean isWorkerPartyTask() {
                return true;
            }

        }.assign(leader, true);

        assertEquals(List.of(leader, alive), assigned);
    }

    private static ICreatureController creature(IPartyController party, boolean worker) {
        return stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getParty" -> party;
            case "isWorker" -> worker;
            default -> null;
        });
    }

    @FunctionalInterface
    private interface Answer {

        Object answer(String name, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "hashCode" -> {
                    return System.identityHashCode(proxy);
                }
                case "equals" -> {
                    return proxy == args[0];
                }
                default -> {
                }
            }
            Object result = answer.answer(method.getName(), args);
            if (result == null && method.getReturnType() == boolean.class) {
                return false;
            }
            return result;
        });
    }
}
