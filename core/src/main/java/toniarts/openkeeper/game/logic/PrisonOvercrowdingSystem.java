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

import com.jme3.math.FastMath;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import java.util.Map;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.RoomPrisonerControl;
import toniarts.openkeeper.tools.convert.map.Variable;

/**
 * An overcrowded Prison risks a jail break. Like in DK II (DKII.EXE 0x4E5A70),
 * a Prison takes in prisoners over its safe capacity, and each one taken in
 * over it rolls for a jail break: the chance is the number of prisoners over
 * the safe capacity times OVERCROWDING_JAIL_BREAK_PERCENTAGE_PER_EXTRA_CREATURE.
 * The jail break frees every prisoner of the Prison (0x4E2E70).
 */
public final class PrisonOvercrowdingSystem implements IGameLogicUpdatable {

    private final IMapController mapController;
    private final int jailBreakPercentagePerExtraPrisoner;
    private final EntitySet prisoners;

    public PrisonOvercrowdingSystem(EntityData entityData, IMapController mapController,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.mapController = mapController;
        jailBreakPercentagePerExtraPrisoner = (int) gameSettings.get(Variable.MiscVariable.MiscType.OVERCROWDING_JAIL_BREAK_PERCENTAGE_PER_EXTRA_CREATURE).getValue();

        prisoners = entityData.getEntities(CreatureImprisoned.class, Stored.class);
    }

    @Override
    public void processTick(float tpf) {
        if (prisoners.applyChanges()) {
            for (Entity prisoner : prisoners.getAddedEntities()) {
                rollJailBreak(prisoner.get(Stored.class));
            }
        }
    }

    private void rollJailBreak(Stored stored) {
        IRoomController room = mapController.getRoomController(stored.room);
        if (room == null || !(room.getObjectControl(ObjectType.PRISONER) instanceof RoomPrisonerControl prison)) {
            return;
        }
        int extraPrisoners = prison.getCurrentCapacity() - prison.getMaxCapacity();
        if (extraPrisoners > 0 && FastMath.rand.nextInt(100) < extraPrisoners * jailBreakPercentagePerExtraPrisoner) {
            prison.releasePrisoners();
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        prisoners.release();
    }
}
