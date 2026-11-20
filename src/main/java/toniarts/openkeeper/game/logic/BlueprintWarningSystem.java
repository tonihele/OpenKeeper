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

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.game.component.BlueprintWarning;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;

/** Native Workshop warning, selected by game-turn parity rather than render time. */
public final class BlueprintWarningSystem implements IGameLogicUpdatable {
    private final EntityData entityData;
    private final EntitySet traps;
    private final EntitySet doors;
    private final Map<Short, IPlayerController> players;
    private long turn;
    private final Room workshop;

    public BlueprintWarningSystem(EntityData entityData, IKwdFile level,
            Map<Short, IPlayerController> players) {
        this.entityData = entityData;
        this.players = players;
        workshop = level.getRoomById((short) 10);
        traps = entityData.getEntities(Owner.class, TrapViewState.class);
        doors = entityData.getEntities(Owner.class, DoorViewState.class);
    }

    @Override
    public void processTick(float tpf) {
        traps.applyChanges();
        doors.applyChanges();
        // This system runs once per logic turn and stops with the paused game loop.
        // Reconstructing turns from elapsed seconds can repeat/skip a phase when
        // the loop wakes slightly before or after a nominal tick boundary.
        boolean oddTurn = (turn++ & 1) != 0;
        Map<Short, Boolean> missingWorkshop = new HashMap<>();
        for (Entity trap : traps) {
            update(trap, trap.get(TrapViewState.class).blueprint, !oddTurn, missingWorkshop);
        }
        for (Entity door : doors) {
            // The native door and trap routines use opposite phases.
            update(door, door.get(DoorViewState.class).blueprint, oddTurn, missingWorkshop);
        }
    }

    private void update(Entity entity, boolean blueprint, boolean highlighted, Map<Short, Boolean> missingWorkshop) {
        short ownerId = entity.get(Owner.class).ownerId;
        boolean warning = blueprint && missingWorkshop.computeIfAbsent(ownerId, id -> {
            IPlayerController player = players.get(id);
            return player != null && (workshop == null || player.getRoomControl().getTypeCount(workshop) == 0);
        });
        BlueprintWarning current = entityData.getComponent(entity.getId(), BlueprintWarning.class);
        if (!warning) {
            if (current != null) entityData.removeComponent(entity.getId(), BlueprintWarning.class);
        } else if (current == null || current.highlighted != highlighted) {
            entityData.setComponent(entity.getId(), new BlueprintWarning(highlighted));
        }
    }

    @Override public void start() { turn = 0; }

    @Override public void stop() {
        traps.release();
        doors.release();
    }
}
