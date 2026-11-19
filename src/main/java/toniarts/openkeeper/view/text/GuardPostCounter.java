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

import com.jme3.app.state.AbstractAppState;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.FieldFilter;
import java.util.function.Predicate;
import java.util.function.IntSupplier;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TrapComponent;

/**
 * Counts the Guard Posts of one player, the one the tooltips are shown to. A Guard Post marks out a patrol route
 * from the nearest Guard Room, so the count is the number of patrol routes the
 * Guard Room tooltip reports. The entity set is brought up to date on every
 * frame, as a set left alone piles up its changes.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class GuardPostCounter extends AbstractAppState implements IntSupplier {

    private final EntitySet traps;
    private final Predicate<Short> guardPostTrapIds;

    /**
     * Creates a new counter
     *
     * @param entityData the entity data to follow
     * @param ownerId the player whose Guard Posts are counted
     * @param guardPostTrapIds tells whether a trap ID is the Guard Post
     */
    public GuardPostCounter(EntityData entityData, short ownerId, Predicate<Short> guardPostTrapIds) {
        this.traps = entityData.getEntities(new FieldFilter<>(Owner.class, "ownerId", ownerId),
                TrapComponent.class, Owner.class);
        this.guardPostTrapIds = guardPostTrapIds;
    }

    /**
     * Counts the Guard Posts of the player
     *
     * @return the number of Guard Posts the player owns
     */
    @Override
    public int getAsInt() {
        int count = 0;
        for (Entity entity : traps) {
            if (guardPostTrapIds.test(entity.get(TrapComponent.class).trapId)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void update(float tpf) {
        traps.applyChanges();
    }

    @Override
    public void cleanup() {
        traps.release();
        super.cleanup();
    }

}
