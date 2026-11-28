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

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.Fearless;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.PrisonEscape;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.PrisonController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The jail break as DK II does it: the hero walks to the door of the Prison
 * and sets its friends off from there (DKII.EXE 0x473C60, 0x4E2E70), and each
 * of them walks to the tile of the cage below the door, onto the door, and is
 * free once on it (0x4739E0)
 */
class JailBreakTest {

    private static final short GOOD = 1;
    private static final short KEEPER = 4;
    private static final short OTHER_KEEPER = 5;
    private static final Point DOOR = new Point(2, 1);
    private static final Point BELOW_DOOR = new Point(2, 2);

    private final EntityData entityData = new DefaultEntityData();
    private final Map<EntityId, CreatureController> controllers = new HashMap<>();
    private final PrisonController prison = createPrison();

    @Test
    void theDoorIsAboveTheFirstTileOfTheCage() {
        assertEquals(DOOR, prison.getDoor());
    }

    @Test
    void heroWalksToTheDoorOfThePrison() {
        CreatureController hero = createCreature(GOOD, GOOD, new Point(5, 5), CreatureState.JAIL_BREAK);
        EntityId friend = imprison(GOOD, new Point(3, 3));

        hero.freePrisoners();

        assertEquals(DOOR, entityData.getComponent(hero.getEntityId(), Navigation.class).target);
        assertEquals(CreatureState.JAIL_BREAK, hero.getStateMachine().getCurrentState());
        assertNull(entityData.getComponent(friend, PrisonEscape.class));
    }

    @Test
    void heroAtTheDoorSetsItsFriendsOff() {
        CreatureController hero = createCreature(GOOD, GOOD, DOOR, CreatureState.JAIL_BREAK);
        EntityId first = imprison(GOOD, new Point(3, 3));
        EntityId enemy = imprison(OTHER_KEEPER, new Point(4, 3));
        EntityId second = imprison(GOOD, new Point(4, 4));
        EntityId gone = imprison(GOOD, new Point(2, 4));
        entityData.removeComponent(gone, CreatureImprisoned.class);

        hero.freePrisoners();

        assertNotNull(entityData.getComponent(first, PrisonEscape.class));
        assertNotNull(entityData.getComponent(second, PrisonEscape.class));
        assertNull(entityData.getComponent(enemy, PrisonEscape.class));
        assertNull(entityData.getComponent(gone, PrisonEscape.class));
        assertNotNull(entityData.getComponent(first, CreatureImprisoned.class)); // not free before the door
        assertFalse(hero.getStateMachine().isInState(CreatureState.JAIL_BREAK));
    }

    @Test
    void heroOutsideAPrisonGivesUp() {
        CreatureController hero = createCreature(GOOD, GOOD, new Point(9, 9), CreatureState.JAIL_BREAK);

        hero.freePrisoners();

        assertNull(entityData.getComponent(hero.getEntityId(), Navigation.class));
        assertFalse(hero.getStateMachine().isInState(CreatureState.JAIL_BREAK));
    }

    @Test
    void prisonerWalksToTheTileBelowTheDoorThenOntoTheDoor() {
        EntityId prisoner = imprison(GOOD, new Point(4, 4));
        CreatureController controller = controllers.get(prisoner);

        // Not before it is let out
        CreatureState.IMPRISONED.update(controller);
        assertNull(entityData.getComponent(prisoner, Navigation.class));

        entityData.setComponent(prisoner, new PrisonEscape());
        CreatureState.IMPRISONED.update(controller);
        assertEquals(BELOW_DOOR, entityData.getComponent(prisoner, Navigation.class).target);

        // There
        entityData.removeComponent(prisoner, Navigation.class);
        entityData.setComponent(prisoner, new Position(0, new Vector3f(BELOW_DOOR.x, 0, BELOW_DOOR.y)));
        CreatureState.IMPRISONED.update(controller);
        assertEquals(DOOR, entityData.getComponent(prisoner, Navigation.class).target);
        assertNotNull(entityData.getComponent(prisoner, CreatureImprisoned.class));
        assertEquals(CreatureState.IMPRISONED, controller.getStateMachine().getCurrentState());
    }

