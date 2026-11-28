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
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Spins a torture wheel, or the victim on it. A wheel lies flat on the floor,
 * or hangs on the wall facing the room. The angle only depends on the tile and
 * the clock, so a wheel and its victim given the same clock always turn
 * together. The victim of a wheel on the wall hangs in front of it, and the
 * trough under the wheel splashes it.
 */
public class TortureWheelSpinControl extends AbstractControl {

    /** DK II lays the torture wheels on the floor, and their victims on them */
    private static final Quaternion LYING_ON_THE_FLOOR = new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);
    /** DK II hangs the victim on a wheel on the wall 0x1C2 in front of the wheel */
    private static final float WALL_WHEEL_VICTIM_DISTANCE = 0x1C2 / 4096f;

    private final DoubleSupplier clockSeconds;
    private final float ticksPerSecond;
    private final Supplier<Spatial> wheelFinder;
    private final Supplier<Spatial> troughFinder;
    private final Consumer<Spatial> troughSplash;
    private Spatial wheel;
    private Spatial trough;
    private long lastSecond = -1;

    /**
     * A control for a wheel
     *
     * @param clockSeconds the time in seconds, share it between a wheel and its victim
     * @param ticksPerSecond the game ticks per second
     */
    public TortureWheelSpinControl(DoubleSupplier clockSeconds, float ticksPerSecond) {
        this(clockSeconds, ticksPerSecond, null, null, null);
    }

    /**
     * A control for the victim of a wheel
     *
     * @param clockSeconds the time in seconds, share it between a wheel and its victim
     * @param ticksPerSecond the game ticks per second
     * @param wheelFinder finds the wheel on the victim's tile, asked again if the wheel is gone
     * @param troughFinder finds the trough on the victim's tile, may return null
     * @param troughSplash called with the trough once a second while the victim hangs upside down
     */
    public TortureWheelSpinControl(DoubleSupplier clockSeconds, float ticksPerSecond, Supplier<Spatial> wheelFinder,
            Supplier<Spatial> troughFinder, Consumer<Spatial> troughSplash) {
        this.clockSeconds = clockSeconds;
        this.ticksPerSecond = ticksPerSecond;
        this.wheelFinder = wheelFinder;
        this.troughFinder = troughFinder;
        this.troughSplash = troughSplash;
    }

    @Override
    protected void controlUpdate(float tpf) {
        boolean victim = wheelFinder != null;
        if (victim && (wheel == null || wheel.getParent() == null)) {
            wheel = wheelFinder.get();
        }
        Spatial turning = victim ? wheel : spatial;
        if (turning == null || !isOnTheWall(turning)) {
            spatial.setLocalRotation(new Quaternion().fromAngleAxis(getAngle(spatial), Vector3f.UNIT_Y).multLocal(LYING_ON_THE_FLOOR));
            return;
        }

        // A wheel on the wall faces the room and turns in its plane
        float angle = getAngle(turning);
        Vector3f position = turning.getLocalTranslation();
        Point tile = WorldUtils.vectorToPoint(position);
        Quaternion rotation = new Quaternion().fromAngles(0, FastMath.atan2(tile.x - position.x, tile.y - position.z), 0)
                .multLocal(new Quaternion().fromAngleAxis(angle, Vector3f.UNIT_Z));
        if (!victim) {
            spatial.setLocalRotation(rotation);
            return;
        }

        // Like DK II (CState_sub_484CE0), the victim hangs in front of the wheel and turns with it
        Vector3f towardsRoom = WorldUtils.pointToVector3f(tile).subtractLocal(position.x, 0, position.z).normalizeLocal();
        spatial.setLocalTranslation(position.add(towardsRoom.multLocal(WALL_WHEEL_VICTIM_DISTANCE)));
        spatial.setLocalRotation(rotation);

        // DK II splashes a victim from the trough once a second while it hangs upside down (turned 0x3A0 to 0x45F of 0x800)
        long second = (long) clockSeconds.getAsDouble();
        if (second != lastSecond) {
            lastSecond = second;
            int turn = Math.floorMod((int) (angle / FastMath.TWO_PI * 2048), 2048);
            if (turn >= 0x3A0 && turn <= 0x45F) {
                if (trough == null || trough.getParent() == null) {
                    trough = troughFinder.get();
                }
                if (trough != null) {
                    troughSplash.accept(trough);
                }
            }
        }
    }

    /**
     * A wheel hangs on the wall above the floor, DK II 0x4EDFC0
     */
    private static boolean isOnTheWall(Spatial wheel) {
        return wheel.getLocalTranslation().y > WorldUtils.FLOOR_HEIGHT;
    }

    /**
     * DK II turns a torture wheel by 4 to 7 2048ths of a turn per game tick,
     * depending on the tile, and the wheels on every other tile the other way
     */
    private float getAngle(Spatial onTile) {
        Point tile = WorldUtils.vectorToPoint(onTile.getLocalTranslation());
        int sum = tile.x + tile.y;
        double speed = (4 + (sum & 3)) * ((sum & 1) != 0 ? 1 : -1) * ticksPerSecond * FastMath.TWO_PI / 2048;
        return (float) ((clockSeconds.getAsDouble() * speed) % FastMath.TWO_PI);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

}
