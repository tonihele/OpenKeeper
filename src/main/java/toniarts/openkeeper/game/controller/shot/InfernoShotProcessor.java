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
 * Prima's official strategy guide for the original gives 40 damage a second,
 * 50 upgraded, for 8 seconds. The shot data is 100 and 125, and the shot's
 * health 30 is 7.5 seconds at the level's 4 ticks a second.
 */
public final class InfernoShotProcessor implements IShotProcessor {

    /**
     * Damage a second per point of shot data 1
     */
    static final double DAMAGE_PER_SECOND_PER_SHOT_DATA = 0.4;

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

        // Shot data 1 is the strength, 2 the reach in tiles
        double interval = getInterval(request.shot());
        int damage = (int) Math.round(request.shotData1() * DAMAGE_PER_SECOND_PER_SHOT_DATA * interval);
        entityData.setComponent(fire, new InfernoArea(damage, request.shotData2(), interval, now));
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

    /**
     * The shot burns again after its timed delay, which is taken to be in
     * milliseconds
     *
     * @param shot the shot
     * @return seconds between two burns
     */
    static double getInterval(Shot shot) {
        return shot.getTimedDelay() / 1000.0;
    }

}
