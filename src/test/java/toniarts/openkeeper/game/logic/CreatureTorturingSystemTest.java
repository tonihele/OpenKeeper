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
package toniarts.openkeeper.game.logic;

import com.badlogic.gdx.ai.fsm.StateMachine;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureFixtures;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What happens to the victims in a torture chamber, like in DK II
 */
class CreatureTorturingSystemTest {

    private static final short KEEPER_ID = 3;
    private static final short HERO_ID = 1;
    private static final Point CHAMBER_TILE = new Point(4, 4);
    private static final short TROLL = 10;
    private static final short MISTRESS = 39;
    private static final short UNIQUE_MISTRESS = 38;
    private static final short IMP = 1;
    private static final short TORTURE_WHEEL = 8;
    private static final short ELECTRIC_CHAIR = 61;

    private final EntityData entityData = new DefaultEntityData();
    private final List<EntityId> leftDevice = new ArrayList<>();
    private final Map<EntityId, Short> converted = new HashMap<>();
    private final Map<EntityId, CreatureState> states = new HashMap<>();
    private final RoomInstance chamberInstance = new RoomInstance(new Room());
    private final Map<Short, Creature> creatures = Map.of(
            TROLL, CreatureFixtures.creature(TROLL, (short) 1092, false),
            UNIQUE_MISTRESS, CreatureFixtures.creature(UNIQUE_MISTRESS, (short) -1000, false),
            MISTRESS, CreatureFixtures.creature(MISTRESS, (short) 0, false),
            IMP, CreatureFixtures.creature(IMP, (short) 1092, true));
    private final List<Object> playerEvents = new ArrayList<>();

