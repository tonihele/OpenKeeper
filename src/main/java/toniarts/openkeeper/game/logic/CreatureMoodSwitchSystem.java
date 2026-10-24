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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper. If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.logic;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import java.util.function.BooleanSupplier;
import toniarts.openkeeper.game.component.CreatureMood;

/**
 * Honours the level script's creature moods switch (trigger action
 * SET_CREATURE_MOODS). In DKII the operations that add, remove, scale, decay or
 * clear the anger counters of a creature do nothing while the switch is off
 * (DKII.EXE 0x486E20-0x4871A0), so the creatures can not turn unhappy or
 * angry. Here the moods of the creatures are marked frozen, which makes the
 * same operations of {@link CreatureMood} do nothing.
 */
public final class CreatureMoodSwitchSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final BooleanSupplier moodsEnabled;
    private final EntitySet entities;

    /**
     * @param entityData the entity data
     * @param moodsEnabled tells whether the moods switch is on
     */
    public CreatureMoodSwitchSystem(EntityData entityData, BooleanSupplier moodsEnabled) {
        this.entityData = entityData;
        this.moodsEnabled = moodsEnabled;
        entities = entityData.getEntities(CreatureMood.class);
    }

    @Override
    public void processTick(float tpf) {
        entities.applyChanges();
        boolean frozen = !moodsEnabled.getAsBoolean();
        for (Entity entity : entities) {
            CreatureMood mood = entity.get(CreatureMood.class);
            CreatureMood updated = mood.withFrozen(frozen);
            if (updated != mood) {
                entityData.setComponent(entity.getId(), updated);
            }
        }
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        entities.release();
    }
}
