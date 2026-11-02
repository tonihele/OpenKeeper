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
package toniarts.openkeeper.game.controller;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.ObjectViewState.GameObjectAnimState;
import toniarts.openkeeper.tools.convert.map.GameObject;

/** DKII's amount-dependent appearance for gold stored in rooms. */
public final class StoredGoldVisuals {

    private StoredGoldVisuals() {
    }

    public static void update(EntityData entityData, EntityId entity, Gold gold, GameObject definition) {
        ObjectViewState current = entityData.getComponent(entity, ObjectViewState.class);
        if (current == null || gold.maxGold <= 0
                || !definition.getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_GOLD)) {
            return;
        }

        // Native 0x4F4440 uses integer division, including the rounded-down thresholds.
        int resource = gold.gold < gold.maxGold / 3 ? 1
                : gold.gold < (2L * gold.maxGold) / 3 ? 2 : 0;
        if (resource > 0 && (definition.getAdditionalResources().size() < resource
                || definition.getAdditionalResources().get(resource - 1) == null)) {
            // Like DKII, leave the existing mesh alone when the selected art is absent.
            return;
        }
        GameObjectAnimState animation = switch (resource) {
            case 1 -> GameObjectAnimState.ADDITIONAL_RESOURCE_1;
            case 2 -> GameObjectAnimState.ADDITIONAL_RESOURCE_2;
            default -> GameObjectAnimState.MESH_RESOURCE;
        };
        if (current.animState != animation) {
            entityData.setComponent(entity, new ObjectViewState(current.objectId, current.state,
                    animation, current.visible, current.lairCreatureTypeId));
        }
    }
}
