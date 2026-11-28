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

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.simsilica.event.EventBus;
import com.simsilica.event.EventListener;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.event.KeeperAttacked;
import toniarts.openkeeper.game.event.ServerEvents;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A creature that starts a fight tells its keeper, as DK II's fight state
 * does on its first tick (0x45EB20). DK II swings and casts within that
 * state, so a creature back from a swing or a spell is no news
 */
class CreatureFightStartTest {

    private static final Point TILE = new Point(5, 6);

    private final List<KeeperAttacked> told = new ArrayList<>();
    private final EventListener<KeeperAttacked> listener = (type, event) -> told.add(event);
    private StateMachine<ICreatureController, CreatureState> stateMachine;

    @BeforeEach
    void listen() {
        EventBus.addListener(ServerEvents.KEEPER_ATTACKED, listener);
    }

    @AfterEach
    void stopListening() {
        EventBus.removeListener(ServerEvents.KEEPER_ATTACKED, listener);
    }

    @Test
    void aFightThatStartsIsNewsAndASwingOrASpellIsNot() {
        ICreatureController creature = (ICreatureController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getOwnerId" -> Player.KEEPER1_ID;
            case "getCreatureCoordinates" -> TILE;
            case "getStateMachine" -> stateMachine;
            default -> null;
        });
        stateMachine = new DefaultStateMachine<>(creature, CreatureState.IDLE);

        stateMachine.changeState(CreatureState.FIGHT);
        stateMachine.changeState(CreatureState.MELEE_ATTACK);
        stateMachine.changeState(CreatureState.FIGHT);
        stateMachine.changeState(CreatureState.CAST_SPELL);
        stateMachine.changeState(CreatureState.FIGHT);

        assertEquals(List.of(new KeeperAttacked(Player.KEEPER1_ID, KeeperAttacked.Target.CREATURE, TILE)), told);

        // Backing off leaves the fight, coming back to it is news again
        stateMachine.changeState(CreatureState.BACK_OFF);
        stateMachine.changeState(CreatureState.FIGHT);

        assertEquals(2, told.size());
    }

}
