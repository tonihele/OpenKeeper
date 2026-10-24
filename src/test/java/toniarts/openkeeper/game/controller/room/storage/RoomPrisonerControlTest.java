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
package toniarts.openkeeper.game.controller.room.storage;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The prisoners of a Prison go free when the room is lost: they keep their own
 * owner, are under their own control again and stand idle.
 */
class RoomPrisonerControlTest {

    private static final short CAPTOR = 1;
    private static final short PRISONER_OWNER = 3;
    private static final short CREATURE_ID = 7;
    private static final Point TILE = new Point(2, 2);

    private EntityData entityData;
    private EntityId roomEntity;
    private RoomPrisonerControl control;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        roomEntity = entityData.createEntity();
        control = createControl(roomEntity);
    }

    @Test
    void destroyingTheRoomFreesThePrisoners() {
        EntityId prisoner = imprison(control);

        control.destroy();

        assertFree(prisoner);
    }

    @Test
    void capturingTheRoomFreesThePrisonersAndEmptiesTheRoom() {
        EntityId first = imprison(control);
        EntityId second = imprison(control);

        control.captured((short) 2);

        assertFree(first);
        assertFree(second);
        assertEquals(0, control.getCurrentCapacity());
    }

    @Test
    void creatureTakenAwayEarlierIsLeftAlone() {
        EntityId prisoner = imprison(control);
        control.removeItem(prisoner);
        entityData.setComponent(prisoner, new Owner(PRISONER_OWNER, (short) 5));

        control.destroy();

        assertEquals(5, entityData.getComponent(prisoner, Owner.class).controlId);
    }

    @Test
    void prisonerThatDiedInPrisonIsNotRevived() {
        EntityId prisoner = imprison(control);
        entityData.removeComponent(prisoner, CreatureImprisoned.class);
        entityData.removeComponent(prisoner, CreatureAi.class);

        control.destroy();

        assertNull(entityData.getComponent(prisoner, CreatureAi.class));
        assertNull(entityData.getComponent(prisoner, Stored.class));
    }

    @Test
    void discardedControlOfAnExpandedRoomDoesNotFreeThePrisoners() {
        EntityId prisoner = imprison(control);
        RoomPrisonerControl replacement = createControl(roomEntity);

        replacement.destroy();

        assertNotNull(entityData.getComponent(prisoner, Stored.class));
        assertEquals(CAPTOR, entityData.getComponent(prisoner, Owner.class).controlId);
    }

    private void assertFree(EntityId prisoner) {
        assertNull(entityData.getComponent(prisoner, Stored.class));
        assertNull(entityData.getComponent(prisoner, CreatureImprisoned.class));
        Owner owner = entityData.getComponent(prisoner, Owner.class);
        assertEquals(PRISONER_OWNER, owner.ownerId);
        assertEquals(PRISONER_OWNER, owner.controlId);
        assertEquals(CreatureState.IDLE, entityData.getComponent(prisoner, CreatureAi.class).getCreatureState());
    }

    private EntityId imprison(RoomPrisonerControl prison) {
        EntityId prisoner = entityData.createEntity();
        entityData.setComponent(prisoner, new Owner(PRISONER_OWNER, CAPTOR));
        entityData.setComponent(prisoner, new CreatureImprisoned(0, 0));
        entityData.setComponent(prisoner, new CreatureAi(0, CreatureState.IMPRISONED, CREATURE_ID));
        prison.addItem(prisoner, TILE);
        return prisoner;
    }

    private RoomPrisonerControl createControl(EntityId room) {
        RoomInstance roomInstance = new RoomInstance(new Room());
        roomInstance.addCoordinate(TILE);
        IRoomController parent = (IRoomController) Proxy.newProxyInstance(
                IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityId" -> room;
                    case "getOwnerId" -> CAPTOR;
                    case "getRoomInstance" -> roomInstance;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return new RoomPrisonerControl(null, parent, entityData, () -> 10) {
            @Override
            protected java.util.Collection<Point> getCoordinates() {
                return roomInstance.getCoordinates();
            }

            @Override
            protected int getNumberOfAccessibleTiles() {
                return 4;
            }
        };
    }
}
