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
package toniarts.openkeeper.view;

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PlayerCameraStateTest {

    /**
     * The level script can ask for a camera path or a zoom on the frame the
     * camera state is attached, before it is initialized and has a camera.
     * Those wait for initialize() instead of failing.
     */
    @Test
    void cameraMovesBeforeInitializeWait() {
        PlayerCameraState cameraState = new PlayerCameraState(null);

        assertDoesNotThrow(() -> {
            cameraState.zoomToPoint(new Vector3f(10, 0, 10));
            cameraState.rotateAroundPoint(new Vector3f(10, 0, 10), true, 90, 2);
            cameraState.doTransition(1, new Vector3f(10, 0, 10), null);
        });
    }
}
