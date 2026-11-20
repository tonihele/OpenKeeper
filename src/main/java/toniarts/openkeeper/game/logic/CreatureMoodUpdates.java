/*
 * Copyright (C) 2026 OpenKeeper
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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.function.Consumer;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.tools.convert.map.Creature;

/** Commits mood changes before delivering their native script transition event. */
final class CreatureMoodUpdates {

    private CreatureMoodUpdates() {
    }

    static void set(EntityData data, EntityId entityId, Creature creature,
            CreatureMood previous, CreatureMood updated, int change, Consumer<EntityId> onAngry) {
        if (creature.getFlags2().contains(Creature.CreatureFlag2.IS_EMOTIONLESS)) {
            return;
        }
        data.setComponent(entityId, updated);
        int threshold = CreatureMood.toRuntimeThreshold(creature.getAttributes().getUnhappyThreshold());
        if (change > 0 && previous.getState(threshold) != 2 && updated.getState(threshold) == 2) {
            onAngry.accept(entityId);
        }
    }
}
