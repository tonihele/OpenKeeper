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
package toniarts.openkeeper.game.controller;

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.Room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * What happens to a creature the keeper drops into a prison or a torture
 * chamber. Like in DK II, the torture chamber takes any creature, also the
 * keeper's own, except the keeper's Imps
 */
class CaptiveDropTest {

    private static final short KEEPER_ID = 3;
    private static final short HERO_ID = 1;

    @Test
    void enemyIsTortured() {
        assertEquals(CreatureState.TORTURED, GameWorldController.getCaptiveState(room(ObjectType.TORTUREE), HERO_ID, false));
    }

    @Test
    void ownCreatureIsTortured() {
        assertEquals(CreatureState.TORTURED, GameWorldController.getCaptiveState(room(ObjectType.TORTUREE), KEEPER_ID, false));
    }

    @Test
    void ownImpIsNotTortured() {
        assertNull(GameWorldController.getCaptiveState(room(ObjectType.TORTUREE), KEEPER_ID, true));
    }

    @Test
    void enemyImpIsTortured() {
        assertEquals(CreatureState.TORTURED, GameWorldController.getCaptiveState(room(ObjectType.TORTUREE), HERO_ID, true));
    }

    @Test
    void enemyIsImprisoned() {
        assertEquals(CreatureState.IMPRISONED, GameWorldController.getCaptiveState(room(ObjectType.PRISONER), HERO_ID, false));
    }

    @Test
    void ownCreatureIsNotImprisoned() {
        assertNull(GameWorldController.getCaptiveState(room(ObjectType.PRISONER), KEEPER_ID, false));
    }

    @Test
    void otherRoomsHoldNoCaptives() {
        assertNull(GameWorldController.getCaptiveState(room(ObjectType.LAIR), HERO_ID, false));
        assertNull(GameWorldController.getCaptiveState(null, HERO_ID, false));
    }

    private static IRoomController room(ObjectType objectType) {
        RoomInstance roomInstance = new RoomInstance(new Room());
        roomInstance.setOwnerId(KEEPER_ID);
        return (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(),
                new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getRoomInstance" -> roomInstance;
                    case "hasObjectControl" -> args[0] == objectType;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

}
