/*
 * Copyright (C) 2026 OpenKeeper
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package toniarts.openkeeper.tools.convert.map;

import com.jme3.network.HostedConnection;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.network.game.GameClientService;
import toniarts.openkeeper.game.network.game.GameHostedService;
import toniarts.openkeeper.game.state.lobby.ClientInfo;
import toniarts.openkeeper.game.state.session.GameSession;
import toniarts.openkeeper.game.state.session.GameSessionServiceListener;
import toniarts.openkeeper.game.state.session.LocalGameSession;
import toniarts.openkeeper.game.trigger.player.PlayerTriggerLogicController;

import static org.junit.jupiter.api.Assertions.*;

class GuiButtonPressedTriggerTest {

    @ParameterizedTest
    @ValueSource(ints = {0,1,2,3,4,5})
    void exactCategoryAndIdMatchesOnlyOnSimulationTick(int category) {
        Fixture f = new Fixture(category,7,255);
        f.press(3,category,7);
        assertEquals(0,f.flag(3));
        f.tick();
        assertEquals(1,f.flag(3));
        assertEquals(0,f.flag(4));
        f.tick();
        assertEquals(1,f.flag(3),"Polling must not replay the click");
    }

    @ParameterizedTest
    @CsvSource({"4,7", "1,6", "1,0", "0,7"})
    void mismatchingButtonDoesNotMatch(int category, int id) {
        Fixture f = new Fixture(1,7,255);
        f.press(3,category,id);
        f.tick();
        assertEquals(0,f.flag(3));
    }

    @Test
    void zeroIdIsExactRatherThanWildcard() {
        Fixture f = new Fixture(1,0,255);
        f.press(3,1,7);
        f.tick();
        assertEquals(0,f.flag(3));
        f.press(3,1,0);
        f.tick();
        assertEquals(1,f.flag(3));
    }

    @Test
    void noHistoricalStateOrImplicitPollingActivation() {
        Fixture f = new Fixture(1,7,255);
        for (int i=0;i<10;i++) f.tick();
        assertEquals(0,f.flag(3));
        f.press(3,1,7);
        f.tick();
        for (int i=0;i<10;i++) f.tick();
        assertEquals(1,f.flag(3));
    }

    @ParameterizedTest
    @ValueSource(ints = {0,1,12345,-1})
    void ignoredValueAndComparisonDoNotFilterTheEvent(int value) {
        Fixture f = new Fixture(1,7,255);
        f.event.setUserData("value",value);
        f.event.setTargetValueComparison(TriggerGeneric.ComparisonType.NOT_EQUAL_TO);
        f.load();
        f.press(3,1,7);
        f.tick();
        assertEquals(1,f.flag(3));
    }

    @ParameterizedTest
    @CsvSource({"1,1", "2,2", "255,4"})
    void usesExistingRepeatLimits(int repeats, int expected) {
        Fixture f = new Fixture(1,7,repeats);
        for (int i=0;i<4;i++) f.press(3,1,7);
        f.tick();
        assertEquals(expected,f.flag(3));
    }

    @Test
    void deliversAllMatchingRootsAndTheirActionsInTheSameTick() {
        Fixture f = new Fixture(1,7,255);
        TriggerGeneric other = f.button(2,1,7,255,-2);
        f.event.setIdNext(2);
        f.triggers.put(2,other);
        f.triggers.put(-2,f.increment(-2,3));
        f.load();
        f.press(3,1,7);
        f.tick();
        assertEquals(2,f.flag(3));
    }

    @Test
    void nestedConditionsObserveDispatchTimeAndDoNotRememberARejectedClick() {
        Fixture f = new Fixture(1,7,255);
        TriggerGeneric condition = f.flagCondition(2,0,1,-1);
        f.event.setIdChild(2);
        f.triggers.put(2,condition);
        f.load();
        f.press(3,1,7);
        f.tick();
        assertEquals(0,f.flag(3));
        f.flags.put(0,1);
        f.tick();
        assertEquals(0,f.flag(3));
        f.press(3,1,7);
        f.tick();
        assertEquals(1,f.flag(3));
    }

    @Test
    void anEventBelowAnOrdinaryConditionIsNotDispatched() {
        Fixture f = new Fixture(1,7,255);
        TriggerGeneric condition = f.flagCondition(2,0,0,1);
        f.triggers.put(2,condition);
        f.first=2;
        f.load();
        f.tick(); // ordinary traversal is now below the enclosing condition
        f.press(3,1,7);
        f.tick();
        f.tick();
        assertEquals(0,f.flag(3));
    }

    @Test
    void dispatchDoesNotMoveAnUnrelatedOrdinaryTraversalCursor() {
        Fixture f = new Fixture(1,7,255);
        TriggerGeneric condition = f.flagCondition(2,0,0,-2);
        f.event.setIdNext(2);
        f.triggers.put(2,condition);
        f.triggers.put(-2,f.increment(-2,9));
        f.load();
        f.tick(); // both players enter the ordinary condition
        f.press(3,1,7);
        f.tick(); // normal action still runs alongside the button event
        assertEquals(1,f.flag(3));
        assertEquals(2,f.flag(9));
    }

    @ParameterizedTest
    @CsvSource({"-1,7", "6,7", "256,7", "1,-1", "1,256", "1,32767"})
    void malformedNetworkOperandsAreNotTruncatedToValidButtonIds(int category, int id) {
        Fixture f = new Fixture(1,7,255);
        f.press(3,category,id);
        f.tick();
        assertEquals(0,f.flag(3));
    }

    @Test
    void unknownPlayerIsIgnoredAndNoEventLeaksToAnotherPlayer() {
        Fixture f = new Fixture(1,7,255);
        f.press(99,1,7);
        f.press(4,1,7);
        f.tick();
        assertEquals(0,f.flag(3));
        assertEquals(1,f.flag(4));
    }

    @Test
    void sessionThreadOnlyEnqueuesAndSimulationThreadOwnsEvaluation() throws InterruptedException {
        Fixture f = new Fixture(1,7,255);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread client = new Thread(() -> {
            try { for (int i=0;i<20;i++) f.press(3,1,7); }
            catch (Throwable t) { failure.set(t); }
        },"synthetic-client-session");
        client.start(); client.join();
        assertNull(failure.get());
        assertEquals(0,f.flag(3));
        f.tick();
        assertEquals(20,f.flag(3));
    }

    @Test
    void stoppingDiscardsUndeliveredInput() {
        Fixture f = new Fixture(1,7,255);
        f.press(3,1,7);
        f.logic.stop();
        f.tick();
        assertEquals(0,f.flag(3));
    }

    @Test
    void realLocalSessionDeliversThroughTheSameServerQueue() {
        Fixture f = new Fixture(1,7,255);
        LocalGameSession local = new LocalGameSession();
        try {
            local.addGameSessionServiceListener(f.listener());
            local.guiButtonPressed((short)1,(short)7);
            assertEquals(0,f.flag(3));
            f.tick();
            assertEquals(1,f.flag(3));
            assertEquals(0,f.flag(4));
        } finally {
            local.getServerEntityData().close();
        }
    }

    @Test
    void realNetworkClientAndHostedSessionDerivePlayerFromConnection() throws Exception {
        Fixture f = new Fixture(1,7,255);
        GameHostedService hosted = new GameHostedService();
        hosted.addGameSessionServiceListener(f.listener());
        ClientInfo client = new ClientInfo();
        client.setKeeper(new Keeper(false,(short)4));
        // Instantiate the real per-connection endpoint without starting sockets.
        Class<?> endpoint = Class.forName(GameHostedService.class.getName()+"$GameSessionImpl");
        Constructor<?> constructor = endpoint.getDeclaredConstructor(GameHostedService.class,
                HostedConnection.class,ClientInfo.class);
        constructor.setAccessible(true);
        GameSession session = (GameSession)constructor.newInstance(hosted,null,client);
        GameClientService remote = new GameClientService();
        Field delegate = GameClientService.class.getDeclaredField("delegate");
        delegate.setAccessible(true);
        delegate.set(remote,session);
        remote.guiButtonPressed((short)1,(short)7);
        assertEquals(0,f.flag(3));
        assertEquals(0,f.flag(4));
        f.tick();
        assertEquals(0,f.flag(3),"Client cannot choose another player as receiver");
        assertEquals(1,f.flag(4));
    }

    private static final class Fixture {
        final Map<Integer,Trigger> triggers = new HashMap<>();
        final Map<Integer,Trigger> loadedTriggers = new HashMap<>();
        final Map<Integer,Integer> flags = new HashMap<>();
        final Map<Short,Player> players = new HashMap<>();
        final Map<Short,Keeper> keepers = Map.of((short)3,new Keeper(false,(short)3),
                (short)4,new Keeper(false,(short)4));
        final Thread simulation = Thread.currentThread();
        final TriggerGeneric event;
        final IKwdFile kwd;
        final ILevelInfo level;
        PlayerTriggerLogicController logic;
        int first=1;

        Fixture(int category, int id, int repeats) {
            for (short playerId : new short[]{3,4}) {
                Player player = new Player();
                player.setPlayerId(playerId);
                player.setTriggerId(1);
                players.put(playerId,player);
            }
            kwd = proxy(IKwdFile.class,(name,args) -> switch(name) {
                case "getTriggers" -> loadedTriggers;
                case "getPlayers" -> players;
                default -> throw new UnsupportedOperationException(name);
            });
            level = proxy(ILevelInfo.class,(name,args) -> {
                assertSame(simulation,Thread.currentThread(),"Only the simulation may access script state");
                return switch(name) {
                    case "getLevelData" -> kwd;
                    case "getPlayers" -> keepers;
                    case "getFlag" -> flags.getOrDefault(((Number)args[0]).intValue(),0);
                    case "setFlag" -> {flags.put(((Number)args[0]).intValue(),(int)args[1]);yield null;}
                    default -> throw new UnsupportedOperationException(name);
                };
            });
            event=button(1,category,id,repeats,-1);
            triggers.put(1,event);
            triggers.put(-1,increment(-1,3));
            load();
        }

        TriggerGeneric button(int id,int category,int button,int repeats,int child) {
            TriggerGeneric event = new TriggerGeneric(kwd);
            event.setId(id);
            event.setType(TriggerGeneric.TargetType.GUI_BUTTON_PRESSED);
            event.setTargetValueComparison(TriggerGeneric.ComparisonType.NONE);
            event.setRepeatTimes((short)repeats);
            event.setIdChild(child);
            event.setUserData("targetType",(short)category);
            event.setUserData("targetId",(short)button);
            event.setUserData("value",0);
            return event;
        }

        TriggerGeneric flagCondition(int id,int flag,int value,int child) {
            TriggerGeneric condition = new TriggerGeneric(kwd);
            condition.setId(id);
            condition.setType(TriggerGeneric.TargetType.FLAG);
            condition.setTargetValueComparison(TriggerGeneric.ComparisonType.EQUAL_TO);
            condition.setRepeatTimes((short)255);
            condition.setIdChild(child);
            condition.setUserData("targetId",(short)flag);
            condition.setUserData("flag",(short)1);
            condition.setUserData("value",value);
            return condition;
        }

        TriggerAction increment(int id,int flag) {
            TriggerAction action = new TriggerAction(kwd);
            action.setId(id);
            action.setType(TriggerAction.ActionType.FLAG);
            action.setUserData("flagId",(short)flag);
            action.setUserData("flag",(short)18);
            action.setUserData("value",1);
            return action;
        }

        void load() {
            loadedTriggers.clear();
            loadedTriggers.putAll(triggers);
            // Give each player's independent tree a distinct action destination,
            // while retaining intentionally shared flags used by nested conditions.
            for (Trigger source : triggers.values()) {
                Trigger copy;
                if (source instanceof TriggerGeneric generic) {
                    TriggerGeneric target = new TriggerGeneric(kwd);
                    target.setType(generic.getType());
                    target.setTargetValueComparison(generic.getTargetValueComparison());
                    copy=target;
                } else {
                    TriggerAction target = new TriggerAction(kwd);
                    target.setType(((TriggerAction)source).getType());
                    copy=target;
                }
                copy.setId(otherId(source.getId()));
                copy.setIdNext(otherId(source.getIdNext()));
                copy.setIdChild(otherId(source.getIdChild()));
                copy.setRepeatTimes(source.getRepeatTimes());
                for (String key : source.getUserDataKeys()) copy.setUserData(key,source.getUserData(key));
                if (copy instanceof TriggerAction action && action.getType()==TriggerAction.ActionType.FLAG
                        && copy.<Short>getUserData("flagId")==3) {
                    copy.setUserData("flagId",(short)4);
                }
                loadedTriggers.put(copy.getId(),copy);
            }
            players.get((short)3).setTriggerId(first);
            players.get((short)4).setTriggerId(otherId(first));
            logic = new PlayerTriggerLogicController(null,level,null,null,null,null);
        }

        private int otherId(int id) {return id==0 ? 0 : id>0 ? id+1000 : id-1000;}

        void press(int player,int category,int id) {
            logic.onGuiButtonPressed((short)player,(short)category,(short)id);
        }
        void tick() {logic.processTick(.25f);}
        int flag(int id) {return flags.getOrDefault(id,0);}
        GameSessionServiceListener listener() {
            return proxy(GameSessionServiceListener.class,(name,args) -> {
                if (!name.equals("onGuiButtonPressed")) throw new UnsupportedOperationException(name);
                logic.onGuiButtonPressed((short)args[2],(short)args[0],(short)args[1]);
                return null;
            });
        }
    }

    @FunctionalInterface private interface Call {Object invoke(String name,Object[] args);}
    private static <T>T proxy(Class<T> type,Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},
                (p,m,a)->call.invoke(m.getName(),a)));
    }
}