    /**
     * The sounds of the victims, with the tick they were sent on
     */
    private final List<List<Object>> soundEvents = new ArrayList<>();
    private int tick;
    private CreatureTorturingSystem system = new CreatureTorturingSystem(entityData, creaturesController(), mapController(),
            (GameEventService) Proxy.newProxyInstance(GameEventService.class.getClassLoader(),
                    new Class<?>[]{GameEventService.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "emitToPlayer" -> {
                            playerEvents.add(List.of(args));
                            yield null;
                        }
                        case "emit", "emitAt" -> {
                            soundEvents.add(List.of(tick, method.getName(), args[0], args[args.length == 2 ? 1 : 3]));
                            yield null;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    }));

    @Test
    void ownVictimGetsAngrierEverySecond() {
        EntityId victim = victim(KEEPER_ID, 1000, TROLL);

        tick(3);

        assertEquals(3 * 1092, entityData.getComponent(victim, CreatureMood.class).get(CreatureMood.REASON_OTHER));
    }

    @Test
    void ownUniqueMistressEnjoysItDownToZero() {
        EntityId victim = victim(KEEPER_ID, 1000, UNIQUE_MISTRESS);
        entityData.setComponent(victim, new CreatureMood(0, 0, 0, 0, 0, 1500));

        tick(1);
        assertEquals(500, entityData.getComponent(victim, CreatureMood.class).get(CreatureMood.REASON_OTHER));

        tick(2);
        assertEquals(0, entityData.getComponent(victim, CreatureMood.class).get(CreatureMood.REASON_OTHER));
    }

    @Test
    void ownMistressFeelsNothing() {
        EntityId victim = victim(KEEPER_ID, 1000, MISTRESS);
        entityData.setComponent(victim, new CreatureMood(0, 0, 0, 0, 0, 5000));

        tick(3);

        assertEquals(5000, entityData.getComponent(victim, CreatureMood.class).get(CreatureMood.REASON_OTHER));
    }

    @Test
    void ownVictimsAngerStopsAtTheCap() {
        EntityId victim = victim(KEEPER_ID, 1000, TROLL);
        entityData.setComponent(victim, new CreatureMood(0, 0, 0, 0, 0, CreatureMood.MAX_ANGER - 1000));

        tick(2);

        assertEquals(CreatureMood.MAX_ANGER, entityData.getComponent(victim, CreatureMood.class).get(CreatureMood.REASON_OTHER));
    }

    @Test
    void emotionlessVictimFeelsNothing() {
        EntityId victim = victim(KEEPER_ID, 1000, IMP);

        tick(3);

        assertEquals(0, entityData.getComponent(victim, CreatureMood.class).get(CreatureMood.REASON_OTHER));
    }

    @Test
    void enemyVictimsMoodIsNotChanged() {
        EntityId victim = victim(HERO_ID, 1000, TROLL);

        tick(3);

        assertEquals(0, entityData.getComponent(victim, CreatureMood.class).get(CreatureMood.REASON_OTHER));
    }

    @Test
    void convertedEnemyLeavesTheDevice() {
        EntityId victim = victim(HERO_ID, 2);

        tick(3);

        assertEquals(KEEPER_ID, converted.get(victim));
        assertEquals(List.of(victim), leftDevice);
    }

    @Test
    void mentorTellsBothKeepersOfTheConversion() {
        victim(HERO_ID, 2);

        tick(3);

        assertEquals(List.of(List.of(GameEventType.TORTURE_CONVERTED_BY_ENEMY, HERO_ID),
                List.of(GameEventType.TORTURE_CONVERTED, KEEPER_ID)), playerEvents);
    }

    @Test
    void electricChairZapsOnTheTickTheVictimTurnsIntoItsSkeleton() {
        EntityId victim = victimOn(ELECTRIC_CHAIR);
        CreatureViewSystem view = new CreatureViewSystem(entityData);
        List<Integer> shocks = new ArrayList<>();

        // The systems run in this order in the game loop
        for (tick = 0; tick < 12 * 4; tick++) {
            CreatureViewState before = entityData.getComponent(victim, CreatureViewState.class);
            system.processTick(0.25f);
            view.processTick(0.25f);
            CreatureViewState after = entityData.getComponent(victim, CreatureViewState.class);
            if (after.state == Creature.AnimationType.TORTURED_CHAIR_SKELETON && before.state != after.state) {
                shocks.add(tick);
            }
        }

        assertEquals(List.of(0, 6 * 4), shocks);
        assertEquals(List.of(List.of(0, "emitAt", GameEventType.TORTURE_ELECTROCUTED, victim),
                List.of(6 * 4, "emitAt", GameEventType.TORTURE_ELECTROCUTED, victim)), soundEvents);
    }

    @Test
    void victimOnTheTortureWheelCriesOutOnEveryTick() {
        EntityId victim = victimOn(TORTURE_WHEEL);

        for (tick = 0; tick < 4; tick++) {
            system.processTick(0.25f);
        }

        assertEquals(List.of(List.of(0, "emit", GameEventType.TORTURE_WHEEL, victim),
                List.of(1, "emit", GameEventType.TORTURE_WHEEL, victim),
                List.of(2, "emit", GameEventType.TORTURE_WHEEL, victim),
                List.of(3, "emit", GameEventType.TORTURE_WHEEL, victim)), soundEvents);
    }

    @Test
    void deadVictimLeavesTheDevice() {
        EntityId victim = victim(HERO_ID, 100);
        tick(1);

        entityData.removeComponent(victim, CreatureTortured.class);
        entityData.setComponent(victim, new Death(1));
        tick(1);

        assertEquals(List.of(victim), leftDevice);
        assertTrue(states.isEmpty());
    }

    @Test
    void victimMovedToAnotherRoomIsNotSetFree() {
        EntityId victim = victim(HERO_ID, 100);
        tick(1);

        // Picked up and dropped somewhere else between two ticks
        entityData.removeComponent(victim, CreatureTortured.class);
        tick(1);

        assertEquals(List.of(victim), leftDevice);
        assertTrue(states.isEmpty());
    }

    @Test
    void victimOfADestroyedChamberIsSetFree() {
        EntityId victim = victim(HERO_ID, 100);
        tick(1);

        // The chamber sets its victims free when it is sold or destroyed
        chamberInstance.setDestroyed(true);
        entityData.removeComponent(victim, CreatureTortured.class);
        tick(1);

        assertEquals(CreatureState.IDLE, states.get(victim));
        assertEquals(HERO_ID, entityData.getComponent(victim, Owner.class).controlId);
    }

    @Test
    void victimOfAChamberDestroyedBeforeItsFirstCheckIsSetFree() {
        EntityId victim = victim(HERO_ID, 100);
        entityData.setComponent(victim, new CreatureTortured(0, 100, 0));
        tick(1);

        chamberInstance.setDestroyed(true);
        entityData.removeComponent(victim, CreatureTortured.class);
        tick(1);

        assertEquals(CreatureState.IDLE, states.get(victim));
    }

    @Test
    void pickedUpVictimIsNotSetFree() {
        EntityId victim = victim(HERO_ID, 100);
        tick(1);

        entityData.removeComponent(victim, CreatureTortured.class);
        entityData.setComponent(victim, new InHand());
        tick(1);

        assertTrue(states.isEmpty());
    }

    @Test
    void ownCreatureIsNotConverted() {
        EntityId victim = victim(KEEPER_ID, 2);

        tick(3);

        assertTrue(converted.isEmpty());
        assertTrue(leftDevice.isEmpty());
        assertTrue(playerEvents.isEmpty());
    }

    private void tick(int seconds) {
        for (int i = 0; i < seconds * 4; i++) {
            system.processTick(0.25f);
        }
    }

    private EntityId victimOn(short deviceObjectId) {
        EntityId device = entityData.createEntity();
        entityData.setComponent(device, new ObjectComponent(deviceObjectId, ObjectType.TORTUREE));
        EntityId victim = victim(HERO_ID, 100);
        entityData.setComponents(victim, new CreatureTortured(0, -1, 0, device),
                new CreatureViewState((short) 1, 0, Creature.AnimationType.WALK),
                new CreatureAi(0, CreatureState.TORTURED, (short) 1));
        return victim;
    }

    private EntityId victim(short ownerId, float tortureTimeToConvert) {
        return victim(ownerId, tortureTimeToConvert, TROLL);
    }

    private EntityId victim(short ownerId, float tortureTimeToConvert, short creatureId) {
        EntityId victim = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.tortureTimeToConvert = tortureTimeToConvert;
        creatureComponent.creatureId = creatureId;
        entityData.setComponent(victim, new CreatureMood());
        entityData.setComponent(victim, creatureComponent);
        entityData.setComponent(victim, new Owner(ownerId, KEEPER_ID));
        entityData.setComponent(victim, new Position(0, WorldUtils.pointToVector3f(CHAMBER_TILE)));
        entityData.setComponent(victim, new CreatureTortured(0, -1, 0));
        return victim;
    }

    private ICreaturesController creaturesController() {
        return (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "createController" -> creature((EntityId) args[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private ICreatureController creature(EntityId entityId) {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "convertCreature" -> {
                        converted.put(entityId, (Short) args[0]);
                        yield null;
                    }
                    case "getStateMachine" -> stateMachine(entityId);
                    case "getCreature" -> creatures.get(entityData.getComponent(entityId, CreatureComponent.class).creatureId);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private StateMachine<?, ?> stateMachine(EntityId entityId) {
        return (StateMachine<?, ?>) Proxy.newProxyInstance(StateMachine.class.getClassLoader(),
                new Class<?>[]{StateMachine.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "changeState" -> {
                        states.put(entityId, (CreatureState) args[0]);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private IMapController mapController() {
        chamberInstance.setOwnerId(KEEPER_ID);
        IRoomObjectControl<?> torturees = (IRoomObjectControl<?>) Proxy.newProxyInstance(IRoomObjectControl.class.getClassLoader(),
                new Class<?>[]{IRoomObjectControl.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "removeItem" -> {
                        leftDevice.add((EntityId) args[0]);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        IRoomController chamber = (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(),
                new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getRoomInstance" -> chamberInstance;
                    case "hasObjectControl" -> args[0] == ObjectType.TORTUREE;
                    case "getObjectControl" -> torturees;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getRoomControllerByCoordinates" -> CHAMBER_TILE.equals(args[0]) ? chamber : null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

}
