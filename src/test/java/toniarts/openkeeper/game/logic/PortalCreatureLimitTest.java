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
package toniarts.openkeeper.game.logic;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.PortalAttracted;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * How many creatures the portals attract. In the original only the creatures
 * that came in through a portal count, and the captured hero portals add to the
 * portals.
 */
class PortalCreatureLimitTest {

    private static final short KEEPER = 3;
    private static final short OTHER_KEEPER = 4;

    // The shipped CREATURES_SUPPORTED_BY_FIRST_PORTAL and CREATURES_SUPPORTED_PER_ADDITIONAL_PORTAL
    private static final int FIRST_PORTAL = 15;
    private static final int PER_ADDITIONAL_PORTAL = 5;

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void noPortalSupportsNoCreatures() {
        assertEquals(0, CreatureSpawnSystem.getPortalCapacity(0, FIRST_PORTAL, PER_ADDITIONAL_PORTAL));
    }

    @Test
    void eachPortalAfterTheFirstSupportsFiveMore() {
        assertEquals(15, CreatureSpawnSystem.getPortalCapacity(1, FIRST_PORTAL, PER_ADDITIONAL_PORTAL));
        assertEquals(20, CreatureSpawnSystem.getPortalCapacity(2, FIRST_PORTAL, PER_ADDITIONAL_PORTAL));
        assertEquals(25, CreatureSpawnSystem.getPortalCapacity(3, FIRST_PORTAL, PER_ADDITIONAL_PORTAL));
    }

    @Test
    void onlyLivingCreaturesThatCameThroughAPortalCount() {
        creature(KEEPER, true, false);
        creature(KEEPER, true, false);
        creature(KEEPER, false, false); // In the level from the start
        creature(KEEPER, true, true); // Dead
        creature(OTHER_KEEPER, true, false);

        assertEquals(2, CreatureSpawnSystem.getAttractedCreatureCount(entityData, KEEPER));
    }

    private void creature(short ownerId, boolean attracted, boolean dead) {
        EntityId entity = entityData.createEntity();
        entityData.setComponent(entity, new CreatureComponent());
        entityData.setComponent(entity, new Owner(ownerId, ownerId));
        if (attracted) {
            entityData.setComponent(entity, new PortalAttracted());
        }
        if (dead) {
            entityData.setComponent(entity, new Death(0));
        }
    }

}
