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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrateDeliveriesTest {

    private static final short SENTRY = 1;
    private static final short BOULDER = 3;

    private static final EntityId OLD_BLUEPRINT = new EntityId(1);
    private static final EntityId NEW_BLUEPRINT = new EntityId(2);
    private static final EntityId CRATE = new EntityId(10);
    private static final EntityId OTHER_CRATE = new EntityId(11);

    @Test
    void oldestBlueprintGetsTheCrate() {
        Map<EntityId, EntityId> deliveries = CrateDeliveries.next(blueprints(SENTRY, SENTRY), Map.of(CRATE, SENTRY), Map.of());

        assertEquals(Map.of(OLD_BLUEPRINT, CRATE), deliveries);
    }

    @Test
    void crateOnlyGoesToItsOwnTrap() {
        Map<EntityId, EntityId> deliveries = CrateDeliveries.next(blueprints(BOULDER, SENTRY), Map.of(CRATE, SENTRY), Map.of());

        assertEquals(Map.of(NEW_BLUEPRINT, CRATE), deliveries);
    }

    @Test
    void deliveryUnderWayStaysAndItsCrateIsNotPromisedTwice() {
        Map<EntityId, EntityId> deliveries = CrateDeliveries.next(blueprints(SENTRY, SENTRY), Map.of(CRATE, SENTRY),
                Map.of(OLD_BLUEPRINT, CRATE));

        assertTrue(deliveries.isEmpty());
    }

    @Test
    void everyBlueprintGetsACrateOfItsOwn() {
        Map<EntityId, Short> crates = new LinkedHashMap<>();
        crates.put(CRATE, SENTRY);
        crates.put(OTHER_CRATE, SENTRY);

        Map<EntityId, EntityId> deliveries = CrateDeliveries.next(blueprints(SENTRY, SENTRY), crates, Map.of());

        assertEquals(Map.of(OLD_BLUEPRINT, CRATE, NEW_BLUEPRINT, OTHER_CRATE), deliveries);
    }

    @Test
    void noCrateNoDelivery() {
        assertTrue(CrateDeliveries.next(blueprints(SENTRY, SENTRY), Map.of(), Map.of()).isEmpty());
    }

    private static SequencedMap<EntityId, Short> blueprints(short oldTrap, short newTrap) {
        SequencedMap<EntityId, Short> blueprints = new LinkedHashMap<>();
        blueprints.put(OLD_BLUEPRINT, oldTrap);
        blueprints.put(NEW_BLUEPRINT, newTrap);
        return blueprints;
    }

}
