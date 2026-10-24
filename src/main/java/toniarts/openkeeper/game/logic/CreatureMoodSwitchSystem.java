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
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import toniarts.openkeeper.game.component.CreatureMood;

/**
 * Honours the level script's creature moods switch (trigger action
 * SET_CREATURE_MOODS). In DKII the anger counters of a creature only change
 * while the switch is on: the operations that add, remove, scale, decay or clear
 * them do nothing when it is off (DKII.EXE 0x486E20-0x4871A0). So while the
 * switch is off the counters are held at the value they had when it was
 * turned off, and the creatures can not turn unhappy or angry.
 * <p>
 * The systems that change the counters are not told about the switch, so this
 * one undoes their changes. It should run after them in the tick, before the creature AI.
 */
public final class CreatureMoodSwitchSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final BooleanSupplier moodsEnabled;
    private final EntitySet entities;
    private final Map<EntityId, CreatureMood> frozenMoods = new HashMap<>();

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
        if (moodsEnabled.getAsBoolean()) {
            frozenMoods.clear();
            return;
        }
        for (Entity entity : entities.getRemovedEntities()) {
            frozenMoods.remove(entity.getId());
        }
        for (Entity entity : entities) {
            freeze(entity);
        }
    }

    private void freeze(Entity entity) {
        CreatureMood current = entity.get(CreatureMood.class);
        CreatureMood frozen = frozenMoods.putIfAbsent(entity.getId(), current);
        if (frozen != null && !sameCounters(frozen, current)) {
            entityData.setComponent(entity.getId(), frozen);
        }
    }

    private static boolean sameCounters(CreatureMood a, CreatureMood b) {
        return a.general == b.general && a.noFood == b.noFood && a.noLair == b.noLair
                && a.noWork == b.noWork && a.noPay == b.noPay && a.other == b.other;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        entities.release();
        frozenMoods.clear();
    }
}
