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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.component.Decay;
import toniarts.openkeeper.game.component.EffectTrigger;
import toniarts.openkeeper.game.component.Position;

/**
 * Sets off effects for the view to play, like a shot's hit
 */
public final class EffectTriggers {

    /**
     * How long, in seconds, a triggered effect's entity lives. Long enough for
     * the view to see it, the effect itself plays on as long as it lasts
     */
    private static final double LIFETIME = 1;

    private EffectTriggers() {
        // Nope
    }

    /**
     * Sets off an effect
     *
     * @param entityData the entity data
     * @param effectId the effect, 0 for none
     * @param position where to play it
     * @param gameTime the current game time
     */
    public static void trigger(EntityData entityData, int effectId, Vector3f position, double gameTime) {
        if (effectId == 0) {
            return;
        }

        EntityId effect = entityData.createEntity();
        entityData.setComponents(effect,
                new Position(0, new Vector3f(position)),
                new EffectTrigger(effectId),
                new Decay(gameTime, LIFETIME));
    }

}
