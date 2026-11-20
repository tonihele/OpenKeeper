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

import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.*;
import static org.junit.jupiter.api.Assertions.*;

class BlueprintWarningSystemTest {
    private final DefaultEntityData entities = new DefaultEntityData();
    private final Room workshop = RoomFixtures.room((short) 10, 0);
    private final PlayerRoomControl rooms = new PlayerRoomControl(new Keeper(false, (short) 3), List.of(workshop));
    private final IPlayerController player = (IPlayerController) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{IPlayerController.class}, (p, m, a) -> m.getName().equals("getRoomControl") ? rooms : null);
    private final IKwdFile level = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{IKwdFile.class}, (p, m, a) -> switch (m.getName()) {
                case "getGameLevel" -> ShotFixtures.gameLevel(8);
                case "getRoomById" -> workshop;
                default -> null;
            });
    private final BlueprintWarningSystem system = new BlueprintWarningSystem(entities, level, Map.of((short) 3, player));

    @Test void trapsAndDoorsAlternateInOppositePhasesOnEveryLogicTurn() {
        EntityId trap = trap(true), door = door(true);
        system.processTick(0);
        assertTrue(warning(trap).highlighted);
        assertFalse(warning(door).highlighted);
        system.processTick(0);
        assertFalse(warning(trap).highlighted);
        assertTrue(warning(door).highlighted);
        system.processTick(0);
        assertTrue(warning(trap).highlighted);
        assertFalse(warning(door).highlighted);
    }

    @Test void buildingLosingAndCapturingAWorkshopRefreshesBothKindsOfBlueprint() {
        EntityId trap = trap(true), door = door(true);
        IRoomController room = room();
        system.processTick(0);
        rooms.onBuild(room);
        system.processTick(0);
        assertNull(warning(trap)); assertNull(warning(door));
        rooms.onSold(room);
        system.processTick(0);
        assertNotNull(warning(trap)); assertNotNull(warning(door));
        rooms.onCaptured(room);
        system.processTick(0);
        assertNull(warning(trap)); assertNull(warning(door));
        rooms.onCapturedByEnemy(room);
        system.processTick(0);
        assertNotNull(warning(trap)); assertNotNull(warning(door));
    }

    @Test void finishedEntitiesAndUnknownOwnersDoNotWarn() {
        EntityId trap = trap(false), door = door(false), other = trap(true);
        entities.setComponent(other, new Owner((short) 9, (short) 9));
        system.processTick(0);
        assertNull(warning(trap)); assertNull(warning(door)); assertNull(warning(other));
        entities.setComponent(trap, new TrapViewState((short) 1, true));
        system.processTick(0);
        assertNotNull(warning(trap));
        entities.setComponent(trap, new TrapViewState((short) 1, false));
        system.processTick(0);
        assertNull(warning(trap));
    }

    @Test void schedulingJitterDoesNotRepeatOrSkipAFlashPhase() {
        EntityId trap = trap(true), door = door(true);
        // Wake just before/after tick boundaries, then simulate a delayed tick.
        // The old elapsed-seconds conversion repeated colors for these intervals.
        float[] intervals = {0.249f, 0.249f, 0.253f, 0.248f, 0.501f, 0.25f};
        for (int turn = 0; turn < intervals.length; turn++) {
            system.processTick(intervals[turn]);
            assertEquals((turn & 1) == 0, warning(trap).highlighted, "Trap turn " + turn);
            assertEquals((turn & 1) != 0, warning(door).highlighted, "Door turn " + turn);
        }
    }

    private EntityId trap(boolean blueprint) {
        EntityId id = entities.createEntity();
        entities.setComponents(id, new Owner((short) 3, (short) 3), new TrapViewState((short) 1, blueprint));
        return id;
    }
    private EntityId door(boolean blueprint) {
        EntityId id = entities.createEntity();
        entities.setComponents(id, new Owner((short) 3, (short) 3), new DoorViewState((short) 1, false, blueprint, false));
        return id;
    }
    private BlueprintWarning warning(EntityId id) { return entities.getComponent(id, BlueprintWarning.class); }
    private IRoomController room() {
        return (IRoomController) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IRoomController.class},
                (p, m, a) -> switch (m.getName()) {
                    case "getRoom" -> workshop;
                    case "isDungeonHeart" -> false;
                    case "hashCode" -> System.identityHashCode(p);
                    case "equals" -> p == a[0];
                    default -> null;
                });
    }
}
