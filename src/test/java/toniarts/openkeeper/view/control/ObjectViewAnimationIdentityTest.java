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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.view.control;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.ObjectViewState;
import static org.junit.jupiter.api.Assertions.*;
class ObjectViewAnimationIdentityTest {
    @Test void visibilityAndRepublishedComponentDoNotResetTheMesh() {
        var a = new ObjectViewState((short) 13, null, ObjectViewState.GameObjectAnimState.MESH_RESOURCE, true);
        var b = new ObjectViewState((short) 13, null, ObjectViewState.GameObjectAnimState.MESH_RESOURCE, false);
        assertTrue(ObjectViewControl.sameAnimation(a, b));
        b.animState = ObjectViewState.GameObjectAnimState.ADDITIONAL_RESOURCE_1;
        assertFalse(ObjectViewControl.sameAnimation(a, b));
        assertFalse(ObjectViewControl.sameAnimation(a, null));
    }
}
