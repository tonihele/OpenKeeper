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
package toniarts.openkeeper.game.controller.shot;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.function.IntSupplier;
import toniarts.openkeeper.game.component.Decay;
import toniarts.openkeeper.game.component.InfernoArea;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.tools.convert.map.Shot;

/**
 * The keeper's Inferno: a fire where the spell was cast, burning the
 * creatures around it while it lasts. The burning itself is done by
 * {@code InfernoSystem}.
 * <p>
 * As in the original's Inferno handler (DKII.EXE 0x4AF660): the shot is a
 * constant one, so it burns every game tick while it lasts, with shot data 1
 * as the damage (100, 125 upgraded) and shot data 2 as the reach in tiles.
 * The shot's health 30 is its lifetime in game ticks, 7.5 seconds at the
 * level's 4 ticks a second.
 */
public final class InfernoShotProcessor implements IShotProcessor {

    private final EntityData entityData;
    private final IGameTimer gameTimer;
    private final IntSupplier ticksPerSecond;

    public InfernoShotProcessor(EntityData entityData, IGameTimer gameTimer, IntSupplier ticksPerSecond) {
        this.entityData = entityData;
        this.gameTimer = gameTimer;
        this.ticksPerSecond = ticksPerSecond;
    }

    @Override
    public void process(ShotRequest request) {
        double now = gameTimer.getGameTime();
        EntityId fire = ShotEntityFactory.create(entityData, request.shot(), request.position(), now);
        entityData.setComponent(fire, new Decay(now, getLifetime(request.shot())));

        entityData.setComponent(fire, new InfernoArea(request.shotData1(), request.shotData2(), 0));
    }

    /**
     * The fire lasts its health in game ticks
     *
     * @param shot the shot
     * @return seconds the fire lasts
     */
    double getLifetime(Shot shot) {
        return (double) shot.getHealth() / ticksPerSecond.getAsInt();
    }

}
