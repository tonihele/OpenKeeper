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
package toniarts.openkeeper.view.control;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.util.function.DoubleSupplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TortureWheelSpinControlTest {

    private static Node spinning(float x, float z, DoubleSupplier clock) {
        Node node = new Node();
        node.setLocalTranslation(x, 0, z);
        node.addControl(new TortureWheelSpinControl(clock, 4));
        return node;
    }

    @Test
    void wheelAndVictimOnTheSameTileTurnTogether() {
        double[] time = {3.7};
        Node wheel = spinning(5.5f, 3.5f, () -> time[0]);
        Node victim = spinning(5.5f, 3.5f, () -> time[0]);

        wheel.updateLogicalState(0.1f);
        victim.updateLogicalState(0.1f);

        assertEquals(wheel.getLocalRotation(), victim.getLocalRotation());
        assertNotEquals(new Quaternion(), wheel.getLocalRotation());
    }

    @Test
    void neighbouringTilesTurnTheOtherWay() {
        Node first = spinning(5.5f, 3.5f, () -> 0.1);
        Node second = spinning(6.5f, 3.5f, () -> 0.1);

        first.updateLogicalState(0.1f);
        second.updateLogicalState(0.1f);

        assertTrue(turnAxis(first).y * turnAxis(second).y < 0);
    }

    /** The wheel's turn around the up axis, without the lying down */
    private static Vector3f turnAxis(Node node) {
        Quaternion lying = new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);
        Vector3f axis = new Vector3f();
        node.getLocalRotation().mult(lying.inverse()).toAngleAxis(axis);
        return axis;
    }
}
