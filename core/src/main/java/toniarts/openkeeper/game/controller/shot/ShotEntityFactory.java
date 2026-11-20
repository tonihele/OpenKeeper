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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.component.Decay;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.ShotViewState;
import toniarts.openkeeper.tools.convert.map.Shot;

/**
 * Creates the entities that shots leave in the world for the view to draw
 */
final class ShotEntityFactory {

    /**
     * The shot health is its lifetime. The unit is not known, this is the same
     * guess the view makes for effect element lifetimes (see
     * {@code VisualEffect}), so that a shot and its effects last about equally
     * long
     */
    static final double SECONDS_PER_SHOT_HEALTH = 0.1;

    private ShotEntityFactory() {
        // Nope
    }

    /**
     * Creates an entity that shows the shot for its lifetime. The entity
     * decays on its own.
     *
     * @param entityData the entity data
     * @param shot the shot to show
     * @param position where to show it
     * @param gameTime current game time
     * @return the created entity
     */
    static EntityId create(EntityData entityData, Shot shot, Vector3f position, double gameTime) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity,
                new Position(0, new Vector3f(position)),
                new ShotViewState(shot.getShotId()),
                new Decay(gameTime, getLifetime(shot)));

        return entity;
    }

    static double getLifetime(Shot shot) {
        return shot.getHealth() * SECONDS_PER_SHOT_HEALTH;
    }

}
