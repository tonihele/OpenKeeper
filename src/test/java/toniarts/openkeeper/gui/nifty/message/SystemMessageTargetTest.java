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
package toniarts.openkeeper.gui.nifty.message;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Where the zoom to button of a system message takes the camera
 */
class SystemMessageTargetTest {

    @Test
    void entityTargetFollowsTheEntity() {
        EntityId creature = new EntityId(7);
        Map<EntityId, Vector3f> positions = new HashMap<>();
        positions.put(creature, new Vector3f(3, 0, 4));
        SystemMessageTarget target = SystemMessageTarget.ofEntity(creature);

        assertEquals(new Vector3f(3, 0, 4), target.resolve(positions::get));

        // The creature has walked off since it was sighted
        positions.put(creature, new Vector3f(10, 0, 12));
        assertEquals(new Vector3f(10, 0, 12), target.resolve(positions::get));
    }

    @Test
    void entityTargetIsGoneWithTheEntity() {
        SystemMessageTarget target = SystemMessageTarget.ofEntity(new EntityId(7));

        assertNull(target.resolve(entityId -> null));
    }

    @Test
    void positionTargetStaysPut() {
        Vector3f place = new Vector3f(5, 0, 6);
        SystemMessageTarget target = SystemMessageTarget.ofPosition(place);
        place.set(0, 0, 0);

        assertEquals(new Vector3f(5, 0, 6), target.resolve(entityId -> {
            throw new AssertionError("A position target has no entity to look up");
        }));
    }
}
