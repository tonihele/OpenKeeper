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
package toniarts.openkeeper.view.text;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TrapComponent;

/**
 * Counts the Guard Posts a player owns. A Guard Post marks out a patrol route
 * from the nearest Guard Room, so the count is the number of patrol routes the
 * Guard Room tooltip reports.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class GuardPostCounter implements ToIntFunction<Short>, AutoCloseable {

    private final EntitySet traps;
    private final Predicate<Short> guardPostTrapIds;

    /**
     * Creates a new counter
     *
     * @param entityData the entity data to follow
     * @param guardPostTrapIds tells whether a trap ID is the Guard Post
     */
    public GuardPostCounter(EntityData entityData, Predicate<Short> guardPostTrapIds) {
        this.traps = entityData.getEntities(TrapComponent.class, Owner.class);
        this.guardPostTrapIds = guardPostTrapIds;
    }

    /**
     * Counts the Guard Posts of a player
     *
     * @param ownerId the player ID
     * @return the number of Guard Posts the player owns
     */
    @Override
    public int applyAsInt(Short ownerId) {
        traps.applyChanges();
        int count = 0;
        for (Entity entity : traps) {
            if (entity.get(Owner.class).ownerId == ownerId
                    && guardPostTrapIds.test(entity.get(TrapComponent.class).trapId)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void close() {
        traps.release();
    }

}
