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
import com.simsilica.es.filter.FieldFilter;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.Spin;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.tools.convert.map.GameObject;

/**
 * Stops the objects that spin, like the roulette wheels of the Casino. In the
 * original a spin (object state 18, 0x4A13F0) slows down and stops after 21
 * game ticks, and then the object goes back to state 0 (0x49EED0). A new spin
 * while the object still spins starts over.
 */
public final class SpinSystem implements IGameLogicUpdatable {

    /**
     * How many game ticks a spin lasts in the original
     */
    public static final int SPIN_TICKS = 21;

    private final EntityData entityData;
    private final IGameTimer gameTimer;
    private final double spinDuration;
    private final EntitySet spinningObjects;

    public SpinSystem(EntityData entityData, IGameTimer gameTimer, int ticksPerSecond) {
        this.entityData = entityData;
        this.gameTimer = gameTimer;
        spinDuration = (double) SPIN_TICKS / ticksPerSecond;
        spinningObjects = entityData.getEntities(new FieldFilter<>(ObjectViewState.class, "state", GameObject.State.SPINNING),
                ObjectViewState.class);
    }

    @Override
    public void processTick(float tpf) {
        double gameTime = gameTimer.getGameTime();
        if (spinningObjects.applyChanges()) {
            for (Entity entity : spinningObjects.getChangedEntities()) {
                entityData.setComponent(entity.getId(), new Spin(gameTime + spinDuration));
            }
        }

        for (Entity entity : spinningObjects) {
            Spin spin = entityData.getComponent(entity.getId(), Spin.class);
            if (spin == null) {
                entityData.setComponent(entity.getId(), new Spin(gameTime + spinDuration));
            } else if (spin.endTime <= gameTime) {
                ObjectViewState viewState = entity.get(ObjectViewState.class);
                entityData.removeComponent(entity.getId(), Spin.class);
                entityData.setComponent(entity.getId(), new ObjectViewState(viewState.objectId, GameObject.State.NONE,
                        viewState.animState, viewState.visible, viewState.lairCreatureTypeId));
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        spinningObjects.release();
    }

}
