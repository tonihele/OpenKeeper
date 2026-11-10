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
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import java.util.function.DoubleSupplier;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Spins a torture wheel, or the victim on it, lying flat on the floor. The
 * angle only depends on the tile and the clock, so a wheel and its victim
 * given the same clock always turn together.
 */
public class TortureWheelSpinControl extends AbstractControl {

    /** DK II lays the torture wheels on the floor, and their victims on them */
    private static final Quaternion LYING_ON_THE_FLOOR = new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);

    private final DoubleSupplier clockSeconds;
    private final float ticksPerSecond;

    /**
     * @param clockSeconds the time in seconds, share it between a wheel and its victim
     * @param ticksPerSecond the game ticks per second
     */
    public TortureWheelSpinControl(DoubleSupplier clockSeconds, float ticksPerSecond) {
        this.clockSeconds = clockSeconds;
        this.ticksPerSecond = ticksPerSecond;
    }

    /**
     * DK II turns a torture wheel by 4 to 7 2048ths of a turn per game tick,
     * depending on the tile, and the wheels on every other tile the other way
     */
    @Override
    protected void controlUpdate(float tpf) {
        Point tile = WorldUtils.vectorToPoint(spatial.getLocalTranslation());
        int sum = tile.x + tile.y;
        double speed = (4 + (sum & 3)) * ((sum & 1) != 0 ? 1 : -1) * ticksPerSecond * FastMath.TWO_PI / 2048;
        float angle = (float) ((clockSeconds.getAsDouble() * speed) % FastMath.TWO_PI);
        spatial.setLocalRotation(new Quaternion().fromAngleAxis(angle, Vector3f.UNIT_Y).multLocal(LYING_ON_THE_FLOOR));
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

}
