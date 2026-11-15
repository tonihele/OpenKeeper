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
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.Spin;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.tools.convert.map.GameObject;

/**
 * Stops the objects that spin, like the roulette wheels of the Casino. In the
 * original a spin (object state 18, 0x4A13F0) slows down and stops after 21
 * game ticks, and then the object goes back to state 0 (0x49EED0). The object
 * controller starts the spin, this only watches the objects that spin.
 */
public final class SpinSystem implements IGameLogicUpdatable {

    /**
     * How many game ticks a spin lasts in the original
     */
    public static final int SPIN_TICKS = 21;

    private final EntityData entityData;
    private final IGameTimer gameTimer;
    private final EntitySet spinningObjects;

    public SpinSystem(EntityData entityData, IGameTimer gameTimer) {
        this.entityData = entityData;
        this.gameTimer = gameTimer;
        spinningObjects = entityData.getEntities(Spin.class, ObjectViewState.class);
    }

    @Override
    public void processTick(float tpf) {
        double gameTime = gameTimer.getGameTime();
        spinningObjects.applyChanges();

        for (Entity entity : spinningObjects) {
            if (entity.get(Spin.class).endTime <= gameTime) {
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
