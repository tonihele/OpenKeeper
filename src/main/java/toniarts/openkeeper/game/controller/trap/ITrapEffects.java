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
import com.simsilica.es.EntityId;
import toniarts.openkeeper.tools.convert.map.Trap;

/**
 * Does what a trap's shot does to the world when the trap fires. The trap
 * system decides when a trap fires and at whom, this applies the shot.
 */
@FunctionalInterface
public interface ITrapEffects {

    /**
     * Fire the trap's shot
     *
     * @param trap the trap that fires
     * @param ownerId the player owning the trap
     * @param origin where the trap is
     * @param target the creature the trap aims at, {@code null} when the trap
     * fires without a victim
     */
    void fire(Trap trap, short ownerId, Vector3f origin, EntityId target);

}
