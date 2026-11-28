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
package toniarts.openkeeper.game.trigger.creature;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.trigger.AbstractThingTriggerControl;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Trigger;
import toniarts.openkeeper.tools.convert.map.TriggerFixtures;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatureTriggerControlTest {

    private static final short TRIGGER_PLAYER_ID = 5;
    private static final short OTHER_PLAYER_ID = 3;

    @Test
    @SuppressWarnings("unchecked")
    void convertedEventHoldsForCreatureOfAnotherOwner() throws Exception {
        CreatureTriggerControl control = control();
        Field eventStates = CreatureTriggerControl.class.getDeclaredField("eventStates");
        eventStates.setAccessible(true);
        Predicate<ICreatureController> converted = ((Map<TriggerGeneric.TargetType, Predicate<ICreatureController>>) eventStates.get(control))
                .get(TriggerGeneric.TargetType.CREATURE_CONVERTED);

        assertTrue(converted.test(creature(false, OTHER_PLAYER_ID)));
        assertFalse(converted.test(creature(false, TRIGGER_PLAYER_ID)));
    }

    @Test
    void polledEventsIgnoreCreatureThatIsGone() throws Exception {

        // A dead creature has no Owner component, asking for its owner would throw
        pollEvents(control(), creature(true, (short) -1));
    }

    private CreatureTriggerControl control() {
        Map<Integer, Trigger> triggers = Map.of(1, TriggerFixtures.generic(1, TriggerGeneric.TargetType.CREATURE_CONVERTED));
        IKwdFile kwdFile = proxy(IKwdFile.class, (method, args) -> method.getName().equals("getTriggers") ? triggers : fail(method));
        ILevelInfo levelInfo = proxy(ILevelInfo.class, (method, args) -> method.getName().equals("getLevelData") ? kwdFile : fail(method));
        IGameController gameController = proxy(IGameController.class, (method, args) -> null);
        return new CreatureTriggerControl(gameController, levelInfo, null, null, null, 1, TRIGGER_PLAYER_ID, null, null, false);
    }

    private static void pollEvents(CreatureTriggerControl control, ICreatureController creature) throws Exception {
        Method setThing = AbstractThingTriggerControl.class.getDeclaredMethod("setThing", Object.class);
        setThing.setAccessible(true);
        setThing.invoke(control, creature);
        Method pollEvents = CreatureTriggerControl.class.getDeclaredMethod("pollEvents");
        pollEvents.setAccessible(true);
        pollEvents.invoke(control);
    }

    private static ICreatureController creature(boolean dead, short ownerId) {
        return proxy(ICreatureController.class, (method, args) -> switch (method.getName()) {
            case "isDead" ->
                dead;
            case "getOwnerId" ->
                dead ? failOwner() : ownerId;
            default ->
                fail(method);
        });
    }

    private static short failOwner() {
        throw new NullPointerException("owner is null");
    }

    private static Object fail(Method method) {
        throw new UnsupportedOperationException(method.getName());
    }

    private interface Handler {

        Object handle(Method method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> handler.handle(method, args));
    }
}
