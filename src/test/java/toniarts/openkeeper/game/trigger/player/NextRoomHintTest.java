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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.es.filter.FieldFilter;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.NextRoomHint;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.trigger.TriggerActionData;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.TriggerAction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NextRoomHintTest {

    private static final short PLAYER_ID = 3;
    private static final short OTHER_PLAYER_ID = 4;

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void hintIsStoredForThePlayer() {
        displayNextRoom(PLAYER_ID, 5);

        assertEquals(5, hintOf(PLAYER_ID).roomId);
        assertNull(hintOf(OTHER_PLAYER_ID));
    }

    @Test
    void newHintReplacesTheOldOneAndZeroSwitchesItOff() {
        displayNextRoom(PLAYER_ID, 5);
        displayNextRoom(PLAYER_ID, 7);
        assertEquals(7, hintOf(PLAYER_ID).roomId);

        displayNextRoom(PLAYER_ID, 0);
        assertEquals(0, hintOf(PLAYER_ID).roomId);

        EntitySet hints = entityData.getEntities(NextRoomHint.class);
        try {
            assertEquals(1, hints.size());
        } finally {
            hints.release();
        }
    }

    private NextRoomHint hintOf(short playerId) {
        EntityId id = entityData.findEntity(new FieldFilter(Owner.class, "ownerId", playerId), NextRoomHint.class, Owner.class);
        return id == null ? null : entityData.getComponent(id, NextRoomHint.class);
    }

    private void displayNextRoom(short playerId, int roomId) {
        TriggerActionData action = new TriggerActionData(1, null, TriggerAction.ActionType.DISPLAY_NEXT_ROOM_TYPE);
        action.setUserData("targetId", (short) roomId);
        triggerControl(playerId).doAction(action);
    }

    private PlayerTriggerControl triggerControl(short playerId) {
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getTriggers")) {
                        return Map.of(1, new TriggerAction(null));
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        ILevelInfo levelInfo = (ILevelInfo) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ILevelInfo.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getLevelData")) {
                        return kwdFile;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        return new PlayerTriggerControl(null, levelInfo, null, null, null, 1, playerId, null, entityData);
    }

}
