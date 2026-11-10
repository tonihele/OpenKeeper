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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.RoomPrisonerControl;
import toniarts.openkeeper.tools.convert.map.CasinoVariableFixtures;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A Prison takes in prisoners over its safe capacity, and each one taken in
 * over it risks a jail break that frees them all
 */
class PrisonOvercrowdingSystemTest {

    private static final short CAPTOR = 1;
    private static final short HERO = 2;
    private static final short ENEMY_KEEPER = 3;
    private static final short CREATURE_ID = 7;
    private static final int SAFE_CAPACITY = 2;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<MiscType, MiscVariable> gameSettings = new EnumMap<>(MiscType.class);
    private final RoomInstance roomInstance = new RoomInstance(new Room());
    private final EntityId prisonId = entityData.createEntity();
    private IRoomController roomController;
    private RoomPrisonerControl prison;
    private int nextTile;

    @BeforeEach
    void setUp() {
        for (int x = 0; x < 6; x++) {
            roomInstance.addCoordinate(new Point(x, 0));
        }
        prison = createPrison();
    }

    @Test
    void prisonersUpToTheSafeCapacityAreSafe() {
        PrisonOvercrowdingSystem system = createSystem(100);
        EntityId first = imprison(HERO);
        EntityId second = imprison(ENEMY_KEEPER);

        system.processTick(0.25f);

        assertImprisoned(first);
        assertImprisoned(second);
    }

    @Test
    void aPrisonerOverTheSafeCapacityCanFreeThemAll() {
        PrisonOvercrowdingSystem system = createSystem(100);
        EntityId hero = imprison(HERO);
        EntityId enemy = imprison(ENEMY_KEEPER);
        system.processTick(0.25f);

        EntityId extra = imprison(HERO);
        system.processTick(0.25f);

        assertFree(hero, HERO);
        assertFree(enemy, ENEMY_KEEPER);
        assertFree(extra, HERO);
        assertEquals(0, prison.getCurrentCapacity());
    }

    @Test
    void noJailBreakWhenTheChanceIsZero() {
        PrisonOvercrowdingSystem system = createSystem(0);
        EntityId first = imprison(HERO);
        imprison(HERO);
        imprison(HERO);
        EntityId last = imprison(HERO);

        system.processTick(0.25f);

        assertImprisoned(first);
        assertImprisoned(last);
        assertEquals(4, prison.getCurrentCapacity());
    }

    @Test
    void theChanceGrowsWithEachPrisonerOverTheSafeCapacity() {
        EntityId first = imprison(HERO);
        imprison(HERO);
        imprison(HERO);
        PrisonOvercrowdingSystem system = createSystem(50);

        // Two over the safe capacity, at 50 % each
        EntityId last = imprison(HERO);
        system.processTick(0.25f);

        assertFree(first, HERO);
        assertFree(last, HERO);
    }

    @Test
    void prisonersAlreadyHeldDoNotRollAgain() {
        EntityId first = imprison(HERO);
        imprison(HERO);
        EntityId third = imprison(HERO);
        PrisonOvercrowdingSystem system = createSystem(100);

        system.processTick(0.25f);

        assertImprisoned(first);
        assertImprisoned(third);
    }

    private void assertImprisoned(EntityId prisoner) {
        assertNotNull(entityData.getComponent(prisoner, CreatureImprisoned.class));
        assertNotNull(entityData.getComponent(prisoner, Stored.class));
        assertEquals(CAPTOR, entityData.getComponent(prisoner, Owner.class).controlId);
    }

    private void assertFree(EntityId prisoner, short ownerId) {
        assertNull(entityData.getComponent(prisoner, CreatureImprisoned.class));
        assertNull(entityData.getComponent(prisoner, Stored.class));
        assertEquals(ownerId, entityData.getComponent(prisoner, Owner.class).controlId);
        assertEquals(CreatureState.IDLE, entityData.getComponent(prisoner, CreatureAi.class).getCreatureState());
    }

    private EntityId imprison(short ownerId) {
        EntityId prisoner = entityData.createEntity();
        entityData.setComponent(prisoner, new Owner(ownerId, CAPTOR));
        entityData.setComponent(prisoner, new CreatureAi(0, CreatureState.IMPRISONED, CREATURE_ID));
        prison.addItem(prisoner, new Point(nextTile++, 0));
        entityData.setComponent(prisoner, new CreatureImprisoned(0, 0));
        return prisoner;
    }

    private PrisonOvercrowdingSystem createSystem(int jailBreakPercentage) {
        gameSettings.put(MiscType.OVERCROWDING_JAIL_BREAK_PERCENTAGE_PER_EXTRA_CREATURE,
                CasinoVariableFixtures.miscVariable(MiscType.OVERCROWDING_JAIL_BREAK_PERCENTAGE_PER_EXTRA_CREATURE, jailBreakPercentage));
        return new PrisonOvercrowdingSystem(entityData, mapController(), gameSettings);
    }

    private RoomPrisonerControl createPrison() {
        IRoomController room = (IRoomController) Proxy.newProxyInstance(
                IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityId" -> prisonId;
                    case "getOwnerId" -> CAPTOR;
                    case "getRoomInstance" -> roomInstance;
                    case "getObjectControl" -> args[0] == ObjectType.PRISONER ? prison : null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        roomController = room;
        return new RoomPrisonerControl(null, room, entityData, () -> 10) {
            @Override
            protected Collection<Point> getCoordinates() {
                return roomInstance.getCoordinates();
            }

            @Override
            protected int getNumberOfAccessibleTiles() {
                return SAFE_CAPACITY;
            }
        };
    }

    private IMapController mapController() {
        return (IMapController) Proxy.newProxyInstance(
                IMapController.class.getClassLoader(), new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getRoomController" -> args[0].equals(prisonId) ? roomController : null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
