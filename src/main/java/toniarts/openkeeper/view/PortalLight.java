/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.view;

import com.jme3.light.PointLight;
import com.jme3.math.ColorRGBA;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/** Persistent Portal light, traced from DKII 0x4dcd20 and CLight::update (0x43a2e0). */
final class PortalLight {
    private static final double STEP_SECONDS = 1.0 / 20;
    private final PointLight light = new PointLight();
    private Point tile;
    private boolean destroyed;
    private int phase = 1;
    private boolean brightening = true;
    private double remainder;

    PortalLight(RoomComponent room) {
        // Native requests 10240 units; CLight clamps that to 10112. One tile is 4096.
        light.setRadius(10112 / 4096f);
        light.setColor(new ColorRGBA(1, 33 / 255f, 26 / 255f, 1));
        setRoom(room);
    }

    void setRoom(RoomComponent room) {
        tile = room.location;
        destroyed = room.destroyed;
        light.setPosition(WorldUtils.pointToVector3f(tile).addLocal(0, 2000 / 4096f, 0));
    }

    void update(float tpf, boolean visible) {
        light.setEnabled(!destroyed && visible);
        remainder += tpf;
        if (remainder + 1e-9 < STEP_SECONDS) {
            return;
        }
        remainder %= STEP_SECONDS;
        // Native advances once per eligible render update, even when several ticks elapsed.
        // Pulse output writes channels from base+0, base+4, base+2 respectively.
        light.setColor(new ColorRGBA(((255 * phase) >> 8) / 255f,
                ((26 * phase) >> 8) / 255f, ((33 * phase) >> 8) / 255f, 1));
        phase += brightening ? 10 : -10;
        if (phase >= 250) {
            brightening = false;
        } else if (phase <= 10) {
            brightening = true;
        }
    }

    PointLight getLight() {
        return light;
    }

    Point getTile() {
        return tile;
    }
}