    @Test
    void prisonerIsFreeOnceOnTheDoor() {
        EntityId prisoner = imprison(GOOD, BELOW_DOOR);
        CreatureController controller = controllers.get(prisoner);
        entityData.setComponent(prisoner, new PrisonEscape());
        entityData.setComponent(prisoner, new Position(0, new Vector3f(DOOR.x, 0, DOOR.y)));

        CreatureState.IMPRISONED.update(controller);

        assertNull(entityData.getComponent(prisoner, CreatureImprisoned.class));
        assertNull(entityData.getComponent(prisoner, PrisonEscape.class));
        assertNull(entityData.getComponent(prisoner, Stored.class));
        assertEquals(GOOD, entityData.getComponent(prisoner, Owner.class).controlId);
        assertFalse(prison.getObjectControl(ObjectType.PRISONER).getItems(BELOW_DOOR).contains(prisoner));
        assertFalse(controller.getStateMachine().isInState(CreatureState.IMPRISONED));
    }

    private EntityId imprison(short owner, Point tile) {
        CreatureController prisoner = createCreature(owner, KEEPER, tile, CreatureState.IMPRISONED);
        entityData.setComponent(prisoner.getEntityId(), new CreatureImprisoned(0, 0));
        prison.getObjectControl(ObjectType.PRISONER).addItem(prisoner.getEntityId(), tile);
        return prisoner.getEntityId();
    }

    private CreatureController createCreature(short owner, short control, Point tile, CreatureState state) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new Owner(owner, control), new Position(0, new Vector3f(tile.x, 0, tile.y)),
                new CreatureAi(0, state, (short) 1), new CreatureComponent(), new Fearless());
        Creature creature = ShotFixtures.creature((short) 1, EnumSet.noneOf(Creature.CreatureFlag.class),
                EnumSet.noneOf(Creature.CreatureFlag3.class));
        CreatureController controller = new CreatureController(entity, entityData, creature,
                stub(INavigationService.class, (name, args) -> "findPath".equals(name) ? new DefaultGraphPath<>() : null),
                stub(ITaskManager.class, (name, args) -> null), () -> 0, Map.of(),
                stub(ICreaturesController.class, (name, args) -> "createController".equals(name) ? controllers.get((EntityId) args[0]) : null),
                stub(IEntityPositionLookup.class, (name, args) -> "getSensedEntities".equals(name) ? Set.of() : null),
                stub(IMapController.class, (name, args) -> "getRoomControllerByCoordinates".equals(name)
                        && prison.getRoomInstance().hasCoordinate((Point) args[0]) ? prison : null),
                levelInfo(), null, null, null);
        controller.getStateMachine().setInitialState(state);
        controllers.put(entity, controller);
        return controller;
    }

    private PrisonController createPrison() {
        IObjectsController objectsController = stub(IObjectsController.class,
                (name, args) -> "loadObject".equals(name) ? entityData.createEntity() : null);

        // A 5 x 5 Prison at 1, 1 with the cage of 3 x 3 in its middle
        RoomInstance roomInstance = new RoomInstance(RoomFixtures.room((short) 11,
                List.of((short) 49, (short) 50, (short) 51, (short) 0, (short) 0, (short) 0)));
        roomInstance.setOwnerId(KEEPER);
        for (int y = 1; y <= 5; y++) {
            for (int x = 1; x <= 5; x++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }
        PrisonController room = new PrisonController(entityData.createEntity(), entityData, null, roomInstance,
                objectsController, () -> 0);
        room.construct();
        return room;
    }

    private static ILevelInfo levelInfo() {
        Keeper good = new Keeper(false, GOOD);
        Keeper keeper = new Keeper(false, KEEPER);
        Keeper other = new Keeper(false, OTHER_KEEPER);
        keeper.createAlliance(OTHER_KEEPER);
        return stub(ILevelInfo.class, (name, args) -> {
            if (!"getPlayer".equals(name)) {
                return null;
            }
            short id = (short) args[0];
            return id == GOOD ? good : id == KEEPER ? keeper : other;
        });
    }

    @FunctionalInterface
    private interface Answer {

        Object answer(String name, Object[] args);
    }

    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Object result = answer.answer(method.getName(), args);
            if (result == null && method.getReturnType() == boolean.class) {
                return false;
            }
            return result;
        });
    }

}
