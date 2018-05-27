/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.game.task.worker;

import com.jme3.math.Vector2f;
import java.util.Objects;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.task.AbstractTileTask;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A task for creatures to rescue a fellow comrade from the harsh world
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class RescueCreatureTask extends AbstractTileTask {

    private final ICreatureController creature;

    public RescueCreatureTask(final IGameWorldController gameWorldController, final IMapController mapController, ICreatureController creature, short playerId) {
        super(gameWorldController, mapController, WorldUtils.vectorToPoint(creature.getPosition()).x, WorldUtils.vectorToPoint(creature.getPosition()).y, playerId);
        this.creature = creature;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation()); // FIXME 0.5f not needed?
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return this.creature.isUnconscious() && this.creature.hasLair();
    }

    @Override
    public boolean isRemovable() {
        return !this.creature.isUnconscious();
    }

    @Override
    public String toString() {
        return "Rescuing a creature at " + getTaskLocation();
    }

    @Override
    protected String getStringId() {
        return "2617";
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        //creature.setHaulable(this.creature);

        // Assign carry to lair
        CarryCreatureToLairTask task = new CarryCreatureToLairTask(gameWorldController, mapController, this.creature, playerId);
        task.assign(creature, true);
    }

    @Override
    public ArtResource getTaskAnimation(ICreatureController creature) {
        return null;
    }

    @Override
    public String getTaskIcon() {
        return "Textures/GUI/moods/SJ-Take_Crate.png";
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(this.creature);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RescueCreatureTask other = (RescueCreatureTask) obj;
        if (!Objects.equals(this.creature, other.creature)) {
            return false;
        }
        return true;
    }

}
