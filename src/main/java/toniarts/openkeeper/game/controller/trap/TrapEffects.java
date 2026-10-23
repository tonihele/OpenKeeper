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
package toniarts.openkeeper.game.controller.trap;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.EnumSet;
import java.util.Set;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.Trap;

/**
 * Applies the shots of the traps. A trap hands its own shot data to its shot,
 * so the trap decides how hard the shot hits.
 */
public final class TrapEffects implements ITrapEffects {

    private static final Logger logger = System.getLogger(TrapEffects.class.getName());

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final Set<Shot.ProcessType> reportedProcessTypes = EnumSet.noneOf(Shot.ProcessType.class);

    public TrapEffects(EntityData entityData, IKwdFile kwdFile) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
    }

    @Override
    public void fire(Trap trap, short ownerId, Vector3f origin, EntityId target) {
        Shot shot = kwdFile.getShotById(trap.getShotTypeId());
        if (shot == null) {
            return;
        }

        switch (shot.getProcessType()) {
            default ->
                reportNotImplemented(shot.getProcessType());
        }
    }

    private void reportNotImplemented(Shot.ProcessType processType) {

        // Traps fire often, say it once
        if (reportedProcessTypes.add(processType)) {
            logger.log(Level.WARNING, "Trap shot type {0} not implemented", processType);
        }
    }

}
