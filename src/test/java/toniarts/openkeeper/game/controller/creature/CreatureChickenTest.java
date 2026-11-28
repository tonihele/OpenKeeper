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

import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A creature under the Chicken spell does nothing but walk to its keeper's
 * Hatchery, or step about when there is none, until the spell wears off
 * (DKII.EXE 0x463BE0)
 */
@SuppressWarnings("unchecked")
class CreatureChickenTest {

    private static final double NOW = 100;

    private double now = NOW;

    private final List<IRoomController> hatcheries = new ArrayList<>();
    private final Set<Point> unreachable = new HashSet<>();
    private final Set<Point> solid = new HashSet<>();
    private final Set<Point> water = new HashSet<>();
    private final Set<Point> lava = new HashSet<>();
    private EntityData entityData;
    private EntityId entity;
    private CreatureController controller;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        entity = entityData.createEntity();
        entityData.setComponent(entity, new Owner((short) 3, (short) 3));
        controller = controller(creature());
    }

    @Test
    void workingCreatureTurnsIntoChickenForTheDuration() {
        controller.getStateMachine().setInitialState(CreatureState.WORK);

        controller.turnIntoChicken(60);

        CreatureAi creatureAi = entityData.getComponent(entity, CreatureAi.class);
        assertEquals(CreatureState.CHICKEN, creatureAi.getCreatureState());
        assertEquals(NOW, creatureAi.stateStartTime, 0.0001);
        assertEquals(60, creatureAi.stateDuration, 0.0001);
        assertEquals(CreatureState.CHICKEN, controller.getStateMachine().getCurrentState());
    }

    @Test
    void immuneCreatureStaysAsItIs() {
        CreatureController immuneController = controller(creature(Creature.CreatureFlag.IS_IMMUNE_TO_CHICKEN));
        immuneController.getStateMachine().setInitialState(CreatureState.WORK);

        immuneController.turnIntoChicken(60);

        assertEquals(CreatureState.WORK, immuneController.getStateMachine().getCurrentState());
    }

    @Test
    void prisonerStaysInPrison() {
        controller.getStateMachine().setInitialState(CreatureState.IMPRISONED);

        controller.turnIntoChicken(60);

        assertEquals(CreatureState.IMPRISONED, controller.getStateMachine().getCurrentState());
    }

    @Test
    void possessedCreatureStaysPossessed() {
        controller.getStateMachine().setInitialState(CreatureState.IDLE);
        entityData.setComponent(entity, new Possessed(0, NOW));

        controller.turnIntoChicken(60);

        assertEquals(CreatureState.IDLE, controller.getStateMachine().getCurrentState());
    }

    @Test
    void chickenWandersUntilTheSpellWearsOff() {
        AtomicBoolean timeUp = new AtomicBoolean();
        AtomicBoolean wandered = new AtomicBoolean();
        AtomicReference<CreatureState> state = new AtomicReference<>(CreatureState.CHICKEN);
        ICreatureController creature = creature(timeUp, wandered, state);

        CreatureState.CHICKEN.update(creature);
        assertTrue(wandered.get());
        assertEquals(CreatureState.CHICKEN, state.get());

        timeUp.set(true);
        CreatureState.CHICKEN.update(creature);
        assertEquals(CreatureState.IDLE, state.get());
    }

    @Test
    void chickenLastsForTheDuration() {
        controller.getStateMachine().setInitialState(CreatureState.WORK);
        controller.turnIntoChicken(60);

        now = NOW + 59.9;
        assertFalse(controller.isStateTimeExceeded());

        now = NOW + 60.1;
        assertTrue(controller.isStateTimeExceeded());
    }

    @Test
    void chickenWalksToTheNearestHatcheryItCanGetTo() {
        entityData.setComponent(entity, new Position(0, WorldUtils.pointToVector3f(1, 1)));
        IRoomController far = hatchery(20, 22);
        IRoomController near = hatchery(5, 7);
        hatcheries.addAll(List.of(far, near));

        controller.navigateAsChicken();
        assertTrue(near.getRoomInstance().hasCoordinate(entityData.getComponent(entity, Navigation.class).target));

        // The nearest is cut off
        unreachable.addAll(near.getRoomInstance().getCoordinates());
        controller.navigateAsChicken();
        assertTrue(far.getRoomInstance().hasCoordinate(entityData.getComponent(entity, Navigation.class).target));
    }

    @Test
    void chickenWithoutAHatcheryStepsToANeighbouringTileThatIsNotSolidWaterOrLava() {
        entityData.setComponent(entity, new Position(0, WorldUtils.pointToVector3f(4, 4)));
        solid.add(new Point(5, 4));
        water.add(new Point(3, 4));
        lava.add(new Point(4, 5));

        controller.navigateAsChicken();
        assertEquals(new Point(4, 3), entityData.getComponent(entity, Navigation.class).target);

        // Boxed in it stays
        entityData.removeComponent(entity, Navigation.class);
        solid.add(new Point(4, 3));
        controller.navigateAsChicken();
        assertNull(entityData.getComponent(entity, Navigation.class));
    }

    /**
     * A one tile high Hatchery from x {@code fromX} to {@code toX}
     */
    private static IRoomController hatchery(int fromX, int toX) {
        RoomInstance instance = new RoomInstance(new Room());
        for (int x = fromX; x <= toX; x++) {
            instance.addCoordinate(new Point(x, 1));
        }
        return (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(),
                new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getRoomInstance" -> instance;
                    case "isTileAccessible" -> true;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Creature creature(Creature.CreatureFlag... flags) {
        EnumSet<Creature.CreatureFlag> set = EnumSet.noneOf(Creature.CreatureFlag.class);
        set.addAll(List.of(flags));
        return ShotFixtures.creature((short) 1, set, EnumSet.noneOf(Creature.CreatureFlag3.class));
    }

    private CreatureController controller(Creature creature) {
        ILevelInfo levelInfo = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                new Class<?>[]{ILevelInfo.class}, (proxy, method, args) -> null);
        FakeMapData mapData = new FakeMapData(30, 9);
        IMapController mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getMapData" -> mapData;
                    case "getRoomsByFunction" -> hatcheries;
                    case "isSolid" -> solid.contains((Point) args[0]);
                    case "isWater" -> water.contains((Point) args[0]);
                    case "isLava" -> lava.contains((Point) args[0]);
                    default -> defaultValue(method.getReturnType());
                });
        INavigationService navigationService = (INavigationService) Proxy.newProxyInstance(INavigationService.class.getClassLoader(),
                new Class<?>[]{INavigationService.class}, (proxy, method, args) -> method.getName().equals("findPath")
                && !unreachable.contains((Point) args[1]) ? new DefaultGraphPath<>() : defaultValue(method.getReturnType()));
        return new CreatureController(entity, entityData, creature, navigationService, null, () -> now, Map.of(), null, null, mapController, levelInfo, null, null, null);
    }

    private static ICreatureController creature(AtomicBoolean timeUp, AtomicBoolean wandered, AtomicReference<CreatureState> state) {
        StateMachine<ICreatureController, CreatureState> stateMachine = (StateMachine<ICreatureController, CreatureState>) Proxy.newProxyInstance(
                StateMachine.class.getClassLoader(), new Class<?>[]{StateMachine.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "changeState" -> {
                        state.set((CreatureState) args[0]);
                        yield null;
                    }
                    case "getCurrentState" -> state.get();
                    default -> defaultValue(method.getReturnType());
                });
        return (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isStateTimeExceeded" -> timeUp.get();
                    case "isStopped" -> true;
                    case "navigateAsChicken" -> {
                        wandered.set(true);
                        yield null;
                    }
                    case "getStateMachine" -> stateMachine;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }

}
