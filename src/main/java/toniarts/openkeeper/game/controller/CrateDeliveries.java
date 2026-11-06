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
package toniarts.openkeeper.game.controller;

import com.simsilica.es.EntityId;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;

/**
 * Which crate goes to which blueprint, for the keeper's traps or for its
 * doors. The oldest blueprints get their crates first, like the workshop
 * manufactures for the oldest blueprints first.
 */
public final class CrateDeliveries {

    private CrateDeliveries() {
        // Nope
    }

    /**
     * The new deliveries
     *
     * @param blueprints the keeper's blueprints and their traps or doors, in
     * the order they were placed
     * @param crates the keeper's crates and their traps or doors
     * @param underWay the deliveries already under way, blueprint to crate,
     * they stay as they are
     * @return the new deliveries, blueprint to crate
     */
    public static Map<EntityId, EntityId> next(SequencedMap<EntityId, Short> blueprints, Map<EntityId, Short> crates,
            Map<EntityId, EntityId> underWay) {
        Set<EntityId> promised = new HashSet<>(underWay.values());
        Map<EntityId, EntityId> deliveries = new LinkedHashMap<>();
        for (Map.Entry<EntityId, Short> blueprint : blueprints.entrySet()) {
            if (underWay.containsKey(blueprint.getKey())) {
                continue;
            }
            for (Map.Entry<EntityId, Short> crate : crates.entrySet()) {
                if (crate.getValue().equals(blueprint.getValue()) && promised.add(crate.getKey())) {
                    deliveries.put(blueprint.getKey(), crate.getKey());
                    break;
                }
            }
        }
        return deliveries;
    }

}
