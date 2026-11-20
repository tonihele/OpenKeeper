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
package toniarts.openkeeper.game.trigger.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;

import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.game.trigger.TriggerActionData;
import toniarts.openkeeper.game.trigger.TriggerLoader;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.PresentationActionFixtures;
import toniarts.openkeeper.tools.convert.map.Trigger;

/**
 * Who sees the speeches, camera moves and messages of a trigger tree. The
 * trees of heroes and neutral creatures belong to the Good and the Neutral
 * player, which nobody plays, so these have to reach the local human keeper.
 */
class PlayerTriggerControlAudienceTest {

    private static final int TRIGGER_ID = 10;

    private final List<String> calls = new ArrayList<>();


    @Test
    void speechInAHeroTreeReachesTheLocalKeeper() {
        run(Player.GOOD_PLAYER_ID, PresentationActionFixtures.playSpeech(TRIGGER_ID, 55));

        assertEquals(List.of("playSpeech:" + Player.KEEPER1_ID), calls);
    }

    @Test
    void speechInANeutralCreatureTreeReachesTheLocalKeeper() {
        run(Player.NEUTRAL_PLAYER_ID, PresentationActionFixtures.playSpeech(TRIGGER_ID, 55));

        assertEquals(List.of("playSpeech:" + Player.KEEPER1_ID), calls);
    }

    @Test
    void widescreenAndMessagesInAHeroTreeReachTheLocalKeeper() {
        run(Player.GOOD_PLAYER_ID, PresentationActionFixtures.setWidescreen(TRIGGER_ID, true));
        run(Player.GOOD_PLAYER_ID, PresentationActionFixtures.displayText(TRIGGER_ID, 7));

        assertEquals(List.of("setWidescreen:" + Player.KEEPER1_ID, "showMessage:" + Player.KEEPER1_ID), calls);
    }

    @Test
    void speechInAKeepersOwnTreeStaysWithThatKeeper() {
        run(Player.KEEPER1_ID, PresentationActionFixtures.playSpeech(TRIGGER_ID, 55));

        assertEquals(List.of("playSpeech:" + Player.KEEPER1_ID), calls);
    }

    @Test
    void speechInAnotherKeepersTreeIsNotSentToTheLocalPlayer() {
        run(Player.KEEPER2_ID, PresentationActionFixtures.playSpeech(TRIGGER_ID, 55));

        assertEquals(List.of("playSpeech:" + Player.KEEPER2_ID), calls);
    }


    private void run(short treeOwner, Trigger action) {
        Map<Integer, Trigger> triggers = new HashMap<>();
        triggers.put(TRIGGER_ID, action);
        IKwdFile kwdFile = stub(IKwdFile.class, (method, args) -> method.getName().equals("getTriggers") ? triggers : null);
        ILevelInfo levelInfo = stub(ILevelInfo.class, (method, args) -> method.getName().equals("getLevelData") ? kwdFile : null);
        PlayerService playerService = stub(PlayerService.class, (method, args) -> {
            calls.add(method.getName() + ":" + args[args.length - 1]);
            return null;
        });
        PlayerTriggerControl control = new PlayerTriggerControl(stub(IGameController.class, (m, a) -> null), levelInfo,
                stub(IGameTimer.class, (m, a) -> null), stub(IMapController.class, (m, a) -> null),
                stub(ICreaturesController.class, (m, a) -> null), TRIGGER_ID, treeOwner, playerService);

        control.doAction((TriggerActionData) new TriggerLoader(kwdFile).load(TRIGGER_ID).getChild(0));
    }

    private interface Answer {

        Object answer(Method method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Object result = answer.answer(method, args);
            if (result == null && method.getReturnType().isPrimitive()) {
                return method.getReturnType() == boolean.class ? (Object) false : (Object) 0;
            }
            return result;
        });
    }
}
