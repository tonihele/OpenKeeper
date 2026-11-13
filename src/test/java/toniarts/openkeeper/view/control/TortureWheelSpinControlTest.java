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
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import toniarts.openkeeper.utils.WorldUtils;

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

    private static Node onTheWall(DoubleSupplier clock) {
        // On the wall of tile (5, 4), moved from its centre towards the wall at -X
        Node wheel = new Node();
        wheel.setLocalTranslation(5 - 0.4f, WorldUtils.FLOOR_HEIGHT + 1, 4);
        wheel.addControl(new TortureWheelSpinControl(clock, 4));
        return wheel;
    }

    @Test
    void wheelOnTheWallFacesTheRoom() {
        Node wheel = onTheWall(() -> 0);
        wheel.updateLogicalState(0.1f);

        Vector3f looksAt = wheel.getLocalRotation().mult(Vector3f.UNIT_Z);
        assertTrue(looksAt.x > 0.99f, "faces +X, away from the wall: " + looksAt);
    }

    @Test
    void victimHangsInFrontOfTheWallWheelAndTurnsWithIt() {
        double[] time = {3.7};
        Node wheel = onTheWall(() -> time[0]);
        Node victim = new Node();
        victim.setLocalTranslation(5, WorldUtils.FLOOR_HEIGHT, 4);
        List<Spatial> splashes = new ArrayList<>();
        victim.addControl(new TortureWheelSpinControl(() -> time[0], 4, () -> wheel, () -> null, splashes::add));

        wheel.updateLogicalState(0.1f);
        victim.updateLogicalState(0.1f);

        assertEquals(wheel.getLocalRotation(), victim.getLocalRotation());
        assertEquals(wheel.getLocalTranslation().y, victim.getLocalTranslation().y, 1e-6f);
        assertEquals(wheel.getLocalTranslation().x + 0x1C2 / 4096f, victim.getLocalTranslation().x, 1e-4f);
    }

    @Test
    void victimOnTheFloorWheelTurnsByItself() {
        Node wheel = spinning(5.5f, 3.5f, () -> 3.7);
        Node victim = new Node();
        victim.setLocalTranslation(5.5f, 0.125f, 3.5f);
        victim.addControl(new TortureWheelSpinControl(() -> 3.7, 4, () -> wheel, () -> null, s -> { }));

        wheel.updateLogicalState(0.1f);
        victim.updateLogicalState(0.1f);

        assertEquals(wheel.getLocalRotation(), victim.getLocalRotation());
        assertEquals(0.125f, victim.getLocalTranslation().y, 1e-6f);
    }

    @Test
    void victimUpsideDownOnTheWallSplashesTheTroughOnceASecond() {
        // The wheel on tile (5, 4) turns 5 2048ths a tick at 4 ticks a second, upside down at 0x400 after 51.2 s
        double[] time = {0x400 / 5.0 / 4 + 0.01};
        Node wheel = onTheWall(() -> time[0]);
        Node trough = new Node();
        List<Spatial> splashes = new ArrayList<>();
        Node victim = new Node();
        victim.setLocalTranslation(5, WorldUtils.FLOOR_HEIGHT, 4);
        victim.addControl(new TortureWheelSpinControl(() -> time[0], 4, () -> wheel, () -> trough, splashes::add));

        wheel.updateLogicalState(0.1f);
        victim.updateLogicalState(0.1f);
        victim.updateLogicalState(0.1f);
        assertEquals(List.of(trough), splashes);

        time[0] += 1;
        victim.updateLogicalState(0.1f);
        assertEquals(2, splashes.size());
    }

    /** The wheel's turn around the up axis, without the lying down */
    private static Vector3f turnAxis(Node node) {
        Quaternion lying = new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);
        Vector3f axis = new Vector3f();
        node.getLocalRotation().mult(lying.inverse()).toAngleAxis(axis);
        return axis;
    }
}
