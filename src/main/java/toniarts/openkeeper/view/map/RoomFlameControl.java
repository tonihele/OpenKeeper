/* Copyright (C) 2026 OpenKeeper. Licensed under the GNU General Public License, version 3 or later. */
package toniarts.openkeeper.view.map;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.effect.VisualEffect;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/** Emits the room's short-lived flame/spark trail along its outer boundary. */
public final class RoomFlameControl extends AbstractControl {
    private static final int FLAME_EFFECT_ID = 20;
    // Room traversal uses the 4 Hz game clock; particle lifetimes use a separate 20 Hz clock.
    private static final double TICK_SECONDS = 1.0 / 4;
    private static final double DISTANCE_PER_TICK = 25.0 / 128;
    private final RoomInstance room;
    private final RoomPerimeter perimeter;
    private final EffectManagerState effects;
    private final IFogOfWarInformation fog;
    private final List<VisualEffect> trail = new ArrayList<>();
    private double elapsed;
    private long tick;

    public RoomFlameControl(RoomInstance room, EffectManagerState effects, IFogOfWarInformation fog) {
        this.room = room;
        this.effects = effects;
        this.fog = fog;
        perimeter = new RoomPerimeter(room.getCoordinates());
    }

    public static boolean isEligible(RoomInstance room) {
        return room.getOwnerId() != 0 && room.getOwnerId() != Player.NEUTRAL_PLAYER_ID && !room.isDestroyed()
                && room.getRoom().getFlags().contains(Room.RoomFlag.HAS_FLAME);
    }

    @Override
    protected void controlUpdate(float tpf) {
        trail.removeIf(effect -> effect.getEffectNode().getParent() == null);
        if (!isEligible(room) || perimeter.size() == 0) {
            clear();
            return;
        }
        elapsed += tpf;
        while (elapsed >= TICK_SECONDS) {
            elapsed -= TICK_SECONDS;
            tick++;
            double distance = (tick * DISTANCE_PER_TICK) % perimeter.size();
            if ((tick & 1) != 0 && fog.isExplored(perimeter.tileAt(distance))) {
                VisualEffect effect = effects.load((Node) spatial, perimeter.positionAt(distance),
                        FLAME_EFFECT_ID, false, room.getOwnerId());
                trail.add(effect);
            }
        }
    }

    /** Stop emitting and remove this room's remaining trail when it is rebuilt or sold. */
    public void clear() {
        trail.forEach(VisualEffect::removeEffect);
        trail.clear();
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }
}
