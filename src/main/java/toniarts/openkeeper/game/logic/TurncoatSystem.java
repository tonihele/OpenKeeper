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
import toniarts.openkeeper.game.component.Turncoat;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.IGameTimer;

/**
 * Ends the Turncoat spell. The creature goes back to fighting for its own
 * keeper when the time is up, or straight away when it is knocked out
 */
public final class TurncoatSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final IGameTimer gameTimer;
    private final EntitySet turncoatEntities;

    public TurncoatSystem(EntityData entityData, IGameTimer gameTimer) {
        this.entityData = entityData;
        this.gameTimer = gameTimer;

        turncoatEntities = entityData.getEntities(Turncoat.class);
    }

    @Override
    public void processTick(float tpf) {
        turncoatEntities.applyChanges();

        for (Entity entity : turncoatEntities) {
            if (entity.get(Turncoat.class).endTime <= gameTimer.getGameTime()
                    || entityData.getComponent(entity.getId(), Unconscious.class) != null) {
                entityData.removeComponent(entity.getId(), Turncoat.class);
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        turncoatEntities.release();
    }

}
