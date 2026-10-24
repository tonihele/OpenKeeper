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
package toniarts.openkeeper.game.task;

import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.sound.SoundEngine;
import toniarts.openkeeper.game.sound.SoundManager;
import toniarts.openkeeper.game.sound.ThingSounds;
import toniarts.openkeeper.game.navigation.INavigationService;

/**
 * Abstract base class for tasks pointing at terrain tiles
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractTileTask extends AbstractTask {

    private final Point location;
    protected final short playerId;

    /**
     * The work sound of each worker, it is restarted when it ends so that it lasts as long as the work
     */
    private final Map<EntityId, Integer> workSounds = new HashMap<>();

    public AbstractTileTask(final INavigationService navigationService, final IMapController mapController, final Point p, final short playerId) {
        super(navigationService, mapController);
        location = p;
        this.playerId = playerId;
    }

    /**
     * Plays the sound of the worker for as long as it works on the tile
     *
     * @param creature the worker
     * @param eventId the sound event of the creature type
     */
    protected void playWorkSound(ICreatureController creature, int eventId) {
        Integer handle = workSounds.get(creature.getEntityId());
        if (handle == null || handle == SoundEngine.NO_HANDLE || !SoundManager.isPlaying(handle)) {
            workSounds.put(creature.getEntityId(), ThingSounds.play(creature.getCreature().getSoundCategory(),
                    eventId, location));
        }
    }

    @Override
    public Point getTaskLocation() {
        return location;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + Objects.hashCode(this.location);
        hash = 71 * hash + this.playerId;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractTileTask other = (AbstractTileTask) obj;
        if (!Objects.equals(this.location, other.location)) {
            return false;
        }
        if (this.playerId != other.playerId) {
            return false;
        }
        return true;
    }

}
