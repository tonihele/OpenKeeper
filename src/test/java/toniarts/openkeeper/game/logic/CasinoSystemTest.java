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

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CasinoBigWinner;
import toniarts.openkeeper.game.component.CasinoDisco;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.CasinoVariableFixtures;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The Casino's jackpot party: the big winner celebrates, its Casino goes into
 * disco mode for 90 seconds or until nobody is left in it, and the keeper's
 * creatures get angrier if the big winner doesn't collect its winnings in time
 */
class CasinoSystemTest {

    private static final short KEEPER = 3;
    private static final short ENEMY = 4;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<MiscType, MiscVariable> gameSettings = new EnumMap<>(MiscType.class);
    private final Map<EntityId, StateMachine<ICreatureController, CreatureState>> stateMachines = new HashMap<>();
    private final List<EntityId> creatures = new ArrayList<>();
    private final RoomInstance roomInstance = new RoomInstance(new Room());
    private final EntityId casinoId = entityData.createEntity();
    private double gameTime = 50;
    private CasinoSystem casinoSystem;

    @BeforeEach
    void setUp() {
        gameSettings.put(MiscType.GAMBLING_JACKPOT_PAY_TIME_LIMIT_MISSED_ANGER_PERCENTAGE_MODIFIER,
                CasinoVariableFixtures.miscVariable(MiscType.GAMBLING_JACKPOT_PAY_TIME_LIMIT_MISSED_ANGER_PERCENTAGE_MODIFIER, 100));
        for (int x = 5; x <= 7; x++) {
            for (int y = 5; y <= 7; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }
        entityData.setComponent(casinoId, new Owner(KEEPER, KEEPER));
        casinoSystem = new CasinoSystem(entityData, creaturesController(), mapController(), entitiesByTile(), () -> gameTime, gameSettings);
    }

    @Test
    void theBigWinnerCelebratesAndItsCasinoGoesIntoDiscoMode() {
        EntityId bigWinner = creature(KEEPER, new Point(6, 6));
        entityData.setComponent(bigWinner, new CasinoBigWinner(240, 1000, 0));

        casinoSystem.processTick(0.25f);

        assertEquals(CreatureState.CELEBRATING, stateMachines.get(bigWinner).getCurrentState());
        assertEquals(50, entityData.getComponent(casinoId, CasinoDisco.class).startTime);
        assertEquals(239.75, entityData.getComponent(bigWinner, CasinoBigWinner.class).timeLeft);
    }

    @Test
    void theDiscoIsOverAfter90Seconds() {
        creature(KEEPER, new Point(6, 6));
        entityData.setComponent(casinoId, new CasinoDisco(50));

        gameTime = 140;
        casinoSystem.processTick(0.25f);
        assertNotNull(entityData.getComponent(casinoId, CasinoDisco.class));

        gameTime = 140.25;
        casinoSystem.processTick(0.25f);
        assertNull(entityData.getComponent(casinoId, CasinoDisco.class));
    }

    @Test
    void theDiscoIsOverWhenNobodyIsLeftInTheCasino() {
        EntityId dancer = creature(KEEPER, new Point(6, 6));
        entityData.setComponent(casinoId, new CasinoDisco(50));

        casinoSystem.processTick(0.25f);
        assertNotNull(entityData.getComponent(casinoId, CasinoDisco.class));

        entityData.setComponent(dancer, new Position(0, WorldUtils.pointToVector3f(10, 10)));
        casinoSystem.processTick(0.25f);
        assertNull(entityData.getComponent(casinoId, CasinoDisco.class));
    }

    @Test
    void theKeepersCreaturesGetAngrierWhenTheBigWinnerDoesNotCollectInTime() {
        EntityId bigWinner = creature(KEEPER, new Point(6, 6));
        entityData.setComponent(bigWinner, new CasinoBigWinner(0.5, 1000, 0));
        EntityId otherCreature = creature(KEEPER, new Point(10, 10));
        EntityId enemyCreature = creature(ENEMY, new Point(10, 10));

        casinoSystem.processTick(0.25f);
        assertEquals(1000, entityData.getComponent(otherCreature, CreatureMood.class).other);

        casinoSystem.processTick(0.25f);
        assertNull(entityData.getComponent(bigWinner, CasinoBigWinner.class));
        assertEquals(2000, entityData.getComponent(bigWinner, CreatureMood.class).other);
        assertEquals(2000, entityData.getComponent(otherCreature, CreatureMood.class).other);
        assertEquals(1000, entityData.getComponent(enemyCreature, CreatureMood.class).other);
    }

    private EntityId creature(short ownerId, Point tile) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponents(entityId, new CreatureComponent(), new Owner(ownerId, ownerId),
                new CreatureMood(0, 0, 0, 0, 0, 1000), new Position(0, WorldUtils.pointToVector3f(tile)));
        creatures.add(entityId);
        return entityId;
    }

    private ICreaturesController creaturesController() {
        return (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(), new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> {
            if (!"createController".equals(method.getName())) {
                throw new UnsupportedOperationException(method.getName());
            }
            StateMachine<ICreatureController, CreatureState> stateMachine = stateMachines.computeIfAbsent((EntityId) args[0], id -> new DefaultStateMachine<>());
            return Proxy.newProxyInstance(ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (creatureProxy, creatureMethod, creatureArgs) -> {
                if ("getStateMachine".equals(creatureMethod.getName())) {
                    return stateMachine;
                }
                throw new UnsupportedOperationException(creatureMethod.getName());
            });
        });
    }

    private IMapController mapController() {
        IRoomController casino = (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "getEntityId" ->
                    casinoId;
                case "getRoomInstance" ->
                    roomInstance;
                case "hasObjectControl" ->
                    true;
                default ->
                    throw new UnsupportedOperationException(method.getName());
            };
        });
        return (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(), new Class<?>[]{IMapController.class}, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "getRoomControllerByCoordinates" ->
                    roomInstance.hasCoordinate((Point) args[0]) ? casino : null;
                case "getRoomController" ->
                    casinoId.equals(args[0]) ? casino : null;
                default ->
                    throw new UnsupportedOperationException(method.getName());
            };
        });
    }

    /**
     * Stands in for the position system: the entities on a tile
     */
    private IEntityPositionLookup entitiesByTile() {
        return (IEntityPositionLookup) Proxy.newProxyInstance(IEntityPositionLookup.class.getClassLoader(), new Class<?>[]{IEntityPositionLookup.class}, (proxy, method, args) -> {
            if (!"getEntitiesInLocation".equals(method.getName()) || !(args[0] instanceof Point tile)) {
                throw new UnsupportedOperationException(method.getName());
            }
            List<EntityId> result = new ArrayList<>();
            for (EntityId entityId : creatures) {
                if (tile.equals(WorldUtils.vectorToPoint(entityData.getComponent(entityId, Position.class).position))) {
                    result.add(entityId);
                }
            }
            return result;
        });
    }

}
